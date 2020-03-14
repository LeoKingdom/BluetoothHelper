package com.ly.qcommesim.esim.bean;

import java.util.Arrays;

public class EsimData {
    private int stepAction = -1; //进行的步骤
    private byte[] dataBytes;
    private int packets; //每次回复需要接收的包数
    private int sumPackets = 0;//已收包数
    private byte[] headBytes;
    private int totalFrame;
    private int currentFrame = 0;

    public int getStepAction() {
        return stepAction;
    }

    public void setStepAction(int stepAction) {
        this.stepAction = stepAction;
    }

    public byte[] getDataBytes() {
        return dataBytes;
    }

    public void setDataBytes(byte[] dataBytes) {
        this.dataBytes = dataBytes;
    }

    public int getPackets() {
        return packets;
    }

    public void setPackets(int packets) {
        this.packets = packets;
    }

    public int getSumPackets() {
        return sumPackets;
    }

    public void setSumPackets(int sumPackets) {
        this.sumPackets = sumPackets;
    }

    public byte[] getHeadBytes() {
        return headBytes;
    }

    public void setHeadBytes(byte[] headBytes) {
        this.headBytes = headBytes;
    }

    public int getTotalFrame() {
        return totalFrame;
    }

    public void setTotalFrame(int totalFrame) {
        this.totalFrame = totalFrame;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }

    @Override
    public String toString() {
        return "EsimData{" +
                "stepAction=" + stepAction +
                ", dataBytes=" + Arrays.toString(dataBytes) +
                ", packets=" + packets +
                ", sumPackets=" + sumPackets +
                ", headBytes=" + Arrays.toString(headBytes) +
                ", totalFrame=" + totalFrame +
                ", currentFrame=" + currentFrame +
                '}';
    }
}
