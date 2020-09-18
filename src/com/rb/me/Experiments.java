package com.rb.me;

import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SPegasos;
import weka.classifiers.meta.Bagging;
import weka.classifiers.meta.RealAdaBoost;
import weka.classifiers.mi.MIBoost;
import weka.classifiers.mi.MILR;
import weka.classifiers.mi.MIWrapper;
import weka.classifiers.mi.SimpleMI;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MultiInstanceToPropositional;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//list of experiments performed for each configuration
public class Experiments
{
    public static void run_MILR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                     int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                     boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                            outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

//        //print weights of instances of the first bag
//        System.out.println();
//        Utils.printInstanceWeightsOfABag(train.instance(0));
//        System.out.println("\nBag weights of the first 10 bags: ");
//        //print the weights of first 10 bags
//        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);

        //------- MILR ----------------
        //NO FILTERING OCCURS INSIDE "MILR" CLASS; e.g. MultiInstanceToPropositional or PropositionalToMultiInstance
        MILR cls = new MILR(); //ridge parameter (-R) can be selected using hyperparameter selection
        //-A [0|1|2]
        //  Defines the type of algorithm (default 0):
        //   0. standard MI assumption
        //   1. collective MI assumption, arithmetic mean for posteriors
        //   2. collective MI assumption, geometric mean for posteriors
        cls.setOptions(weka.core.Utils.splitOptions("-A 2")); //geometric average, put -D for debugging output
        System.out.println(cls.getClass().getName() + " classifier options => " + Arrays.toString(cls.getOptions()) + "\n");
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_MILR_SS




    public static void run_BG_MILR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                        int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                        boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);

        //------- MILR ----------------
        //NO FILTERING OCCURS INSIDE "MILR" CLASS; e.g. MultiInstanceToPropositional or PropositionalToMultiInstance
        MILR milr = new MILR(); //ridge parameter (-R) can be selected using hyperparameter selection
        //-A [0|1|2]
        //  Defines the type of algorithm (default 0):
        //   0. standard MI assumption
        //   1. collective MI assumption, arithmetic mean for posteriors
        //   2. collective MI assumption, geometric mean for posteriors
        milr.setOptions(weka.core.Utils.splitOptions("-A 2")); //geometric average, put -D for debugging output
        Bagging cls = new Bagging(); //IMPROVES MILR PERFORMANCE with 10 iterations
        cls.setOptions(weka.core.Utils.splitOptions("-I 10 -num-slots 4")); // num execution slots in parallel
        cls.setClassifier(milr);
        System.out.println(cls.getClass().getName() + " classifier options => " + Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_BG_MILR_SS



//    public static void run_W_MILR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
//                                       int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
//                                       boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
//    {
//        //forward impute the multivariate time series
//        //mtses = Imputations.getInstance()
//                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);
//
//        //System.out.println(mtses.get(0).toVerticalString());
//        //System.exit(0);
//
//        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
//                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-a");
//
//        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
//                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-b");
//
//        //clear mtses
//        //mtses.clear();
//        //mtses = null;
//
//
//        if (setAData.classIndex() == -1)
//            setAData.setClassIndex(setAData.numAttributes() - 1);
//        if (setBData.classIndex() == -1)
//            setBData.setClassIndex(setBData.numAttributes() - 1);
//
//
//        //reweight the instances in each bag according to their timestamp minutes
//        setAData = Utils.reweightInstancesOfEachBagByTs(setAData, keepTsAsAVariable);
//        setBData = Utils.reweightInstancesOfEachBagByTs(setBData, keepTsAsAVariable);
//
//
//        Instances train = new Instances(setAData);
//        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));
//
//        //undersample
//        train = Utils.underSample(train, true);
//
//        //print weights of instances of the first bag
//        System.out.println();
//        Utils.printInstanceWeightsOfABag(train.instance(0));
//        System.out.println("\nBag weights of the first 10 bags: ");
//        //print the weights of first 10 bags
//        Utils.printBagWeights(train, 0, 10);
//
//
//        //copy test data
//        Instances test = new Instances(setBData);
//
//
//        //WeightedInstancesHandlerWrapper does not have meaning for MILR, since it will take care of bag weights instead of weights of inner bag instances
//
//
//        //------- MILR ----------------
//        //NO FILTERING OCCURS INSIDE "MILR" CLASS; e.g. MultiInstanceToPropositional or PropositionalToMultiInstance
//        MILR cls = new MILR(); //ridge parameter (-R) can be selected using hyperparameter selection
//        //-A [0|1|2]
//        //  Defines the type of algorithm (default 0):
//        //   0. standard MI assumption
//        //   1. collective MI assumption, arithmetic mean for posteriors
//        //   2. collective MI assumption, geometric mean for posteriors
//        cls.setOptions(weka.core.Utils.splitOptions("-A 2")); //geometric average, put -D for debugging output
//        System.out.println(cls.getClass().getName() + " classifier options => " + Arrays.toString(cls.getOptions()) + "\n");
//        //-----------------------------
//
//
//        if(crossValidateOnSetAInstead)
//        {
//            //cross validate on set-a over 10-fold
//            Utils.crossValidate(cls, train, null, 1, 10, false);
//        } // if
//        else
//        {
//            //average the results over 10 runs
//            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
//        } // else
//    } // run_W_MILR_SS


//    public static void run_W_BG_MILR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
//                                          int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
//                                          boolean crossValidateOnSetAInstead, keepTsAsAVariable) throws Exception
//    {
//        //forward impute the multivariate time series
//        //mtses = Imputations.getInstance()
//                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);
//
//        //System.out.println(mtses.get(0).toVerticalString());
//        //System.exit(0);
//
//        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
//                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-a");
//
//        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
//                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-b");
//
//        //clear mtses
//        //mtses.clear();
//        //mtses = null;
//
//
//        if (setAData.classIndex() == -1)
//            setAData.setClassIndex(setAData.numAttributes() - 1);
//        if (setBData.classIndex() == -1)
//            setBData.setClassIndex(setBData.numAttributes() - 1);
//
//
//        //reweight the instances in each bag according to their timestamp minutes
//        setAData = Utils.reweightInstancesOfEachBagByTs(setAData, keepTsAsAVariable);
//        setBData = Utils.reweightInstancesOfEachBagByTs(setBData, keepTsAsAVariable);
//
//
//        Instances train = new Instances(setAData);
//        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));
//
//        //undersample
//        train = Utils.underSample(train, true);
//
//        //print weights of instances of the first bag
//        System.out.println();
//        Utils.printInstanceWeightsOfABag(train.instance(0));
//        System.out.println("\nBag weights of the first 10 bags: ");
//        //print the weights of first 10 bags
//        Utils.printBagWeights(train, 0, 10);
//
//
//        //copy test data
//        Instances test = new Instances(setBData);
//
//        //------- MILR ----------------
//        //NO FILTERING OCCURS INSIDE "MILR" CLASS; e.g. MultiInstanceToPropositional or PropositionalToMultiInstance
//        MILR milr = new MILR(); //ridge parameter (-R) can be selected using hyperparameter selection
//        //-A [0|1|2]
//        //  Defines the type of algorithm (default 0):
//        //   0. standard MI assumption
//        //   1. collective MI assumption, arithmetic mean for posteriors
//        //   2. collective MI assumption, geometric mean for posteriors
//        milr.setOptions(weka.core.Utils.splitOptions("-A 2")); //geometric average, put -D for debugging output
//        Bagging cls = new Bagging(); //IMPROVES MILR PERFORMANCE with 10 iterations
//        cls.setOptions(weka.core.Utils.splitOptions("-I 10 -num-slots 4")); // num execution slots in parallel
//        cls.setClassifier(milr);
//        System.out.println(cls.getClass().getName() + " classifier options => " + Arrays.toString(cls.getOptions()) + "\n");
//        //-----------------------------
//
//
//        if(crossValidateOnSetAInstead)
//        {
//            //cross validate on set-a over 10-fold
//            Utils.crossValidate(cls, train, null, 1, 10, false);
//        } // if
//        else
//        {
//            //average the results over 10 runs
//            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
//        } // else
//    } // run_W_BG_MILR_SS



    public static void run_MIW_RF_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                       int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                       boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

//        //print weights of instances of the first bag
//        System.out.println();
//        Utils.printInstanceWeightsOfABag(train.instance(0));
//        System.out.println("\nBag weights of the first 10 bags: ");
//        //print the weights of first 10 bags
//        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);

        //------- MIWrapper -----------
        MIWrapper cls = new MIWrapper();
        cls.setOptions(weka.core.Utils.splitOptions("-P 2 -A 1")); // P = 2 geometric average, A = 1 unit weighting for an instance inside bag
        RandomForest rf = new RandomForest();                   //TODO 1000 trees
        rf.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        cls.setClassifier(rf); // you can define other parameters for an internal classifier or perform CVParameterSelection for hyperparameter selection
        System.out.println(cls.getClass().getName() + " classifier options => " + Arrays.toString(cls.getOptions()) + "\n");
        //-P [1|2|3]
        //The method used in testing:
        //1.arithmetic average
        //2.geometric average
        //3.max probability of positive bag.
        //(default: 1)
        //
        //-A [0|1|2|3]
        //The type of weight setting for each single-instance:
        //0.keep the weight to be the same as the original value; => //TODO this weighting is actually: bagWeight / #instaces_in_the_bag => NOT RELEVANT
        //1.weight = 1.0 //TODO we choose 1 here
        //2.weight = 1.0/Total number of single-instance in the
        //corresponding bag
        //3. weight = Total number of single-instance / (Total
        //number of bags * Total number of single-instance
        //in the corresponding bag).
        //(default: 3)
        //
        //-D
        //If set, classifier is run in debug mode and
        //may output additional info to the console
        //
        //-W
        //Full name of base classifier.
        //(default: weka.classifiers.rules.ZeroR)
        //Options specific to classifier weka.classifiers.rules.ZeroR:
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_MIW_RF_SS


    public static void run_MIW_LR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                       int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                       boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);

        //------- MIWrapper -----------
        MIWrapper cls = new MIWrapper();
        cls.setOptions(weka.core.Utils.splitOptions("-P 2 -A 1")); // P = 2 geometric average, A = 1 unit weighting for an instance inside bag
        cls.setClassifier(new Logistic()); // you can define other parameters for an internal classifier or perform CVParameterSelection for hyperparameter selection
        System.out.println(cls.getClass().getName() + " classifier options => " + Arrays.toString(cls.getOptions()) + "\n");
        //-P [1|2|3]
        //The method used in testing:
        //1.arithmetic average
        //2.geometric average
        //3.max probability of positive bag.
        //(default: 1)
        //
        //-A [0|1|2|3]
        //The type of weight setting for each single-instance:
        //0.keep the weight to be the same as the original value; => //TODO this weighting is actually: bagWeight / #instaces_in_the_bag => NOT RELEVANT
        //1.weight = 1.0 //TODO we choose 1 here
        //2.weight = 1.0/Total number of single-instance in the
        //corresponding bag
        //3. weight = Total number of single-instance / (Total
        //number of bags * Total number of single-instance
        //in the corresponding bag).
        //(default: 3)
        //
        //-D
        //If set, classifier is run in debug mode and
        //may output additional info to the console
        //
        //-W
        //Full name of base classifier.
        //(default: weka.classifiers.rules.ZeroR)
        //Options specific to classifier weka.classifiers.rules.ZeroR:
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_MIW_LR_SS



    // uses timestamp based weighting
    public static void run_W_MIW_RF_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                         int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                         boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);



        //reweight by ts and then remove ts
        setAData = Utils.reweightInstancesOfEachBagByTs(setAData, keepTsAsAVariable);
        setBData = Utils.reweightInstancesOfEachBagByTs(setBData, keepTsAsAVariable);



        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);

        //------- MIWrapper -----------
        MIWrapper cls = new MIWrapper();
        cls.setOptions(weka.core.Utils.splitOptions("-P 2 -A 4")); // P = 2 geometric average, A = 4 keep original inner bag instance weights
        //cls.setDoNotCheckCapabilities(true); // to handle weighted scheme
        RandomForest rf = new RandomForest();
        rf.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        cls.setClassifier(rf); // you can define other parameters for an internal classifier or perform CVParameterSelection for hyperparameter selection
        System.out.println(cls.getClass().getName() + " classifier options => " + Arrays.toString(cls.getOptions()) + "\n");
        //-P [1|2|3]
        //The method used in testing:
        //1.arithmetic average
        //2.geometric average
        //3.max probability of positive bag.
        //(default: 1)
        //
        //-A [0|1|2|3]
        //The type of weight setting for each single-instance:
        //0.keep the weight to be the same as the original value;
        //1.weight = 1.0
        //2.weight = 1.0/Total number of single-instance in the
        //corresponding bag
        //3. weight = Total number of single-instance / (Total
        //number of bags * Total number of single-instance
        //in the corresponding bag).
        //(default: 3)
        //
        //-D
        //If set, classifier is run in debug mode and
        //may output additional info to the console
        //
        //-W
        //Full name of base classifier.
        //(default: weka.classifiers.rules.ZeroR)
        //Options specific to classifier weka.classifiers.rules.ZeroR:
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_W_MIW_RF_SS



    //uses timestamp based weighting
    public static void run_W_MIW_LR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                         int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                         boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);


        //reweight by ts and then remove ts
        setAData = Utils.reweightInstancesOfEachBagByTs(setAData, keepTsAsAVariable);
        setBData = Utils.reweightInstancesOfEachBagByTs(setBData, keepTsAsAVariable);


        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);

        //------- MIWrapper -----------
        MIWrapper cls = new MIWrapper();
        cls.setOptions(weka.core.Utils.splitOptions("-P 2 -A 4")); // P = 2 geometric average, A = 4 keep original inner bag instance weights
        //cls.setDoNotCheckCapabilities(true); // to handle weighted scheme
        cls.setClassifier(new Logistic()); // you can define other parameters for an internal classifier or perform CVParameterSelection for hyperparameter selection
        System.out.println(cls.getClass().getName() + " classifier options => " + Arrays.toString(cls.getOptions()) + "\n");
        //-P [1|2|3]
        //The method used in testing:
        //1.arithmetic average
        //2.geometric average
        //3.max probability of positive bag.
        //(default: 1)
        //
        //-A [0|1|2|3]
        //The type of weight setting for each single-instance:
        //0.keep the weight to be the same as the original value;
        //1.weight = 1.0
        //2.weight = 1.0/Total number of single-instance in the
        //corresponding bag
        //3. weight = Total number of single-instance / (Total
        //number of bags * Total number of single-instance
        //in the corresponding bag).
        //(default: 3)
        //
        //-D
        //If set, classifier is run in debug mode and
        //may output additional info to the console
        //
        //-W
        //Full name of base classifier.
        //(default: weka.classifiers.rules.ZeroR)
        //Options specific to classifier weka.classifiers.rules.ZeroR:
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_W_MIW_LR_SS



    public static void run_RB_MIW_RF_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                          int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                          boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

//        //print weights of instances of the first bag
//        System.out.println();
//        Utils.printInstanceWeightsOfABag(train.instance(0));
//        System.out.println("\nBag weights of the first 10 bags: ");
//        //print the weights of first 10 bags
//        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);



        //------- MIWrapper -----------
        MIWrapper mil = new MIWrapper();
        mil.setOptions(weka.core.Utils.splitOptions("-P 2 -A 1")); // P = 2 geometric average, A = 1 unit weighting for an instance inside bag
        RandomForest rf = new RandomForest();                       // TODO 1000 trees
        rf.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        mil.setClassifier(rf); // you can define other parameters for an internal classifier or perform CVParameterSelection for hyperparameter selection


        RealAdaBoost cls = new RealAdaBoost(); //improves MIW-RF and MIW-LR performance
        cls.setOptions(weka.core.Utils.splitOptions("-I 10")); // number of iterations 10
        cls.setClassifier(mil);


        System.out.println(cls.getClass().getName() + " classifier options => " + Arrays.toString(cls.getOptions()) + "\n");
        //-P [1|2|3]
        //The method used in testing:
        //1.arithmetic average
        //2.geometric average
        //3.max probability of positive bag.
        //(default: 1)
        //
        //-A [0|1|2|3]
        //The type of weight setting for each single-instance:
        //0.keep the weight to be the same as the original value; => //TODO this weighting is actually: bagWeight / #instaces_in_the_bag => NOT RELEVANT
        //1.weight = 1.0 //TODO we choose 1 here
        //2.weight = 1.0/Total number of single-instance in the
        //corresponding bag
        //3. weight = Total number of single-instance / (Total
        //number of bags * Total number of single-instance
        //in the corresponding bag).
        //(default: 3)
        //
        //-D
        //If set, classifier is run in debug mode and
        //may output additional info to the console
        //
        //-W
        //Full name of base classifier.
        //(default: weka.classifiers.rules.ZeroR)
        //Options specific to classifier weka.classifiers.rules.ZeroR:
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_RB_MIW_RF_SS


    public static void run_RB_MIW_LR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                          int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                          boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);

        //------- MIWrapper -----------
        MIWrapper mil = new MIWrapper();
        mil.setOptions(weka.core.Utils.splitOptions("-P 2 -A 1")); // P = 2 geometric average, A = 1 unit weighting for an instance inside bag
        mil.setClassifier(new Logistic()); // you can define other parameters for an internal classifier or perform CVParameterSelection for hyperparameter selection


        RealAdaBoost cls = new RealAdaBoost(); //improves MIW-RF and MIW-LR performance
        cls.setOptions(weka.core.Utils.splitOptions("-I 10"));
        cls.setClassifier(mil);


        System.out.println(cls.getClass().getName() + " classifier options => " + Arrays.toString(cls.getOptions()) + "\n");
        //-P [1|2|3]
        //The method used in testing:
        //1.arithmetic average
        //2.geometric average
        //3.max probability of positive bag.
        //(default: 1)
        //
        //-A [0|1|2|3]
        //The type of weight setting for each single-instance:
        //0.keep the weight to be the same as the original value; => //TODO this weighting is actually: bagWeight / #instaces_in_the_bag => NOT RELEVANT
        //1.weight = 1.0 //TODO we choose 1 here
        //2.weight = 1.0/Total number of single-instance in the
        //corresponding bag
        //3. weight = Total number of single-instance / (Total
        //number of bags * Total number of single-instance
        //in the corresponding bag).
        //(default: 3)
        //
        //-D
        //If set, classifier is run in debug mode and
        //may output additional info to the console
        //
        //-W
        //Full name of base classifier.
        //(default: weka.classifiers.rules.ZeroR)
        //Options specific to classifier weka.classifiers.rules.ZeroR:
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_RB_MIW_LR_SS


    //uses timestamp based weighting
    public static void run_W_RB_MIW_RF_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                            int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                            boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);


        //reweight by temporal order
        setAData = Utils.reweightInstancesOfEachBagByTs(setAData, keepTsAsAVariable);
        setBData = Utils.reweightInstancesOfEachBagByTs(setBData, keepTsAsAVariable);


        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);



        //------- MIWrapper -----------
        MIWrapper mil = new MIWrapper();
        mil.setOptions(weka.core.Utils.splitOptions("-P 2 -A 4")); // P = 2 geometric average, A = 4 keep original inner bag instance weights
        //mil.setDoNotCheckCapabilities(true); // to handle weighted scheme
        RandomForest rf = new RandomForest();
        rf.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        mil.setClassifier(rf); // you can define other parameters for an internal classifier or perform CVParameterSelection for hyperparameter selection


        RealAdaBoost cls = new RealAdaBoost(); //improves MIW-RF and MIW-LR performance
        cls.setOptions(weka.core.Utils.splitOptions("-I 10")); // number of iterations 10
        cls.setClassifier(mil);


        System.out.println(cls.getClass().getName() + " classifier options => " + Arrays.toString(cls.getOptions()) + "\n");
        //-P [1|2|3]
        //The method used in testing:
        //1.arithmetic average
        //2.geometric average
        //3.max probability of positive bag.
        //(default: 1)
        //
        //-A [0|1|2|3]
        //The type of weight setting for each single-instance:
        //0.keep the weight to be the same as the original value;
        //1.weight = 1.0
        //2.weight = 1.0/Total number of single-instance in the
        //corresponding bag
        //3. weight = Total number of single-instance / (Total
        //number of bags * Total number of single-instance
        //in the corresponding bag).
        //(default: 3)
        //
        //-D
        //If set, classifier is run in debug mode and
        //may output additional info to the console
        //
        //-W
        //Full name of base classifier.
        //(default: weka.classifiers.rules.ZeroR)
        //Options specific to classifier weka.classifiers.rules.ZeroR:
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_W_RB_MIW_RF_SS




    //does not use timestamp based weighting
    public static void run_W_RB_MIW_LR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                            int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                            boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);


        //reweight and then remove ts
        setAData = Utils.reweightInstancesOfEachBagByTs(setAData, keepTsAsAVariable);
        setBData = Utils.reweightInstancesOfEachBagByTs(setBData, keepTsAsAVariable);


        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);

        //------- MIWrapper -----------
        MIWrapper mil = new MIWrapper();
        mil.setOptions(weka.core.Utils.splitOptions("-P 2 -A 4")); // P = 2 geometric average, A = 4 keep original inner bag instance weights
        //mil.setDoNotCheckCapabilities(true); // to handle weighted scheme
        mil.setClassifier(new Logistic()); // you can define other parameters for an internal classifier or perform CVParameterSelection for hyperparameter selection


        RealAdaBoost cls = new RealAdaBoost(); //improves MIW-RF and MIW-LR performance
        cls.setOptions(weka.core.Utils.splitOptions("-I 10"));
        cls.setClassifier(mil);


        System.out.println(cls.getClass().getName() + " classifier options => " + Arrays.toString(cls.getOptions()) + "\n");
        //-P [1|2|3]
        //The method used in testing:
        //1.arithmetic average
        //2.geometric average
        //3.max probability of positive bag.
        //(default: 1)
        //
        //-A [0|1|2|3]
        //The type of weight setting for each single-instance:
        //0.keep the weight to be the same as the original value;
        //1.weight = 1.0
        //2.weight = 1.0/Total number of single-instance in the
        //corresponding bag
        //3. weight = Total number of single-instance / (Total
        //number of bags * Total number of single-instance
        //in the corresponding bag).
        //(default: 3)
        //
        //-D
        //If set, classifier is run in debug mode and
        //may output additional info to the console
        //
        //-W
        //Full name of base classifier.
        //(default: weka.classifiers.rules.ZeroR)
        //Options specific to classifier weka.classifiers.rules.ZeroR:
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_W_RB_MIW_LR_SS



    //uses weight method 3 of MultiInstanceToPropositional by default => NOT RELEVANT
    public static void run_MIB_RF_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                       int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                       boolean crossValidateOnSetAInstead,  boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);

        //--------- MIBoost -----------
        //MultiInstanceToPropositional to propositional filter is present inside "MIBoost" class, weight bags, but all bags have the same weights,
        //however instances are weighted irrelavantly; internally uses method 3 of MultiInstanceToPropositional
        MIBoost cls = new MIBoost();
        //cls.setWeightMethod(new SelectedTag(MultiInstanceToPropositional.WEIGHTMETHOD_1, MultiInstanceToPropositional.TAGS_WEIGHTMETHOD));
        //-B <num>
        //        The number of bins in discretization
        //    (default 0, no discretization)
        //
        //-R <num>
        //        Maximum number of boost iterations.
        //(default 10)
        //
        //-W <class name> (can also be done by setClassifier method)
        //Full name of classifier to boost.
        //eg: weka.classifiers.bayes.NaiveBayes
        cls.setOptions(weka.core.Utils.splitOptions("-R 10"));
        //OPTIONS FOR RANDOM FOREST:
        //-I <num>
        //  Number of iterations (i.e., the number of trees in the random forest).
        //  (default value 10) //tested 100
        //
        // -K <number of attributes>
        //  Number of attributes to randomly investigate. (default 0)
        //  (<1 = int(log_2(#predictors)+1)). //not possible, 0 is only possible value
        //
        // -S <num>
        //  Seed for random number generator.
        //  (default 1)
        //-N <num>
        // Number of folds for backfitting (default 0, no backfitting).
        //-B
        // Break ties randomly when several attributes look equally good.
        //-batch-size
        //  The desired batch size for batch prediction  (default 100).
        RandomForest rf = new RandomForest();
        rf.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        cls.setClassifier(rf);
        System.out.println(cls.getClass().getName() + " classifier options" + Arrays.toString(cls.getOptions()) + "\n");
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_MIB_RF_SS



    //uses weight method 3 of MultiInstanceToPropositional by default => NOT RELEVANT
    public static void run_MIB_LR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                       int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                       boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);

        //--------- MIBoost -----------
        //MultiInstanceToPropositional to propositional filter is present inside "MIBoost" class, weight bags, but all bags have the same weights,
        //however instances are weighted irrelavantly; internally uses method 3 of MultiInstanceToPropositional
        MIBoost cls = new MIBoost();
        //cls.setWeightMethod(new SelectedTag(MultiInstanceToPropositional.WEIGHTMETHOD_1, MultiInstanceToPropositional.TAGS_WEIGHTMETHOD));
        //-B <num>
        //        The number of bins in discretization
        //    (default 0, no discretization)
        //
        //-R <num>
        //        Maximum number of boost iterations.
        //(default 10)
        //
        //-W <class name> (can also be done by setClassifier method)
        //Full name of classifier to boost.
        //eg: weka.classifiers.bayes.NaiveBayes
        cls.setOptions(weka.core.Utils.splitOptions("-R 10"));
        cls.setClassifier(new Logistic());
        System.out.println(cls.getClass().getName() + " classifier options" + Arrays.toString(cls.getOptions()) + "\n");
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_MIB_LR_SS



    //uses weight method 3 of MultiInstanceToPropositional by default => NOT RELEVANT
    public static void run_W_MIB_RF_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                         int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                         boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);



        //reweight by ts and then remove ts
        setAData = Utils.reweightInstancesOfEachBagByTs(setAData, keepTsAsAVariable);
        setBData = Utils.reweightInstancesOfEachBagByTs(setBData, keepTsAsAVariable);



        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);

        //--------- MIBoost -----------
        //MultiInstanceToPropositional to propositional filter is present inside "MIBoost" class, weight bags, but all bags have the same weights,
        //however instances are weighted irrelavantly; internally uses method 3 of MultiInstanceToPropositional
        MIBoost cls = new MIBoost();
        //cls.setWeightMethod(new SelectedTag(MultiInstanceToPropositional.WEIGHTMETHOD_KEEP_INNER_BAG_INSTANCE_WEIGHTS, MultiInstanceToPropositional.TAGS_WEIGHTMETHOD));
        //cls.setDoNotCheckCapabilities(true);
        //-B <num>
        //        The number of bins in discretization
        //    (default 0, no discretization)
        //
        //-R <num>
        //        Maximum number of boost iterations.
        //(default 10)
        //
        //-W <class name> (can also be done by setClassifier method)
        //Full name of classifier to boost.
        //eg: weka.classifiers.bayes.NaiveBayes
        cls.setOptions(weka.core.Utils.splitOptions("-R 10"));
        //OPTIONS FOR RANDOM FOREST:
        //-I <num>
        //  Number of iterations (i.e., the number of trees in the random forest).
        //  (default value 10) //tested 100
        //
        // -K <number of attributes>
        //  Number of attributes to randomly investigate. (default 0)
        //  (<1 = int(log_2(#predictors)+1)). //not possible, 0 is only possible value
        //
        // -S <num>
        //  Seed for random number generator.
        //  (default 1)
        //-N <num>
        // Number of folds for backfitting (default 0, no backfitting).
        //-B
        // Break ties randomly when several attributes look equally good.
        //-batch-size
        //  The desired batch size for batch prediction  (default 100).
        RandomForest rf = new RandomForest();
        rf.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        cls.setClassifier(rf);
        System.out.println(cls.getClass().getName() + " classifier options" + Arrays.toString(cls.getOptions()) + "\n");
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_W_MIB_RF_SS




    //uses weight method 3 of MultiInstanceToPropositional by default => NOT RELEVANT
    public static void run_W_MIB_LR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                         int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                         boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);


        //reweight by ts and then remove ts
        setAData = Utils.reweightInstancesOfEachBagByTs(setAData, keepTsAsAVariable);
        setBData = Utils.reweightInstancesOfEachBagByTs(setBData, keepTsAsAVariable);


        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);

        //--------- MIBoost -----------
        //MultiInstanceToPropositional to propositional filter is present inside "MIBoost" class, weight bags, but all bags have the same weights,
        //however instances are weighted irrelavantly; internally uses method 3 of MultiInstanceToPropositional
        MIBoost cls = new MIBoost();
        //cls.setWeightMethod(new SelectedTag(MultiInstanceToPropositional.WEIGHTMETHOD_KEEP_INNER_BAG_INSTANCE_WEIGHTS, MultiInstanceToPropositional.TAGS_WEIGHTMETHOD));
        //cls.setDoNotCheckCapabilities(true);
        //-B <num>
        //        The number of bins in discretization
        //    (default 0, no discretization)
        //
        //-R <num>
        //        Maximum number of boost iterations.
        //(default 10)
        //
        //-W <class name> (can also be done by setClassifier method)
        //Full name of classifier to boost.
        //eg: weka.classifiers.bayes.NaiveBayes
        cls.setOptions(weka.core.Utils.splitOptions("-R 10"));
        cls.setClassifier(new Logistic());
        System.out.println(cls.getClass().getName() + " classifier options" + Arrays.toString(cls.getOptions()) + "\n");
        //-----------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_W_MIB_LR_SS




    //-------- SMI --------
    public static void run_SMI_RF_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                   int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                   boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);


        //-----------------------------------------
        SimpleMI cls = new SimpleMI();
        cls.setOptions(weka.core.Utils.splitOptions("-M 1")); // the best performing is arithmetic average
        //Instances newTrain = cls.transform(train);
        //for (int i = 0; i < newTrain.numInstances(); i++)
        //{
        //    println(newTrain.instance(i));
        //} // for
        //System.exit(0);

        //-M [1|2|3]
        //The method used in transformation:
        //1.arithmetic average; 2.geometric center;
        //3.using minimax combined features of a bag (default: 1)
        //
        //Method 3:
        //Define s to be the vector of the coordinate-wise maxima
        //and minima of X, ie.,
        //s(X)=(minx1, ..., minxm, maxx1, ...,maxxm), transform
        //the exemplars into mono-instance which contains attributes s(X)
        RandomForest rf = new RandomForest();
        rf.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        cls.setClassifier(rf);
        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());
        //-----------------------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_SMI_RF_SS

    public static void run_SMI_LR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                     int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                     boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);


        //-----------------------------------------
        SimpleMI cls = new SimpleMI();
        cls.setOptions(weka.core.Utils.splitOptions("-M 1")); // the best performing is arithmetic average
        //Instances newTrain = cls.transform(train);
        //for (int i = 0; i < newTrain.numInstances(); i++)
        //{
        //    println(newTrain.instance(i));
        //} // for
        //System.exit(0);

        //-M [1|2|3]
        //The method used in transformation:
        //1.arithmetic average; 2.geometric center;
        //3.using minimax combined features of a bag (default: 1)
        //
        //Method 3:
        //Define s to be the vector of the coordinate-wise maxima
        //and minima of X, ie.,
        //s(X)=(minx1, ..., minxm, maxx1, ...,maxxm), transform
        //the exemplars into mono-instance which contains attributes s(X)
        cls.setClassifier(new Logistic());
        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());
        //-----------------------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_SMI_LR_SS



    //works since it is a statistical summary
    public static void run_W_SMI_RF_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                     int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                     boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);


        //reweight and remove ts
        setAData = Utils.reweightInstancesOfEachBagByTs(setAData, keepTsAsAVariable);
        setBData = Utils.reweightInstancesOfEachBagByTs(setBData, keepTsAsAVariable);


        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);


        //-----------------------------------------
        SimpleMI cls = new SimpleMI();
        cls.setOptions(weka.core.Utils.splitOptions("-M 1")); // the best performing is arithmetic average
        //cls.setDoNotCheckCapabilities(true);
        //Instances newTrain = cls.transform(train);
        //for (int i = 0; i < newTrain.numInstances(); i++)
        //{
        //    println(newTrain.instance(i));
        //} // for
        //System.exit(0);

        //-M [1|2|3]
        //The method used in transformation:
        //1.arithmetic average; 2.geometric center;
        //3.using minimax combined features of a bag (default: 1)
        //
        //Method 3:
        //Define s to be the vector of the coordinate-wise maxima
        //and minima of X, ie.,
        //s(X)=(minx1, ..., minxm, maxx1, ...,maxxm), transform
        //the exemplars into mono-instance which contains attributes s(X)
        RandomForest rf = new RandomForest();
        rf.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        cls.setClassifier(rf);
        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());
        //-----------------------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_W_SMI_RF_SS




    //works since it is a statistical summary
    public static void run_W_SMI_LR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                     int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                     boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //reweight and remove ts
        setAData = Utils.reweightInstancesOfEachBagByTs(setAData, keepTsAsAVariable);
        setBData = Utils.reweightInstancesOfEachBagByTs(setBData, keepTsAsAVariable);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);


        //-----------------------------------------
        SimpleMI cls = new SimpleMI();
        cls.setOptions(weka.core.Utils.splitOptions("-M 1")); // the best performing is arithmetic average
        //cls.setDoNotCheckCapabilities(true);
//        Instances newTrain = cls.transform(train);
//        for (int i = 0; i < newTrain.numInstances(); i++)
//        {
//            Instance instance = newTrain.instance(i);
//            //if(Double.compare(instance.value(0), 132539.0) == 0)
//            //{
//                StringBuilder sb = new StringBuilder();
//                for (int attIndex = 0; attIndex < instance.numAttributes(); attIndex++)
//                {
//                    if(attIndex == 0)
//                        sb.append(instance.toString(attIndex));
//                    else {
//                        sb.append(", ");
//                        sb.append(instance.value(attIndex));
//                    }
//                } // for
//                System.out.println(sb.toString());
//                //break;
//            //}
//        } // for
//        System.exit(0);


        //-M [1|2|3]
        //The method used in transformation:
        //1.arithmetic average; 2.geometric center;
        //3.using minimax combined features of a bag (default: 1)
        //
        //Method 3:
        //Define s to be the vector of the coordinate-wise maxima
        //and minima of X, ie.,
        //s(X)=(minx1, ..., minxm, maxx1, ...,maxxm), transform
        //the exemplars into mono-instance which contains attributes s(X)
        cls.setClassifier(new Logistic());
        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());
        //-----------------------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_W_SMI_LR_SS





    //RB-SMI
    public static void run_RB_SMI_RF_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                     int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                     boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);


        //-----------------------------------------
        SimpleMI mil = new SimpleMI();
        mil.setOptions(weka.core.Utils.splitOptions("-M 1")); // the best performing is arithmetic average
        //Instances newTrain = mil.transform(train);
        //for (int i = 0; i < newTrain.numInstances(); i++)
        //{
        //    println(newTrain.instance(i));
        //} // for
        //System.exit(0);

        //-M [1|2|3]
        //The method used in transformation:
        //1.arithmetic average; 2.geometric center;
        //3.using minimax combined features of a bag (default: 1)
        //
        //Method 3:
        //Define s to be the vector of the coordinate-wise maxima
        //and minima of X, ie.,
        //s(X)=(minx1, ..., minxm, maxx1, ...,maxxm), transform
        //the exemplars into mono-instance which contains attributes s(X)
        RandomForest rf = new RandomForest();                   // TODO 1000 trees
        rf.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        mil.setClassifier(rf);

        RealAdaBoost cls = new RealAdaBoost(); // drops SMI-LR performance, //improves SMI-RF performance
        cls.setOptions(weka.core.Utils.splitOptions("-I 10"));
        cls.setClassifier(mil);

        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());
        //-----------------------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_RB_SMI_RF_SS




    public static void run_BG_SMI_LR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                        int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                        boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);


        //-----------------------------------------
        SimpleMI mil = new SimpleMI();
        mil.setOptions(weka.core.Utils.splitOptions("-M 1")); // the best performing is arithmetic average
        //Instances newTrain = mil.transform(train);
        //for (int i = 0; i < newTrain.numInstances(); i++)
        //{
        //    println(newTrain.instance(i));
        //} // for
        //System.exit(0);

        //-M [1|2|3]
        //The method used in transformation:
        //1.arithmetic average; 2.geometric center;
        //3.using minimax combined features of a bag (default: 1)
        //
        //Method 3:
        //Define s to be the vector of the coordinate-wise maxima
        //and minima of X, ie.,
        //s(X)=(minx1, ..., minxm, maxx1, ...,maxxm), transform
        //the exemplars into mono-instance which contains attributes s(X)
        mil.setClassifier(new Logistic());

        Bagging cls = new Bagging(); // drops SMI-LR performance, //improves SMI-RF performance
        cls.setOptions(weka.core.Utils.splitOptions("-I 10")); // define -num-slots #cores for parallel execution
        cls.setClassifier(mil);

        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());
        //-----------------------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_BG_SMI_LR_SS




    //works since it is a statistical summary
    public static void run_W_RB_SMI_RF_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                          int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                          boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);



        //reweight and remove ts
        setAData = Utils.reweightInstancesOfEachBagByTs(setAData, keepTsAsAVariable);
        setBData = Utils.reweightInstancesOfEachBagByTs(setBData, keepTsAsAVariable);



        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);


        //-----------------------------------------
        SimpleMI mil = new SimpleMI();
        mil.setOptions(weka.core.Utils.splitOptions("-M 1")); // the best performing is arithmetic average
        //mil.setDoNotCheckCapabilities(true);
        //Instances newTrain = mil.transform(train);
        //for (int i = 0; i < newTrain.numInstances(); i++)
        //{
        //    println(newTrain.instance(i));
        //} // for
        //System.exit(0);

        //-M [1|2|3]
        //The method used in transformation:
        //1.arithmetic average; 2.geometric center;
        //3.using minimax combined features of a bag (default: 1)
        //
        //Method 3:
        //Define s to be the vector of the coordinate-wise maxima
        //and minima of X, ie.,
        //s(X)=(minx1, ..., minxm, maxx1, ...,maxxm), transform
        //the exemplars into mono-instance which contains attributes s(X)
        RandomForest rf = new RandomForest();
        rf.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        mil.setClassifier(rf);

        RealAdaBoost cls = new RealAdaBoost(); // drops SMI-LR performance, //improves SMI-RF performance
        cls.setOptions(weka.core.Utils.splitOptions("-I 10"));
        cls.setClassifier(mil);

        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());
        //-----------------------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_W_RB_SMI_RF_SS




    //works since it is a statistical summary
    public static void run_W_BG_SMI_LR_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                        int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                        boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, true, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);



        //reweight and remove ts
        setAData = Utils.reweightInstancesOfEachBagByTs(setAData, keepTsAsAVariable);
        setBData = Utils.reweightInstancesOfEachBagByTs(setBData, keepTsAsAVariable);


        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);


        //-----------------------------------------
        SimpleMI mil = new SimpleMI();
        mil.setOptions(weka.core.Utils.splitOptions("-M 1")); // the best performing is arithmetic average
        //mil.setDoNotCheckCapabilities(true);
        //Instances newTrain = mil.transform(train);
        //for (int i = 0; i < newTrain.numInstances(); i++)
        //{
        //    println(newTrain.instance(i));
        //} // for
        //System.exit(0);

        //-M [1|2|3]
        //The method used in transformation:
        //1.arithmetic average; 2.geometric center;
        //3.using minimax combined features of a bag (default: 1)
        //
        //Method 3:
        //Define s to be the vector of the coordinate-wise maxima
        //and minima of X, ie.,
        //s(X)=(minx1, ..., minxm, maxx1, ...,maxxm), transform
        //the exemplars into mono-instance which contains attributes s(X)
        mil.setClassifier(new Logistic());

        Bagging cls = new Bagging(); // drops SMI-LR performance, //improves SMI-RF performance
        cls.setOptions(weka.core.Utils.splitOptions("-I 10")); // define -num-slots #cores for parallel execution
        cls.setClassifier(mil);

        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());
        //-----------------------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_W_BG_SMI_LR_SS



    //NEW
    public static void run_SMI_SVM_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                     int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                     boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);


        //-----------------------------------------
        SimpleMI cls = new SimpleMI();
        cls.setOptions(weka.core.Utils.splitOptions("-M 1")); // the best performing is arithmetic average
        //Instances newTrain = cls.transform(train);
        //for (int i = 0; i < newTrain.numInstances(); i++)
        //{
        //    println(newTrain.instance(i));
        //} // for
        //System.exit(0);

        //-M [1|2|3]
        //The method used in transformation:
        //1.arithmetic average; 2.geometric center;
        //3.using minimax combined features of a bag (default: 1)
        //
        //Method 3:
        //Define s to be the vector of the coordinate-wise maxima
        //and minima of X, ie.,
        //s(X)=(minx1, ..., minxm, maxx1, ...,maxxm), transform
        //the exemplars into mono-instance which contains attributes s(X)
        LibSVM svm = new LibSVM();
        svm.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_LINEAR, LibSVM.TAGS_KERNELTYPE)); // Sigmoid and Linear kernel worked better
        //svm.setOptions(weka.core.Utils.splitOptions("-B"));
        svm.setNormalize(false);
        svm.setDoNotReplaceMissingValues(true);
        cls.setClassifier(svm);
        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());
        //-----------------------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, true);
        } // else
    } // run_SMI_SVM_SS


    //LOWER PREDICTIVE PERFORMANCE
    public static void run_SMI_SPS_SS(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                                      int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                                      boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable) throws Exception
    {
        //forward impute the multivariate time series
        //mtses = Imputations.getInstance()
                //.impute(mtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

        //System.out.println(mtses.get(0).toVerticalString());
        //System.exit(0);

        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        //clear mtses
        //mtses.clear();
        //mtses = null;


        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //remove ts
        //setAData = Utils.removeTs(setAData);
        //setBData = Utils.removeTs(setBData);

        Instances train = new Instances(setAData);
        System.out.println("Before undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        //undersample
        train = Utils.underSample(train, true);

        //print weights of instances of the first bag
        System.out.println();
        Utils.printInstanceWeightsOfABag(train.instance(0));
        System.out.println("\nBag weights of the first 10 bags: ");
        //print the weights of first 10 bags
        Utils.printBagWeights(train, 0, 10);


        //copy test data
        Instances test = new Instances(setBData);


        //-----------------------------------------
        SimpleMI cls = new SimpleMI();
        cls.setOptions(weka.core.Utils.splitOptions("-M 1")); // the best performing is arithmetic average
        //Instances newTrain = cls.transform(train);
        //for (int i = 0; i < newTrain.numInstances(); i++)
        //{
        //    println(newTrain.instance(i));
        //} // for
        //System.exit(0);

        //-M [1|2|3]
        //The method used in transformation:
        //1.arithmetic average; 2.geometric center;
        //3.using minimax combined features of a bag (default: 1)
        //
        //Method 3:
        //Define s to be the vector of the coordinate-wise maxima
        //and minima of X, ie.,
        //s(X)=(minx1, ..., minxm, maxx1, ...,maxxm), transform
        //the exemplars into mono-instance which contains attributes s(X)
        SPegasos svm = new SPegasos();
        svm.setDontNormalize(true);
        svm.setDontReplaceMissing(true);
        cls.setClassifier(svm);
        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());
        //-----------------------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, true);
        } // else
    } // run_SMI_SPS_SS



    public static void run_MIW_RF(String imputeMethod, String transformMethodSimpleName, AttributeTransformFilter atf,
                                  ImbalanceHandler ibh) throws Exception
    {
        Instances setAData = new Instances(new FileReader(imputeMethod + "_set_a"
                + File.separator + transformMethodSimpleName + File.separator + "set_a.arff"));

        Instances setBData = new Instances(new FileReader(imputeMethod  +"_set_b"
                + File.separator + transformMethodSimpleName + File.separator + "set_b.arff"));

        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);


        Instances train = setAData;
        Instances test = setBData;


        //undersample, oversample or do nothing
        train = ibh.apply(train, true);


        if(!atf.equals(AttributeTransformFilter.NONE))
        {
            //convert to prop data
            train = Utils.toProp(train);
            test = Utils.toProp(test);

            //apply standardisation, normalization, etc
            train = atf.apply(train, true);
            test = atf.apply(test, true);

            //convert back mi data
            train = Utils.toMi(train);
            test = Utils.toMi(test);
        }


        //random forest with 100 trees and parallelization
        //multi-instance data
        MIWrapper cls = new MIWrapper();
        cls.setOptions(weka.core.Utils.splitOptions("-P 2 -A 1")); // P = 2 geometric average, A = 1 unit weighting for an instance inside bag
        RandomForest rf = new RandomForest();                   //TODO 1000 trees
        rf.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        cls.setClassifier(rf); // you can define other parameters for an internal classifier or perform CVParameterSelection for hyperparameter selection
        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());


        //average the results over 10 runs
        Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
    } // run_MIW_RF



    public static void run_RF(String imputeMethod, String transformMethodSimpleName, AttributeTransformFilter atf,
                              ImbalanceHandler ibh) throws Exception
    {
        Instances setAData = new Instances(new FileReader(imputeMethod + "_set_a"
                + File.separator + transformMethodSimpleName + File.separator + "set_a.arff"));

        Instances setBData = new Instances(new FileReader(imputeMethod  +"_set_b"
                + File.separator + transformMethodSimpleName + File.separator + "set_b.arff"));

        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //to transform multi-instance data to single instance data
        SimpleMI smi = new SimpleMI();
        smi.setOptions(weka.core.Utils.splitOptions("-M 1")); // arithmetic average, for bag containing single instance this does nothing

        Instances train = smi.transform(setAData);
        train.deleteAttributeAt(0); // delete the bagID attribute
        Instances test = smi.transform(setBData);
        test.deleteAttributeAt(0); // delete the bagID attribute

        //undersample, oversample or do nothing
        train = ibh.apply(train, true);


        //apply standardisation, normalization, etc
        train = atf.apply(train, true);
        test = atf.apply(test, true);


        //random forest with 100 trees and parallelization
        RandomForest cls = new RandomForest();              //TODO 1000 trees
        cls.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());


        //average the results over 10 runs
        Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
    } // run_RF



    public static void run_RF(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, VarRanges varRanges, int setASize,
                              int setBSize, Integer setCSize, double missingValuePlaceHolder, Imputations.ImputeMethod method,
                              boolean crossValidateOnSetAInstead, boolean keepTsAsAVariable,
                              AttributeTransformFilter atf,
                              ImbalanceHandler ibh) throws Exception
    {
        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);


        //experiment, no difference between mi data undersampling and single instance data undersampling, samples the same record ids
        //setAData = ibh.apply(setAData, true);
        //List<String> recordIds = new ArrayList<>();
        //for(int index = 0; index < setAData.numInstances(); index ++)
        //{
        //    Instance bagInstance = setAData.instance(index);
        //    String recordID = bagInstance.toString(bagInstance.attribute(0));
        //    //System.out.println(recordID);
        //    recordIds.add(recordID);
        //} // for
        //Utils.moveFiles(recordIds, Constants.PHYSIONET_SET_A_DIR_PATH, "undersampled");
        //System.exit(0);


        //to transform multi-instance data to single instance data
        SimpleMI smi = new SimpleMI();
        smi.setOptions(weka.core.Utils.splitOptions("-M 1")); // arithmetic average, for bag containing single instance this does nothing

        Instances train = smi.transform(setAData);
        train.deleteAttributeAt(0); // delete the bagID attribute
        Instances test = smi.transform(setBData);
        test.deleteAttributeAt(0); // delete the bagID attribute


        //undersample, oversample or do nothing
        train = ibh.apply(train, true);


        //apply standardisation, normalization, etc
        train = atf.apply(train, true);
        test = atf.apply(test, true);


        //random forest with 100 trees and parallelization
        RandomForest cls = new RandomForest();              //TODO 1000 trees
        cls.setOptions(weka.core.Utils.splitOptions("-I 100 -num-slots 0")); // default I is 10 (10 trees) -num-slots (use 0 to auto-detect number of cores)
        System.out.println(Arrays.toString(cls.getOptions()) + "\n" + cls.getClass());
        //-----------------------------------------


        if(crossValidateOnSetAInstead)
        {
            //cross validate on set-a over 10-fold
            Utils.crossValidate(cls, train, null, 1, 10, true, false);
        } // if
        else
        {
            //average the results over 10 runs
            Utils.simpleTrainTestOverMultipleRuns(cls, train, 10, test, false);
        } // else
    } // run_RF

} // Experiments