package com.rb.me;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.mi.SimpleMI;
import weka.core.*;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.pyscript.PyScriptFilter;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.unsupervised.attribute.MultiInstanceToPropositional;
import weka.filters.unsupervised.attribute.PropositionalToMultiInstance;
import weka.filters.unsupervised.attribute.Standardize;
import weka.gui.visualize.Plot2D;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;

import static com.rb.me.Constants.*;
import static org.junit.Assert.*;

//class containing utility methods
public class Utils
{
    public static int defaultPaddingLength(Collection<String> stringCollection)
    {
        int defaultPaddingLength = 0;
        for(String var : stringCollection)
        {
            if(var.length() > defaultPaddingLength)
                defaultPaddingLength = var.length();
        } // for

        return defaultPaddingLength + 1; // + 1 to make a space for the longest named variable
    } // defaultPaddingLength


    public static int defaultPaddingLength(String... array)
    {
        int defaultPaddingLength = 0;
        for(String var : array)
        {
            if(var.length() > defaultPaddingLength)
                defaultPaddingLength = var.length();
        } // for

        return defaultPaddingLength + 1; // + 1 to make a space for the longest named variable
    } // defaultPaddingLength


    public static String padLeftSpaces(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append(' ');
        }
        sb.append(inputString);

        return sb.toString();
    }


    //helper method to get the file name (base name + extension) from the given path
    public static String fileNameFromPath(String path) {
        //Apache IO fileNameUtils works on every path
        //Tested on:
        //-----------------------------------------------
        //"hdfs://123.23.12.4344:9000/user/filename.txt"
        //"resources/cabs.xml"
        //"bluebird:/usr/doctools/dp/file.txt"
        //"\\\\server\\share\\file.xml"
        //"file:///C:/Users/Default/Downloads/file.pdf"
        //-----------------------------------------------
        //returns empty string for not found file names e.g. for => "\\\\server\\share\\"

        //return name of the directory if the path is a dir path
        return FilenameUtils.getName(path);
    } // fileNameFromPath


    //method to convert hours:minutes to minutes
    public static int toMinutes(String timeStampString)
    {
        String[] hoursAndMinutes = timeStampString.split(":");
        int hoursByMinutes = Integer.parseInt(hoursAndMinutes[0]) * 60;
        int minutes = Integer.parseInt(hoursAndMinutes[1]);
        return hoursByMinutes + minutes;
    }



    //helper method to list files from the local path in the local file system
    public static List<String> listFilesFromLocalPath(String localPathString, boolean recursive)
    {
        //resulting list of files
        List<String> localFilePaths = new ArrayList<String>();

        //get the Java file instance from local path string
        File localPath = new File(localPathString);


        //this case is possible if the given localPathString does not exit => which means neither file nor a directory
        if (!localPath.exists()) {
            System.err.println("\n" + localPathString + " is neither a file nor a directory; please provide correct local path");

            //return with empty list
            return new ArrayList<String>();
        } // if


        //at this point localPath does exist in the file system => either as a directory or a file


        //if recursive approach is requested
        if (recursive) {
            //recursive approach => using a queue
            Queue<File> fileQueue = new LinkedList<File>();

            //add the file in obtained path to the queue
            fileQueue.add(localPath);

            //while the fileQueue is not empty
            while (!fileQueue.isEmpty()) {
                //get the file from queue
                File file = fileQueue.remove();

                //file instance refers to a file
                if (file.isFile()) {
                    //update the list with file absolute path
                    localFilePaths.add(file.getAbsolutePath());
                } // if
                else   //else file instance refers to a directory
                {
                    //list files in the directory and add to the queue
                    File[] listedFiles = file.listFiles();
                    for (File listedFile : listedFiles) {
                        fileQueue.add(listedFile);
                    } // for
                } // else

            } // while
        } // if
        else        //non-recursive approach
        {
            //if the given localPathString is actually a directory
            if (localPath.isDirectory()) {
                File[] listedFiles = localPath.listFiles();

                //loop all listed files
                for (File listedFile : listedFiles) {
                    //if the given listedFile is actually a file, then update the resulting list
                    if (listedFile.isFile())
                        localFilePaths.add(listedFile.getAbsolutePath());
                } // for
            } // if
            else        //it is a file then
            {
                //return the one and only file absolute path to the resulting list
                localFilePaths.add(localPath.getAbsolutePath());
            } // else
        } // else


        //return the resulting list; list can be empty if given path is an empty directory without files and sub-directories
        return localFilePaths;
    } // listFilesFromLocalPath


    public static String fileContentsFromLocalFilePath(String localFilePath) {
        InputStream localFileInputStream = inputStreamFromLocalFilePath(localFilePath);
        return stringContentFromInputStream(localFileInputStream);
    } // fileContentsFromLocalFilePath


    //helper method convert the file's or whatever input stream to string content;
    //some code snippets can cause IOException here, therefore we throw exception;
    //it will be able to get string content both from a local file or network file
    private static String stringContentFromInputStream(InputStream inputStream)
    {
        //string builder to contain all lines of the local file or network file
        StringBuilder stringContentBuilder = new StringBuilder("");

        //a buffered reader to read input stream
        BufferedReader br = null;

        try {
            //instead of reading whole file input stream at once, we read the file input stream character by character
            //create a buffered reader from input stream via input stream reader
            br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            //variable to hold the int representation of character read
            int charRep = -1;

            //READING LINE BY LINE CAUSES EXCEPTIONS ESPECIALLY FROM INPUT STREAMS; THEREFORE WE READ CHAR BY CHAR
            //read the input stream character by character
            //The character read, as an integer in the range 0 to 65535 (0x00-0xffff), or -1 if the end of the stream has been reached
            while ((charRep = br.read()) != -1) {
                //char 2 bytes;	range => 0 to 65,536 (unsigned)
                stringContentBuilder.append((char) charRep);
            } // while

        } // try
        catch (Exception ex) // will catch all exceptions including "IOException"
        {
            ex.printStackTrace();

            //if any problem occurs during read operation, return an empty string
            return "";
        } // catch
        finally {
            //close buffered reader
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } // catch
            } // if
        } // finally


        //return the resulting string from string builder
        return stringContentBuilder.toString();
    } // stringContentFromInputStream


    //an overloaded version of the method to get the input stream from the local file path
    public static InputStream inputStreamFromLocalFilePath(String localFilePath)
    {
        //initialize input stream with empty string bytes;
        //when converted it will be empty string and in turn empty visits array list
        InputStream targetStream = new ByteArrayInputStream("".getBytes());

        try {
            //we assume that file exists
            File localFile = new File(localFilePath);

            //if local file does not exist (as a directory or a file); then return input stream with empty string bytes;
            //else if local file does exist, but does not refer to file; then again return input stream with empty string bytes;
            if (!localFile.exists() || (localFile.exists() && !localFile.isFile()))
                return targetStream;


            //at this point localFile exists, and it is actually a file
            targetStream = new FileInputStream(localFile);
            //return the resulting input stream
            return targetStream;
        } // try
        catch (Exception ex) {
            ex.printStackTrace();

            //if any problem occurs during input stream generation, return input stream with empty string bytes;
            //which in conversion will be empty string and in turn empty visits array list
            return new ByteArrayInputStream("".getBytes());
        } // catch
    } // inputStreamFromLocalFilePath


    //returns the path of the written file
    public static String writeToLocalFileSystem(String localFilePathString, String newDirName,
                                              String writableContent, String newFileExtension)
    {
        //new file path to return
        String newFilePath = null;

        //to be closed in finally
        PrintWriter printWriter = null;

        try {
            File localFile = new File(localFilePathString);

            //if the localFile is a directory, print message to standard error and return
            if (localFile.isDirectory()) {
                System.err.println("\n" + localFilePathString + " is a directory; not able to generate writable file path from it");
            } // if
            //there is a possibility given path does not exist which means does not refer to either file or directory
            //so perform checks for each of them; which is equivalent to FileSystem.resolvePath() for HDFS
            else if (localFile.isFile())
            {
                //get the parent directory of localFile
                File localFileParentDir = localFile.getParentFile();

                //make a new directory called <resultFileNameAppendix> in the parent directory; if that directory exists do not create it
                //getAbsolutePath() returns path string without trailing directory separator
                File resultingSubDir = new File(localFileParentDir.getAbsolutePath() + File.separator + newDirName);

                //mkdir a directory if the resulting sub dir path does not exist
                if (!resultingSubDir.exists())
                    resultingSubDir.mkdir();


                //change the file extension
                String localFileName = localFile.getName();
                String currentFileExtension = FilenameUtils.getExtension(localFileName);
                String newLocalFileName = localFileName.replace(currentFileExtension, newFileExtension);


                //now create a writable path from resultingSubDir and the given localFile
                //getAbsolutePath() returns path string without trailing directory separator
                String writableFilePathString = resultingSubDir.getAbsolutePath() + File.separator + newLocalFileName;

                //create a file from writable path
                File writableFile = new File(writableFilePathString);
                writableFile.createNewFile();

                //write with print writer
                printWriter = new PrintWriter(writableFile);
                printWriter.print(writableContent);

                System.out.println("Wrote file => " + writableFile.getAbsolutePath());

                //update newFilePath
                newFilePath = writableFile.getAbsolutePath();
            } // else if
            else
                System.err.println("\n" + localFilePathString + " is neither a file nor a directory; please provide correct file path");

        } // try
        catch (Exception ex) {
            ex.printStackTrace();
        } // catch
        finally {
            if (printWriter != null) printWriter.close();
        } // finally


        //return the file path
        return newFilePath;
    } // writeToLocalFileSystem



    //to obtain input stream from a string
    public static InputStream stringToInputStream(String str)
    {
        return new ByteArrayInputStream(str.getBytes());
    } // stringToInputStream


    //helper method to convert mtses to weka instances
    public static Instances mtsesToMIData(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, String bagName,
                                          String[] classLabels, boolean includeTsMinutes,
                                          String subsetOfTheDataset) throws Exception
    {
        if(mtses.isEmpty()) throw new RuntimeException("At least one mtse should be provided");
        List<String> vars = mtses.get(0).getVars();

        //obtain record ids from mtses in the given list order, where real bags will be constructed for these mtses
        List<String> recordIdsInMtses = new ArrayList<>();
        mtses.forEach(mtse -> recordIdsInMtses.add(mtse.getRecordID() + ""));

        //feedback
        System.out.println("Generating content for multi-instance format...");

        StringBuilder sb = new StringBuilder("@relation " + dataset.toSimpleString() + "_" + subsetOfTheDataset).append("\n\n");
        sb.append("@attribute ").append(bagName).append(" ")
                .append("{").append(String.join(",", recordIdsInMtses)).append("}").append("\n");
        sb.append("@attribute bag relational").append("\n");

        if(includeTsMinutes) sb.append(" @attribute ").append("tsMinutes").append(" numeric").append("\n");

        vars.forEach(s -> {
            sb.append(" @attribute ").append(s.trim()).append(" numeric").append("\n");
        });
        sb.append("@end bag").append("\n");
        sb.append("@attribute class {").append(String.join(",", classLabels)).append("}").append("\n\n");
        sb.append("@data").append("\n");
        for(MTSE mtse : mtses)
        {
            sb.append(mtse.getRecordID()).append(",").append("\"");
            Map<Integer, List<Double>> valuesInVarNameOrder = mtse.getValuesInVarNameOrder();
            List<String> joinedValuesInVarNameOrder = new ArrayList<>();
            for(Integer ts : valuesInVarNameOrder.keySet())
            {
                if(includeTsMinutes)
                    //it joins all elements in the row by comma
                    joinedValuesInVarNameOrder.add(ts + "," + String.join(",", toStringCollection(valuesInVarNameOrder.get(ts))));
                else
                    joinedValuesInVarNameOrder.add(String.join(",", toStringCollection(valuesInVarNameOrder.get(ts))));
            } // for
            //then joins rows by \n and appends a comma, class label and new line character
            sb.append(String.join("\\n", joinedValuesInVarNameOrder)).append("\"").append(",")
                    .append(outcomes.get(mtse.getRecordID()).getInHospitalDeath0Or1()).append("\n");
        } // for

        System.out.println("Relation " + dataset.toSimpleString() + "_" + subsetOfTheDataset + " is loaded as Weka Instances...");
        return new ConverterUtils.DataSource(stringToInputStream(sb.toString())).getDataSet();

    } // mtsesToMIData


    //helper method to convert mtses to weka instances
    public static void mtsesToMIArffData(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, String destination, String bagName,
                                          String[] classLabels, boolean includeTsMinutes,
                                          String subsetOfTheDataset) throws Exception
    {
        if(mtses.isEmpty()) throw new RuntimeException("At least one mtse should be provided");
        List<String> vars = mtses.get(0).getVars();

        //obtain record ids from mtses in the given list order, where real bags will be constructed for these mtses
        List<String> recordIdsInMtses = new ArrayList<>();
        mtses.forEach(mtse -> recordIdsInMtses.add(mtse.getRecordID() + ""));

        //feedback
        System.out.println("Generating content for multi-instance format...");

        StringBuilder sb = new StringBuilder("@relation " + dataset.toSimpleString() + "_" + subsetOfTheDataset).append("\n\n");
        sb.append("@attribute ").append(bagName).append(" ")
                .append("{").append(String.join(",", recordIdsInMtses)).append("}").append("\n");
        sb.append("@attribute bag relational").append("\n");

        if(includeTsMinutes) sb.append(" @attribute ").append("tsMinutes").append(" numeric").append("\n");

        vars.forEach(s -> {
            sb.append(" @attribute ").append(s.trim()).append(" numeric").append("\n");
        });
        sb.append("@end bag").append("\n");
        sb.append("@attribute class {").append(String.join(",", classLabels)).append("}").append("\n\n");
        sb.append("@data").append("\n");
        for(MTSE mtse : mtses)
        {
            sb.append(mtse.getRecordID()).append(",").append("\"");
            Map<Integer, List<Double>> valuesInVarNameOrder = mtse.getValuesInVarNameOrder();
            List<String> joinedValuesInVarNameOrder = new ArrayList<>();
            for(Integer ts : valuesInVarNameOrder.keySet())
            {
                if(includeTsMinutes)
                    //it joins all elements in the row by comma
                    joinedValuesInVarNameOrder.add(ts + "," + String.join(",", toStringCollection(valuesInVarNameOrder.get(ts))));
                else
                    joinedValuesInVarNameOrder.add(String.join(",", toStringCollection(valuesInVarNameOrder.get(ts))));
            } // for
            //then joins rows by \n and appends a comma, class label and new line character
            sb.append(String.join("\\n", joinedValuesInVarNameOrder)).append("\"").append(",")
                    .append(outcomes.get(mtse.getRecordID()).getInHospitalDeath0Or1()).append("\n");
        } // for

        try {
            File newFile = new File( destination + File.separator + subsetOfTheDataset + ".arff");
            newFile.createNewFile();
            PrintWriter pw = new PrintWriter(newFile);
            pw.print(sb.toString());
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Relation " + subsetOfTheDataset + " is written...");

    } // mtsesToMIArffData


    public static void mtsesToArffData(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, String destination,
                                       String[] classLabels, boolean includeTsMinutes,
                                       String subsetOfTheDataset)
    {
        if(mtses.isEmpty()) throw new RuntimeException("At least one mtse should be provided");
        List<String> vars = mtses.get(0).getVars();

        //feedback
        System.out.println("Generating content in arff format...");

        StringBuilder sb = new StringBuilder("@relation " + dataset.toSimpleString() + "_" + subsetOfTheDataset).append("\n\n");

        if(includeTsMinutes) sb.append("@attribute ").append("tsMinutes").append(" numeric").append("\n");

        vars.forEach(s -> {
            sb.append("@attribute ").append(s.trim()).append(" numeric").append("\n");
        });
        sb.append("@attribute class {").append(String.join(",", classLabels)).append("}").append("\n\n");
        sb.append("@data").append("\n");
        for(MTSE mtse : mtses)
        {
            Map<Integer, List<Double>> valuesInVarNameOrder = mtse.getValuesInVarNameOrder();
            List<String> joinedValuesInVarNameOrder = new ArrayList<>();
            for(Integer ts : valuesInVarNameOrder.keySet())
            {
                if(includeTsMinutes)
                    //it joins all elements in the row by comma
                    joinedValuesInVarNameOrder.add(ts + "," + String.join(",", toStringCollection(valuesInVarNameOrder.get(ts))));
                else
                    joinedValuesInVarNameOrder.add(String.join(",", toStringCollection(valuesInVarNameOrder.get(ts))));
            } // for
            //then joins rows by \n and appends a comma, class label and new line character

            joinedValuesInVarNameOrder.forEach(sb::append);
            sb.append(",").append(outcomes.get(mtse.getRecordID()).getInHospitalDeath0Or1()).append("\n");
        } // for


        try {
            File newFile = new File( destination + File.separator + subsetOfTheDataset + ".arff");
            newFile.createNewFile();
            PrintWriter pw = new PrintWriter(newFile);
            pw.print(sb.toString());
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Relation " + subsetOfTheDataset + " is written...");
    }


    //helper method to return var and its ranges
    public static VarRanges varRangesFromLocalFilePath(String lineComponentDelimiter, Dataset dataset, String varRangesLocalFilePath)
    {
        //read the variable ranges file and extract the ranges
        String varRangesFileContents = Utils.fileContentsFromLocalFilePath(varRangesLocalFilePath);
        String[] varRangesLines = StringUtils.split(varRangesFileContents, "\r\n|\r|\n");
        //first line is the header line, extract the range variables

        //obtain var ranges for this dataset
        VarRanges varRanges = VarRanges.getInstance(dataset);


        //now for each other line, extract the ranges of each variable, each line contains one variable, except first header line
        for(int index = 1; index < varRangesLines.length; index ++)
        {
            String thisLine = varRangesLines[index];

            //thisLineComponents will have the same length as of #ranges + 1
            String[] thisLineComponents = thisLine.split(lineComponentDelimiter);
            for(int idx = 0; idx < thisLineComponents.length; idx ++) thisLineComponents[idx] = thisLineComponents[idx].trim(); // trim every element

            //var name is at index 0 of thisLineComponents
            String thisVar = thisLineComponents[0];
            //OUTLIER_LOW, VALID_LOW, IMPUTE, VALID_HIGH, OUTLIER_HIGH
            Ranges rangesForThisVar
                    = new Ranges(dataset, thisVar,
                    Double.parseDouble(thisLineComponents[1]),
                    Double.parseDouble(thisLineComponents[2]),
                    Double.parseDouble(thisLineComponents[3]),
                    Double.parseDouble(thisLineComponents[4]),
                    Double.parseDouble(thisLineComponents[5]));

            //update the var ranges
            varRanges.add(thisVar, rangesForThisVar);
        } // for

        System.out.println("Read VarRanges files");

        return varRanges;
    } // varRanges


    //helper method to extract general descriptors from all files in each set of files
    //All valid values for general descriptors are non-negative (≥ 0).
    //A value of -1 indicates missing or unknown data, e.g. height or weight are not recorded
    //the structure to be returned is as follows:
    //RecordID,Age,Gender,Height,ICUType,Weight
    //132539,54,0,-1,4,-1
    //......
    public static void writeGeneralDescriptorsRecords(LinkedHashSet<String> descriptorVars, String lineComponentDelimiter,
                                                      String destinationLocalDirPath, String newFileExtension, String ...fileSetLocalDirPaths)
    {
        //for each file set directory, write one general descriptors records
        for(String thisFileSetDirPath : fileSetLocalDirPaths)
        {
            String thisFileSetDirName = fileNameFromPath(thisFileSetDirPath);

            StringBuilder genDescRecordsBuilder = new StringBuilder(String.join(lineComponentDelimiter, descriptorVars)).append("\n");

            //obtain file paths in this dir path
            List<String> filePathsInThisDir = listFilesFromLocalPath(thisFileSetDirPath, false);

            //now for each file, obtain general descriptors
            for(String filePath : filePathsInThisDir)
            {
                //obtain file contents
                String fileContents = fileContentsFromLocalFilePath(filePath);
                //split the lines
                String[] lines = StringUtils.split(fileContents, "\r\n|\r|\n");


                //map which maps varName to its value
                LinkedHashMap<String, String> varNameAndValueMap = new LinkedHashMap<>();
                //populate
                for(String varName : descriptorVars)
                    varNameAndValueMap.put(varName, null);


                //discard the first line, which is Time,Parameter,Value
                //for each line obtain descriptor vars and their values
                for(int index = 1; index < lines.length; index ++)
                {
                    String thisLine = lines[index];

                    //split the line with delimiter and obtain values
                    String[] thisLineComponents = thisLine.split(lineComponentDelimiter);

                    //first component is timestamp in hh:mm, then var name and then its value
                    int tsMinutes = Utils.toMinutes(thisLineComponents[0]);
                    String varName = thisLineComponents[1];
                    String varValue = thisLineComponents[2];

                    if(descriptorVars.contains(varName) && tsMinutes == 0)
                    {
                        //in some files descriptors vars are ordered differently, so use map to insert value correctly
                        varNameAndValueMap.put(varName, varValue);
                    } // if

                    //do not process other lines having tsMinutes bigger than 0
                    if(tsMinutes > 0) break;
                } // for each line


                //populate general descriptors for this file
                for(String varName : varNameAndValueMap.keySet())
                    genDescRecordsBuilder.append(varNameAndValueMap.get(varName)).append(lineComponentDelimiter);
                //append new line
                genDescRecordsBuilder.append("\n");


                //report if some variable have null value in the map after processing
                if(varNameAndValueMap.containsValue(null))
                    System.out.println("map, contains null value for some varName => " + filePath);
            } // for each file


            String newFileName
                    = "GeneralDescriptorsRecords"
                    + thisFileSetDirName.replace("set", "") + "." + newFileExtension;
            try {
                File newFile = new File(destinationLocalDirPath + File.separator + newFileName);
                newFile.createNewFile();
                PrintWriter pw = new PrintWriter(newFile);
                pw.print(genDescRecordsBuilder.toString());
                pw.close();

                System.out.println("Wrote file => " + newFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            } // catch

        } // for each file set dir

    } // writeGeneralDescriptorsRecords



    //helper method to load instances from multi-instance arff file
    public static Instances loadInstancesFromMIArffFile(String miArffFilePath, boolean isClassIndexLast) throws Exception
    {
        //sample mi arff file path => PHYSIONET_DATA_FOLDER + File.separator + "multi_instance_with_ts_arff" + File.separator + "physionet_dataset_.arff"
        ConverterUtils.DataSource source
                = new ConverterUtils.DataSource(miArffFilePath);
        //load all data
        Instances data = source.getDataSet();
        // setting class attribute if the data format does not provide this information
        // For example, the XRFF format saves the class attribute information as well
        if (data.classIndex() == -1)
            if (isClassIndexLast) data.setClassIndex(data.numAttributes() - 1);
            else    data.setClassIndex(0); // else it is first

        return data;
    } // loadInstancesFromMIArffFile



    //Sample execution statement:
    //Utils.writeMTSEsToMultiInstanceArffFile(Dataset.PhysioNet, mtses, outcomes,
    //    "patient_record_id", new String[]{"1", "0"}, true, DATA_FOLDER,
    //    "multi_instance_with_ts_arff", ""); // "", "set-a", "set-b", "set-c"
    //------------------------------------------------------------------------
    //helper method to write mtses to multi-instance format:
    //@relation physionet
    // @attribute patient_record_id {132539,132540,132541,132543,132545,...,163037}
    // @attribute bag relational
    //   @attribute (NI)DiasABP numeric
    //   @attribute (NI)MAP numeric
    //   @attribute (NI)SysABP numeric
    //   @attribute ALP numeric
    //   @attribute ALT numeric
    //   ...
    //   @attribute pH numeric
    // @end bag
    // @attribute class {0,1}
    //
    // @data
    // 132539,"0,59.26,79.05,119.4,116.75,394.61,506.54,2.92,27.42,2.91,156.52,1.51,0.55,11.4,141.5,23.12,30.68,87.52,4.14,2.92,2.03,139.07,40.47,150.42,190.81,19.72,96.64,37.04,7.15,1.2,119.57,12.67,83.6,7.49\n...",0
    // ...
    public static void writeMTSEsToMultiInstanceArffFile(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, String bagName,
                                                         String[] classLabels, boolean includeTsMinutes,
                                                         String destinationDirPath, String newDirNameToPutFile, String fileNameAppendix)
    {
        if(mtses.isEmpty()) throw new RuntimeException("At lease one mtse should be provided");
        List<String> vars = mtses.get(0).getVars();

        //obtain record ids from mtses in the given list order, where real bags will be constructed for these mtses
        List<String> recordIdsInMtses = new ArrayList<>();
        mtses.forEach(mtse -> recordIdsInMtses.add(mtse.getRecordID() + ""));

        //feedback
        System.out.println("Generating content for multi-instance arff file...");

        StringBuilder sb = new StringBuilder("@relation " + dataset.toSimpleString() + "_" + fileNameAppendix).append("\n\n");
        sb.append("@attribute ").append(bagName).append(" ")
                .append("{").append(String.join(",", recordIdsInMtses)).append("}").append("\n");
        sb.append("@attribute bag relational").append("\n");

        if(includeTsMinutes) sb.append(" @attribute ").append("tsMinutes").append(" numeric").append("\n");

        vars.forEach(s -> {
            sb.append(" @attribute ").append(s.trim()).append(" numeric").append("\n");
        });
        sb.append("@end bag").append("\n");
        sb.append("@attribute class {").append(String.join(",", classLabels)).append("}").append("\n\n");
        sb.append("@data").append("\n");
        for(MTSE mtse : mtses)
        {
            sb.append(mtse.getRecordID()).append(",").append("\"");
            Map<Integer, List<Double>> valuesInVarNameOrder = mtse.getValuesInVarNameOrder();
            List<String> joinedValuesInVarNameOrder = new ArrayList<>();
            for(Integer ts : valuesInVarNameOrder.keySet())
            {
                if(includeTsMinutes)
                    //it joins all elements in the row by comma
                    joinedValuesInVarNameOrder.add(ts + "," + String.join(",", toStringCollection(valuesInVarNameOrder.get(ts))));
                else
                    joinedValuesInVarNameOrder.add(String.join(",", toStringCollection(valuesInVarNameOrder.get(ts))));
            } // for
            //then joins rows by \n and appends a comma, class label and new line character
            sb.append(String.join("\\n", joinedValuesInVarNameOrder)).append("\"").append(",")
                    .append(outcomes.get(mtse.getRecordID()).getInHospitalDeath0Or1()).append("\n");
        } // for


        String newFileName
                = dataset.toSimpleString() + "_" + fileNameAppendix + ".arff";
        try
        {
            File dir = new File(destinationDirPath + File.separator + newDirNameToPutFile);
            if(!dir.exists()) dir.mkdir();

            File newFile = new File( dir.getAbsolutePath() + File.separator + newFileName);
            newFile.createNewFile();
            PrintWriter pw = new PrintWriter(newFile);
            pw.print(sb.toString());
            pw.close();

            System.out.println("Wrote file => " + newFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        } // catch
    } // writeMTSEsToMultiInstanceArffFile


    //helper method to obtain mtse list from set-a
    public static Object mtsesFromSetA(String setAPath, String varRangesFilePath)
    {
        VarRanges varRanges;
        if(VarRanges.getInstance(Dataset.PhysioNet).isEmpty())
            varRanges = Utils.varRangesFromLocalFilePath(",", Dataset.PhysioNet, varRangesFilePath);
        else
            varRanges = VarRanges.getInstance(Dataset.PhysioNet);


        List<String> setAFilePaths = Utils.listFilesFromLocalPath(setAPath, false);
        // = Utils.listFilesFromLocalPath("./" + method + "_set_a", false);

        List<String> allFilePaths = new ArrayList<>();
        allFilePaths.addAll(setAFilePaths);

        //preprocess files
        ConcurrentHashMap<String, String> filePathFileContentsMap
                = (ConcurrentHashMap<String, String>) Preprocessing//.generateTimeSeriesData(allFilePaths, "", varRanges);
                               .generateTimeSeriesDataInParallel(allFilePaths, "", varRanges);


        long start = System.currentTimeMillis();
        double missingValuePlaceHolder = -2.0;
        //List<MTSE> mtses = new ArrayList<>();
        //ConcurrentLinkedQueue<MTSE> mtses = new ConcurrentLinkedQueue<>();


        //make use of concurrency of the concurrent map
        List<MTSE> mtses = filePathFileContentsMap.entrySet().parallelStream().map(entry ->
                MTSE.fromFile(Dataset.PhysioNet, Utils.fileNameFromPath(entry.getKey()), entry.getValue(), ",", missingValuePlaceHolder))
                .collect(Collectors.toList());



//        //obtain multivariate time series from each file
//        for(String filePath : filePathFileContentsMap.keySet())
//        {
//            String fileName = Utils.fileNameFromPath(filePath);
//            String fileContents = filePathFileContentsMap.get(filePath);
//
//            //create multivariate time series object from each file's contents
//            MTSE mtse
//                    = MTSE.fromFile(Dataset.PhysioNet, fileName, fileContents, ",", missingValuePlaceHolder);
//            //println(mtse.toVerticalString());
//
//            mtses.add(mtse);
//            //break;
//        } // for

        //System.exit(0);
        long end = System.currentTimeMillis();
        System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(end-start) + " seconds for reading all mtses");


        //clear memory
        //filePathFileContentsMap.clear();
        //filePathFileContentsMap = null;

        //return //new ArrayList<>(mtses);
        return new ImmutablePair<>(setAFilePaths.size(), mtses);
    } // mtsesFromSetA


    //helper method to get the longest time series in csv format
    public static String longestMTSEInSetA(String imputeMethod, Object omtsesTrainSizePair, String varRangesFilePath)
    {
        VarRanges varRanges;
        if(VarRanges.getInstance(Dataset.PhysioNet).isEmpty())
            varRanges = Utils.varRangesFromLocalFilePath(",", Dataset.PhysioNet, varRangesFilePath);
        else
            varRanges = VarRanges.getInstance(Dataset.PhysioNet);


        //List<String> setAFilePaths = Utils.listFilesFromLocalPath(setAPath, false);
                                   // = Utils.listFilesFromLocalPath("./" + method + "_set_a", false);

//        List<String> allFilePaths = new ArrayList<>();
//        allFilePaths.addAll(setAFilePaths);
//
//        //preprocess files
//        Map<String, String> filePathFileContentsMap = Preprocessing.generateTimeSeriesData(allFilePaths, "", varRanges);


//        long start = System.currentTimeMillis();
        double missingValuePlaceHolder = -2.0;
//        List<MTSE> mtses = new ArrayList<>();
//
//
//        //obtain multivariate time series from each file
//        for(String filePath : filePathFileContentsMap.keySet())
//        {
//            String fileName = Utils.fileNameFromPath(filePath);
//            String fileContents = filePathFileContentsMap.get(filePath);
//
//            //create multivariate time series object from each file's contents
//            MTSE mtse
//                    = MTSE.fromFile(Dataset.PhysioNet, fileName, fileContents, ",", missingValuePlaceHolder);
//            //println(mtse.toVerticalString());
//
//            mtses.add(mtse);
//            //break;
//        } // for
//        //System.exit(0);
//        long end = System.currentTimeMillis();
//        System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(end-start) + " seconds for reading all mtses");
//
//
//        //clear memory
//        filePathFileContentsMap.clear();
//        filePathFileContentsMap = null;


        Imputations.ImputeMethod method = Imputations.ImputeMethod.MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION;
        if(imputeMethod.equalsIgnoreCase("mean"))
            method = Imputations.ImputeMethod.MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION;
        else if(imputeMethod.equalsIgnoreCase("forward"))
            method = Imputations.ImputeMethod.LIPTON_FORWARD_FILLING_IMPUTATION;
        else if(imputeMethod.equalsIgnoreCase("zero"))
            method = Imputations.ImputeMethod.ZERO_IMPUTATION;
        else if(imputeMethod.equalsIgnoreCase("normal_value"))
            method = Imputations.ImputeMethod.NORMAL_VALUE_IMPUTATION;




        Pair<Integer, List<MTSE>> realPair = (Pair<Integer, List<MTSE>>) omtsesTrainSizePair;
        int trainingSetSize = realPair.getLeft();
        List<MTSE> mtses = realPair.getRight();

        mtses = Imputations.getInstance().imputeParallel(mtses,
                //Imputations.ImputeMethod.MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION,
                //Imputations.ImputeMethod.LIPTON_FORWARD_FILLING_IMPUTATION,
                method,
                new int[]{0, trainingSetSize //setAFilePaths.size() //setCFilePaths.size()
                }, varRanges, missingValuePlaceHolder);


        int maxLength = 0;
        MTSE maxLengthMTSE = null;
        for(MTSE mtse : mtses)
        {
            int thisLength = mtse.getTimeStamps().size();
            if (thisLength > maxLength)
            {
                maxLength = thisLength;
                maxLengthMTSE = mtse;
            } // if
        } // for


        assert maxLengthMTSE != null;
        return maxLengthMTSE.toCSVString(false, false,true, false);
    } // longestMTSEInSetA



    //helper method to obtain the transformations
    public static String transformMtsesToCSVString(String imputeMethod, String transformMethod, Object omtsesTrainSizePair, String varRangesFilePath)
    {
        VarRanges varRanges;
        if(VarRanges.getInstance(Dataset.PhysioNet).isEmpty())
            varRanges = Utils.varRangesFromLocalFilePath(",", Dataset.PhysioNet, varRangesFilePath);
        else
            varRanges = VarRanges.getInstance(Dataset.PhysioNet);



        //List<String> setAFilePaths = Utils.listFilesFromLocalPath(setAPath, false);
        // = Utils.listFilesFromLocalPath("./" + method + "_set_a", false);

        //        List<String> allFilePaths = new ArrayList<>();
//        allFilePaths.addAll(setAFilePaths);
//
//        //preprocess files
//        Map<String, String> filePathFileContentsMap = Preprocessing.generateTimeSeriesData(allFilePaths, "", varRanges);


//        long start = System.currentTimeMillis();
        double missingValuePlaceHolder = -2.0;
//        List<MTSE> mtses = new ArrayList<>();
//
//
//        //obtain multivariate time series from each file
//        for(String filePath : filePathFileContentsMap.keySet())
//        {
//            String fileName = Utils.fileNameFromPath(filePath);
//            String fileContents = filePathFileContentsMap.get(filePath);
//
//            //create multivariate time series object from each file's contents
//            MTSE mtse
//                    = MTSE.fromFile(Dataset.PhysioNet, fileName, fileContents, ",", missingValuePlaceHolder);
//            //println(mtse.toVerticalString());
//
//            mtses.add(mtse);
//            //break;
//        } // for
//        //System.exit(0);
//        long end = System.currentTimeMillis();
//        System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(end-start) + " seconds for reading all mtses");
//
//
//        //clear memory
//        filePathFileContentsMap.clear();
//        filePathFileContentsMap = null;


        Imputations.ImputeMethod method = Imputations.ImputeMethod.MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION;
        if(imputeMethod.equalsIgnoreCase("mean"))
            method = Imputations.ImputeMethod.MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION;
        else if(imputeMethod.equalsIgnoreCase("forward"))
            method = Imputations.ImputeMethod.LIPTON_FORWARD_FILLING_IMPUTATION;
        else if(imputeMethod.equalsIgnoreCase("zero"))
            method = Imputations.ImputeMethod.ZERO_IMPUTATION;
        else if(imputeMethod.equalsIgnoreCase("normal_value"))
            method = Imputations.ImputeMethod.NORMAL_VALUE_IMPUTATION;


        //Unit<A>
        //Pair<A,B>
        //Triplet<A,B,C>
        //Quartet<A,B,C,D>
        //Quintet<A,B,C,D,E>
        //Sextet<A,B,C,D,E,F>
        //Septet<A,B,C,D,E,F,G>
        //Octet<A,B,C,D,E,F,G,H>
        //Ennead<A,B,C,D,E,F,G,H,I>
        //Decade<A,B,C,D,E,F,G,H,I,J>


        Pair<Integer, List<MTSE>> realPair = (Pair<Integer, List<MTSE>>) omtsesTrainSizePair;
        int trainingSetSize = realPair.getLeft();
        List<MTSE> mtses = realPair.getRight();

        mtses = Imputations.getInstance().imputeParallel(mtses,
                //Imputations.ImputeMethod.MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION,
                //Imputations.ImputeMethod.LIPTON_FORWARD_FILLING_IMPUTATION,
                method,
                new int[]{0, trainingSetSize, //setAFilePaths.size() //setCFilePaths.size()
                }, varRanges, missingValuePlaceHolder);


        //uses parallelstream API
        mtses = Utils.transformMtses(mtses, trainingSetSize, //setAFilePaths.size(), //setCFilePaths.size(),
                                                    TransformMethod.valueOf(transformMethod));

        StringBuilder sb
                = new StringBuilder(mtses.get(0).toCSVString(false, false, true, false));
        for(int index = 1; index < mtses.size(); index ++)
            sb.append(mtses.get(index).toCSVString(false, false, false, false));

        return sb.toString();
    } // transformMtsesToCSVString




    //helper method to generate number of positives and number of negatives in generated mtses
    public static void printNumPosNumNeg(Collection<MTSE> mtses, Outcomes outcomes)
    {
        int posCounter = 0;
        int negCounter = 0;
        for(MTSE mtse : mtses)
        {
            if(outcomes.get(mtse.getRecordID()).getInHospitalDeath0Or1() == 1)
                posCounter ++;
            else
                negCounter ++;
        } // for
        System.out.println("#positives: " + posCounter  + ", #negatives: " + negCounter);
    } // printNumPosNumNeg


    //helper method to transform mtses
    public static List<MTSE> transformMtses(List<MTSE> imputedMtses, int trainingSetEndIndex, TransformMethod... tMethods)
    {
        long start = System.currentTimeMillis();

        HashSet<Integer> trainingRecords = new HashSet<>(); // we do not consider a concurrent hashset since, we only do contains operation.
        imputedMtses.subList(0, trainingSetEndIndex).forEach(mtse -> trainingRecords.add(mtse.getRecordID()));

        //transformed mtses
        List<MTSE> tMtses;

        Queue<MTSE> trainingData = new ConcurrentLinkedQueue<>();
        Queue<MTSE> testData = new ConcurrentLinkedQueue<>();
        System.out.println("Transformation Methods: " + Arrays.toString(tMethods));
        System.out.println("Transformation started...");
        //AtomicInteger transformationCounter = new AtomicInteger(0);

        //train data and test data should be transformed separately, because in parallel stream orderings are not kept
        imputedMtses.parallelStream().forEach(mtse ->
        {
            MTSE tMtse = mtse.transformTo(tMethods);
            if(trainingRecords.contains(tMtse.getRecordID()))
                trainingData.add(tMtse);
            else
                testData.add(tMtse);
            //System.out.println("MTSE " + transformationCounter.incrementAndGet() + " transformed...");
        });
        List<MTSE> train = new ArrayList<>(trainingData);
        List<MTSE> test = new ArrayList<>(testData);
        Collections.sort(train); // sorts by record id to keep the original insertion ordering
        Collections.sort(test); // sorts by record id to keep the original insertion ordering
        train.addAll(test);
        tMtses = train;
        long end = System.currentTimeMillis();
        System.out.println("Transformation finished...");
        System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(end-start) + " seconds for transformation...");
        System.out.println("Number of transformed mtses: " + tMtses.size());

        return tMtses;
    } // transformMtses



    //helper method to transform mtses to embedding space without transformation
    public static List<MTSE> transformMtsesToEmSpace(List<MTSE> imputedMtses, int trainingSetEndIndex, int embeddingParameterM,
                                                     int delayParameterNu)
    {
        HashSet<Integer> trainingRecords = new HashSet<>(); // we do not consider a concurrent hashset since, we only do contains operation.
        imputedMtses.subList(0, trainingSetEndIndex).forEach(mtse -> trainingRecords.add(mtse.getRecordID()));

        //transformed mtses
        List<MTSE> tMtses;

        Queue<MTSE> trainingData = new ConcurrentLinkedQueue<>();
        Queue<MTSE> testData = new ConcurrentLinkedQueue<>();
        System.out.println("M = " + embeddingParameterM + ", NU = " + delayParameterNu + ", Transformation Method: EMBEDDINGS_AS_BAGS");
        System.out.println("Transformation started...");
        long start = System.currentTimeMillis();
        AtomicInteger embeddableCounter = new AtomicInteger(0);
        AtomicInteger transformationCounter = new AtomicInteger(0);
        //train data and test data should be transformed separately, because in parallel stream orderings are not kept
        imputedMtses.parallelStream().forEach(mtse ->
        {
            if(mtse.isEmbeddable(embeddingParameterM, delayParameterNu)) embeddableCounter.incrementAndGet();
            List<MTSE> embeddings = mtse.transformToEmbeddingSpace(embeddingParameterM, delayParameterNu);
            String recordID = String.valueOf(embeddings.get(0).getRecordID());
            if(trainingRecords.contains(Integer.parseInt(recordID.substring(0, recordID.length() - 1))))
                trainingData.addAll(embeddings);
            else
                testData.addAll(embeddings);
            System.out.println("MTSE " + transformationCounter.incrementAndGet() + " transformed...");
        });
        List<MTSE> train = new ArrayList<>(trainingData);
        List<MTSE> test = new ArrayList<>(testData);
        Collections.sort(train); // sorts by record id to keep the original insertion ordering
        Collections.sort(test); // sorts by record id to keep the original insertion ordering
        train.addAll(test);
        tMtses = train;

        for(MTSE tMtse : tMtses)
        {
            Outcomes outcomes = Outcomes.getInstance(tMtse.getDataset());
            String recordID = tMtse.getRecordID() + "";
            int origRecordID = Integer.parseInt(recordID.substring(0, recordID.length() - 1));
            Outcome outcome = outcomes.get(origRecordID);
            //obtained record id should be added to outcomes
            Outcome newOutcome = new Outcome(tMtse.getRecordID(), outcome.getSapsIScore(), outcome.getSofaScore(),
                    outcome.getLengthOfStayInDays(), outcome.getSurvivalInDays(), outcome.getInHospitalDeath0Or1());
            outcomes.add(tMtse.getRecordID(), newOutcome);
        } // for

        long end = System.currentTimeMillis();
        System.out.println("Transformation finished...");
        System.out.println("It took " + TimeUnit.MILLISECONDS.toMinutes(end-start) + " minutes for transformation...");
        System.out.println("Number of mtses after transformation: " + tMtses.size());
        System.out.println(embeddableCounter.intValue() + " mtses out of " + imputedMtses.size() + " were embeddable...");

        return tMtses;
    } // transformMtsesToEmSpace


    //helper method to transform mtses to embedding space
    public static List<MTSE> transformMtsesToEmSpace(List<MTSE> imputedMtses, int trainingSetEndIndex, int embeddingParameterM,
                                              int delayParameterNu, TransformMethod... tMethods)
    {
        HashSet<Integer> trainingRecords = new HashSet<>(); // we do not consider a concurrent hashset since, we only do contains operation.
        imputedMtses.subList(0, trainingSetEndIndex).forEach(mtse -> trainingRecords.add(mtse.getRecordID()));

        //transformed mtses
        List<MTSE> tMtses;

        Queue<MTSE> trainingData = new ConcurrentLinkedQueue<>();
        Queue<MTSE> testData = new ConcurrentLinkedQueue<>();
        System.out.println("M = " + embeddingParameterM + ", NU = " + delayParameterNu + ", Transformation Methods: " + Arrays.toString(tMethods));
        System.out.println("Transformation started...");
        long start = System.currentTimeMillis();
        AtomicInteger embeddableCounter = new AtomicInteger(0);
        AtomicInteger transformationCounter = new AtomicInteger(0);
        //train data and test data should be transformed separately, because in parallel stream orderings are not kept
        imputedMtses.parallelStream().forEach(mtse ->
        {
            if(mtse.isEmbeddable(embeddingParameterM, delayParameterNu)) embeddableCounter.incrementAndGet();
            MTSE tMtse = mtse.transformToEmbeddingSpace(embeddingParameterM, delayParameterNu, tMethods);
            if(trainingRecords.contains(tMtse.getRecordID()))
                trainingData.add(tMtse);
            else
                testData.add(tMtse);
            System.out.println("MTSE " + transformationCounter.incrementAndGet() + " transformed...");
        });
        List<MTSE> train = new ArrayList<>(trainingData);
        List<MTSE> test = new ArrayList<>(testData);
        Collections.sort(train); // sorts by record id to keep the original insertion ordering
        Collections.sort(test); // sorts by record id to keep the original insertion ordering
        train.addAll(test);
        tMtses = train;
        long end = System.currentTimeMillis();
        System.out.println("Transformation finished...");
        System.out.println("It took " + TimeUnit.MILLISECONDS.toMinutes(end-start) + " minutes for transformation...");
        System.out.println("Number of transformed mtses: " + tMtses.size());
        System.out.println(embeddableCounter.intValue() + " mtses out of " + imputedMtses.size() + " were embeddable...");

        return tMtses;
    } // transformMtsesToEmSpace


    //helper method to compate weka standardize and python standardise
    public static void compareStandardise(Dataset dataset, List<MTSE> mtses, Outcomes outcomes, int setASize, int setBSize, boolean keepTsAsAVariable)
            throws Exception
    {
        Instances setAData = Utils.mtsesToMIData(dataset, mtses.subList(0, setASize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-a");

        Instances setBData = Utils.mtsesToMIData(dataset, mtses.subList(setASize, setASize + setBSize),
                outcomes, "patient_record_id", new String[]{"1", "0"}, keepTsAsAVariable, "set-b");

        if (setAData.classIndex() == -1)
            setAData.setClassIndex(setAData.numAttributes() - 1);
        if (setBData.classIndex() == -1)
            setBData.setClassIndex(setBData.numAttributes() - 1);

        //to transform multi-instance data to single instance data
        SimpleMI smi = new SimpleMI();
        smi.setOptions(weka.core.Utils.splitOptions("-M 1")); // arithmetic average, for bag containing single instance this does not nothing

        Instances train = smi.transform(setAData);
        train.deleteAttributeAt(0); // delete the bagID attribute
        Instances test = smi.transform(setBData);
        test.deleteAttributeAt(0); // delete the bagID attribute


        PyScriptFilter filter = new PyScriptFilter();
        filter.setPythonFile(new File("scripts/standardise.py"));
        filter.setInputFormat(train);

        Instances pyscriptData = Filter.useFilter(train, filter);

        Standardize filter2 = new Standardize();
        filter2.setInputFormat(train);

        Instances defaultStdData = Filter.useFilter(train, filter2);

        // check instances
        for(int x = 0; x < train.numInstances(); x++) {
            assertEquals(pyscriptData.get(x).toString(), defaultStdData.get(x).toString());
        }
    } // compareStandardise


    //helper method to print weka instances
    public static void printWekaInstances(Instances data, int limit)
    {
        if(limit < 1 || limit > data.numInstances())
            throw new RuntimeException("limit should be in range");

        for(int index = 0; index < limit; index ++)
        {
            System.out.println(data.instance(index).toString());
        } // for
    } // printWekaInstances


    //helper method to convert Number collection to String collection
    public static Collection<String> toStringCollection(Collection<? extends Number> collection)
    {
        List<String> stringReps = new ArrayList<>();
        for(Number n : collection)
        {
            stringReps.add(n.toString());
        } // for

        return stringReps;
    } // toStringCollection


    //numeric attribute range in the form of "first-last"
    //sample call: Utils.csv2Arff(localFilePath, "1-" + (consideredVars.size() + 1)); // +1 for tsMinutes
    public static void csv2Arff(String localFilePathString, String numericAttributeRange)
    {

        //System.out.println("\nUsage: CSV2Arff <input.csv> <output.arff>\n");

        //file consistency check
        try
        {
            File localFile = new File(localFilePathString);

            //if the localFile is a directory, print message to standard error and return
            if (localFile.isDirectory()) {
                System.err.println("\n" + localFilePathString + " is a directory; not able to generate writable file path from it");
            } // if
            //there is a possibility given path does not exist which means does not refer to either file or directory
            //so perform checks for each of them; which is equivalent to FileSystem.resolvePath() for HDFS
            else if (localFile.isFile())
            {
                //get the parent directory of localFile
                File localFileParentDir = localFile.getParentFile();

                //make a new directory called <resultFileNameAppendix> in the parent directory; if that directory exists do not create it
                //getAbsolutePath() returns path string without trailing directory separator
                File resultingSubDir = new File(localFileParentDir.getAbsolutePath() + File.separator + "arff"); //newDirName);

                //mkdir a directory if the resulting sub dir path does not exist
                if (!resultingSubDir.exists())
                    resultingSubDir.mkdir();


                //change the file extension
                String localFileName = localFile.getName();
                String currentFileExtension = FilenameUtils.getExtension(localFileName);
                String newLocalFileName = localFileName.replace(currentFileExtension, "arff"); //newFileExtension);


                //now create a writable path from resultingSubDir and the given localFile
                //getAbsolutePath() returns path string without trailing directory separator
                String writableFilePathString = resultingSubDir.getAbsolutePath() + File.separator + newLocalFileName;

                //create a file from writable path
                File writableFile = new File(writableFilePathString);
                writableFile.createNewFile();


                //now generate arff file using WEKA API
                // load CSV
                CSVLoader loader = new CSVLoader();
                loader.setSource(localFile);
                //below line not working with weka version 3.7.0
                //loader.setNumericAttributes(numericAttributeRange); // forces all attributed to be set numeric, argument should be "first-last"
                Instances data = loader.getDataSet();

                // save ARFF
                ArffSaver saver = new ArffSaver();
                saver.setInstances(data);
                saver.setFile(writableFile);
                //saver.setDestination(writableFile); no need as of weka 3.5.3
                saver.writeBatch();


                System.out.println("Generated arff file at => " + writableFile.getAbsolutePath());
            } // else if
            else
                System.err.println("\n" + localFilePathString + " is neither a file, nor a directory; please provide correct file path");

        } // try
        catch (Exception ex) {
            ex.printStackTrace();
        } // catch

    } // csv2Arff


    public static void csv2Arff(String fileContents, String relationName, //int recordID,
                                String destination//, String numericAttributeRange
    ) throws IOException
    {
        //now generate arff file using WEKA API
        // load CSV
        CSVLoader loader = new CSVLoader();
        loader.setSource(stringToInputStream(fileContents));
        //below line not working with weka version 3.7.0
        //loader.setNumericAttributes("first-last"); // forces all attributed to be set numeric, argument should be "first-last"
        Instances data = loader.getDataSet();
        data.setRelationName(relationName);

        // Declare the class attribute along with its values
//        FastVector fvClassVal = new FastVector(2);
//        fvClassVal.addElement("1.0");
//        fvClassVal.addElement("0.0");
//        Attribute label = new Attribute("class", fvClassVal);
//        data.setClass(label);
//
        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1);

        // save ARFF
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(destination + File.separator + relationName + ".arff"));
        //saver.setDestination(writableFile); no need as of weka 3.5.3
        saver.writeBatch();

        loader.reset();


        System.out.println(relationName + ".arff  written");
    } // csv2Arff


    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }


//    public static boolean isFloat(String str) {
//        try {
//            Float.parseFloat(str);
//            return true;
//        } catch (NumberFormatException nfe) {
//            return false;
//        }
//    }


    public static boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }


    //method to calculate mean arterial (blood) pressure from systolic (sbp or sysabp) and diastolic (dbp or disabp) blood pressures
    public static double map(int sbp, int dbp)
    {
        if(sbp < 0 || dbp < 0)
            return -1;

        //formula is (sbp + 2 * dbp) / 3
        double sum = sbp + 2.0 * dbp;

        return format("#.##", RoundingMode.HALF_UP, sum / 3);
    } // map


    //helper method to calculate sysabp (sbp) from map and diasabp (dbp)
    public static int sbp(double map, int dbp)
    {
        if(Double.compare(map, 0.0) < 0 || dbp < 0)
            return -1;

        double sbp = 3.0 * map - 2.0 * dbp;
        if(Double.compare(sbp, 0.0) < 0)
            return -1;

        return (int) sbp;
    } // sbp


    //helper method to calculate diasabp (dbp) from mao and sysabp (sbp)
    public static int dbp(double map, int sbp)
    {
        if(Double.compare(map, 0.0) < 0 || sbp < 0)
            return -1;

        double dbp = (3.0 * map - 1.0 * sbp) / 2.0;
        if(Double.compare(dbp, 0.0) < 0)
            return -1;

        return (int) dbp;
    } // dbp


    public static double format(String decimalFormatPattern, RoundingMode roundingMode, double value)
    {
        DecimalFormat df = new DecimalFormat(decimalFormatPattern);
        df.setRoundingMode(roundingMode);

        return Double.parseDouble(df.format(value));
    } // format


    //helper method to calculate the mean of double values
    public static double mean(Collection<Double> values, boolean formatDecimalPlaces)
    {
        double sum = 0.0;
        for(double value : values)
        {
            sum += value;
        } // for

        if (formatDecimalPlaces)
            return format("#.##", RoundingMode.HALF_UP, sum / values.size());
        else
            return sum / values.size();
    } // mean


    //helper method to calculate the weighted mean, note that weights should be in corresponding order for the values
    public static double weightedMean(List<Double> values, List<Double> weights)
    {
        if(values.size() != weights.size())
            throw new RuntimeException("number of values and number of weights should be the same");

        double sumOfWeights = 0;
        for(double weight : weights)
            sumOfWeights += weight;

        double weightedSum = 0;
        for(int index = 0; index < values.size(); index ++)
        {
            weightedSum += values.get(index) * weights.get(index);
        } // for

        return weightedSum / sumOfWeights;
    } // weightedMean


    public static double median(Double[] input)
    {
        if (input.length == 0) {
            throw new IllegalArgumentException("to calculate median we need at least 1 element");
        }

        Arrays.sort(input);

        if (input.length % 2 == 0)
        {
            return (input[input.length / 2 - 1] + input[input.length / 2]) / 2.0;
        }
        else
            return input[input.length / 2];
    } // median


    //the overloaded version to compute the median form the values
    public static double median(Collection<Double> values)
    {
        if (values.size() == 0) {
            throw new IllegalArgumentException("to calculate median we need at least 1 element");
        }

        return median(values.toArray(new Double[]{}));
    } // median



    //helper method to calculate population variance of the given list of values
    public static double populationVariance(Collection<Double> input)
    {
        if(input.isEmpty() || input.size() == 1) return 0;

        //calculate the mean first
        double mean = mean(input, false);

        double sumOfSquareDifferences = 0;
        for(double val : input)
        {
            double squareDifference = (val - mean) * (val - mean);
            sumOfSquareDifferences += squareDifference;
        } // for

        return sumOfSquareDifferences / input.size();
    } // populationVariance


    //helper method to calculate the population std of the list of values
    public static double populationStd(Collection<Double> input)
    {
        return Math.sqrt(populationVariance(input));
    } // populationStd


    //method to calculate the weighted variance
    public static double weightedPopulationVariance(List<Double> values, List<Double> weights)
    {
        if(values.isEmpty() || values.size() == 1) return 0;

        double weightedMean = weightedMean(values, weights);

        double sumOfWeights = 0;
        for(double weight : weights)
            sumOfWeights += weight;

        double sumOfWeightedSquareDifferences = 0;
        for(int index = 0; index < values.size(); index++)
        {
            double weightedSquareDifference = weights.get(index) * (values.get(index) - weightedMean) * (values.get(index) - weightedMean);
            sumOfWeightedSquareDifferences += weightedSquareDifference;
        } // for

        return sumOfWeightedSquareDifferences / sumOfWeights;
    } // weightedPopulationVariance


    //helper method to calculate weighted std
    public static double weightedPopulationStd(List<Double> values, List<Double> weights)
    {
        return Math.sqrt(weightedPopulationVariance(values, weights));
    } // weightedPopulationStd


    //helper method to find mode of the numbers, mode is the most common number
    public static double modeOrMean(List<Double> values)
    {
        if(values.isEmpty())
            throw new RuntimeException("there should be at least one number!");

        double maxValue = 0;
        int maxCount = 0;

        for (int i = 0; i < values.size(); ++i)
        {
            int count = 0;
            for (Double value : values) {
                if (value.equals(values.get(i)))
                    ++count;
            }

            if (count > maxCount) {
                maxCount = count;
                maxValue = values.get(i);
            }
        } // for

        //if maxCount is still one - no most common number return mean
        if(maxCount == 1)
            return mean(values, false);
        else
            return maxValue;
    } // mode


    //helper method to find min
    public static double min(List<Double> values)
    {
        if(values.isEmpty())
            throw new RuntimeException("there should be at least one number!");

        if(values.size() == 1)
            return values.get(0);

        double min = Double.POSITIVE_INFINITY;
        for(double value : values)
        {
            if(value < min)
                min = value;
        } // for

        return min;
    } // min

    //helper method to find max
    public static double max(List<Double> values)
    {
        if(values.isEmpty())
            throw new RuntimeException("there should be at least one number!");

        if(values.size() == 1)
            return values.get(0);

        double max = Double.NEGATIVE_INFINITY;
        for(double value : values)
        {
            if(value > max)
                max = value;
        } // for

        return max;
    } // max


    //helper method to find range; range = max - min
    public static double range(List<Double> values)
    {
        return max(values) - min(values);
    } // range


    //weighted geometric center is not possible, since you use min and max
    //helper method to locate geometric center
    public static double geometricCenter(List<Double> values)
    {
        //per definition in SimpleMI class and https://stackoverflow.com/questions/30858889/finding-center-of-set-of-coordinates-using-java
        return (min(values) + max(values)) / 2.0;
    } // geometricCenter



    //helper method to find the kurtosis of the points
    public static double kurtosis(List<Double> values)
    {
        //by definition provided here: https://www.itl.nist.gov/div898/handbook/eda/section3/eda35b.htm
        //find mean and std first
        double mean = mean(values, false);
        double std = populationStd(values);

        double sumOfQuadrupledDifferences = 0;
        for(double value : values)
        {
            double quadrupledDifference = Math.pow(value - mean, 4);
            sumOfQuadrupledDifferences += quadrupledDifference;
        } // for

        return sumOfQuadrupledDifferences / (values.size() * Math.pow(std, 4));
    } // kurtosis


    //helper method to find skewness of points
    public static double skewness(List<Double> values)
    {
        //by definition provided here: https://www.itl.nist.gov/div898/handbook/eda/section3/eda35b.htm
        //find mean and std first
        double mean = mean(values, false);
        double std = populationStd(values);

        double sumOfTripledDifferences = 0;
        for(double value : values)
        {
            double tripledDifference = Math.pow(value - mean, 3);
            sumOfTripledDifferences += tripledDifference;
        } // for

        return sumOfTripledDifferences / (values.size() * Math.pow(std, 3));
    } // skewness


    //helper method to calculate averaged power of the given points
    public static double averagedPower(List<Double> values)
    {
        if(values.isEmpty())
            throw new RuntimeException("there should be at least one number!");

        return energySpectralDensity(values) / (1.0 * values.size());
    } // averagedPower


    //helper method to calculate energy spectral density of the points given in the order (timestamped based order)
    public static double energySpectralDensity(List<Double> values)
    {
        if(values.isEmpty())
            throw new RuntimeException("there should be at least one number!");


        //Formula -> energy = sum(from 0 to T) x(t)^2
        double sumOfPowers = 0;
        for(double value : values)
        {
            double power = Math.pow(value, 2);
            sumOfPowers += power;
        } // for

        return sumOfPowers;

        //signal is not continuous
        ////we assume that the numbers in values are given in the timestamp based order
        ////then energy E = integral(-infinity, +infinity) |x(t)|^2 dt where t is the time step (T is the last time step)
        ////this simplifies to E = from(t_0, T) |x(t)|^3 / 3 = |x(T)|^3 / 3 - |x(t_0)|^3 / 3
        //int T = values.size() - 1;
        //int t_0 = 0;
        //return ( Math.pow(Math.abs(values.get(T)), 3) / 3.0 ) - ( Math.pow(Math.abs(values.get(t_0)), 3) / 3.0 );

    } // energySpectralDensity



    //helper method to convert points to rms (root mean square)
    public static double rms(List<Double> values)
    {
        return Math.sqrt(averagedPower(values));
    } // rms


    //helper method to convert the points to geometric mean
    public static double geometricMean(List<Double> values)
    {
        if(values.isEmpty())
            throw new RuntimeException("there should be at least one number!");

        double product = 1.0;
        for(double value : values)
            product *= value;

        return Math.pow(product, 1.0 / values.size());
    } // geometricMean


    //helper method to calculate weighted geometric mean
    public static double weightedGeometricMean(List<Double> values, List<Double> weights)
    {
        if(values.isEmpty() || weights.isEmpty())
            throw new RuntimeException("There should be at least one number or weight!");
        if(values.size() != weights.size())
            throw new RuntimeException("The number of values and the number of weights should be the same");

        //formula is here: https://en.wikipedia.org/wiki/Weighted_geometric_mean

        //calculate sum of weights
        double sumOfWeights = 0;
        for(double weight : weights)
            sumOfWeights += weight;

        double weightedProduct = 1.0;
        for(int index = 0; index < values.size(); index++)
            weightedProduct *= Math.pow(values.get(index), weights.get(index));

        return Math.pow(weightedProduct, 1.0 / sumOfWeights);
    } // weightedGeometricMean





    //helper method to compute the mean of variables in all given time series using masking vector
    public static Map<String, Double> meansOfVariablesUsingMaskingVector(List<MTSE> mtses)
    {
        if(mtses.isEmpty()) throw new RuntimeException("At least one mtse should be provided as an argument");

        HashMap<String, Double> varMeanMap = new HashMap<>();
        //all mtses have the same vars, so get vars
        List<String> vars = mtses.get(0).getVars();

        //for each variable compute the mean
        for(String var : vars)
        {

//            double sumOfMaskingMultipliedByVarValueForThisVar = 0;
//            double sumOfMaskingsForThisVar = 0;
//
//            for(MTSE mtse : mtses)
//            {
//                List<Double> varValuesInTsOrderForThisVar = mtse.getVarValuesInTsOrder().get(var);
//                List<Integer> maskingsInTsOrderForThisVar = mtse.getMaskingsInTsOrder().get(var);
//
//                //if masking size and number of var values are different throw exception
//                if(varValuesInTsOrderForThisVar.size() != maskingsInTsOrderForThisVar.size())
//                    throw new RuntimeException("Masking size and number of var values should be the same");
//
//                for(int tsIndex = 0; tsIndex < varValuesInTsOrderForThisVar.size(); tsIndex ++)
//                {
//                    sumOfMaskingsForThisVar += maskingsInTsOrderForThisVar.get(tsIndex);
//                    sumOfMaskingMultipliedByVarValueForThisVar
//                            += maskingsInTsOrderForThisVar.get(tsIndex) * varValuesInTsOrderForThisVar.get(tsIndex);
//                } // for each tsIndex
//            } // for each mtse
//
//            Double meanForThisVar
//                    = Utils.format("#.##", RoundingMode.HALF_UP,
//                    sumOfMaskingMultipliedByVarValueForThisVar / sumOfMaskingsForThisVar);
//            varMeanMap.put(var, meanForThisVar);


            DoubleAdder sumOfMaskingMultipliedByVarValueForThisVar = new DoubleAdder();
            DoubleAdder sumOfMaskingsForThisVar = new DoubleAdder();

            mtses.parallelStream().forEach(mtse ->
            {
                List<Double> varValuesInTsOrderForThisVar = mtse.getVarValuesInTsOrder().get(var);
                List<Integer> maskingsInTsOrderForThisVar = mtse.getMaskingsInTsOrder().get(var);

                //if masking size and number of var values are different throw exception
                if(varValuesInTsOrderForThisVar.size() != maskingsInTsOrderForThisVar.size())
                    throw new RuntimeException("Masking size and number of var values should be the same");

                for(int tsIndex = 0; tsIndex < varValuesInTsOrderForThisVar.size(); tsIndex ++)
                {
                    sumOfMaskingsForThisVar.add(maskingsInTsOrderForThisVar.get(tsIndex));
                    sumOfMaskingMultipliedByVarValueForThisVar
                            .add(maskingsInTsOrderForThisVar.get(tsIndex) * varValuesInTsOrderForThisVar.get(tsIndex));
                } // for each tsIndex
            });

            Double meanForThisVar
                    = Utils.format("#.##", RoundingMode.HALF_UP,
                    sumOfMaskingMultipliedByVarValueForThisVar.doubleValue() / sumOfMaskingsForThisVar.doubleValue());
            varMeanMap.put(var, meanForThisVar);

        } // for each var

        return varMeanMap;
    } // meansOfVariablesUsingMaskingVector


    //helper method to calculate the medians of variables in mtses
    public static Map<String, Double> mediansOfVariables(List<MTSE> mtses)
    {
        if(mtses.isEmpty()) throw new RuntimeException("At least one mtse should be provided as an argument");

        HashMap<String, Double> varMedianMap = new HashMap<>();
        //all mtses have the same vars, so get vars
        List<String> vars = mtses.get(0).getVars();

        HashMap<String, ConcurrentLinkedQueue<Double>> varAndAllValuesMap = new HashMap<>();
        //initialize a list for each var
        for(String var : vars)
            varAndAllValuesMap.put(var, new ConcurrentLinkedQueue<>());

        //for each variable, collect all its non-negative values
        for(String var : vars)
        {
            ConcurrentLinkedQueue<Double> allValuesOfThisVar = varAndAllValuesMap.get(var);

            mtses.parallelStream().forEach(mtse ->
            {
                List<Double> varValuesInTsOrderForThisVar = mtse.getVarValuesInTsOrder().get(var);
                List<Integer> maskingsInTsOrderForThisVar = mtse.getMaskingsInTsOrder().get(var);

                //if masking size and number of var values are different throw exception
                if(varValuesInTsOrderForThisVar.size() != maskingsInTsOrderForThisVar.size())
                    throw new RuntimeException("Masking size and number of var values should be the same");

                for (Double valueAtThisTs : varValuesInTsOrderForThisVar)
                {
                    if (valueAtThisTs >= 0) //only add valid values to the list, missing values are < 0
                        allValuesOfThisVar.add(valueAtThisTs);
                } // for each tsIndex


            }); // for each mtse

        } // for each var

        //now for each var sort its list and compute median
        for(String var : varAndAllValuesMap.keySet())
        {
            ConcurrentLinkedQueue<Double> list = varAndAllValuesMap.get(var);
            //list.sort(Double::compareTo); // sorts inside median()

            //compute the median
            double median = median(list); // method internally sorts the array
            varMedianMap.put(var, median);
        } // for

        return varMedianMap;
    } // mediansOfVariables



    //var args to array method
    public static <T> T[] varArgsToArray(T... args)
    {
        return args;
    }


    //helper method to generate outcomes from "outcomes" files
    public static Outcomes outcomesFromLocalFilePaths(String lineComponentDelimiter, Dataset dataset, String... outcomesLocalFilePaths)
    {
        if(outcomesLocalFilePaths.length == 0)
            throw new RuntimeException("Please provide at least one file path for outcomes");

        //create outcomes for the given dataset
        Outcomes outcomes = Outcomes.getInstance(dataset);

        //now process each file
        for(String outcomesFilePath : outcomesLocalFilePaths)
        {
            String fileContents = fileContentsFromLocalFilePath(outcomesFilePath);
            //obtain lines
            String[] lines = StringUtils.split(fileContents, "\r\n|\r|\n");

            //now for each line, split the line, obtain the outcomes
            //file structure is as follows:
            //RecordID,SAPS-I,SOFA,Length_of_stay,Survival,In-hospital_death
            //132539,6,1,5,-1,0
            //132540,16,8,8,-1,0
            //132541,21,11,19,-1,0
            //.....

            //all values are integer representable
            //discard the first line which is the header line
            for(int index = 1; index < lines.length; index ++)
            {
                String thisLine = lines[index];

                //line will have 6 components
                String[] thisLineComponents = thisLine.split(lineComponentDelimiter);

                //retrieve values
                int recordID = Integer.parseInt(thisLineComponents[0]);
                int sapsIScore = Integer.parseInt(thisLineComponents[1]);
                int sofaScore = Integer.parseInt(thisLineComponents[2]);
                int lengthOfStayInDays = Integer.parseInt(thisLineComponents[3]);
                int survivalInDays = Integer.parseInt(thisLineComponents[4]);
                int inHospitalDeath0Or1 = Integer.parseInt(thisLineComponents[5]);

                //if(!(inHospitalDeath0Or1 == 0 || inHospitalDeath0Or1 == 1)) System.err.println("missing outcomes ");

                //create an outcome
                Outcome outcome = new Outcome(recordID, sapsIScore, sofaScore, lengthOfStayInDays, survivalInDays, inHospitalDeath0Or1);
                //update outcomes for this dataset
                outcomes.add(recordID, outcome);

            } // for each line
        } // for each file

        System.out.println("Read Outcomes files");

        return outcomes;
    } // outcomesFromLocalFilePaths


    //helper method to obtain general descriptors from file paths
    public static GeneralDescriptorsRecords genDescRecordsFromLocalFilePaths(String lineComponentDelimiter, Dataset dataset,
                                                                             String... generalDescriptorsRecordsLocalFilePaths)
    {
        if(generalDescriptorsRecordsLocalFilePaths.length == 0)
            throw new RuntimeException("Please provide at least one file path for general descriptors records");

        //create general descriptors records for the given dataset
        GeneralDescriptorsRecords records = GeneralDescriptorsRecords.getInstance(dataset);

        //now process each file
        for(String genDescRecordsLocalFilePath : generalDescriptorsRecordsLocalFilePaths)
        {
            String fileContents = fileContentsFromLocalFilePath(genDescRecordsLocalFilePath);
            //obtain lines
            String[] lines = StringUtils.split(fileContents, "\r\n|\r|\n");

            //now for each line, split the line, obtain the general descriptors records
            //file structure is as follows:
            ///RecordID,Age,Gender,Height,ICUType,Weight
            //132539,54,0,-1,4,-1
            //......


            //discard the first line which is the header line
            for(int index = 1; index < lines.length; index ++)
            {
                String thisLine = lines[index];

                //line will have 6 components
                String[] thisLineComponents = thisLine.split(lineComponentDelimiter);

                try {
                    //retrieve values
                    int recordID = Integer.parseInt(thisLineComponents[0]);
                    int ageInYears = Integer.parseInt(thisLineComponents[1]);
                    int gender0or1 = Integer.parseInt(thisLineComponents[2]);
                    double heightInCentimeters = Double.parseDouble(thisLineComponents[3]);
                    int icuTypeCode = Integer.parseInt(thisLineComponents[4]);
                    double weighInKg = Double.parseDouble(thisLineComponents[5]);

                    //create a GeneralDescriptorsRecord
                    GeneralDescriptorsRecord record = new GeneralDescriptorsRecord(recordID, ageInYears,
                            gender0or1, heightInCentimeters, icuTypeCode, weighInKg);
                    //update GeneralDescriptorsRecords for this dataset
                    records.add(recordID, record);

                }
                catch(NumberFormatException ex)
                {
                    ex.printStackTrace();
                    System.out.println(genDescRecordsLocalFilePath + "  => " + thisLine + " => " + (index + 1));
                } // catch

            } // for each line
        } // for each file


        System.out.println("Read GeneralDescriptorsRecords files");

        return records;
    } // genDescRecordsFromLocalFilePaths



    //helper method to cross validate data
    public static void crossValidate(Classifier cls, Instances data, Instances holdOutTestData, int seed, int folds, boolean stratifyFolds,
                                     boolean displayRocCurve) throws Exception
    {
        System.out.println("Original data: " + Utils.classImbalanceOnWekaInstances(data));

        // randomize data
        Random rand = new Random(seed);
        Instances randData = new Instances(data);
        randData.randomize(rand);

        System.out.println("Randomized data: " + Utils.classImbalanceOnWekaInstances(randData));

        //  For example in a binary classification problem where we want to predict if a passenger on Titanic survived or not.
        //  we have two classes here Passenger either survived or did not survive.
        //  We ensure that each fold has a percentage of passengers that survived, and a percentage of passengers that did not survive.
        if (stratifyFolds && randData.classAttribute().isNominal())
        {
            System.out.println("Class value is nominal, stratifying folds");
            randData.stratify(folds); // stratification is better in every case

            System.out.println("Randomized data after stratification: " + Utils.classImbalanceOnWekaInstances(randData));
        }



        // perform cross-validation
        System.out.println();
        System.out.println("=== Setup ===");
        System.out.println("Classifier: " + weka.core.Utils.toCommandLine(cls));
        System.out.println("Dataset: " + randData.relationName());
        System.out.println("Folds: " + folds);
        System.out.println("Seed: " + seed);
        System.out.println();
        Evaluation evalAll = new Evaluation(randData);

        //list of aurocs and auprcs
        List<Double> aurocs = new ArrayList<>();
        List<Double> auprcs = new ArrayList<>();

        for (int n = 0; n < folds; n++)
        {
            Evaluation eval = new Evaluation(randData);
            Instances train = randData.trainCV(folds, n, rand);
            Instances test = randData.testCV(folds, n);
            // the above code is used by the StratifiedRemoveFolds filter, the
            // code below by the Explorer/Experimenter:
            // Instances train = randData.trainCV(folds, n, rand);

            System.out.println("Fold " + (n+1) + ", training data => " + Utils.classImbalanceOnWekaInstances(train));
            if(holdOutTestData == null)
                System.out.println("Fold " + (n+1) + ", test data => " + Utils.classImbalanceOnWekaInstances(test));
            else
                System.out.println("Fold " + (n+1) + ", test data => " + Utils.classImbalanceOnWekaInstances(holdOutTestData));


            // build and evaluate classifier
            Classifier clsCopy = AbstractClassifier.makeCopy(cls);
                                //Classifier.makeCopy(cls); //weka 3.7.0
            clsCopy.buildClassifier(train);
            if(holdOutTestData == null) {
                eval.evaluateModel(clsCopy, test);
                evalAll.evaluateModel(clsCopy, test);
            }
            else {
                eval.evaluateModel(clsCopy, holdOutTestData);
                evalAll.evaluateModel(clsCopy, holdOutTestData);
            }


            System.out.println("\n=== Fold " + (n+1) + ", Classifier: " + clsCopy.getClass() + ",      AUROC: " + eval.areaUnderROC(0)
                    + ",      Weighted AUROC: " + eval.weightedAreaUnderROC()
                    //+ ",      AUPRC: " + eval.areaUnderPRC(0)
                    //+ ",      Weighted AUPRC: " + eval.weightedAreaUnderPRC()
                    + ",      Accuracy: " + accuracy(eval) + " ==== ");

            aurocs.add(eval.areaUnderROC(0));
            //auprcs.add(eval.areaUnderPRC(0));

            System.out.println("=== Fold " + (n+1) + ", Classifier: " + clsCopy.getClass() + ", Avg. AUROC: " + evalAll.areaUnderROC(0)
                    + ", Avg. Weighted AUROC: " + evalAll.weightedAreaUnderROC()
                    //+ ", Avg. AUPRC: " + evalAll.areaUnderPRC(0)
                    //+ ", Avg. Weighted AUPRC: " + evalAll.weightedAreaUnderPRC()
                    + ", Avg. Accuracy: " + accuracy(evalAll) +  " ==== \n");

            // output evaluation
            System.out.println();
            System.out.println(eval.toMatrixString("=== Confusion matrix for fold " + (n+1) + "/" + folds + " ===\n"));
        } // for


        //TODO save model and test on hold-out-set;
        // check with weka explorer: https://www.youtube.com/watch?v=UzT4W1tOKD4
        // this page https://waikato.github.io/weka-wiki/generating_classifier_evaluation_output_manually/
        // says models are saved as built from full training set, even after cross validation,
        // in this case, compare explorer output to programmatic train-test output


        //finally, evaluate model on hold-out test data; not meaningful
        //System.out.println("Final evaluation on hold-out test set...");
        //evalAll.evaluateModel(Classifier.makeCopy(cls), holdOutTestData);


        //evalAll.crossValidateModel(cls, data, folds, new Random(seed)); // Equivalent to the for loop above


        // output evaluation
        System.out.println();
        System.out.println(evalAll.toSummaryString("=== " + folds + "-fold Cross-validation ===", false));

        System.out.println("AUROC => " + evalAll.areaUnderROC(0));
        System.out.println("Weighted AUROC => " + evalAll.weightedAreaUnderROC());
        //System.out.println("AUPRC => " + evalAll.areaUnderPRC(0));
        //System.out.println("Weighted AUPRC => " + evalAll.weightedAreaUnderPRC());
        System.out.println("Accuracy => " + accuracy(evalAll));
        System.out.println("Max AUROC in runs => " + max(aurocs));
        System.out.println("Max AUPRC in runs => " + max(auprcs));
        System.out.println("====================================================");


        // generate curve
        if(displayRocCurve)
            toROCCurve(evalAll);
    } // crossValidate


    //helper method to train and test on in given number of runs
    public static void simpleTrainTestOverMultipleRuns(Classifier cls, Instances trainingData, int runs,
                                                       Instances testData, boolean displayROCCurve
                                                        //, Instances separateHoldOutSet, boolean testOnSeparateHoldOutSet
                                                        ) throws Exception
    {
        System.out.println("WEKA version: " + Version.VERSION);
        System.out.println("Original data: " + Utils.classImbalanceOnWekaInstances(trainingData));

        // perform cross-validation
        System.out.println();
        System.out.println("=== Setup ===");
        System.out.println("Classifier: " + cls.toString()); //weka.core.Utils.toCommandLine(cls));
        System.out.println("Dataset: " + trainingData.relationName());
        System.out.println("Runs: " + runs);
        System.out.println();
        Evaluation evalAll = new Evaluation(trainingData);

        //list of aurocs and auprcs
        List<Double> aurocs = new ArrayList<>();
        List<Double> auprcs = new ArrayList<>();

        for (int i = 0; i < runs; i++)
        {
            // randomize data
            int seed = i + 1;
            Random rand = new Random(seed);
            Instances randTrainingData = new Instances(trainingData);
            randTrainingData.randomize(rand);


            // build classifier
            Classifier clsCopy = AbstractClassifier.makeCopy(cls);
                                //Classifier.makeCopy(cls); // weka 3.7.0
            clsCopy.buildClassifier(randTrainingData);
            // evaluate classifier
            Evaluation eval = new Evaluation(randTrainingData);
            eval.evaluateModel(clsCopy, testData);
            evalAll.evaluateModel(clsCopy, testData);


            System.out.println("Run " + (i+1) + ", training data => " + Utils.classImbalanceOnWekaInstances(randTrainingData));
            System.out.println("Run " + (i+1) + ", test data => " + Utils.classImbalanceOnWekaInstances(testData));


            System.out.println("\n=== Run " + (i+1) + ", Classifier: " + clsCopy.getClass() + ",      AUROC: " + eval.areaUnderROC(0)
                    + ",      Weighted AUROC: " + eval.weightedAreaUnderROC()
                    + ",      AUPRC: " + eval.areaUnderPRC(0)
                    + ",      Weighted AUPRC: " + eval.weightedAreaUnderPRC()
                    + ",      Accuracy: " + accuracy(eval) + " ==== ");

            aurocs.add(eval.areaUnderROC(0));
            auprcs.add(eval.areaUnderPRC(0));

            System.out.println("=== Run " + (i+1) + ", Classifier: " + clsCopy.getClass() + ", Avg. AUROC: " + evalAll.areaUnderROC(0)
                    + ", Avg. Weighted AUROC: " + evalAll.weightedAreaUnderROC()
                    + ", Avg. AUPRC: " + evalAll.areaUnderPRC(0)
                    + ", Avg. Weighted AUPRC: " + evalAll.weightedAreaUnderPRC()
                    + ", Avg. Accuracy: " + accuracy(evalAll) +  " ==== \n");


            // output evaluation
            System.out.println();
            System.out.println(eval.toMatrixString("=== Confusion matrix for run " + (i+1) + "/" + runs + " ===\n"));
        } // for


        // output evaluation
        System.out.println();
        System.out.println(evalAll.toSummaryString("=== " + runs + "-runs Train-test ===", false));

        System.out.println("AUROC => " + evalAll.areaUnderROC(0));
        System.out.println("Weighted AUROC => " + evalAll.weightedAreaUnderROC());
        System.out.println("AUPRC => " + evalAll.areaUnderPRC(0));
        System.out.println("Weighted AUPRC => " + evalAll.weightedAreaUnderPRC());
        System.out.println("Accuracy => " + accuracy(evalAll));

        int tp, fn, fp, tn;
        tp = (int) format("#.", RoundingMode.HALF_UP, evalAll.numTruePositives(0) / runs);
        fn = (int) format("#.", RoundingMode.HALF_UP, evalAll.numFalseNegatives(0) / runs);
        fp = (int) format("#.", RoundingMode.HALF_UP, evalAll.numFalsePositives(0) / runs);
        tn = (int) format("#.", RoundingMode.HALF_UP, evalAll.numTrueNegatives(0) / runs);

        System.out.println("Recall => " + recall(tp, fn, fp, tn));
        System.out.println("tp, fn, fp, tn => " + tp + ", " + fn + ", " + fp + ", " + tn);
        System.out.println("Max AUROC in runs => " + max(aurocs));
        System.out.println("Max AUPRC in runs => " + max(auprcs));
        System.out.println("====================================================");

        // generate curve
        if(displayROCCurve) toROCCurve(evalAll);


        //not possible to get average model to test on set-c, not possible to get the best performing classifier (which already have a build classifier run on it)
        //if(testOnSeparateHoldOutSet)
        //{
        //} // if
    } // simpleTrainTestOverMultipleRuns


    //helper method to do simply train and test
    public static void simpleTrainTest(Classifier cls, Instances train, Instances test,  int seedToRandomizeTrainingData,
                                       boolean displayROCCurve //, Instances separateHoldOutSet, boolean testOnSeparateHoldOutSet
                                        ) throws Exception
    {
        Random rand = new Random(seedToRandomizeTrainingData);   // create seeded number generator
        Instances randTrainingData = new Instances(train);   // create copy of original data
        randTrainingData.randomize(rand);         // randomize data with number generator

        System.out.println("WEKA version: " + Version.VERSION);
        System.out.println("Train => " + Utils.classImbalanceOnWekaInstances(randTrainingData));
        System.out.println("Test => " + Utils.classImbalanceOnWekaInstances(test));
        //System.out.println(randTrainingData.toSummaryString());
        //System.out.println(test.toSummaryString());


        //make copy
        Classifier clsCopy = AbstractClassifier.makeCopy(cls);
                             //Classifier.makeCopy(cls);

        clsCopy.buildClassifier(randTrainingData); //(train);
        // evaluate the classifier and print some statistics
        Evaluation eval = new Evaluation(randTrainingData); //(train);
        eval.evaluateModel(clsCopy, test);
        //System.out.println(eval.toSummaryString("\nResults\n======\n", false));
        System.out.println(eval.toSummaryString());
        //println(eval.toCumulativeMarginDistributionString());
        System.out.println(eval.toMatrixString());
        //println(eval.toClassDetailsString());
        //positive class is at index 0
        //PredictivePerformanceEvaluator predEval
        //        = new PredictivePerformanceEvaluator(eval.numTruePositives(0), eval.numFalseNegatives(0),
        //        eval.numFalsePositives(0), eval.numTrueNegatives(0), new String[]{"1", "0"});
        //println(predEval.confusionMatrix());


        System.out.println("AUROC => " + eval.areaUnderROC(0));
        System.out.println("Weighted AUROC => " + eval.weightedAreaUnderROC());
        System.out.println("Accuracy => " + accuracy(eval));
        System.out.println("====================================================");

        // generate curve
        if(displayROCCurve) toROCCurve(eval);


//        //if testing on a separate data is requested
//        if(testOnSeparateHoldOutSet)
//        {
//            Random random = new Random(seedToRandomizeTrainingData);   // create seeded number generator
//            Instances randTrainData = new Instances(train);   // create copy of original data
//            randTrainData.randomize(random);         // randomize data with number generator
//
//            System.out.println("Train => " + Utils.classImbalanceOnWekaInstances(randTrainData));
//            System.out.println("Separate Hold Out Set => " + Utils.classImbalanceOnWekaInstances(separateHoldOutSet));
//
//            //make copy
//            Classifier clsCopy2 = AbstractClassifier.makeCopy(cls);
//                                //= Classifier.makeCopy(cls);
//
//            clsCopy2.buildClassifier(randTrainData); //(train);
//            // evaluate the classifier and print some statistics
//            Evaluation evaluation = new Evaluation(randTrainData);
//            evaluation.evaluateModel(clsCopy2, separateHoldOutSet);
//
//            System.out.println("Separate Hold Out Set, AUROC => " + evaluation.areaUnderROC(0));
//            System.out.println("Separate Hold Out Set, Weighted AUROC => " + evaluation.weightedAreaUnderROC());
//            System.out.println("Separate Hold Out Set, Accuracy => " + accuracy(evaluation));
//            System.out.println("====================================================");
//        } // if

    } // simpleTrainTest


    //TODO implement method to perform hyperparameter selection for classifier
    // write different hyperparam selection for each classifier class
    // use CVParamSelection from ParallelRadonMachine


    //helper method to generate ROC curve
    public static void toROCCurve(Evaluation eval)
    {
        try
        {
            // generate curve
            ThresholdCurve tc = new ThresholdCurve();
            int classIndex = 0;
            Instances result = tc.getCurve(eval.predictions(), classIndex);

            // plot curve
            ThresholdVisualizePanel vmc = new ThresholdVisualizePanel();
            vmc.setROCString("(Area under ROC = " +
                    weka.core.Utils.doubleToString(tc.getROCArea(result), 4) + ")");
            vmc.setName(result.relationName());
            PlotData2D tempd = new PlotData2D(result);
            tempd.setPlotName(result.relationName());
            tempd.addInstanceNumberAttribute();
            // specify which points are connected
            boolean[] cp = new boolean[result.numInstances()];
            for (int n = 1; n < cp.length; n++)
                cp[n] = true;
            tempd.setConnectPoints(cp);
            // add plot
            vmc.addPlot(tempd);

            // display curve
            String plotName = vmc.getName();
            final javax.swing.JFrame jf =
                    new javax.swing.JFrame("Weka Classifier Visualize: "+plotName);
            jf.setSize(500,400);
            jf.getContentPane().setLayout(new BorderLayout());
            jf.getContentPane().add(vmc, BorderLayout.CENTER);
            jf.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent e) {
                    jf.dispose();
                }
            });
            jf.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    } // toROCCurve


    //does not work as expected
    public static void visualizeData(Instances data) throws Exception
    {
        //data = toProp(data);


        // Set up a window for the plot and a Plot2D object
        JFrame jf = new javax.swing.JFrame("Visualize groups of instances using different glyphs");
        jf.setSize(500, 400);
        jf.getContentPane().setLayout(new java.awt.BorderLayout());
        Plot2D p2D = new weka.gui.visualize.Plot2D();
        jf.getContentPane().add(p2D, java.awt.BorderLayout.CENTER);

        //configure the plot
        PlotData2D pd1 = new weka.gui.visualize.PlotData2D(data);
        //ArrayList<Integer> shapeTypesForInstances = new ArrayList<Integer>(data.numInstances());
        FastVector shapeTypesForInstances = new FastVector(data.numInstances());
        //ArrayList<Integer> shapeSizesForInstances = new ArrayList<Integer>(data.numInstances());
        FastVector shapeSizesForInstances = new FastVector(data.numInstances());

        for (int index = 0; index < data.numInstances(); index ++)
        {
            Instance inst = data.instance(index);
            shapeTypesForInstances.add((int)inst.value(data.attribute("class").index())); // Change attribute giving group indicators here
            shapeSizesForInstances.add(3);
        }
//        int[] types = new int[shapeTypesForInstances.size()];
//        for(int index = 0; index < types.length; index ++)
//            types[index] = shapeTypesForInstances.get(index);
//        int[] sizes = new int[shapeSizesForInstances.size()];
//        for(int index =0; index < sizes.length; index ++)
//            sizes[index] = shapeSizesForInstances.get(index);

        pd1.setShapeType(shapeTypesForInstances);
        pd1.setShapeSize(shapeSizesForInstances);
        pd1.setCustomColour(java.awt.Color.BLACK);
        p2D.setMasterPlot(pd1);
        p2D.setXindex(data.attribute("patient_record_id").index()); // Change attribute name for X axis here
        p2D.setYindex(data.attribute("class").index()); // Change attribute name for Y axis here

        // Make plot visible
        jf.setVisible(true);
    } // visualizeData


    //shortcut method to generate the accuracy for the learned model
    public static double accuracy(Evaluation evaluation)
    {
        PredictivePerformanceEvaluator evaluator = new PredictivePerformanceEvaluator(evaluation.numTruePositives(0),
                evaluation.numFalseNegatives(0), evaluation.numFalsePositives(0),
                evaluation.numTrueNegatives(0), new String[]{"1", "0"});

        return evaluator.accuracy();
    } // accuracy


    public static double recall(int tp, int fn, int fp, int tn)
    {
        PredictivePerformanceEvaluator evaluator = new PredictivePerformanceEvaluator(tp, fn, fp, tn, new String[]{"1", "0"});
        return evaluator.recall();
    } // recall


    //helper method to print weights of instances of each bag
    public static void printInstanceWeightsOfABag(Instance bagInstance)
    {
        //recordId is at attribute 0
        //bagInstance.attribute(0).value(0) => obtains value from patient_record_id attribute in the dataset description, not from the instance
        System.out.println("Bag " + bagInstance.toString(bagInstance.attribute(0)) + "'s instance weights: ");
        Instances bag = bagInstance.relationalValue(1);
        for(int index = 0; index < bag.numInstances(); index ++)
        {
            Instance thisInnerInstance = bag.instance(index);
            System.out.println("The weight of instance: " + thisInnerInstance
                    + " => " + thisInnerInstance.weight());
        } // for
    } // printInstanceWeightsOfABag



    //helper method to print bag weights of multi-instance data
    public static void printBagWeights(Instances multiInstanceData, int from, int to)
    {
        if(from < 0 || from > multiInstanceData.numInstances() || to < 0 || to > multiInstanceData.numInstances() || from > to)
            throw new RuntimeException("Please correctly set from and to");

        multiInstanceData = new Instances(multiInstanceData, from, to);

        for(int index = 0; index < multiInstanceData.numInstances(); index ++)
        {
            Instance thisBag = multiInstanceData.instance(index);
            //thisBag.attribute(0).value(index) > obtains value from patient_record_id attribute in the dataset description, not from the instance
            System.out.println(thisBag.toString(thisBag.attribute(0)) + "'s Weight => "
                    + thisBag.weight());
        } // for
    } // printBagWeights


    //helper method to remove ts from the given data
    public static Instances removeTs(Instances data)
    {
        if(!data.attribute(1).relation().attribute(0).name().contains("ts"))
        {
            throw new RuntimeException("First attribute is not timestamp");
        } // if

        //take copy of the instance before manipulation
        data = new Instances(data);

        for(int index = 0; index < data.numInstances(); index ++)
        {
            //data.instance(index).attribute(0).value(0) => obtains value from patient_record_id attribute in the dataset description, not from the instance
            //System.out.println(data.instance(index).toString(bagInstance.attribute(0) + "'s Weight => " + data.instance(index).weight());
            //System.out.println(data.instance(index).toString(bagInstance.attribute(0) + "'s bag => "
            //        + data.instance(index).relationalValue(1));

            Instances bagOfThisBagInstance = data.instance(index).relationalValue(1);

            //remove ts minutes
            bagOfThisBagInstance.deleteAttributeAt(0);
            //System.out.println(bagOfThisBagInstance.numAttributes());
            //System.out.println("\n====== After deletion =====\n");
            //printWekaInstances(bagOfThisBagInstance);

            //break;
        } // for

        //remove attribute name from schema; (tsMinutes)
        //bag relational is at index 1, and index 0 of that relation is tsMinutes
        data.attribute(1).relation().deleteAttributeAt(0);

        return data;
    } // removeTs


    //helper method to reweight instances inside each bag by ts through keeping multiInstance format
    public static Instances reweightInstancesOfEachBagByTs(Instances data, boolean keepTsAsAVariable) throws Exception
    {
        //take copy of the instance before manipulation
        data = new Instances(data);


        for(int index = 0; index < data.numInstances(); index ++)
        {
            //data.instance(index).attribute(0).value(0) => obtains value from patient_record_id attribute in the dataset description, not from the instance
            //System.out.println(data.instance(index).toString(bagInstance.attribute(0) + "'s Weight => " + data.instance(index).weight());
            //System.out.println(data.instance(index).toString(bagInstance.attribute(0) + "'s bag => "
            //        + data.instance(index).relationalValue(1));

            Instances bagOfThisBagInstance = data.instance(index).relationalValue(1);

            double sumOfTsMinutes = 0;
            for(int innerIndex = 0; innerIndex < bagOfThisBagInstance.numInstances(); innerIndex ++)
            {
                Instance thisInnerInstance = bagOfThisBagInstance.instance(innerIndex);
                double timeStampOfThisInstance = thisInnerInstance.value(0); // ts is at index 0
                sumOfTsMinutes += timeStampOfThisInstance;
            }


            //System.out.println("\n ===== Before reweighting =====\n");
            //for(int innerIndex = 0; innerIndex < bagOfThisBagInstance.numInstances(); innerIndex ++)
            //{
            //    Instance thisInnerInstance = bagOfThisBagInstance.instance(innerIndex);
                //System.out.println("The weight of instance with ts: " + thisInnerInstance.value(0)
                //                                    + " => " + thisInnerInstance.weight());
                //System.out.println(thisInnerInstance);
            //} // for


            for(int innerIndex = 0; innerIndex < bagOfThisBagInstance.numInstances(); innerIndex ++)
            {
                Instance thisInnerInstance = bagOfThisBagInstance.instance(innerIndex);
                double timeStampOfThisInstance = thisInnerInstance.value(0); // ts is at index 0
                thisInnerInstance.setWeight( (timeStampOfThisInstance / sumOfTsMinutes) ); // + (1.0 / bagOfThisBagInstance.numInstances()) decreases performance
                                                                                            // + 1 decreases performance
            } // for


            //System.out.println("\n ===== After reweighting =====\n");
            //for(int innerIndex = 0; innerIndex < bagOfThisBagInstance.numInstances(); innerIndex ++)
            //{
            //    Instance thisInnerInstance = bagOfThisBagInstance.instance(innerIndex);
                //System.out.println("The weight of instance with ts: " + thisInnerInstance.value(0)
                //        + " => " + thisInnerInstance.weight());
                //System.out.println(thisInnerInstance);
            //} // for


            //remove ts minutes
            if(!keepTsAsAVariable)
                bagOfThisBagInstance.deleteAttributeAt(0);
            //System.out.println(bagOfThisBagInstance.numAttributes());
            //System.out.println("\n====== After deletion =====\n");
            //printWekaInstances(bagOfThisBagInstance);

            //break;
        } // for

        //remove attribute name from schema; (tsMinutes)
        //bag relational is at index 1, and index 0 of that relation is tsMinutes
        if(!keepTsAsAVariable)
            data.attribute(1).relation().deleteAttributeAt(0);

        //System.out.println(new Instances(data, 0, 100));

        return data;
    } // reweightInstancesOfEachBagByTs


    //helper method to reweight instances obtained by multi-instance filter
    public static Instances transformMIDataToProp(Instances miData, boolean removeTs, boolean keepPropFormat, boolean reweightByTs) throws Exception
    {
        if (miData.numInstances() == 0 || miData.instance(0).relationalValue(1) == null)
            throw new RuntimeException("Given data is either empty or is not multi-instance data");

        //apply mi to prop
        MultiInstanceToPropositional miToProp = new MultiInstanceToPropositional();
        //-A <num>
        //  The type of weight setting for each prop. instance:
        //0.weight = original single bag weight /Total number of
        //prop. instance in the corresponding bag;
        //1.weight = 1.0;
        //2.weight = 1.0/Total number of prop. instance in the
        //corresponding bag;
        //3. weight = Total number of prop. instance / (Total number
        //of bags * Total number of prop. instance in the
        //corresponding bag).
        //(default:0)
        miToProp.setOptions(weka.core.Utils.splitOptions("-A 1"));
        miToProp.setInputFormat(miData);
        Instances newData = Filter.useFilter(miData, miToProp);
        //System.out.println("After applying miToProp filter newData => " + Utils.classImbalanceOnWekaInstances(newData));
        //System.out.println(newData);

        if(reweightByTs)
        {
            LinkedHashMap<String, List<Instance>> bagIdInstanceMap = new LinkedHashMap<>();

            for(int index = 0; index < newData.numInstances(); index ++)
            {
                Instance thisPropInstance = newData.instance(index);
                String bagId = thisPropInstance.toString(thisPropInstance.attribute(0));

                //if the bagId is not there, create a new list and apply there
                if(!bagIdInstanceMap.containsKey(bagId))
                {
                    List<Instance> instanceList = new ArrayList<>();
                    instanceList.add(thisPropInstance);
                    bagIdInstanceMap.put(bagId, instanceList);
                } // if
                else
                {
                    bagIdInstanceMap.get(bagId).add(thisPropInstance);
                } // else

                //System.out.println(bagId + ", Ts: " + thisPropInstance.toString(thisPropInstance.attribute(1)) + " => " + thisPropInstance.toString());
            } // for


            //for each bagId, reweight the instances by their ts
            for(String bagId : bagIdInstanceMap.keySet())
            {
                //compute the sum of timestampMinutes
                List<Instance> instanceList = bagIdInstanceMap.get(bagId);
                double sumOfTsMinutes = 0;
                for(Instance instance : instanceList)
                    sumOfTsMinutes += Double.parseDouble(instance.toString(instance.attribute(1)));

                //now reweight
                for(Instance instance : instanceList)
                {
                    double tsMinutes = Double.parseDouble(instance.toString(instance.attribute(1)));
                    double weight = tsMinutes / sumOfTsMinutes;
                    instance.setWeight(weight); //TODO disabled +1 works in 3.7.0 and 3.7.2; weightedInstanceHandlers cannot handle weighted data interestingly in weka 3.9.4
                } // for

                //instanceList.forEach(System.out::println);
            } // for each bagId
        } // if

        //System.exit(0);


        //ts should be removed before bag_id attribute
        if(removeTs)
        {
            if (!newData.attribute(1).relation().attribute(0).name().contains("ts"))
            {
                System.out.println("There is no ts attribute, not removing ts");
            } // if
            else
                newData.deleteAttributeAt(1);
        }

        if(!keepPropFormat) // attribute 0 is a bag_id attribute (contains all bag_ids in {})
        {
            //remove bag_id attribute
            newData.deleteAttributeAt(0);
            newData = buildInstances(newData); // build instances one by one to be in mono-instance format
        }

        return newData;
    } // transformMiDataToProp



    //helper method to impute the file content and write it as a new file
    public static void imputeAndWrite(String DATA_FOLDER, List<MTSE> mtses, Imputations.ImputeMethod imputeMethod, int setASize, int setBSize, VarRanges varRanges,
                                      double currentMissingValuePlaceHolder)
    {
        long start = System.currentTimeMillis();

        Imputations imputations = Imputations.getInstance();
        List<MTSE> imtses = imputations.impute(mtses, imputeMethod, new int[]{0, setASize}, varRanges, currentMissingValuePlaceHolder);

        long end = System.currentTimeMillis();
        System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(end-start) + " seconds for imputation");


        for(int index = 0; index < imtses.size(); index ++)
        {
            MTSE imtse = imtses.get(index);
            //println(imtse.toVerticalString());
            //println(imtse.toVerticalMaskingVectorString());
            //println(imtse.toCSVString(true));
            //break;

            if(index < setASize)
                imtse.writeToFile(DATA_FOLDER, imputeMethod.toActionString() + "_set-a", "csv", true);
            else if(index < setASize + setBSize)
                imtse.writeToFile(DATA_FOLDER, imputeMethod.toActionString() + "_set-b", "csv", true);
            else
                imtse.writeToFile(DATA_FOLDER, imputeMethod.toActionString() + "_set-c", "csv", true);
        } // for
    } // imputeAndWrite







    //transform List<LabeledPoint> to Instances
    private static Instances buildInstances(Instances propData)
    {
        //create an empty arraylist first
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();

        // for one labeledPoint in a partition
        // do get feature vector and create attributes from them

        for (int i = 0; i < propData.numAttributes() - 1; i++)
        {
            // attribute name will be x1, x2, x3 etc...
            Attribute attribute = new Attribute(propData.attribute(i).name().replace("bag_", ""));
            attributes.add(attribute);
        } // for

        // Declare the class attribute along with its values
        FastVector fvClassVal = new FastVector(2);
        fvClassVal.addElement("1");
        fvClassVal.addElement("0");
        Attribute label = new Attribute("class", fvClassVal);

        // Declare the feature vector, first add class label attribute
        FastVector fvWekaAttributes = new FastVector(4);
        //then for each attribute in an attributes add them to wekaAttributes
        for (Attribute attribute : attributes)
        {
            fvWekaAttributes.addElement(attribute);
        } // for
        fvWekaAttributes.addElement(label);


        // Create an empty training set
        Instances newData = new Instances("new_relation" //propData.relationName()
                , fvWekaAttributes, 10);
        // Set class index
        newData.setClassIndex(fvWekaAttributes.size() - 1);


        //for each labeledPoint in partition, create an instance
        //from that labeled point
        for (int i = 0; i < propData.numInstances(); i++)
        {
            Instance thisPropInstance = propData.instance(i);

            // Create the instance, number of attributes will be #features + label
            Instance instance = //new Instance(attributes.size() + 1); // weka 3.7.0
                                new DenseInstance(attributes.size() + 1);

            //class label of labeled point
            double lbl = thisPropInstance.classValue();

            //first set class label for the attribute
            instance.setValue((Attribute)fvWekaAttributes.elementAt(fvWekaAttributes.size() - 1), lbl);


            for (int index = 0; index < thisPropInstance.numAttributes() - 1; index++)
            {
                instance.setValue((Attribute) fvWekaAttributes
                        .elementAt(index), Double.parseDouble(thisPropInstance.toString(thisPropInstance.attribute(index))));

                instance.setWeight(thisPropInstance.weight());
            } // for

            // add the instance
            newData.add(instance);
        } // for

						 /*instance.setValue((Attribute)fvWekaAttributes.elementAt(0), 1.0);
						 instance.setValue((Attribute)fvWekaAttributes.elementAt(1), 0.5);
						 instance.setValue((Attribute)fvWekaAttributes.elementAt(2), "gray");
						 instance.setValue((Attribute)fvWekaAttributes.elementAt(3), "positive");*/

        return newData;
    } // buildInstances




    //transform List<LabeledPoint> to Instances
    private static Instances buildInstances(Instances propData, HashSet<Integer> instanceIndicesToDelete, HashSet<String> instanceToRemoveByBagID)
    {
        //create an empty arraylist first
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();

        // for one labeledPoint in a partition
        // do get feature vector and create attributes from them

//        List<String> attrValues = new ArrayList<>();
//        for(int index = 0; index < propData.attribute(0).numValues(); index++)
//        {
//            String bagId = propData.attribute(0).value(index);
//            //if(!instanceToRemoveByBagID.contains(bagId))
//                attrValues.add(bagId);
//        }

        //weka 3.7.0
        FastVector fAttrValues = new FastVector();
        for(int index = 0; index < propData.attribute(0).numValues(); index++)
        {
            String bagId = propData.attribute(0).value(index);
            //if(!instanceToRemoveByBagID.contains(bagId))
            fAttrValues.addElement(bagId);
        }


        Attribute attr = new Attribute(propData.attribute(0).name(), fAttrValues, propData.attribute(0).getMetadata());
        attributes.add(attr);
        for (int i = 1; i < propData.numAttributes() - 1; i++)
        {
            // attribute name will be x1, x2, x3 etc...
            Attribute attribute = new Attribute(propData.attribute(i).name()//.replace("bag_", "")
                                );
            attributes.add(attribute);
        } // for

        // Declare the class attribute along with its values
        FastVector fvClassVal = new FastVector(2);
        fvClassVal.addElement("1");
        fvClassVal.addElement("0");
        Attribute label = new Attribute("class", fvClassVal);

        // Declare the feature vector, first add class label attribute
        FastVector fvWekaAttributes = new FastVector(4);
        //then for each attribute in an attributes add them to wekaAttributes
        for (Attribute attribute : attributes)
        {
            fvWekaAttributes.addElement(attribute);
        } // for
        fvWekaAttributes.addElement(label);


        // Create an empty training set
        Instances newData = new Instances(//"new_relation"
                                                propData.relationName()
                                , fvWekaAttributes, 10);
        // Set class index
        newData.setClassIndex(fvWekaAttributes.size() - 1);


        //for each labeledPoint in partition, create an instance
        //from that labeled point
        for (int i = 0; i < propData.numInstances(); i++)
        {
            Instance thisPropInstance = propData.instance(i);

            if(!(instanceIndicesToDelete == null || instanceIndicesToDelete.isEmpty()) && instanceIndicesToDelete.contains(i))
                continue; // do not add this intance


            String bagId = thisPropInstance.toString(thisPropInstance.attribute(0));
            if(!(instanceToRemoveByBagID == null || instanceToRemoveByBagID.isEmpty()) && instanceToRemoveByBagID.contains(bagId))
                continue; // do not add this instance

            // Create the instance, number of attributes will be #features + label
            Instance instance = //new Instance(attributes.size() + 1); // weka 3.7.0
                                new DenseInstance(attributes.size() + 1);

            //class label of labeled point
            double lbl = thisPropInstance.classValue();

            //first set class label for the attribute
            instance.setValue((Attribute)fvWekaAttributes.elementAt(fvWekaAttributes.size() - 1), lbl);

            instance.setValue((Attribute) fvWekaAttributes
                    .elementAt(0), thisPropInstance.toString(thisPropInstance.attribute(0)));

            for (int index = 1; index < thisPropInstance.numAttributes() - 1; index++)
            {
                instance.setValue((Attribute) fvWekaAttributes
                        .elementAt(index), Double.parseDouble(thisPropInstance.toString(thisPropInstance.attribute(index))));

                instance.setWeight(thisPropInstance.weight());
            } // for

            // add the instance
            newData.add(instance);
        } // for

						 /*instance.setValue((Attribute)fvWekaAttributes.elementAt(0), 1.0);
						 instance.setValue((Attribute)fvWekaAttributes.elementAt(1), 0.5);
						 instance.setValue((Attribute)fvWekaAttributes.elementAt(2), "gray");
						 instance.setValue((Attribute)fvWekaAttributes.elementAt(3), "positive");*/

        return newData;
    } // buildInstances



    //helper method to demo MIBoost bag weighting
    public static void demoMIBoostWeighting(Instances data)
    {
        for(int index = 0; index < data.numInstances(); index ++)
        {
            System.out.println(data.instance(index).attribute(0).value(index) + "'s Weight => " + data.instance(index).weight());
        } // for

        //Initialize the bags' weights
        double N = (double)data.numInstances(), sumNi=0;
        for(int i=0; i<N; i++)
            sumNi += data.instance(i).relationalValue(1).numInstances();
        for(int i=0; i<N; i++){
            data.instance(i).setWeight(sumNi/N);
        }

        System.out.println("================ AFTER REWEIGHTING =================");
        for(int index = 0; index < data.numInstances(); index ++)
        {
            System.out.println(data.instance(index).attribute(0).value(index) + "'s Weight => " + data.instance(index).weight());
        } // for
    } // demoMIBoostWeighting


    //helper method to find class imbalance in weka instances
    public static String classImbalanceOnWekaInstances(Instances data)
    {
        int numPositiveInstances = 0;
        int numNegativeInstances = 0;

        //for each instance check its class and update counters
        for(int index = 0; index < data.numInstances(); index ++)
        {
            Instance thisInstance = data.instance(index);
            if(Double.compare(thisInstance.value(data.classIndex()), 0.0) == 0)
                numPositiveInstances ++;
            else
                numNegativeInstances ++;
        } // for

        return "numPositives: " + numPositiveInstances + ", numNegatives: " + numNegativeInstances + ", Imbalance: "
                + format("#.##", RoundingMode.HALF_UP, 100 * numPositiveInstances / (data.numInstances() * 1.0))
                + "% - " +  format("#.##", RoundingMode.HALF_UP,100 * numNegativeInstances / (data.numInstances() * 1.0)) + "%";
    } // classImbalanceOnWekaInstances



    //helper method to undersample majority class to the size of minority class (50% - 50% imbalance)
    public static Instances underSample(Instances train, boolean verbose) throws Exception
    {
        //Undersample majority class
        //Instead of using weka.filters.supervised.instance.Resample,
        // a much easier way to achieve the same effect is to use weka.filters.supervised.SpreadSubsample instead, with distributionSpread=1.0:
        String[] filterOptions = new String[2];
        filterOptions[0] = "-M";                                               // "distributionSpread"
        filterOptions[1] = "1.0";    //1.0 for 50%-50%, 1.5 for 40%-60%, 2.0 for 1/3, 2/3, 2.333 for 30%-70%
        SpreadSubsample underSampleFilter = new SpreadSubsample();              // a new instance of filter
        underSampleFilter.setOptions(filterOptions);                           // set options
        if(verbose) System.out.println(underSampleFilter.getClass().getName() + " filter options: " + Arrays.toString(underSampleFilter.getOptions()));
        underSampleFilter.setInputFormat(train);                                // inform the filter about the dataset **AFTER** setting options
        train = Filter.useFilter(train, underSampleFilter);         // apply filter
        if(verbose) System.out.println("After undersampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        return train;
    } // underSample


    //helper method to oversample minority class roughly to the size of majority class
    public static Instances overSample(Instances train) throws Exception
    {
        //Weka code
        //int sampleSize = (int)((m_SampleSizePercent / 100.0) * ((1 - m_BiasToUniformClass) * numInstancesPerClass[i] +
        //        m_BiasToUniformClass * data.numInstances() / numActualClasses));

        int numPositiveInstances = 0;
        int numNegativeInstances = 0;

        //for each instance check its class and update counters
        for(int index = 0; index < train.numInstances(); index ++)
        {
            Instance thisInstance = train.instance(index);
            String bagId = thisInstance.toString(thisInstance.attribute(0));
            //System.out.println(bagId);

            if(Double.compare(thisInstance.value(train.classIndex()), 0.0) == 0)
            {
                numPositiveInstances++;
            }
            else
            {
                numNegativeInstances++;
            }
        } // for


        int bigger = Math.max(numPositiveInstances, numNegativeInstances);

        double sampleSizePercent = (bigger / (train.numInstances() * 1.0)) * 100 * 2; // multiple by 100 for percent, 2 for Y/2

        //oversample
        train = reSample(sampleSizePercent, train);

        System.out.println("After oversampling Train => " + Utils.classImbalanceOnWekaInstances(train));
        return train;
    } // overSample


    //TODO dropping performance, might be a bug of Weka.
    //helper method to oversample the minority class to the size of majority class
    public static Instances overSampleByKeepingMajorityClassUntouched(Instances train) throws Exception
    {
        //Weka code
        //int sampleSize = (int)((m_SampleSizePercent / 100.0) * ((1 - m_BiasToUniformClass) * numInstancesPerClass[i] +
        //        m_BiasToUniformClass * data.numInstances() / numActualClasses));


        int numPositiveInstances = 0;
        int numNegativeInstances = 0;

        Instances positives = new Instances(train); positives.delete();
        Instances negatives = new Instances(train); negatives.delete();
        //HashSet<String> negativeBagIds = new HashSet<>();
        //HashSet<String> positiveBagIds = new HashSet<>();
        //List<Integer> negativeIndices = new ArrayList<>();
        //HashSet<Integer> positiveIndices = new HashSet<>();

        //for each instance check its class and update counters
        for(int index = 0; index < train.numInstances(); index ++)
        {
            Instance thisInstance = train.instance(index);
            String bagId = thisInstance.toString(thisInstance.attribute(0));
            //System.out.println(bagId);

            if(Double.compare(thisInstance.value(train.classIndex()), 0.0) == 0)
            {
                //negatives.remove(thisInstance);
                positives.add(thisInstance);
                //positiveBagIds.add(bagId);
                numPositiveInstances++;
                //positiveIndices.add(index);
            }
            else
            {
                //positives.remove(thisInstance);
                negatives.add(thisInstance);
                //negativeBagIds.add(bagId);
                //negativeIndices.add(index);
                numNegativeInstances++;
            }
        } // for

        //Error:
        //for(int index : positiveIndices)
        //    train.delete(index);

//        Iterator<Instance> itr = train.iterator();
//        int count = 0;
//        while (itr.hasNext())
//        {
//            if(positiveIndices.contains(count))
//                itr.remove();
//            count ++;
//        }
//
//        System.out.println(train.numInstances());
//        System.exit(0);



//        //first negatives then positives
//        train.sort(new Comparator<Instance>() {
//            @Override
//            public int compare(Instance t1, Instance t2)
//            {
//                return Double.compare(t1.value(train.classIndex()), t2.value(train.classIndex()));
//            }
//        });
//
//
//        Instances miPositives = new Instances(train, 0, numPositiveInstances);
//        //miPositives.forEach(instance -> System.out.println(instance.value(instance.classIndex())));
//        Instances miNegatives = new Instances(train, numPositiveInstances, numNegativeInstances);
//        System.out.println("NumPositives: " + miPositives.numInstances());
//        System.out.println("NumNegatives : " + miNegatives.numInstances());
////        //System.exit(0);


//        // handle the case, when negative is a minority class
//        Instances propPositives = toProp(positives);
//        propPositives = buildInstances(propPositives, null, negativeBagIds);
//        //System.out.println(propPositives);
//        Instances propNegatives = toProp(negatives);
//        propNegatives = buildInstances(propNegatives, null, positiveBagIds);
//        //System.out.println(propNegatives);
//
//        Instances miPositives = toMi(propPositives);
//        Instances miNegatives = toMi(propNegatives);
//        System.out.println("NumPositives: " + miPositives.numInstances());
//        System.out.println("NumNegatives : " + miNegatives.numInstances());
//        //System.exit(0);



        Instances miPositives = positives;
        Instances miNegatives = negatives;
        System.out.println("NumPositives: " + miPositives.numInstances());
        System.out.println("NumNegatives : " + miNegatives.numInstances());


        int bigger = Math.max(numPositiveInstances, numNegativeInstances);

        double sampleSizePercent = (bigger / (
                            //train.numInstances()
                            miPositives.numInstances()
                            * 1.0)) * 100
                            ; // for untouched majority class
                            //* 2; // multiple by 100 for percent, 2 for Y/2


        //System.out.println((sampleSizePercent / 100) * (1.0 * train.numInstances() / train.numClasses()));
        //System.exit(0);

        //oversample
        miPositives = reSample(sampleSizePercent, miPositives);


        System.out.println("After oversampling Train (before adding negatives) => " + Utils.classImbalanceOnWekaInstances(miPositives));


        //causing error, it might be because, miPositives are oversampled instances
//        for(int index = 0; index < miNegatives.numInstances(); index ++)
//            miPositives.add(miNegatives.instance(index));

        //works when positives are added on top of negatives
        for(int index = 0; index < miPositives.numInstances(); index ++)
            miNegatives.add(miPositives.instance(index));
        miPositives = miNegatives;
        System.out.println("After adding negatives => " + Utils.classImbalanceOnWekaInstances(miPositives));


//        System.out.println(new Instances(miPositives, 0, 10));
//        miPositives = toProp(miPositives);
//        miPositives = toMi(miPositives);


        //set the weight of each bag to 1
        for(int index = 0; index < miPositives.numInstances(); index++)
            miPositives.instance(index).setWeight(1);


        List<String> attrValues = new ArrayList<>();
        for(int index = 0; index < miPositives.attribute(0).numValues(); index++)
        {
            String bagId = miPositives.attribute(0).value(index);
            //if(!instanceToRemoveByBagID.contains(bagId))
            attrValues.add(bagId);
        }
        System.out.println("New number of bags: " + attrValues.size());
        System.out.println("Num instances: " + miPositives.numInstances());


        System.out.println("After oversampling Train => " + Utils.classImbalanceOnWekaInstances(miPositives));

        return miPositives;
    } // overSampleByKeepingMajorityClassUntouched


    //method to resample the given data
    private static Instances reSample(double sampleSizePercent, Instances data) throws Exception
    {
        //To oversample the minority class so that both classes have the same number of instances,
        // use the supervised Resample filter with noReplacement=false, biasToUniformClass=1.0,
        // and sampleSizePercent=Y, where Y/2 is (approximately) the percentage of data that belongs to the majority class.
        Resample filter = new Resample();
        filter.setNoReplacement(false);
        filter.setBiasToUniformClass(1);
        filter.setSampleSizePercent(sampleSizePercent);
        System.out.println("ReSample Filter options: " + Arrays.toString(filter.getOptions()));
        filter.setInputFormat(data);
        return Filter.useFilter(data, filter);        // apply filter
    } // reSample



    //helper method to convert mi data to prop
    public static Instances toProp(Instances miData) throws Exception
    {
        //apply mi to prop
        MultiInstanceToPropositional miToProp = new MultiInstanceToPropositional();
        //-A <num>
        //  The type of weight setting for each prop. instance:
        //0.weight = original single bag weight /Total number of
        //prop. instance in the corresponding bag;
        //1.weight = 1.0;
        //2.weight = 1.0/Total number of prop. instance in the
        //corresponding bag;
        //3. weight = Total number of prop. instance / (Total number
        //of bags * Total number of prop. instance in the
        //corresponding bag).
        //(default:0)
        //NONE OF OPTIONS KEEP INNER BAG INSTANCE WEIGHTING => MODIFY WEKA SOURCE CODE
        miToProp.setOptions(weka.core.Utils.splitOptions("-A 1")); // keep the original weighting
        miToProp.setInputFormat(miData);
        Instances propData = Filter.useFilter(miData, miToProp);

        return propData;
    } // toProp


    public static Instances toMi(Instances propData) throws Exception
    {
        //THE INNER BAG INSTANCE WEIGHTS SHOULD BE STILL BE KEPT HERE
        PropositionalToMultiInstance propToMi = new PropositionalToMultiInstance();
        propToMi.setDoNotWeightBags(true);
        //propToMi.setOptions(weka.core.Utils.splitOptions("-no-weights")); // weka 3.7.0
        propToMi.setInputFormat(propData);
        Instances miData = Filter.useFilter(propData, propToMi);
        return miData;
    } // toMi


    //helper method to oversample with smote
    //TODO SMOTE is not available for MI data giving null-pointer exception on distance calculation.
    public static Instances smote(Instances train, boolean verbose) throws Exception
    {
        //Instances propTrain = toProp(train);

        SMOTE smote = new SMOTE();
        if(verbose) System.out.println("Filter options: " + Arrays.toString(smote.getOptions()));
        smote.setInputFormat(train);
        train = Filter.useFilter(train, smote);

        //Instances miTrain = toMi(propTrain);

        if(verbose) System.out.println("After oversampling Train => " + Utils.classImbalanceOnWekaInstances(train));

        return train;
    } // smote


    //helper method to obtain number of available processers for parallel run
    public static int numCores()
    {
        return Runtime.getRuntime().availableProcessors();
    } // numCores



    public static Date toDate(String dateString, String dateFormatPattern) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(dateFormatPattern, Locale.ENGLISH);
        return dateFormat.parse(dateString);
    } // toDate


    //toDateStringWithoutTime is used to remove time from the date
    public static String toDateStringWithoutTime(Date date) {
        return date.toString().replaceAll("(\\d\\d:){2}\\d\\d\\s", "");
    } // toDateStringWithoutTime


    //date string without day month year
    public static String toDateStringWithoutDayMonthYear(Date date) {
        SimpleDateFormat sdf =
                new SimpleDateFormat("HH:mm:ss zzz"); //("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
        return sdf.format(date);
    } // toDateStringWithoutDayMonthYear


    //"yyyy-MM-dd HH:mm:ss"
    public static String toDateString(long millisecondsFromUnixEpoch, String dateFormatPattern)
    {
        DateFormat dateFormat = new SimpleDateFormat(dateFormatPattern, Locale.ENGLISH);
        Date date = new Date(millisecondsFromUnixEpoch);
        return dateFormat.format(date);
    } //toDateString


    //helper method to move files from one directory to the other
    public static void moveFiles(List<String> recordIDs, String folderLocation, String newFolderName) throws Exception
    {
        Set<String> recodsIDsSet = new HashSet<>(recordIDs);

        List<String> filePaths = Utils.listFilesFromLocalPath(folderLocation, false);
        for(String filePath : filePaths)
        {
            String fileName = fileNameFromPath(filePath);

            if(recodsIDsSet.contains(fileName.replace(".txt", "")))
            {
                String newLocation = folderLocation + File.separator + newFolderName + File.separator + fileName;
                Path temp = Files.move
                        (Paths.get(filePath),
                                Paths.get(newLocation));

                if (temp != null) {
                    System.out.println(fileName + " renamed and moved successfully");
                } else {
                    System.out.println("Failed to move the file");
                }
            }
        } // for
    } // moveFiles



    //helper method to find average auroc, auprc of brits and gru_d and m_rnn
    public static void findAvgAUROC_AUPRC(String filePath)
    {
        //obtain file contents
        String fileContents = fileContentsFromLocalFilePath(filePath);
        //split the lines
        String[] lines = StringUtils.split(fileContents, "\r\n|\r|\n");

        List<Double> aurocs = new ArrayList<>();
        List<Double> auprcs = new ArrayList<>();
        //for each line
        for(String line : lines)
        {
            if(line.contains("AUROC"))
            {
                String[] splits = line.split("\\(");
                String auroc_auprc = splits[1];

                splits = auroc_auprc.split(",");
                String auroc = splits[0];
                String auprc = splits[1];

                auroc = auroc.trim().replace("\'", "").replace("AUROC", "").trim();
                auprc = auprc.trim().replace("\'", "").replace("AUPRC", "").replace(")", "").trim();

                aurocs.add(Double.parseDouble(auroc));
                auprcs.add(Double.parseDouble(auprc));
            } // if
        } // for

        System.out.println("#aurocs: " + aurocs.size() + ", #aurprcs: " + auprcs.size());
        System.out.println("Max AUROC: " + max(aurocs) + ", Max AUPRC: " + max(auprcs));
        System.out.println("Last AUROC: " + aurocs.get(aurocs.size()  - 1) + ", Last AUPRC: " + auprcs.get(auprcs.size() - 1));
        System.out.println(fileNameFromPath(filePath) + " => Avg. AUROC: " + mean(aurocs, false) + ", Avg. AUPRC: " + mean(auprcs, false) + "\n");
    } // findAvgAUROC_AUPRC


    //helper method to update outcomes for brits data
    public static void updateOutcomes(Dataset dataset, List<String> outcomesFilePaths)
    {
        Outcomes outcomes = Outcomes.getInstance(dataset);

        System.out.println("Updating outcomes...");

        //for each outcome file update outcomes
        for(String filePath : outcomesFilePaths)
        {
            //obtain file contents
            String fileContents = fileContentsFromLocalFilePath(filePath);
            //split the lines
            String[] lines = StringUtils.split(fileContents, "\r\n|\r|\n");

            //each line contains record id, and in hospital death 0 or 1 separated by comma
            for(String line : lines)
            {
                String[] lineComponents = line.split(",");
                int recordID = Integer.parseInt(lineComponents[0]);
                int inHospitalDeath0Or1 = (int) Double.parseDouble(lineComponents[1]);
                Outcome outcome = new Outcome(recordID, 0, 0, 0, 0, inHospitalDeath0Or1);
                outcomes.add(recordID, outcome);
            } // for
        } // for

        System.out.println("Outcomes updated...");

    } // updateOutcomes



    //2nd experimental setup all brits, grud, mrnn configurations
    public static void secondSetupRNNAll(boolean standardize)
    {
        String[] methods = new String[] {"brits", "m_rnn", "gru_d"};

        TransformMethod[] transformMethods = new TransformMethod[]{
                TransformMethod.TRANSFORM_SUPER_13, TransformMethod.TRANSFORM_BEST, TransformMethod.TRANSFORM_FLAT};


        for (String method : methods)
        {
            System.out.println("Data obtained by " + method.toUpperCase() + " method...");

            //update outcomes first
            List<String> newOutcomeFilePaths = Utils.listFilesFromLocalPath("./" + method + "_set_a/outcomes", false);
            newOutcomeFilePaths.addAll(Utils.listFilesFromLocalPath("./"+ method + "_set_b/outcomes", false));
            Utils.updateOutcomes(Dataset.PhysioNet, newOutcomeFilePaths);


            //list files in the local file path
            List<String> setAFilePaths //= Utils.listFilesFromLocalPath(PHYSIONET_SET_A_DIR_PATH, false);
                                         = Utils.listFilesFromLocalPath("./" + method + "_set_a", false);
            List<String> setBFilePaths //= Utils.listFilesFromLocalPath(PHYSIONET_SET_B_DIR_PATH, false);
                                         = Utils.listFilesFromLocalPath("./" + method + "_set_b", false);
            //List<String> setCFilePaths = Utils.listFilesFromLocalPath(PHYSIONET_SET_C_DIR_PATH, false);

            List<String> allFilePaths = new ArrayList<>();
            allFilePaths.addAll(setAFilePaths);
            allFilePaths.addAll(setBFilePaths);
            //allFilePaths.addAll(setCFilePaths);


            //brits, gru_d, m_rnn files
            Map<String, String> filePathFileContentsMap = new LinkedHashMap<>();
            for(String filePath : allFilePaths)
            {
                String fileContents = Utils.fileContentsFromLocalFilePath(filePath);
                filePathFileContentsMap.put(filePath, fileContents);
            } // for



            long start = System.currentTimeMillis();
            double missingValuePlaceHolder = -2.0;
            List<MTSE> mtses = new ArrayList<>();

            //TODO parallelStream API to read mtses from file
            //obtain multivariate time series from each file
            for(String filePath : filePathFileContentsMap.keySet())
            {
                String fileName = Utils.fileNameFromPath(filePath);
                String fileContents = filePathFileContentsMap.get(filePath);

                //create multivariate time series object from each file's contents
                MTSE mtse
                        = MTSE.fromFile(Dataset.PhysioNet, fileName, fileContents, ",", missingValuePlaceHolder);
                //println(mtse.toVerticalString());

                mtses.add(mtse);
                //break;
            } // for
            //System.exit(0);
            long end = System.currentTimeMillis();
            System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(end-start) + " seconds for reading all mtses");


            //clear memory
            filePathFileContentsMap.clear();
            filePathFileContentsMap = null;


            //run all experiments
            try
            {
                //sort mtses by record id
                List<MTSE> train = new ArrayList<>();
                for(int index = 0; index < setAFilePaths.size(); index ++)
                    train.add(mtses.get(index));
                Collections.sort(train); // sorts by record id
                List<MTSE> test = new ArrayList<>();
                for(int index = setAFilePaths.size(); index < setAFilePaths.size() + setBFilePaths.size(); index ++)
                    test.add(mtses.get(index));
                Collections.sort(test); // sorts by record id
                train.addAll(test); // keeps insertion order
                mtses = train; // now train and test data are individually sorted by record id





                // data already imputed by brits, m_rnn or gru_d





                for(TransformMethod transformMethod : transformMethods)
                {
                    long startProg = System.currentTimeMillis();


                    List<MTSE> tMtses
                            = Utils.transformMtses(mtses, setAFilePaths.size(), //setCFilePaths.size(),
                            transformMethod);


                    Utils.mtsesToMIArffData(Dataset.PhysioNet, tMtses.subList(0, setAFilePaths.size()), Outcomes.getInstance(Dataset.PhysioNet),
                            method + "_set_a" + File.separator + transformMethod.simpleName(), "patient_record_id",
                            new String[]{"1", "0"}, false, "set_a");

                    Utils.mtsesToMIArffData(Dataset.PhysioNet, tMtses.subList(setAFilePaths.size(),
                            setAFilePaths.size() + setBFilePaths.size()), Outcomes.getInstance(Dataset.PhysioNet),
                            method + "_set_b" + File.separator + transformMethod.simpleName(), "patient_record_id",
                            new String[]{"1", "0"}, false, "set_b");



                    AttributeTransformFilter atf = standardize ? AttributeTransformFilter.STANDARDIZE : AttributeTransformFilter.NONE;

                    // running with 100 trees
                    Experiments.run_RF(method, transformMethod.simpleName(),
                                atf, ImbalanceHandler.NONE);


                    long endProg = System.currentTimeMillis();
                    System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(endProg-startProg) + " seconds for execution");
                    System.out.println("====================================================");
                }

            } catch (Exception e)
            {
                e.printStackTrace();
            }


        } // for

    } // secondSetupRNNAll



    //2nd experimental setup all mean, forward configurations
    public static void secondSetupMeanForwardAll(boolean standardize //, boolean undersample_50_50
                                                )
    {
        String[] methods = new String[] {"mean", "forward"};

        TransformMethod[] transformMethods = new TransformMethod[]
                {
                TransformMethod.TRANSFORM_SUPER_13, TransformMethod.TRANSFORM_BEST,
                //TransformMethod.TRANSFORM_MIL
                };


        for (String method : methods)
        {
            System.out.println("Data obtained by " + method.toUpperCase() + " method...");

            //list files in the local file path
            List<String> setAFilePaths = Utils.listFilesFromLocalPath(
                    //!undersample_50_50 ?
                            PHYSIONET_SET_A_DIR_PATH,
                            //: PHYSIONET_SET_A_DIR_PATH + File.separator + "undersampled",
                    false);
                    // = Utils.listFilesFromLocalPath("./" + method + "_set_a", false);
            List<String> setBFilePaths = Utils.listFilesFromLocalPath(PHYSIONET_SET_B_DIR_PATH, false);
                    // = Utils.listFilesFromLocalPath("./" + method + "_set_b", false);
            //List<String> setCFilePaths = Utils.listFilesFromLocalPath(PHYSIONET_SET_C_DIR_PATH, false);

            List<String> allFilePaths = new ArrayList<>();
            allFilePaths.addAll(setAFilePaths);
            allFilePaths.addAll(setBFilePaths);
            //allFilePaths.addAll(setCFilePaths);


            //preprocess files
            Map<String, String> filePathFileContentsMap = Preprocessing.generateTimeSeriesData(allFilePaths, "",
                    VarRanges.getInstance(Dataset.PhysioNet));

            long start = System.currentTimeMillis();
            double missingValuePlaceHolder = -2.0;
            List<MTSE> mtses = new ArrayList<>();


            //TODO parallelStream API to read mtses from file
            //obtain multivariate time series from each file
            for(String filePath : filePathFileContentsMap.keySet())
            {
                String fileName = Utils.fileNameFromPath(filePath);
                String fileContents = filePathFileContentsMap.get(filePath);

                //create multivariate time series object from each file's contents
                MTSE mtse
                        = MTSE.fromFile(Dataset.PhysioNet, fileName, fileContents, ",", missingValuePlaceHolder);
                //println(mtse.toVerticalString());

                mtses.add(mtse);
                //break;
            } // for
            //System.exit(0);
            long end = System.currentTimeMillis();
            System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(end-start) + " seconds for reading all mtses");


            //clear memory
            filePathFileContentsMap.clear();
            filePathFileContentsMap = null;


            //run all experiments
            try
            {
                //sort mtses by record id
                List<MTSE> train = new ArrayList<>();
                for (int index = 0; index < setAFilePaths.size(); index++)
                    train.add(mtses.get(index));
                Collections.sort(train); // sorts by record id
                List<MTSE> test = new ArrayList<>();
                for (int index = setAFilePaths.size(); index < setAFilePaths.size() + setBFilePaths.size(); index++)
                    test.add(mtses.get(index));
                Collections.sort(test); // sorts by record id
                train.addAll(test); // keeps insertion order
                mtses = train; // now train and test data are individually sorted by record id



                //impute using of one the chosen methods
                Imputations.ImputeMethod imethod = method.equals("mean") ?
                        Imputations.ImputeMethod.MEAN_VALUE_WITH_MASKING_VECTOR_IMPUTATION : Imputations.ImputeMethod.LIPTON_FORWARD_FILLING_IMPUTATION;
                mtses = Imputations.getInstance().impute(mtses,
                        imethod,
                        new int[]{0, setAFilePaths.size() //setCFilePaths.size()
                        }, VarRanges.getInstance(Dataset.PhysioNet), missingValuePlaceHolder);


                for(TransformMethod transformMethod : transformMethods)
                {
                    long startProg = System.currentTimeMillis();


                    List<MTSE> tMtses
                            = Utils.transformMtses(mtses, setAFilePaths.size(), //setCFilePaths.size(),
                            transformMethod);


                    Utils.mtsesToMIArffData(Dataset.PhysioNet, tMtses.subList(0, setAFilePaths.size()), Outcomes.getInstance(Dataset.PhysioNet),
                            method + "_set_a" + File.separator + transformMethod.simpleName(), "patient_record_id",
                            new String[]{"1", "0"}, false, "set_a");

                    Utils.mtsesToMIArffData(Dataset.PhysioNet, tMtses.subList(setAFilePaths.size(),
                            setAFilePaths.size() + setBFilePaths.size()), Outcomes.getInstance(Dataset.PhysioNet),
                            method + "_set_b" + File.separator + transformMethod.simpleName(), "patient_record_id",
                            new String[]{"1", "0"}, false, "set_b");



                    AttributeTransformFilter atf = standardize ? AttributeTransformFilter.STANDARDIZE : AttributeTransformFilter.NONE;


                    if (!transformMethod.equals(TransformMethod.TRANSFORM_MIL))
                        Experiments.run_RF(method, transformMethod.simpleName(),
                                atf, ImbalanceHandler.NONE);
                    else
                        Experiments.run_MIW_RF(method, transformMethod.simpleName(),
                                atf, ImbalanceHandler.NONE);



                    long endProg = System.currentTimeMillis();
                    System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(endProg-startProg) + " seconds for execution");
                    System.out.println("====================================================");
                } // for

            } catch (Exception e)
            {
                e.printStackTrace();
            }

        } // for

    } // secondSetupMeanForwardAll




    //NOTES:
    //RealAdaBoost cls = new RealAdaBoost(); // improves MIW-LR performance better than all other meta learners
    //Bagging cls = new Bagging(); // drops MIW-LR performance
    //AdditiveRegression cls = new AdditiveRegression(); // error on MIW-LR - designed for regression tasks; cannot handle binary class!
    //LogitBoost cls = new LogitBoost(); // error on MIW-LR - cannot handle numeric class!
    //MultiBoostAB cls = new MultiBoostAB(); //improves MIW-LR performance but not better than RealAdaBoost
    //AdaBoostM1 cls = new AdaBoostM1(); //improves MIW-LR performance but not better than RealAdaBoost
    //RotationForest cls = new RotationForest(); //error on MIW-LR - Cannot handle relational attributes!
    //RacedIncrementalLogitBoost cls = new RacedIncrementalLogitBoost(); //error on MIW-LR - cannot handle numeric class!
    //Decorate cls = new Decorate(); // error - Decorate can only handle numeric and nominal values.
    //ClassificationViaRegression cls = new ClassificationViaRegression(); //error -  Cannot handle numeric class!
    //CostSensitiveClassifier cls = new CostSensitiveClassifier(); // java.lang.Exception: On-demand cost file doesn't exist
    //---------------------------------------------
    //SimpleMI up to 0.8395 AUROC using a.a. and RealAdaBoost <- SimpleMI <- RandomForest
    //RealAdaBoost cls = new RealAdaBoost(); // drops SMI-LR performance, //improves SMI-RF performance
    //RotationForest cls = new RotationForest(); //error on SMI-LR - Cannot handle relational attributes!
    //Decorate cls = new Decorate(); // error on SMI-LR - Decorate can only handle numeric and nominal values.
    //Bagging cls = new Bagging(); // RF is bagging algorithm //improves SMI-LR performance
    //AdaBoostM1 cls = new AdaBoostM1(); // drops SMI-LR performance
    //MultiBoostAB cls = new MultiBoostAB(); // drops SMI-LR performance
    //----------------------------------------------
    //RealAdaBoost cls = new RealAdaBoost(); // drops MILR performance
    //Bagging cls = new Bagging(); //IMPROVES MILR PERFORMANCE with 10 iterations

} // class Utils
