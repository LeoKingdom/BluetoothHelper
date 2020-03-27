package com.ly.qcommesim.core.helper;

import android.app.Application;

import com.fastble.fastble.data.BleDevice;
import com.xble.xble.core.BaseCore;

import java.util.List;

public class BaseHelper extends BaseCore {
    private BleBaseHelper bleBaseHelper;
    public BaseHelper(Application app,String mac){
        super(app);
        bleBaseHelper=new BleBaseHelper(app,mac);
    }
    public BleDevice getConnDevice(String mac){
        List<BleDevice> connList = getAllConnectBLEs();
        if (connList!=null&&connList.size()>0){
            for (BleDevice device:connList){
                if (mac.equalsIgnoreCase(device.getMac())){
                    return device;
                }
            }
        }
        return null;
    }
}
