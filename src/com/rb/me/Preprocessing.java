package com.rb.me;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

//class to handle preprocessing for each dataset separately
public class Preprocessing
{

    //helper method to construct time series data in the following structure:
    //ts1, var1Value, var2Value, var3Value, ....
    //ts2, var1Value, var2Value, var3Value, ....
    //...
    public static Map<String, String> generateTimeSeriesData(List<String> allFilePaths, String missingValuePlaceHolder, VarRanges varRanges)
    {
        long startProg = System.currentTimeMillis();

        System.out.println("Preprocessing started...");

        //metadata is added as the last component in the header row
        /*
        General descriptors:
        --------------------
        RecordID (a unique integer for each ICU stay)
        Age (years)
        Gender (0: female, or 1: male)
        Height (cm)
        ICUType (1: Coronary Care Unit, 2: Cardiac Surgery Recovery Unit,
        3: Medical ICU, or 4: Surgical ICU)
        Weight (kg)*.
        --------------------
        A value of -1 indicates missing or unknown data (for example, if a patient's height was not recorded).
         */


        HashSet<String> varsToDiscard = new HashSet<>();
        //Variables to discard: RecordID, Age, Gender, Height, ICUType, Weight
        varsToDiscard.add("RecordID");
        varsToDiscard.add("Age");
        varsToDiscard.add("Gender");
        varsToDiscard.add("Height");
        varsToDiscard.add("ICUType");
        varsToDiscard.add("Weight"); // both general descriptor and time series variable, handled carefully
        varsToDiscard.add("MechVent"); // this is a time series variable with values 0 and 1, but no significance in time series prediction, therefore discarded


        String[] vars = new String[]{"Albumin", "ALP", "ALT", "AST", "Bilirubin", "BUN", "Cholesterol", "Creatinine",
                "DiasABP", "FiO2", "GCS", "Glucose", "HCO3", "HCT", "HR", "K", "Lactate", "Mg", "MAP",
                /*"MechVent",*/  // discarded MechVent variable which has the value of only 0 or 1, added this variable to the set of discarded variables above
                "Na", "NIDiasABP", "NIMAP", "NISysABP", "PaCO2", "PaO2", "pH", "Platelets", "RespRate", "SaO2",
                "SysABP", "Temp", "TroponinI", "TroponinT", "Urine", "WBC",
                //"Weight"
                };
        TreeSet<String> consideredVars = new TreeSet<String>(Arrays.asList(vars));




        //file paths generated at the end
        //List<String> newFilePaths = new ArrayList<>();


        //default padding length is the length of the variable with the longest name
        int defaultPaddingLength = Utils.defaultPaddingLength(consideredVars);


        //list will keep the insertion order
        List<String> paddedConsideredVars = new ArrayList<>();
        for(String var : consideredVars)
            paddedConsideredVars.add(Utils.padLeftSpaces(var, defaultPaddingLength));

        String firstLineOfANewFile = Utils.padLeftSpaces("tsMinutes", defaultPaddingLength)
                + "," + String.join(",", paddedConsideredVars);
        List<String> firstLineComponents = new ArrayList<>(consideredVars); firstLineComponents.add(0, "tsMinutes");


        //map for filePath and its generated content
        LinkedHashMap<String, String> filePathFileContentsMap = new LinkedHashMap<>();


        for(String filePath : allFilePaths)
        {
            //System.out.println("Processing file: " + filePath);

            String fileContents = Utils.fileContentsFromLocalFilePath(filePath);


            //all files are actually record ids
            String fileName = Utils.fileNameFromPath(filePath);
            int recordID = Integer.parseInt(FilenameUtils.removeExtension(fileName));


            //general descriptors or meta parameters will be appended as the last component to the header row
            //therefore, there is only one meta string builder for each file
            //StringBuilder metaBuilder = new StringBuilder().append(",").append("{"); //meta start withs ,{ => comma is for separating it from other variables


            //StringBuilder sb = new StringBuilder("");
            TreeMap<Integer, String[]> tsLineValuesMap = new TreeMap<Integer, String[]>();

            //obtain a scanner for file contents string
            try (Scanner scanner = new Scanner(fileContents))
            {
                //discard first line
                //first line of the file is in the form => Time,Parameter,Value
                scanner.nextLine();


                //StringBuilder sb;
                String[] lineValues;

                //now read the fileContents line by line
                while (scanner.hasNextLine())
                {
                    //read the fileContents line by line
                    String line = scanner.nextLine();


                    //the line structure is the following:
                    //time_stamp,variable_name,variable_value
                    String[] lineComponents = line.split(",");

                    //there are cases where the variable name is empty
                    //in this case, discard this line
                    //variable_name is the second component at "time_stamp,variable_name,variable_value"
                    if(lineComponents[1].trim().isEmpty()) continue;



                    String timeStampString = lineComponents[0];
                    int timeStampMinutes = Utils.toMinutes(timeStampString);

                    //for each time stamp create new entry in the tree map
                    lineValues = tsLineValuesMap.get(timeStampMinutes); // returns null if there is no such a key
                    if(lineValues == null) {
                        lineValues = new String[firstLineComponents.size()]; // with the size equal to number of vars + 1
                        for(int i = 0; i < firstLineComponents.size(); i++)
                            lineValues[i] = Utils.padLeftSpaces(missingValuePlaceHolder, defaultPaddingLength); //firstLineComponents.get(i).length());

                        //add time stamp value first
                        //which will be in the index 0
                        lineValues[0] = Utils.padLeftSpaces(timeStampMinutes + "", defaultPaddingLength); //firstLineComponents.get(0).length());

                        //put it to the map
                        tsLineValuesMap.put(timeStampMinutes, lineValues);
                    }


                    String variableName = lineComponents[1];
                    String variableValue = lineComponents[2];


                    ///handle MechVent carefully which is the variable we are not interested
                    if(variableName.equals("MechVent"))
                        continue; // continue to the next variable, each line represents one variable value


                    //discard weight too
                    if(variableName.equals("Weight"))
                        continue;


                    //handle general descriptor variables (which are only available at timestamp 0);
                    //handle weight carefully for timestamp 0
                    //varsToDiscard does not contain Weight
                    if(varsToDiscard.contains(variableName) && timeStampMinutes == 0)
                    {
                        //meta will have the following structure:
                        //{RecordID: value;Age: value;Gender: value;Height: value;ICUType: value;Weight: value}
                        //metaBuilder.append(variableName).append("=").append(variableValue).append(";");
                    }
                    //WE CONSIDER WEIGHT VARIABLE AS A TIME SERIES VARIABLE AT TIMESTAMP 0
                    //else if(variableName.equals("Weight") && timeStampMinutes == 0)
                    //{
                    //    //add weight at timestamp 0 as meta value
                    //    //weight will be last meta value to be appended
                    //    //metaBuilder.append(variableName).append("=").append(variableValue).append(";");
                    //}
                    else {
                        //get the index of variable name
                        int index = firstLineComponents.indexOf(variableName);
                        //add the variable value to the corresponding index, it will update the map values implicitly
                        lineValues[index]
                                = Utils.padLeftSpaces(ensureNonNegativityAndGet(variableValue, missingValuePlaceHolder),
                                defaultPaddingLength); //firstLineComponents.get(index).length());
                    }

                } // while


                //close meta and add it the first line of a new file
                //metaBuilder.append("InHospitalDeath").append("=").append(outcomes.get(recordID).getInHospitalDeath0Or1()).append("}");
                //firstLineOfANewFile += metaBuilder.toString();


                //explicitly remove the key == 0 and associated values, since, timestamp==0 has all variables empty
                //WRONG 133919.txt in set-a has pH value at time stamp 0, do not remove the key
                //tsLineValuesMap.remove(0);



                //now build a string from the values of variables and create a new file
                StringBuilder sb = new StringBuilder(firstLineOfANewFile).append("\n");
                //now for each key-value pair, add it to the sb
                for(Integer key : tsLineValuesMap.keySet())
                {
                    String[] lineVals = tsLineValuesMap.get(key);
                    sb.append(String.join(",", lineVals)).append("\n");
                } // for


                ////write results to local file system, method might return null if IO exception occurs
                //String newFilePath = Utils.writeToLocalFileSystem(filePath, newDirNameToPutFiles, sb.toString(), "csv");
                //if(newFilePath != null)
                //    newFilePaths.add(newFilePath);
                //else
                //    System.err.println("newFilePath is null at generateTimeSeriesData() method");


                //update the map with the string value of sb
                filePathFileContentsMap.put(filePath, sb.toString());
            } catch (Exception ex)
            {
                ex.printStackTrace();
            } // catch


        } // for each file



        //can be called when sophisticated outlier removal methods are applied
        // TODO for sophisticated methods: you should do outlier removal on set A,
        //  determine the thresholds there and remove the ones from set B and C according to those thresholds received from set A.
        // medically motivated ranges => those you can just apply to all sets
        //it will generate a new map of file path and file contents after outlier removal
        //filePathFileContentsMap = (LinkedHashMap<String, String>) removeOutliers(varRanges, consideredVars, filePathFileContentsMap, missingValuePlaceHolder);
        //combine vars similar to the GRU-D paper
        //filePathFileContentsMap = (LinkedHashMap<String, String>) combineVars(consideredVars, filePathFileContentsMap, missingValuePlaceHolder, BloodPressurePreference.PreferInvasive);


        System.out.println("Preprocessing ended...");


        long endProg = System.currentTimeMillis();
        System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(endProg-startProg) + " seconds for preprocessing");


        return filePathFileContentsMap;
    } // generateTimeSeriesData


    //parallelizes the preprocessing pipeline
    public static Map<String, String> generateTimeSeriesDataInParallel(List<String> allFilePaths, String missingValuePlaceHolder, VarRanges varRanges)
    {
        long startProg = System.currentTimeMillis();

        System.out.println("Preprocessing started...");

        //metadata is added as the last component in the header row
        /*
        General descriptors:
        --------------------
        RecordID (a unique integer for each ICU stay)
        Age (years)
        Gender (0: female, or 1: male)
        Height (cm)
        ICUType (1: Coronary Care Unit, 2: Cardiac Surgery Recovery Unit,
        3: Medical ICU, or 4: Surgical ICU)
        Weight (kg)*.
        --------------------
        A value of -1 indicates missing or unknown data (for example, if a patient's height was not recorded).
         */


        HashSet<String> varsToDiscard = new HashSet<>();
        //Variables to discard: RecordID, Age, Gender, Height, ICUType, Weight
        varsToDiscard.add("RecordID");
        varsToDiscard.add("Age");
        varsToDiscard.add("Gender");
        varsToDiscard.add("Height");
        varsToDiscard.add("ICUType");
        varsToDiscard.add("Weight"); // both general descriptor and time series variable, handled carefully
        varsToDiscard.add("MechVent"); // this is a time series variable with values 0 and 1, but no significance in time series prediction, therefore discarded


        String[] vars = new String[]{"Albumin", "ALP", "ALT", "AST", "Bilirubin", "BUN", "Cholesterol", "Creatinine",
                "DiasABP", "FiO2", "GCS", "Glucose", "HCO3", "HCT", "HR", "K", "Lactate", "Mg", "MAP",
                /*"MechVent",*/  // discarded MechVent variable which has the value of only 0 or 1, added this variable to the set of discarded variables above
                "Na", "NIDiasABP", "NIMAP", "NISysABP", "PaCO2", "PaO2", "pH", "Platelets", "RespRate", "SaO2",
                "SysABP", "Temp", "TroponinI", "TroponinT", "Urine", "WBC",
                //"Weight"
        };
        TreeSet<String> consideredVars = new TreeSet<String>(Arrays.asList(vars));




        //file paths generated at the end
        //List<String> newFilePaths = new ArrayList<>();


        //default padding length is the length of the variable with the longest name
        int defaultPaddingLength = Utils.defaultPaddingLength(consideredVars);


        //list will keep the insertion order
        List<String> paddedConsideredVars = new ArrayList<>();
        for(String var : consideredVars)
            paddedConsideredVars.add(Utils.padLeftSpaces(var, defaultPaddingLength));

        String firstLineOfANewFile = Utils.padLeftSpaces("tsMinutes", defaultPaddingLength)
                + "," + String.join(",", paddedConsideredVars);
        List<String> firstLineComponents = new ArrayList<>(consideredVars); firstLineComponents.add(0, "tsMinutes");


        //map for filePath and its generated content
        //LinkedHashMap<String, String> filePathFileContentsMap = new LinkedHashMap<>();
        ConcurrentHashMap<String, String> filePathFileContentsMap = new ConcurrentHashMap<>();
        //filePathFileContentsMap. //TODO change default concurrency level


        allFilePaths.parallelStream().forEach(filePath ->
        {
            //System.out.println("Processing file: " + filePath);

            String fileContents = Utils.fileContentsFromLocalFilePath(filePath);


            //all files are actually record ids
            String fileName = Utils.fileNameFromPath(filePath);
            int recordID = Integer.parseInt(FilenameUtils.removeExtension(fileName));


            //general descriptors or meta parameters will be appended as the last component to the header row
            //therefore, there is only one meta string builder for each file
            //StringBuilder metaBuilder = new StringBuilder().append(",").append("{"); //meta start withs ,{ => comma is for separating it from other variables


            //StringBuilder sb = new StringBuilder("");
            TreeMap<Integer, String[]> tsLineValuesMap = new TreeMap<Integer, String[]>();

            //obtain a scanner for file contents string
            try (Scanner scanner = new Scanner(fileContents))
            {
                //discard first line
                //first line of the file is in the form => Time,Parameter,Value
                scanner.nextLine();


                //StringBuilder sb;
                String[] lineValues;

                //now read the fileContents line by line
                while (scanner.hasNextLine())
                {
                    //read the fileContents line by line
                    String line = scanner.nextLine();


                    //the line structure is the following:
                    //time_stamp,variable_name,variable_value
                    String[] lineComponents = line.split(",");

                    //there are cases where the variable name is empty
                    //in this case, discard this line
                    //variable_name is the second component at "time_stamp,variable_name,variable_value"
                    if(lineComponents[1].trim().isEmpty()) continue;



                    String timeStampString = lineComponents[0];
                    int timeStampMinutes = Utils.toMinutes(timeStampString);

                    //for each time stamp create new entry in the tree map
                    lineValues = tsLineValuesMap.get(timeStampMinutes); // returns null if there is no such a key
                    if(lineValues == null) {
                        lineValues = new String[firstLineComponents.size()]; // with the size equal to number of vars + 1
                        for(int i = 0; i < firstLineComponents.size(); i++)
                            lineValues[i] = Utils.padLeftSpaces(missingValuePlaceHolder, defaultPaddingLength); //firstLineComponents.get(i).length());

                        //add time stamp value first
                        //which will be in the index 0
                        lineValues[0] = Utils.padLeftSpaces(timeStampMinutes + "", defaultPaddingLength); //firstLineComponents.get(0).length());

                        //put it to the map
                        tsLineValuesMap.put(timeStampMinutes, lineValues);
                    }


                    String variableName = lineComponents[1];
                    String variableValue = lineComponents[2];


                    ///handle MechVent carefully which is the variable we are not interested
                    if(variableName.equals("MechVent"))
                        continue; // continue to the next variable, each line represents one variable value


                    //discard weight too
                    if(variableName.equals("Weight"))
                        continue;


                    //handle general descriptor variables (which are only available at timestamp 0);
                    //handle weight carefully for timestamp 0
                    //varsToDiscard does not contain Weight
                    if(varsToDiscard.contains(variableName) && timeStampMinutes == 0)
                    {
                        //meta will have the following structure:
                        //{RecordID: value;Age: value;Gender: value;Height: value;ICUType: value;Weight: value}
                        //metaBuilder.append(variableName).append("=").append(variableValue).append(";");
                    }
                    //WE CONSIDER WEIGHT VARIABLE AS A TIME SERIES VARIABLE AT TIMESTAMP 0
                    //else if(variableName.equals("Weight") && timeStampMinutes == 0)
                    //{
                    //    //add weight at timestamp 0 as meta value
                    //    //weight will be last meta value to be appended
                    //    //metaBuilder.append(variableName).append("=").append(variableValue).append(";");
                    //}
                    else {
                        //get the index of variable name
                        int index = firstLineComponents.indexOf(variableName);
                        //add the variable value to the corresponding index, it will update the map values implicitly
                        lineValues[index]
                                = Utils.padLeftSpaces(ensureNonNegativityAndGet(variableValue, missingValuePlaceHolder),
                                defaultPaddingLength); //firstLineComponents.get(index).length());
                    }

                } // while


                //close meta and add it the first line of a new file
                //metaBuilder.append("InHospitalDeath").append("=").append(outcomes.get(recordID).getInHospitalDeath0Or1()).append("}");
                //firstLineOfANewFile += metaBuilder.toString();


                //explicitly remove the key == 0 and associated values, since, timestamp==0 has all variables empty
                //WRONG 133919.txt in set-a has pH value at time stamp 0, do not remove the key
                //tsLineValuesMap.remove(0);



                //now build a string from the values of variables and create a new file
                StringBuilder sb = new StringBuilder(firstLineOfANewFile).append("\n");
                //now for each key-value pair, add it to the sb
                for(Integer key : tsLineValuesMap.keySet())
                {
                    String[] lineVals = tsLineValuesMap.get(key);
                    sb.append(String.join(",", lineVals)).append("\n");
                } // for


                ////write results to local file system, method might return null if IO exception occurs
                //String newFilePath = Utils.writeToLocalFileSystem(filePath, newDirNameToPutFiles, sb.toString(), "csv");
                //if(newFilePath != null)
                //    newFilePaths.add(newFilePath);
                //else
                //    System.err.println("newFilePath is null at generateTimeSeriesData() method");


                //update the map with the string value of sb
                filePathFileContentsMap.put(filePath, sb.toString());
            } catch (Exception ex)
            {
                ex.printStackTrace();
            } // catch

        });

        System.out.println("Preprocessing ended...");

        long endProg = System.currentTimeMillis();
        System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(endProg-startProg) + " seconds for preprocessing");

        return filePathFileContentsMap;
    } // generateTimeSeriesDataInParallel



    //all variable values in physionet dataset are double representable, remove negative values of variables by the following method
    //valid values of variables are non-negative (>= 0)
    private static String ensureNonNegativityAndGet(String variableValue, String missingValuePlaceHolder)
    {
        if(Utils.isDouble(variableValue))
        {
            double dValue = Double.parseDouble(variableValue);
            if(Double.compare(dValue, 0.0) < 0)
                return missingValuePlaceHolder;
            else
                return variableValue;
        } // if
        else
            return missingValuePlaceHolder;
    } // ensureNonNegativityAndGet



    //helper method to remove outliers based on specified variable ranges
    public static Map<String, String> removeOutliers(VarRanges varRanges, TreeSet<String> consideredVars, Map<String,
                                                String> filePathFileContentsMap, String missingValuePlaceHolder)
    {
        //the new file paths to return
        //List<String> newFilePaths = new ArrayList<>();


        //create a list from consideredVars which keeps the insertion order
        ArrayList<String> consideredVarsList = new ArrayList<>(consideredVars);
        //also add tsMinutes into considered var list at the index of 0
        consideredVarsList.add(0, "tsMinutes");


        //calculate the padding length
        int defaultPaddingLength = Utils.defaultPaddingLength(consideredVars);


        //new filePathFileContentsMap after outlier removal
        LinkedHashMap<String, String> newFilePathFileContentsMap = new LinkedHashMap<>();


        //now read all files one by one and remove outliers
        for(String localFilePath : filePathFileContentsMap.keySet())
        {
            String fileContents = filePathFileContentsMap.get(localFilePath);

            String[] lines = StringUtils.split(fileContents, "\r\n|\r|\n");


            StringBuilder linesBuilder
                    = new StringBuilder(Utils.padLeftSpaces("tsMinutes", defaultPaddingLength));
            //now populate the first line with new considered vars
            for(String consideredVarName : consideredVars)
                linesBuilder.append(",").append(Utils.padLeftSpaces(consideredVarName, defaultPaddingLength));


            //METADATA is obtained by GeneralDescriptorsRecords
            //append metadata, metadata is available as the last component in the header row
            //String[] splits = lines[0].split(",");
            //linesBuilder.append(",").append(splits[splits.length - 1]);


            //new line
            linesBuilder.append("\n");


            //for each line, split and obtain variable value, skip the first line which is the header line
            for(int index = 1; index < lines.length; index ++)
            {
                String thisLine = lines[index];

                //obtain the components of this line
                String[] thisLineComponents = thisLine.split(",");


                //append tsMinutes first, it is at index 0
                linesBuilder.append(Utils.padLeftSpaces(thisLineComponents[0], defaultPaddingLength));


                //thisLineComponents and consideredVarsList have the same length and variable names, values are indexed at the same location
                for(int idx = 1; idx < thisLineComponents.length; idx ++) // skip tsMinutes by starting at idx 1
                {
                    String varName = consideredVarsList.get(idx);
                    String varValue = thisLineComponents[idx];

                    //obtain ranges for the current variable
                    Ranges ranges = varRanges.getRanges(varName);

                    //new var value after outlier removal
                    String newVarValue;

                    //all variable values are numeric and can be converted to double
                    //the check will also eliminate parsing of missing or incorrect data
                    if(Utils.isDouble(varValue))
                    {
                        //V.ix[V < ranges.OUTLIER_LOW[variable]] = np.nan
                        //V.ix[V > ranges.OUTLIER_HIGH[variable]] = np.nan
                        //V.ix[V < ranges.VALID_LOW[variable]] = ranges.VALID_LOW[variable]
                        //V.ix[V > ranges.VALID_HIGH[variable]] = ranges.VALID_HIGH[variable]

                        double dValue = Double.parseDouble(varValue);

                        if(Double.compare(dValue, ranges.getOutlierLow()) < 0)
                            newVarValue = missingValuePlaceHolder;
                        else if(Double.compare(dValue, ranges.getOutlierHigh()) > 0)
                            newVarValue = missingValuePlaceHolder;
                            //at this point -> outlierLow <= dValue <= outlierHigh
                        else if(Double.compare(dValue, ranges.getValidLow()) < 0) //outlierLow <= dValue < validLow
                            newVarValue = ranges.getValidHigh() + "";
                        else if(Double.compare(dValue, ranges.getValidHigh()) > 0) //validHigh < dValue <= outlierHigh
                            newVarValue = ranges.getValidHigh() + "";
                        else                                    // validLow <= dValue <= validHigh
                            newVarValue = varValue;
                    } // if
                    else newVarValue = missingValuePlaceHolder;


                    //append each line component
                    linesBuilder.append(",").append(Utils.padLeftSpaces(newVarValue, defaultPaddingLength));
                } // for each line component

                //new line
                linesBuilder.append("\n");

            } // for each line


            //write results to local file system, method might return null if IO exception occurs
            //String newFilePath = Utils.writeToLocalFileSystem(localFilePath, newDirNameToPutFiles, linesBuilder.toString(), "csv");
            //if(newFilePath != null)
            //    newFilePaths.add(newFilePath);
            //else
            //    System.err.println("newFilePath is null at removeOutliersMethod() method");


            //update the map with the string value of the linesBuilder
            newFilePathFileContentsMap.put(localFilePath, linesBuilder.toString());
        } // for each file


        //return newFilePaths;
        return newFilePathFileContentsMap;
    } // removeOutliers



    //helper method to combine:
    //NIDiasABP with DiasABP
    //NIMAP with MAP
    //NISysABP with SysABP
    public static Map<String, String> combineVars(TreeSet<String> consideredVars, Map<String, String> filePathFileContentsMap,
                                                  String missingValuePlaceHolder, BloodPressurePreference bbp)
    {
        System.out.println("Combine step for vars started...");

        String varDiasABP = "DiasABP";
        String varNIDiasABP = "NIDiasABP";
        String varMAP = "MAP"; // mean arterial pressure or mean arterial blood pressure (mabp)
        String varNIMAP = "NIMAP";
        String varSysABP = "SysABP";
        String varNISysABP = "NISysABP";


        //new vars
        String newVarDiasABP = "(NI)DiasABP";
        String newVarMAP = "(NI)MAP";
        String newVarSysABP = "(NI)SysABP";


        TreeSet<String> newConsideredVars = new TreeSet<>(consideredVars);
        newConsideredVars.remove(varDiasABP);
        newConsideredVars.remove(varNIDiasABP);
        newConsideredVars.remove(varMAP);
        newConsideredVars.remove(varNIMAP);
        newConsideredVars.remove(varSysABP);
        newConsideredVars.remove(varNISysABP);

        //add new vars to the tree set
        newConsideredVars.add(newVarDiasABP);
        newConsideredVars.add(newVarMAP);
        newConsideredVars.add(newVarSysABP);


        //calculate the padding length
        int defaultPaddingLength = Utils.defaultPaddingLength(newConsideredVars);


        //convert the consideredVars to the list
        ArrayList<String> consideredVarsList = new ArrayList<>(consideredVars);
        int indexOfDiasABP = consideredVarsList.indexOf(varDiasABP) + 1; // + 1 is because line contains one more variable called tsMinutes
        int indexOfNIDiasABP = consideredVarsList.indexOf(varNIDiasABP) + 1;
        int indexOfMAP = consideredVarsList.indexOf(varMAP) + 1;
        int indexOfNIMAP = consideredVarsList.indexOf(varNIMAP) + 1;
        int indexOfSysABP = consideredVarsList.indexOf(varSysABP) + 1;
        int indexOfNISysABP = consideredVarsList.indexOf(varNISysABP) + 1;



        //new filePathFileContentsMap after combining vars
        LinkedHashMap<String, String> newFilePathFileContentsMap = new LinkedHashMap<>();



        for(String localFilePath : filePathFileContentsMap.keySet())
        {
            String fileContents = filePathFileContentsMap.get(localFilePath);

            StringBuilder linesBuilder
                    = new StringBuilder(Utils.padLeftSpaces("tsMinutes", defaultPaddingLength));
            //now populate the first line with new considered vars
            for(String newConsideredVarName : newConsideredVars)
                linesBuilder.append(",").append(Utils.padLeftSpaces(newConsideredVarName, defaultPaddingLength));
            linesBuilder.append("\n");


            try (Scanner scanner = new Scanner(fileContents))
            {
                //discard the first line
                //String headerLine =
                scanner.nextLine();


                //METADATA is obtained by GeneralDescriptorsRecords
                //append metadata, metadata is available as the last component in the header row
                //String[] splits = headerLine.split(",");
                //linesBuilder.append(",").append(splits[splits.length - 1]);
                //linesBuilder.append("\n");


                //int lineNumber = 1;

                //now read each line one by one
                while(scanner.hasNextLine())
                {
                    String thisLine = scanner.nextLine();

                    //lineNumber++;

                    //now split the line into contents
                    //each line contains tsMinutes and values of variables in the alphabetical order of variable names
                    //alphabetical order of variable names are present in consideredVars argument
                    String[] thisLineComponents = thisLine.split(",");

                    //variable values starting from the index 1, because index 0 is tsMinutes
                    //mABP and niMABP will be converted to double; niSysABP, sysABP, niDiasABP, diasABP will be converted to integer
                    //if number format exception occurs then it is a missing value

                    //IMPORTANT: use trim()
                    //Integer.parseInt(" 5"); // space before; yields NumberFormatException
                    //Integer.parseInt("5 "); // space after; yields NumberFormatException

                    String diasABP = thisLineComponents[indexOfDiasABP].trim(); // invasive
                    String niDiasABP = thisLineComponents[indexOfNIDiasABP].trim(); // non-invasive

                    String sysABP = thisLineComponents[indexOfSysABP].trim(); // invasive
                    String niSysABP = thisLineComponents[indexOfNISysABP].trim(); // non-invasive

                    String mABP = thisLineComponents[indexOfMAP].trim(); // invasive
                    String niMABP = thisLineComponents[indexOfNIMAP].trim(); // non-invasive


                    //All valid values for general descriptors, time series variables, outcome-related descriptors are non-negative (≥ 0).
                    //A value of -1 indicates missing or unknown data (for example, if a patient's height was not recorded).


                    //VAR 1
                    String newDiasABP = combineIntRepresentableVars(diasABP, niDiasABP, missingValuePlaceHolder, bbp);


                    //VAR 2
                    String newSysABP = combineIntRepresentableVars(sysABP, niSysABP, missingValuePlaceHolder, bbp);


                    //VAR 3
                    String newMABP = combineDoubleRepresentableVars(mABP, niMABP, missingValuePlaceHolder, bbp);


                    //TRY TO POPULATE missing sbp from (map and dbp), missing dbp from (sbp, map), or missing map from (sbp, dbp)
                    int sbp = Utils.isInteger(newSysABP) ? Integer.parseInt(newSysABP) : -1;
                    int dbp = Utils.isInteger(newDiasABP) ? Integer.parseInt(newDiasABP) : -1;
                    double map = Utils.isDouble(newMABP) ? Double.parseDouble(newMABP) : -1.0;

                    //if newMABP value is not present, then calculate it from sbp and dbp, otherwise keep original newMABP value
                    if (sbp >= 0 && dbp >= 0 && Double.compare(map, 0.0) < 0) // if map is missing
                    {
                        map = Utils.map(sbp, dbp); // may return -1, in case of incorrect data
                        newMABP = Double.compare(map, 0.0) >= 0 ? map + "" : missingValuePlaceHolder;
                    }
                    else if((sbp >= 0 && dbp < 0) &&  Double.compare(map, 0.0) >= 0) // only dbp missing, calculate it from variables map and sbp
                    {
                        dbp = Utils.dbp(map, sbp); // may return -1, in case of incorrect data
                        newDiasABP = dbp >= 0 ? dbp + "" : missingValuePlaceHolder;
                    }
                    else if((sbp < 0 && dbp >= 0) && Double.compare(map, 0.0) >= 0)  // only sbp missing, calculate it from variables map and dbp
                    {
                        sbp = Utils.sbp(map, dbp); // may return -1, in case of incorrect data
                        newSysABP = sbp >= 0 ? sbp + "" : missingValuePlaceHolder;
                    }
                    //else map is missing, and you cannot calculate either sbp or dbp, or one of (sbp, dbp) is missing, so map cannot be calculated


                    //System.err.println("File: " + localFilePath + " => "
                    //        + lineNumber + " => mABP: " + mABP + ", niMABP: " + niMABP + ", newMABP: " + newMABP +  ", sbp: " + sbp + ", dbp: " + dbp + ", cmap: "
                    //        + "*" /*Utils.map(sbp, dbp)*/ );


                    //now populate the lines
                    //according to alphabetical order (NI)DiasABP, (NI)MAP, (NI)SysABP are ordered as written and comes before any other variable name in the list
                    //therefore, after tsMinutes, values of these variables will be present and then the values of other variables will be added
                    linesBuilder.append(Utils.padLeftSpaces(thisLineComponents[0] + "", defaultPaddingLength)); //first put ts value


                    for(String newConsideredVar : newConsideredVars)
                    {
                        //handle new vars separately, they will added in the order of if else according to alphabetical order
                        if(newConsideredVar.equals(newVarDiasABP))
                            linesBuilder.append(",").append(Utils.padLeftSpaces(newDiasABP, defaultPaddingLength));
                        else if(newConsideredVar.equals(newVarMAP))
                            linesBuilder.append(",").append(Utils.padLeftSpaces(newMABP, defaultPaddingLength));
                        else if(newConsideredVar.equals(newVarSysABP))
                            linesBuilder.append(",").append(Utils.padLeftSpaces(newSysABP, defaultPaddingLength));
                        else
                        {
                            int indexOfOtherVar = consideredVarsList.indexOf(newConsideredVar) + 1;
                            String otherVarValue = thisLineComponents[indexOfOtherVar];

                            linesBuilder.append(",").append(Utils.padLeftSpaces(otherVarValue, defaultPaddingLength));
                        } // else
                    }
                    //add the end of line
                    linesBuilder.append("\n");

                } // while


                //update the map with a file path and its new file contents through the string value of linesBuilder
                newFilePathFileContentsMap.put(localFilePath, linesBuilder.toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }


            //Utils.writeToLocalFileSystem(localFilePath, "varJoin", linesBuilder.toString(), "csv");

        } // for each file


        //return the set of new vars
        //return newConsideredVars;

        System.out.println("Combine step for vars ended...");

        return newFilePathFileContentsMap;
    } // combineVars



    private enum BloodPressurePreference {PreferInvasive, PreferNonInvasive, PreferAverage};

    //method for combining int representable variable values
    private static String combineIntRepresentableVars(String invasiveVarValue, String nonInvasiveVarValue, String missingValuePlaceHolder, BloodPressurePreference bbp)
    {
        if(!Utils.isInteger(invasiveVarValue) && !Utils.isInteger(nonInvasiveVarValue))
            return missingValuePlaceHolder;
        else if(Utils.isInteger(invasiveVarValue) && !Utils.isInteger(nonInvasiveVarValue))
        {
            return Integer.parseInt(invasiveVarValue) >= 0 ? invasiveVarValue : missingValuePlaceHolder; // 0 is also a legal measurement
        }
        else if(!Utils.isInteger(invasiveVarValue) && Utils.isInteger(nonInvasiveVarValue))
        {
            return Integer.parseInt(nonInvasiveVarValue) >= 0 ? nonInvasiveVarValue : missingValuePlaceHolder; // 0 is also a legal measurement
        }
        else // both are integers
        {
            int val1 = Integer.parseInt(invasiveVarValue);
            int val2 = Integer.parseInt(nonInvasiveVarValue);

            if(val1 > 0 && val2 > 0)
            {
                //this paper suggests choosing invasive one over non-invasive: https://onlinelibrary.wiley.com/doi/full/10.1111/j.1365-2044.2012.07302.x
                switch(bbp)
                {
                    case PreferInvasive:
                        return val1 + "";
                    case PreferNonInvasive:
                        return val2 + "";
                    default:
                        //take their average
                        return ((val1 + val2) / 2) + ""; // integer division is OK here, since the original values are integers
                } // switch
            }
            else if(val1 > 0) // since val2 <= 0 here
            {
                return val1 + "";
            }
            else if(val2 > 0) // since val1 <= 0 here
            {
                return val2 + "";
            }
            else if(val1 == 0 && val2 == 0)
                return 0 + "";
            else // val1 < 0 && val2 < 0
            {
                return missingValuePlaceHolder;
            } // else
        } // else

    } // combineIntRepresentableVars



    private static String combineDoubleRepresentableVars(String invasiveVarValue, String nonInvasiveVarValue, String missingValuePlaceHolder, BloodPressurePreference bbp)
    {
        if(!Utils.isDouble(invasiveVarValue) && !Utils.isDouble(nonInvasiveVarValue))
            return missingValuePlaceHolder;
        else if(Utils.isDouble(invasiveVarValue) && !Utils.isDouble(nonInvasiveVarValue))
        {
            // 0 is also a legal measurement
            double var1DoubleValue = Double.parseDouble(invasiveVarValue);
            return Double.compare(var1DoubleValue, 0.0) >= 0 ? invasiveVarValue : missingValuePlaceHolder;
        }
        else if(!Utils.isDouble(invasiveVarValue) && Utils.isDouble(nonInvasiveVarValue))
        {
            // 0 is also a legal measurement
            double var2DoubleValue = Double.parseDouble(nonInvasiveVarValue);
            return Double.compare(var2DoubleValue, 0.0) >= 0 ? nonInvasiveVarValue : missingValuePlaceHolder;
        }
        else // both are doubles
        {
            double val1 = Double.parseDouble(invasiveVarValue);
            double val2 = Double.parseDouble(nonInvasiveVarValue);

            //val1 > 0 and val2 > 0
            if(Double.compare(val1, 0.0) > 0 && Double.compare(val2, 0.0) > 0)
            {
                //this paper suggests choosing invasive one over non-invasive: https://onlinelibrary.wiley.com/doi/full/10.1111/j.1365-2044.2012.07302.x
                switch(bbp)
                {
                    case PreferInvasive:
                        return val1 + "";
                    case PreferNonInvasive:
                        return val2 + "";
                    default:
                        //take their average
                        double average = Utils.format("#.##", RoundingMode.HALF_UP, (val1 + val2) / 2.0);
                        return average + "";
                } // switch
            }
            else if(Double.compare(val1, 0.0) > 0) // since val2 <= 0 here
            {
                return val1 + "";
            }
            else if(Double.compare(val2, 0.0) > 0) // since val1 <= 0 here
            {
                return val2 + "";
            }
            else if(Double.compare(val1, 0.0) == 0 && Double.compare(val2, 0.0) == 0) // both are 0
                return 0 + ""; // 0 is also a legal measurement
            else // val1 < 0 && val2 < 0
            {
                return missingValuePlaceHolder;
            } // else
        } // else
    } // combineDoubleRepresentableVars


    //helper method to remove outliers based on specified variable ranges
    //write files to the local file system and return their paths as a list
    public static List<String> removeOutliers(VarRanges varRanges, TreeSet<String> consideredVars, List<String> allPreProFilePaths,
                                              String missingValuePlaceHolder, String newDirNameToPutFiles)
    {
        //the new file paths to return
        List<String> newFilePaths = new ArrayList<>();


        //create a list from consideredVars which keeps the insertion order
        ArrayList<String> consideredVarsList = new ArrayList<>(consideredVars);
        //also add tsMinutes into considered var list at the index of 0
        consideredVarsList.add(0, "tsMinutes");


        //calculate the padding length
        int defaultPaddingLength = Utils.defaultPaddingLength(consideredVars);


        //now read all files one by one and remove outliers
        for(String localFilePath : allPreProFilePaths)
        {
            String fileContents = Utils.fileContentsFromLocalFilePath(localFilePath);
            String[] lines = StringUtils.split(fileContents, "\r\n|\r|\n");


            StringBuilder linesBuilder
                    = new StringBuilder(Utils.padLeftSpaces("tsMinutes", defaultPaddingLength));
            //now populate the first line with new considered vars
            for(String consideredVarName : consideredVars)
                linesBuilder.append(",").append(Utils.padLeftSpaces(consideredVarName, defaultPaddingLength));


            //METADATA is obtained by GeneralDescriptorsRecords
            //append metadata, metadata is available as the last component in the header row
            //String[] splits = lines[0].split(",");
            //linesBuilder.append(",").append(splits[splits.length - 1]);


            //new line
            linesBuilder.append("\n");


            //for each line, split and obtain variable value, skip the first line which is the header line
            for(int index = 1; index < lines.length; index ++)
            {
                String thisLine = lines[index];

                //obtain the components of this line
                String[] thisLineComponents = thisLine.split(",");


                //append tsMinutes first, it is at index 0
                linesBuilder.append(Utils.padLeftSpaces(thisLineComponents[0], defaultPaddingLength));


                //thisLineComponents and consideredVarsList have the same length and variable names, values are indexed at the same location
                for(int idx = 1; idx < thisLineComponents.length; idx ++) // skip tsMinutes by starting at idx 1
                {
                    String varName = consideredVarsList.get(idx);
                    String varValue = thisLineComponents[idx];

                    //obtain ranges for the current variable
                    Ranges ranges = varRanges.getRanges(varName);

                    //new var value after outlier removal
                    String newVarValue;

                    //all variable values are numeric and can be converted to double
                    //the check will also eliminate parsing of missing or incorrect data
                    if(Utils.isDouble(varValue))
                    {
                        //V.ix[V < ranges.OUTLIER_LOW[variable]] = np.nan
                        //V.ix[V > ranges.OUTLIER_HIGH[variable]] = np.nan
                        //V.ix[V < ranges.VALID_LOW[variable]] = ranges.VALID_LOW[variable]
                        //V.ix[V > ranges.VALID_HIGH[variable]] = ranges.VALID_HIGH[variable]

                        double dValue = Double.parseDouble(varValue);

                        if(Double.compare(dValue, ranges.getOutlierLow()) < 0)
                            newVarValue = missingValuePlaceHolder;
                        else if(Double.compare(dValue, ranges.getOutlierHigh()) > 0)
                            newVarValue = missingValuePlaceHolder;
                            //at this point -> outlierLow <= dValue <= outlierHigh
                        else if(Double.compare(dValue, ranges.getValidLow()) < 0) //outlierLow <= dValue < validLow
                            newVarValue = ranges.getValidHigh() + "";
                        else if(Double.compare(dValue, ranges.getValidHigh()) > 0) //validHigh < dValue <= outlierHigh
                            newVarValue = ranges.getValidHigh() + "";
                        else                                    // validLow <= dValue <= validHigh
                            newVarValue = varValue;
                    } // if
                    else newVarValue = missingValuePlaceHolder;


                    //append each line component
                    linesBuilder.append(",").append(Utils.padLeftSpaces(newVarValue, defaultPaddingLength));
                } // for each line component

                //new line
                linesBuilder.append("\n");

            } // for each line


            //write results to local file system, method might return null if IO exception occurs
            String newFilePath = Utils.writeToLocalFileSystem(localFilePath, newDirNameToPutFiles, linesBuilder.toString(), "csv");
            if(newFilePath != null)
                newFilePaths.add(newFilePath);
            else
                System.err.println("newFilePath is null at removeOutliers() method");
        } // for each file


        return newFilePaths;
    } // removeOutliers



    //helper method to combine:
    //NIDiasABP with DiasABP
    //NIMAP with MAP
    //NISysABP with SysABP
    //write files to the local file system and return their paths as a list
    public static List<String> combineVars(TreeSet<String> consideredVars, List<String> allPreProFilePaths, String missingValuePlaceHolder, BloodPressurePreference bbp,
                                           String newDirNameToPutFiles)
    {
        //the new file paths to return
        List<String> newFilePaths = new ArrayList<>();


        String varDiasABP = "DiasABP";
        String varNIDiasABP = "NIDiasABP";
        String varMAP = "MAP"; // mean arterial pressure or mean arterial blood pressure (mabp)
        String varNIMAP = "NIMAP";
        String varSysABP = "SysABP";
        String varNISysABP = "NISysABP";


        //new vars
        String newVarDiasABP = "(NI)DiasABP";
        String newVarMAP = "(NI)MAP";
        String newVarSysABP = "(NI)SysABP";


        TreeSet<String> newConsideredVars = new TreeSet<>(consideredVars);
        newConsideredVars.remove(varDiasABP);
        newConsideredVars.remove(varNIDiasABP);
        newConsideredVars.remove(varMAP);
        newConsideredVars.remove(varNIMAP);
        newConsideredVars.remove(varSysABP);
        newConsideredVars.remove(varNISysABP);

        //add new vars to the tree set
        newConsideredVars.add(newVarDiasABP);
        newConsideredVars.add(newVarMAP);
        newConsideredVars.add(newVarSysABP);


        //calculate the padding length
        int defaultPaddingLength = Utils.defaultPaddingLength(newConsideredVars);


        //convert the consideredVars to the list
        ArrayList<String> consideredVarsList = new ArrayList<>(consideredVars);
        int indexOfDiasABP = consideredVarsList.indexOf(varDiasABP) + 1; // + 1 is because line contains one more variable called tsMinutes
        int indexOfNIDiasABP = consideredVarsList.indexOf(varNIDiasABP) + 1;
        int indexOfMAP = consideredVarsList.indexOf(varMAP) + 1;
        int indexOfNIMAP = consideredVarsList.indexOf(varNIMAP) + 1;
        int indexOfSysABP = consideredVarsList.indexOf(varSysABP) + 1;
        int indexOfNISysABP = consideredVarsList.indexOf(varNISysABP) + 1;

        for(String localFilePath : allPreProFilePaths)
        {
            String fileContents = Utils.fileContentsFromLocalFilePath(localFilePath);

            StringBuilder linesBuilder
                    = new StringBuilder(Utils.padLeftSpaces("tsMinutes", defaultPaddingLength));
            //now populate the first line with new considered vars
            for(String newConsideredVarName : newConsideredVars)
                linesBuilder.append(",").append(Utils.padLeftSpaces(newConsideredVarName, defaultPaddingLength));
            linesBuilder.append("\n");


            try (Scanner scanner = new Scanner(fileContents))
            {
                //discard the first line
                //String headerLine =
                scanner.nextLine();


                //METADATA is obtained by GeneralDescriptorsRecords
                //append metadata, metadata is available as the last component in the header row
                //String[] splits = headerLine.split(",");
                //linesBuilder.append(",").append(splits[splits.length - 1]);
                //linesBuilder.append("\n");


                //int lineNumber = 1;

                //now read each line one by one
                while(scanner.hasNextLine())
                {
                    String thisLine = scanner.nextLine();

                    //lineNumber++;

                    //now split the line into contents
                    //each line contains tsMinutes and values of variables in the alphabetical order of variable names
                    //alphabetical order of variable names are present in consideredVars argument
                    String[] thisLineComponents = thisLine.split(",");

                    //variable values starting from the index 1, because index 0 is tsMinutes
                    //mABP and niMABP will be converted to double; niSysABP, sysABP, niDiasABP, diasABP will be converted to integer
                    //if number format exception occurs then it is a missing value

                    //IMPORTANT: use trim()
                    //Integer.parseInt(" 5"); // space before; yields NumberFormatException
                    //Integer.parseInt("5 "); // space after; yields NumberFormatException

                    String diasABP = thisLineComponents[indexOfDiasABP].trim(); // invasive
                    String niDiasABP = thisLineComponents[indexOfNIDiasABP].trim(); // non-invasive

                    String sysABP = thisLineComponents[indexOfSysABP].trim(); // invasive
                    String niSysABP = thisLineComponents[indexOfNISysABP].trim(); // non-invasive

                    String mABP = thisLineComponents[indexOfMAP].trim(); // invasive
                    String niMABP = thisLineComponents[indexOfNIMAP].trim(); // non-invasive


                    //All valid values for general descriptors, time series variables, outcome-related descriptors are non-negative (≥ 0).
                    //A value of -1 indicates missing or unknown data (for example, if a patient's height was not recorded).


                    //VAR 1
                    String newDiasABP = combineIntRepresentableVars(diasABP, niDiasABP, missingValuePlaceHolder, bbp);


                    //VAR 2
                    String newSysABP = combineIntRepresentableVars(sysABP, niSysABP, missingValuePlaceHolder, bbp);


                    //VAR 3
                    String newMABP = combineDoubleRepresentableVars(mABP, niMABP, missingValuePlaceHolder, bbp);


                    //TRY TO POPULATE missing sbp from (map and dbp), missing dbp from (sbp, map), or missing map from (sbp, dbp)
                    int sbp = Utils.isInteger(newSysABP) ? Integer.parseInt(newSysABP) : -1;
                    int dbp = Utils.isInteger(newDiasABP) ? Integer.parseInt(newDiasABP) : -1;
                    double map = Utils.isDouble(newMABP) ? Double.parseDouble(newMABP) : -1.0;

                    //if newMABP value is not present, then calculate it from sbp and dbp, otherwise keep original newMABP value
                    if (sbp >= 0 && dbp >= 0 && Double.compare(map, 0.0) < 0) // if map is missing
                    {
                        map = Utils.map(sbp, dbp); // may return -1, in case of incorrect data
                        newMABP = Double.compare(map, 0.0) >= 0 ? map + "" : missingValuePlaceHolder;
                    }
                    else if((sbp >= 0 && dbp < 0) &&  Double.compare(map, 0.0) >= 0) // only dbp missing, calculate it from variables map and sbp
                    {
                        dbp = Utils.dbp(map, sbp); // may return -1, in case of incorrect data
                        newDiasABP = dbp >= 0 ? dbp + "" : missingValuePlaceHolder;
                    }
                    else if((sbp < 0 && dbp >= 0) && Double.compare(map, 0.0) >= 0)  // only sbp missing, calculate it from variables map and dbp
                    {
                        sbp = Utils.sbp(map, dbp); // may return -1, in case of incorrect data
                        newSysABP = sbp >= 0 ? sbp + "" : missingValuePlaceHolder;
                    }
                    //else map is missing, and you cannot calculate either sbp or dbp, or one of (sbp, dbp) is missing, so map cannot be calculated


                    //System.err.println("File: " + localFilePath + " => "
                    //        + lineNumber + " => mABP: " + mABP + ", niMABP: " + niMABP + ", newMABP: " + newMABP +  ", sbp: " + sbp + ", dbp: " + dbp + ", cmap: "
                    //        + "*" /*Utils.map(sbp, dbp)*/ );


                    //now populate the lines
                    //according to alphabetical order (NI)DiasABP, (NI)MAP, (NI)SysABP are ordered as written and comes before any other variable name in the list
                    //therefore, after tsMinutes, values of these variables will be present and then the values of other variables will be added
                    linesBuilder.append(Utils.padLeftSpaces(thisLineComponents[0] + "", defaultPaddingLength)); //first put ts value


                    for(String newConsideredVar : newConsideredVars)
                    {
                        //handle new vars separately, they will added in the order of if else according to alphabetical order
                        if(newConsideredVar.equals(newVarDiasABP))
                            linesBuilder.append(",").append(Utils.padLeftSpaces(newDiasABP, defaultPaddingLength));
                        else if(newConsideredVar.equals(newVarMAP))
                            linesBuilder.append(",").append(Utils.padLeftSpaces(newMABP, defaultPaddingLength));
                        else if(newConsideredVar.equals(newVarSysABP))
                            linesBuilder.append(",").append(Utils.padLeftSpaces(newSysABP, defaultPaddingLength));
                        else
                        {
                            int indexOfOtherVar = consideredVarsList.indexOf(newConsideredVar) + 1;
                            String otherVarValue = thisLineComponents[indexOfOtherVar];

                            linesBuilder.append(",").append(Utils.padLeftSpaces(otherVarValue, defaultPaddingLength));
                        } // else
                    }
                    //add the end of line
                    linesBuilder.append("\n");

                } // while

            } catch (Exception ex) {
                ex.printStackTrace();
            }


            String newFilePath = Utils.writeToLocalFileSystem(localFilePath, newDirNameToPutFiles, //"varJoin",
                    linesBuilder.toString(), "csv");
            if(newFilePath != null)
                newFilePaths.add(newFilePath);
            else
                System.err.println("newFilePath is null at combineVars() method");
        } // for


        //return the new file paths
        return newFilePaths;
    } // combineVars




    //helper method to construct time series data in the following structure:
    //ts1, var1Value, var2Value, var3Value, ....
    //ts2, var1Value, var2Value, var3Value, ....
    //...
    //write files to the local file system and return their paths as a list
    public static List<String> generateTimeSeriesData(HashSet<String> varsToDiscard, TreeSet<String> consideredVars,
                                                      List<String> allFilePaths, String missingValuePlaceHolder,
                                                      String newDirNameToPutFiles)
    {
        //metadata is added as the last component in the header row
        /*
        General descriptors:
        --------------------
        RecordID (a unique integer for each ICU stay)
        Age (years)
        Gender (0: female, or 1: male)
        Height (cm)
        ICUType (1: Coronary Care Unit, 2: Cardiac Surgery Recovery Unit,
        3: Medical ICU, or 4: Surgical ICU)
        Weight (kg)*.
        --------------------
        A value of -1 indicates missing or unknown data (for example, if a patient's height was not recorded).
         */

        //file paths generated at the end
        List<String> newFilePaths = new ArrayList<>();


        //default padding length is the length of the variable with the longest name
        int defaultPaddingLength = Utils.defaultPaddingLength(consideredVars);


        //list will keep the insertion order
        List<String> paddedConsideredVars = new ArrayList<>();
        for(String var : consideredVars)
            paddedConsideredVars.add(Utils.padLeftSpaces(var, defaultPaddingLength));

        String firstLineOfANewFile = Utils.padLeftSpaces("tsMinutes", defaultPaddingLength)
                + "," + String.join(",", paddedConsideredVars);
        List<String> firstLineComponents = new ArrayList<>(consideredVars); firstLineComponents.add(0, "tsMinutes");


        for(String filePath : allFilePaths)
        {
            //System.out.println("Processing file: " + filePath);

            String fileContents = Utils.fileContentsFromLocalFilePath(filePath);


            //all files are actually record ids
            String fileName = Utils.fileNameFromPath(filePath);
            int recordID = Integer.parseInt(FilenameUtils.removeExtension(fileName));


            //general descriptors or meta parameters will be appended as the last component to the header row
            //therefore, there is only one meta string builder for each file
            //StringBuilder metaBuilder = new StringBuilder().append(",").append("{"); //meta start withs ,{ => comma is for separating it from other variables


            //StringBuilder sb = new StringBuilder("");
            TreeMap<Integer, String[]> tsLineValuesMap = new TreeMap<Integer, String[]>();

            //obtain a scanner for file contents string
            try (Scanner scanner = new Scanner(fileContents))
            {
                //discard first line
                //first line of the file is in the form => Time,Parameter,Value
                scanner.nextLine();


                //StringBuilder sb;
                String[] lineValues;

                //now read the fileContents line by line
                while (scanner.hasNextLine())
                {
                    //read the fileContents line by line
                    String line = scanner.nextLine();


                    //the line structure is the following:
                    //time_stamp,variable_name,variable_value
                    String[] lineComponents = line.split(",");

                    //there are cases where the variable name is empty
                    //in this case, discard this line
                    //variable_name is the second component at "time_stamp,variable_name,variable_value"
                    if(lineComponents[1].trim().isEmpty()) continue;



                    String timeStampString = lineComponents[0];
                    int timeStampMinutes = Utils.toMinutes(timeStampString);

                    //for each time stamp create new entry in the tree map
                    lineValues = tsLineValuesMap.get(timeStampMinutes); // returns null if there is no such a key
                    if(lineValues == null) {
                        lineValues = new String[firstLineComponents.size()]; // with the size equal to number of vars + 1
                        for(int i = 0; i < firstLineComponents.size(); i++)
                            lineValues[i] = Utils.padLeftSpaces(missingValuePlaceHolder, defaultPaddingLength); //firstLineComponents.get(i).length());

                        //add time stamp value first
                        //which will be in the index 0
                        lineValues[0] = Utils.padLeftSpaces(timeStampMinutes + "", defaultPaddingLength); //firstLineComponents.get(0).length());

                        //put it to the map
                        tsLineValuesMap.put(timeStampMinutes, lineValues);
                    }


                    String variableName = lineComponents[1];
                    String variableValue = lineComponents[2];


                    ///handle MechVent carefully which is the variable we are not interested
                    if(variableName.equals("MechVent"))
                        continue; // continue to the next variable, each line represents one variable value


                    //discard weight too
                    if(variableName.equals("Weight"))
                        continue;


                    //handle general descriptor variables (which are only available at timestamp 0);
                    //handle weight carefully for timestamp 0
                    //varsToDiscard does not contain Weight
                    if(varsToDiscard.contains(variableName) && timeStampMinutes == 0)
                    {
                        //meta will have the following structure:
                        //{RecordID: value;Age: value;Gender: value;Height: value;ICUType: value;Weight: value}
                        //metaBuilder.append(variableName).append("=").append(variableValue).append(";");
                    }
                    //WE CONSIDER WEIGHT VARIABLE AS A TIME SERIES VARIABLE AT TIMESTAMP 0
                    //else if(variableName.equals("Weight") && timeStampMinutes == 0)
                    //{
                    //    //add weight at timestamp 0 as meta value
                    //    //weight will be last meta value to be appended
                    //    //metaBuilder.append(variableName).append("=").append(variableValue).append(";");
                    //}
                    else {
                        //get the index of variable name
                        int index = firstLineComponents.indexOf(variableName);
                        //add the variable value to the corresponding index, it will update the map values implicitly
                        lineValues[index]
                                = Utils.padLeftSpaces(ensureNonNegativityAndGet(variableValue, missingValuePlaceHolder),
                                defaultPaddingLength); //firstLineComponents.get(index).length());
                    }

                } // while


                //close meta and add it the first line of a new file
                //metaBuilder.append("InHospitalDeath").append("=").append(outcomes.get(recordID).getInHospitalDeath0Or1()).append("}");
                //firstLineOfANewFile += metaBuilder.toString();


                //explicitly remove the key == 0 and associated values, since, timestamp==0 has all variables empty
                //WRONG 133919.txt in set-a has pH value at time stamp 0, do not remove the key
                //tsLineValuesMap.remove(0);



                //now build a string from the values of variables and create a new file
                StringBuilder sb = new StringBuilder(firstLineOfANewFile).append("\n");
                //now for each key-value pair, add it to the sb
                for(Integer key : tsLineValuesMap.keySet())
                {
                    String[] lineVals = tsLineValuesMap.get(key);
                    sb.append(String.join(",", lineVals)).append("\n");
                } // for


                //write results to local file system, method might return null if IO exception occurs
                String newFilePath = Utils.writeToLocalFileSystem(filePath, newDirNameToPutFiles, sb.toString(), "csv");
                if(newFilePath != null)
                    newFilePaths.add(newFilePath);
                else
                    System.err.println("newFilePath is null at generateTimeSeriesData() method");

            } catch (Exception ex) {
                ex.printStackTrace();
                //if(ex.getMessage() == null)
                //throw new RuntimeException(ex);
            } // catch


        } // for each file


        return newFilePaths;
    } // generateTimeSeriesData







    //helper method to construct time series data in the following structure:
    //       ts1:   ts2:    ts3:
    //var1 :  1     2       3
    //var2:   1     2       3
    public static void generateTimeSeriesData2(HashSet<String> varsToDiscard, TreeSet<String> consideredVars, List<String> allFilePaths,
                                               String missingValuePlaceHolder)
    {
        //default padding length is the length of the variable with the longest name
        int defaultPaddingLength = Utils.defaultPaddingLength(consideredVars);

        for(String filePath : allFilePaths)
        {
            //System.out.println("Processing file: " + filePath);

            String fileContents = Utils.fileContentsFromLocalFilePath(filePath);

            //generate padding for the first component of the first line
            StringBuilder linesBuilder = new StringBuilder(Utils.padLeftSpaces("", defaultPaddingLength));


            //tree map to hold time series -> variable values
            TreeMap<Integer, String[]> tsLineValuesMap = new TreeMap<Integer, String[]>();


            //create an array list from considered vars, so that the order in the set is kept
            ArrayList<String> consideredVarList = new ArrayList<>(consideredVars);


            //obtain a scanner for file contents string
            try (Scanner scanner = new Scanner(fileContents))
            {
                //discard first line
                //first line of the file is in the form => Time,Parameter,Value
                scanner.nextLine();

                //StringBuilder sb;
                //TreeMap<Integer, String> tsVarValueMap;

                int maxTimeStampLength = 0;


                String[] lineValues;


                //now read the fileContents line by line
                while (scanner.hasNextLine())
                {
                    //read the fileContents line by line
                    String line = scanner.nextLine();


                    //the line structure is the following:
                    //time_stamp,variable_name,variable_value
                    String[] lineComponents = line.split(",");



                    //there are cases where the variable name is empty
                    //in this case, discard this line
                    //variable_name is the second component at "time_stamp,variable_name,variable_value"
                    if(lineComponents[1].trim().isEmpty()) continue;



                    String timeStampString = lineComponents[0];

                    //maxTimeStampLength will be used for padding of values
                    if(timeStampString.length() > maxTimeStampLength)
                        maxTimeStampLength = timeStampString.length();


                    int timeStampMinutes = Utils.toMinutes(timeStampString);


                    String variableName = lineComponents[1];

                    String variableValue = lineComponents[2];


                    //for each time stamp create new entry in the tree map
                    lineValues = tsLineValuesMap.get(timeStampMinutes); // returns null if there is no such a key
                    if(lineValues == null) {
                        lineValues = new String[consideredVars.size() + 1]; // with the size equal to number of vars + 1 (+1 for time stamp)
                        Arrays.fill(lineValues, missingValuePlaceHolder);

                        //add time stamp value first
                        //which will be in the index 0
                        lineValues[0] = timeStampMinutes + "";

                        //put it to the map
                        tsLineValuesMap.put(timeStampMinutes, lineValues);
                    }


                    //handle general descriptor variables; handle weight carefully for timestamp 0
                    if(varsToDiscard.contains(variableName))
                    {
                        // do nothing
                    }
                    //else if(variableName.equals("Weight") && timeStampMinutes == 0)
                    //{
                    //
                    //}
                    else {
                        //get the index of variable name
                        int index = consideredVarList.indexOf(variableName) + 1; // +1 for time stamp
                        //add the variable value to the corresponding index, it will update the map values implicitly
                        lineValues[index] = ensureNonNegativityAndGet(variableValue, missingValuePlaceHolder);
                    } // else

                } // while


                //explicitly remove the key == 0 and associated values, since, timestamp 0 has all variables empty
                //WRONG 133919.txt in set-a has pH value at time stamp 0, do not remove the key
                //tsLineValuesMap.remove(0);


                //now populate the map in the form:
                //       ts1:   ts2:    ts3:
                //var1 :  1     2       3
                //var2:   1     2       3


                //transformation snippet
                TreeMap<String, ArrayList<String>> varTsVarValuesMap = new TreeMap<>();
                for(String var : consideredVars)
                {
                    //var values are listed according to the time stamp order or chronological order
                    ArrayList<String> valuesOfThisVarInEachTimeStamp = new ArrayList<>();
                    for(Integer thisTimeStamp : tsLineValuesMap.keySet())
                    {
                        String[] lineVals = tsLineValuesMap.get(thisTimeStamp);
                        int varIndex = consideredVarList.indexOf(var) + 1;
                        String varValueAtThisTimeStamp = lineVals[varIndex];

                        //populate arraylist
                        valuesOfThisVarInEachTimeStamp.add(varValueAtThisTimeStamp);
                    } // for

                    //now populate the tree map
                    varTsVarValuesMap.put(var, valuesOfThisVarInEachTimeStamp);
                } // for



                //now populate the first line
                for(Integer timeStampMinutes : tsLineValuesMap.keySet())
                {
                    linesBuilder
                            .append(",")
                            .append(Utils.padLeftSpaces("tsm: " + timeStampMinutes, maxTimeStampLength + 5)); // +5 for tsm:
                } // for
                linesBuilder.append("\n");


                //now populate the lines with values
                for(String var : varTsVarValuesMap.keySet())
                {
                    linesBuilder.append(Utils.padLeftSpaces(var, defaultPaddingLength));

                    for(String valueOfThisVarInThisTimeStamp : varTsVarValuesMap.get(var))
                    {
                        linesBuilder
                                .append(",")
                                .append(Utils.padLeftSpaces(valueOfThisVarInThisTimeStamp, maxTimeStampLength + 5)); // +5 for tsm:
                    } // for

                    linesBuilder.append("\n");
                } // for



                //write results to local file system
                Utils.writeToLocalFileSystem(filePath, "prepro2", linesBuilder.toString(), "csv");


            } catch (Exception ex) {
                ex.printStackTrace();


            } // catch

        } // for

    } // generateTimeSeriesData2

} // Preprocessing
