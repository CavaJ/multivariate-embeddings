package com.rb.me;

import java.io.Serializable;
import java.math.RoundingMode;
import java.util.*;

//class to hold all outcomes from all different files
public class Outcomes implements Serializable, Comparable<Outcomes>
{
    //main data structure to hold the outcomes
    //it will map from integer to outcome
    private LinkedHashMap<Integer, Outcome> outcomes;

    //dataset - this "outcomes" is attached to
    private Dataset dataset;


    //multiton pattern, for each dataset there can be only one "Outcomes"
    private static HashMap<Dataset, Outcomes> multiton = new HashMap<>();


    //static method to access multiton
    public static Outcomes getInstance(Dataset dataset)
    {
        //if multiton does not contain outcomes for this dataset
        if(!multiton.containsKey(dataset)) multiton.put(dataset, new Outcomes(dataset));

        return multiton.get(dataset);
    } // getInstance


    //constructor
    private Outcomes(Dataset dataset)
    {
        this.dataset = dataset;
        outcomes = new LinkedHashMap<>();
    } // Outcomes

    //emptiness checker method
    public boolean isEmpty()
    {
        return outcomes.isEmpty();
    }

    //method to add an outcome and its corresponding record id
    public void add(int recordID, Outcome outcome)
    {
        //report if replace occurs
        if(outcomes.containsKey(recordID)) System.err.println("Outcomes for recordID: " + recordID + " is replaced");

        outcomes.put(recordID, outcome);
    } // add

    //method for getting outcome by recordID
    public Outcome get(int recordID)
    {
        return outcomes.get(recordID);
    } // get


    //method to obtain record ids
    public Set<Integer> getRecordIDs()
    {
        return outcomes.keySet();
    }


    public int size()
    {
        return outcomes.size();
    }


    public int countInHospitalDeathPositive()
    {
        int count = 0;
        for(Outcome outcome : outcomes.values())
        {
            count += outcome.getInHospitalDeath0Or1();
        } // for

        return count;
    } // countInHospitalDeathPositive

    public int countInHospitalDeathNegative()
    {
        return outcomes.size() - countInHospitalDeathPositive();
    } // countInHospitalDeathPositive


    public String classImbalanceInHospitalDeath()
    {
        int positiveCount = countInHospitalDeathPositive();
        int negativeCount = countInHospitalDeathNegative();
        return summaryString(positiveCount, negativeCount, outcomes.size());
    } // classImbalanceInHospitalDeath


    public String classImbalanceInHospitalDeath(int startIndexInclusive, int endIndexExclusive)
    {
        int positiveCount = countInHospitalDeathPositive(startIndexInclusive, endIndexExclusive);
        int negativeCount = countInHospitalDeathNegative(startIndexInclusive, endIndexExclusive);
        List<Outcome> startEndOutcomes = new ArrayList<>(outcomes.values()).subList(startIndexInclusive, endIndexExclusive);

        return summaryString(positiveCount, negativeCount, startEndOutcomes.size());
    } // classImbalanceInHospitalDeath


    private String summaryString(int positiveCount, int negativeCount, int numDataInstances)
    {
        double positiveImbalancePercent = Utils.format("#.##", RoundingMode.HALF_UP,
                100.f * (positiveCount / (numDataInstances * 1.0)));
        double negativeImbalancePercentage = Utils.format("#.##", RoundingMode.HALF_UP,
                100.f * (negativeCount / (numDataInstances * 1.0)));

        return "Death in %: " + positiveImbalancePercent + " %" + ", Survival in %: " + negativeImbalancePercentage + " %"
                + "\nDeath count: " + positiveCount + ", Survival count: " + negativeCount;
    }


    //to count between different files, for example the first 4000 files belong to set-a, the next set-b and the next set-c
    public int countInHospitalDeathPositive(int startIndexInclusive, int endIndexExclusive)
    {
        int count = 0;
        List<Outcome> startEndOutcomes = new ArrayList<>(outcomes.values()).subList(startIndexInclusive, endIndexExclusive);
        for(Outcome outcome : startEndOutcomes)
        {
            count += outcome.getInHospitalDeath0Or1();
        } // for

        return count;
    } // countInHospitalDeathPositive

    public int countInHospitalDeathNegative(int startIndexInclusive, int endIndexExclusive)
    {
        List<Outcome> startEndOutcomes = new ArrayList<>(outcomes.values()).subList(startIndexInclusive, endIndexExclusive);
        return startEndOutcomes.size() - countInHospitalDeathPositive(startIndexInclusive, endIndexExclusive);
    } // countInHospitalDeathNegative


    //get the dataset this "outcomes" belongs to
    public Dataset getDataset() {
        return dataset;
    }


    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return dataset == ((Outcomes) o).getDataset();
    }

    //hash code is determined by the dataset, since the same dataset cannot have two "outcomes" instances
    @Override
    public int hashCode() {
        return Objects.hash(dataset);
    }

    //for comparing two "outcomes" instances
    public int compareTo(Outcomes other)
    {
        return dataset.compareTo(other.getDataset());
    } // compareTo


    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        int count = 0;
        for(Outcome outcome : outcomes.values())
        {
            count ++;
            sb.append(count).append(" -> ").append(outcome).append("\n");
        } // for

        return sb.toString();
    } // toString


    //TODO Outcome class can be defined as a private class inside this class
    //private Outcome instance can be instantiated from getInstance() method from its parameters
    //which will have the same parameters as the constructor of Outcome class

} // class Outcomes
