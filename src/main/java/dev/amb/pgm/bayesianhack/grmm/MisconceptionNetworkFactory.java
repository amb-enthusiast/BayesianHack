package dev.amb.pgm.bayesianhack.grmm;

import cc.mallet.grmm.inference.BruteForceInferencer;
import cc.mallet.grmm.inference.Inferencer;
import cc.mallet.grmm.inference.JunctionTreeInferencer;
import cc.mallet.grmm.inference.VariableElimination;
import cc.mallet.grmm.types.Assignment;
import cc.mallet.grmm.types.AssignmentIterator;
import cc.mallet.grmm.types.Factor;
import cc.mallet.grmm.types.FactorGraph;
import cc.mallet.grmm.types.UndirectedModel;
import cc.mallet.grmm.types.Variable;
import cc.mallet.util.MalletLogger;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author AntB
 */
public class MisconceptionNetworkFactory {
    // Want a pairwise MN for a image I (i, j) pixels
    // Simplifying assumptions:
    //      i)      Assume pixel potentials are same model, i.e. local pixel position is not explicitly modelled.  Therefore we use total of i*j instances of phi_pixel
    //      ii)     Model is a four-neighbour hood network for  pixel P_ij, links for P_i-1j , P_i+1j, P_ij-1, P_ij+1 neighbours.  At edges, only 3/2 links/corners use a 3 or 2 neighbour model
    //      iii)    All pairwise potentials are the same model, regardless of pixel pair location in image

    static final Logger logger = MalletLogger.getLogger("dev.amb.pgm.ocr.markovnet.MisconceptionNetworkFactory");
    
    public UndirectedModel createMisconceptionNetwork() {
        logger.setLevel(Level.INFO);

        UndirectedModel modelGraph = new UndirectedModel();
        
        Variable[] allVars = this.createVariables();
        
        
        // Now create the potentials
        // A-B Potential
        double[] affinityAB = {30.0d, 5.0d, 1.0d, 10.0d};
        
//        Variable[] pairAB = { modelGraph.getVariable(0) , modelGraph.getVariable(1) };
//        TableFactor potentialAB = new TableFactor( pairAB );
//        potentialAB.setValues(affinityAB);
//        modelGraph.addFactor(potentialAB);
        // This is equivalent to above
        modelGraph.addFactor(allVars[0], allVars[1], affinityAB);
        System.out.println("\nModel after AB potential added:\n" + modelGraph.prettyOutputString());

        // B-C TablePotential
        double[] affinityBC = {100.0d, 1.0d, 1.0d, 100.0d};
        modelGraph.addFactor(allVars[1], allVars[2], affinityBC);
        System.out.println("\nModel after AB & BC potential added:\n" + modelGraph.prettyOutputString());
        
        // C-D TablePotential
        double[] affinityCD = {1.0d, 100.0d, 100.0d, 1.0d};
        modelGraph.addFactor(allVars[2], allVars[3], affinityCD);
        System.out.println("\nModel after AB & BC & CD potential added:\n" + modelGraph.prettyOutputString());

        // D-A TablePotential
        double[] affinityDA = {100.0d, 1.0d, 1.0d, 100.0d};
        modelGraph.addFactor(allVars[0], allVars[3], affinityDA);
        System.out.println("\nModel after AB & BC & CD & DA potential added:\n" + modelGraph.prettyOutputString());


        return modelGraph;
    }

    public ArrayList<Double> marginalProbInferenceQuery(FactorGraph model) {
        logger.setLevel(Level.FINE);
        
        Inferencer inf = new VariableElimination();
        inf.computeMarginals(model);

        // now model contains marginal probs for each variable
        ArrayList<Double> probResults = new ArrayList<Double>();

        // Once you've run computeMarginals in an Inferencer, you can collect the marginals by calling the lookupMarginal method of the inferencer. This returns a Factor object that you can then query to find out what the marginals were. The easiest way to query a Factor object is using its assignmentIterator method. Here's how you can do that:
        int allVars = model.variablesSet().size();

        for (int varIdx = 0; varIdx < allVars; varIdx++) {

            Variable var = model.getVariable(varIdx);

            Factor varMarginalProbs = inf.lookupMarginal(var);

            AssignmentIterator it = varMarginalProbs.assignmentIterator();
            while (it.hasNext()) {
                int outcome = it.indexOfCurrentAssn ();
                System.out.println( "Index of current assignment in iterator = " + outcome);
                System.out.println(var + "_" + outcome + "   " + varMarginalProbs.value(it));
                probResults.add(varMarginalProbs.value(it));
                
                it.next();
            }
        
        }

        // Note that this just looks at single-variable marginals. Most inferencers compute marginals over pairs of variables as well. To get these, pick one of the Factor objects that lives in your factor graph. Then call the lookupMarginal(Factor) method on that factor.

        return probResults;
    }
    
    
    public Double jointAssignmentInferenceQuery(FactorGraph model, int[] outcomes) {
        logger.setLevel(Level.FINE);
        
        Inferencer inf = new BruteForceInferencer();
        // Requres call to compute marginals
        inf.computeMarginals(model);
        
        ArrayList<Variable> vars = new ArrayList<Variable>();
        for (Object varObj : model.variablesSet()) {
            vars.add( (Variable) varObj);
        }
                
        
        Assignment assignment = new Assignment(vars, outcomes);
        
        return inf.lookupJoint(assignment);
    }
    
    public Double jointAssignmentInferenceQuery(FactorGraph model, Variable[] vars,int[] outcomes) {
        logger.setLevel(Level.FINE);
        
        Inferencer inf = new JunctionTreeInferencer();
        // Requres call to compute marginals
        inf.computeMarginals(model);      
        
        Assignment assignment = new Assignment(vars, outcomes);
        
        return inf.query(model, assignment);
    }
    
    
    public Variable[] createVariables() {
        // create variables
        Variable varA = new Variable(2);
        varA.setLabel("A");
        
        Variable varB = new Variable(2);
        varB.setLabel("B");
        
        Variable varC = new Variable(2);
        varC.setLabel("C");
        
        Variable varD = new Variable(2);
        varD.setLabel("D");
        
        Variable[] allVars = {varA, varB, varC, varD};
        
        return allVars;
    }
}