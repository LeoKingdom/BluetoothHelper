/**************************************************************************************************
 * Copyright 2017 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.ly.qcommesim.core.utils;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * <p>This class contains all useful methods for this module.</p>
 */
public class Utils {

    public static long betweenTimes = 20;

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
     * 判断mac地址是否合法
     *
     * @param mac
     * @return
     */
    public static boolean isMacAddress(String mac) {
        String paternStr = "^[a-fA-F0-9]{2}+:[a-fA-F0-9]{2}+:[a-fA-F0-9]{2}+:[a-fA-F0-9]{2}+:[a-fA-F0-9]{2}+:[a-fA-F0-9]{2}$";
        Pattern pattern=Pattern.compile(paternStr);
        return pattern.matcher(mac).find();
    }

    /**
     * 判断url是否有效
     *
     * @param url
     * @return
     */
    public static boolean isUrlEffective(String url) {
        URL url1;
        try {
            url1 = new URL(url);
            InputStream inputStream = url1.openStream();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}