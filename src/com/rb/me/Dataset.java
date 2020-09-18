package com.rb.me;

import java.io.Serializable;

public enum Dataset implements Serializable
{
    PhysioNet,
    MIMIC_III;

    //TODO implement description methods for the datasets
    public String toSimpleString()
    {
        switch(this)
        {
            case PhysioNet:
                return "physionet_dataset";
            case MIMIC_III:
                return "mimic_iii_dataset";
            default:
                return "unknown_dataset";
        }
    } // toDescString

} // enum Dataset
