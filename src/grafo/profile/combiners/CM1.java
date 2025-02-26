package grafo.profile.combiners;

import grafo.optilib.metaheuristics.Combiner;
import grafo.profile.structure.PSolution;

import java.util.Set;

public class CM1 implements Combiner<PSolution> {
    @Override
    public PSolution combine(PSolution s1, PSolution s2) {
        PSolution toRet = (s1.isBetterThan(s2)) ? s1 : s2;
        Set<Integer> differentLabels = s1.getNodesWithDifferentLabelIn(s2);
        while (!differentLabels.isEmpty()) {
            PSolution best = null;
            int bestNodeToExchange = -1;
            int bestLabelToAssign = -1;
            for (int node : differentLabels) {
                int prevLabel = s1.getLabelOfNode(node);
                int newLabel = s2.getLabelOfNode(node);
                s1.swapNodes(prevLabel, newLabel);
                if (best == null || s1.isBetterThan(best)) {
                    best = new PSolution(s1);
                    bestNodeToExchange = node;
                    bestLabelToAssign = newLabel;
                }
                s1.swapNodes(prevLabel, newLabel);
            }
            s1.swapNodes(s1.getLabelOfNode(bestNodeToExchange), bestLabelToAssign);
            if (best.isBetterThan(toRet)) {
                toRet = new PSolution(best);
            }
            s1 = best;
            differentLabels = s1.getNodesWithDifferentLabelIn(s2);
        }
        return toRet;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
