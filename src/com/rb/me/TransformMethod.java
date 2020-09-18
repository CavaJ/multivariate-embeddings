package com.rb.me;

//enum for transformation methods
public enum TransformMethod
{
    TRANSFORM_MINIMUM,
    TRANSFORM_MAXIMUM,
    TRANSFORM_MINIMAX,
    TRANSFORM_MEAN,
    TRANSFORM_WEIGHTED_MEAN,
    TRANSFORM_MEDIAN,
    TRANSFORM_MODE,
    TRANSFORM_STD,
    TRANSFORM_WEIGHTED_STD,
    TRANSFORM_VARIANCE,
    TRANSFORM_WEIGHTED_VARIANCE,
    TRANSFORM_RANGE,
    TRANSFORM_GEOMETRIC_CENTER,
    TRANSFORM_GEOMETRIC_MEAN,
    TRANSFORM_WEIGHTED_GEOMETRIC_MEAN,
    TRANSFORM_KURTOSIS,
    TRANSFORM_SKEWNESS,
    TRANSFORM_AVG_POWER,
    TRANSFORM_RMS,
    TRANSFORM_ESD,
    TRANSFORM_SUPER_13,
    TRANSFORM_SUPER_17,
    TRANSFORM_SUPER_13_UNWEIGHTED,
    TRANSFORM_BEST,
    TRANSFORM_FLAT,
    TRANSFORM_MIL;

    public MTSE transform(MTSE mtse)
    {
        switch(this)
        {
            case TRANSFORM_MINIMUM:
                return mtse.transformToMin();
            case TRANSFORM_MAXIMUM:
                return mtse.transformToMax();
            case TRANSFORM_MINIMAX:
                return mtse.transformToMinimax();
            case TRANSFORM_MEAN:
                return mtse.transformToMean(false);
            case TRANSFORM_WEIGHTED_MEAN:
                return mtse.transformToMean(true);
            case TRANSFORM_MEDIAN:
                return mtse.transformToMedian();
            case TRANSFORM_MODE:
                return mtse.transformToMode();
            case TRANSFORM_STD:
                return mtse.transformToStd(false);
            case TRANSFORM_WEIGHTED_STD:
                return mtse.transformToStd(true);
            case TRANSFORM_VARIANCE:
                return mtse.transformToVariance(false);
            case TRANSFORM_WEIGHTED_VARIANCE:
                return mtse.transformToVariance(true);
            case TRANSFORM_RANGE:
                return mtse.transformToRange();
            case TRANSFORM_GEOMETRIC_CENTER:
                return mtse.transformToGeometricCenter();
            case TRANSFORM_GEOMETRIC_MEAN:
                return mtse.transformToGeometricMean(false);
            case TRANSFORM_WEIGHTED_GEOMETRIC_MEAN:
                return mtse.transformToGeometricMean(true);
            case TRANSFORM_KURTOSIS:
                return mtse.transformToKurtosis();
            case TRANSFORM_SKEWNESS:
                return mtse.transformToSkewness();
            case TRANSFORM_AVG_POWER:
                return mtse.transformToAveragedPower();
            case TRANSFORM_RMS:
                return mtse.transformToRMS();
            case TRANSFORM_ESD:
                return mtse.transformToESD();
            case TRANSFORM_SUPER_13:
                return mtse.transformToSuper13();
            case TRANSFORM_SUPER_17:
                return mtse.transformToSuper17();
            case TRANSFORM_SUPER_13_UNWEIGHTED:
                return mtse.transformToSuper13Unweighted();
            case TRANSFORM_BEST:
                return mtse.transformToBest();
            case TRANSFORM_FLAT:
                return mtse.transformToFlat();
            default:
                return mtse; // return mtse itself (in case of MIL too)
        } // switch
    } // transform

    public String simpleName()
    {
        switch(this)
        {
            case TRANSFORM_FLAT: return "flat";
            case TRANSFORM_SUPER_13: return "super13";
            case TRANSFORM_SUPER_17: return "super17";
            case TRANSFORM_BEST: return "best";
            case TRANSFORM_MIL: return "mil";
            default: return "default";
        }
    }

    public static TransformMethod[] valuesForSuper13()
    {
        return new TransformMethod[]
                {
                        TRANSFORM_MINIMUM,
                        TRANSFORM_MAXIMUM,
                        //TRANSFORM_MINIMAX,
                        //TRANSFORM_MEAN,
                        TRANSFORM_WEIGHTED_MEAN,
                        TRANSFORM_MEDIAN,
                        TRANSFORM_MODE,
                        //TRANSFORM_STD,
                        TRANSFORM_WEIGHTED_STD,
                        //TRANSFORM_VARIANCE,
                        TRANSFORM_WEIGHTED_VARIANCE,
                        TRANSFORM_RANGE,
                        TRANSFORM_GEOMETRIC_CENTER,
                        //TRANSFORM_GEOMETRIC_MEAN,
                        TRANSFORM_WEIGHTED_GEOMETRIC_MEAN,
                        //TRANSFORM_KURTOSIS,
                        //TRANSFORM_SKEWNESS,
                        TRANSFORM_AVG_POWER,
                        TRANSFORM_RMS,
                        TRANSFORM_ESD
                };
    } // valuesForSuper13


    public static TransformMethod[] valuesForSuper13Unweighted()
    {
        return new TransformMethod[]
                {
                        TRANSFORM_MINIMUM,
                        TRANSFORM_MAXIMUM,
                        //TRANSFORM_MINIMAX,
                        TRANSFORM_MEAN,
                        //TRANSFORM_WEIGHTED_MEAN,
                        TRANSFORM_MEDIAN,
                        TRANSFORM_MODE,
                        TRANSFORM_STD,
                        //TRANSFORM_WEIGHTED_STD,
                        TRANSFORM_VARIANCE,
                        //TRANSFORM_WEIGHTED_VARIANCE,
                        TRANSFORM_RANGE,
                        TRANSFORM_GEOMETRIC_CENTER,
                        TRANSFORM_GEOMETRIC_MEAN,
                        //TRANSFORM_WEIGHTED_GEOMETRIC_MEAN,
                        //TRANSFORM_KURTOSIS,
                        //TRANSFORM_SKEWNESS,
                        TRANSFORM_AVG_POWER,
                        TRANSFORM_RMS,
                        TRANSFORM_ESD
                };
    } // valuesForSuper13Unweighted


    public static TransformMethod[] valuesForSuper17()
    {
        return new TransformMethod[]
                {
                        TRANSFORM_MINIMUM,
                        TRANSFORM_MAXIMUM,
                        //TRANSFORM_MINIMAX,
                        TRANSFORM_MEAN,
                        TRANSFORM_WEIGHTED_MEAN,
                        TRANSFORM_MEDIAN,
                        TRANSFORM_MODE,
                        TRANSFORM_STD,
                        TRANSFORM_WEIGHTED_STD,
                        TRANSFORM_VARIANCE,
                        TRANSFORM_WEIGHTED_VARIANCE,
                        TRANSFORM_RANGE,
                        TRANSFORM_GEOMETRIC_CENTER,
                        TRANSFORM_GEOMETRIC_MEAN,
                        TRANSFORM_WEIGHTED_GEOMETRIC_MEAN,
                        //TRANSFORM_KURTOSIS,
                        //TRANSFORM_SKEWNESS,
                        TRANSFORM_AVG_POWER,
                        TRANSFORM_RMS,
                        TRANSFORM_ESD
                };
    } // valuesForSuper17


    public static TransformMethod[] valuesForBest()
    {
        return new TransformMethod[]
                {
                        TRANSFORM_MEAN,
                        TRANSFORM_WEIGHTED_MEAN,
                        TRANSFORM_WEIGHTED_GEOMETRIC_MEAN,
                        TRANSFORM_AVG_POWER,
                        TRANSFORM_RMS
                };
    } // valuesForBest
} // TransformMethod
