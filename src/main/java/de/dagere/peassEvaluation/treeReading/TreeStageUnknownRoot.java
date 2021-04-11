package de.dagere.peassEvaluation.treeReading;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.ClazzFileFinder;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.kieker.KiekerPatternConverter;
import kieker.analysis.trace.AbstractTraceProcessingStage;
import kieker.model.repository.SystemModelRepository;
import kieker.model.system.model.Execution;
import kieker.model.system.model.ExecutionTrace;

public class TreeStageUnknownRoot extends AbstractTraceProcessingStage<ExecutionTrace> {

   private static final Logger LOG = LogManager.getLogger(TreeStageUnknownRoot.class);

   private CallTreeNode root;

   private final boolean ignoreEOIs;
   private final ModuleClassMapping mapping;
   private CallTreeNode lastParent = null, lastAdded = null;
   private int lastStackSize = 1;
   private long testTraceId = -1;

   public TreeStageUnknownRoot(final SystemModelRepository systemModelRepository, final boolean ignoreEOIs, final ModuleClassMapping mapping) {
      super(systemModelRepository);

      this.ignoreEOIs = ignoreEOIs;
      this.mapping = mapping;
   }

   @Override
   protected void execute(final ExecutionTrace trace) throws Exception {
      LOG.info("Trace: " + trace.getTraceId());

      for (final Execution execution : trace.getTraceAsSortedExecutionSet()) {
         final String fullClassname = execution.getOperation().getComponentType().getFullQualifiedName().intern();
         final String methodname = execution.getOperation().getSignature().getName().intern();
         final String call = fullClassname + "#" + methodname;
         final String kiekerPattern = KiekerPatternConverter.getKiekerPattern(execution.getOperation());
         LOG.debug("{} {}", kiekerPattern, execution.getEss());

         if (methodname.startsWith("test") && execution.getEss() == 0 && execution.getEoi() == 0) {
            if (root == null) {
               root = new CallTreeNode("root-" + call, null, null, null);
            }
            CallTreeNode currentRoot = root.appendChild(call, kiekerPattern, null);
            lastParent = currentRoot;
            lastAdded = currentRoot;
            lastStackSize = 1;
            testTraceId = execution.getTraceId();
         } else if (!methodname.equals("class$") && !methodname.startsWith("access$")) {
            addExecutionToTree(execution, fullClassname, methodname, call, kiekerPattern);
         }
      }
   }

   private void addExecutionToTree(final Execution execution, final String fullClassname, final String methodname, final String call, final String kiekerPattern) {
      if (root != null && execution.getTraceId() == testTraceId) {
         LOG.debug(fullClassname + " " + execution.getOperation().getSignature() + " " + execution.getEoi() + " " + execution.getEss());
         LOG.trace("Last Stack: " + lastStackSize);

         callLevelDown(execution);
         callLevelUp(execution);
         LOG.trace("Parent: {} {}", lastParent.getCall(), lastParent.getEss());

         if (execution.getEss() + 1 == lastParent.getEss()) {
            final String message = "Trying to add " + call + "(" + execution.getEss() + ")" + " to " + lastParent.getCall() + "(" + lastParent.getEss()
                  + "), but parent ess always needs to be child ess -1";
            LOG.error(message);
            throw new RuntimeException(message);
         }

         boolean hasEqualNode = false;
         for (CallTreeNode candidate : lastParent.getChildren()) {
            if (candidate.getKiekerPattern().equals(kiekerPattern)) {
               hasEqualNode = true;
               lastAdded = candidate;
            }
         }
         if (!ignoreEOIs || !hasEqualNode) {
            lastAdded = lastParent.appendChild(call, kiekerPattern, null);
            setModule(fullClassname, lastAdded);
         }
      }
   }

   public CallTreeNode getRoot() {
      return root;
   }

   private void setModule(final String fullClassname, final CallTreeNode node) {
      final String outerClazzName = ClazzFileFinder.getOuterClass(fullClassname);
      final String moduleOfClass = mapping.getModuleOfClass(outerClazzName);
      if (moduleOfClass == null) {
         throw new RuntimeException("Module of " + outerClazzName + " not found " + fullClassname);
      }
      node.setModule(moduleOfClass);
   }

   private void callLevelUp(final Execution execution) {
      while (execution.getEss() < lastStackSize) {
         LOG.trace("Level up: " + execution.getEss() + " " + lastStackSize);
         lastParent = lastParent.getParent();
         LOG.debug("Parent: {}", lastParent.getCall());
         lastStackSize--;
         if (lastParent == null) {
            throw new RuntimeException("Should not set lastParent to null");
         }
      }
   }

   private void callLevelDown(final Execution execution) {
      if (execution.getEss() > lastStackSize) {
         LOG.trace("Level down: " + execution.getEss() + " " + lastStackSize);
         lastParent = lastAdded;
         // lastStackSize++;
         if (lastStackSize + 1 != lastParent.getEss() + 1) {
            LOG.error("Down caused wrong lastStackSize: {} {}", lastStackSize, lastParent.getEss());
         }
         lastStackSize = lastParent.getEss() + 1;
         LOG.trace("Stack size after going down: {} Measured: {}", lastParent.getEss(), lastStackSize);
      }
   }
}
