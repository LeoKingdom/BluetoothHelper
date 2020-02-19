package com.ly.blehelper.core.callbacks;


import fastble.data.BleDevice;
import fastble.exception.BleException;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/11/26 18:45
 * version: 1.0
 */
public abstract class NotifyOpenCallback extends BaseBleCallback {
    public abstract void onNotifySuccess(BleDevice device);

    public abstract void onNotifyFailed(BleException e);

    public abstract void onCharacteristicChanged(String mac, byte[] data);
}
