
package dev.amb.pgm.bayesianhack.dimple;

import com.analog.lyric.dimple.factorfunctions.core.FactorTable;
import com.analog.lyric.dimple.factorfunctions.core.IFactorTable;
import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.Discrete;
import com.analog.lyric.dimple.model.variables.VariableBase;
import com.analog.lyric.dimple.solvers.sumproduct.SumProductSolver;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author AntB
 */
public class App_DimpleEM {
    
    
    public static final String DATASET_FILENAME = "hiddenVar_ABH.csv";
    
    
    public static void main(String[] args) {
        
        /*
            Create initial BN FactorGraph
            
                ( H )
                // \\
               *     *
            ( A )   ( B )
        */
        
        
        /*
            Now for some learning shizzle
        */
        
        // create tied TableFactors
        
        IFactorTable tf_A_H = createFactorTableA_H();
        IFactorTable tf_B_H = createFactorTableB_H();
        IFactorTable tf_H = createFactorTableH();
        
        // set up the factor graph with input data
        
        FactorGraph learningBN = new FactorGraph();
        
        learningBN.setName("Learning P(A,B,H)");
        
        // Read the learning data into an ArrayList of strings (ex header)
        ArrayList<String> trainingData = loadTrainingData();
        
        // I thnk we add factors, then fix values, one per "timeslice"
        for(String row : trainingData) {
            
            Discrete a_var = new Discrete( new String[] { "a0" , "a1" } );
            Discrete b_var = new Discrete( new String[] { "b0" , "b1" } );
            Discrete h_var = new Discrete( new String[] { "h0" , "h1" } );
            
            // create factors using repeated TableFactors :)
            
            Factor f_A = learningBN.addFactor(tf_A_H, a_var, h_var);
            f_A.setDirectedTo(a_var);
            
            Factor f_B = learningBN.addFactor(tf_B_H, b_var, h_var);
            f_B.setDirectedTo(b_var);
            
            Factor f_H = learningBN.addFactor(tf_H, h_var);
            f_H.setDirectedTo(h_var);
            
            
            // set evidence values
            String[] data = row.split(",");
            if(data.length != 3) {
                System.out.println("Error: data row does not have 3 values! " + data.length);
            } else {
                if(data[0] != null && data[0].equals("N/A") == false) {
                    System.out.println("\tSetting varA to " + data[0]);
                    a_var.setFixedValue(data[0]);
                }

                if(data[1] != null && data[1].equals("N/A") == false) {
                    System.out.println("\tSetting varB to " + data[1]);
                    b_var.setFixedValue(data[1]);
                }

                if(data[2] != null && data[2].equals("N/A") == false) {
                    System.out.println("\tSetting varH to " + data[2]);
                    h_var.setFixedValue(data[2]);
                }
            }
            
        }
        
        IFactorTable[] factorsToLearn = new IFactorTable[3];
        factorsToLearn[0] = tf_A_H;
        factorsToLearn[1] = tf_B_H;
        factorsToLearn[2] = tf_H;
        // Check values
        System.out.println("\n*** Pre baumWelch parameters::\n");
        
        for(IFactorTable f : factorsToLearn) {
            System.out.println("weights = " + Arrays.toString(f.getWeightsSparseUnsafe()));
        }
        
        
        
        // now we need to run baumWelch in such a way that the prob params are tied across the "timeslices"
        learningBN.setSolverFactory(new SumProductSolver());
        learningBN.baumWelch(factorsToLearn, 1, 10);
        
        
        
        // Now build standard BN from learned FactorTables
        
        FactorGraph result = new FactorGraph();
        
        Discrete a = new Discrete( new String[] { "a0" , "a1" } );
        a.setName("A");
        Discrete b = new Discrete( new String[] { "b0" , "b1" } );
        b.setName("B");
        Discrete h = new Discrete( new String[] { "h0" , "h1" } );
        h.setName("H");
        
        result.setName("Learned Model P(A, B , H)");
        
        
        // P(A | H)
        result.addFactor(tf_A_H, a, h);
        
        // P(B | H)
        result.addFactor(tf_B_H, b , h);
        
        // P(H)
        result.addFactor(tf_H, h);
        
        
        // get marginal probs
        result.solve();
        
        System.out.println("\n*** Post baumWelch parameters::\n");
        
        
        printBeliefs(result, "");
        
    }

    
    
    public static void printBeliefs(FactorGraph fg, String varFamily) {
        for(VariableBase var : fg.getVariables()) {
                
                if(var.getName().contains(varFamily)) {
                    System.out.println("Beliefs for " + varFamily + " variables = " + var.getName() + " = " + Arrays.toString( (double[]) ((Discrete) var).getBelief()));
                }
            }
        
    }
    
    
    public static ArrayList<String> loadTrainingData() {
        File dataFile = null;
        
        try {
            dataFile = new File(App_DimpleEM.class.getClassLoader().getResource(DATASET_FILENAME).toURI());
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        ArrayList<String> trainingData = new ArrayList<String>();
        
        if(dataFile != null) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(dataFile));
                String header = br.readLine();
                
                String row = null;
                while( (row = br.readLine()) != null) {
                    trainingData.add(row);
                }
                br.close();
                
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Loaded " + trainingData.size() + " rows of data");

        return trainingData;
        
    }
    
    
    public static IFactorTable createFactorTableA_H(){
        //  A   H   P(A | H)
        //  0   0   0.39
        //  0   1   0.45
        //  1   0   0.61
        //  1   1   0.55
        // double[] distA_H = { 0.39d , 0.45d , 0.61d , 0.55d };
        int[][] idx = new int[][] {
            { 0 , 0 },
            { 0 , 1 },
            { 1 , 0 },
            { 1 , 1 }
        };
        
        double[] weights = { 0.39d , 0.45d ,
                             0.61d , 0.55d };
        
        
        return FactorTable.create(idx , weights , new Discrete( new String[] { "a0" , "a1" } ), new Discrete( new String[] { "h0" , "h1" } ));
        
        
    }
    
    public static IFactorTable createFactorTableB_H() {
        //  B   H   P(B | H)
        //  0   0   0.48
        //  0   1   0.57
        //  1   0   0.52
        //  1   1   0.43
        // double[] distB_H = { 0.48d , 0.57d , 0.52d , 0.43d };
        
        int[][] idx = new int[][] {
            { 0 , 0 },
            { 0 , 1 },
            { 1 , 0 },
            { 1 , 1 }
        };
        
        
        double[] weights = { 0.48d , 0.57d ,
                            0.52d , 0.43d };
        
        
        return FactorTable.create(idx, weights, new Discrete( new String[] { "b0" , "b1" } ), new Discrete( new String[] { "h0" , "h1" } ));
        
    }
    
    public static IFactorTable createFactorTableH() {
        //  H   P(H)
        //  0   0.6
        //  1   0.4
        // double[] distH = {0.6d , 0.4d};
        int[][] idx = new int[][] {
            { 0 } ,
            { 1 }
        };
        
        double[] weights = {0.6d , 0.4d};
        
        return FactorTable.create(idx, weights, new Discrete( new String[] { "h0" , "h1" } ));
        
    }
    
}
