package grafo.cdp.algorithms;

import grafo.cdp.structure.CDPInstance;
import grafo.cdp.structure.CDPSolution;
import grafo.optilib.metaheuristics.Algorithm;
import grafo.optilib.results.Result;
import grafo.optilib.tools.RandomManager;

public class MultipleExecutions implements Algorithm<CDPInstance, CDPSolution> {

    private Algorithm<CDPInstance, CDPSolution> algorithm;
    private int executions;

    public MultipleExecutions(Algorithm<CDPInstance, CDPSolution> algorithm, int executions) {
        this.algorithm = algorithm;
        this.executions = executions;
    }

    @Override
    public Result execute(CDPInstance instance) {
        Result r = new Result(instance.getName());
        for (int i = 0; i < executions; i++) {
            RandomManager.setSeed(System.currentTimeMillis());
            Result ri = algorithm.execute(instance);
            for (Result.ResultInfo result : ri.getResults()) {
                if (result.getName().equals("OF")) {
                    r.add("OF it_"+(i+1), result.getValue());
                } else if (result.getName().equals("Time (s)")) {
                    r.add("Time it_"+(i+1), result.getValue());
                }
            }
        }
        return r;
    }

    @Override
    public CDPSolution getBestSolution() {
        return null;
    }

    @Override
    public String toString() {
        return this.algorithm.toString()+"_"+executions;
    }
}
