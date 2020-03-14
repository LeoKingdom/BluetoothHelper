/**************************************************************************************************
 * Copyright 2017 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.ly.qcommesim.qcomm.utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

/**
 * <p>This final class encapsulates all useful general constants for the application.</p>
 */
public final class Consts {

    /**
     * The boolean to display - or not - the log depending on the debug state.
     */
    public static final boolean DEBUG = false;

    /**
     * The unit to use to display file size.
     */
    public static final String UNIT_FILE_SIZE = " KB";

    /**
     * The character which represents a percentage.
     */
    public static final String PERCENTAGE_CHARACTER = "%";

    /**
     * <p>To define the format of a date.</p>
     */
    public static final String DATE_FORMAT = "HH:mm - d MMM yyyy";

    /**
     *
     * and {@link Activity#startActivityForResult(Intent, int) startActivityForResult} when requesting to enable
     * the device Bluetooth.
     */
    public static final int ACTION_REQUEST_ENABLE_BLUETOOTH = 101;

    /**
     * The code to use for the methods {@link Activity#onRequestPermissionsResult(int, String[], int[])}
     * onRequestPermissionsResult} and
     * {@link android.support.v4.app.ActivityCompat#requestPermissions(Activity, String[], int) requestPermissions}
     * when requesting to the user to enable needed permissions.
     */
    public static final int ACTION_REQUEST_PERMISSIONS = 102;

    /**
     * Opcode when launching picker activity expecting result.
     */
    public final static int ACTION_REQUEST_PICK_FILE = 103;

    /**
     * The extra key used with {@link #ACTION_REQUEST_PICK_FILE ACTION_REQUEST_PICK_FILE} to provide an absolute path
     * to the file which had been picked during the action.
     */
    public static final String EXTRA_FILE_RESULT_KEY = "EXTRA_FILE_RESULT_KEY";

    /**
     * The default time to scan for BLE devices - this is used as a timeout to call
     * {@link BluetoothAdapter#stopLeScan(BluetoothAdapter.LeScanCallback) stopLeScan}.
     */
    public static final int SCANNING_TIME = 10000;

    /**
     * <p>This class regroups all keys and values used for intents.</p>
     */
    public class Intents {
        /**
         * The extra key used by any other application to store a file path in an intent to start this application.
         */
        public static final String EXTRA_FILE_PATH = "EXTRA_FILE_PATH";
        /**
         * The extra key used by any other application to store a service ID/UUID to use to filter devices in this
         * application.
         */
        public static final String EXTRA_SERVICE_FILTER = "EXTRA_SERVICE_FILTER";
    }


}
