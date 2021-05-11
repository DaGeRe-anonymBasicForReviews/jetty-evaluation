package de.dagere.peass.rtsEvaluation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.inference.TTest;

import de.dagere.peassEvaluation.statistics.JmhBenchmarkValues;
import de.dagere.peassEvaluation.statistics.JmhReader;

class Change {

   final String version;
   final String benchmark;
   final JmhBenchmarkValues values, valuesOld;

   public Change(final String version, final String benchmark, final JmhBenchmarkValues values, final JmhBenchmarkValues valuesOld) {
      this.version = version;
      this.benchmark = benchmark;
      this.values = values;
      this.valuesOld = valuesOld;
   }

}

class AllChanges {
   List<Change> changes = new LinkedList<>();
}

public class GetRegressions {

   private static AllChanges changes = new AllChanges();

   public static void main(final String[] args) throws IOException {
      File commitFile = new File(args[0]);
      File measurementFolder = new File(args[1]);

      String commitString = FileUtils.readFileToString(commitFile, StandardCharsets.UTF_8);
      String[] commits = commitString.split("\n");

      Map<String, List<JmhBenchmarkValues>> lastValues = new HashMap<>();

      for (String commit : commits) {
         File measurementFile = new File(measurementFolder, commit + ".json");
         if (measurementFile.exists()) {
            Map<String, List<JmhBenchmarkValues>> currentValues = JmhReader.getBenchmarkValues(measurementFile);
            currentValues.forEach((benchmarkName, values) -> {
               if (lastValues.containsKey(benchmarkName)) {
                  // System.out.println("Comparing " + benchmarkName + " in " + commit);
                  List<JmhBenchmarkValues> currentBenchmarkValues = lastValues.get(benchmarkName);
                  compareValues(commit, benchmarkName, values, currentBenchmarkValues);
               }
               lastValues.put(benchmarkName, values);
            });
         }
      }
      
      for (Change change : changes.changes) {
         double tValue = new TTest().t(change.values.getStatistics(), change.valuesOld.getStatistics());
         System.out.println(change.version + " " + change.benchmark + " " + tValue);
      }
      System.out.println(changes.changes.size());
   }

   private static void compareValues(final String version, final String benchmark, final List<JmhBenchmarkValues> values, final List<JmhBenchmarkValues> currentBenchmarkValues) {
      currentBenchmarkValues.forEach(value -> {
         values.forEach(oldValue -> {
            if (oldValue.getParams().equals(value.getParams())) {

               boolean isChange = new TTest().tTest(oldValue.getStatistics(), value.getStatistics(), 0.01);
               if (isChange) {
                  System.out.println("Change in " + oldValue.getParams() + " " + value.getParams());
                  System.out.println(oldValue.getStatistics().getMean() + " " + value.getStatistics().getMean());
                  changes.changes.add(new Change(version, benchmark, oldValue, value));
               }
            }
         });
      });
   }
}
