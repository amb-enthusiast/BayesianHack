package dev.amb.pgm.bayesianhack.grmm;


import cc.mallet.grmm.types.CPT;
import cc.mallet.grmm.types.DirectedModel;
import cc.mallet.grmm.types.TableFactor;

import java.util.ArrayList;


public class App_GrmmEM 
{
    public static void main( String[] args )
    {
//        ProbUtils.displayTestData();
        
        System.out.println("Now for some serious EM!\n");
        
        DirectedModel model = ProbUtils.createInitialModel();
        System.out.println("Initial model:\n" + model.dumpToString());
        
        // these stay the same, so we may as well re-use
        CPT pH = (CPT) model.getFactor(0);
        
        // Let's pull out the P(A | H) and P(B | H) factors, so we can overwrite in a loop
        
        for(int idx = 0; idx < 2000;idx++) {
            System.out.println("\niteration#" + (idx+1));
            TableFactor pH_AB = ProbUtils.calcProb_HgivenAB(model , ProbUtils.calcProb_AandB(model));
            
            ArrayList<TestDataInstance> rawData = DataUtils.createTestData();
            ArrayList<TestInstanceCount> countData = DataUtils.countDataFromInstances(rawData);

            TableFactor pA_H = ProbUtils.calcProb_updateExpectedAgivenH(pH_AB, countData);
            TableFactor pB_H = ProbUtils.calcProb_updateExpectedBgivenH(pH_AB, countData);
            TableFactor new_pH = ProbUtils.calcProb_updateExpectHgivenData(pH_AB, countData);
            model = ProbUtils.createModelFromFactors(new_pH, pA_H, pB_H);
            
        }
        
        System.out.println(model.getFactor(0).dumpToString());
        System.out.println(model.getFactor(1).dumpToString());
        System.out.println(model.getFactor(2).dumpToString());
    }
}
