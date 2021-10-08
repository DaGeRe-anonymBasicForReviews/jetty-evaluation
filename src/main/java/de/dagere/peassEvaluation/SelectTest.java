package de.dagere.peassEvaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import de.dagere.peass.dependency.ClazzFileFinder;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.changesreading.ClazzFinder;
import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.Version;
import de.dagere.peass.testtransformation.TestMethodFinder;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Option;

class TestProperties {
   TestCase test;
   int occurences;
   int length;
   double coefficient;

   public TestProperties(final TestCase test, final int occurences, final int length, final double coefficient) {
      this.test = test;
      this.occurences = occurences;
      this.length = length;
      this.coefficient = coefficient;
   }

   public double getCoefficient() {
      return coefficient;
   }

   public TestCase getTest() {
      return test;
   }
}

public class SelectTest implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(SelectTest.class);

   @Option(names = { "-folder", "--folder" }, description = "Folder of the project that should be analyzed", required = true)
   File projectFolder;

   @Option(names = { "-dependencyfile", "--dependencyfile" }, description = "Path to the dependencyfile")
   File dependencyFile;

   @Option(names = { "-tracesFolder", "--tracesFolder" }, description = "Path to the current traces folder", required = true)
   File tracesFolder;

   @Option(names = { "-method", "--method" }, description = "Method that contains the regression", required = true)
   String method;

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final SelectTest command = new SelectTest();
      final CommandLine commandLine = new CommandLine(command);
      System.exit(commandLine.execute(args));
   }

   @Override
   public Void call() throws Exception {
      Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
      String newestVersion = dependencies.getNewestVersion();

      Version version = dependencies.getVersions().get(newestVersion);

      TestSet tests = version.getTests();

      System.out.println("Before sleep test removal: " + tests.classCount());

      List<TestCase> withoutSleepTests = selectWithoutSleepTests(tests);

      System.out.println("After sleep test removal: " + tests.classCount());

      TestProperties selectedTest = selectTestBasedOnTraces(newestVersion, withoutSleepTests);

      // int selectedIndex = new Random().nextInt(withoutSleepTests.size());
      //
      // TestCase tc = withoutSleepTests.get(selectedIndex);
      // System.out.println(tc.getModule() + ChangedEntity.MODULE_SEPARATOR + tc.getClazz() + ChangedEntity.METHOD_SEPARATOR + tc.getMethod());
      //
      if (selectedTest != null) {
         writeTest(selectedTest.getTest());
      }

      return null;
   }

   private void writeTest(final TestCase finalTest) throws IOException {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File("test.txt")))) {
         writer.write(finalTest.getModule() + ChangedEntity.MODULE_SEPARATOR +
               finalTest.getClazz() + ChangedEntity.METHOD_SEPARATOR +
               finalTest.getMethod());
         writer.flush();
      }
   }

   private TestProperties selectTestBasedOnTraces(final String newestVersion, final List<TestCase> withoutSleepTests) throws IOException {
      TestProperties selectedTest = null;

      File parentFolder = new File(tracesFolder, "views_jetty.project" + File.separator + "view_" + newestVersion);
      for (TestCase test : withoutSleepTests) {
         String fileName = test.getModule() + ChangedEntity.MODULE_SEPARATOR + test.getClazz();
         File testFolder = new File(parentFolder, fileName);
         File methodFolder = new File(testFolder, test.getMethod());

         File traceFile = new File(methodFolder, newestVersion.substring(0, 6) + "_method_expanded");

         if (traceFile.exists()) {
            String content = Files.readString(traceFile.toPath());

            int occurences = StringUtils.countMatches(content, method);
            int length = StringUtils.countMatches(content, "\n");
            double coefficient = ((double) length) / occurences;
            System.out.println("Test: " + test.toString() + " " + occurences + " " + length + " " + coefficient);
            if (selectedTest == null || coefficient < selectedTest.coefficient) {
               selectedTest = new TestProperties(test, occurences, length, coefficient);
            }
         } else {
            LOG.info("Did not analyze tracefile {} since it did not exist", traceFile);
         }
      }
      if (selectedTest != null) {
         System.out.println("Finally selected: " + selectedTest.getTest());
      } else {
         System.out.println("No test selected");
      }
      return selectedTest;
   }

   private List<TestCase> selectWithoutSleepTests(final TestSet tests) throws FileNotFoundException {
      List<TestCase> withoutSleepTests = new LinkedList<>();
      for (TestCase tc : tests.getTests()) {
         File moduleFile = new File(projectFolder, tc.getModule());
         File clazzFile = ClazzFileFinder.getClazzFile(moduleFile, tc.toEntity());
         CompilationUnit unit = JavaParserProvider.parse(clazzFile);
         for (ClassOrInterfaceDeclaration clazz : ClazzFinder.getClazzDeclarations(unit)) {
            if (clazz.getNameAsString().equals(tc.getShortClazz())) {
               List<MethodDeclaration> methods4 = TestMethodFinder.findJUnit4TestMethods(clazz);
               addIfDoesNotContainSleep(withoutSleepTests, tc, methods4);
               List<MethodDeclaration> methods5 = TestMethodFinder.findJUnit5TestMethods(clazz);
               addIfDoesNotContainSleep(withoutSleepTests, tc, methods5);
            }
         }
      }
      return withoutSleepTests;
   }

   private void addIfDoesNotContainSleep(final List<TestCase> withoutSleepTests, final TestCase tc, final List<MethodDeclaration> methods) {
      for (MethodDeclaration method : methods) {
         if (method.getNameAsString().equals(tc.getMethod())) {
            String text = method.getBody().get().toString();
            if (!text.contains("Thread.sleep")) {
               withoutSleepTests.add(tc);
            }
         }
      }
   }
}
