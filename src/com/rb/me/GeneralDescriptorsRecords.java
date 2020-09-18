package com.rb.me;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

//class to have all general descriptors of all records in the dataset
public class GeneralDescriptorsRecords implements Serializable, Comparable<GeneralDescriptorsRecords>
{
    private Dataset dataset;

    //map to hold recordID and its general descriptors
    private LinkedHashMap<Integer, GeneralDescriptorsRecord> generalDescriptorsRecords;

    //multiton approach
    private static HashMap<Dataset, GeneralDescriptorsRecords> multiton = new HashMap<>();


    //static getInstance method
    public static GeneralDescriptorsRecords getInstance(Dataset dataset)
    {
        //if multiton does not contain general descriptors records for this dataset
        if(!multiton.containsKey(dataset)) multiton.put(dataset, new GeneralDescriptorsRecords(dataset));

        return multiton.get(dataset);
    } // getInstance


    //add method to add recordID and corresponding general descriptors
    public void add(int recordID, GeneralDescriptorsRecord record)
    {
        //report if replace occurs
        if(generalDescriptorsRecords.containsKey(recordID)) System.err.println("GeneralDescriptorsRecord for recordID: " + recordID + "is replaced");

        generalDescriptorsRecords.put(recordID, record);
    } // add


    //obtain general descriptors record by record id
    public GeneralDescriptorsRecord getGeneralDescriptors(int recordID)
    {
        return generalDescriptorsRecords.get(recordID);
    } // get


    //emptiness checker
    public boolean isEmpty()
    {
        return generalDescriptorsRecords.isEmpty();
    } // isEmpty


    //private constructor
    private GeneralDescriptorsRecords(Dataset dataset)
    {
        this.dataset = dataset;
        generalDescriptorsRecords = new LinkedHashMap<>();
    } // GeneralDescriptorsRecords

    //helper method to get record ids
    public Set<Integer> getRecordIDs()
    {
        return generalDescriptorsRecords.keySet();
    } // getRecordIDs

    public Dataset getDataset() {
        return dataset;
    }

    public int size() { return generalDescriptorsRecords.size(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return dataset == ((GeneralDescriptorsRecords) o).getDataset();
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataset);
    }

    public int compareTo(GeneralDescriptorsRecords other)
    {
        return dataset.compareTo(other.getDataset());
    } // compareTo

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for(Integer recordID : generalDescriptorsRecords.keySet())
        {
            sb.append(generalDescriptorsRecords.get(recordID)).append("\n");
        } // for

        return sb.toString();
    } // toString

} // GeneralDescriptorsRecords
