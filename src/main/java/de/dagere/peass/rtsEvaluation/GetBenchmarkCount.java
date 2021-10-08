package de.dagere.peass.rtsEvaluation;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.utils.Constants;

/**
 * Gets the count of benchmarks selected by one file
 * @author reichelt
 *
 */
public class GetBenchmarkCount {
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      File inputFile = new File(args[0]);
      ExecutionData executions = Constants.OBJECTMAPPER.readValue(inputFile, ExecutionData.class);
      
      int count = 0;
      for (TestSet versionTests : executions.getVersions().values()) {
         count+= versionTests.getTests().size();
      }
      System.out.println("Selected: " + count);
      System.out.println("Executions required (predecessor needs to be measured): " + count * 2);
   }
}
