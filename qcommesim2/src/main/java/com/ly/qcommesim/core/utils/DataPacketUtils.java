package com.ly.qcommesim.core.utils;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/9/24 17:23
 * version: 1.0
 */
public class DataPacketUtils {
    /**
     * @param datas 字节流
     * @param curr  当前帧
     * @param total 总帧
     * @return 当前帧字节数组
     */

    public static final int CURR_MAX_MTU = 20; //当前支持最大传输字节
    private static final int CURR_MAX_BYTE = 19; //每包字节数
    public static final int CURR_MAX_FRAME = 4096; //每帧字节数
    private static final int CURR_MAX_FRAME_PACKETS = 216; //每帧每包数

    public static byte[] currentPacket(byte[] datas, int curr, int total) {
        byte[] eachFrameBytes = null;
        if (datas.length > CURR_MAX_FRAME) {
            if (curr == total) {
                //最后一帧,不一定是4kb
                int lastPacketLenght = datas.length - (total - 1) * CURR_MAX_FRAME;
                eachFrameBytes = TransformUtils.subBytes(datas, (curr - 1) * CURR_MAX_FRAME, lastPacketLenght);
            } else {
                //每一帧,长度为4kb
                eachFrameBytes = TransformUtils.subBytes(datas, (curr - 1) * CURR_MAX_FRAME, CURR_MAX_FRAME);
            }
        } else {
            eachFrameBytes = datas;
        }
        return eachFrameBytes;
    }

    public static byte[] sortEachFrame(byte[] datas, int curr, int total) {
        byte[] currentFrame = currentPacket(datas, curr, total);
        byte[] sortFrame = splitEachFrameBytes(currentFrame);
        return sortFrame;
    }

    /**
     * 将每一帧的每一包加上序号和最后的crc字节
     *
     * @param eachFrameBytes 一帧数据
     * @return 加上序号后一帧数据
     */
    public static byte[] splitEachFrameBytes(byte[] eachFrameBytes) {
        int length = (eachFrameBytes.length % CURR_MAX_BYTE == 0) ? (eachFrameBytes.length / CURR_MAX_BYTE) : (eachFrameBytes.length / CURR_MAX_BYTE + 1);
        int num = 0;
        int num1 = 0;
        byte[] lastBytes = null;
        boolean isTrue = eachFrameBytes.length % CURR_MAX_BYTE == 0;
        int tem = CURR_MAX_BYTE;
        for (int i = 1; i <= length; i++) {
            num++;
            num1++;
            byte[] eachHeadBytes = new byte[]{(byte) (Integer.parseInt(Integer.toHexString(num), 16))};
            if (num == CURR_MAX_FRAME_PACKETS) {
                num = 0;
            }

            if (!isTrue && i == length) {
                tem = eachFrameBytes.length - CURR_MAX_BYTE * (length - 1);
            }
            byte[] eachBytes = TransformUtils.subBytes(eachFrameBytes, (i - 1) * CURR_MAX_BYTE, tem);
            byte[] handleBytes = TransformUtils.combineArrays(eachHeadBytes, eachBytes);
            if (i != 1) {
                lastBytes = TransformUtils.combineArrays(lastBytes, handleBytes);
            } else {
                lastBytes = handleBytes;
            }
        }
        byte crcByte = CRCCheckUtils.calcCrc8(eachFrameBytes);
        if (isTrue) {
            lastBytes = TransformUtils.combineArrays(lastBytes, new byte[]{(byte) (num1 + 1), crcByte});
        } else {
            lastBytes = TransformUtils.combineArrays(lastBytes, new byte[]{crcByte});
        }
        return lastBytes;
    }


    public static byte[] getHeadBytes(Context context, InputStream inputStream) {
        byte[] bytes = null;
        try {
            bytes = TransformUtils.streamToByte(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] bytes1 = TransformUtils.subBytes(bytes, 0, 5);
        return bytes1;
    }


    public static byte[] dataLenght(int dataLenght) {
        byte[] lengthByte = new byte[3];
        int l = dataLenght / 256;
        int m = dataLenght % 256;
        lengthByte[0] = TransformUtils.hexToByte("FD");
        lengthByte[1] = TransformUtils.int2byte(l);
        lengthByte[2] = TransformUtils.int2byte(m);
        return lengthByte;
    }

    public static byte[] frameHeadBytes(byte[] headData, byte[] data, int totalFrame, int currFrame) {
        byte[] firstByte = dataLenght(data.length);
        byte[] middleByte = new byte[]{(byte) totalFrame, (byte) currFrame};
        byte[] endByte = new byte[]{headData[headData.length - 2], headData[headData.length - 1]};
        return TransformUtils.combineArrays(firstByte, middleByte, endByte);
    }

    public static byte[] frameBytes(int totalFrame, int currFrame) {
        byte[] frameByte = new byte[2];
        frameByte[0] = TransformUtils.int2byte(totalFrame);
        frameByte[1] = TransformUtils.int2byte(currFrame);
        return frameByte;
    }


    //0xAB 0x00 0x04 0x03  0x01  0x20 0x03  0x03  0x01 0x0A 0x64 收包回复
    //帧头 数据-长度 总帧 当前帧 MId  EId  丢包数 之后为丢包序号(上限五包,超过填0xFF)

    /**
     * @param data         字节流
     * @param responseByte 响应字节数组
     * @return
     */
    public static byte[] losePackets(byte[] data, byte[] responseByte) {
        byte[] lostPacketBytes = null;
        int resLength = responseByte.length;
        byte loseCurrentByte = responseByte[resLength - 1];
        if (resLength > 8) {
            byte loseTotalByte = responseByte[7];
            //截取丢包序号
            byte[] loseNums = Arrays.copyOfRange(responseByte, 7, resLength);
            if (loseCurrentByte == (byte) 0x00 && loseTotalByte == (byte) 0x00) {
                //未丢包
                return null;
            }

            if (loseTotalByte == (byte) 0xFF || (loseTotalByte == (byte) 0x00 && loseCurrentByte == (byte) 0x01)) {
                //丢五包以上或者校验错误,responseByte长度为9
                return new byte[]{loseCurrentByte};
            }
            byte[] loseTotalBytes = null;
            for (int i = 0; i < loseNums.length; i++) {
                int num = 0xFF & loseNums[i];//丢包序号
                byte[] eachPacket = TransformUtils.subBytes(data, num * CURR_MAX_MTU, CURR_MAX_MTU);
                if (i == 0) {
                    loseTotalBytes = eachPacket;
                } else {
                    loseTotalBytes = TransformUtils.combineArrays(loseTotalBytes, eachPacket);
                }
            }

            return loseTotalBytes;
        } else if (resLength == 8) {
            if (loseCurrentByte == (byte) 0x00) {
                //未丢包
                return null;
            }
        }
        return lostPacketBytes;
    }
}
