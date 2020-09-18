package com.rb.me;

import java.io.Serializable;
import java.util.Objects;

//data class which describes an outcome
public class Outcome implements Serializable, Comparable<Outcome>
{
    //outcome file has the following structure:
    //RecordID,SAPS-I,SOFA,Length_of_stay,Survival,In-hospital_death
    //    132539,6,1,5,-1,0
    //    132540,16,8,8,-1,0
    //    132541,21,11,19,-1,0

    //RecordID (a unique integer for each ICU stay)
    //SAPS-I score (Le Gall et al., 1984)
    //SOFA score (Ferreira et al., 2001)
    //Length of stay (days)
    //Survival (days)
    //In-hospital death (0: survivor, or 1: died in-hospital)

    //instance variables
    private int recordID;
    private int sapsIScore;
    private int sofaScore;
    private int lengthOfStayInDays;
    private int survivalInDays;
    private int inHospitalDeath0Or1;

    public Outcome(int recordID, int sapsIScore, int sofaScore, int lengthOfStayInDays, int survivalInDays, int inHospitalDeath0Or1)
    {
        this.recordID = recordID;
        this.sapsIScore = sapsIScore;
        this.sofaScore = sofaScore;
        this.lengthOfStayInDays = lengthOfStayInDays;
        this.survivalInDays = survivalInDays;
        this.inHospitalDeath0Or1 = inHospitalDeath0Or1;
    } // Outcome

    public int getSapsIScore() {
        return sapsIScore;
    }

    public int getSofaScore() {
        return sofaScore;
    }

    public int getLengthOfStayInDays() {
        return lengthOfStayInDays;
    }

    public int getSurvivalInDays() {
        return survivalInDays;
    }

    public int getInHospitalDeath0Or1() {
        return inHospitalDeath0Or1;
    }

    public int getRecordID() {
        return recordID;
    }

    @Override
    public String toString() {
        return "Outcome{" +
                "recordID = " + recordID +
                ", sapsIScore = " + sapsIScore +
                ", sofaScore = " + sofaScore +
                ", lengthOfStayInDays = " + lengthOfStayInDays +
                ", survivalInDays = " + survivalInDays +
                ", inHospitalDeath0Or1 = " + inHospitalDeath0Or1 +
                '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return recordID == ((Outcome) o).getRecordID();
    } // equals

    @Override
    public int hashCode()
    {
        return Objects.hash(recordID);
    }

    @Override
    public int compareTo(Outcome other) {
        return Integer.compare(recordID, other.getRecordID());
    }

} // class
