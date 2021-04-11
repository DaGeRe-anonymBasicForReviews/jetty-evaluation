package de.dagere.peassEvaluation;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class RegressionInjector implements Callable<Void> {

   @Option(names = { "-projectFolder", "--projectFolder" }, description = "Folder of project (for code analysis)", required = true)
   private File projectFolder;

   @Option(names = { "-dataFolder", "--dataFolder" }, description = "Folder for data (subfolder traces should be present, trees is created)", required = true)
   private File dataFolder;

   @Option(names = { "-count", "--count" }, description = "Count of problems for injection", required = true)
   private int count;

   public static void main(final String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new RegressionInjector());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }

   @Override
   public Void call() throws Exception {
      File gitFile = new File(projectFolder, ".git");
      if (!gitFile.exists()) {
         throw new RuntimeException("Folder needs to be a git repo, but " + gitFile.getAbsolutePath() + " exists");
      }

      File treeFolder = new File(dataFolder, "trees");
      File[] trees = treeFolder.listFiles();
      try (FileWriter benchmarkNameWriter = new FileWriter(new File(dataFolder, "regressions.csv"))) {
         for (int regressionIndex = 0; regressionIndex < count; regressionIndex++) {
            new RegressionBranchGenerator(projectFolder).createRegressionBranch(trees, benchmarkNameWriter, regressionIndex);
         }
      }

      return null;
   }

   

   

}
