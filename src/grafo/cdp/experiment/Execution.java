package grafo.cdp.experiment;

import grafo.cdp.algorithms.*;
import grafo.cdp.constructives.DGM;
import grafo.cdp.improvement.DynLS;
import grafo.cdp.structure.CDPInstance;
import grafo.cdp.structure.CDPInstanceFactory;
import grafo.cdp.structure.CDPSolution;
import grafo.optilib.metaheuristics.Algorithm;
import grafo.optilib.metaheuristics.Constructive;
import grafo.optilib.metaheuristics.Improvement;
import grafo.optilib.results.Experiment;

import java.util.Calendar;
import java.util.Locale;

public class Execution {

    public static final int TIME_LIMIT = 1800*1000;

    final static String[] folderIndex = {"all"};
    final static String pathFolder = "instances/cdp";

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        CDPInstanceFactory factory = new CDPInstanceFactory();

        Algorithm<CDPInstance, CDPSolution>[] algs = new Algorithm[] {
                new MultipleExecutions(
                        new SSS(new DGM(-1.00f), new DynLS(1.00f), 0.75f, 10, 2),
                        10
                ),

                new MultipleExecutions(
                        new HPSS(new DGM(-1.00f), new DynLS(1.00f), 0.75f, 10, 2),
                        10
                ),
                new MultipleExecutions(
                        new APSS(
                                new Constructive[]{new DGM(0.25f), new DGM(0.5f), new DGM(0.75f), new DGM(-1.00f)},
                                new Improvement[]{new DynLS(1.00f), new DynLS(1.25f), new DynLS(1.50f), new DynLS(-1.00f)},
                                0.75f, 10, 2
                        ),
                        10
                ),
                new MultipleExecutions(
                        new CPSS(
                                new Constructive[]{new DGM(0.25f), new DGM(0.5f), new DGM(0.75f), new DGM(-1.00f)},
                                new Improvement[]{new DynLS(1.00f), new DynLS(1.25f), new DynLS(1.50f), new DynLS(-1.00f)},
                                0.75f, 10, 2
                        ),
                        10
                ),

        };

        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);

        String date = String.format("%04d-%02d-%02d", year, month, day);

        String[] extensions = new String[]{".txt"};
        for (String index : folderIndex) {
            String outDir = "cdp_results/" + date + "_" + index + "/";
            Experiment<CDPInstance, CDPInstanceFactory, CDPSolution> experiment = new Experiment<>(algs, factory);
            experiment.launch(outDir, pathFolder+"/"+index+"/", extensions);
        }
    }
}
