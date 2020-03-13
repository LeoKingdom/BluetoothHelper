package com.ly.qcommesim.core.callbacks;


/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/15 14:25
 * version: 1.0
 */
public abstract class WriteCallback extends BaseBleCallback {
    /**
     * 写入操作
     * @param actionType 操作类型,fota升级用到
     * @param current   当前包
     * @param total     总大小
     * @param justWrite 每一包
     */
    public abstract void writeSuccess(int actionType,int current, int total, byte[] justWrite);
}
