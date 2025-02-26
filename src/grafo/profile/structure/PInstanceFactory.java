package grafo.profile.structure;

import grafo.optilib.structure.InstanceFactory;

public class PInstanceFactory extends InstanceFactory<PInstance> {
    @Override
    public PInstance readInstance(String s) {
        return new PInstance(s);
    }
}