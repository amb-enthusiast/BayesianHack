package dev.amb.pgm.bayesianhack.samiam;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.TableShell;
import edu.ucla.belief.io.NetworkIO;
import java.io.File;


public class App_SamiamEM {
    
    public static void main( String[] args ) {
        
        
        try {
            
            if(args.length == 0) {
                System.out.println("WARN: You didnt supply a filename for the output as a runtime argument.\nWill default to project directory");
            }
            
            // Read the A, B, H network file to get starting model
            File netFile = null;
            try {
                netFile = new File(App_SamiamEM.class.getClassLoader().getResource("simpleHiddenNode.net").toURI());
            } catch(Exception e) {
                e.printStackTrace();
            }
            
            if(netFile != null) {
                
                BeliefNetwork startModel = NetworkIO.read(netFile);
                
                // Now execute EM over 15 iterations & write model to file
                // EM over hiddenVar_ABH.csv file (it is already in SAMIAM friendly format that matches the .net file model)
                // and simpleHiddenVar.net initial BN
                BeliefNetwork learnedModel = EMUtils.doEM("hiddenVar_ABH.csv", startModel, 15, "EM_learned_simpleHiddenVar.net");

                // Just print out the results to inspect
                System.out.println("\nNow displaying the resultant CPTs in the learned model:");
                for(Object obj : learnedModel.tables()) {
                    TableShell table = (TableShell) obj;
                    System.out.println(table.getCPT().tableString());
                }
            }
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
 
}