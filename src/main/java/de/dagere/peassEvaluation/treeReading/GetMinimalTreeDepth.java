package de.dagere.peassEvaluation.treeReading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.utils.Constants;

public class GetMinimalTreeDepth {
   public static void main(String[] args) throws IOException {
      File regressionFile = new File("scripts/regressions.csv");

      File treeResultsFolder = new File(args[0]);
      File treeFolder = new File(treeResultsFolder, "trees");

      try (BufferedReader fileReader = new BufferedReader(new FileReader(regressionFile))) {
         String line;
         while ((line = fileReader.readLine()) != null) {
            String[] splitted = line.split(";");

            String regression = splitted[0];
            String test = splitted[1];
            String method = splitted[2];

            File tree = new File(treeFolder, "root-" + test + ".json");
            CallTreeNode rootNode = Constants.OBJECTMAPPER.readValue(tree, CallTreeNode.class);

            int level = getChildLevel(rootNode, method, 0);
            
            level = getParameterFileLevel(treeFolder, test, method, tree, level);
            
            System.out.println(regression + " " + test + " " + method + " " + level);
         }
      }
   }

   private static int getParameterFileLevel(File treeFolder, String test, String method, File tree, int level) throws IOException, StreamReadException, DatabindException {
      int i = 1;
      File parameterFile = new File(treeFolder, "root-" + test + "_" + i + ".json");
      while (parameterFile.exists()) {
         CallTreeNode parameterRootNode = Constants.OBJECTMAPPER.readValue(parameterFile, CallTreeNode.class);
         int currentChildLevel = getChildLevel(parameterRootNode, method, 0);
         level = Math.min(level, currentChildLevel);
         i++;
         parameterFile = new File(treeFolder, "root-" + test + "_" + i + ".json");
      }
      return level;
   }

   private static int getChildLevel(CallTreeNode rootNode, String method, int i) {
      if (rootNode.getCall().equals(method)) {
         return i;
      } else {
         int minValue = Integer.MAX_VALUE;
         for (CallTreeNode child : rootNode.getChildren()) {
            minValue = Math.min(minValue, getChildLevel(child, method, i + 1));
         }
         return minValue;
      }
   }
}
