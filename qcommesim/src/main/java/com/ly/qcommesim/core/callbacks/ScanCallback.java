package com.ly.qcommesim.core.callbacks;


import com.fastble.fastble.data.BleDevice;

import java.util.List;


/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/11/26 16:53
 * version: 1.0
 */
public abstract class ScanCallback extends BaseBleCallback {
    public abstract void onScanFinished(List<BleDevice> bleDeviceList);
    public void onScanStart() {}
    public void onScanning(BleDevice device) {}
    public void onBleDisable() {}
}
