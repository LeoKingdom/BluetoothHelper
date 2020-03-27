package com.ly.qcommesim.core.beans;

import com.ly.qcommesim.esim.enums.EsimEnum;

/**
 * 协议头实体类
 * [0xFD,0x00,0x00,0x00,0x00,0x10,0x01]
 * 固定位 长度  长度  总帧 当前帧 模块 事件
 */
public class OrderBean {
    final byte first = (byte) 0xFD;
    int longL;
    int shortL;
    byte tFrame;
    byte cFrame;
    byte module;
    byte event;
    private byte[] data;

    public OrderBean(byte[] data) {
        this.data = data;
        setLongL(data[1] & 0xFF);
        setShortL(data[2] & 0xFF);
        settFrame(data[3]);
        setcFrame(data[4]);
        setModule(data[5]);
        setEvent(data[6]);
    }

    public byte getFirst() {
        return first;
    }

    public int getLongL() {
        return longL;
    }

    public void setLongL(int longL) {
        this.longL = longL;
    }

    public int getShortL() {
        return shortL;
    }

    public void setShortL(int shortL) {
        this.shortL = shortL;
    }

    public byte gettFrame() {
        return tFrame;
    }

    public void settFrame(byte tFrame) {
        this.tFrame = tFrame;
    }

    public byte getcFrame() {
        return cFrame;
    }

    public void setcFrame(byte cFrame) {
        this.cFrame = cFrame;
    }

    public byte getModule() {
        return module;
    }

    public void setModule(byte module) {
        this.module = module;
    }

    public byte getEvent() {
        return event;
    }

    public void setEvent(byte event) {
        this.event = event;
    }

    @Override
    public String toString() {
        return "OrderBean{" +
                "first=" + first +
                ", longL=" + longL +
                ", shortL=" + shortL +
                ", tFrame=" + tFrame +
                ", cFrame=" + cFrame +
                ", module=" + module +
                ", event=" + event +
                '}';
    }


    public EsimEnum esimAction() {
        if (data.length > 7) {
            boolean moduleId = data[5] == (byte) 0x30;
            byte eventId = data[6];
            if (moduleId) {
                if (eventId == (byte) 0x08) {
                    return EsimEnum.ESIM_ACTIVE;
                } else if (eventId == (byte) 0x09) {
                    return EsimEnum.ESIM_CANCEL;
                } else if (eventId == (byte) 0x0A) {
                    return EsimEnum.ESIM_DELETE;
                }

            }
        }
        return null;
    }
}
