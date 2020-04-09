package com.ly.qcommesim.core.utils;

import android.content.Context;
import android.util.Log;

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
    public static final int MAX_BYTES=20;//最大传输单元
    public static final int MAX_FRAME_BYTES=4096;//帧总字节
    /**
     * @param datas 字节流
     * @param curr  当前帧
     * @param total 总帧
     * @return 当前帧字节数组
     */
    public static byte[] currentPacket(byte[] datas, int curr, int total) {
        byte[] eachFrameBytes = null;
        if (datas.length > MAX_FRAME_BYTES) {
            if (curr == total) {
                //最后一帧,不一定是4kb
                int lastPacketLenght = datas.length - (total - 1) * MAX_FRAME_BYTES;
                eachFrameBytes = TransformUtils.subBytes(datas, (curr - 1) * MAX_FRAME_BYTES, lastPacketLenght);
            } else {
                //每一帧,长度为4kb
                eachFrameBytes = TransformUtils.subBytes(datas, (curr - 1) * MAX_FRAME_BYTES, MAX_FRAME_BYTES);
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
        int length = (eachFrameBytes.length % 19 == 0) ? (eachFrameBytes.length / 19) : (eachFrameBytes.length / 19 + 1);
        int num = 0;
        int num1 = 0;
        byte[] lastBytes = null;
        boolean isTrue = eachFrameBytes.length % 19 == 0;
        int tem = 19;
        for (int i = 1; i <= length; i++) {
            num++;
            num1++;
            byte[] eachHeadBytes = new byte[]{(byte) (Integer.parseInt(Integer.toHexString(num), 16))};
            if (num == 216) {
                num = 0;
            }

            if (!isTrue && i == length) {
                tem = eachFrameBytes.length - 19 * (length - 1);
            }
            byte[] eachBytes = TransformUtils.subBytes(eachFrameBytes, (i - 1) * 19, tem);
            byte[] handleBytes = TransformUtils.combineArrays(eachHeadBytes, eachBytes);
            if (i != 1) {
                lastBytes = TransformUtils.combineArrays(lastBytes, handleBytes);
            } else {
                lastBytes = handleBytes;
            }
        }
        byte crcByte = CRCCheckUtils.calcCrc8(eachFrameBytes);
        if (isTrue) {
            lastBytes = TransformUtils.combineArrays(lastBytes, new byte[]{(byte) (num1+1), crcByte});
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

    /**
     * @param inputStream 文件输入流
     * @return 加上校验字节后的字节数组
     */
    public static byte[] crcCombineBytes(InputStream inputStream) {
        //合并后的字节数组
        byte[] lastBytes = null;
        try {
            byte[] bytes = TransformUtils.streamToByte(inputStream);
            //由于需要带上序号,因此总的byte数组长度增大
            int totalPackets0 = (bytes.length % 19 == 0) ? (bytes.length / 19) : (bytes.length / 19 + 1);
            int crcTotalFrame = bytes.length % 3891 == 0 ? bytes.length / 3891 : bytes.length / 3891 + 1;
            byte[] crcBytes = null;
            int num = 0;
            int currentPacket = 0;
            if (bytes.length > 4 * 1024) {
                for (int j = 0; j < totalPackets0; j++) {
                    num++;
                    byte[] eachHeadBytes = new byte[]{(byte) (Integer.parseInt(Integer.toHexString(num), 16))};
                    if (num == 216) {
                        num = 0;
                    }
                    int tem = 0;
                    if (j != totalPackets0 - 1) {
                        tem = 19;
                    } else {
                        tem = bytes.length - 19 * (totalPackets0 - 1);
                    }
                    byte[] eachBytes = TransformUtils.subBytes(bytes, j * 19, tem);
                    byte[] handleBytes = TransformUtils.combineArrays(eachHeadBytes, eachBytes);
                    if (j != 0) {
                        lastBytes = TransformUtils.combineArrays(lastBytes, handleBytes);
                    } else {
                        lastBytes = TransformUtils.combineArrays(handleBytes);
                    }
                }
            } else {
                for (int j = 0; j < totalPackets0; j++) {
                    byte[] eachHeadBytes = new byte[]{(byte) (Integer.parseInt(Integer.toHexString(j + 1), 16))};
                    int tem = 0;
                    if (j != totalPackets0 - 1) {
                        tem = 19;
                    } else {
                        tem = bytes.length - 19 * (totalPackets0 - 1);
                    }
                    byte[] eachBytes = TransformUtils.subBytes(bytes, j * 19, tem);
                    byte[] handleBytes = TransformUtils.combineArrays(eachHeadBytes, eachBytes);
                    if (j != 0) {
                        lastBytes = TransformUtils.combineArrays(lastBytes, handleBytes);
                    } else {
                        lastBytes = TransformUtils.combineArrays(handleBytes);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lastBytes;
    }

    /**
     * @param inputStream 文件输入流
     * @return 加上序号后的字节数组
     */
    public static byte[] combinePacket(InputStream inputStream) {
        //合并后的字节数组
        byte[] lastBytes = null;
        try {
            byte[] bytes = TransformUtils.streamToByte(inputStream);
            int length = bytes.length % 20 == 0 ? (bytes.length / 20) : (bytes.length / 20 + 1);
            //由于需要带上序号,因此总的byte数组长度增大
            int totalPackets0 = (bytes.length % 19 == 0) ? (bytes.length / 19) : (bytes.length / 19 + 1);
            int crcTotalFrame = bytes.length % 3891 == 0 ? bytes.length / 3891 : bytes.length / 3891 + 1;
            byte[] crcBytes = null;
            int num = 0;
            int currentPacket = 0;
            if (bytes.length > 4 * 1024) {
                for (int j = 0; j < totalPackets0; j++) {
                    num++;
                    byte[] eachHeadBytes = new byte[]{(byte) (Integer.parseInt(Integer.toHexString(num), 16))};
                    if (num == 216) {
                        num = 0;
                    }
                    int tem = 0;
                    if (j != totalPackets0 - 1) {
                        tem = 19;
                    } else {
                        tem = bytes.length - 19 * (totalPackets0 - 1);
                    }
                    byte[] eachBytes = TransformUtils.subBytes(bytes, j * 19, tem);
                    byte[] handleBytes = TransformUtils.combineArrays(eachHeadBytes, eachBytes);
                    if (j != 0) {
                        lastBytes = TransformUtils.combineArrays(lastBytes, handleBytes);
                    } else {
                        lastBytes = TransformUtils.combineArrays(handleBytes);
                    }
                }
            } else {
                for (int j = 0; j < totalPackets0; j++) {
                    byte[] eachHeadBytes = new byte[]{(byte) (Integer.parseInt(Integer.toHexString(j + 1), 16))};
                    int tem = 0;
                    if (j != totalPackets0 - 1) {
                        tem = 19;
                    } else {
                        tem = bytes.length - 19 * (totalPackets0 - 1);
                    }
                    byte[] eachBytes = TransformUtils.subBytes(bytes, j * 19, tem);
                    byte[] handleBytes = TransformUtils.combineArrays(eachHeadBytes, eachBytes);
                    if (j != 0) {
                        lastBytes = TransformUtils.combineArrays(lastBytes, handleBytes);
                    } else {
                        lastBytes = TransformUtils.combineArrays(handleBytes);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lastBytes;
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

    public static byte[] frameHeadBytes(byte[] headData,byte[] data,int totalFrame,int currFrame){
        byte[] firstByte=dataLenght(data.length);
        byte[] middleByte=new byte[]{(byte)totalFrame,(byte)currFrame};
        byte[] endByte=new byte[]{headData[headData.length-2],headData[headData.length-1]};
        return TransformUtils.combineArrays(firstByte,middleByte,endByte);
    }

    public static byte[] frameBytes(int totalFrame, int currFrame) {
        byte[] frameByte = new byte[2];
        frameByte[0] = TransformUtils.int2byte(totalFrame);
        frameByte[1] = TransformUtils.int2byte(currFrame);
        return frameByte;
    }

    /**
     * 数据帧帧头
     *
     * @param dataLength 字节长度
     * @param totalFrame 总帧数
     * @param currFrame  当前帧
     * @return
     */
    public static byte[] eachFrameFirstPacket(int dataLength, int totalFrame, int currFrame) {
        Log.e("dataLength---", dataLength + "");
        byte[] frameOrderByte = new byte[7];
        int l = dataLength / 256;
        int m = dataLength % 256;
        frameOrderByte[0] = TransformUtils.hexToByte("FD");
        frameOrderByte[1] = TransformUtils.int2byte(l);
        frameOrderByte[2] = TransformUtils.int2byte(m);
        frameOrderByte[3] = TransformUtils.int2byte(totalFrame);
        frameOrderByte[4] = TransformUtils.int2byte(currFrame);
        frameOrderByte[5] = TransformUtils.hexToByte("20");
        frameOrderByte[6] = TransformUtils.hexToByte("03");
        return frameOrderByte;
    }

    /**
     * 文件字节数少于13,不需要分包,直接在帧头末尾拼接即可
     *
     * @param data
     * @return
     */
    public static byte[] eachFrameBytes(byte[] data) {
        byte[] headBytes = new byte[]{(byte)0xFD, 0, (byte) data.length, 0, 0, (byte) 0x20, (byte) 0x03};
        return TransformUtils.combineArrays(headBytes, data);
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
                byte[] eachPacket = TransformUtils.subBytes(data, num * MAX_BYTES, MAX_BYTES);
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
