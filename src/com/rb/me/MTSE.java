package com.rb.me;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

//TODO further memory optimization seems possible (the current version vs. the proposed version in terms of parallelStream runtime)
// var name orders are not important -> horizontalData.keySet()
// ts orders are important -> define data structure which hold ts sorted list and timestamps for that list (where timestamps will be retrievable)
// in this case, horizontalData can be removed by keeping listOfVarValuesInTsOrder as an only value-holding instance variable
//class to handle multivariate time series of patient files
public class MTSE implements Serializable, Comparable<MTSE>
{
    //dataset of this multivariate time series
    private Dataset dataset;

    //unique record id
    private int recordID;

    //time stamps hash to make this mtse comparable to other with same recordID and dataset (especially during embedding)
    private int tssHash;


    //vars hash is important during transformations
    private int varsHash;


    //horizontality and verticality are determined by the positioning of values of each variable
    //main data structure to hold the time series; ts -> (var -> varValue)
    //    var1 var2 var3
    //ts1
    //ts2
    //TreeMap<Integer, TreeMap<String, Double>> verticalData; // gives us a change to decrease, increase, change timestamps

    //var -> (ts -> varValue) // over ordered time stamps
    //     ts1 ts2 ts3 ts4
    //var1
    //var2
    TreeMap<String, TreeMap<Integer, Double>> horizontalData; // gives us a chance to increase, decrease, change variables


    //separate instance variable to hold the list of var values in ts order
    //outer list is in var name order and inner list is in ts order
    //List<List<Double>> listOfVarValuesInTimestampOrder;


    //vertical masking for this multivariate time series
    //if variable d is observed at time stamp t, then masking[t][d] = 1, otherwise masking[t][d] = 0
    //    var1 var2 var3
    //ts1
    //ts2
    //TreeMap<Integer, TreeMap<String, Integer>> verticalMaskingVector; // ts -> (var -> maskingValue)

    //horizontal masking vector
    //if variable d is observed at time stamp t, then masking[d][t] = 1, otherwise masking[d][t] = 0
    //     ts1 ts2
    //var1
    //var2
    //TreeMap<String, TreeMap<Integer, Integer>> horizontalMaskingVector; // var -> (ts -> maskingValue)


    //constructor
    //will take a record id, tree set of time stamps, tree set of var names, and list of value arrays of corresponding vars
    public MTSE(Dataset dataset, int recordID, List<Integer> timeStamps, List<String> varNames, List<List<Double>> varValuesInTsOrder)
    {
        //prechecks
        if(timeStamps.isEmpty() || varNames.isEmpty() || varValuesInTsOrder.isEmpty() || atLeastOneEmpty(varValuesInTsOrder))
            throw new RuntimeException("Provided collections cannot be empty");

        if(varValuesInTsOrder.size() != varNames.size())
            throw new RuntimeException("var names, and their corresponding values should have the same size!");

        for(List<Double> thisVarValues : varValuesInTsOrder)
        {
            if(thisVarValues.size() != timeStamps.size())
                throw new RuntimeException("Number of time stamps and number of variable values should be the same");
        } // for


        //assign dataset
        this.dataset = dataset;
        //assign record id
        this.recordID = recordID;
        //assign tss hash
        // TODO this can be problematic when some timestamps are removed from the data structure,
        //  make sure that timestamps cannot be removed from the original time series
        this.tssHash = timeStamps.hashCode();


        //it becomes important when transformations are performed
        //TODO also make sure that variables are not removed from the original MTSE, otherwise this won't make sense
        this.varsHash = varNames.hashCode();


        //convert varNames to array
        String[] varNamesArray = varNames.toArray(new String[]{});
        //convert time stamps to array
        Integer[] tsArray = timeStamps.toArray(new Integer[]{});


        //initialize horizontal data
        horizontalData = new TreeMap<String, TreeMap<Integer, Double>>();
        for(int varIndex = 0; varIndex < varNamesArray.length; varIndex ++)
        {
            TreeMap<Integer, Double> tsVarValueMap = new TreeMap<>();

            //populate horizontal data
            for(int tsIndex = 0; tsIndex < tsArray.length; tsIndex ++)
            {
                tsVarValueMap.put(tsArray[tsIndex], varValuesInTsOrder.get(varIndex).get(tsIndex));
            }

            horizontalData.put(varNamesArray[varIndex], tsVarValueMap);
        } // for


        //call to the original method
        //listOfVarValuesInTimestampOrder = listOfVarValuesInTsOrder();


        ////initialize vertical data
        //verticalData = new TreeMap<Integer, TreeMap<String, Double>>();
        //for(int tsIndex = 0; tsIndex < tsArray.length; tsIndex ++)
        //{
        //    TreeMap<String, Double> varValueMap = new TreeMap<>();
        //
        //    for(int varIndex = 0; varIndex < varNamesArray.length; varIndex ++)
        //    {
        //        varValueMap.put(varNamesArray[varIndex], varValuesInTsOrder.get(varIndex).get(tsIndex));
        //    } // for
        //
        //    verticalData.put(tsArray[tsIndex], varValueMap);
        //} // for



        ////initialize vertical masking vector from vertical data
        //verticalMaskingVector = new TreeMap<>();
        //for(Integer ts : verticalData.keySet())
        //{
        //    //var values in this ts
        //    TreeMap<String, Double> varValuesInThisTs = verticalData.get(ts);
        //
        //    TreeMap<String, Integer> maskingVectorElementsForThisTs = new TreeMap<>();
        //    for(String var : varValuesInThisTs.keySet())
        //    {
        //        //if it is non-negative the masking value is 1
        //        //note that missing values are marked with negative values in each MTSe
        //        if(Double.compare(varValuesInThisTs.get(var), 0.0) >= 0)
        //            maskingVectorElementsForThisTs.put(var, 1);
        //        else
        //            maskingVectorElementsForThisTs.put(var, 0);
        //    } // for each var
        //
        //    verticalMaskingVector.put(ts, maskingVectorElementsForThisTs);
        //} // for each ts



        ////now initialize horizontal masking vector
        //horizontalMaskingVector = new TreeMap<>();
        //for(String var : horizontalData.keySet())
        //{
        //    //obtain the internal map of timestamp and var values
        //    TreeMap<Integer, Double> varValuesInTimeStampOrder = horizontalData.get(var);
        //
        //    TreeMap<Integer, Integer> maskingVectorElementsForThisVar = new TreeMap<>();
        //    for(Integer ts : varValuesInTimeStampOrder.keySet())
        //    {
        //        //if it is non-negative the masking value is 1
        //        //note that missing values are marked with negative values in each MTSe
        //        if(Double.compare(varValuesInTimeStampOrder.get(ts), 0.0) >= 0)
        //            maskingVectorElementsForThisVar.put(ts, 1);
        //        else
        //            maskingVectorElementsForThisVar.put(ts, 0);
        //    } // for each ts
        //
        //    horizontalMaskingVector.put(var, maskingVectorElementsForThisVar);
        //} // for each var

    } // MTSE


    //getter method for masking vector
    //public TreeMap<Integer, TreeMap<String, Integer>> getVerticalMaskingVector() {
    //    return verticalMaskingVector;
    //}

    //public TreeMap<String, TreeMap<Integer, Integer>> getHorizontalMaskingVector() {
    //    return horizontalMaskingVector;
    //}

    //helper method to get maskings in ts order
    public Map<String, List<Integer>> getMaskingsInTsOrder()
    {
        Map<String, List<Integer>> maskingsInTsOrder = new HashMap<>();


        for(String var : horizontalData.keySet())
        {
            TreeMap<Integer, Double> valuesInTsOrderForThisVar = horizontalData.get(var);

            //list of maskings for this var
            List<Integer> maskingsForThisVarInTsOrder = new ArrayList<>();

            for(Integer ts : valuesInTsOrderForThisVar.keySet())
            {
                double valueOfThisVarInThisTs = valuesInTsOrderForThisVar.get(ts);
                if(Double.compare(valueOfThisVarInThisTs, 0.0) >= 0)
                    maskingsForThisVarInTsOrder.add(1);
                else
                    maskingsForThisVarInTsOrder.add(0);
            } // for


            //update map
            maskingsInTsOrder.put(var, maskingsForThisVarInTsOrder);
        } // for



        //for(String var : horizontalMaskingVector.keySet())
        //{
        //    TreeMap<Integer, Integer> tsMaskingMap = horizontalMaskingVector.get(var);
        //
        //    //list of maskings for this var
        //    List<Integer> maskingsForThisVarInTsOrder = new ArrayList<>();
        //
        //    for(Integer ts : tsMaskingMap.keySet())
        //    {
        //        maskingsForThisVarInTsOrder.add(tsMaskingMap.get(ts));
        //    } // for
        //
        //    maskingsInTsOrder.put(var, maskingsForThisVarInTsOrder);
        //} // for

        return maskingsInTsOrder;
    } // getMaskingsInTsOrder


    //helper method to get values in Var order
    //   var1 var2 var3 ...
    //ts1
    //ts2
    //ts3
    //...
    //it will return one row in above matrix
    public Map<Integer, List<Double>> getValuesInVarNameOrder()
    {
        LinkedHashMap<Integer, List<Double>> valuesInVarNameOrder = new LinkedHashMap<>();


        for(String var : horizontalData.keySet())
        {
            TreeMap<Integer, Double> valuesInTsOrderForThisVar = horizontalData.get(var);
            for(Integer ts : valuesInTsOrderForThisVar.keySet())
            {
                //get returns null if the key does not exist
                List<Double> valuesForThisTs = valuesInVarNameOrder.get(ts);
                if(valuesForThisTs == null)
                {
                    valuesForThisTs = new ArrayList<>();
                    valuesInVarNameOrder.put(ts, valuesForThisTs);
                } // if

                valuesForThisTs.add(valuesInTsOrderForThisVar.get(ts));
            } // for
        } // for


        //for(Integer ts : verticalData.keySet())
        //{
        //    //row of values for this ts
        //    List<Double> row = new ArrayList<>();
        //
        //    //var_name -> value
        //    //it has var name order
        //    TreeMap<String, Double> varValueInThisTs = verticalData.get(ts);
        //    for(String var : varValueInThisTs.keySet())
        //    {
        //        row.add(varValueInThisTs.get(var));
        //    } // for
        //
        //    valuesInVarNameOrder.put(ts, row);
        //} // for

        return valuesInVarNameOrder;
    } // for

    //private method to update vertical data when horizontal data are updated
    //private void updateVerticalData()
    //{
    //    for (Integer ts : verticalData.keySet())
    //    {
    //        TreeMap<String, Double> varVarValueMap = new TreeMap<>();
    //
    //        for (String varName : horizontalData.keySet())
    //        {
    //            varVarValueMap.put(varName, horizontalData.get(varName).get(ts));
    //        } // for
    //
    //        verticalData.put(ts, varVarValueMap);
    //    } // for
    //} // updateVerticalData


    ////private method to update horizontal data when vertical data are updated
    //private void updateHorizontalData()
    //{
    //    for(String varName : horizontalData.keySet())
    //    {
    //        TreeMap<Integer, Double> tsVarValueMap = new TreeMap<>();
    //
    //         for(Integer ts : verticalData.keySet())
    //         {
    //             tsVarValueMap.put(ts, verticalData.get(ts).get(varName));
    //         } // for
    //
    //        horizontalData.put(varName, tsVarValueMap);
    //    } // for
    //
    //} // updateHorizontalData


    private <T> boolean atLeastOneEmpty(Collection<? extends Collection<T>> complexCollection)
    {
        for(Collection<T> element : complexCollection)
        {
            if(element.isEmpty())
                return true;
        } // for

        return false;
    } // atLeastOneEmpty


    //to vertical string method
    public String toVerticalString()
    {
        StringBuilder sb = new StringBuilder(recordID + ":\n" + paddedVarNamesHeading());

        List<String> varNames = getVars();
        varNames.add(0, "tsMinutes");
        int defaultPaddingLength = Utils.defaultPaddingLength(varNames);


        //obtain vertical data
        TreeMap<Integer, TreeMap<String, Double>> vertData = vertData();


        //now for each time stamp append values
        for(Integer ts : //verticalData
                            vertData.keySet())
        {
            sb.append(Utils.padLeftSpaces(ts + "", defaultPaddingLength));
            TreeMap<String, Double> varVarValuesMap = //verticalData
                                                        vertData.get(ts);

            for(String varName : varVarValuesMap.keySet())
            {
                sb.append(",").append(Utils.padLeftSpaces(varVarValuesMap.get(varName).toString(), defaultPaddingLength));
            } // for
            sb.append("\n");
        } // for

        return sb.toString();
    } // toVerticalString


    //helper method to obtain padded var names heading
    private String paddedVarNamesHeading()
    {
        List<String> varNames = getVars();
        varNames.add(0, "tsMinutes");

        //padding length
        int defaultPaddingLength = Utils.defaultPaddingLength(varNames);

        //now for each variable update string builder
        StringBuilder sb = new StringBuilder(Utils.padLeftSpaces(varNames.get(0), defaultPaddingLength));
        for(int index = 1; index < varNames.size(); index ++)
            sb.append(",").append(Utils.padLeftSpaces(varNames.get(index), defaultPaddingLength));
        sb.append("\n");

        return sb.toString();
    } // paddedVarNamesHeading


    ////helper method to print the masking vector string
    //public String toVerticalMaskingVectorString()
    //{
    //    StringBuilder sb = new StringBuilder(paddedVarNamesHeading());
    //
    //    List<String> varNames = getVars();
    //    varNames.add(0, "tsMinutes");
    //    int defaultPaddingLength = Utils.defaultPaddingLength(varNames);
    //
    //
    //    //now for each time stamp append values
    //    for(Integer ts : verticalMaskingVector.keySet())
    //    {
    //        sb.append(Utils.padLeftSpaces(ts + "", defaultPaddingLength));
    //        TreeMap<String, Integer> varMaskingsMap = verticalMaskingVector.get(ts);
    //
    //        for(String varName : varMaskingsMap.keySet())
    //        {
    //            sb.append(",").append(Utils.padLeftSpaces(varMaskingsMap.get(varName).toString(), defaultPaddingLength));
    //        } // for
    //        sb.append("\n");
    //    } // for
    //
    //    return sb.toString();
    //} // toVerticalMaskingVectorString


    //toHorizontalString method
    public String toHorizontalString()
    {
        //default padding for vars
        int defaultPaddingLengthForVars = Utils.defaultPaddingLength(horizontalData.keySet());

        List<Integer> timeStamps = getTimeStamps();
        String[] stringTimeStamps = new String[timeStamps.size()];
        for(int index = 0; index < stringTimeStamps.length; index ++) stringTimeStamps[index] = "tsm: " + timeStamps.get(index);
        int defaultPaddingLengthForTimestamps = Utils.defaultPaddingLength(stringTimeStamps);

        //choose the padding length which is bigger
        int finalPaddingLength = Math.max(defaultPaddingLengthForTimestamps, defaultPaddingLengthForVars);

        String[] paddedStringTimeStamps = new String[stringTimeStamps.length];
        for(int index = 0; index < paddedStringTimeStamps.length; index ++)
            paddedStringTimeStamps[index] = Utils.padLeftSpaces(stringTimeStamps[index], finalPaddingLength);

        //update string builder with initial empty padded string
        StringBuilder sb = new StringBuilder(Utils.padLeftSpaces("", defaultPaddingLengthForVars + 1)); //+1 for considering a space for comma
        //append time stamps for the header line
        sb.append(String.join(",", paddedStringTimeStamps));
        //append new line
        sb.append("\n");

        //now print values for each var
        for(String var : horizontalData.keySet())
        {
            sb.append(Utils.padLeftSpaces(var, defaultPaddingLengthForVars));
            TreeMap<Integer, Double> tsVarValuesMap = horizontalData.get(var);

            for(Integer ts : tsVarValuesMap.keySet())
            {
                sb.append(",").append(Utils.padLeftSpaces(tsVarValuesMap.get(ts) + "", finalPaddingLength));
            } // for

            //new line
            sb.append("\n");
        } // for

        return sb.toString();
    } // toHorizontalString


    //toString method executes toVerticalString method
    public String toString()
    {
        return toVerticalString();
    } // toString


    //toCSVString method without any padding but similar to toVerticalString()
    public String toCSVString(boolean discardTimeStamps, boolean withDateTime, boolean withHeader, boolean addClass)
    {
        List<String> varNames = getVars();
        //add class
        if(addClass) varNames.add("class");
        if(!discardTimeStamps) varNames.add(0, "tsMinutes");

        //initialize string builder with header
        StringBuilder sb;
        if(withHeader) {
            sb = new StringBuilder(String.join(",", varNames));
            sb.append("\n");
        }
        else sb = new StringBuilder("");


        //obtain vertical data
        TreeMap<Integer, TreeMap<String, Double>> vertData = vertData();


        //now for each time stamp append values
        for(Integer ts : //verticalData
                            vertData.keySet())
        {
            if(!discardTimeStamps)
            {
                if(!withDateTime)
                    sb.append(ts).append(",");
                else
                    sb.append(Utils.toDateString(TimeUnit.MINUTES.toMillis(ts), "yyyy-MM-dd HH:mm:ss")).append(",");
            } // if

            TreeMap<String, Double> varVarValuesMap = //verticalData
                                                        vertData.get(ts);

            List<String> stringValues = new ArrayList<>();
            for(Double val : varVarValuesMap.values())
            {
                stringValues.add(val.toString());
            } // for

            //add class
            if(addClass) stringValues.add(Outcomes.getInstance(dataset).get(recordID).getInHospitalDeath0Or1() + "");

            sb.append(String.join(",", stringValues));
            sb.append("\n");
        } // for

        return sb.toString();
    } // toCSVString


    //method to write mtse to file
    public void writeToFile(String destinationDirPath, String newDirNameToPutFile, String newFileExtension, boolean writeInPaddedFormat)
    {
        String newFileName
                = getRecordID() + "." + newFileExtension;
        try
        {
            String newDirPath = destinationDirPath + File.separator + newDirNameToPutFile;
            File newDir = new File(newDirPath);
            if(!newDir.exists()) newDir.mkdir();

            File newFile = new File( newDir.getAbsolutePath() + File.separator + newFileName);
            newFile.createNewFile();
            PrintWriter pw = new PrintWriter(newFile);
            pw.print(writeInPaddedFormat ? toVerticalString() : toCSVString(false, false, true, false));
            pw.close();

            System.out.println("Wrote file => " + newFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        } // catch
    } // writeToFile


    //get method for vars
    public List<String> getVars()
    {
        return new ArrayList<>(horizontalData.keySet());
    } // getVars

    //get method for time stamps
    public List<Integer> getTimeStamps()
    {
        List<String> vars = getVars();
        if(!vars.isEmpty())
        {
            return new ArrayList<>(horizontalData.get(vars.get(0)).keySet());
        }
        else
            return new ArrayList<>(); // return an empty list

        //return new ArrayList<>(verticalData.keySet());
    } // getTimeStamps


    //get variable its ts ordered values as a map
    public Map<String, List<Double>> getVarValuesInTsOrder()
    {
        HashMap<String, List<Double>> varValuesMapInTsOrder = new HashMap<>();
        for(String var : horizontalData.keySet())
        {
            TreeMap<Integer, Double> tsVarValues = horizontalData.get(var);
            List<Double> thisVarValuesInTsOrder = new ArrayList<>();
            for(Integer ts : tsVarValues.keySet())
                thisVarValuesInTsOrder.add(tsVarValues.get(ts));

            varValuesMapInTsOrder.put(var, thisVarValuesInTsOrder);
        } // for

        return varValuesMapInTsOrder;
    } // getVarValuesInTsOrder


    //set method for setting var values in ts order
    public void setVarValuesInTsOrder(Map<String, List<Double>> newVarValues)
    {
        for(String var : newVarValues.keySet())
        {
            List<Double> newValuesOfThisVar = newVarValues.get(var);

            //map associated with this var in horizontal data
            TreeMap<Integer, Double> tsVarValues = horizontalData.get(var);
            Integer[] tss = tsVarValues.keySet().toArray(new Integer[]{});

            if(tss.length != newValuesOfThisVar.size())
                throw new RuntimeException("number of timestamps does not agree");

            for(int tsIndex = 0; tsIndex < tss.length; tsIndex ++)
            {
                //update the internal map of horizontal data, tsVarValues references it
                tsVarValues.put(tss[tsIndex], newValuesOfThisVar.get(tsIndex));
            } // for each ts

        } // for each var

        //horizontal data are updated, so update vertical data too
        //updateVerticalData();
    } // setVarValuesInTsOrder


    //getter method for record id
    public int getRecordID() {
        return recordID;
    }

    public void setRecordID(int recordID) {
        this.recordID = recordID;
    }

    public Dataset getDataset() {
        return dataset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MTSE that = (MTSE) o;
        return  dataset == that.dataset
                && recordID == that.recordID
                && tssHash == that.tssHash
                && varsHash == that.varsHash;
    }

    @Override
    public int hashCode()
    {
        //we might have time series of the same record id with possibly different time stamps and variable names
        return Objects.hash(dataset, recordID, tssHash, varsHash);
    }


    //compareTo method for natural ordering
    public int compareTo(MTSE other)
    {
        int result = dataset.compareTo(other.dataset);
        if(result == 0)
        {
            result = Integer.compare(recordID, other.recordID);
            if(result == 0)
            {
                result = Integer.compare(tssHash, other.tssHash);
                if(result == 0)
                    result = Integer.compare(varsHash, other.varsHash);
            } // if
        } // if

        return result;
    } // compareTo


    public static MTSE fromFile(Dataset dataset, String fileName, String fileContents, String lineComponentDelimiter, double missingValuePlaceHolder)
    {
        int recordID = Integer.parseInt(FilenameUtils.removeExtension(fileName));

        //split the contents to lines
        String[] lines = StringUtils.split(fileContents, "\r\n|\r|\n");

        //tree set of time stamps
        List<Integer> timeStamps = new ArrayList<>();

        //obtain var names by splitting the first line
        String[] varNamesArray = lines[0].split(lineComponentDelimiter);
        //trim every element
        for(int index = 0; index < varNamesArray.length; index ++) varNamesArray[index] = varNamesArray[index].trim();
        // skip the first index which is "tsMinutes"
        List<String> varNames = Arrays.asList(varNamesArray).subList(1, varNamesArray.length);


        //list to hold the list of values of each var in time stamp order
        //List<List<Double>> varValuesInTsOrder = new ArrayList<>(varNames.size()); // have the same size as the number of vars

        //var and its values in ts order
        LinkedHashMap<String, List<Double>> varVarValuesMap = new LinkedHashMap<>();


        //for each line parse the line by collecting relevant information, skip first line which is the header line
        for(int lineIndex = 1; lineIndex < lines.length; lineIndex ++)
        {
            String thisLine = lines[lineIndex];

            //split the line with delimiter
            String[] thisLineComponents = thisLine.split(lineComponentDelimiter);
            for(int componentIndex = 0; componentIndex < thisLineComponents.length; componentIndex ++)
                thisLineComponents[componentIndex] = thisLineComponents[componentIndex].trim(); // trim every element


            //the first component is tsMinutes, which is integer
            int tsMinutes = Integer.parseInt(thisLineComponents[0]);
            timeStamps.add(tsMinutes);


            //start from the component 1 which is contains the value of the first variable
            for (int varIndex = 1; varIndex < thisLineComponents.length; varIndex++)
            {
                //if list does not exist for this var, create it
                if (!varVarValuesMap.containsKey(varNamesArray[varIndex])) varVarValuesMap.put(varNamesArray[varIndex], new ArrayList<>());

                //obtain values
                List<Double> valuesInTsOrder = varVarValuesMap.get(varNamesArray[varIndex]);

                //if it is a double parsable, then assign double value, otherwise assign Double.POSITIVE_INFINITY (which means, missing value)
                if(Utils.isDouble(thisLineComponents[varIndex]))
                    valuesInTsOrder.add(Double.valueOf(thisLineComponents[varIndex]));
                else
                    valuesInTsOrder.add(missingValuePlaceHolder);
                // .add(-1.0);
                //.add(Double.POSITIVE_INFINITY);
            } // for each line component

        } // for each line

        return new MTSE(dataset, recordID, timeStamps, varNames, new ArrayList<>(varVarValuesMap.values()));
    } // fromFile

    //helper method to create MTSE from file
    //it will parse the file in the following format:
    //    var1 var2
    //ts1
    //ts2
    public static MTSE fromFile(Dataset dataset, String localFilePath, String lineComponentDelimiter, double missingValuePlaceHolder)
    {
        //all files are actually record ids
        String fileName = Utils.fileNameFromPath(localFilePath);

        //obtain file contents
        String fileContents = Utils.fileContentsFromLocalFilePath(localFilePath);

        return fromFile(dataset, fileName, fileContents, lineComponentDelimiter, missingValuePlaceHolder);
    } // fromFile


    //TODO you can have result of this method as a seperate instance variable to speed up performance
    //get var values in ts order
    private List<List<Double>> listOfVarValuesInTsOrder()
    {
        List<List<Double>> varValuesInTsOrder = new ArrayList<>();
        for(String var : horizontalData.keySet())
        {
            TreeMap<Integer, Double> tsVarValues = horizontalData.get(var);
            List<Double> thisVarValuesInTsOrder = new ArrayList<>();
            for(Integer ts : tsVarValues.keySet())
                thisVarValuesInTsOrder.add(tsVarValues.get(ts));

            varValuesInTsOrder.add(thisVarValuesInTsOrder);
        } // for

        return varValuesInTsOrder;
    } // listOfVarValuesInTsOrder


    //helper method to generate vertical data of MTSE
    private TreeMap<Integer, TreeMap<String, Double>> vertData()
    {
        //initialize vertical data
        TreeMap<Integer, TreeMap<String, Double>> vertData = new TreeMap<>();
        for//(int tsIndex = 0; tsIndex < tsArray.length; tsIndex ++)
            (Integer ts : getTimeStamps())
        {
            TreeMap<String, Double> varValueMap = new TreeMap<>();

            for//(int varIndex = 0; varIndex < varNamesArray.length; varIndex ++)
                (String var : horizontalData.keySet())
            {
                varValueMap//.put(varNamesArray[varIndex], varValuesInTsOrder.get(varIndex).get(tsIndex));
                            .put(var, horizontalData.get(var).get(ts));
            } // for

            vertData//.put(tsArray[tsIndex], varValueMap);
                    .put(ts, varValueMap);
        } // for

        return vertData;
    } // vertData


    //helper method to copy this multivariate time series
    public MTSE deepCopy()
    {
        return new MTSE(dataset, getRecordID(), getTimeStamps(), getVars(), listOfVarValuesInTsOrder());
    } // deepCopy



    //helper method to find whether MTSE is embeddable wth a given embedding parameter m and delay parameter nu
    public boolean isEmbeddable(int embeddingParameterM, int delayParameterNu)
    {
        int length = getTimeStamps().size();
        //it is embeddable when length > (m - 1) * nu + 1
        return length >= (embeddingParameterM - 1) * delayParameterNu + 1;
    } // isEmbeddable



    public List<MTSE> embed(int embeddingParameterM, int delayParameterNu)
    {
        //TODO returning original mtse when it is not embeddable, you can also discard mtses which are not embeddable during learning
        if(!isEmbeddable(embeddingParameterM, delayParameterNu))
        {
            List<MTSE> orig = new ArrayList<>();
            orig.add(this);
            return orig;
            //throw new RuntimeException("Time series is not embeddable!");
        }


        List<MTSE> embeddings = new ArrayList<>();
        //Integer[] tsArray = getTimeStamps().toArray(new Integer[]{});
        List<Integer> timestamps = getTimeStamps();
        int T = timestamps.size();
        List<String> vars = getVars();


        //outer list is ordered by var name, inner list is ordered by timestamp
        List<List<Double>> listOfVarValuesInTsOrder = //listOfVarValuesInTimestampOrder;
                                                        listOfVarValuesInTsOrder();


        for (int tsIndex = 0; tsIndex < T; tsIndex++)
        {
            //every satisfying tsIndex will create new MTSE
            if (tsIndex >= (embeddingParameterM - 1) * delayParameterNu)
            {
                //timestamps of this embedding (or MTSE)
                List<Integer> tssForThisEmbedding = new ArrayList<>();

                //var values of this embedding (or MTSE)
                List<List<Double>> listOfVarValuesInTsOrderForThisEmbedding = new ArrayList<>();
                vars.forEach(s -> listOfVarValuesInTsOrderForThisEmbedding.add(null));  // initialize all with null



                //populate the values for embeddings
                for (int i = 1; i <= embeddingParameterM; i++)
                {
                    int indexToObtain = tsIndex - ((embeddingParameterM - i) * delayParameterNu);
                    tssForThisEmbedding.add(timestamps.get(indexToObtain));


                    for (int varIndex = 0; varIndex < listOfVarValuesInTsOrder.size(); varIndex++)
                    {
                        List<Double> valuesInTsOrderForThisVar = listOfVarValuesInTsOrder.get(varIndex);


                        //values of current var for this embedding
                        List<Double> thisVarValuesInTsOrderForThisEmbedding = listOfVarValuesInTsOrderForThisEmbedding.get(varIndex);
                        if (thisVarValuesInTsOrderForThisEmbedding == null)
                        {
                            thisVarValuesInTsOrderForThisEmbedding = new ArrayList<>();
                            listOfVarValuesInTsOrderForThisEmbedding.set(varIndex, thisVarValuesInTsOrderForThisEmbedding);
                        }


                        thisVarValuesInTsOrderForThisEmbedding.add(valuesInTsOrderForThisVar.get(indexToObtain));
                    } // for each var

                } // for each i until m (including)


                //create new MTSE
                //for(List<Double> list : listOfVarValuesInTsOrderForThisEmbedding)
                //    System.out.println(list);
                MTSE embedding = new MTSE(dataset, recordID, tssForThisEmbedding, vars, listOfVarValuesInTsOrderForThisEmbedding);
                embeddings.add(embedding);
            } // if tsIndex satisfies

        } // for each ts

        return embeddings;
    } // embed


    //helper method to transform mtse to flat
    //it is usable when all mtses have the same length (Utils class imp. for all mtses)
    public MTSE transformToFlat()
    {
        List<String> vars = getVars();

        LinkedHashMap<Integer, List<List<Double>>> map = new LinkedHashMap<>();
        for(String var : horizontalData.keySet())
        {
            TreeMap<Integer, Double> varValuesInTsOrder = horizontalData.get(var);
            for(Integer ts : varValuesInTsOrder.keySet())
            {
                double valueOfThisVarInThisTs = varValuesInTsOrder.get(ts);
                if(map.containsKey(ts))
                {
                    List<List<Double>> listOfVarValuesInTsOrder = map.get(ts);
                    List<Double> list = new ArrayList<>();
                    list.add(valueOfThisVarInThisTs);
                    listOfVarValuesInTsOrder.add(list);
                } // if
                else
                {
                    List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
                    List<Double> list = new ArrayList<>();
                    list.add(valueOfThisVarInThisTs);
                    listOfVarValuesInTsOrder.add(list);
                    map.put(ts, listOfVarValuesInTsOrder);
                }
            } // for
        } // for

        int count = 1;
        MTSE initMTSE = null;
        for(Integer ts : map.keySet())
        {
            List<String> newVars = new ArrayList<>();
            for(String varName : vars)
                newVars.add(String.format("%02d", count) + "_" + varName);

            List<Integer> timeStamps = new ArrayList<>();
            timeStamps.add(ts);

            if(count == 1)
                initMTSE = new MTSE(dataset, recordID, timeStamps, newVars, map.get(ts));
            else
                initMTSE = horizontalJoin(initMTSE, new MTSE(dataset, recordID, timeStamps, newVars, map.get(ts)));
            count ++;
        } // for

        return initMTSE;
    } // transformToFlat



    //helper method to add mean transformation to mtse
    public MTSE transformToMean(boolean weighted)
    {
        if(weighted) return transformToWeightedMean();


        int sumTs = 0;

        List<Integer> origTimeStamps = getTimeStamps();
        //it will also transform the time stamp minutes to mean
        for (Integer ts : origTimeStamps)
        {
            sumTs += ts;
        } // for
        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add(sumTs / origTimeStamps.size()); // integer division


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to average var values
        for(String var : horizontalData.keySet())
        {
            List<Double> meanValueForThisVar = new ArrayList<>();

            double sumForThisVar = 0;
            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            for(Integer ts : valuesInTsOrder.keySet())
            {
                sumForThisVar += valuesInTsOrder.get(ts);
            } // for

            //values are added in var name order
            meanValueForThisVar.add(sumForThisVar / valuesInTsOrder.keySet().size());
            listOfVarValuesInTsOrder.add(meanValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_avg");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToMean


    //method to transform MTSE into weighted mean
    private MTSE transformToWeightedMean()
    {
        int sumTs = 0;

        List<Double> tss = new ArrayList<>();
        List<Integer> origTimeStamps = getTimeStamps();
        //it will also transform the time stamp minutes to mean
        for (Integer ts : origTimeStamps)
        {
            sumTs += ts;
            tss.add(ts * 1.0);
        } // for


        //map to hold ts and their weights
        TreeMap<Integer, Double> tsWeightMap = new TreeMap<>();
        List<Double> weights = new ArrayList<>();
        for(Integer ts : origTimeStamps)
        {
            double weight = ts / (1.0 * sumTs);
            tsWeightMap.put(ts, weight);
            weights.add(weight);
        } // for
        double sumOfWeights = 0.0;
        for(Integer ts : tsWeightMap.keySet())
            sumOfWeights += tsWeightMap.get(ts);


        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add((int) Utils.weightedMean(tss, weights));



        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to average var values
        for(String var : horizontalData.keySet())
        {
            List<Double> weightedMeanValueForThisVar = new ArrayList<>();

            double weightedSumForThisVar = 0;
            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            for(Integer ts : valuesInTsOrder.keySet())
            {
                weightedSumForThisVar += tsWeightMap.get(ts) * valuesInTsOrder.get(ts);
            } // for

            //values are added in var name order
            weightedMeanValueForThisVar.add(weightedSumForThisVar / sumOfWeights);
            listOfVarValuesInTsOrder.add(weightedMeanValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_w_avg");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToWeightedMean


    //TODO lower performance than max
    //helper method to transform mtse into min feature space
    public MTSE transformToMin()
    {
        int minTs = Integer.MAX_VALUE;

        List<Integer> origTimeStamps = getTimeStamps();
        //it will also transform the time stamp minutes to mean
        for (Integer ts : origTimeStamps)
        {
            if(ts < minTs)
                minTs = ts;
        } // for
        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add(minTs); // integer division


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to average var values
        for(String var : horizontalData.keySet())
        {
            List<Double> minValueForThisVar = new ArrayList<>();

            double min = Double.POSITIVE_INFINITY;
            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            for(Integer ts : valuesInTsOrder.keySet())
            {
                double varValInThisTs = valuesInTsOrder.get(ts);
                if(varValInThisTs < min)
                    min = varValInThisTs;
            } // for

            //values are added in var name order
            minValueForThisVar.add(min);
            listOfVarValuesInTsOrder.add(minValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_min");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToMin


    //helper method to transform mtse into max feature space
    public MTSE transformToMax()
    {
        int maxTs = getTimeStamps().stream().mapToInt(ts -> ts).max().orElse(Integer.MIN_VALUE);

        //it will also transform the time stamp minutes to mean
        // for
        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add(maxTs); // integer division


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to average var values
        for(String var : horizontalData.keySet())
        {
            List<Double> maxValueForThisVar = new ArrayList<>();

            double max = Double.NEGATIVE_INFINITY;
            TreeMap<Integer, Double> valuesInTsOrder;
            valuesInTsOrder = horizontalData.get(var);
            for(Integer ts : valuesInTsOrder.keySet())
            {
                double varValInThisTs = valuesInTsOrder.get(ts);
                if(varValInThisTs > max)
                    max = varValInThisTs;
            } // for

            //values are added in var name order
            maxValueForThisVar.add(max);
            listOfVarValuesInTsOrder.add(maxValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_max");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToMax


    //inefficient
    //public static MTSE horizontalJoin(MTSE... mtses)
    //{
    //    if(mtses.length == 0)
    //        throw new RuntimeException("At least one mtse should be provided");
    //
    //    MTSE finalMTSE = mtses[0];
    //    for(int index = 1; index < mtses.length; index ++)
    //    {
    //        finalMTSE = horizontalJoin(finalMTSE, mtses[index]);
    //    } // for
    //
    //    return finalMTSE;
    //} // horizontalJoin


    //helper method to join two mtses horizontally
    //mtse1var1, mtse1var2, ...., mtse2var1, mtse2var2, ....
    public static MTSE horizontalJoin(MTSE mtse1, MTSE mtse2)
    {
        //some prechecks
        //datasets should the same
        if(mtse1.dataset != mtse2.dataset)
            throw new RuntimeException("datasets should be the same");
        //record ids should be the same
        if(mtse1.getRecordID() != mtse2.getRecordID())
            throw new RuntimeException("Only mtses belonging to the same recordId or patient can be joined");
        //number of timestamps should be the same
        List<Integer> mtse1TimeStamps = mtse1.getTimeStamps(); // obtains a different object in each call
        List<Integer> mtse2TimeStamps = mtse2.getTimeStamps(); // obtains a different object in each call
        if(mtse1TimeStamps.size() != mtse2TimeStamps.size())
            throw new RuntimeException("the number of time stamps should be the same");
        //if(mtse1.getVars().size() != mtse2.getVars().size())
        //    throw new RuntimeException("the number of variables should be the same"); // #variables should not be the same
        //variables names should be different
        List<String> mtse1Vars = mtse1.getVars(); // obtains a different object in each call
        List<String> mtse2Vars = mtse2.getVars(); // obtains a different object in each call
        HashSet<String> vars = new HashSet<>(mtse1Vars);
        for(String var : mtse2Vars)
            if(vars.contains(var)) throw new RuntimeException("Variables names should be different");


        //average the timestamps
        List<Integer> joinedTimeStamps = new ArrayList<>();
        for(int index = 0; index < mtse1TimeStamps.size(); index ++)
        {
            joinedTimeStamps.add((mtse1TimeStamps.get(index) + mtse2TimeStamps.get(index)) / 2); //integer division
        } // for

        //check whether the newly created timestamps are unique, they must be unique
        if(joinedTimeStamps.size() != new HashSet<>(joinedTimeStamps).size())
            throw new RuntimeException("created timestamps should be unique!");


        //combine variables => #variables = #variables_mtse1 + #variables_mtse2
        mtse1Vars.addAll(mtse2Vars);
        //System.out.println(Arrays.toString(joinedVars.toArray(new String[]{})));

        List<List<Double>> joinedVarValuesInTsOrder =  //new ArrayList<>(mtse1.listOfVarValuesInTimestampOrder);
                                                        mtse1.listOfVarValuesInTsOrder(); // each call will generate a different object on listOfVarValuesInTsOrder()
        joinedVarValuesInTsOrder.addAll(//mtse2.listOfVarValuesInTimestampOrder
                                        mtse2.listOfVarValuesInTsOrder()
                                        );

        //return combined mtse
        return new MTSE(mtse1.dataset, mtse1.getRecordID(), joinedTimeStamps, mtse1Vars, joinedVarValuesInTsOrder);
    } // horizontalJoin


    //helper method to join two mtses vertically
    public static MTSE verticalJoin(MTSE mtse1, MTSE mtse2)
    {
        //some prechecks
        //datasets should the same
        if(mtse1.dataset != mtse2.dataset)
            throw new RuntimeException("datasets should be the same");
        //record ids should be the same
        if(mtse1.getRecordID() != mtse2.getRecordID())
            throw new RuntimeException("Only mtses belonging to the same recordId or patient can be joined");
        //number of variables should be the same
        //if(mtse1.getTimeStamps().size() != mtse2.getTimeStamps().size())
        //    throw new RuntimeException("the number of time stamps should be the same");
        List<String> mtse1Vars = mtse1.getVars(); // obtains a different object in each call
        List<String> mtse2Vars = mtse2.getVars(); // obtains a different object in each call
        if(mtse1Vars.size() != mtse2Vars.size())
            throw new RuntimeException("the number of variables should be the same"); // #number of variables should not be the same
        //variables names should be the same
        HashSet<String> mtse1VarsSet = new HashSet<>(mtse1Vars);
        HashSet<String> mtse2VarsSet = new HashSet<>(mtse2Vars);
        if(!mtse1VarsSet.equals(mtse2VarsSet)) throw new RuntimeException("Variables names should be the same on vertical join");
        //timestamps should be different (they are used as keys in maps)

        //HashSet<Integer> tss = new HashSet<>(mtse1.getTimeStamps());
        //for(Integer ts : mtse2.getTimeStamps())
        //    if(tss.contains(ts)) throw new RuntimeException("Timestamps should be different"); // uniqueness checked below


        //join the timestamps
        List<Integer> joinedTimeStamps = mtse1.getTimeStamps(); // each call will generate a different object on getTimeStamps()
        //to keep the order of timestamps, last timestamp of mtse1 is added to each time stamp of mtse2
        int lastTimeStamp = joinedTimeStamps.get(joinedTimeStamps.size() - 1);
        if(lastTimeStamp == 0) lastTimeStamp = 1;
        for(int timeStamp : mtse2.getTimeStamps())
            joinedTimeStamps.add(timeStamp + lastTimeStamp);


        //check whether the newly created timestamps are unique, they must be unique
        if(joinedTimeStamps.size() != new HashSet<>(joinedTimeStamps).size())
            throw new RuntimeException("created timestamps should be unique!");



        List<List<Double>> joinedVarValuesInTsOrder = mtse1//.listOfVarValuesInTimestampOrder;
                                                        .listOfVarValuesInTsOrder(); // each call will generate a different object on listOfVarValuesInTsOrder()
        List<List<Double>> mtse2VarValuesInTsOrder = mtse2//.listOfVarValuesInTimestampOrder;
                                                        .listOfVarValuesInTsOrder();
        for(int varIndex = 0; varIndex < joinedVarValuesInTsOrder.size(); varIndex ++)
        {
            List<Double> valuesInTsOrder = joinedVarValuesInTsOrder.get(varIndex);
            //add the values of mtse2 for this var in ts order to valuesInTsOrder
            valuesInTsOrder.addAll(mtse2VarValuesInTsOrder.get(varIndex)); //updates the joinedVarValuesInTsOrder internally
        } // for

        //return combined mtse
        return new MTSE(mtse1.dataset, mtse1.getRecordID(), joinedTimeStamps, mtse1Vars, joinedVarValuesInTsOrder);
    } // verticalJoin


    //TODO better performance than min and max individually considered
    //helper method to transform the dataset into minimax feature space
    public MTSE transformToMinimax()
    {
        return MTSE.horizontalJoin(transformToMin(), transformToMax());
    } // transformToMinimax


    //TODO lower performance on mean imputation (forward imputation over 0.82 in RB-SMI-RF), therefore we will not consider weighted median
    //helper method to do median transformation
    public MTSE transformToMedian()
    {
        //it will also transform the time stamp minutes to median
        List<Double> tss = new ArrayList<>();
        for (Integer ts : getTimeStamps())
        {
            tss.add(ts * 1.0);
        } // for
        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add((int) Utils.median(tss));


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to average var values
        for(String var : horizontalData.keySet())
        {
            List<Double> medianValueForThisVar = new ArrayList<>();

            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            List<Double> valueList = new ArrayList<>();
            for(Integer ts : valuesInTsOrder.keySet())
            {
                valueList.add(valuesInTsOrder.get(ts));
            } // for

            //values are added in var name order
            medianValueForThisVar.add(Utils.median(valueList));
            listOfVarValuesInTsOrder.add(medianValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_med");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToMedian


    //helper method to perform variance transformation
    public MTSE transformToVariance(boolean weighted)
    {
        int sumTs = 0;

        //it will also transform the time stamp minutes to variance
        List<Double> tss = new ArrayList<>();
        List<Integer> origTimeStamps = getTimeStamps();
        for (Integer ts : origTimeStamps)
        {
            tss.add(ts * 1.0);
            sumTs += ts;
        } // for


        //weights
        List<Double> weights = new ArrayList<>();
        for(Integer ts : origTimeStamps)
        {
            weights.add(ts / (1.0 * sumTs));
        } // for


        List<Integer> timeStamps = new ArrayList<>();
        if(weighted)
            timeStamps.add((int) Utils.weightedPopulationVariance(tss, weights));
        else
            timeStamps.add((int) Utils.populationVariance(tss));



        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to average var values
        for(String var : horizontalData.keySet())
        {
            List<Double> varianceValueForThisVar;
            varianceValueForThisVar = new ArrayList<>();

            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            List<Double> valueList = new ArrayList<>();
            for(Integer ts : valuesInTsOrder.keySet())
            {
                valueList.add(valuesInTsOrder.get(ts));
            } // for

            //values are added in var name order
            if(weighted)
                varianceValueForThisVar.add(Utils.weightedPopulationVariance(valueList, weights));
            else
                varianceValueForThisVar.add(Utils.populationVariance(valueList));

            listOfVarValuesInTsOrder.add(varianceValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        if(weighted)
        {
            for (String var : getVars())
                newVarNames.add(var + "_w_var");
        }
        else
        {
            for (String var : getVars())
                newVarNames.add(var + "_var");
        } // else

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToVariance


    //helper method to transform to std
    public MTSE transformToStd(boolean weighted)
    {
        int sumTs = 0;

        List<Integer> origTimeStamps = getTimeStamps();
        //it will also transform the time stamp minutes to variance
        List<Double> tss = new ArrayList<>();
        for (Integer ts : origTimeStamps)
        {
            tss.add(ts * 1.0);
            sumTs += ts;
        } // for


        //weights
        List<Double> weights = new ArrayList<>();
        for(Integer ts : origTimeStamps)
        {
            weights.add(ts / (1.0 * sumTs));
        } // for


        List<Integer> timeStamps = new ArrayList<>();
        if(weighted)
            timeStamps.add((int) Utils.weightedPopulationStd(tss, weights));
        else
            timeStamps.add((int) Utils.populationStd(tss));



        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to average var values
        for(String var : horizontalData.keySet())
        {
            List<Double> stdValueForThisVar = new ArrayList<>();

            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            List<Double> valueList = new ArrayList<>();
            for(Integer ts : valuesInTsOrder.keySet())
            {
                valueList.add(valuesInTsOrder.get(ts));
            } // for

            //values are added in var name order
            if(weighted)
                stdValueForThisVar.add(Utils.weightedPopulationStd(valueList, weights));
            else
                stdValueForThisVar.add(Utils.populationStd(valueList));

            listOfVarValuesInTsOrder.add(stdValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        if(weighted)
        {
            for (String var : getVars())
                newVarNames.add(var + "_w_std");
        }
        else
        {
            for (String var : getVars())
                newVarNames.add(var + "_std");
        } // else

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToStd


    //TODO lower performance around (0.54) on mean imputation, forward -> 0.7965
    //method to transform mtse to mode
    public MTSE transformToMode()
    {
        //it will also transform the time stamp minutes to mode
        List<Double> tss = new ArrayList<>();
        for (Integer ts : getTimeStamps())
        {
            tss.add(ts * 1.0);
        } // for
        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add((int) Utils.modeOrMean(tss));


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to average var values
        for(String var : horizontalData.keySet())
        {
            ArrayList<Double> modeValueForThisVar = new ArrayList<>();

            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            List<Double> valueList = new ArrayList<>();
            for(Integer ts : valuesInTsOrder.keySet())
            {
                valueList.add(valuesInTsOrder.get(ts));
            } // for

            //values are added in var name order
            modeValueForThisVar.add(Utils.modeOrMean(valueList));
            listOfVarValuesInTsOrder.add(modeValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_mode");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToMode


    //AUROC performance is around 0.795
    //helper method to transform to range
    public MTSE transformToRange()
    {
        //it will also transform the time stamp minutes to range
        List<Double> tss = new ArrayList<>();
        for (Integer ts : getTimeStamps())
        {
            tss.add(ts * 1.0);
        } // for
        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add((int) Utils.range(tss));


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to transform var values
        for(String var : horizontalData.keySet())
        {
            ArrayList<Double> rangeValueForThisVar = new ArrayList<>();

            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            List<Double> valueList = new ArrayList<>();
            for(Integer ts : valuesInTsOrder.keySet())
            {
                valueList.add(valuesInTsOrder.get(ts));
            } // for

            //values are added in var name order
            rangeValueForThisVar.add(Utils.range(valueList));
            listOfVarValuesInTsOrder.add(rangeValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_range");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToRange


    //helper method to transform to geometric center
    public MTSE transformToGeometricCenter()
    {
        //it will also transform the time stamp minutes to range
        List<Double> tss = new ArrayList<>();
        for (Integer ts : getTimeStamps())
        {
            tss.add(ts * 1.0);
        } // for
        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add((int) Utils.geometricCenter(tss));


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to transform var values
        for(String var : horizontalData.keySet())
        {
            ArrayList<Double> geoCenValueForThisVar = new ArrayList<>();

            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            List<Double> valueList = new ArrayList<>();
            for(Integer ts : valuesInTsOrder.keySet())
            {
                valueList.add(valuesInTsOrder.get(ts));
            } // for

            //values are added in var name order
            geoCenValueForThisVar.add(Utils.geometricCenter(valueList));
            listOfVarValuesInTsOrder.add(geoCenValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_geo_cen");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToGeometricCenter


    //helper method to transform into skewness
    public MTSE transformToSkewness()
    {
        //it will also transform the time stamp minutes to range
        List<Double> tss = new ArrayList<>();
        for (Integer ts : getTimeStamps())
        {
            tss.add(ts * 1.0);
        } // for
        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add((int) Utils.mean(tss, false)); // TODO mean() instead of skewness(), skewness generates non-unique timestamps


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to transform var values
        for(String var : horizontalData.keySet())
        {
            ArrayList<Double> skewnessValueForThisVar = new ArrayList<>();

            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            List<Double> valueList = new ArrayList<>();
            for(Integer ts : valuesInTsOrder.keySet())
            {
                valueList.add(valuesInTsOrder.get(ts));
            } // for

            //values are added in var name order
            skewnessValueForThisVar.add(Utils.skewness(valueList));
            listOfVarValuesInTsOrder.add(skewnessValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_skew");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToSkewness


    //TODO lower performance (0.75 mean imputation, 0.68 forward imputation)
    //helper method to transform to kurtosis
    public MTSE transformToKurtosis()
    {
        //it will also transform the time stamp minutes to kurtosis
        List<Double> tss = new ArrayList<>();
        for (Integer ts : getTimeStamps())
        {
            tss.add(ts * 1.0);
        } // for
        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add((int) Utils.kurtosis(tss));


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to transform var values
        for(String var : horizontalData.keySet())
        {
            ArrayList<Double> kurtosisValueForThisVar = new ArrayList<>();

            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            List<Double> valueList = new ArrayList<>();
            for(Integer ts : valuesInTsOrder.keySet())
            {
                valueList.add(valuesInTsOrder.get(ts));
            } // for

            //values are added in var name order
            kurtosisValueForThisVar.add(Utils.kurtosis(valueList));
            listOfVarValuesInTsOrder.add(kurtosisValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_kurto");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToKurtosis


    //method to transform to geometric mean and weighted geometric mean
    public MTSE transformToGeometricMean(boolean weighted)
    {
        int sumTs = 0;

        List<Integer> origTimeStamps = getTimeStamps();
        //it will also transform the time stamp minutes to variance
        List<Double> tss = new ArrayList<>();
        for (Integer ts : origTimeStamps)
        {
            tss.add(ts * 1.0);
            sumTs += ts;
        } // for


        //weights
        List<Double> weights = new ArrayList<>();
        for(Integer ts : origTimeStamps)
        {
            weights.add(ts / (1.0 * sumTs));
        } // for


        List<Integer> timeStamps = new ArrayList<>();
        if(weighted)
            timeStamps.add((int) Utils.weightedGeometricMean(tss, weights));
        else
            timeStamps.add((int) Utils.geometricMean(tss));


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to average var values
        for(String var : horizontalData.keySet())
        {
            List<Double> geoMeanValueForThisVar;
            geoMeanValueForThisVar = new ArrayList<>();

            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            List<Double> valueList = new ArrayList<>();
            for(Integer ts : valuesInTsOrder.keySet())
            {
                valueList.add(valuesInTsOrder.get(ts));
            } // for

            //values are added in var name order
            if(weighted)
                geoMeanValueForThisVar.add(Utils.weightedGeometricMean(valueList, weights));
            else
                geoMeanValueForThisVar.add(Utils.geometricMean(valueList));

            listOfVarValuesInTsOrder.add(geoMeanValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        if(weighted)
        {
            for (String var : getVars())
                newVarNames.add(var + "_w_geo_mean");
        }
        else
        {
            for (String var : getVars())
                newVarNames.add(var + "_geo_mean");
        } // else

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToGeometricMean


    //method to transform to average power
    public MTSE transformToAveragedPower()
    {
        //it will also transform the time stamp minutes to kurtosis
        List<Double> tss = new ArrayList<>();
        for (Integer ts : getTimeStamps())
        {
            tss.add(ts * 1.0);
        } // for
        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add((int) Utils.averagedPower(tss));


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to transform var values
        for(String var : horizontalData.keySet())
        {
            ArrayList<Double> avgPowerValueForThisVar = new ArrayList<>();

            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            List<Double> valueList = new ArrayList<>();
            for(Integer ts : valuesInTsOrder.keySet())
            {
                valueList.add(valuesInTsOrder.get(ts));
            } // for

            //values are added in var name order
            avgPowerValueForThisVar.add(Utils.averagedPower(valueList));
            listOfVarValuesInTsOrder.add(avgPowerValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_avg_power");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToAveragedPower


    //method to transform to root mean square (rms)
    public MTSE transformToRMS()
    {
        //it will also transform the time stamp minutes to kurtosis
        List<Double> tss = new ArrayList<>();
        for (Integer ts : getTimeStamps())
        {
            tss.add(ts * 1.0);
        } // for
        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add((int) Utils.rms(tss));


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to transform var values
        for(String var : horizontalData.keySet())
        {
            ArrayList<Double> rmsValueForThisVar = new ArrayList<>();

            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            List<Double> valueList = new ArrayList<>();
            for(Integer ts : valuesInTsOrder.keySet())
            {
                valueList.add(valuesInTsOrder.get(ts));
            } // for

            //values are added in var name order
            rmsValueForThisVar.add(Utils.rms(valueList));
            listOfVarValuesInTsOrder.add(rmsValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_rms");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToRMS


    //method to transform to energy spectral density
    public MTSE transformToESD()
    {
        //it will also transform the time stamp minutes to kurtosis
        List<Double> tss = new ArrayList<>();
        for (Integer ts : getTimeStamps())
        {
            tss.add(ts * 1.0);
        } // for
        List<Integer> timeStamps = new ArrayList<>();
        timeStamps.add((int) Utils.energySpectralDensity(tss));


        List<List<Double>> listOfVarValuesInTsOrder = new ArrayList<>();
        //now go through horizontal data to transform var values
        for(String var : horizontalData.keySet())
        {
            ArrayList<Double> esdValueForThisVar = new ArrayList<>();

            TreeMap<Integer, Double> valuesInTsOrder = horizontalData.get(var);
            List<Double> valueList = new ArrayList<>();
            for(Integer ts : valuesInTsOrder.keySet())
            {
                valueList.add(valuesInTsOrder.get(ts));
            } // for

            //values are added in var name order
            esdValueForThisVar.add(Utils.energySpectralDensity(valueList));
            listOfVarValuesInTsOrder.add(esdValueForThisVar);
        } // for

        //update var names in such a way that original order does not change
        List<String> newVarNames = new ArrayList<>();
        for(String var : getVars())
            newVarNames.add(var + "_esd");

        return new MTSE(dataset, getRecordID(), timeStamps, newVarNames, listOfVarValuesInTsOrder);
    } // transformToESD


    //method to transform to super feature space
    public MTSE transformToSuper13()
    {
        return transformTo(TransformMethod.valuesForSuper13());

        //inefficient
        //return MTSE.horizontalJoin(transformToMin(), transformToMax(), transformToMean(true), transformToMedian(),
        //        transformToMode(), transformToStd(true), transformToVariance(true), transformToRange(), transformToGeometricCenter(),
        //        transformToGeometricMean(true),
        //        //transformToKurtosis(), //sometimes results in NaN values because of a division by std
        //        //transformToSkewness(), //sometimes results in NaN values because of a division by std
        //        transformToAveragedPower(), transformToRMS(), transformToESD());
    } // transformToSuper13


    //to transform to super 13, but picking only unweighted transformations
    public MTSE transformToSuper13Unweighted()
    {
        return transformTo(TransformMethod.valuesForSuper13Unweighted());
    }


    //to transform to super 17
    public MTSE transformToSuper17()
    {
        return transformTo(TransformMethod.valuesForSuper17());
    }


    //helper method to transform to best peforming params
    public MTSE transformToBest()
    {
        return transformTo(TransformMethod.valuesForBest());
    } // transformToBest


    //helper method to transform from the given transformation enum params
    public MTSE transformTo(TransformMethod... transformMethods)
    {
        if(transformMethods.length == 0)
            return this;

        //if (transformMethods.length == 1)
        //    return transformMethods[0].transform(this);

        MTSE finalMTSE = transformMethods[0].transform(this);
        for(int index = 1; index < transformMethods.length; index ++)
        {
            finalMTSE = horizontalJoin(finalMTSE, transformMethods[index].transform(this));
        } // for

        return finalMTSE;
    } // transformTo


    //helper method to transform to embedding space without transformation
    //each embedding will represent one bag in MIL setting
    public List<MTSE> transformToEmbeddingSpace(int embeddingParameterM, int delayParameterNu)
    {
        List<MTSE> embeddings = embed(embeddingParameterM, delayParameterNu);
        //for each embedding update its record id according to its index
        for(int index = 0; index < embeddings.size(); index++)
        {
            MTSE thisEmbedding = embeddings.get(index);
            //Outcomes outcomes = Outcomes.getInstance(thisEmbedding.dataset);
            //Outcome outcome = outcomes.get(thisEmbedding.getRecordID());
            thisEmbedding.setRecordID(Integer.parseInt(thisEmbedding.getRecordID() + "" + (index + 1)));
            //obtained record id should be added to outcomes
            //Outcome newOutcome = new Outcome(thisEmbedding.getRecordID(), outcome.getSapsIScore(), outcome.getSofaScore(),
            //                    outcome.getLengthOfStayInDays(), outcome.getSurvivalInDays(), outcome.getInHospitalDeath0Or1());
            //outcomes.add(thisEmbedding.getRecordID(), newOutcome);
        } // for

        return embeddings;
    } // transformToEmbeddingSpace


    //helper method to transform MTSE into embeding space by generating embeddings, transforming them
    //to the provided methods and final by vertical joining the embeddings to obtain one MTSE
    public MTSE transformToEmbeddingSpace(int embeddingParameterM, int delayParameterNu, TransformMethod... transformMethods)
    {
        List<MTSE> embeddings = embed(embeddingParameterM, delayParameterNu);

        if(embeddings.isEmpty()) throw new RuntimeException("Embeddings are empty!");

        MTSE finalMTSE = embeddings.get(0).transformTo(transformMethods);
        for(int index = 1; index < embeddings.size(); index ++)
        {
            finalMTSE = verticalJoin(finalMTSE, embeddings.get(index).transformTo(transformMethods));
        } // for

        return finalMTSE;
    } // transformToEmbeddingSpace



    //TODO continue implementation of transformation methods (joining MTSEs together to gain combined feature space)
    // - geometric median (weighted)
    // - radon point (weighted)
    //Feature spaces implemented: 'Minimum', 'Maximum', 'Minimax', '(w)Mean', 'Median', 'Mode', '(w)Standard_deviation', '(w)Variance',
    //'Range', 'Geometric_center', '(w)Geometric_mean', 'Kurtosis', 'Skewness', 'Averaged_power', 'Root_mean_square', 'Energy_spectral_density', 'Super'
    // use weka.attributeSelection package, especially classes (e.g. PrincipalComponents) implementing AttributeTransformer interface

} // class MTSE
