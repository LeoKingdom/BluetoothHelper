package com.ly.blehelper.esim.callbacks;


import com.ly.blehelper.core.callbacks.BaseBleCallback;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/22 15:46
 * version: 1.0
 * <p>
 * 激活esim卡回调
 */
public abstract class EsimActiveCallback extends BaseBleCallback {
    /**
     * 蓝牙地址非法
     */
    public  void macInvalidate(){}

    /**
     * 未找到设备
     */
   public void deviceNotFound(){}

   public void notifyCallback(byte[] data){}

    /**
     * 激活结果,非0为失败
     * @param isActivated 是否成功
     */
   public abstract void activeResult(boolean isActivated);
}
