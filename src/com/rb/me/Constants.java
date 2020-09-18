package com.rb.me;

import java.io.File;

//java class to keep the application wide constants
public final class Constants
{
    public static final String PHYSIONET_DATA_FOLDER
            //= "C:\\Users\\rbabayev\\Downloads\\physionet-challenge\\original_data";
            //= "C:\\Users\\rbabayev\\Downloads\\physionet_challenge_test\\original_data";
            = "data"; // project local data folder


    public static final String PHYSIONET_SET_A_DIR_PATH = PHYSIONET_DATA_FOLDER + File.separator + "set-a";
    public static final String PHYSIONET_SET_B_DIR_PATH = PHYSIONET_DATA_FOLDER + File.separator + "set-b";
    public static final String PHYSIONET_SET_C_DIR_PATH = PHYSIONET_DATA_FOLDER + File.separator + "set-c";


    public static final String PHYSIONET_SET_A_OUTCOMES_FILE_PATH = PHYSIONET_DATA_FOLDER + File.separator + "Outcomes-a.txt";
    public static final String PHYSIONET_SET_B_OUTCOMES_FILE_PATH = PHYSIONET_DATA_FOLDER + File.separator + "Outcomes-b.txt";
    public static final String PHYSIONET_SET_C_OUTCOMES_FILE_PATH = PHYSIONET_DATA_FOLDER + File.separator + "Outcomes-c.txt";


    public static final String PHYSIONET_VAR_RANGES_FILE_PATH = "resources/variable_ranges_physionet.csv";

    // PRIVATE //
    private Constants() {
        //this prevents even the native class from
        //calling this constructor as well :
        throw new AssertionError();
    }
} // class Constants
