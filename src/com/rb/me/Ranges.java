package com.rb.me;


import java.io.Serializable;
import java.util.Objects;

//class to hold outlier removal ranges for the given variable
public class Ranges implements Serializable, Comparable<Ranges>
{
    private Dataset dataset;
    private String varName;
    private double outlierLow;
    private double validLow;
    private double normal;
    private double validHigh;
    private double outlierHigh;

    public Ranges(Dataset dataset, String varName, double outlierLow, double validLow, double normal, double validHigh, double outlierHigh)
    {
        this.dataset = dataset;
        this.varName = varName;
        this.outlierLow = outlierLow;
        this.validLow = validLow;
        this.normal = normal;
        this.validHigh = validHigh;
        this.outlierHigh = outlierHigh;
    } // Ranges


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return this.hashCode() == ((Ranges) o).hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataset, varName);
    }


    @Override
    public int compareTo(Ranges other)
    {
        //more clear version of two-component compareTo
        int result = dataset.compareTo(other.getDataset());
        if(result == 0)
        {
            result = varName.compareTo(other.getVarName());
        } // if
        return result;
    } // compareTo


    //public String toString()
    //{
    //    return varName + "," + outlierLow + "," + validLow + "," + normal + "," + validHigh + "," + outlierHigh;
    //} // toString


    //toString
    @Override
    public String toString() {
        return "Ranges{" +
                "varName = '" + varName + '\'' +
                ", outlierLow = " + outlierLow +
                ", validLow = " + validLow +
                ", normal = " + normal +
                ", validHigh = " + validHigh +
                ", outlierHigh = " + outlierHigh +
                '}';
    }

    public String getVarName() {
        return varName;
    }

    public double getOutlierLow() {
        return outlierLow;
    }

    public double getValidLow() {
        return validLow;
    }

    public double getNormal() {
        return normal;
    }

    public double getValidHigh() {
        return validHigh;
    }

    public double getOutlierHigh() {
        return outlierHigh;
    }

    public Dataset getDataset()
    {
        return dataset;
    }

} // class Ranges
