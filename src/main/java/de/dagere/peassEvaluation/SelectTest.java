package de.dagere.peassEvaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import de.peass.dependency.ClazzFileFinder;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.changesreading.ClazzFinder;
import de.peass.dependency.changesreading.JavaParserProvider;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.Version;
import de.peass.testtransformation.TestMethodFinder;
import de.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class SelectTest implements Callable<Void> {

   @Option(names = { "-folder", "--folder" }, description = "Folder of the project that should be analyzed", required = true)
   File projectFolder;

   @Option(names = { "-dependencyfile", "--dependencyfile" }, description = "Path to the dependencyfile")
   File dependencyFile;

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

      List<TestCase> withoutSleepTests = selectWithoutSleepTests(tests);

      int selectedIndex = new Random().nextInt(withoutSleepTests.size());
      
      TestCase tc = withoutSleepTests.get(selectedIndex);
      System.out.println(tc.getModule() + ChangedEntity.MODULE_SEPARATOR + tc.getClazz() + ChangedEntity.METHOD_SEPARATOR + tc.getMethod());
      
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File("test.txt")))){
         writer.write(tc.getModule() + ChangedEntity.MODULE_SEPARATOR + tc.getClazz() + ChangedEntity.METHOD_SEPARATOR + tc.getMethod());
         writer.flush();
      }
      
      return null;
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
