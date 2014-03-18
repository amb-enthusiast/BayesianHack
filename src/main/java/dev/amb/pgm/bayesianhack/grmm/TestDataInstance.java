
package dev.amb.pgm.bayesianhack.grmm;

/**
 *
 * @author AntB
 */
public class TestDataInstance {
    
    private String varA = "A";
    private int valueA = -1;
    
    private String varB = "B";
    private int valueB = -1;
    
    private String varH = "H";
    private String nullHValue = "N/A";
    
    private int stateValueH = -1;

    // getters and setters
    
    public String getNullValueH() {
        return this.nullHValue;
    }
    
    public void setNullValueH(boolean nullFlag) {
        if(nullFlag == true) {
            this.nullHValue = "N/A";
            this.stateValueH = -1;
        } else {
            this.nullHValue = "0";
            this.stateValueH = -1;
        }
    }
    
    public void setStateValueH(int valueH) {
        this.stateValueH = valueH;
    }
    
    public int getStateValueH() {
        return this.stateValueH;
    }
    
    
    
    public String getVarALabel() {
        return varA;
    }

    public int getValueA() {
        return valueA;
    }

    public void setValueA(int valueA) {
        this.valueA = valueA;
    }

    public String getVarBLabel() {
        return varB;
    }
    
    public int getValueB() {
        return valueB;
    }

    public void setValueB(int valueB) {
        this.valueB = valueB;
    }
    
    public String toSamiamCsvString() {
        // assume A, B, H header - H is N/A for SAMIAM
        return this.getValueA() + "," + this.getValueB() + ",N/A";
    }
    
}
