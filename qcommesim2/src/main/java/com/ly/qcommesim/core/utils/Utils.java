/**************************************************************************************************
 * Copyright 2017 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.ly.qcommesim.core.utils;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * 去除特殊制表符
     * @param str
     * @return
     */
    public static String replaceBlank(String str) {
        String dest = "";
        if (str!=null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }

}