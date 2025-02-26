package grafo.profile.improvement;

import grafo.optilib.metaheuristics.Improvement;
import grafo.profile.structure.PSolution;

import java.util.Objects;

public class Insert implements Improvement<PSolution> {
    @Override
    public void improve(PSolution sol) {
        int n = sol.getNumberOfNodes(); int bestOF = sol.getOfValue();
        PSolution bestSol = null;
        PSolution originalSol = new PSolution(sol);
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < n - 1; i++) {
                for (int j = i; j < n; j++) {
                    sol.swapNodes(j, j + 1);
                    int actVal = sol.getOfValue();
                    if (actVal < bestOF) {
                        bestSol = new PSolution(sol);
                        bestOF = actVal;
                        improved = true;
                    }
                }
                sol.copy(Objects.requireNonNullElse(bestSol, originalSol));
            }
            sol.copy(originalSol);
            for (int i = n - 1; i > 0; i--) {
                for (int j = i; j > 1; j--) {
                    sol.swapNodes(j, j - 1);
                    int actVal = sol.getOfValue();
                    if (actVal < bestOF) {
                        bestSol = new PSolution(sol);
                        bestOF = actVal;
                        improved = true;
                    }
                }
                sol.copy(Objects.requireNonNullElse(bestSol, originalSol));
            }
            sol.copy(Objects.requireNonNullElse(bestSol, originalSol));
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
