package com.ly.qcommesim.core.callbacks;


import com.fastble.fastble.exception.BleException;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/11/26 18:54
 * version: 1.0
 */
public abstract class ReadRssiCallback extends BaseBleCallback {
    public abstract void onRemoteRssi(int rssi);

    public  void onRssiFailure(BleException exception){}
}
