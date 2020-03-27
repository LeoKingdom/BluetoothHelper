package com.ly.qcommesim.esim.beans;

import com.fastble.fastble.data.BleDevice;

public class EsimDevice {
    private String mac;//蓝牙mac地址
    private String url; //运营商的激活服务器地址
    private BleDevice bleDevice; //fastble device

    public String getMac() {
        return mac;
    }

    public void setMac(String mMac) {
        this.mac = mMac;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String mUrl) {
        this.url = mUrl;
    }

    public BleDevice getBleDevice() {
        return bleDevice;
    }

    public void setBleDevice(BleDevice bleDevice) {
        this.bleDevice = bleDevice;
    }

    @Override
    public String toString() {
        return "EsimDevice{" +
                "mMac='" + mac + '\'' +
                ", mUrl='" + url + '\'' +
                ", bleDevice=" + bleDevice +
                '}';
    }
}
