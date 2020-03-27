package com.ly.qcommesim.core.enums;

public enum  DisconnStateEnum {

    DISCONN_BLECLOSE(0), //手动关闭主控蓝牙
    DISCONN_AWAY(1), //蓝牙设备走远而导致断开
    DISCONN_CODE(2), //调用代码主动断开
    DISCONN_OTHER(3); //其他异常情况断开

    public int state;

    DisconnStateEnum(int state) {
        this.state = state;
    }
}
