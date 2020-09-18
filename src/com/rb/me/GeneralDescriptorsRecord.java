package com.rb.me;


import java.io.Serializable;
import java.util.Objects;

//class to hold general descriptors for a patient
public class GeneralDescriptorsRecord implements Serializable, Comparable<GeneralDescriptorsRecord>
{
    //As noted, these six descriptors are collected at the time the patient is admitted to the ICU.
    //Their associated time-stamps are set to 00:00 (thus they appear at the beginning of each patient's record).
    //---------------------------------------------------------------------------------------------------------
    //RecordID (a unique integer for each ICU stay)
    //Age (years)
    //Gender (0: female, or 1: male)
    //Height (cm)
    //ICUType (1: Coronary Care Unit, 2: Cardiac Surgery Recovery Unit, 3: Medical ICU, or 4: Surgical ICU)
    //Weight (kg)*.
    //---------------------------------------------------------------------------------------------------------
    //The ICUType was added for use in Phase 2; it specifies the type of ICU to which the patient has been admitted.
    //All valid values for general descriptors are non-negative (≥ 0).
    //A value of -1 indicates missing or unknown data, e.g. height or weight are not recorded

    //instance variables
    private int recordID;
    private int ageInYears;
    private int gender0Or1;
    private double heightInCentimeters;
    private ICUType icuType;
    private double weightInKg;

    //constructor
    public GeneralDescriptorsRecord(int recordID, int ageInYears, int gender0Or1, double heightInCentimeters, int icuTypeCode, double weightInKg)
    {
        this.recordID = recordID;
        this.ageInYears = ageInYears;
        this.gender0Or1 = gender0Or1;
        this.heightInCentimeters = heightInCentimeters;
        this.icuType = ensureAndGet(icuTypeCode);
        this.weightInKg = weightInKg;
    } // GeneralDescriptorsRecord


    //helper method to check whether height is missing
    public boolean isHeightInCentimetersMissing()
    {
        return Double.compare(heightInCentimeters, 0.0) < 0;
    } // isHeightInCentimetersMissing


    //helper method to check whether weight at time stamp 0 is missing
    public boolean isWeightInKgMissing()
    {
        return Double.compare(weightInKg, 0.0) < 0;
    } // isWeightInKgMissing


    public int getRecordID() {
        return recordID;
    }

    public int getAgeInYears() {
        return ageInYears;
    }

    public int getGender0Or1() {
        return gender0Or1;
    }

    public double getHeightInCentimeters() {
        return heightInCentimeters;
    }

    public ICUType getIcuType() {
        return icuType;
    }

    public double getWeightInKg() {
        return weightInKg;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return recordID == ((GeneralDescriptorsRecord) o).getRecordID();
    } // equals

    @Override
    public int hashCode() {
        return Objects.hash(recordID);
    } // hashCode


    //for comparing two (GeneralDescriptorsRecord)s
    public int compareTo(GeneralDescriptorsRecord other)
    {
        return Integer.compare(recordID, other.getRecordID());
    } // compareTo


    @Override
    public String toString() {
        return "GenDescRecord{" +
                "recordID = " + recordID +
                ", ageInYears = " + ageInYears +
                ", gender0Or1 = " + gender0Or1 +
                ", heightInCentimeters = " + heightInCentimeters +
                ", icuType = " + icuType +
                ", weightInKg = " + weightInKg +
                '}';
    } // toString


    //ensure and get
    private ICUType ensureAndGet(int icuTypeCode)
    {
        switch (icuTypeCode)
        {
            case 1:
                return ICUType.Coronary_Care_Unit;
                //break;
            case 2:
                return ICUType.Cardiac_Surgery_Recovery_Unit;
                //break;
            case 3:
                return ICUType.Medical_ICU;
                //break;
            case 4:
                return ICUType.Surgical_ICU;
                //break;
            default:
                throw new RuntimeException("There is no such an ICU type with code: " + icuTypeCode);
        } // switch
    } // ensureAndGet

    private enum ICUType
    {
        Coronary_Care_Unit(1),
        Cardiac_Surgery_Recovery_Unit(2),
        Medical_ICU(3),
        Surgical_ICU(4);

        private final int code;

        ICUType(int icuTypeCode)
        {
            this.code = icuTypeCode;
        } // ICUType

        public int getCode()
        {
            return code;
        } // getCode


        @Override
        public String toString()
        {
            return "ICUType{" +
                    "name = '" + this.name() + '\'' +
                    ", code = " + code +
                    '}';
        } // toString

    } // enum ICUType

} // class
