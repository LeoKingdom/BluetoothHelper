/**************************************************************************************************
 * Copyright 2017 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.ly.qcommesim.core.utils;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * <p>This class contains all useful methods for this module.</p>
 */
public class Utils {

    public static long betweenTimes=200;
    //反射来调用BluetoothDevice.removeBond取消设备的配对
    public static void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e("bondRemove--", e.getMessage());
        }
    }

}