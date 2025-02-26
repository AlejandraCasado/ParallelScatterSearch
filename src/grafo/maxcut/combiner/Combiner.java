package grafo.maxcut.combiner;

import grafo.maxcut.structure.MCSolution;

import java.util.List;

public interface Combiner {
    List<MCSolution> combine(MCSolution s1, MCSolution s2);
}
