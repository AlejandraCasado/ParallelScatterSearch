package grafo.maxcut.structure;

import grafo.optilib.structure.InstanceFactory;

public class MCFactory extends InstanceFactory<MCInstance> {
    @Override
    public MCInstance readInstance(String s) {return new MCInstance(s);
    }
}
