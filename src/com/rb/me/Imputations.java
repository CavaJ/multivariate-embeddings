package com.rb.me;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//TODO you can use parallelStream API to reduce the imputation time
//helper class to handle different kinds of value imputations
public class Imputations
{
    private static Imputations singleton = null;

    public enum ImputeMethod
    {
        ZERO_IMPUTATION,
        VALID_LOW_IMPUTATION,
        NORMAL_VALUE_IMPUTATION,
        MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION,
        LIPTON_FORWARD_FILLING_IMPUTATION;


        public String toString()
        {
            switch (this)
            {
                case ZERO_IMPUTATION:
                    return "zero_imputation";
                case VALID_LOW_IMPUTATION:
                    return "valid_low_imputation";
                case NORMAL_VALUE_IMPUTATION:
                    return "normal_value_imputation";
                case MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION:
                    return "mean_imputation";
                case LIPTON_FORWARD_FILLING_IMPUTATION:
                    return "forward_imputation";
                default:
                    return "unknown_imputation";
            } // switch
        } // toString

        public String toActionString()
        {
            return toString().replace("imputation", "imputed");
        } // toString
    } // enum


    private Imputations() {}
    public static Imputations getInstance()
    {
        if(singleton == null) singleton = new Imputations();
        return singleton;
    } // getInstance



    //helper method to impute the list of multivariate time series
    public List<MTSE> imputeParallel(List<MTSE> mtseList, ImputeMethod imputeMethod, int[] trainingSetStartIndexIncEndIndexExc,
                             VarRanges varRanges, double currentMissingValuePlaceHolder)
    {
        long startProg = System.currentTimeMillis();

        //deepCopy parallelization
        List<MTSE> dcList = mtseList.parallelStream().map(MTSE::deepCopy).collect(Collectors.toList());


        System.out.println(imputeMethod + " started...");

        switch(imputeMethod)
        {
            case ZERO_IMPUTATION:
                dcList.parallelStream().forEach(mtse ->
                {
                    Map<String, List<Double>> varValuesInTsOrder = mtse.getVarValuesInTsOrder();
                    //for each var impute its values
                    for (String var : varValuesInTsOrder.keySet()) {
                        List<Double> valuesInTsOrder = varValuesInTsOrder.get(var);
                        for (int tsIndex = 0; tsIndex < valuesInTsOrder.size(); tsIndex++) {
                            if (Double.compare(valuesInTsOrder.get(tsIndex), currentMissingValuePlaceHolder) == 0)
                                valuesInTsOrder.set(tsIndex, 0.0);
                        } // for each tsIndex


                        //update the map with changes
                        varValuesInTsOrder.put(var, valuesInTsOrder);
                    } // for each var


                    //update the mtse
                    mtse.setVarValuesInTsOrder(varValuesInTsOrder);
                });
                break;
            case VALID_LOW_IMPUTATION:
                dcList.parallelStream().forEach(mtse ->
                {
                    Map<String, List<Double>> varValuesInTsOrder = mtse.getVarValuesInTsOrder();
                    //for each var impute its values
                    for (String var : varValuesInTsOrder.keySet())
                    {
                        List<Double> valuesInTsOrder = varValuesInTsOrder.get(var);
                        for (int tsIndex = 0; tsIndex < valuesInTsOrder.size(); tsIndex++) {
                            if (Double.compare(valuesInTsOrder.get(tsIndex), currentMissingValuePlaceHolder) == 0)
                                valuesInTsOrder.set(tsIndex, varRanges.getRanges(var).getValidLow());
                        } // for each tsIndex


                        //update the map with changes
                        varValuesInTsOrder.put(var, valuesInTsOrder);
                    } // for each var


                    //update the mtse
                    mtse.setVarValuesInTsOrder(varValuesInTsOrder);
                }); // for each mtse
                break;
            case NORMAL_VALUE_IMPUTATION:
                dcList.parallelStream().forEach(mtse ->
                {
                    Map<String, List<Double>> varValuesInTsOrder = mtse.getVarValuesInTsOrder();
                    //for each var impute its values
                    for (String var : varValuesInTsOrder.keySet()) {
                        List<Double> valuesInTsOrder = varValuesInTsOrder.get(var);
                        for (int tsIndex = 0; tsIndex < valuesInTsOrder.size(); tsIndex++) {
                            if (Double.compare(valuesInTsOrder.get(tsIndex), currentMissingValuePlaceHolder) == 0)
                                valuesInTsOrder.set(tsIndex, varRanges.getRanges(var).getNormal());
                        } // for each tsIndex


                        //update the map with changes
                        varValuesInTsOrder.put(var, valuesInTsOrder);
                    } // for each var


                    //update the mtse
                    mtse.setVarValuesInTsOrder(varValuesInTsOrder);
                }); // for each mtse
                break;
            case MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION:
                //get mean for each variable as a map
                //mean value should be calculated in a training set, then applied to test set
                //in the case of Physionet, it will be calculated on set-a, then will be applied to set-b and set-c
                //Map<String, Double> varMeanMap = Utils.meansOfVariablesUsingMaskingVector(dcList);
                Map<String, Double> varMeanMap
                        = Utils.meansOfVariablesUsingMaskingVector(dcList.subList(trainingSetStartIndexIncEndIndexExc[0], trainingSetStartIndexIncEndIndexExc[1]));
                dcList.parallelStream().forEach(mtse ->
                {
                    Map<String, List<Double>> varValuesInTsOrder = mtse.getVarValuesInTsOrder();
                    Map<String, List<Integer>> maskingsInTsOrder = mtse.getMaskingsInTsOrder();

                    //for each var impute its values
                    for(String var : varValuesInTsOrder.keySet())
                    {
                        List<Double> valuesInTsOrderForThisVar = varValuesInTsOrder.get(var);
                        List<Integer> maskingInTsOrderForThisVar = maskingsInTsOrder.get(var);


                        for(int tsIndex = 0; tsIndex < valuesInTsOrderForThisVar.size(); tsIndex ++)
                        {
                            double imputedVarValueInThisTs
                                    = maskingInTsOrderForThisVar.get(tsIndex)
                                    * valuesInTsOrderForThisVar.get(tsIndex)
                                    + (1 - maskingInTsOrderForThisVar.get(tsIndex)) * varMeanMap.get(var);

                            valuesInTsOrderForThisVar.set(tsIndex, imputedVarValueInThisTs);
                        } // for each tsIndex


                        //update the map with changes
                        varValuesInTsOrder.put(var, valuesInTsOrderForThisVar);
                    } // for each var


                    //update the mtse
                    mtse.setVarValuesInTsOrder(varValuesInTsOrder);
                }); // for each mtse
                break;
            case LIPTON_FORWARD_FILLING_IMPUTATION:
                //use the median obtained from training dataset for test data set during imputation of test dataset
                //compute on set-A and apply it on set-B and set-C
                Map<String, Double> varMedianMap
                        = Utils.mediansOfVariables(dcList.subList(trainingSetStartIndexIncEndIndexExc[0], trainingSetStartIndexIncEndIndexExc[1]));
                dcList.parallelStream().forEach(mtse ->
                {
                    //obtain var values in ts order
                    Map<String, List<Double>> varValuesInTsOrder = mtse.getVarValuesInTsOrder();
                    Map<String, List<Integer>> maskingsInTsOrder = mtse.getMaskingsInTsOrder();
                    for(String var : varValuesInTsOrder.keySet())
                    {
                        Double medianForThisVar = varMedianMap.get(var);

                        List<Double> valuesInTsOrderForThisVar = varValuesInTsOrder.get(var);
                        List<Integer> maskingsInTsOrderForThisVar = maskingsInTsOrder.get(var);

                        //masking value with zero represent missing value
                        List<Integer> missingValueIndices = new ArrayList<>();
                        for(int index = 0; index < maskingsInTsOrderForThisVar.size(); index ++)
                        {
                            int masking = maskingsInTsOrderForThisVar.get(index);
                            if(masking == 0) missingValueIndices.add(index);
                        } // for


                        //for each missing value index check whether there is a measurement before that
                        OUTER: for(int missingValueIndex : missingValueIndices)
                        {
                            if(missingValueIndex == 0)
                            {
                                valuesInTsOrderForThisVar.set(missingValueIndex, medianForThisVar);
                                continue; // go to the next missing value
                            }


                            for (int presentValueIndex = missingValueIndex - 1; presentValueIndex >= 0; presentValueIndex --)
                            {
                                if(maskingsInTsOrderForThisVar.get(presentValueIndex) == 1) // if there is a previously recorded measurement
                                {
                                    Double previouslyRecordedValue = valuesInTsOrderForThisVar.get(presentValueIndex);
                                    valuesInTsOrderForThisVar.set(missingValueIndex, previouslyRecordedValue);
                                    continue OUTER; // break this loop, because previous existing value found, go to the next missing value
                                } // if
                            } // for each presentValueIndex

                            //at this point no previously recorded measurement found, set the median as a value
                            valuesInTsOrderForThisVar.set(missingValueIndex, medianForThisVar);
                        } // for each missing value index


                        //update the map with changes
                        varValuesInTsOrder.put(var, valuesInTsOrderForThisVar);
                    } // for each var


                    //update the mtse
                    mtse.setVarValuesInTsOrder(varValuesInTsOrder);
                }); // for each mtse
                break;
        } // switch


        System.out.println(imputeMethod + " finished...");

        long endProg = System.currentTimeMillis();
        System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(endProg-startProg) + " seconds for imputation");

        return dcList;
    } // imputeParallel



    //helper method to impute the list of multivariate time series
    public List<MTSE> impute(List<MTSE> mtseList, ImputeMethod imputeMethod, int[] trainingSetStartIndexIncEndIndexExc,
                             VarRanges varRanges, double currentMissingValuePlaceHolder)
    {
        long startProg = System.currentTimeMillis();


        //obtain deep copy of the given mtse instances
        List<MTSE> dcList = new ArrayList<>();
        for(MTSE mtse : mtseList)
            dcList.add(mtse.deepCopy());

        System.out.println(imputeMethod + " started...");

        switch(imputeMethod)
        {
            case ZERO_IMPUTATION:
                for(MTSE mtse : dcList)
                {
                    Map<String, List<Double>> varValuesInTsOrder = mtse.getVarValuesInTsOrder();
                    //for each var impute its values
                    for(String var : varValuesInTsOrder.keySet())
                    {
                        List<Double> valuesInTsOrder = varValuesInTsOrder.get(var);
                        for(int tsIndex = 0; tsIndex < valuesInTsOrder.size(); tsIndex ++)
                        {
                            if (Double.compare(valuesInTsOrder.get(tsIndex), currentMissingValuePlaceHolder) == 0)
                                valuesInTsOrder.set(tsIndex, 0.0);
                        } // for each tsIndex


                        //update the map with changes
                        varValuesInTsOrder.put(var, valuesInTsOrder);
                    } // for each var


                    //update the mtse
                    mtse.setVarValuesInTsOrder(varValuesInTsOrder);
                } // for each mtse
                break;
            case VALID_LOW_IMPUTATION:
                for(MTSE mtse : dcList)
                {
                    Map<String, List<Double>> varValuesInTsOrder = mtse.getVarValuesInTsOrder();
                    //for each var impute its values
                    for(String var : varValuesInTsOrder.keySet())
                    {
                        List<Double> valuesInTsOrder = varValuesInTsOrder.get(var);
                        for(int tsIndex = 0; tsIndex < valuesInTsOrder.size(); tsIndex ++)
                        {
                            if (Double.compare(valuesInTsOrder.get(tsIndex), currentMissingValuePlaceHolder) == 0)
                                valuesInTsOrder.set(tsIndex, varRanges.getRanges(var).getValidLow());
                        } // for each tsIndex


                        //update the map with changes
                        varValuesInTsOrder.put(var, valuesInTsOrder);
                    } // for each var


                    //update the mtse
                    mtse.setVarValuesInTsOrder(varValuesInTsOrder);
                } // for each mtse
                break;
            case NORMAL_VALUE_IMPUTATION:
                for(MTSE mtse : dcList)
                {
                    Map<String, List<Double>> varValuesInTsOrder = mtse.getVarValuesInTsOrder();
                    //for each var impute its values
                    for(String var : varValuesInTsOrder.keySet())
                    {
                        List<Double> valuesInTsOrder = varValuesInTsOrder.get(var);
                        for(int tsIndex = 0; tsIndex < valuesInTsOrder.size(); tsIndex ++)
                        {
                            if (Double.compare(valuesInTsOrder.get(tsIndex), currentMissingValuePlaceHolder) == 0)
                                valuesInTsOrder.set(tsIndex, varRanges.getRanges(var).getNormal());
                        } // for each tsIndex


                        //update the map with changes
                        varValuesInTsOrder.put(var, valuesInTsOrder);
                    } // for each var


                    //update the mtse
                    mtse.setVarValuesInTsOrder(varValuesInTsOrder);
                } // for each mtse
                break;
            case MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION:
                //get mean for each variable as a map
                //mean value should be calculated in a training set, then applied to test set
                //in the case of Physionet, it will be calculated on set-a, then will be applied to set-b and set-c
                //Map<String, Double> varMeanMap = Utils.meansOfVariablesUsingMaskingVector(dcList);
                Map<String, Double> varMeanMap
                        = Utils.meansOfVariablesUsingMaskingVector(dcList.subList(trainingSetStartIndexIncEndIndexExc[0], trainingSetStartIndexIncEndIndexExc[1]));
                for(MTSE mtse : dcList)
                {
                    Map<String, List<Double>> varValuesInTsOrder = mtse.getVarValuesInTsOrder();
                    Map<String, List<Integer>> maskingsInTsOrder = mtse.getMaskingsInTsOrder();

                    //for each var impute its values
                    for(String var : varValuesInTsOrder.keySet())
                    {
                        List<Double> valuesInTsOrderForThisVar = varValuesInTsOrder.get(var);
                        List<Integer> maskingInTsOrderForThisVar = maskingsInTsOrder.get(var);


                        for(int tsIndex = 0; tsIndex < valuesInTsOrderForThisVar.size(); tsIndex ++)
                        {
                            double imputedVarValueInThisTs
                                    = maskingInTsOrderForThisVar.get(tsIndex)
                                    * valuesInTsOrderForThisVar.get(tsIndex)
                                    + (1 - maskingInTsOrderForThisVar.get(tsIndex)) * varMeanMap.get(var);

                                valuesInTsOrderForThisVar.set(tsIndex, imputedVarValueInThisTs);
                        } // for each tsIndex


                        //update the map with changes
                        varValuesInTsOrder.put(var, valuesInTsOrderForThisVar);
                    } // for each var


                    //update the mtse
                    mtse.setVarValuesInTsOrder(varValuesInTsOrder);
                } // for each mtse
                break;
            case LIPTON_FORWARD_FILLING_IMPUTATION:
                //use the median obtained from training dataset for test data set during imputation of test dataset
                //compute on set-A and apply it on set-B and set-C
                Map<String, Double> varMedianMap
                        = Utils.mediansOfVariables(dcList.subList(trainingSetStartIndexIncEndIndexExc[0], trainingSetStartIndexIncEndIndexExc[1]));
                for(MTSE mtse : dcList)
                {
                    //obtain var values in ts order
                    Map<String, List<Double>> varValuesInTsOrder = mtse.getVarValuesInTsOrder();
                    Map<String, List<Integer>> maskingsInTsOrder = mtse.getMaskingsInTsOrder();
                    for(String var : varValuesInTsOrder.keySet())
                    {
                        Double medianForThisVar = varMedianMap.get(var);

                        List<Double> valuesInTsOrderForThisVar = varValuesInTsOrder.get(var);
                        List<Integer> maskingsInTsOrderForThisVar = maskingsInTsOrder.get(var);

                        //masking value with zero represent missing value
                        List<Integer> missingValueIndices = new ArrayList<>();
                        for(int index = 0; index < maskingsInTsOrderForThisVar.size(); index ++)
                        {
                            int masking = maskingsInTsOrderForThisVar.get(index);
                            if(masking == 0) missingValueIndices.add(index);
                        } // for


                        //for each missing value index check whether there is a measurement before that
                        OUTER: for(int missingValueIndex : missingValueIndices)
                        {
                            if(missingValueIndex == 0)
                            {
                                valuesInTsOrderForThisVar.set(missingValueIndex, medianForThisVar);
                                continue; // go to the next missing value
                            }


                            for (int presentValueIndex = missingValueIndex - 1; presentValueIndex >= 0; presentValueIndex --)
                            {
                                if(maskingsInTsOrderForThisVar.get(presentValueIndex) == 1) // if there is a previously recorded measurement
                                {
                                    Double previouslyRecordedValue = valuesInTsOrderForThisVar.get(presentValueIndex);
                                    valuesInTsOrderForThisVar.set(missingValueIndex, previouslyRecordedValue);
                                    continue OUTER; // break this loop, because previous existing value found, go to the next missing value
                                } // if
                            } // for each presentValueIndex

                            //at this point no previously recorded measurement found, set the median as a value
                            valuesInTsOrderForThisVar.set(missingValueIndex, medianForThisVar);
                        } // for each missing value index


                        //update the map with changes
                        varValuesInTsOrder.put(var, valuesInTsOrderForThisVar);
                    } // for each var


                    //update the mtse
                    mtse.setVarValuesInTsOrder(varValuesInTsOrder);
                } // for each mtse
                break;
        } // switch


        System.out.println(imputeMethod + " finished...");


        long endProg = System.currentTimeMillis();
        System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(endProg-startProg) + " seconds for imputation");


        return dcList;
    } // impute

} // class Imputations
