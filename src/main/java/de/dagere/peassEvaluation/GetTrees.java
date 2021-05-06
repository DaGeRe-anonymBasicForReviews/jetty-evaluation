package de.dagere.peassEvaluation;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.execution.MavenPomUtil;
import de.dagere.peass.dependency.execution.ProjectModules;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.utils.Constants;
import de.dagere.peassEvaluation.treeReading.KiekerTreeReaderConfiguration;
import de.dagere.peassEvaluation.treeReading.TreeStageUnknownRoot;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import teetime.framework.Execution;

@Command(description = "Gets the trees after a kieker-jmh-execution", name = "getTrees")

public class GetTrees implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(GetTrees.class);

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
      ProjectModules modules = MavenPomUtil.getModules(new File(projectFolder, "pom.xml"));
      ModuleClassMapping mapping = new ModuleClassMapping(projectFolder, modules);
      for (File kiekerFolder : traceFolder.listFiles()) {
         System.out.println("Analyzing: " + kiekerFolder.getAbsolutePath());
         TreeStageUnknownRoot stage = executeTreeStage(kiekerFolder, true, mapping);
         CallTreeNode root = stage.getRoot();
         if (root != null) {
            System.out.println("Root: " + root.getCall());

            File treeFile = new File(treeFolder, root.getCall() + ".json");
            int i = 0;
            while (treeFile.exists()) {
               i++;
               treeFile = new File(treeFolder, root.getCall() + "_" + i + ".json");
            }
            Constants.OBJECTMAPPER.writeValue(treeFile, root);
         } else {
            LOG.error("No root in " + kiekerFolder.getAbsolutePath());
         }

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
