package dev.amb.pgm.bayesianhack.jsmile;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import smile.Network;
import smile.SMILEException;
import smile.learning.DataSet;
import smile.learning.EM;

public class SmileNetworkTools {

    public static final String TUTORIAL_FILENAME = "smileTutorial_a.xdsl";
    public static final String LEARNED_FILENAME = "smile_EM_learned_hiddenVar_ABH.xdsl";
    public static final String DATASET_FILENAME = "hiddenVar_ABH.csv";

    public static final int SEED_VALUE = 0;
    
    
    public static DataSet createDataset() {
        try {
            
            DataSet data = new DataSet();
            
            File dataFile = new File(SmileNetworkTools.class.getClassLoader().getResource(DATASET_FILENAME).toURI());
            
            // We've got a data file with "N/A' values as required by SamIam - just make sure these are converted to -1 values for SMILE
            data.readFile(dataFile.getAbsolutePath(), "N/A", -1, -1.0f, true);
            
            System.out.println("DataSet data contains " + data.getRecordCount() + " records, over " + data.getVariableCount() + " variables");
            
            // Let's just print the dataset for comparison
            StringBuilder recordBuilder = new StringBuilder();
            
            for(int recordIdx = 0 ; recordIdx < data.getRecordCount(); recordIdx++) {
                
                for(int varIdx = 0; varIdx < data.getVariableCount();varIdx++) {
                    recordBuilder.append(data.getVariableId(varIdx));
                    recordBuilder.append(" = ");
                    recordBuilder.append(data.getInt(varIdx, recordIdx));
                    recordBuilder.append(" ");
                }
                recordBuilder.append("\n");
            }
            
            System.out.println("Input dataset:\n" + recordBuilder.toString());
            
            return data;

        } catch (SMILEException sme) {
            System.out.println(sme.getMessage());
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    
    public static void emLearning(DataSet data, Network network) {
        try {

            // Set up and config EM
            EM em = new EM();
            em.setEqSampleSize(0);
            em.setRandomizeParameters(true);
            em.setSeed(SEED_VALUE);
            
            em.learn(data, network, data.matchNetwork(network));
            
            
            System.out.println(network.writeString());
            
            System.out.println("EM log-likelihood: " + em.getLastScore());
            
            network.clearAllEvidence();
            
            network.updateBeliefs();
            
            File outFile = new File(LEARNED_FILENAME);
            System.out.println("INFO: Saving saved model to: " + outFile.getAbsolutePath());
            
            network.writeFile(outFile.getAbsolutePath());
            
        } catch (SMILEException sme) {
            sme.printStackTrace();
        }
    }

    
    public static Network createLearningNetwork() {

        try {
            Network net = new Network();

            net.addNode(Network.NodeType.Cpt, "A");
            net.setOutcomeId("A", 0, "a0");
            net.setOutcomeId("A", 1, "a1");

            net.addNode(Network.NodeType.Cpt, "B");
            net.setOutcomeId("B", 0, "b0");
            net.setOutcomeId("B", 1, "b1");

            net.addNode(Network.NodeType.Cpt, "H");
            net.setOutcomeId("H", 0, "h0");
            net.setOutcomeId("H", 1, "h1");

            net.addArc("H", "A");
            net.addArc("H", "B");

            return net;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    
    
    public static Network createTutorialNetwork() {

        try {
            Network net = new Network();

            // Creating node "Success" and setting/adding outcomes:
            net.addNode(Network.NodeType.Cpt, "Success");
            net.setOutcomeId("Success", 0, "Success");
            net.setOutcomeId("Success", 1, "Failure");

            // Creating node "Forecast" and setting/adding outcomes:
            net.addNode(Network.NodeType.Cpt, "Forecast");
            net.addOutcome("Forecast", "Good");
            net.addOutcome("Forecast", "Moderate");
            net.addOutcome("Forecast", "Poor");
            net.deleteOutcome("Forecast", 0);
            net.deleteOutcome("Forecast", 0);

            // Adding an arc from "Success" to "Forecast":
            net.addArc("Success", "Forecast");

            // Filling in the conditional distribution for node "Success". The 
            // probabilities are:
            // P("Success" = Success) = 0.2
            // P("Success" = Failure) = 0.8
            double[] aSuccessDef = {0.2, 0.8};
            net.setNodeDefinition("Success", aSuccessDef);

            // Filling in the conditional distribution for node "Forecast". The 
            // probabilities are:
            // P("Forecast" = Good | "Success" = Success) = 0.4
            // P("Forecast" = Moderate | "Success" = Success) = 0.4
            // P("Forecast" = Poor | "Success" = Success) = 0.2
            // P("Forecast" = Good | "Success" = Failure) = 0.1
            // P("Forecast" = Moderate | "Success" = Failure) = 0.3
            // P("Forecast" = Poor | "Success" = Failure) = 0.6
            double[] aForecastDef = {0.4, 0.4, 0.2, 0.1, 0.3, 0.6};
            net.setNodeDefinition("Forecast", aForecastDef);

            // Changing the nodes' spacial and visual attributes:
            net.setNodePosition("Success", 20, 20, 80, 30);
            net.setNodeBgColor("Success", Color.red);
            net.setNodeTextColor("Success", Color.white);
            net.setNodeBorderColor("Success", Color.black);
            net.setNodeBorderWidth("Success", 2);
            net.setNodePosition("Forecast", 30, 100, 60, 30);

            return net;

        } catch (SMILEException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    
    
    public static void inferenceInTutorialNetwork(Network tutorialNet) {
        try {

            /*
             * Assume the input network is the one created in the tutorial.
             * We want to compute P("Forecast" = Moderate)
             * Updating the network:
             */
            tutorialNet.updateBeliefs();


            // Getting the index of the "Moderate" outcome:
            String[] aForecastOutcomeIds = tutorialNet.getOutcomeIds("Forecast");
            int outcomeIndex;
            for (outcomeIndex = 0; outcomeIndex < aForecastOutcomeIds.length; outcomeIndex++) {
                if ("Moderate".equals(aForecastOutcomeIds[outcomeIndex])) {
                    break;
                }
            }

            // Getting the value of the probability:
            double[] aValues = tutorialNet.getNodeValue("Forecast");
            double P_ForecastIsModerate = aValues[outcomeIndex];

            System.out.println("P(\"Forecast\" = Moderate) = " + P_ForecastIsModerate);


            /*
             * We want to compute P("Success" = Failure | "Forecast" = Good)
             */
            tutorialNet.setEvidence("Forecast", "Good");

            // Updating the network:
            tutorialNet.updateBeliefs();


            // Getting the index of the "Failure" outcome:
            String[] aSuccessOutcomeIds = tutorialNet.getOutcomeIds("Success");
            for (outcomeIndex = 0; outcomeIndex < aSuccessOutcomeIds.length; outcomeIndex++) {
                if ("Failure".equals(aSuccessOutcomeIds[outcomeIndex])) {
                    break;
                }
            }

            // Now looking up & printing the value of the probability for the index we just found...
            aValues = tutorialNet.getNodeValue("Success");
            double P_SuccIsFailGivenForeIsGood = aValues[outcomeIndex];
            System.out.println("P(\"Success\" = Failure | \"Forecast\" = Good) = " + P_SuccIsFailGivenForeIsGood);

            /*
             * We want to compute P("Success" = Success | "Forecast" = Poor)
             */
            // Clearing existing evidence in node "Forecast":
            tutorialNet.clearEvidence("Forecast");

            // Introducing the new evidence in node "Forecast" & updating the network:
            tutorialNet.setEvidence("Forecast", "Good");
            tutorialNet.updateBeliefs();

            // Getting the index of the "Failure" outcome:
            aSuccessOutcomeIds = tutorialNet.getOutcomeIds("Success");
            for (outcomeIndex = 0; outcomeIndex < aSuccessOutcomeIds.length; outcomeIndex++) {
                if ("Failure".equals(aSuccessOutcomeIds[outcomeIndex])) {
                    break;
                }
            }


            // Getting the value of the probability:
            aValues = tutorialNet.getNodeValue("Success");
            double P_SuccIsSuccGivenForeIsPoor = aValues[outcomeIndex];
            System.out.println("P(\"Success\" = Success | \"Forecast\" = Poor) = " + P_SuccIsSuccGivenForeIsPoor);

        } catch (SMILEException sme) {
            sme.printStackTrace();
        }
    }
    
}