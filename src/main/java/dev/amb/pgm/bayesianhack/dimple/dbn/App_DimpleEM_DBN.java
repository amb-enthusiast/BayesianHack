
package dev.amb.pgm.bayesianhack.dimple.dbn;

import com.analog.lyric.dimple.factorfunctions.core.FactorTable;
import com.analog.lyric.dimple.factorfunctions.core.IFactorTable;
import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.Discrete;
import com.analog.lyric.dimple.solvers.sumproduct.SumProductSolver;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author AntB
 */
public class App_DimpleEM_DBN {
    
    public static void main(String[] args) {
        
        /*
        
        Assume a known structure for the DBN (initial and transition models) over X1, X2, X3 and X4 variables.
        
        Use EM to learn t0 model using S_initial structure and initial values from N data sequences
        Use EM to learn (t+1 | t) model using S_trans structure from N data sequences, over { 0=>1, 1=>2 , ... , Nseq-1=>Nseq } observations
        
        This means:
        1) do EM for S_initial CPTs on a dataset created from the first rows of N sequences
        2) do EM for S_trans CPTs on N datasets, without artifical "stitching together"
        
        
        // Known initial structure:
            X1_0  X2_0  X3_0  X4_0 (0 edges)
            Learn P(X1_0) , P(X2_0) , P(X3_0) , P(X4_0)
        
        // Known transition structure:
        
                t                 t+1
        
                        X1_t+1          X2_t+1
        
                            \\          //
                             _|        |_

                                 X3_t+1
        
                                   ||
                                   \/
        
            X4_t   ===========>  X4_t+1
        (4 edges, 1 inter-timeslice edge)
        
        Learn:
        P(X1_t) , P(X2_t) , P(X3_t) , P(X4_t | X3_t , X4_t_1 )
        */
        
        
        // Set path to training data folder
        final String traininDataFolder = App_DimpleEM_DBN.class.getResource("/dbnTrainingData").getFile();
        
        
        
        // Create initial variables
        Discrete var_X1_0 = new Discrete(new String[] { "0" , "1"});
        var_X1_0.setName("X1_t0");
        
        Discrete var_X2_0 = new Discrete(new String[] { "0" , "1"});
        var_X2_0.setName("X2_t0");
        
        Discrete var_X3_0 = new Discrete(new String[] { "0" , "1"});
        var_X3_0.setName("X3_t0");
        
        Discrete var_X4_0 = new Discrete(new String[] { "0" , "1"});
        var_X4_0.setName("X4_t0");
        
        
        // Create CPTs for t0
        IFactorTable tf_X1_0 = createInitialBinaryCPT(var_X1_0);
        IFactorTable tf_X2_0 = createInitialBinaryCPT(var_X2_0);
        IFactorTable tf_X3_0 = createInitialBinaryCPT(var_X3_0);
        IFactorTable tf_X4_0 = createInitialBinaryCPT(var_X4_0);
        
        IFactorTable[] initialTables = {
            tf_X1_0, tf_X2_0, tf_X3_0, tf_X4_0
        };
        
        
        
        // Create transition vars
        Discrete var_X1_t = new Discrete(new String[] { "0" , "1"});
        var_X1_t.setName("X1_t");
        
        Discrete var_X2_t = new Discrete(new String[] { "0" , "1"});
        var_X2_t.setName("X2_t");
        
        Discrete var_X3_t = new Discrete(new String[] { "0" , "1"});
        var_X3_t.setName("X3_t");
        
        Discrete var_X4_t_1 = new Discrete(new String[] { "0" , "1"});
        var_X4_t_1.setName("X4_t-1");
        
        Discrete var_X4_t = new Discrete(new String[] { "0" , "1"});
        var_X4_t.setName("X4_t");
        
        
        // Create CPTs for t | t-1
        IFactorTable tf_X1_t = createInitialBinaryCPT(var_X1_t);
        IFactorTable tf_X2_t = createInitialBinaryCPT(var_X2_t);
        IFactorTable tf_X3_t = createInitialBinaryCPT(var_X3_t);
        IFactorTable tf_X4_t = createInitialTransitionModel_X4(var_X4_t, var_X3_t, var_X4_t_1);
        
        IFactorTable[] transitionTables = {
            tf_X1_t, tf_X2_t, tf_X3_t, tf_X4_t
        };
        
        
        
        System.out.println("Default Initial model probabilities::");
        for(IFactorTable table : initialTables) {
            System.out.println("\tProbs = "+ Arrays.toString(table.getWeightsSparseUnsafe()));
        }

        
        // Now grab the data
        
        ArrayList<File> dataFiles = getTrainingDataFromFolder(traininDataFolder);
        
        // Set up training data for initial model
        ArrayList<String[]> initialObservations = getInitialCsvData(dataFiles);
        

        // Do EM on initial probs
        FactorGraph initialModel = new FactorGraph();
        initialModel.setName("learning_initialModel");
        
        for(int idx = 0; idx < initialObservations.size(); idx++) {
            
            // create vars, add factors and set evidence from observations
            Discrete var_X1 = new Discrete( new String[] {"0" , "1"} );
            Discrete var_X2 = new Discrete( new String[] {"0" , "1"} );
            Discrete var_X3 = new Discrete( new String[] {"0" , "1"} );
            Discrete var_X4 = new Discrete( new String[] {"0" , "1"} );
            
            Factor prob_X1 = initialModel.addFactor(tf_X1_0, var_X1);
            prob_X1.setDirectedTo(var_X1);
            
            Factor prob_X2 = initialModel.addFactor(tf_X2_0, var_X2);
            prob_X2.setDirectedTo(var_X2);
            
            Factor prob_X3 = initialModel.addFactor(tf_X3_0, var_X3);
            prob_X3.setDirectedTo(var_X3);
            
            Factor prob_X4 = initialModel.addFactor(tf_X4_0, var_X4);
            prob_X4.setDirectedTo(var_X4);
            
            
            String[] obs = initialObservations.get(idx);
            
            String value_X1 = obs[0];
            String value_X2 = obs[1];
            String value_X3 = obs[2];
            String value_X4 = obs[3];
            
            System.out.println("\t\tInitialObs_" + (idx+1) + ":: X1="+value_X1 + " , X2=" + value_X2 + " , X3=" + value_X3 + " , X4=" + value_X4);
            
            // Set X1 evidence, if observed value
            if(value_X1.equals("0")) {
                var_X1.setFixedValue("0");
            } else if(value_X1.equals("1")) {
                var_X1.setFixedValue("1");
            }
            
            // Set X2 evidence, if observed value
            if(value_X2.equals("0")) {
                var_X2.setFixedValue("0");
            } else if(value_X2.equals("1")) {
                var_X2.setFixedValue("1");
            }
            
            // Set X3 evidence, if observed value
            if(value_X3.equals("0")) {
                var_X3.setFixedValue("0");
            } else if(value_X3.equals("1")) {
                var_X3.setFixedValue("1");
            }
            
            // Set X4 evidence, if observed value
            if(value_X4.equals("0")) {
                var_X4.setFixedValue("0");
            } else if(value_X4.equals("1")) {
                var_X4.setFixedValue("1");
            }
            
        }
        
        System.out.println("\n\nNow running EM over the initial conditions factorGraph");
        initialModel.setSolverFactory(new SumProductSolver());
        
        initialModel.baumWelch(initialTables, 2, 25);
        
        System.out.println("EM for initial model complete");
        initialObservations = null;
        
        
        System.out.println("\nLearned Initial model probabilities::");
        for(IFactorTable table : initialTables) {
            System.out.println("\tProbs = "+ Arrays.toString(table.getWeightsSparseUnsafe()));
        }
        
        
        
        // Now do EM over the sequence data (!)
        // Set up training data for transition model
        ArrayList<ArrayList<String>> sequenceObservations = getTransitionCsvDatasets(dataFiles);
        
        
        System.out.println("\n\nNow perform EM for transition model");
        
        System.out.println("\nDefault Transition model probabilities::");
        for(IFactorTable table : transitionTables) {
            System.out.println("\tProbs = "+ Arrays.toString(table.getWeightsSparseUnsafe()));
        }
        
        initialModel = null;
        
        
        FactorGraph sequenceModel = new FactorGraph();
        sequenceModel.setName("learning_sequenceModel");
        
        for(int datasetIdx = 0; datasetIdx < sequenceObservations.size() ; datasetIdx++) {
            
            ArrayList<String> dataset = sequenceObservations.get(datasetIdx);
            
            for(int t = 1; t < dataset.size(); t++) {
                
                // t-1 row
                String past = dataset.get(t-1);
                
                // t row
                String present = dataset.get(t);
                
                // create variables
                Discrete var_X1 = new Discrete( new String[] {"0" , "1"} );
                Discrete var_X2 = new Discrete( new String[] {"0" , "1"} );
                Discrete var_X3 = new Discrete( new String[] {"0" , "1"} );
                Discrete var_X4 = new Discrete( new String[] {"0" , "1"} );
                Discrete var_X4_past = new Discrete( new String[] {"0" , "1"} );
                
                // add factors
                Factor prob_X1 = sequenceModel.addFactor(tf_X1_t, var_X1);
                prob_X1.setDirectedTo(var_X1);
                
                Factor prob_X2 = sequenceModel.addFactor(tf_X2_t, var_X2);
                prob_X2.setDirectedTo(var_X2);
                
                Factor prob_X3 = sequenceModel.addFactor(tf_X3_t, var_X3);
                prob_X3.setDirectedTo(var_X3);
                
                Factor prob_X4 = sequenceModel.addFactor(tf_X4_t, var_X4, var_X4_past, var_X3);
                prob_X4.setDirectedTo(var_X4);
                
                // Now set evidence
                String[] pastObs = past.split(",");
                String[] presentObs = present.split(",");
                
                String value_X1 = presentObs[0];
                String value_X2 = presentObs[1];
                String value_X3 = presentObs[2];
                String value_X4 = presentObs[3];
                String value_X4_past = pastObs[3];
                
                if(value_X1.equals("0")) {
                    var_X1.setFixedValue("0");
                } else if(value_X1.equals("1")) {
                    var_X1.setFixedValue("1");
                }
                
                if(value_X2.equals("0")) {
                    var_X2.setFixedValue("0");
                } else if(value_X2.equals("1")) {
                    var_X2.setFixedValue("1");
                }
                
                if(value_X3.equals("0")) {
                    var_X3.setFixedValue("0");
                } else if(value_X3.equals("1")) {
                    var_X3.setFixedValue("1");
                }
                
                if(value_X4.equals("0")) {
                    var_X4.setFixedValue("0");
                } else if(value_X4.equals("1")) {
                    var_X4.setFixedValue("1");
                }
                
                if(value_X4_past.equals("0")) {
                    var_X4_past.setFixedValue("0");
                } else if(value_X4_past.equals("1")) {
                    var_X4_past.setFixedValue("1");
                }
            }
            
        }
        
        
        
        // Now do EM
        sequenceModel.setSolverFactory(new SumProductSolver());
        sequenceModel.baumWelch(transitionTables, 2, 50);

        System.out.println("Transition Model EM completed");

        System.out.println("Learned Transition model probabilities::");
        for(IFactorTable table : transitionTables) {
            System.out.println("\tProbs = "+ Arrays.toString(table.getWeightsSparseUnsafe()));
        }
        
    }
    
    public static ArrayList<File> getTrainingDataFromFolder(String trainingDataFolderName) {
        if(trainingDataFolderName == null || trainingDataFolderName.length() == 0) {
            return null;
        }
        
        File trainFolder = null;
        try {
            trainFolder = new File(trainingDataFolderName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
        if(trainFolder == null) {
            return null;
        }
        
        if(trainFolder.isDirectory() == false) {
            System.out.println("WARN: Input filename for training data doesn't point to a directory:: " + trainingDataFolderName);
            return null;
        }
        
        if(trainFolder.listFiles().length == 0) {
            System.out.println("WARN: Empty input directory:: " + trainingDataFolderName);
            return null;
        }
        
        // only pull out CSV files
        ArrayList<File> csvFiles = new ArrayList<File>();
        
        for(File file : trainFolder.listFiles()) {
            if(file.getName().endsWith(".csv")) {
                csvFiles.add(file);
            } else {
                System.out.println("INFO: Skipped " + file.getName());
            }
        }
        
        return csvFiles;
    }
    
    public static ArrayList<String[]> getInitialCsvData(List<File> inputFiles) {
        // Assume X1,X2,X3,X4 format and headers
        if(inputFiles == null || inputFiles.size() == 0) {
            return null;
        }
        
        
        ArrayList<String[]> initialData = new ArrayList<String[]>();
        for(File file : inputFiles) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                
                //ignore headers
                br.readLine();
                
                String row = null;
                
                // only read the second row in the file
                initialData.add(br.readLine().split(","));
                
                br.close();
                
                
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
            
        }
        
        return initialData;
    }
    
    public static ArrayList<ArrayList<String>> getTransitionCsvDatasets(List<File> inputFiles) {
        
        if(inputFiles == null || inputFiles.isEmpty()) {
            return null;
        }
        
        ArrayList<ArrayList<String>> allDatasets = new ArrayList<ArrayList<String>>();
        
        for(File file : inputFiles) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                
//                System.out.println("INFO:: " + file.getName() + " header = " + br.readLine());
                
                
                ArrayList<String> dataset = new ArrayList<String>();
                
                String row = null;
                
                while( (row = br.readLine()) != null) {
                    dataset.add(row);
                }
                br.close();
                allDatasets.add(dataset);
                
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
            
        }
        
        return allDatasets;
    }
    
    
    public static IFactorTable createInitialBinaryCPT(Discrete variable) {
        
        if(variable == null) {
            return null;
        }
        
        int[][] idx = {
            {0},
            {1}
        };
        
        // nudge probs away from uniform by no more than 0.1
        final double randDiff = Math.random() * 0.1 * 0.5;
        
        
        double[] probs = {
            (0.5 + randDiff) ,
            (0.5 - randDiff)
        };
//        System.out.println("\tINFO: Sum of probs = " + (probs[0] + probs[1]) );
        
        return FactorTable.create(idx, probs, variable);
        
    }
    
    public static IFactorTable createInitialTransitionModel_X4(Discrete var_X4_t, Discrete var_X3_t , Discrete var_X4_t_1) {
        
        if(var_X4_t == null || var_X4_t_1 == null || var_X3_t == null) {
            return null;
        }
        
        int[][] idx = {
        
            {0, 0, 0},
            {0, 0, 1},
            {0, 1, 0},
            {0, 1, 1},
            
            {1, 0, 0},
            {1, 0, 1},
            {1, 1, 0},
            {1, 1, 1}
                
        };
        
        final double diff1 = Math.random() * 0.1d * 0.5d;
        final double diff2 = Math.random() * 0.1d * 0.5d;
        final double diff3 = Math.random() * 0.1d * 0.5d;
        final double diff4 = Math.random() * 0.1d * 0.5d;
        
        double[] probs = {
            
            0.5d + diff1,
            0.5d + diff2,
            0.5d + diff3,
            0.5d + diff4,
            
            0.5d - diff1,
            0.5d - diff2,
            0.5d - diff3,
            0.5d - diff4
        };
        
        double sum = 0.0d;
        for(double val : probs) {
            sum = sum + val;
        }
        
//        System.out.println("\tINFO: Sum of probs in X4_T CPTS is: " + sum);
        
        return FactorTable.create(idx, probs, var_X4_t, var_X3_t, var_X4_t_1);
        
    }
}
 