package de.dagere.peassEvaluation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.analysis.CalledMethodLoader;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TraceElement;
import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.execution.utils.ProjectModules;
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
      File simpleTraceFolder = new File(dataFolder, "simpleTraces");
      treeFolder.mkdirs();
      simpleTraceFolder.mkdirs();
      ProjectModules modules = MavenPomUtil.getModules(new File(projectFolder, "pom.xml"), new ExecutionConfig());
      ModuleClassMapping mapping = new ModuleClassMapping(projectFolder, modules, new ExecutionConfig());
      for (File kiekerFolder : traceFolder.listFiles()) {
         LOG.info("Analyzing: " + kiekerFolder.getAbsolutePath());
         CallTreeNode root = readTree(treeFolder, mapping, kiekerFolder);
         
         if (root != null) {
            writeSimpleTrace(simpleTraceFolder, mapping, kiekerFolder, root);
         }
      }
      return null;
   }

   private void writeSimpleTrace(File simpleTraceFolder, ModuleClassMapping mapping, File kiekerFolder, CallTreeNode root) throws IOException {
      File usableFile = findUsableFile(simpleTraceFolder, root.getCall(), ".txt");
      KiekerConfig kiekerConfig = new KiekerConfig();
      kiekerConfig.setTraceSizeInMb(10000);
      CalledMethodLoader loader = new CalledMethodLoader(kiekerFolder, mapping, kiekerConfig);
      ArrayList<TraceElement> shortTrace = loader.getShortTrace("");

      // Big traces are not handled
      if (shortTrace != null) {
         StringBuilder builder = new StringBuilder();
         shortTrace.forEach(element -> 
            builder.append(element != null ? element.toString() : "")
                   .append("\n"));
         
         Files.write(usableFile.toPath(), builder.toString().getBytes());
      }
      
   }

   public void readTrace(File traceFolder, ModuleClassMapping mapping, File kiekerFolder) {

   }

   private CallTreeNode readTree(File treeFolder, ModuleClassMapping mapping, File kiekerFolder) throws IOException, StreamWriteException, DatabindException {
      TreeStageUnknownRoot stage = executeTreeStage(kiekerFolder, true, mapping);
      CallTreeNode root = stage.getRoot();
      if (root != null) {
         LOG.info("Root: " + root.getCall());

         String call = root.getCall();
         File treeFile = findUsableFile(treeFolder, call, ".json");
         LOG.info("Writing to: {}", treeFile);
         Constants.OBJECTMAPPER.writeValue(treeFile, root);
      } else {
         LOG.error("No root in " + kiekerFolder.getAbsolutePath());
      }
      return root;
   }

   private File findUsableFile(File treeFolder, String call, String ending) {
      File treeFile = new File(treeFolder, call + ending);
      int i = 0;
      while (treeFile.exists()) {
         i++;
         treeFile = new File(treeFolder, call + "_" + i + ending);
      }
      return treeFile;
   }

   public static TreeStageUnknownRoot executeTreeStage(final File kiekerTraceFolder, final boolean ignoreEOIs, final ModuleClassMapping mapping) {
      KiekerTreeReaderConfiguration configuration = new KiekerTreeReaderConfiguration();
      TreeStageUnknownRoot stage = configuration.readTree(kiekerTraceFolder, ignoreEOIs, mapping);

      Execution execution = new Execution(configuration);
      execution.executeBlocking();

      return stage;
   }

}
