package de.dagere.peassEvaluation;

import java.io.File;
import java.io.IOException;

import de.peass.utils.StreamGobbler;

public class GitRegressionBranchUtil {
   
   public static void branch(final File projectFolder, final String name) throws InterruptedException, IOException {
      final ProcessBuilder builderBranch = new ProcessBuilder("git", "branch", name);
      builderBranch.directory(projectFolder);
      builderBranch.start().waitFor();
      
      final ProcessBuilder builderSwitch = new ProcessBuilder("git", "switch", name);
      builderSwitch.directory(projectFolder);
      builderSwitch.start().waitFor();
   }
   
   public static void commit(final File projectFolder, final String commitText) throws InterruptedException, IOException {
      final ProcessBuilder builderAdd = new ProcessBuilder("git", "add", "-A");
      builderAdd.directory(projectFolder);
      builderAdd.start().waitFor();

      final ProcessBuilder builderCommit = new ProcessBuilder("git", "-c", "user.name='Anonym'",
            "-c", "user.email='anonym@generated.org'",
            "commit", "-m", commitText);
      builderCommit.directory(projectFolder);
      final Process process = builderCommit.start();
      StreamGobbler.getFullProcess(process, true);
      int returncode = process.waitFor();
      if (returncode != 0) {
         throw new RuntimeException("Unexpected return code of git commit: " + returncode);
      }
   }
}
