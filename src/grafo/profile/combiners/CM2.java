package grafo.profile.combiners;

import grafo.optilib.metaheuristics.Combiner;
import grafo.optilib.tools.RandomManager;
import grafo.profile.structure.PSolution;

import java.util.Random;
import java.util.Set;

public class CM2 implements Combiner<PSolution> {
    @Override
    public PSolution combine(PSolution s1, PSolution s2) {
        PSolution toRet = null;
        Set<Integer> differentLabels = s1.getNodesWithDifferentLabelIn(s2);

        Random rnd = RandomManager.getRandom();
        while (!differentLabels.isEmpty()) {
            PSolution best = new PSolution(s1);
            int bestNodeToExchange = -1;
            int bestLabelToAssign = -1;
            for (int node : differentLabels) {
                int prevLabel = s1.getLabelOfNode(node);
                int newLabel = s2.getLabelOfNode(node);
                s1.swapNodes(prevLabel, newLabel);
                if (Double.compare(rnd.nextDouble(), 0.5) < 0 || bestNodeToExchange == -1) {
                    best = new PSolution(s1);
                    bestNodeToExchange = node;
                    bestLabelToAssign = newLabel;
                }
                s1.swapNodes(prevLabel, newLabel);
            }
            s1.swapNodes(s1.getLabelOfNode(bestNodeToExchange), bestLabelToAssign);
            if (toRet == null || best.isBetterThan(toRet)) {
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
