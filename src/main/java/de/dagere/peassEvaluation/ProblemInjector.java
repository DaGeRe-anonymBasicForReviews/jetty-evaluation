package de.dagere.peassEvaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

import de.peass.dependency.ClazzFileFinder;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.JavaParserProvider;
import de.peass.dependency.traces.TraceReadUtils;
import de.peass.dependency.traces.requitur.content.TraceElementContent;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class ProblemInjector implements Callable<Void> {
   @Option(names = { "-projectFolder", "--projectFolder" }, description = "Folder of project (for code analysis)", required = true)
   private File projectFolder;

   @Option(names = { "-dataFolder", "--dataFolder" }, description = "Folder for data (subfolder traces should be present, trees is created)", required = true)
   private File dataFolder;

   public static void main(final String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new ProblemInjector());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }

   @Override
   public Void call() throws Exception {
      File gitFile = new File(projectFolder, ".git");
      if (!gitFile.exists()) {
         throw new RuntimeException("Folder needs to be a git repo, but " + gitFile.getAbsolutePath() + " exists");
      }

      File treeFolder = new File(dataFolder, "trees");
      for (File treeFile : treeFolder.listFiles()) {
         CallTreeNode node = Constants.OBJECTMAPPER.readValue(treeFile, CallTreeNode.class);

         List<CallTreeNode> leafs = getLeafs(node.getChildren());
         for (CallTreeNode leaf : leafs) {
            ChangedEntity entity = leaf.toEntity();
            generateRegression(entity);
         }
      }

      // CallTreeNode selectedNode = new CallTreeNode("jetty-util§org.eclipse.jetty.util.thread.QueuedThreadPool#execute(Runnable)",
      // "public void jetty-util§org.eclipse.jetty.util.thread.QueuedThreadPool.execute(Runnable)", null, null);

      return null;
   }

   private List<CallTreeNode> getLeafs(final List<CallTreeNode> level) {
      List<CallTreeNode> nextLevel = new LinkedList<>();
      for (CallTreeNode levelNode : level) {
         nextLevel.addAll(levelNode.getChildren());
      }
      if (nextLevel.isEmpty()) {
         return level;
      } else {
         return getLeafs(nextLevel);
      }
   }

   private void generateRegression(final ChangedEntity entity) throws FileNotFoundException, IOException {
      File module = new File(projectFolder, entity.getModule());
      File clazzFile = ClazzFileFinder.getClazzFile(module, entity);
      if (clazzFile.exists()) {
         CompilationUnit unit = JavaParserProvider.parse(clazzFile);
         TraceElementContent methodTraceElement = new TraceElementContent(entity.getClazz(), entity.getMethod(), entity.getParameterTypes(), -1);
         CallableDeclaration<?> callable = TraceReadUtils.getMethod(methodTraceElement, unit);

         if (callable instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) callable;
            BlockStmt changed = method.getBody().get().addStatement("System.out.println(\"This makes everything slower\");");
            method.setBody(changed);
         } else if (callable instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructor = (ConstructorDeclaration) callable;
            BlockStmt changed = constructor.getBody().addStatement("System.out.println(\"This makes everything slower\");");
            constructor.setBody(changed);
         } else {
            throw new RuntimeException("Method " + entity.getMethod() + " " + entity.getParametersPrintable() + " not found in " + entity.getClazz());
         }
         Files.write(clazzFile.toPath(), unit.toString().getBytes(Charset.defaultCharset()));
      } else {
         throw new RuntimeException("File " + clazzFile.getAbsolutePath() + " not found");
      }
   }

}
