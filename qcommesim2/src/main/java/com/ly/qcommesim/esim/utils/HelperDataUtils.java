package com.ly.qcommesim.esim.utils;

import com.ly.qcommesim.core.utils.DataPacketUtils;
import com.ly.qcommesim.core.utils.TransformUtils;

import java.util.Arrays;

public class HelperDataUtils {

    /**
     * 处理数据头
     *
     * @param data
     * @param currentFrame
     * @param totalFrame
     * @param moduleId
     * @param eventId
     * @return
     */
    public static byte[] frameHeadBytes(byte[] data, int currentFrame, int totalFrame, byte moduleId, byte eventId) {
        //处理每帧数据
        byte[] currentFrameBytes = DataPacketUtils.currentPacket(data, currentFrame, totalFrame);
        byte[] dataLengts = DataPacketUtils.dataLenght(currentFrameBytes.length);
        byte[] headBytes = new byte[]{-3, dataLengts[1], dataLengts[2], (byte) totalFrame, (byte) currentFrame, moduleId, eventId};
        return headBytes;
    }

    /**
     * 组包
     *
     * @param data     包数据
     * @param preBytes 每次组包后的数据
     * @return
     */
    public static byte[] combinePacket(byte[] data, byte[] preBytes) {
        data = Arrays.copyOfRange(data, 1, data.length);
        byte[] dataBytes = preBytes;
        if (dataBytes == null) {
            dataBytes = data;
        } else {
            dataBytes = TransformUtils.combineArrays(preBytes, data);
        }
        return dataBytes;
    }

    /**
     * 计算字节长度
     *
     * @param lLength 高位
     * @param sLength 低位
     * @return
     */
    public static int calTotalPackets(int lLength, int sLength) {
        int tt = 0;
        if (lLength != 0) {
            tt = (lLength * 256) + sLength;
        } else {
            tt = sLength;
        }
        tt = tt % 19 == 0 ? tt + 1 : tt; //若刚好整包crc会独立一包
        return tt;
    }

    public static int calTotalFrame(int length) {
        int totalFrame = length % DataPacketUtils.CURR_MAX_FRAME == 0 ? length / DataPacketUtils.CURR_MAX_FRAME : length / DataPacketUtils.CURR_MAX_FRAME + 1;
        return totalFrame;
    }
}
