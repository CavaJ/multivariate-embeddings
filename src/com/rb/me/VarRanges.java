package com.rb.me;

import java.io.Serializable;
import java.util.*;

//multiton class to hold variable ranges for each dataset
public class VarRanges implements Serializable, Comparable<VarRanges>
{
    private Dataset dataset;

    //main data structure which will map var name to ranges
    private TreeMap<String, Ranges> varAndRangesMap;


    //multiton pattern, for each dataset there can be only one "VarRanges"
    private static HashMap<Dataset, VarRanges> multiton = new HashMap<>();

    public static VarRanges getInstance(Dataset dataset)
    {
        //if multiton does not contain var ranges for this dataset
        if(!multiton.containsKey(dataset)) multiton.put(dataset, new VarRanges(dataset));

        return multiton.get(dataset);
    } // getInstance

    //constructor
    private VarRanges(Dataset dataset)
    {
        this.dataset = dataset;
        varAndRangesMap = new TreeMap<>();
    } // VarRanges

    //add method which adds variable and its ranges to the data structure
    public void add(String varName, Ranges ranges)
    {
        //report if replace occurs
        if(varAndRangesMap.containsKey(varName)) System.err.println("Ranges for variable: " + varName + "are replaced");

        varAndRangesMap.put(varName, ranges);
    } // add


    //method to get all vars
    public Set<String> varNames()
    {
        return varAndRangesMap.keySet();
    } // varNames


    //emptiness check
    public  boolean isEmpty()
    {
        return varAndRangesMap.isEmpty();
    } // isEmpty


    //method to obtain a dataset of this instance
    public Dataset getDataset()
    {
        return dataset;
    } // getDataset


    //method to obtain ranges for the given variable
    public Ranges getRanges(String varName)
    {
        return varAndRangesMap.get(varName);
    } // getRanges


    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return dataset == ((VarRanges) o).getDataset();
    }

    //hash code is determined by the dataset, since the same dataset cannot have two "VarRanges" instances
    @Override
    public int hashCode() {
        return Objects.hash(dataset);
    }

    @Override
    public int compareTo(VarRanges other) {
        return dataset.compareTo(other.getDataset());
    } // compareTo


    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        for(Ranges ranges : varAndRangesMap.values())
        {
            sb.append(ranges).append("\n");
        } // for

        return sb.toString();
    } // toString

    //TODO Ranges class can also be defined as a private class inside this class
    //private Ranges instance can be instantiated from getInstance() method from its parameters
    //which will have the same parameters as the constructor of Ranges class
} // class VarRanges
