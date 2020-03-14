package com.ly.qcommesim.core.callbacks;


import com.fastble.fastble.data.BleDevice;
import com.fastble.fastble.exception.BleException;


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
