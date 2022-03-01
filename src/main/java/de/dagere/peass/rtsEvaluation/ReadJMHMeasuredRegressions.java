package de.dagere.peass.rtsEvaluation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.inference.TTest;

import de.dagere.peassEvaluation.statistics.JmhBenchmarkValues;
import de.dagere.peassEvaluation.statistics.JmhReader;

public class ReadJMHMeasuredRegressions {

   private static AllChanges allChanges = new AllChanges();

   public static void main(final String[] args) throws IOException {
      File measurementFolder = new File(args[0]);

      for (File regressionFolder : measurementFolder.listFiles()) {
         if (regressionFolder.isDirectory() && regressionFolder.getName().startsWith("regression-")) {
            File basicFile = new File(regressionFolder, "basic.json");
            File regressionFile = new File(regressionFolder, regressionFolder.getName() + ".json");
            Map<String, List<JmhBenchmarkValues>> basicValues = JmhReader.getBenchmarkValues(basicFile);
            Map<String, List<JmhBenchmarkValues>> currentValues = JmhReader.getBenchmarkValues(regressionFile);
            for (String benchmark : currentValues.keySet()) {
               compareValues(regressionFolder.getName(), benchmark, basicValues.get(benchmark), currentValues.get(benchmark));
            }

         }

      }

      for (Change change : allChanges.changes) {
         double tValue = new TTest().t(change.values.getStatistics(), change.valuesOld.getStatistics());
         System.out.println(change.version + " " + change.benchmark + " " + tValue);
      }
      System.out.println(allChanges.changes.size());
   }

   private static void compareValues(final String version, final String benchmark, final List<JmhBenchmarkValues> values, final List<JmhBenchmarkValues> currentBenchmarkValues) {
      currentBenchmarkValues.forEach(value -> {
         values.forEach(oldValue -> {
            if (oldValue.getParams().equals(value.getParams())) {

               boolean isChange = new TTest().tTest(oldValue.getStatistics(), value.getStatistics(), 0.01);
               if (isChange) {
                  System.out.println("Change in " + oldValue.getParams() + " " + value.getParams());
                  System.out.println(oldValue.getStatistics().getMean() + " " + value.getStatistics().getMean());
                  allChanges.changes.add(new Change(version, benchmark, oldValue, value));
               }
            }
         });
      });
   }
}
