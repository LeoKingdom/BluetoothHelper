package com.ly.blehelper.core.callbacks;

import android.bluetooth.BluetoothGatt;


import fastble.data.BleDevice;
import fastble.exception.BleException;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/11/26 17:48
 * version: 1.0
 */
public abstract class ConnectCallback extends BaseBleCallback {
    public abstract void onConnectSuccess(BleDevice device, BluetoothGatt gatt);

    public abstract void onConnectFail(BleDevice device, BleException e);

    public  void onBleDisable(){}

    public abstract void onDisconnect(boolean isActiveDisConnected, BleDevice device);
}
