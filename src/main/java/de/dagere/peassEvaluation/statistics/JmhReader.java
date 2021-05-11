package de.dagere.peassEvaluation.statistics;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.dagere.peass.utils.Constants;

public class JmhReader {
   
   public static Map<String, List<JmhBenchmarkValues>> getBenchmarkValues(final File file) throws JsonProcessingException, IOException{
      ArrayNode benchmarks = (ArrayNode) Constants.OBJECTMAPPER.readTree(file);
      Map<String, List<JmhBenchmarkValues>> results = new HashMap<>();
      for (JsonNode benchmarkMethod : benchmarks) {
         final String name = benchmarkMethod.get("benchmark").asText();
         JsonNode primaryMetric = benchmarkMethod.get("primaryMetric");
         StatisticalSummary summary = getStatistics(benchmarkMethod, primaryMetric);
         
         List<JmhBenchmarkValues> currentBenchmarkValues = results.get(name);
         if (currentBenchmarkValues == null) {
            currentBenchmarkValues = new LinkedList<>();
            results.put(name, currentBenchmarkValues);
         }
         
         List<String> paramValues = getParams(benchmarkMethod);
         currentBenchmarkValues.add(new JmhBenchmarkValues(paramValues, summary));
      }
      return results;
   }

   /**
    * @deprecated please use getBenchmarkValues instead
    */
   @Deprecated
   public static List<JmhBenchmarkValues> getValues(final File file) throws JsonProcessingException, IOException {
      ArrayNode benchmarks = (ArrayNode) Constants.OBJECTMAPPER.readTree(file);
      List<JmhBenchmarkValues> results = new LinkedList<>();
      for (JsonNode benchmarkMethod : benchmarks) {
         JsonNode primaryMetric = benchmarkMethod.get("primaryMetric");
         StatisticalSummary summary = getStatistics(benchmarkMethod, primaryMetric);
         
         List<String> paramValues = getParams(benchmarkMethod);
         results.add(new JmhBenchmarkValues(paramValues, summary));
      }
      return results;
   }

   private static List<String> getParams(final JsonNode benchmarkMethod) {
      ObjectNode params = (ObjectNode) benchmarkMethod.get("params");
      List<String> paramValues = new LinkedList<>();
      if (params != null) {
         for (JsonNode value : params) {
            paramValues.add(value.asText());
         }
      }
      return paramValues;
   }

   private static StatisticalSummary getStatistics(final JsonNode benchmarkMethod, final JsonNode primaryMetric) {
      JsonNode forks = benchmarkMethod.get("forks");
      JsonNode score = primaryMetric.get("score");
      JsonNode scoreError = primaryMetric.get("scoreError");
      double variance = scoreError.asDouble() * scoreError.asDouble();
      StatisticalSummary summary = new StatisticalSummaryValues(score.asDouble(), variance, forks.asInt(), 0, 0, 0);
      return summary;
   }
}
