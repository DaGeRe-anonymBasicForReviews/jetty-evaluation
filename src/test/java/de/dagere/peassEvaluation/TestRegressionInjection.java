package de.dagere.peassEvaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import de.dagere.peassEvaluation.regressionGeneration.CodeRegressionCreator;
import de.peass.dependency.analysis.data.ChangedEntity;

public class TestRegressionInjection {
   
   @Test
   public void testSimpleInjection() throws FileNotFoundException, IOException {
      File exampleDir = createRegression("testA");
      
      String changedFile = FileUtils.readFileToString(new File(exampleDir, "src/main/java/ExampleClass.java"), StandardCharsets.UTF_8);
      MatcherAssert.assertThat(changedFile, Matchers.containsString("System.nanoTime()"));
   }
   
   @Test
   public void testReturnInjection() throws FileNotFoundException, IOException {
      File exampleDir = createRegression("testB");
      
      String changedFile = FileUtils.readFileToString(new File(exampleDir, "src/main/java/ExampleClass.java"), StandardCharsets.UTF_8);
      MatcherAssert.assertThat(changedFile.indexOf("System.nanoTime()"), Matchers.lessThan(changedFile.indexOf("return")));
   }

   private File createRegression(final String testMethodName) throws IOException, FileNotFoundException {
      File exampleDir = new File("target/example");
      FileUtils.copyDirectory(new File("src/test/resources/example"), exampleDir);
      
      CodeRegressionCreator regressionCreator = new CodeRegressionCreator(exampleDir);
      
      regressionCreator.createCodeRegression(new ChangedEntity("ExampleClass", "", testMethodName));
      return exampleDir;
   }
}
