package com.ly.blehelper.core.callbacks;


/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/11/27 11:24
 * version: 1.0
 */
public abstract class MTUCallback extends BaseBleCallback {
    public abstract void setFail(String err);

    public abstract void setSuccess(int mtu);
}
