package com.ly.blehelper.upgrade_core.fota_upgrade.callbacks;


import com.ly.blehelper.core.callbacks.BaseBleCallback;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/15 14:59
 * version: 1.0
 */
public abstract class DataCallback extends BaseBleCallback {
    public  void nextFrame(int currentFrame,int totalFrame){}
    public  void reSend(){}
    public abstract void done();
    public void checkOutTime(){}
    public void binChecking(){}
    public void binCheckDone(boolean isBin){}
    public void fileNotFound(String msg){}
}
