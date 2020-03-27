package com.ly.qcommesim.esim.enums;

public enum  EsimEnum {
    ESIM_ACTIVE(0), //激活
    ESIM_CANCEL(1), //去活
    ESIM_DELETE(2), //删除profile
    MODULE_BASIC(3), //moduleId=0x10 目前只有定位获取一个情况
    MODULE_OTA(4), //moduleId=0x20 ota升级
    MODULE_ESIM(5); //moduleId=0x30 esim
    public int action;
     EsimEnum(int action){
        this.action=action;
    }
}
