
package dev.amb.pgm.bayesianhack.grmm;

import com.sun.org.apache.bcel.internal.generic.AALOAD;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author AntB
 */
public class DataUtils {

    
    // create simple dataset
    
    public static ArrayList<TestDataInstance> createTestData() {
        
        // create simple data set mimicing lecture notes
        /*
                Count	A	B
                6	0	0
                1	0	1
                1	1	0
                4	1	1   
         */
        
        ArrayList<TestDataInstance> simpleData = new ArrayList<TestDataInstance>();
        
        for(int idx=0 ; idx < 6;idx++) {
            TestDataInstance temp = new TestDataInstance();
            temp.setValueA(0);
            temp.setValueB(0);
            simpleData.add(temp);
        }
        
        for(int idx=0; idx<4;idx++) {
            TestDataInstance temp = new TestDataInstance();
            temp.setValueA(1);
            temp.setValueB(1);
            simpleData.add(temp);
        }
        
        TestDataInstance d1 = new TestDataInstance();
        d1.setValueA(0);
        d1.setValueB(1);
        simpleData.add(d1);
        
        TestDataInstance d2 = new TestDataInstance();
        d2.setValueA(1);
        d2.setValueB(0);
        simpleData.add(d2);
        
        return simpleData;
        
    }
    
    
    public static ArrayList<TestInstanceCount> countDataFromInstances(List<TestDataInstance> inputData) {
        
        // assume we know the size of each variable, and it is 2 :)
        
        long count_A0B0 = 0;
        long count_A0B1 = 0;
        long count_A1B0 = 0;
        long count_A1B1 = 0;
        
        for(TestDataInstance data : inputData) {
            if(data.getValueA() == 0 && data.getValueB() == 0) {
                count_A0B0++;
            } else if(data.getValueA() == 0 && data.getValueB() == 1) {
                count_A0B1++;
            } else if(data.getValueA() == 1 && data.getValueB() == 0) {
                count_A1B0++;
            } else if(data.getValueA() == 1 && data.getValueB() == 1) {
                count_A1B1++;
            }
        }
        // results list
        ArrayList<TestInstanceCount> countData = new ArrayList<TestInstanceCount>();
        
        // create count objects
        TestInstanceCount c1 = new TestInstanceCount();
        c1.setAssignmentVarA(0);
        c1.setAssignmentVarB(0);
        c1.setAssignmentCount(count_A0B0);
        countData.add(c1);
        c1 = null;
        
        TestInstanceCount c2 = new TestInstanceCount();
        c2.setAssignmentVarA(0);
        c2.setAssignmentVarB(1);
        c2.setAssignmentCount(count_A0B1);
        countData.add(c2);
        c2 = null;
                
        TestInstanceCount c3 = new TestInstanceCount();
        c3.setAssignmentVarA(1);
        c3.setAssignmentVarB(0);
        c3.setAssignmentCount(count_A1B0);
        countData.add(c3);
        c3 = null;
        
        TestInstanceCount c4 = new TestInstanceCount();
        c4.setAssignmentVarA(1);
        c4.setAssignmentVarB(1);
        c4.setAssignmentCount(count_A1B1);
        countData.add(c4);
        c4 = null;
        
        return countData;
    }
    
    // TODO write to CSV
    public void writeDataToCsv(String filename , ArrayList<TestDataInstance> inputData) {
        
        String header = "";
        
        File outFile = null;
        int dataCount = 0;
        try {
            outFile = new File(filename);
            
            FileWriter fw = new FileWriter(outFile);
        
            BufferedWriter bw = new BufferedWriter(fw);
        
            bw.write(header);
            
            for(TestDataInstance data : inputData) {
                bw.newLine();
                bw.write(data.toSamiamCsvString());
                dataCount++;
            }
            
            System.out.println("Written " + dataCount + " rows of data to SAMIAM CSV format @ " + filename);
            
        } catch(Exception e) {
            e.printStackTrace();
        }
        
    }
    
    // TODO read CSV
    public ArrayList<TestDataInstance> readSamiamCsv(String filename) {
        
        File outFile = null;
        int dataCount = 0;
        
        ArrayList<TestDataInstance> inputData = new ArrayList<TestDataInstance>();
                
        try {
            outFile = new File(filename);
            FileReader fr = new FileReader(outFile);
            BufferedReader br = new BufferedReader(fr);
        
            // skip the header
            br.readLine();
            
            // now read each line
            String row;
            
            while( (row = br.readLine()) != null) {
                String[] values = row.split("");
                Integer aValue = Integer.parseInt(values[0]);
                Integer bValue = Integer.parseInt(values[1]);
                
                TestDataInstance data = new TestDataInstance();
                data.setValueA(aValue);
                data.setValueB(bValue);
                
                // we have an N/A value for H, which is okay.
                data.setNullValueH(true);
                dataCount++;
            }
            
            System.out.println("Read " + dataCount + " rows of SAMIAM CSV data from @ " + filename);
            return inputData;
            
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
        
        
        
    }
    
    // TODO store in database
    
    // TODO get data instance from database
    
    // TODO get count data from database
    
    
    // TODO conversion utils
    
    // TODO convert CSV, to include hidden values with "N/A" SAMIAM string
    
    
    
}
