
package dev.amb.pgm.bayesianhack.dimple;


import com.analog.lyric.dimple.exceptions.DimpleException;
import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.Discrete;
import com.analog.lyric.dimple.model.variables.VariableBase;
import java.util.ArrayList;
import java.util.List;

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

    
    
    
    public static void main(String[] args){
        
        // Build a factor over 3 binary vars
        
        /*
        
            X1         X2
              \\      //
               \\    //
                  X3
            
        */
        
        FactorGraph bn = createVstruct();
        
        
        try {
            System.out.println(bn.toString());
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        
        System.out.println("Starting BNover X1, X2 and X3:\n" + bn.getFullString());
        
        FactorGraph newBN = marginaliseOutVariables(bn, "X1");
        
        System.out.println("\n\nNow with X1 removed....\n\n" + newBN.getFullString());
        
    }
    
    
    public static FactorGraph createVstruct() {
        FactorGraph bn = new FactorGraph("P(X3 | X1, X2");
        
        Discrete var_X1 = new Discrete("absent" , "present");
        var_X1.setName("X1");
        
        Discrete var_X2 = new Discrete("absent" , "present");
        var_X2.setName("X2");
        
        Discrete var_X3 = new Discrete("absent" , "present");
        var_X3.setName("X3");
        
        bn.addVariables(var_X1, var_X2, var_X3);
        
        // P(X1)
        //  X1   P(X1)
        //  0   0.6
        //  1   0.4
        
        double[] dist_X1 = {0.6d , 0.4d};
        
        int[][] idx_X1 = new int[][] {
            //X1
            { 0 } ,
            { 1 }
        };
        
        //P(X2)
        //  X2   P(X2)
        //  0   0.7
        //  1   0.3
        
        double[] dist_X2 = {0.6d , 0.4d};
        
        int[][] idx_X2 = new int[][] {
            //X2
            { 0 } ,
            { 1 }
        };
        
        
        // P(X3 | X1, X2)
        //  X3  X1  X2  P(X2)
        //  0   0   0   0.1
        //  0   0   1   0.2
        //  0   1   0   0.15
        //  0   1   1   0.4
        //  1   0   0   0.9
        //  1   0   1   0.8
        //  1   1   0   0.85
        //  1   1   1   0.6
        
        
        // indices over P(X3 | X1, X2)
        int[][] idx_X3 = new int[][] {
        
            // X3, X1, X2
            {0 , 0 , 0},
            {0 , 0 , 1},
            {0 , 1 , 0},
            {0 , 1 , 1},
            {1 , 0 , 0},
            {1 , 0 , 1},
            {1 , 1 , 0},
            {1 , 1 , 1},
        };
        
        
        double[] dist_X3 = new double[] {
          0.1, 0.2, 0.15, 0.4,
          0.9, 0.8, 0.85, 0.6
        };
        
        Factor p_X3_given_X1_X2 = bn.addFactor(idx_X3, dist_X3 , var_X3, var_X1, var_X2);
        p_X3_given_X1_X2.setDirectedTo(var_X3);
        
        Factor p_X1 = bn.addFactor(idx_X1, dist_X1, var_X1);
        p_X1.setDirectedTo(var_X1);
        
        Factor p_X2 = bn.addFactor(idx_X2, dist_X2, var_X2);
        p_X2.setDirectedTo(var_X2);
        return bn;
    }
    
    
    
    
    
    
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
            
            // Create new list of sibling factors
            List<Factor> siblings = new ArrayList<Factor>();
            
            for(Factor f : var.getSiblings()) {
                siblings.add(f);
            }
            
            for(Factor f : siblings) {
                
                VariableBase[] allVars = new VariableBase[f.getSiblingCount()];
                    
                for(int idx = 0 ; idx < f.getSiblingCount(); idx++) {
                    allVars[idx] = f.getSibling(idx);
                }

                try {
                    f.replaceVariablesWithJoint(allVars, var); // nope nothing happens :)
                } catch(DimpleException de) {
                    de.printStackTrace();
                }
                
                // TODO do we need to build a new FactorGraph???
                
            }
            
        }
        
        
        return bn;
        
    }
    
    
    // Can we answer marginalisation queries?
    
    // Can we set evidence and get new factors?
    
    // Can we get the likelihood score for a given dataset?
    
    // Can we just do Factor manipulation as a last resort?
    
    
    // Here is code for param estimation from GitHub, unpick this for details
    /*
    
*   Copyright 2013 Analog Devices, Inc.
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.


                private FactorGraph _fg;
                private IFactorTable [] _tables;
                private Random _r;
                private HashMap<IFactorTable,ArrayList<Factor>> _table2factors;
                private boolean _forceKeep;

                public ParameterEstimator(FactorGraph fg, IFactorTable [] tables, Random r)
                {
                        _fg = fg;
                        _tables = tables;
                        _r = r;

                        HashMap<IFactorTable,ArrayList<Factor>> table2factors = new HashMap<IFactorTable, ArrayList<Factor>>();

                        for (Factor f  : fg.getFactorsFlat())
                        {
                                IFactorTable ft = f.getFactorTable();
                                if (! table2factors.containsKey(ft))
                                        table2factors.put(ft,new ArrayList<Factor>());
                                table2factors.get(ft).add(f);
                        }

                        //Verify directionality is consistent.
                        _table2factors = table2factors;

                }

                public void setRandom(Random r)
                {
                        _r = r;
                }

                public HashMap<IFactorTable,ArrayList<Factor>> getTable2Factors()
                {
                        return _table2factors;
                }

                public IFactorTable [] getTables()
                {
                        return _tables;
                }

                IFactorTable [] saveFactorTables(IFactorTable [] fts)
                {
                        IFactorTable [] savedFts = new IFactorTable[fts.length];
                        for (int i = 0; i < fts.length; i++)
                                savedFts[i] = fts[i].clone();
                        return savedFts;
                }

                IFactorTable [] unique(IFactorTable  [] factorTables)
                {
                        HashSet<IFactorTable> set = new HashSet<IFactorTable>();
                        for (int i = 0; i < factorTables.length; i++)
                                set.add(factorTables[i]);
                        factorTables = new IFactorTable[set.size()];
                        int i = 0;
                        for (IFactorTable ft : set)
                        {
                                factorTables[i] = ft;
                                i++;
                        }
                        return factorTables;
                }

                public FactorGraph getFactorGraph()
                {
                        return _fg;
                }

                public void setForceKeep(boolean val)
                {
                        _forceKeep = val;
                }

                public void run(int numRestarts, int numSteps)
                {
                        //make sure the factortable list is unique
                        _tables = unique(_tables);

                        //measure betheFreeEnergy
                        _fg.solve();
                        double currentBFE = _fg.getBetheFreeEnergy();
                        IFactorTable [] bestFactorTables = saveFactorTables(_tables);

                        //for each restart
                        for (int i = 0; i <= numRestarts; i++)
                        {
                                //if not first time, pick random weights
                                if (i != 0)
                                        for (int j = 0; j < _tables.length; j++)
                                        {
                                                _tables[j].randomizeWeights(_r);
                                                if (_tables[j].isDirected())
                                                        _tables[j].normalizeConditional();
                                        }

                                //for numSteps
                                for (int j = 0; j < numSteps; j++)
                                {
                                        runStep(_fg);
                                }

                                _fg.solve();
                                double newBetheFreeEnergy = _fg.getBetheFreeEnergy();

                                //if betheFreeEnergy is better
                                //store this is answer
                                if (newBetheFreeEnergy < currentBFE || _forceKeep)
                                {
                                        currentBFE = newBetheFreeEnergy;
                                        bestFactorTables = saveFactorTables(_tables);
                                }

                        }

                        //Set weights to best answer
                        for (int i = 0; i < _tables.length; i++)
                        {
                                _tables[i].copy(bestFactorTables[i]);
                        }
                }

                public abstract void runStep(FactorGraph fg);

                public static class BaumWelch extends ParameterEstimator
                {

                        public BaumWelch(FactorGraph fg, IFactorTable[] tables, Random r)
                        {
                                super(fg, tables, r);

                                for (IFactorTable table : getTable2Factors().keySet())
                                {
                                        ArrayList<Factor> factors = getTable2Factors().get(table);
                                        int [] direction = null;
                                        for (Factor f : factors)
                                        {
                                                if (f.getFactorTable() != table)
                                                {
                                                        Misc.breakpoint();
                                                }
                                                int [] tmp = f.getDirectedTo();
                                                if (tmp == null)
                                                        throw new DimpleException("Baum Welch only works with directed Factors");
                                                if (direction == null)
                                                        direction = tmp;
                                                else
                                                {
                                                        if (tmp.length != direction.length)
                                                                throw new DimpleException("Directions must be the same for all factors sharing a Factor Table");
                                                        for (int i = 0; i < tmp.length; i++)
                                                                if (tmp[i] != direction[i])
                                                                        throw new DimpleException("Directions must be the same for all factors sharing a Factor Table");
                                                }
                                        }
                                }
                        }



                        @Override
                        public void runStep(FactorGraph fg)
                        {

                                //run BP
                                fg.solve();

                                //Assign new weights
                                //For each Factor Table
                                for (IFactorTable ft : getTable2Factors().keySet())
                                {
                                        //Calculate the average of the FactorTable beliefs
                                        ArrayList<Factor> factors = getTable2Factors().get(ft);

                                        double [] sum = new double[ft.sparseSize()];

                                        for (Factor f : factors)
                                        {
                                                if (f.getFactorTable() != ft)
                                                {
                                                        Misc.breakpoint();
                                                }
                                                double [] belief = (double[])requireNonNull(f.getSolver()).getBelief();
                                                for (int i = 0; i < sum.length; i++)
                                                        sum[i] += belief[i];


                                        }


                                        //Get first directionality
                                        Factor firstFactor = factors.get(0);
                                        int [] directedTo = firstFactor.getDirectedTo();
                                        int [] directedFrom = firstFactor.getDirectedFrom();

                                        //Set the weights to that
                                        ft.replaceWeightsSparse(sum);
                                        if (directedTo != null && directedFrom != null)
                                        {
                                                ft.makeConditional(BitSetUtil.bitsetFromIndices(directedTo.length + directedFrom.length, directedTo));
                                        }
                }
                        }
                }
        }
    
    */
    
}
