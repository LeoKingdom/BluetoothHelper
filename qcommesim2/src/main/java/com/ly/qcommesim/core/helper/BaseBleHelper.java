package com.ly.qcommesim.core.helper;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;

import com.fastble.fastble.data.BleDevice;
import com.xble.xble.core.BaseCore;
import com.xble.xble.core.log.Logg;

public class BaseBleHelper extends BaseCore {

    public BaseBleHelper(Application app) {
        super(app);
    }

    public BleDevice getConnDevice(String mac) {
        BleDevice existsDevice = isTargetBLEExist(mac);
        if (existsDevice != null) {
            return existsDevice;
        }
        return null;
    }

    private static final String TAG = "BleBaseHelper";
    private long scanTimeout = 10000;


    /**
     * 打开蓝牙,监听返回
     *
     * @param context
     * @param requestCode
     */
    public void enableBle(Activity context, int requestCode) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        context.startActivityForResult(enableBtIntent, requestCode);
    }

    /**
     * 判断GPS是否打开
     *
     * @param context
     * @return
     */
    private boolean isGpsOpen(Context context) {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 提醒用户去设置页打开gps,监听返回
     *
     * @param activity
     * @param requestCode
     */
    public void openGPS(Activity activity, int requestCode) {
        if (!isGpsOpen(activity)) {
            Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivityForResult(locationIntent, requestCode);
        }
    }


    /*********************打印log方法***********************/

    /**
     * 打印过程 (只打印1次)
     *
     * @param text 内容
     */
    private void printLogOne(String text) {
        if (!Logg.isPrintOne) {
            Logg.t(TAG).recordLog(getClass(), text, Logg.VERBOSE);
            Logg.isPrintOne = true;
        }

    }

    /**
     * 打印过程
     *
     * @param text 内容
     */
    private void printLog(String text) {
        Logg.t(TAG).recordLog(getClass(), text, Logg.VERBOSE);
    }

    /**
     * 打印结果
     *
     * @param text 内容
     */
    private void printResult(String text) {
        Logg.t(TAG).recordLog(getClass(), text, Logg.INFO);
    }

    /**
     * 打印错误
     *
     * @param text 内容
     */
    private void printError(String text) {
        Logg.t(TAG).recordLog(getClass(), text, Logg.ERROR);
    }
}
