package de.dagere.peassEvaluation;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peassEvaluation.treeReading.KiekerTreeReaderConfiguration;
import de.dagere.peassEvaluation.treeReading.TreeStageUnknownRoot;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import teetime.framework.Execution;

@Command(description = "Gets the trees after a kieker-jmh-execution", name = "getTrees")

public class GetTrees implements Callable<Void>{
   
   @Option(names = { "-dataFolder", "--dataFolder" }, description = "Folder for data (subfolder traces should be present, trees is created)")
   private File dataFolder;
   
   @Option(names = { "-projectFolder", "--projectFolder" }, description = "Folder of project (for code analysis)")
   private File projectFolder;
   
   public static void main(final String[] args) throws IOException, XmlPullParserException {
      try {
         final CommandLine commandLine = new CommandLine(new GetTrees());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }
   
   @Override
   public Void call() throws Exception {
      File treeFolder = new File(dataFolder, "trees");
      File traceFolder = new File(dataFolder, "traces");
      treeFolder.mkdirs();
      ModuleClassMapping mapping = new ModuleClassMapping(projectFolder);
      for (File kiekerFolder : traceFolder.listFiles()) {
         System.out.println("Analyzing: " + kiekerFolder.getAbsolutePath());
         TreeStageUnknownRoot stage = executeTreeStage(kiekerFolder, true, mapping);
         System.out.println("Root: " + stage.getRoot().getCall());

         File treeFile = new File(treeFolder, stage.getRoot().getCall() + ".json");
         int i = 0;
         while (treeFile.exists()) {
            i++;
            treeFile = new File(treeFolder, stage.getRoot().getCall() + "_" + i + ".json");
         }
         Constants.OBJECTMAPPER.writeValue(treeFile, stage.getRoot());
      }
      return null;
   }

   public static TreeStageUnknownRoot executeTreeStage(final File kiekerTraceFolder, final boolean ignoreEOIs, final ModuleClassMapping mapping) {
      KiekerTreeReaderConfiguration configuration = new KiekerTreeReaderConfiguration();
      TreeStageUnknownRoot stage = configuration.readTree(kiekerTraceFolder, ignoreEOIs, mapping);

      Execution execution = new Execution(configuration);
      execution.executeBlocking();

      return stage;
   }

}
