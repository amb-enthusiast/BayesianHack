package dev.amb.pgm.bayesianhack.samiam;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.EliminationHeuristic;
import edu.ucla.belief.inference.HuginEngineGenerator;
import edu.ucla.belief.inference.JoinTreeSettings;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.io.PropertySuperintendent;
import edu.ucla.belief.learn.Learning;
import edu.ucla.belief.learn.LearningData;
import java.io.File;


/**
 *
 * @author AntB
 */
public class EMUtils {
    
    
    public static final int DEFAULT_MAX_ITERS = 25;
  
    
    public static BeliefNetwork doEM(String dataFilename , BeliefNetwork startNetwork, int maxIterations , String outputModelFilename) throws Exception {
        
        /*
         * Do soe sanity checks on input
         */
        if(outputModelFilename == null || outputModelFilename.isEmpty() == true) {
            throw new Exception("You provided a null or empty filename for the output model = need a proper file name.");
        }
        
        if(startNetwork == null || startNetwork.vertices().isEmpty() == true) {
            throw new Exception("You provided a null or empty initial BeliefNetwork - need a model with structure over variables and initial CPT estimates.");
        }
        
        if(dataFilename == null || dataFilename.isEmpty() == true) {
            throw new Exception("You provided a null or empty datasetFilename- need a proper dataset to perfrom EM");
        }
        
        if(maxIterations < 1) {
            System.out.println("WARN: You provided a maxIterations value < 1, so setting value to default of " + DEFAULT_MAX_ITERS);
            maxIterations = DEFAULT_MAX_ITERS;
        }
        
        boolean bias = true;
        
        
        // Setup the Hugin Inference engine Dynamator for BN
        HuginEngineGenerator dynamator = new edu.ucla.belief.inference.HuginEngineGenerator();        
        JoinTreeSettings settings = dynamator.getSettings( (PropertySuperintendent) startNetwork, true );        
        settings.setEliminationHeuristic( EliminationHeuristic.MIN_FILL );
        
        // Set the output filename
        Learning.renamePathForEmOutput(outputModelFilename);
        
        // Set a required loglikelihood threshold value, even though it doesnt seem to do anything...
        double defaultThreshold = 0.0000001d;
        
        
        // Set up dateset file
        LearningData data = null;
        
        File dataFile = null;
        try {
            dataFile = new File(App_SamiamEM.class.getClassLoader().getResource(dataFilename).toURI());
            if(dataFile.exists() == false) {

                dataFile = new File(dataFilename);

                if(dataFile.exists() == false) {
                    throw new Exception("Could not find the specified dataFilename [" + dataFilename + "]either in the classpath or on filesystem");
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println("INFO: Path to dataFile = " + dataFile.getAbsolutePath());
        
        File outFile = new File(outputModelFilename);
        
        /*
         * FIXME I couldn't get thresholding to work as expected by default, so I wrote my own iteration loop.  Bad news is it requires diskIO.
         * I sent an email tot he SamIam team, but no response (as of 18th March 2014).
         * */
        BeliefNetwork learnedNetwork = null;
        
        int currentIter;
        
        //run through every iteration until max iters
        for(currentIter = 0; currentIter <= maxIterations; currentIter++) {
            
            if(currentIter == 0) {
                
                data = new LearningData();
                data.readData(dataFile, startNetwork);        
        
                
                System.out.println("\tIteration#" + currentIter);
                Learning.learnParamsEM(startNetwork, data, defaultThreshold, maxIterations, dynamator, bias);
                NetworkIO.saveFileAs(startNetwork, outFile, outFile);
                
            } else {
                
                System.out.println("\tIteration#" + currentIter);

                learnedNetwork = NetworkIO.read(outFile);
                
                data = new LearningData();
                data.readData(dataFile, learnedNetwork);
                
                Learning.learnParamsEM(learnedNetwork, data, defaultThreshold, maxIterations, dynamator, bias);
                
                // now write to file, so it can be read in and updated in this loop...
                NetworkIO.saveFileAs(learnedNetwork, outFile, outFile);

            }
        }
        
        System.out.println("INFO: Final log-likelihood score = " + Learning.getLastLikelihood());
        
        System.out.println("INFO: Resultant learned model written to " + outFile.getAbsolutePath());

        if(learnedNetwork != null) {
            return learnedNetwork;
        } else {
            System.out.println("Why is the learnedModel object null at the end??");
            return NetworkIO.read(outFile);
        }
    }

}
