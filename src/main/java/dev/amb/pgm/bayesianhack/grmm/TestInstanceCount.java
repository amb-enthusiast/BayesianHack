
package dev.amb.pgm.bayesianhack.grmm;

/**
 *
 * @author AntB
 */
public class TestInstanceCount {
    
    private int assignmentVarA = 0;
    private int assignmentVarB = 0;
    private long assignmentCount = 0;

    public int getAssignmentVarA() {
        return assignmentVarA;
    }

    public void setAssignmentVarA(int assignmentVarA) {
        this.assignmentVarA = assignmentVarA;
    }

    public int getAssignmentVarB() {
        return assignmentVarB;
    }

    public void setAssignmentVarB(int assignmentVarB) {
        this.assignmentVarB = assignmentVarB;
    }

    public long getAssignmentCount() {
        return assignmentCount;
    }

    public void setAssignmentCount(long assignmentCount) {
        this.assignmentCount = assignmentCount;
    }
    
    @Override
    public String toString() {
        
        return "count(A=" + this.assignmentVarA + ", B=" + this.assignmentVarB + ")=" + this.assignmentCount;
    }

}
