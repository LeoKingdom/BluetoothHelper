package com.ly.blehelper.upgrade_core.fota_upgrade.callbacks;


import com.ly.blehelper.core.callbacks.BaseBleCallback;

import fastble.data.BleDevice;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/12/31 17:12
 * version: 1.0
 */
public abstract class UpgradeStatusCallback extends BaseBleCallback {
    public void deviceDisconn(BleDevice device, boolean isActive){}
    public void fileIsEmpty(){}
    public void connSuccess(BleDevice device){}
    public void reConnSuccess(BleDevice device){}
}
