package de.dagere.peassEvaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.inference.TTest;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peassEvaluation.statistics.JmhBenchmarkValues;
import de.dagere.peassEvaluation.statistics.JmhReader;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class GetRegressions implements Callable<Void> {

   @Option(names = { "-dataFolder", "--dataFolder" }, description = "Folder for data (subfolder traces should be present, trees is created)")
   private File dataFolder;

   public static void main(final String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new GetRegressions());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }
   
   List<Integer> minimalIterationsForChange = new LinkedList<>();

   @Override
   public Void call() throws Exception {
      for (File regressionFolder : dataFolder.listFiles()) {
         if (regressionFolder.isDirectory() && regressionFolder.getName().startsWith("regression-")) {
            readRegression(regressionFolder);
         }
      }

      File regressionFile = new File(dataFolder, "regressions.csv");
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(regressionFile))){
         int count = 0;
         for (int i = 0; i < 100; i++) {
            writer.write(i + " ");
            for (Integer val : minimalIterationsForChange) {
               if (val == i) {
                  count++;
               }
            }
            writer.write(count + "\n");
         }
      }
      return null;
   }

   private void readRegression(final File regressionFolder) throws JsonProcessingException, IOException {
      System.out.print("Reading " + regressionFolder.getName() + " ");
      List<JmhBenchmarkValues> basicValues = JmhReader.getValues(new File(regressionFolder, "basic.json"));
      List<JmhBenchmarkValues> regressionValues = JmhReader.getValues(new File(regressionFolder, regressionFolder.getName() + ".json"));

      int changeError = 100;
      for (JmhBenchmarkValues basicValue : basicValues) {
         boolean found = false;
         for (JmhBenchmarkValues regressionValue : regressionValues) {
            if (basicValue.getParams().equals(regressionValue.getParams())) {
               found = true;
               
               changeError = findMinimalChangeIterations(changeError, basicValue, regressionValue);
               
//                     for (double type1error = 0.01; type1error < 0.2; type1error += 0.01) {
//                        boolean isChange = new TTest().tTest(basicValue.getStatistics(), regressionValue.getStatistics(), type1error);
//                        if (isChange) {
//                           changeError = type1error;
//                           break;
//                        }
//                     }
//                     if (changeError == -1) {
//                        System.out.println(basicValue.getStatistics().getMean() + " " + regressionValue.getStatistics().getMean());
//                        System.out.println(basicValue.getStatistics().getStandardDeviation() + " " + regressionValue.getStatistics().getStandardDeviation());
//                     }
            }
         }
         if (!found) {
            System.out.println("Warning: " + basicValue.getParams() + " not found in regression execution");
         }
      }
      System.out.println(changeError);
      minimalIterationsForChange.add(changeError);
   }

   private int findMinimalChangeIterations(int changeError, final JmhBenchmarkValues basicValue, final JmhBenchmarkValues regressionValue) {
      for (int i = 3; i < 100; i++) {
         StatisticalSummary basic = new StatisticalSummaryValues(basicValue.getStatistics().getMean(), 
               basicValue.getStatistics().getVariance(),
               i, 
               0, 0, 0);
         StatisticalSummary regression = new StatisticalSummaryValues(regressionValue.getStatistics().getMean(), 
               regressionValue.getStatistics().getVariance(),
               i, 
               0, 0, 0);
         boolean isChange = new TTest().tTest(basic, regression, 0.01);
         if (isChange) {
            changeError = i; break;
         }
      }
      return changeError;
   }
}
