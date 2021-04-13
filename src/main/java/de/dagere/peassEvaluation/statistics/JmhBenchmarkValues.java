package de.dagere.peassEvaluation.statistics;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

public class JmhBenchmarkValues {
   private final List<String> params;
   private final StatisticalSummary statistics;

   public JmhBenchmarkValues(final List<String> params, final StatisticalSummary statistics) {
      this.params = params;
      this.statistics = statistics;
   }

   public List<String> getParams() {
      return params;
   }

   public StatisticalSummary getStatistics() {
      return statistics;
   }
   
   
}