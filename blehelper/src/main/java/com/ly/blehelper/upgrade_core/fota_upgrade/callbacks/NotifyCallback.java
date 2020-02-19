package com.ly.blehelper.upgrade_core.fota_upgrade.callbacks;


import com.ly.blehelper.core.callbacks.BaseBleCallback;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/15 14:22
 * version: 1.0
 */
public abstract class NotifyCallback extends BaseBleCallback {
    public  void characteristicChange(int action,byte[] backBytes){}
    public void deviceReconn(){}
}
