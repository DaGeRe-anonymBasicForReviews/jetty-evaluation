package de.dagere.peassEvaluation.treeReading;

import java.io.File;

import de.peass.dependency.analysis.KiekerReaderConfiguration;
import de.peass.dependency.analysis.ModuleClassMapping;
import kieker.analysis.trace.reconstruction.TraceReconstructionStage;

public class KiekerTreeReaderConfiguration extends KiekerReaderConfiguration {
   public TreeStageUnknownRoot readTree(final File kiekerTraceFolder, final boolean ignoreEOIs, final ModuleClassMapping mapping) {
      TreeStageUnknownRoot treeStage = new TreeStageUnknownRoot(systemModelRepositoryNew, ignoreEOIs, mapping);
      
      TraceReconstructionStage executionStage = prepareTillExecutionTrace(kiekerTraceFolder);
      this.connectPorts(executionStage.getExecutionTraceOutputPort(), treeStage.getInputPort());
      
      return treeStage;
   }
}