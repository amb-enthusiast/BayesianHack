
package dev.amb.pgm.bayesianhack.dimple;


import com.analog.lyric.dimple.factorfunctions.core.FactorTable;
import com.analog.lyric.dimple.factorfunctions.core.FactorTableEntry;
import com.analog.lyric.dimple.factorfunctions.core.IFactorTable;
import com.analog.lyric.dimple.factorfunctions.core.IFactorTableIterator;
import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.domains.DiscreteDomain;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.Discrete;
import com.analog.lyric.dimple.model.variables.VariableBase;
import com.analog.lyric.dimple.solvers.sumproduct.SumProductSolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

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
        
        FactorGraph model = createStudentGraph();
        
        Factor f = model.getFactorByName("P(G | I, D)");
        
        removeAssignment(f);
        
    }
    
    
    
    /*
    
    Here is where we hack around with Dimple to learn more about probabilistic queries
    */
    
    
    public static void testSetEvidence() {
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
    
    
    public static void testJointProbability() {
        
        //testSetEvidence();
        
        FactorGraph student = createStudentGraph();
        
        // create an assignment
        HashMap<String, String> assignment = new HashMap<String, String>();
        assignment.put("Difficulty" , "hard");
        assignment.put("Intelligence" , "smart");
        assignment.put("SAT" , "good");
        assignment.put("Grade" , "B");
        assignment.put("Letter" , "strong");
        
        String probLabel = "P(";
        for(String k : assignment.keySet()) {
            probLabel = probLabel + " " + k + "=" + assignment.get(k);
        }
        Double probValue = probabilityOfJointAssignment(student, assignment);
        
        System.out.println(probLabel + ")=" + probValue);
        
        Double probTotal = null;
        
        for(String a : student.getVariableByName("Difficulty").asDiscreteVariable().getDiscreteDomain().getElements(new String[0])) {
            for(String b : student.getVariableByName("Intelligence").asDiscreteVariable().getDiscreteDomain().getElements(new String[0])) {
                for(String c : student.getVariableByName("SAT").asDiscreteVariable().getDiscreteDomain().getElements(new String[0])) {
                    for(String d : student.getVariableByName("Grade").asDiscreteVariable().getDiscreteDomain().getElements(new String[0])) {
                        for(String e : student.getVariableByName("Letter").asDiscreteVariable().getDiscreteDomain().getElements(new String[0])) {
                            HashMap<String, String> assign = new HashMap<String, String>();
                            assign.put("Difficulty" , a);
                            assign.put("Intelligence" , b);
                            assign.put("SAT" , c);
                            assign.put("Grade" , d);
                            assign.put("Letter" , e);

                            String assignLabel = "P(";
                            for(String k : assign.keySet()) {
                                assignLabel = assignLabel + " " + k + "=" + assign.get(k);
                            }
                            Double prob = probabilityOfJointAssignment(student, assign);
                            System.out.println(assignLabel + ") = " + prob);
                            
                            if(probTotal == null) {
                                probTotal = prob;
                            } else {
                                probTotal = probTotal + prob;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("\nSum of probabiltiies over all joint assignments = " + probTotal);
        
    }
    
    
    public static Double probabilityOfJointAssignment(FactorGraph model , HashMap<String, String> assignment) {
        if(model == null) {
            return null;
        }
        if(assignment == null) {
            return null;
        }
        if(model.getVariableCount() == 0) {
            return null;
        }
        if(model.getFactorCount() == 0) {
            return null;
        }
        if(assignment.isEmpty()) {
            return null;
        }
        if(model.getVariableCount() != assignment.size()) {
            return null;
        }
        
        // makes sure the variables match up
        boolean match = false;
        for(VariableBase v : model.getVariables()) {
            for(String varName : assignment.keySet()) {
        
                if(v.getName().equals(varName)) {
                    match = true;
                    break;
                }
            }
        }
        if(match == false) {
            return null;
        }
        
        
        // Split the model up into factors and their related variables
        HashMap<Factor, ArrayList<Discrete>> factorVariables = new HashMap<Factor, ArrayList<Discrete>>();
        
        for(Factor f : model.getFactors()) {
            
            ArrayList<Discrete> factorVars = new ArrayList<Discrete>();
            
            for(VariableBase v : f.getSiblings()) {
                factorVars.add(v.asDiscreteVariable());
            }
            factorVariables.put(f, factorVars);
            
        }
        
        
        // Now we know what vars are in each factor, we can get the correct index
        // look up the CPT weights, and then multiply up (well, take logs and add to avoid underflow)
        
        Double logProb = null;
        
        for(Factor f : factorVariables.keySet()) {
            
            
            String[] orderedValues = new String[f.getSiblingCount()];
            
            for(int idx = 0 ; idx < f.getSiblingCount(); idx++) {
                for(Discrete var : factorVariables.get(f)) {
                    
                    if(f.getSibling(idx).getName().equals(var.getName())) {
                        orderedValues[idx] = assignment.get(var.getName());
                        break;
                    }
                }
                
            }
            
            // now get the index for the assignment
            int jointIdx = f.getFactorTable().getDomainIndexer().jointIndexFromElements((Object[]) orderedValues);
            
            // now accumulate the probability value
            Double prob = f.getFactorTable().getWeightForJointIndex(jointIdx);
            if(logProb == null) {
                logProb = Math.log(prob);
            } else {
                logProb = logProb + Math.log(prob);
            }
        }
        
        return Math.exp(logProb);
    }
    
    
    public static Factor marginaliseVariables(Factor factor , Discrete... variablesToMarginaliseOut ) {
        
        if(factor == null) {
            return null;
        }
        if(variablesToMarginaliseOut == null) {
            return factor;
        }
        if(factor.getSiblingCount() == 0) {
            return null;
        }
        if(variablesToMarginaliseOut.length == 0) {
            return null;
        }
        if(variablesToMarginaliseOut.length > factor.getSiblingCount()) {
            //WARN that there are more vars to marginalise out than are in the model!
            return null;
        }
        
        HashSet<Discrete> modelVarsToMarginalise = new HashSet<Discrete>();
        HashSet<Discrete> modelVarsToKeep = new HashSet<Discrete>();
        
        // need to create a new factorTable that has the correct variables, and is
        // conditionally normalised against the correct variable
        
        
        
        for(Discrete marVar : variablesToMarginaliseOut) {
            for(VariableBase v : factor.getSiblings()) {
                Discrete modelVar = v.asDiscreteVariable();
                if(marVar.getName().equals(modelVar.getName()) || marVar.equals(modelVar)) {
                    modelVarsToMarginalise.add(modelVar);
                    break;
                } else {
                    modelVarsToKeep.add(modelVar);
                    break;
                }
            }
        }
        
        // reduce Factor to one over, having summed out and renormalised over vars to marginalise
        
        
        return null;
    }
    
    
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
    
    
    // Learn more about the indexing mechanisms in Dimple FactorTables
    public static void removeAssignment(Factor f) {
        if(f == null) {
            System.out.println("The input factor is null; fix and try again.");
            return;
        }
        
        if(f.getSiblingCount()== 0) {
            System.out.println("The input factor has 0 variables; fix and try again.");
            return;
        }
        
        // Just run through the factor table to see the entries
        
        IFactorTable ft = f.getFactorTable();
        
        IFactorTableIterator iterator1 = ft.fullIterator();
        
        System.out.println("Inspecting assignments in " + f.getName());
        
        while(iterator1.hasNext()) {
            FactorTableEntry entry = iterator1.next();
            
            System.out.println("\t" + "jointIdx = " + entry.jointIndex() + ", values = " + Arrays.toString(entry.values(new String[0])) + " Prob=" + ft.getWeightForJointIndex(entry.jointIndex()));
        }
        
        System.out.println("Creating a new FactorTable with domain 0 - the domain at idx=0, corresponding to variable @ idx=0.\nRemoved:" +
                Arrays.toString(ft.getDomainIndexer().get(0).getElements(new String[0])));
        
//        DiscreteDomain[] newDomains = new DiscreteDomain[f.getSiblingCount()-1];
//        for(int idx = 1; idx < f.getSiblingCount()-1;idx++) {
//            newDomains[idx] = ft.getDomainIndexer().get(idx);
//        }
        IFactorTable newFT = FactorTable.create(ft.getDomainIndexer().get(1) , ft.getDomainIndexer().get(2));
        
        // Now iterate over the assignments and exmaine the values before we set any!
        IFactorTableIterator iterator2 = newFT.fullIterator();
        while(iterator2.hasNext()) {
            FactorTableEntry entry = iterator2.next();
            System.out.println("\tidx=" + entry.jointIndex() + " " + Arrays.toString(entry.values(new String[0])) + " , value=" + newFT.getWeightForJointIndex(entry.jointIndex()));
        }
        
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