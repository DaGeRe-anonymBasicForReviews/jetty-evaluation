package de.dagere.peassEvaluation.regressionGeneration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import de.peass.dependency.ClazzFileFinder;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.JavaParserProvider;
import de.peass.dependency.traces.TraceReadUtils;
import de.peass.dependency.traces.requitur.content.TraceElementContent;

public class CodeRegressionCreator {
   private final File projectFolder;

   public CodeRegressionCreator(final File projectFolder) {
      this.projectFolder = projectFolder;
   }

   public void createCodeRegression(final ChangedEntity entity) throws FileNotFoundException, IOException {
      File module = new File(projectFolder, entity.getModule());
      File clazzFile = ClazzFileFinder.getClazzFile(module, entity);
      if (clazzFile.exists()) {
         CompilationUnit unit = JavaParserProvider.parse(clazzFile);
         TraceElementContent methodTraceElement = new TraceElementContent(entity.getClazz(), entity.getMethod(), entity.getParameterTypes(), -1);
         CallableDeclaration<?> callable = TraceReadUtils.getMethod(methodTraceElement, unit);

         String statement = "{final long exitTime = System.nanoTime() + 5; " +
               "         long currentTime;" +
               "         do {\n" +
               "            currentTime = System.nanoTime();\n" +
               "         } while (currentTime < exitTime);}";
         Statement busyWait = new JavaParser().parseStatement(statement).getResult().get();
         if (callable instanceof MethodDeclaration) {
            insertToMethod(callable, busyWait);
         } else if (callable instanceof ConstructorDeclaration) {
            insertToConstructor(callable, busyWait);
         } else {
            throw new RuntimeException("Method " + entity.getMethod() + " " + entity.getParametersPrintable() + " not found in " + entity.getClazz() + " " + callable);
         }
         Files.write(clazzFile.toPath(), unit.toString().getBytes(Charset.defaultCharset()));
      } else {
         throw new RuntimeException("File " + clazzFile.getAbsolutePath() + " not found");
      }
   }

   private void insertToConstructor(final CallableDeclaration<?> callable, final Statement busyWait) {
      ConstructorDeclaration constructor = (ConstructorDeclaration) callable;
      BlockStmt changed = constructor.getBody().addStatement(busyWait);
      constructor.setBody(changed);
   }

   private void insertToMethod(final CallableDeclaration<?> callable, final Statement busyWait) {
      MethodDeclaration method = (MethodDeclaration) callable;
      BlockStmt oldBody = method.getBody().get();
      BlockStmt changed = oldBody.addStatement(0, busyWait);
      method.setBody(changed);
   }
}
