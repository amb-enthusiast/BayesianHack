
package dev.amb.pgm.bayesianhack.dimple;


import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.core.INode;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.VariableBase;
import java.util.ArrayList;

/**
 *
 * @author AntB
 */
public class App_DimpleProbability {
    
    
    
    /*
    Want to look at how to do more general probability queries with Dimple
    This might be a blocker for us, if we cant get at the P(Evidence | Model) and more general MAP queries
    That said, the EM method must be doing something similar, therefore there may be good clues in the Dimple src
    */

    
    
    public static FactorGraph marginaliseOutVariables(FactorGraph bn , String... variableNamesToMarginaliseOut) {
        
        // Hmm.. we can readily get the marginals for each variable, but can we get a new FactorGraph, where we have marginalised out the requested variables?
        
        // For a FactorGraph, are there operations on the factors that would marginalise out a variable
        if(bn == null) {
            return null;
        }
        
        if(bn.getVariableCount() < 1) {
            return null;
        }
        
        if(variableNamesToMarginaliseOut == null) {
            return bn;
        }
        
        if(variableNamesToMarginaliseOut.length == 0) {
            return bn;
        }
        
        ArrayList<String> validVariableNames = new ArrayList<String>();
        
        // check variables in input factorGraph
        for(String varName : variableNamesToMarginaliseOut) {
            if(bn.getVariableByName(varName) != null) {
                validVariableNames.add(varName);
            } else {
                System.out.println("WARN: Skipping variable named <" + varName + "> as it is not contianed in the input factorGraph.");
            }
        }
        
        FactorGraph g = new FactorGraph();
        
        for(String varName : validVariableNames) {
            
            VariableBase var = bn.getVariableByName(varName);
            
            for(INode sibling : var.getSiblings()) {
                
                if(sibling.isFactor() || sibling instanceof com.analog.lyric.dimple.model.factors.Factor) {
                    
                    Factor f = (Factor) sibling;
                    
                    // get factor variable list
                    VariableBase[] allVars = new VariableBase[7];
                    
                    f.replaceVariablesWithJoint(f.getSiblings().toArray(new VariableBase[0]), var);
                    
                    // TODO do we need to build a new FactorGraph???
                    
                }
                
            }
            
            
        }
        
        
        return bn;
        
    }
    
    
    // Can we answer marginalisation queries?
    
    // Can we set evidence and get new factors?
    
    // Can we just do Factor manipulation as a last resort?
}
