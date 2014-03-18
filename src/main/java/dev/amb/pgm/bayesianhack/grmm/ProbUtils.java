
package dev.amb.pgm.bayesianhack.grmm;


import cc.mallet.grmm.inference.BruteForceInferencer;
import cc.mallet.grmm.inference.Inferencer;
import cc.mallet.grmm.types.Assignment;
import cc.mallet.grmm.types.AssignmentIterator;
import cc.mallet.grmm.types.CPT;
import cc.mallet.grmm.types.DirectedModel;
import cc.mallet.grmm.types.Factor;
import cc.mallet.grmm.types.FactorGraph;
import cc.mallet.grmm.types.TableFactor;
import cc.mallet.grmm.types.VarSet;
import cc.mallet.grmm.types.Variable;
import cc.mallet.util.MalletLogger;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author AntB
 */
public class ProbUtils {
    
    
    // methods to help with factor set up and inference
    
    static final Logger logger = MalletLogger.getLogger("dev.amb.pgm.hiddenVar.FactorUtils");
    
    
    public static DirectedModel createModelFromFactors(TableFactor factorH , TableFactor factorA_H , TableFactor factorB_H) {
        
        DirectedModel model = new DirectedModel();
        
        Variable varH = factorH.getVariable(0);
        Variable varA = factorA_H.getVariable(0);
        Variable varB = factorB_H.getVariable(0);
        
        CPT pA_H = new CPT(factorA_H, varA);
        CPT pB_H = new CPT(factorB_H , varB);
        CPT pH = new CPT(factorH, varH);
        
        System.out.println("H=" + varH.getLabel() + ",A=" + varA.getLabel() + ", B=" + varB.getLabel() );
        
        model.addFactor(pH);
        model.addFactor(pA_H);
        model.addFactor(pB_H);
        
        return model;
    }
    // TODO take initial conditions data for dist
    public static DirectedModel createInitialModel() {
        
        DirectedModel model0 = new DirectedModel();
        
        logger.setLevel(Level.INFO);
        
        logger.log(Level.INFO, "Creating variables...");
        // Create3 binary variables, A and B (observed) and H (hidden)
        Variable varA = getVariableA();
        Variable varB = getVariableB();
        Variable varH = getVariableH();
        
        Variable[] allVars = {varA, varB, varH};
        
        
        logger.log(Level.INFO, "Creating distribution assignment probs...");
        // Now create potentials - we are in BN land, so CPDs
        
        //  H   P(H)
        //  0   0.6
        //  1   0.4
        double[] distH = {0.6d , 0.4d};
        
        //  A   H   P(A | H)
        //  0   0   0.39
        //  0   1   0.45
        //  1   0   0.61
        //  1   1   0.55
        double[] distA_H = { 0.39d , 0.45d , 0.61d , 0.55d };
        
        
        //  B   H   P(B | H)
        //  0   0   0.48
        //  0   1   0.57
        //  1   0   0.52
        //  1   1   0.43
        double[] distB_H = { 0.48d , 0.57d , 0.52d , 0.43d };
        
        
        // create factors
        Variable[] pairAH = {varA , varH};
        Variable[] pairBH = {varB , varH};
        Variable[] pairAB = {varA , varB};
        
        logger.log(Level.INFO, "Creating CPDs...");
        
        TableFactor factorH = new TableFactor(varH, distH);
        CPT pH = new CPT(factorH, varH);
        
        
        TableFactor factorAH = new TableFactor( pairAH , distA_H);
        CPT pA_H = new CPT(factorAH, varA);
        
        TableFactor factorBH = new TableFactor(pairBH, distB_H);
        CPT pB_H = new CPT(factorBH , varB);
        
        
        // add to directed graph model0
        logger.log(Level.INFO, "Creating model...");
//        System.out.println("INFO: Model var set = " + model0.variablesSet().size() + " , " + model0.varSet() );
        
        model0.addFactor(pH);
        
        model0.addFactor(pA_H);
        
        model0.addFactor(pB_H);
        
        // check results
        System.out.println("Created initial model::\n" + model0.prettyOutputString());       
        
        return model0;
        
    }

    
    /*
     * inference methods
     */
    
    public static Double calcProb_jointAssignment(FactorGraph model, Variable[] vars, int[] outcomes) {
        logger.setLevel(Level.FINE);
        
        if(vars.length != outcomes.length) {
            System.out.println("WARN: joint assignment error - specified number of vars != number of outcomes");
            return null;
            
        } else {
            Inferencer inf = new BruteForceInferencer();
            // Requres call to compute marginals
            inf.computeMarginals(model);

            Assignment assignment = new Assignment(vars, outcomes);

            Double prob = inf.lookupJoint(assignment);
            String assignDump = assignment.dumpToString();
            String[] assign = assignDump.split("\n");
            System.out.println("\t" + assign[1] + "(" + assign[2] + ")=" + prob);

            return prob;
        }
    }
    
    
    public static TableFactor calcProb_marginalForVar(FactorGraph model, Variable var) {
        logger.setLevel(Level.FINE);
        
        // Once you've run computeMarginals in an Inferencer, you can collect the marginals by calling the lookupMarginal method of the inferencer. This returns a Factor object that you can then query to find out what the marginals were. The easiest way to query a Factor object is using its assignmentIterator method. Here's how you can do that:
        Inferencer inf = new BruteForceInferencer();
        inf.computeMarginals(model);

        // now model contains marginal probs for each variable
        return (TableFactor) inf.lookupMarginal(var);
    }
    
    public static TableFactor calcProb_H(DirectedModel model) {
        return calcProb_marginalForVar(model, model.getVariable(0));
    }
    
    public static TableFactor calcProb_A(DirectedModel model) {
        return calcProb_marginalForVar(model, model.getVariable(1));
    }
    
    public static TableFactor calcProb_B(DirectedModel model) {
        return calcProb_marginalForVar(model, model.getVariable(2));
        
    }
    
    public static TableFactor calcProb_AandB(DirectedModel model) {
        
        // Once you've run computeMarginals in an Inferencer, you can collect the marginals by calling the lookupMarginal method of the inferencer. This returns a Factor object that you can then query to find out what the marginals were. The easiest way to query a Factor object is using its assignmentIterator method. Here's how you can do that:
        Inferencer inf = new BruteForceInferencer();
        inf.computeMarginals(model);

        VarSet vars = model.varSet();
        vars.remove(model.getVariable(0));
        
        return (TableFactor) inf.lookupMarginal(vars);
        
    }
    
    public static TableFactor calcProb_AandH(DirectedModel model) {
        
        // Once you've run computeMarginals in an Inferencer, you can collect the marginals by calling the lookupMarginal method of the inferencer. This returns a Factor object that you can then query to find out what the marginals were. The easiest way to query a Factor object is using its assignmentIterator method. Here's how you can do that:
        Inferencer inf = new BruteForceInferencer();
        inf.computeMarginals(model);

        VarSet vars = model.varSet();
        vars.remove(model.getVariable(2));
        
        return (TableFactor) inf.lookupMarginal(vars);
        
    }
    
    public static TableFactor calcProb_BandH(DirectedModel model) {
        
        // Once you've run computeMarginals in an Inferencer, you can collect the marginals by calling the lookupMarginal method of the inferencer. This returns a Factor object that you can then query to find out what the marginals were. The easiest way to query a Factor object is using its assignmentIterator method. Here's how you can do that:
        Inferencer inf = new BruteForceInferencer();
        inf.computeMarginals(model);

        VarSet vars = model.varSet();
        vars.remove(model.getVariable(1));
        
        return (TableFactor) inf.lookupMarginal(vars);
        
    }
    
    public static TableFactor calcProb_AgivenH(TableFactor pH , DirectedModel model)  {
        TableFactor tf = (TableFactor) model.asTable();
        Variable[] vars = { tf.getVariable(0) , tf.getVariable(2)};
        tf = (TableFactor) tf.marginalize(vars );
        tf.divideBy(pH);
        return tf;
    }
    
    public static TableFactor calcProb_BgivenH(TableFactor pH , DirectedModel model)  {
        TableFactor tf = (TableFactor) model.asTable();
        Variable[] vars = { tf.getVariable(0) , tf.getVariable(1)};
        tf = (TableFactor) tf.marginalize(vars );
        tf.divideBy(pH);
        return tf;
    }
    
    public static TableFactor calcProb_HgivenAB(DirectedModel model , TableFactor pAB) {
        TableFactor tf = (TableFactor) model.asTable();
        tf.divideBy(pAB);
        return tf;
    }
    
    public static TableFactor calcProb_updateExpectedAgivenH(Factor pH_AB, ArrayList<TestInstanceCount> countData) {
        
        // this is where data meets distribution!
        
        long sum = 0;
        
        long count_A0B0 = 0;
        long count_A0B1 = 0;
        long count_A1B0 = 0;
        long count_A1B1 = 0;
        
        for(TestInstanceCount count : countData) {
            
            sum = sum + count.getAssignmentCount();
            
            if(count.getAssignmentVarA() == 0 && count.getAssignmentVarB() == 0) {
                count_A0B0 = count.getAssignmentCount();
            } else if(count.getAssignmentVarA() == 0 && count.getAssignmentVarB() == 1) {
                count_A0B1 = count.getAssignmentCount();
            } else if(count.getAssignmentVarA() == 1 && count.getAssignmentVarB() == 0) {
                count_A1B0 = count.getAssignmentCount();
            } else if(count.getAssignmentVarA() == 1 && count.getAssignmentVarB() == 1) {
                count_A1B1 = count.getAssignmentCount();
            }
            
        }
        
        Variable varA = null;
        Variable varB= null;
        Variable varH = null;
        
        if(pH_AB.getVariable(0).getLabel().equals("A")){
            varA = pH_AB.getVariable(0);
        } else if(pH_AB.getVariable(0).getLabel().equals("B")) {
            varB = pH_AB.getVariable(0);
        } else if(pH_AB.getVariable(0).getLabel().equals("H")) {
            varH = pH_AB.getVariable(0); 
        }
        
        if(pH_AB.getVariable(1).getLabel().equals("A")){
            varA = pH_AB.getVariable(1);
        } else if(pH_AB.getVariable(1).getLabel().equals("B")) {
            varB = pH_AB.getVariable(1);
        } else if(pH_AB.getVariable(1).getLabel().equals("H")) {
            varH = pH_AB.getVariable(1); 
        }
        
        if(pH_AB.getVariable(2).getLabel().equals("A")){
            varA = pH_AB.getVariable(2);
        } else if(pH_AB.getVariable(2).getLabel().equals("B")) {
            varB = pH_AB.getVariable(2);
        } else if(pH_AB.getVariable(2).getLabel().equals("H")) {
            varH = pH_AB.getVariable(2); 
        }
        
        if(varA == null) {
            System.out.println("WARN: wha wha wha... input CPD doesnt contain A variable!");
        }
        if(varB == null) {
            System.out.println("WARN: wha wha wha... input CPD doesnt contain B variable!");
        }
        if(varH == null) {
            System.out.println("WARN: wha wha wha... input CPD doesnt contain B variable!");
        }
        
        
        double pH0_A0B0 = -1d;
        double pH1_A0B0 = -1d;

        double pH0_A0B1 = -1d;
        double pH1_A0B1 = -1d;

        double pH0_A1B0 = -1d;
        double pH1_A1B0 = -1d;

        double pH0_A1B1 = -1d;
        double pH1_A1B1 = -1d;
            
        
        AssignmentIterator it = pH_AB.assignmentIterator();
            
        while(it.hasNext() == true) {
            
            int idx = it.indexOfCurrentAssn();
            
            Assignment assign = it.assignment();
            
            int outcomeA = assign.getColumnInt(varA)[0];
            int outcomeB = assign.getColumnInt(varB)[0];
            
            // get the assignment, check if it equals 00,01, 10, or 11 case and multiply out.
                
            if(outcomeA == 0 && outcomeB == 0) {
                pH0_A0B0 = pH_AB.value(it);
                it.next();
                pH1_A0B0 = pH_AB.value(it);
                
            } else if(outcomeA == 0 && outcomeB == 1){
                pH0_A0B1 = pH_AB.value(it);
                it.next();
                pH1_A0B1 = pH_AB.value(it);
                
            } else if(outcomeA == 1 && outcomeB == 0){
                pH0_A1B0 = pH_AB.value(it);
                it.next();
                pH1_A1B0 = pH_AB.value(it);
                
            } else if(outcomeA == 1 && outcomeB == 1){
                pH0_A1B1 = pH_AB.value(it);
                it.next();
                pH1_A1B1 = pH_AB.value(it);
                
            } else {
                System.out.println("WARN: Wierd assignment: " + assign.dumpToString());
            }
            
            it.next();
        }
        
        // for A=0, I need :
        //          (H=0, A=0, B=0) and (H=1, A=0, B=1) from P(H | A, B)
        //          (H=1, A=0, B=0) and (H=0, A=0, B=1) from P(H | A, B)
        
        // for A=1, I need :
        //          (H=0, A=0, B=0) and (H=1, A=0, B=1) from P(H | A, B)
        //          (H=0, A=1, B=0) and (H=1, A=1, B=1) from P(H | A, B)
        
        
        // new P(A | H) = countAB*P(A | H)*P(B | H) - consider H0,H1 and B0,B1 cases
        
        double newPA0_H0 = (count_A0B0 * pH0_A0B0 + count_A0B1 * pH0_A0B1) / sum;
        double newPA0_H1 = (count_A0B0 * pH1_A0B0 + count_A0B1 * pH1_A0B1) / sum;
        double newPA1_H0 = (count_A1B0 * pH0_A1B0 + count_A1B1 * pH0_A1B1) / sum;
        double newPA1_H1 = (count_A1B0 * pH1_A1B0 + count_A1B1 * pH1_A1B1) / sum;
        
        double[] newValues = {newPA0_H0 , newPA0_H1 , newPA1_H0 , newPA1_H1};
        
        Variable[] varsAH = {varA , varH};
        TableFactor new_pA_H = new TableFactor(varsAH);
        new_pA_H.setValues(newValues);
        
        Factor pH = new_pA_H.marginalizeOut(varA);
        new_pA_H.divideBy(pH);
        
        return new_pA_H;
    }
    
    
    public static TableFactor calcProb_updateExpectedBgivenH(Factor pH_AB, ArrayList<TestInstanceCount> countData) {
        
        // this is where data meets distribution!
        
        long sum = 0;
        
        long count_A0B0 = 0;
        long count_A0B1 = 0;
        long count_A1B0 = 0;
        long count_A1B1 = 0;
        
        for(TestInstanceCount count : countData) {
            
            sum = sum + count.getAssignmentCount();
            
            if(count.getAssignmentVarA() == 0 && count.getAssignmentVarB() == 0) {
                count_A0B0 = count.getAssignmentCount();
            } else if(count.getAssignmentVarA() == 0 && count.getAssignmentVarB() == 1) {
                count_A0B1 = count.getAssignmentCount();
            } else if(count.getAssignmentVarA() == 1 && count.getAssignmentVarB() == 0) {
                count_A1B0 = count.getAssignmentCount();
            } else if(count.getAssignmentVarA() == 1 && count.getAssignmentVarB() == 1) {
                count_A1B1 = count.getAssignmentCount();
            }
            
        }
        
        Variable varA = null;
        Variable varB= null;
        Variable varH = null;
        
        if(pH_AB.getVariable(0).getLabel().equals("A")){
            varA = pH_AB.getVariable(0);
        } else if(pH_AB.getVariable(0).getLabel().equals("B")) {
            varB = pH_AB.getVariable(0);
        } else if(pH_AB.getVariable(0).getLabel().equals("H")) {
            varH = pH_AB.getVariable(0); 
        }
        
        if(pH_AB.getVariable(1).getLabel().equals("A")){
            varA = pH_AB.getVariable(1);
        } else if(pH_AB.getVariable(1).getLabel().equals("B")) {
            varB = pH_AB.getVariable(1);
        } else if(pH_AB.getVariable(1).getLabel().equals("H")) {
            varH = pH_AB.getVariable(1); 
        }
        
        if(pH_AB.getVariable(2).getLabel().equals("A")){
            varA = pH_AB.getVariable(2);
        } else if(pH_AB.getVariable(2).getLabel().equals("B")) {
            varB = pH_AB.getVariable(2);
        } else if(pH_AB.getVariable(2).getLabel().equals("H")) {
            varH = pH_AB.getVariable(2); 
        }
        
        if(varA == null) {
            System.out.println("WARN: wha wha wha... input CPD doesnt contain A variable!");
        }
        if(varB == null) {
            System.out.println("WARN: wha wha wha... input CPD doesnt contain B variable!");
        }
        if(varH == null) {
            System.out.println("WARN: wha wha wha... input CPD doesnt contain H variable!");
        }
        
        double pH0_A0B0 = -1d;
        double pH1_A0B0 = -1d;

        double pH0_A0B1 = -1d;
        double pH1_A0B1 = -1d;

        double pH0_A1B0 = -1d;
        double pH1_A1B0 = -1d;

        double pH0_A1B1 = -1d;
        double pH1_A1B1 = -1d;
            
        
        AssignmentIterator it = pH_AB.assignmentIterator();
            
        while(it.hasNext() == true) {
            
            int idx = it.indexOfCurrentAssn();
            
            Assignment assign = it.assignment();
//            System.out.println("INFO: Assign column int for A,B: " + assign.getColumnInt(varA)[0] + " , " + assign.getColumnInt(varB)[0]);
            
            int outcomeA = assign.getColumnInt(varA)[0];
            int outcomeB = assign.getColumnInt(varB)[0];
            
            // get the assignment, check if it equals 00,01, 10, or 11 case and multiply out.
                
            if(outcomeA == 0 && outcomeB == 0) {
                pH0_A0B0 = pH_AB.value(it);
                it.next();
                pH1_A0B0 = pH_AB.value(it);
                
            } else if(outcomeA == 0 && outcomeB == 1){
                pH0_A0B1 = pH_AB.value(it);
                it.next();
                pH1_A0B1 = pH_AB.value(it);
                
            } else if(outcomeA == 1 && outcomeB == 0){
                pH0_A1B0 = pH_AB.value(it);
                it.next();
                pH1_A1B0 = pH_AB.value(it);
                
            } else if(outcomeA == 1 && outcomeB == 1){
                pH0_A1B1 = pH_AB.value(it);
                it.next();
                pH1_A1B1 = pH_AB.value(it);
                
            } else {
                System.out.println("WARN: Wierd assignment: " + assign.dumpToString());
            }
            
            it.next();
        }
        
        // for B=0, I need :
        //          (H=0, A=0, B=0) and (H=1, A=1, B=0) from P(H | A, B)
        //          (H=1, A=0, B=0) and (H=0, A=1, B=0) from P(H | A, B)
        
        // for B=1, I need :
        //          (H=0, A=0, B=1) and (H=0, A=1, B=1) from P(H | A, B)
        //          (H=1, A=0, B=1) and (H=1, A=1, B=1) from P(H | A, B)
        
        
        // new P(A | H) = count*P(A | H)*P(B | H)
        
        double newPB0_H0 = (count_A0B0 * pH0_A0B0 + count_A1B0 * pH0_A1B0) / sum;
        double newPB0_H1 = (count_A0B0 * pH1_A0B0 + count_A1B0 * pH1_A1B0) / sum;
        double newPB1_H0 = (count_A0B1 * pH0_A0B1 + count_A1B1 * pH0_A1B1) / sum;
        double newPB1_H1 = (count_A0B1 * pH1_A0B1 + count_A1B1 * pH1_A1B1) / sum;
        
        double[] newValues = {newPB0_H0 , newPB0_H1 , newPB1_H0 , newPB1_H1};
        
        Variable[] varsBH = {varB , varH};
        TableFactor new_pB_H = new TableFactor(varsBH);
        new_pB_H.setValues(newValues);
        
        Factor pH = new_pB_H.marginalizeOut(varB);
        
        new_pB_H.divideBy(pH);
//        System.out.println("INFO: Updated P(A | H):\n" + new_pA_H.dumpToString());
        
        return new_pB_H;
    }
        
    public static TableFactor calcProb_updateExpectHgivenData(Factor pH_AB, ArrayList<TestInstanceCount> countData) {
        
        long sum = 0;
        
        long count_A0B0 = 0;
        long count_A0B1 = 0;
        long count_A1B0 = 0;
        long count_A1B1 = 0;
        
        for(TestInstanceCount count : countData) {
            
            sum = sum + count.getAssignmentCount();
            
            if(count.getAssignmentVarA() == 0 && count.getAssignmentVarB() == 0) {
                count_A0B0 = count.getAssignmentCount();
            } else if(count.getAssignmentVarA() == 0 && count.getAssignmentVarB() == 1) {
                count_A0B1 = count.getAssignmentCount();
            } else if(count.getAssignmentVarA() == 1 && count.getAssignmentVarB() == 0) {
                count_A1B0 = count.getAssignmentCount();
            } else if(count.getAssignmentVarA() == 1 && count.getAssignmentVarB() == 1) {
                count_A1B1 = count.getAssignmentCount();
            }   
        }
        
        
        Variable varA = null;
        Variable varB= null;
        Variable varH = null;
        
        if(pH_AB.getVariable(0).getLabel().equals("A")){
            varA = pH_AB.getVariable(0);
        } else if(pH_AB.getVariable(0).getLabel().equals("B")) {
            varB = pH_AB.getVariable(0);
        } else if(pH_AB.getVariable(0).getLabel().equals("H")) {
            varH = pH_AB.getVariable(0); 
        }
        
        if(pH_AB.getVariable(1).getLabel().equals("A")){
            varA = pH_AB.getVariable(1);
        } else if(pH_AB.getVariable(1).getLabel().equals("B")) {
            varB = pH_AB.getVariable(1);
        } else if(pH_AB.getVariable(1).getLabel().equals("H")) {
            varH = pH_AB.getVariable(1); 
        }
        
        if(pH_AB.getVariable(2).getLabel().equals("A")){
            varA = pH_AB.getVariable(2);
        } else if(pH_AB.getVariable(2).getLabel().equals("B")) {
            varB = pH_AB.getVariable(2);
        } else if(pH_AB.getVariable(2).getLabel().equals("H")) {
            varH = pH_AB.getVariable(2); 
        }
        
        if(varA == null) {
            System.out.println("WARN: wha wha wha... input CPD doesnt contain A variable!");
        }
        if(varB == null) {
            System.out.println("WARN: wha wha wha... input CPD doesnt contain B variable!");
        }
        if(varH == null) {
            System.out.println("WARN: wha wha wha... input CPD doesnt contain H variable!");
        }
        
        // for H=0, I need :
        //          (H=0, A=0, B=0) and (H=0, A=0, B=1) and P(H=0, A=1, B=0) and P(H=0, A=1, B=1) from P(H | A, B)
        
        // for H=1, I need :
        //          (H=1, A=0, B=0) and (H=1, A=0, B=1) and P(H=1, A=1, B=0) and P(H=1, A=1, B=1) from P(H | A, B)        
        double pH0_A0B0 = -1d;
        double pH1_A0B0 = -1d;

        double pH0_A0B1 = -1d;
        double pH1_A0B1 = -1d;

        double pH0_A1B0 = -1d;
        double pH1_A1B0 = -1d;

        double pH0_A1B1 = -1d;
        double pH1_A1B1 = -1d;
            
        
        AssignmentIterator it = pH_AB.assignmentIterator();
            
        while(it.hasNext() == true) {
            
            int idx = it.indexOfCurrentAssn();
            
            Assignment assign = it.assignment();
//            System.out.println("INFO: Assign column int for A,B: " + assign.getColumnInt(varA)[0] + " , " + assign.getColumnInt(varB)[0]);
            
            int outcomeA = assign.getColumnInt(varA)[0];
            int outcomeB = assign.getColumnInt(varB)[0];
            
            // get the assignment, check if it equals 00,01, 10, or 11 case and multiply out.
                
            if(outcomeA == 0 && outcomeB == 0) {
                pH0_A0B0 = pH_AB.value(it);
                it.next();
                pH1_A0B0 = pH_AB.value(it);
                
            } else if(outcomeA == 0 && outcomeB == 1){
                pH0_A0B1 = pH_AB.value(it);
                it.next();
                pH1_A0B1 = pH_AB.value(it);
                
            } else if(outcomeA == 1 && outcomeB == 0){
                pH0_A1B0 = pH_AB.value(it);
                it.next();
                pH1_A1B0 = pH_AB.value(it);
                
            } else if(outcomeA == 1 && outcomeB == 1){
                pH0_A1B1 = pH_AB.value(it);
                it.next();
                pH1_A1B1 = pH_AB.value(it);
                
            } else {
                System.out.println("WARN: Wierd assignment: " + assign.dumpToString());
            }
            
            it.next();
        }
        
        
        double newPH0 = (count_A0B0 * pH0_A0B0 + count_A0B1 * pH0_A0B1 + count_A1B0 * pH0_A1B0 + count_A1B1 * pH0_A1B1) / sum;
        double newPH1 = (count_A0B0 * pH1_A0B0 + count_A0B1 * pH1_A0B1 + count_A1B0 * pH1_A1B0 + count_A1B1 * pH1_A1B1) / sum;
        
        Variable[] varsH = { varH};
        TableFactor new_pH = new TableFactor(varsH);
        double[] values = {newPH0 , newPH1};
        new_pH.setValues( values);
        
        return new_pH;
    }
    /*
     * 
P(H | A, B)      H=0                         H=1
A=0,B=0         0.5226130653	0.4773869347
A=0,B=1         0.6112115732	0.3887884268
A=1,B=0         0.5834994022	0.4165005978
A=1,B=1         0.667976976	0.332023024

     */
    
    // variable methods
    
    public static Variable getVariableA() {
        Variable varA = new Variable(2);
        varA.setLabel("A");
        
        return varA;
    }
    
    public static Variable getVariableB() {
        Variable varB = new Variable(2);
        varB.setLabel("B");
        
        return varB;
    }
    
    public static Variable getVariableH() {
        Variable varH = new Variable(2);
        varH.setLabel("H");
        
        return varH;
    }
    
    
    public static void displayTestData() {
        
        DirectedModel model = createInitialModel();
//        FactorUtils.marginalProbInferenceQuery(model);
        
        Variable[] vars = model.varSet().toVariableArray();
        for(Variable v : vars) {
                System.out.println("\tModel var: " + v.toString());
        }
        
        
        //A=0, B=0, H=0
        int[] outcomes1 = {0 , 0 , 0};
        calcProb_jointAssignment(model, vars, outcomes1);
        
        //A=0, B=0, H=1
        int[] outcomes2 = {0 , 0 , 1};
        calcProb_jointAssignment(model, vars, outcomes2);
        
        //A=0, B=1, H=0
        int[] outcomes3 = {0 , 1 , 0};
        calcProb_jointAssignment(model, vars, outcomes3);
        
        //A=0, B=1, H=1
        int[] outcomes4 = {0 , 1, 1 };
        calcProb_jointAssignment(model, vars, outcomes4);
        
        //A=1, B=0, H=0
        int[] outcomes5 = {1 , 0 , 0};
        calcProb_jointAssignment(model, vars, outcomes5);
        
        //A=1, B=0, H=1
        int[] outcomes6 = {1 , 0 , 1};
        calcProb_jointAssignment(model, vars, outcomes6);
        
        //A=1, B=1, H=0
        int[] outcomes7 = {1 , 1 , 0};
        calcProb_jointAssignment(model, vars, outcomes7);
        
        //A=0, B=0, H=0
        int[] outcomes8 = {1 , 1 , 1};
        calcProb_jointAssignment(model, vars, outcomes8);
        
        /*
         * Manual results for theta0:
            A   B   H	P(A , B , H)
            0   0   0	0.11232
            0   0   1	0.1026
            0   1   0	0.12168
            0   1   1	0.0774
            1   0   0	0.17568
            1   0   1	0.1254
            1   1   0	0.19032
            1   1   1	0.0946
         */
        System.out.println("INFO: Sum of initial model = " + model.sum());
        
        // create data
        ArrayList<TestDataInstance> rawData = DataUtils.createTestData();
        ArrayList<TestInstanceCount> countData = DataUtils.countDataFromInstances(rawData);
        System.out.println("\nExample data::");
        for(TestInstanceCount count : countData) {
            System.out.println("\t" + count.toString());
        }
        
        System.out.println("INFO: Check probability queries...");
        System.out.println("INFO: P(H)");
        TableFactor pH = calcProb_H(model);
        System.out.println(pH.dumpToString());
        
        System.out.println("INFO: P(A)");
        TableFactor pA = calcProb_A(model);
        System.out.println(pA.dumpToString());
        
        System.out.println("INFO: P(A, B)");
        TableFactor pAB = calcProb_AandB(model);
        System.out.println(pAB.dumpToString());
        
        
        System.out.println("\nINFO: P(A , H)");
        TableFactor pAH = calcProb_AandH(model);
        System.out.println(pAH.dumpToString());
        
        System.out.println("\nINFO: Calculated P(A | H)");
        TableFactor pA_H = calcProb_AgivenH(pH, model);
        System.out.println(pA_H.dumpToString());
        
        
        System.out.println("\nINFO: P(B)");
        TableFactor pB= calcProb_B(model);
        System.out.println(pB.dumpToString());

        System.out.println("\nINFO: P(B , H)");
        TableFactor pBH = calcProb_BandH(model);
        System.out.println(pBH.dumpToString());
        
        System.out.println("\nINFO: Calculated P(B | H)");
        TableFactor pB_H = calcProb_BgivenH(pH, model);
        System.out.println(pA_H.dumpToString());
        
        
        System.out.println("\nINFO: Calculated P(H | A , B)");
        TableFactor pH_AB = calcProb_HgivenAB(model , pAB);
        System.out.println(pH_AB.dumpToString());
        
        
        System.out.println("\nINFO: Updated estimated P(H | A)");
        System.out.println(calcProb_updateExpectedAgivenH(pH_AB, countData));
        
        System.out.println("\nINFO: Updated estimated P(H | B)");
        System.out.println(calcProb_updateExpectedBgivenH(pH_AB, countData).dumpToString());
        
    }
}
