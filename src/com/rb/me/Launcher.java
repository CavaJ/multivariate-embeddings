package com.rb.me;


import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.rb.me.Constants.*;

//After making changes to .gitignore file, do the following:
//git rm -rf --cached .
//git add .
//git commit -m ".gitignore fix"
//This removes all files from the repository and adds them back (this time respecting the rules in your .gitignore).

public class Launcher
{

    public String SayMyName() //(String fileLoc)
    {
        System.out.println("SayMyName() called");


        //try {
            File newFile = //new File(fileLoc);
                             new File( "resources" + File.separator + "new.txt");
            //newFile.createNewFile();
            //PrintWriter pw = new PrintWriter(newFile);
            //pw.print("Executed " + new Date());
            //pw.close();
            String msg = Utils.fileContentsFromLocalFilePath(newFile.getAbsolutePath());
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}


        return(msg); //("My name is " + getClass().getName());
    }



    public static void main(String[] args)
    {
        ////new Launcher().SayMyName("resources/new.txt");
        //Object omtsesTrainSizePair = Utils.mtsesFromSetA("data/set-a", "resources/variable_ranges_physionet.csv");
        ////System.out.println(Utils.longestMTSEInSetA("forward", omtsesTrainSizePair,
        ////         "resources/variable_ranges_physionet.csv"));
        ////System.out.println(
        //        Utils.transformMtsesToCSVString("forward", "TRANSFORM_SUPER_13",
        //        omtsesTrainSizePair, "resources/variable_ranges_physionet.csv");
        ////);
        //System.exit(0);


        //Utils.findAvgAUROC_AUPRC("resources/brits.txt");
        //Utils.findAvgAUROC_AUPRC("resources/gru_d.txt");
        //Utils.findAvgAUROC_AUPRC("resources/m_rnn.txt");
        //System.exit(0);

        //snippet to generate general descriptors for all files of each file set in a separate file
        //LinkedHashSet<String> descriptorVars = new LinkedHashSet<String>(Arrays.asList("RecordID", "Age", "Gender", "Height", "ICUType", "Weight"));
        //Utils.writeGeneralDescriptorsRecords(descriptorVars, ",", DATA_FOLDER, "txt", setADir, setBDir, setCDir);
        //System.exit(0);
        //after running the above snippet we have the following files
        //String setAGenDescRecordsFile = PHYSIONET_DATA_FOLDER + File.separator + "GeneralDescriptorsRecords-a.txt";
        //String setBGenDescRecordsFile = PHYSIONET_DATA_FOLDER + File.separator + "GeneralDescriptorsRecords-b.txt";
        //String setCGenDescRecordsFile = PHYSIONET_DATA_FOLDER + File.separator + "GeneralDescriptorsRecords-c.txt";


        //method to handle outcomes
        Outcomes outcomes = Utils.outcomesFromLocalFilePaths(",", Dataset.PhysioNet,
                PHYSIONET_SET_A_OUTCOMES_FILE_PATH, PHYSIONET_SET_B_OUTCOMES_FILE_PATH, PHYSIONET_SET_C_OUTCOMES_FILE_PATH);
        //method to handle var ranges
        VarRanges varRanges = Utils.varRangesFromLocalFilePath(",", Dataset.PhysioNet, PHYSIONET_VAR_RANGES_FILE_PATH);
        //method to handle general descriptors records
        //GeneralDescriptorsRecords genDescRecords
        //        = Utils.genDescRecordsFromLocalFilePaths(",", Dataset.PhysioNet,
        //                                        setAGenDescRecordsFile, setBGenDescRecordsFile, setCGenDescRecordsFile);



        //TODO implement utility methods for the first experimental setup



        // second experimental setup, no standardization, no imbalance handling
        Utils.secondSetupRNNAll(false);
        Utils.secondSetupMeanForwardAll(false);


        // second experimental setup, with standardization, no imbalance handling
        Utils.secondSetupRNNAll(true);
        Utils.secondSetupMeanForwardAll(true);



        //TODO complete the generation of own hand-engineered file for outlier removal
        //TODO remove outliers by methodological approach
        //TODO handle empty variables, empty files and missing data (some other imputation methods can also be implemented)

    } // main



    private static void checkAndFix(TreeSet<String> newConsideredVars, List<String> allVarJoinFilePaths)
    {
        //no patients have age smaller than 15, which is also used by GRU-D and deep learning benchmark papers for MIMIC-III dataset, too.

        //int fileCounter = 0;

        int tsCountSum = 0;

        int maxTimeStep = 0;


        //create a list from newConsideredVars which keeps the insertion order
        ArrayList<String> newConsideredVarsList = new ArrayList<>(newConsideredVars);
        //also add tsMinutes into considered var list at the index of 0
        newConsideredVarsList.add(0, "tsMinutes");


        //fileName, varName, varEmptiness
        //HashMap<String, LinkedHashMap<String, StringBuilder>> fileNameVarEmptinessMap = new HashMap<>();

        //map to collect all variable values in all files in an ordered fashion, such that max and min can be extracted
        LinkedHashMap<String, TreeSet<Double>> varAndOrderedValuesMap = new LinkedHashMap<>();
        //initialize the map with all variables
        for(String var : newConsideredVars)
            varAndOrderedValuesMap.put(var, new TreeSet<>());


        for(String localFilePath : allVarJoinFilePaths)
        {
            String fileContents = Utils.fileContentsFromLocalFilePath(localFilePath);
            String[] lines = StringUtils.split(fileContents,"\r\n|\r|\n");

            int emptyLinesCount = 0;

            LinkedHashMap<String, StringBuilder> varAndEmptinessMap = new LinkedHashMap<>(); // to keep the insertion order
            for(String var : newConsideredVars)
            {
                //populate all vars with empty string builders
                varAndEmptinessMap.put(var, new StringBuilder(""));
            } // for


            //discard empty files
            //if(lines.length - 1 == 0)
            //    println("File: " + localFilePath + " => is empty" + "\n");


            //line at index 0 is a header line, do not touch it
            for(int index = 1; index < lines.length; index ++)
            {
                String thisLine = lines[index];

                //divide this line to components
                String[] thisLineComponents = thisLine.split(",");

                //newConsideredVarsList and thisLineComponents must have the same size
                //it is ensured by adding tsMinutes to newConsideredVarsList above the outer for loop

                for(String var : newConsideredVars)
                {
                    int indexOfVar = newConsideredVarsList.indexOf(var);
                    String varValue = thisLineComponents[indexOfVar].trim(); // trim to get rid of spaces

                    //update map
                    varAndEmptinessMap.get(var).append(varValue);

                    TreeSet<Double> varValuesSet = varAndOrderedValuesMap.get(var);
                    if(Utils.isDouble(varValue))
                        varValuesSet.add(Double.valueOf(varValue));
                } // for


                //there is no other line with all variables empty except the line with time stamp 0
                //line with time stamp 0 is discarded for all files in generateTimeSeriesData() method
                //StringBuilder empty = new StringBuilder("");
                //for(int idx = 1; idx < thisLineComponents.length; idx ++) // discard the first component which is ts minutes and is always nonempty
                //    empty.append(thisLineComponents[idx].trim());
                //
                //if(empty.toString().isEmpty())
                //{
                //    emptyLinesCount++;
                //    println("File: " + localFilePath + " => empty line => " + index);
                //}

            } // for

            //after checking all lines we have populated, put var and its emptiness as a map for this file
            //fileNameVarEmptinessMap.put(Utils.fileNameFromPath(localFilePath), varAndEmptinessMap);



            //check which variable has all values missing in each file
            //no variable is empty in all files, but separate files have empty variables
            //StringBuilder emptyVarsDescription = new StringBuilder("");
            //for(String var : varAndEmptinessMap.keySet())
            //{
            //    if(varAndEmptinessMap.get(var).toString().isEmpty())
            //        emptyVarsDescription.append("\"").append(var).append("\"").append(",");
            //} // for
            //println("File : " + localFilePath + " => "  + emptyVarsDescription.toString() + " are empty");
            //System.out.println();


            //count the number of files with number of lines >= 20, -2 for header line and line with time stamp 0
            //if(lines.length - 2 >= 20) fileCounter ++;







            //TODO they have merged some lines together, e.g. by a time interval, check that approach
            // (in fact, by hourly or two-hourly samples)

            tsCountSum += lines.length - 1 - emptyLinesCount; // -1 for header line
            if(lines.length - 1 - emptyLinesCount > maxTimeStep)
                maxTimeStep = lines.length - 1 - emptyLinesCount;

        } // for all files


        //for each variable prints its value set
        for(String var : varAndOrderedValuesMap.keySet())
            println("\"" + var + "\" => " + varAndOrderedValuesMap.get(var) + ", Mean => "
                    + Utils.mean(varAndOrderedValuesMap.get(var), true) + ", Size: " + varAndOrderedValuesMap.get(var).size() + "\n");




//        LinkedHashMap<String, Boolean> globalVarEmptinessMap = new LinkedHashMap<>();
//        for(String var : newConsideredVars)
//        {
//            //initially all vars are empty
//            globalVarEmptinessMap.put(var, true);
//        } // for
//
//
//        //now for all files and for all variables populate the above map
//        for(String fileName : fileNameVarEmptinessMap.keySet())
//        {
//            for(String var : fileNameVarEmptinessMap.get(fileName).keySet())
//            {
//                Boolean currentEmptiness = globalVarEmptinessMap.get(var);
//                Boolean newEmptiness
//                        = currentEmptiness && fileNameVarEmptinessMap.get(fileName).get(var).toString().isEmpty();
//                globalVarEmptinessMap.put(var, newEmptiness);
//            } // for
//        } // for
//
//
//        for(String var : globalVarEmptinessMap.keySet())
//        {
//            if(globalVarEmptinessMap.get(var))
//                println("\"" + var + "\" is empty in all files");
//        } // for


        //Number of files (set-a, set-b, set-c, lines.length - 1 >= 20) with number of lines >= 20: 11815 vs. all files count: 12000
        //println("Number of files with number of lines >= 20: " + fileCounter + " vs. all files count: " + allVarJoinFilePaths.size());

        //Average number of time stamps in 12000 files (set-a, set-b, set-c, tsCountSum += lines.length - 1): 74.86266666666667
        //Average number of time stamps in 4000 files (set-a, tsCountSum += lines.length - 1): 74.816
        //Average number of time stamps in 4000 files (set-b, tsCountSum += lines.length - 1): 74.767
        //Average number of time stamps in 4000 files (set-c, tsCountSum += lines.length - 1): 75.005
        println("Average number of time stamps in " + allVarJoinFilePaths.size() + " files: " + (tsCountSum / (allVarJoinFilePaths.size() * 1.0)));

        //Maximum time steps in 4000 files (set-a, lines.length - 1 > maxTimeStep): 203
        println("Maximum time steps in " + + allVarJoinFilePaths.size() + " files: " + maxTimeStep);
    } // checkAndFix




    private static void println(Object o)
    {
        System.out.println(o);
    } // println


    private static void runExperiments(List<MTSE> initMtses, Outcomes outcomes, VarRanges varRanges, int setASize, int setBSize,
                                       Integer setCSize, double missingValuePlaceHolder) throws Exception
    {
        //configurations on SMI-LR, BG-SMI-LR, SMI-RF, RB-SMI-RF
        List<Imputations.ImputeMethod> methods = new ArrayList<>();
        //methods.add(Imputations.ImputeMethod.MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION);
        //methods.add(Imputations.ImputeMethod.LIPTON_FORWARD_FILLING_IMPUTATION);
        //boolean[] trainTestVsCV = new boolean[]{false, true};

        boolean runConfig = false;
        Imputations.ImputeMethod method //= Imputations.ImputeMethod.MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION;
                                        //= Imputations.ImputeMethod.LIPTON_FORWARD_FILLING_IMPUTATION;
                                        = null;



        //for(boolean runConfig : trainTestVsCV)
        //{
            //for (Imputations.ImputeMethod method : methods)
            //{

                  List<MTSE> mtses = initMtses;
                                   // = Imputations.getInstance().impute(initMtses, method, new int[]{0, setASize}, varRanges, missingValuePlaceHolder);

//                //SMI-SVM
//                Experiments.run_SMI_SVM_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null,
//                        missingValuePlaceHolder, method, runConfig, false);

//                //SMI-SPS
//                Experiments.run_SMI_SPS_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null,
//                        missingValuePlaceHolder, method, runConfig, false);


//                //SMI-LR
//                Experiments.run_SMI_LR_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null,
//                        missingValuePlaceHolder, method, runConfig, false);
//
//
//                //BG-SMI-LR
//                Experiments.run_BG_SMI_LR_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null,
//                        missingValuePlaceHolder, method, runConfig, false);
//
//
//                //SMI-RF
//                Experiments.run_SMI_RF_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null,
//                        missingValuePlaceHolder, method, runConfig, false);
//
//                //RB-SMI-RF
//                Experiments.run_RB_SMI_RF_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null,
//                        missingValuePlaceHolder, method, runConfig, false);



                //configurations for MILR, BG-MILR, MIW-LR, MIW-RF, RB-MIW-LR, RB-MIW-RF

//                //MILR
//                Experiments.run_MILR_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null,
//                        missingValuePlaceHolder, method, runConfig, false);
//
//
//                //MIW-LR
//                Experiments.run_MIW_LR_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null,
//                        missingValuePlaceHolder, method, runConfig, false);
//

//                //MIW-RF
//                Experiments.run_MIW_RF_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null,
//                        missingValuePlaceHolder, method, runConfig, false);


//                //BG-MILR
//                //Experiments.run_BG_MILR_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                //        setBSize, null,
//                //        missingValuePlaceHolder, method, runConfig, false);
//
//
//                //RB-MIW-LR
//                Experiments.run_RB_MIW_LR_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null,
//                        missingValuePlaceHolder, method, runConfig, false);

//                //RB-MIW-RF
//                Experiments.run_RB_MIW_RF_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null,
//                        missingValuePlaceHolder, method, runConfig, false);




                //Experiments.run_MIB_LR_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
                //        setBSize, null,
                //        missingValuePlaceHolder, method, runConfig, false);

                //Experiments.run_MIB_RF_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
                //        setBSize, null,
                //        missingValuePlaceHolder, method, runConfig, false);



                Experiments.run_RF(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
                        setBSize, null, missingValuePlaceHolder, method, runConfig, false,
                        AttributeTransformFilter.NONE, ImbalanceHandler.NONE);




//                //a weighted scheme only works for SMI configurations, since base classifiers implements WeightedInstanceHandler interface
//                //weighted experimental setups
//                //SMI-LR
//                Experiments.run_W_SMI_LR_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null, missingValuePlaceHolder, method, runConfig, false);
//
//                //BG-SMI-LR
//                Experiments.run_W_BG_SMI_LR_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null, missingValuePlaceHolder, method, runConfig, false);
//
//                //SMI-RF
//                Experiments.run_W_SMI_RF_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null, missingValuePlaceHolder, method, runConfig, false);
//
//                //RB_SMI_RF
//                Experiments.run_W_RB_SMI_RF_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
//                        setBSize, null, missingValuePlaceHolder, method, runConfig, false);



                //------------- MULTI-INSTANCE LEARNERS --------------

                ////MIW-LR
                //Experiments.run_W_MIW_LR_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
                //        setBSize, null,
                //        missingValuePlaceHolder, method, runConfig, false);


                ////MIW-RF
                //Experiments.run_W_MIW_RF_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
                //        setBSize, null,
                //        missingValuePlaceHolder, method, runConfig, false);


                ////RB-MIW-LR
                //Experiments.run_W_RB_MIW_LR_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
                //        setBSize, null,
                //        missingValuePlaceHolder, method, runConfig, false);

                ////RB-MIW-RF
                //Experiments.run_W_RB_MIW_RF_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
                //        setBSize, null,
                //        missingValuePlaceHolder, method, runConfig, false);


                //Experiments.run_W_MIB_LR_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
                //        setBSize, null,
                //        missingValuePlaceHolder, method, runConfig, false);

                //Experiments.run_W_MIB_RF_SS(Dataset.PhysioNet, mtses, outcomes, varRanges, setASize,
                //        setBSize, null,
                //        missingValuePlaceHolder, method, runConfig, false);


            //} // for each impute method

        //} // for each runConfig

    } // runExperiments

} // class Launcher
