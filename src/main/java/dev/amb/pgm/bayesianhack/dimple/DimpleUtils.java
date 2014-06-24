
package dev.amb.pgm.bayesianhack.dimple;

import com.analog.lyric.collect.BitSetUtil;
import com.analog.lyric.dimple.factorfunctions.core.FactorTable;
import com.analog.lyric.dimple.factorfunctions.core.FactorTableBase;
import com.analog.lyric.dimple.factorfunctions.core.IFactorTable;
import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.domains.JointDomainIndexer;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.Discrete;
import com.analog.lyric.dimple.model.variables.VariableBase;
import java.util.Arrays;

/**
 *
 * @author AntB
 */
public class DimpleUtils {

    
    
    public static FactorGraph createInitialModel() {
        
        // create nodes
        Discrete varA = new Discrete( 0 , 1);
        varA.setName("A");
        
        Discrete varB = new Discrete(0 , 1);
        varB.setName("B");
        
        Discrete varH = new Discrete(0 , 1);
        varH.setName("H");
        
        Discrete[] variables = { varA, varB, varH };
        
        // cpt indices
        int[][] indexH = {
            new int[] { 0 } ,
            new int[] { 1 }
        };
        
        int[][] indexA_H = { 
            new int[] { 0 , 0 } ,
            new int[] { 0 , 1 } ,
            new int[] { 1 , 0 } ,
            new int[] { 1 , 1 }
        };
        
        
        int[][] indexB_H = { 
            new int[] { 0 , 0 } ,
            new int[] { 0 , 1 } ,
            new int[] { 1 , 0 } ,
            new int[] { 1 , 1 }
        };
        
        // create CPT values
        double[] paramsA = { 0.39d , 0.45d ,0.61d , 0.55d };
        
        
        double[] paramsB = { 0.48d , 0.57d , 0.52d , 0.43d };
        double[] paramsH = { 0.6d , 0.4d };
        
        // create CPT factors
        IFactorTable factorTable_H = FactorTable.create(indexH, paramsH, varH);
        IFactorTable factorTable_A_H = FactorTable.create(indexA_H, paramsA, varA, varH);
        IFactorTable factorTable_B_H = FactorTable.create(indexB_H, paramsB, varB, varH);
        
        
        // combine into FactorGraph & done
        
        FactorGraph initialGraph = new FactorGraph();
        
        initialGraph.addVariables(variables);
        
        Factor factorH = initialGraph.addFactor(factorTable_H, varH);
        factorH.setName("Factor{H}");
        
        Factor factorA_H = initialGraph.addFactor(factorTable_A_H, varA, varH);
        factorA_H.setName("Factor{A | H}");
        
        Factor factorB_H = initialGraph.addFactor(factorTable_B_H, varB, varH);
        factorB_H.setName("Factor{B | H}");
        
        
        return initialGraph;
        
    }
}
