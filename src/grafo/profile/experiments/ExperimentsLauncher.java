
package grafo.profile.experiments;

import grafo.optilib.metaheuristics.Algorithm;
import grafo.optilib.metaheuristics.Combiner;
import grafo.optilib.results.Experiment;
import grafo.profile.algorithm.*;
import grafo.profile.combiners.CM1;
import grafo.profile.combiners.CM2;
import grafo.profile.constructive.C1;
import grafo.profile.constructive.C2;
import grafo.profile.constructive.C3;
import grafo.profile.constructive.C4;
import grafo.profile.improvement.Insert;
import grafo.profile.improvement.Swap;
import grafo.profile.structure.PInstance;
import grafo.profile.structure.PInstanceFactory;
import grafo.profile.structure.PSolution;
import grafo.optilib.metaheuristics.Constructive;
import grafo.optilib.metaheuristics.Improvement;

import java.util.Calendar;

public class ExperimentsLauncher {

    final static String pathFolder = "pmp_instances";

    final static String[] folderIndex = {"speedup"};
    final static String instanceIndex = ".mtx.rnd";

    static Constructive<PInstance, PSolution> c1;
    static Constructive<PInstance, PSolution> c2;
    static Constructive<PInstance, PSolution> c3;
    static Constructive<PInstance, PSolution> c4;
    static Improvement<PSolution> improvement;
    static Improvement<PSolution> improvement2;

    public static void main(String[] args) {

        c1 = new C1(-1.0);
        c2 = new C2(-1.0);
        c3 = new C3(-1.0);
        c4 = new C4(-1.0);
        improvement = new Insert();
        improvement2 = new Swap();
        Algorithm<PInstance, PSolution> algorithms[] = new Algorithm[]{

                new HPSS(c1, improvement, new CM1(), 100, 10, 10, 0.05),
                new SSS(c1, improvement, new CM1(), 100, 10, 10,0.05),
                new CPSS(new Constructive[]{c1, c2, c3, c4} , new Improvement[]{improvement, improvement2}, new Combiner[]{new CM1(), new CM2()}, 100, 10, 10,0.05),
                new APSS(new Constructive[]{c1, c2, c3, c4} , new Improvement[]{improvement, improvement2}, new Combiner[]{new CM1(), new CM2()}, 100, 10, 10,0.05),

        };

        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);

        String date = String.format("%04d-%02d-%02d", year, month, day);

        String[] extensions = new String[]{instanceIndex};
        PInstanceFactory factory = new PInstanceFactory();
        for (String index : folderIndex) {
            String outDir = "profileResults/" + date + "_" + index + "/";
            Experiment<PInstance, PInstanceFactory, PSolution> experiment = new Experiment<>(algorithms, factory);
            experiment.launch(outDir, pathFolder+"/"+index+"/", extensions);
        }
    }
}
