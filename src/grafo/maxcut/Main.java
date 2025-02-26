
package grafo.maxcut;

import grafo.maxcut.algorithm.*;
import grafo.maxcut.combiner.CB1;
import grafo.maxcut.combiner.CB2;
import grafo.maxcut.combiner.CB3;
import grafo.maxcut.combiner.Combiner;
import grafo.maxcut.constructive.C1;
import grafo.maxcut.constructive.C2;
import grafo.maxcut.improvement.LS1;
import grafo.maxcut.improvement.LS2;
import grafo.maxcut.structure.MCFactory;
import grafo.maxcut.structure.MCInstance;
import grafo.maxcut.structure.MCSolution;
import grafo.optilib.metaheuristics.Algorithm;
import grafo.optilib.metaheuristics.Constructive;
import grafo.optilib.metaheuristics.Improvement;
import grafo.optilib.results.Experiment;

import java.util.Calendar;

public class Main {
    static String pathFolder = "./instances/mcp/all";

    public static void main(String[] args) {
        int executions = 10;
        Algorithm<MCInstance, MCSolution>[] algorithms=new Algorithm[]{
                new MultipleExecutions(new SSS(100, new C2(),new LS2()), executions),
                new MultipleExecutions(new HPSS(100,new C2(),new LS2()), executions),
                new MultipleExecutions(
                        new APSS(100,
                                new Constructive[]{
                                        new C1(),
                                        new C2()
                                },
                                new Improvement[]{
                                        new LS1(),
                                        new LS2()
                                },
                                new Combiner[]{
                                        new CB1(),
                                        new CB2(),
                                        new CB3()
                        }),
                        executions),
                new MultipleExecutions(
                        new CPSS(100,
                                new Constructive[]{
                                        new C1(),
                                        new C2()
                                },
                                new Improvement[]{
                                        new LS1(),
                                        new LS2()
                                },
                                new Combiner[]{
                                        new CB1(),
                                        new CB2(),
                                        new CB3()
                                }),
                        executions),
        };
        if (args.length > 0) {
            pathFolder = args[0];
        }
        MCFactory factory=new MCFactory();
        Experiment<MCInstance, MCFactory, MCSolution> experiment=new Experiment<>(algorithms,factory);
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        String date = String.format("%04d-%02d-%02d", year, month, day);
        String outDir = "mcpResults/" + date + "/";
        experiment.launch(outDir, pathFolder, new String[]{".mc", ".rud", ".txt"});
    }
}