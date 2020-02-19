package com.ly.blehelper.core.callbacks;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/11/27 15:08
 * version: 1.0
 * 蓝牙和gps打开回调
 */
public interface OpenListener {
    void open(boolean isOpenBle, boolean isOpenGps);
}
