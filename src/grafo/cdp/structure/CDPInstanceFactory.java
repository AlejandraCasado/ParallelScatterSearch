package grafo.cdp.structure;

import grafo.optilib.structure.InstanceFactory;

public class CDPInstanceFactory extends InstanceFactory<CDPInstance> {


    @Override
    public CDPInstance readInstance(String s) {
        return new CDPInstance(s);
    }
}
