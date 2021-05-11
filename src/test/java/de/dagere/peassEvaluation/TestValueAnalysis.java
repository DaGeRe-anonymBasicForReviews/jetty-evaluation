package de.dagere.peassEvaluation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peassEvaluation.statistics.JmhBenchmarkValues;
import de.dagere.peassEvaluation.statistics.JmhReader;

public class TestValueAnalysis {

   @Test
   public void readBasicValues() throws JsonProcessingException, IOException {
      File inputFile = new File("src/test/resources/example-measurement-values/basic.json");
      Map<String, List<JmhBenchmarkValues>> benchmarkValues = JmhReader.getBenchmarkValues(inputFile);
      List<JmhBenchmarkValues> statistics = benchmarkValues.get("org.eclipse.jetty.util.PoolStrategyBenchmark.testAcquireReleasePoolWithStrategy");
      Assert.assertEquals(8897841.621005083, statistics.get(0).getStatistics().getMean(), 0.001);
      Assert.assertEquals(754202.2898317131, statistics.get(0).getStatistics().getStandardDeviation(), 0.001);

      Assert.assertEquals(9674404.780799752, statistics.get(1).getStatistics().getMean(), 0.001);
      Assert.assertEquals(494099.56286359177, statistics.get(1).getStatistics().getStandardDeviation(), 0.001);
   }

   @Test
   public void readRegressionValues() throws JsonProcessingException, IOException {
      File inputFile = new File("src/test/resources/example-measurement-values/regression-0.json");
      Map<String, List<JmhBenchmarkValues>> benchmarkValues = JmhReader.getBenchmarkValues(inputFile);
      List<JmhBenchmarkValues> statistics = benchmarkValues.get("org.eclipse.jetty.util.PoolStrategyBenchmark.testAcquireReleasePoolWithStrategy");
      Assert.assertEquals(9202941.630909853, statistics.get(0).getStatistics().getMean(), 0.001);
      Assert.assertEquals(724845.1722929896, statistics.get(0).getStatistics().getStandardDeviation(), 0.001);

      Assert.assertEquals(9790942.497074742, statistics.get(1).getStatistics().getMean(), 0.001);
      Assert.assertEquals(146891.89385610234, statistics.get(1).getStatistics().getStandardDeviation(), 0.001);
   }
}
