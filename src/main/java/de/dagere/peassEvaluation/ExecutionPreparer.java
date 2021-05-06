package de.dagere.peassEvaluation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.execution.MavenPomUtil;
import de.dagere.peass.dependency.execution.ProjectModules;
import de.dagere.peass.dependency.moduleinfo.ModuleInfoEditor;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationConfiguration;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;

public class ExecutionPreparer {
   public static void main(final String[] args) throws FileNotFoundException, IOException, XmlPullParserException {
      File projectFolder = new File(args[0]);
      ProjectModules modules = MavenPomUtil.getModules(new File(projectFolder, "pom.xml"));
      List<File> moduleList = modules.getModules();
      for (File module : moduleList) {
         editPom(module);

         final File potentialModuleFile = new File(module, "src/main/java/module-info.java");
         System.out.println("Checking " + potentialModuleFile.getAbsolutePath());
         if (potentialModuleFile.exists()) {
            ModuleInfoEditor.addKiekerRequires(potentialModuleFile);
         }

      }
      instrumentSources(projectFolder);
   }

   private static void instrumentSources(final File projectFolder) throws IOException {
      final HashSet<String> includedPatterns = new HashSet<>();
      includedPatterns.add("*");
      final InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, true, false, includedPatterns, true, 1000);
      final InstrumentKiekerSource sourceInstrumenter = new InstrumentKiekerSource(configuration);
      sourceInstrumenter.instrumentProject(projectFolder);
   }

   private static void editPom(final File module) throws IOException, XmlPullParserException, FileNotFoundException {
      File pomFile = new File(module, "pom.xml");
      final Model model;
      try (FileInputStream fileInputStream = new FileInputStream(pomFile)) {
         final MavenXpp3Reader reader = new MavenXpp3Reader();
         model = reader.read(fileInputStream);
      }
      MavenPomUtil.extendDependencies(model, false);

      try (FileWriter fileWriter = new FileWriter(pomFile)) {
         final MavenXpp3Writer writer = new MavenXpp3Writer();
         writer.write(fileWriter, model);
      }
   }
}
