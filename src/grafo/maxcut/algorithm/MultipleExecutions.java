package grafo.maxcut.algorithm;

import grafo.maxcut.structure.MCInstance;
import grafo.maxcut.structure.MCSolution;
import grafo.optilib.metaheuristics.Algorithm;
import grafo.optilib.results.Result;
import grafo.optilib.tools.RandomManager;

public class MultipleExecutions implements Algorithm<MCInstance, MCSolution> {

    private final Algorithm<MCInstance, MCSolution> algorithm;
    private final int executions;

    public MultipleExecutions(Algorithm<MCInstance, MCSolution> algorithm, int executions) {
        this.algorithm = algorithm;
        this.executions = executions;
    }

    @Override
    public Result execute(MCInstance instance) {
        Result r = new Result(instance.getName());
        System.out.print(instance.getName()+"\t");
        for (int i = 0; i < executions; i++) {
            RandomManager.setSeed(System.currentTimeMillis());
            Result ri = algorithm.execute(instance);
            for (Result.ResultInfo result : ri.getResults()) {
                if (result.getName().equals("cuts")) {
                    System.out.print(result.getValue()+"\t");
                    r.add("cuts it_"+(i+1), result.getValue());
                } else if (result.getName().equals("time")) {
                    System.out.print(result.getValue()+"\t");
                    r.add("time it_"+(i+1), result.getValue());
                }
            }
        }
        System.out.println();
        return r;
    }

    @Override
    public MCSolution getBestSolution() {
        return null;
    }

    @Override
    public String toString() {
        return this.algorithm.toString()+"_"+executions;
    }
}
