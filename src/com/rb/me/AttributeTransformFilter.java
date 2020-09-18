package com.rb.me;

import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;

//enum to for attribute transformations in weka
public enum AttributeTransformFilter
{
    NONE,
    STANDARDIZE,
    NORMALIZE_BETWEEN_ZERO_AND_ONE,
    NORMALIZE_BETWEEN_MINUS_ONE_AND_ONE;

    public Instances apply(Instances data, boolean verbose) throws Exception
    {
        switch (this)
        {
            case STANDARDIZE:
                //applies standardisation with zero mean and unit variance
                Standardize standardizeFilter = new Standardize();
                standardizeFilter.setInputFormat(data);
                if(verbose) System.out.println("Standardising data with zero mean and unit variance...");
                return Filter.useFilter(data, standardizeFilter);
            case NORMALIZE_BETWEEN_ZERO_AND_ONE:
                //by default scale = 1.0 and translation is 0.0, so values are in the range of [0,1]
                Normalize filter1 = new Normalize();
                filter1.setScale(1.0);
                filter1.setTranslation(0.0);
                filter1.setInputFormat(data);
                if(verbose) System.out.println("Normalizing data in the interval of [0, 1]...");
                return Filter.useFilter(data, filter1);
            case NORMALIZE_BETWEEN_MINUS_ONE_AND_ONE:
                //with scale = 2.0 and translation = -1.0 you get values in the range [-1,+1].
                Normalize filter2 = new Normalize();
                filter2.setScale(2.0);
                filter2.setTranslation(-1.0);
                filter2.setInputFormat(data);
                if(verbose) System.out.println("Normalizing data in the interval of [-1, 1]...");
                return Filter.useFilter(data, filter2);
            default:
                return data;
        } // switch

    } // apply

} // enum AttributeTransformFilter
