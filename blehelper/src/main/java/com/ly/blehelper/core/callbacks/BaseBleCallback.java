package com.ly.blehelper.core.callbacks;

import fastble.exception.BleException;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/11/22 15:17
 * version: 1.0
 */
public abstract class BaseBleCallback {
    /**
     * 错误
     *
     * @param err
     */
    public  void error(String err){}

    /**
     * 操作超时
     *
     * @param e
     */
    public  void timeout(BleException e){}

    /**
     * uuid无效,进行读、写、通知等操作必须
     */
    public  void uuidInvalid(){}

    /**
     * 设备未连接
     */
    public  void deviceNotConnect(){}

    /**
     * 准备就绪,可以进行接下来的逻辑了
     */
    public  void onNext(){}
}
