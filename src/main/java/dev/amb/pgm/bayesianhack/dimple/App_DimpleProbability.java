
package dev.amb.pgm.bayesianhack.dimple;


import com.analog.lyric.dimple.factorfunctions.core.IFactorTable;
import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.domains.DiscreteDomain;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.Discrete;
import com.analog.lyric.dimple.model.variables.VariableBase;
import com.analog.lyric.dimple.solvers.sumproduct.SumProductSolver;
import java.util.Arrays;

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
        
            X1        X2
              \\     //
               \\   //
                 *  *
                  X3
        */
        
        FactorGraph model = createStudentGraph();
        System.out.println("Created the Student model, now running inference to get marginal probs...\n");
        
        model.solve();
        for(VariableBase v : model.getVariables()) {
            Discrete var = v.asDiscreteVariable();
            System.out.println("\nP(" + var.getName() + ")::");
            
            double[] probs = var.getBelief();
            DiscreteDomain vals = var.getDiscreteDomain();
            
            if(probs.length == vals.size()) {
                for(int idx=0; idx < probs.length; idx++) {
                    System.out.println("\tP(" + var.getName() + " = " + vals.getElement(idx) + ") = " + probs[idx]);
                }
            } else {
                System.out.println("WARN:: Variable " + var.getName() + " # values != # of beliefs!");
            }
        }
        
        String evidenceVarName = "Grade";
        String evidenceValue = "C";
        System.out.println("\n****\nNow setting " + evidenceVarName + " = " + evidenceValue + "...\n");
        
        setEvidence(model, evidenceVarName, evidenceValue);
        
        
        System.out.println("Updated marginal beliefs::\n");
        
        for(VariableBase v : model.getVariables()) {
            Discrete var = v.asDiscreteVariable();
            System.out.println("\nP(" + var.getName() + ")::");
            
            double[] probs = var.getBelief();
            DiscreteDomain vals = var.getDiscreteDomain();
            
            if(probs.length == vals.size()) {
                for(int idx=0; idx < probs.length; idx++) {
                    System.out.println("\tP(" + var.getName() + " = " + vals.getElement(idx) + ") = " + probs[idx]);
                }
            } else {
                System.out.println("WARN:: Variable " + var.getName() + " # values != # of beliefs!");
            }
        }
        
        
    }
    
    
    
    /*
    
    Here is where we hack around with Dimple to learn more about probabilistic queries
    */
    
    
    public static FactorGraph setEvidence(FactorGraph fg , String variableName, Object variableValue) {
        
        if(fg == null) {
            return null;
        }
        
        if(variableName == null) {
            return fg;
        }
        
        System.out.println("INFO: Seek to set " + variableName + " = " + variableValue);
        
        for(Factor f : fg.getFactors()) {
            
            for(VariableBase var : f.getSiblings()) {
                
                System.out.println("\tChecking f_" + f.getName() + " var=" + var.getName() + "...");
                        
                if(var.getName().equals(variableName)) {
                    
                    System.out.println("\t***" + f.getName() + " contains " + variableName + "; will set value=" + variableValue + "\n");
                    
                    var.asDiscreteVariable().setFixedValue(variableValue);
                }
                
                
            }
            
        }
        
        
        fg.setSolverFactory(new SumProductSolver());
        fg.solve();
        
        for(Factor f : fg.getFactors()) {
            
            for(VariableBase v : f.getSiblings()) {
                
                if(v.getName().equals(variableName)) {
                    
                    Object beliefs = f.getSolver().getBelief();

                    if(beliefs != null) {
                        
                        double[] newBeliefs = (double[]) beliefs;
                        System.out.println("Factor  " + f.getName() + " OLD beliefs = " + Arrays.toString( f.getFactorTable().getWeightsSparseUnsafe()));
                        System.out.println("Factor  " + f.getName() + " SOLVER beliefs = " + Arrays.toString( newBeliefs));
                        
                        f.getFactorTable().replaceWeightsSparse(newBeliefs);
                        System.out.println("Factor  " + f.getName() + " NEW pre-norm beliefs = " + Arrays.toString( f.getFactorTable().getWeightsSparseUnsafe()));
                        
//                        int [] directedTo = f.getDirectedTo();
//                        int [] directedFrom = f.getDirectedFrom();
//
//                        //Set the weights to that
//                        f.getFactorTable().replaceWeightsSparse(newBeliefs);

                        IFactorTable newFactorTable = f.getFactorTable().clone();
//                        newFactorTable.normalizeConditional();
                        
//                        if (directedTo != null && directedFrom != null)
//                        {
//                                newFactorTable.makeConditional(BitSetUtil.bitsetFromIndices(directedTo.length + directedFrom.length, directedTo));
//                        }
                        // Does this work, in terms of modification?
                        f.getFactorTable().replaceWeightsSparse(newFactorTable.getWeightsSparseUnsafe());
                        
                        System.out.println("Factor  " + f.getName() + " NEW post-norm beliefs = " + Arrays.toString( f.getFactorTable().getWeightsSparseUnsafe()));
                        System.out.println("Factor  " + f.getName() + " is directed? " + f.getFactorTable().isDirected());
                        System.out.println("Factor  " + f.getName() + " is conditional? " + f.getFactorTable().isConditional());
                        System.out.println("Factor  " + f.getName() + " is normalised? " + f.getFactorTable().isNormalized());
                    }
                }        
            }
                
        }
        
        return fg;
        
    }
    
    
    public static FactorGraph createStudentGraph() {
        
        FactorGraph student = new FactorGraph("StudentModel");
        
        Discrete difficulty = new Discrete("easy" , "hard");
        difficulty.setName("Difficulty");
        int[][] difficultyIdx = {
            {0},
            {1}
        };
        double[] difficultProb = {
            0.6 , 0.4
        };
        
        
        Discrete intelligence = new Discrete("dull" , "smart");
        intelligence.setName("Intelligence");
        int[][] intelligenceIdx = {
            {0},
            {1}
        };
        double[] intelligenceProb = {
            0.7,0.3
        };
        
        
        Discrete grade = new Discrete("A" , "B" , "C");
        grade.setName("Grade");
        int[][] gradeIdx = {
            {0,0,0},
            {0,0,1},
            {0,1,0},
            {0,1,1},
            {1,0,0},
            {1,0,1},
            {1,1,0},
            {1,1,1},
            {2,0,0},
            {2,0,1},
            {2,1,0},
            {2,1,1}
        };
        double[] gradeProb = {
            0.3d, 0.05d, 0.9d, 0.5d,
            0.4d, 0.25d, 0.08d, 0.3d,
            0.3d, 0.7d, 0.02d, 0.2d
        };
        
        
        Discrete sat = new Discrete("poor" , "good");
        sat.setName("SAT");
        int[][] satIdx = {
            {0, 0},
            {0, 1},
            {1, 0},
            {1, 1}
        };
        
        double[] satProb = {
            0.95d, 0.2d,
            0.05d, 0.8d
        };
        
        
        Discrete letter = new Discrete("weak" , "strong");
        letter.setName("Letter");
        int[][] letterIdx = {
            {0, 0},
            {0, 1},
            {0, 2},
            {1, 0},
            {1, 1},
            {1, 2},
        };
        double[] letterProb = {
            0.1d, 0.4d, 0.9d,
            0.9d, 0.6d, 0.1d
        };
        
        
        Factor prob_diff = student.addFactor(difficultyIdx, difficultProb, difficulty);
        prob_diff.setName("P(D)");
        prob_diff.setDirectedTo(difficulty);
        
        
        Factor prob_intel = student.addFactor(intelligenceIdx, intelligenceProb, intelligence);
        prob_intel.setName("P(I)");
        prob_intel.setDirectedTo(intelligence);
        
        
        Factor prob_grade = student.addFactor(gradeIdx, gradeProb, grade, intelligence, difficulty);
        prob_grade.setName("P(G | I, D)");
        prob_grade.setDirectedTo(grade);
        
        
        Factor prob_sat = student.addFactor(satIdx, satProb, sat, intelligence);
        prob_sat.setName("P(S | I)");
        prob_sat.setDirectedTo(sat);
        
        
        Factor prob_letter = student.addFactor(letterIdx, letterProb, letter, grade);
        prob_letter.setName("P(L | G)");
        prob_letter.setDirectedTo(letter);
        
        return student;
    }
    
    
    public static FactorGraph createTestGraph() {
        
        FactorGraph bn = new FactorGraph("P(X3 | X1, X2");
        
        Discrete var_X1 = new Discrete(0 , 1);
        var_X1.setName("X1");
        
        Discrete var_X2 = new Discrete(0 , 1);
        var_X2.setName("X2");
        
        Discrete var_X3 = new Discrete(0 , 1);
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
        p_X3_given_X1_X2.setName("P(X3 | X1, X2)");
        p_X3_given_X1_X2.getFactorTable().normalizeConditional();
        
        Factor p_X1 = bn.addFactor(idx_X1, dist_X1, var_X1);
        p_X1.setDirectedTo(var_X1);
        p_X1.setName("P(X1)");
        p_X1.getFactorTable().normalizeConditional();
        
        Factor p_X2 = bn.addFactor(idx_X2, dist_X2, var_X2);
        p_X2.setDirectedTo(var_X2);
        p_X2.setName("P(X2)");
        p_X2.getFactorTable().normalizeConditional();
        
        return bn;
    }
    
}