package dev.amb.pgm.bayesianhack.jsmile;

import smile.Network;
import smile.learning.DataSet;

/**
 *
 * @author AntB
 */
public class App_SmileEM {

    public static void main(String[] args) {

        System.out.println("\nCreating A, B and H network - all vars are binary valued");
        Network net = SmileNetworkTools.createLearningNetwork();

        
        System.out.println("Reading datafile [" + SmileNetworkTools.DATASET_FILENAME + "] to create a SMILE Dataset");
        DataSet data = SmileNetworkTools.createDataset();

        
        System.out.println("\nExecute EM to learn parameters for A,B,H network from data");
        SmileNetworkTools.emLearning(data, net);
        
    }
}
