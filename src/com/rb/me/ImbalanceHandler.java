package com.rb.me;

import weka.core.Instances;

//enum to handle class imbalances
public enum ImbalanceHandler
{
    NONE,
    UNDERSAMPLE_50_50,
    SMOTE;

    public Instances apply(Instances data, boolean verbose) throws Exception
    {
        switch(this)
        {
            case UNDERSAMPLE_50_50:
                return Utils.underSample(data, verbose);
            case SMOTE:
                return Utils.smote(data, verbose); // TODO smote parameters can be adjusted to have 50%-50%
            default:
                return data;
        } // switch
    }
} // enum ImbalanceHandler
