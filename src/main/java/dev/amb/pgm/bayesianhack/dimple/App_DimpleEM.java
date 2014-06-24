
package dev.amb.pgm.bayesianhack.dimple;

import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.VariableBase;
import java.util.Arrays;

/**
 *
 * @author AntB
 */
public class App_DimpleEM {
    
    public static void main(String[] args) {
        
        FactorGraph g = DimpleUtils.createInitialModel();
        
        System.out.println("Initial BN factor graph in Dimple:\n" + g.getFullString());
        
        for(Factor factor : g.getFactors()) {
            System.out.println("\t" + factor.getName() + Arrays.toString(factor.getFactorTable().getWeightsSparseUnsafe()));
        }
        
        // just for testing, run inference
        g.solve();
        System.out.println("\nNow having run the solver, get marginal probs (beliefs)::");
        for(VariableBase var : g.getVariables()) {
            System.out.println("P(" + var.getName() + ")=" + Arrays.toString(var.asDiscreteVariable().getBelief()));
        }
        
        
    }

}
