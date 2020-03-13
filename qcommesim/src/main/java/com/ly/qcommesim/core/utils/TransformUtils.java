package com.ly.qcommesim.core.utils;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/9/16 10:21
 * version: 1.0
 * <p>
 * 字节  字符串等转换工具
 */
public class TransformUtils {

    /**
     * 将十进制数转换成两个字节长度的十六进制字符串
     *
     * @param num 转换的数
     * @return
     */
    public static byte[] int2TwoBytes(int num) {
        byte[] numBytes=int2ByteArray(num);
        if (num < 256) return combineArrays(new byte[]{0},numBytes);
        if (num < 65535) return numBytes;
        return null;
    }

    /**
     * 字节数组转换为16进制字符串
     *
     * @param b 转换的字节数组
     */
    public static String bytesToHexString(byte[] b) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() < 2) {
                stringBuffer.append(0);
            }
            stringBuffer.append(hex.toUpperCase());
            if (i != b.length - 1) {
                stringBuffer.append(" ");
            }
        }
        return stringBuffer.toString();
    }

    public static int byte2Int(byte b) {
        return b & 0xFF;
    }

    public static String bytes2String(byte[] bytes) {
        String str = null;
        try {
            str = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e("byte2String--", e.getMessage());
        }
        return str;
    }

    public static byte[] int2ByteArray(int num) {
        String hexString = int2hex(num);
        return hexToByteArray(hexString);
    }

    /**
     * 字符串转为16进制字节数组
     *
     * @param string 转换的字符串
     * @return
     */

    public static byte[] getHexBytes(String string) {
        int len = string.length() / 2;
        char[] chars = string.toCharArray();
        String[] hexStr = new String[len];
        byte[] bytes = new byte[len];
        for (int i = 0, j = 0; j < len; i += 2, j++) {
            hexStr[j] = "" + chars[i] + chars[i + 1];
            bytes[j] = (byte) Integer.parseInt(hexStr[j], 16);
        }
        return bytes;
    }


    /**
     * Hex字符串转byte
     *
     * @param inHex 待转换的Hex字符串
     * @return 转换后的byte
     */
    public static byte hexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }

    /**
     * hex字符串转byte数组
     *
     * @param inHex 待转换的Hex字符串
     * @return 转换后的byte数组结果
     */
    public static byte[] hexToByteArray(String inHex) {
        int hexlen = inHex.length();
        byte[] result;
        if (hexlen % 2 == 1) {
            //奇数
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {
            //偶数
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = hexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }

    /**
     * 十六进制字符串转字符串
     *
     * @param hexString
     * @return
     */
    public static String hexString2String(String hexString) {
        return bytes2String(hexToByteArray(hexString));
    }

    public static String string2HexString(String string) {
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = string.getBytes();
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
            // sb.append(' ');
        }
        return sb.toString().trim();
    }

    public static String hexInt2String(int hexInt) {
        String string = null;
        string = String.valueOf(Integer.parseInt(String.valueOf(hexInt), 16));
        return string;
    }

    public static String int2hex(int num) {
        return Integer.toHexString(num);
    }

    public static byte int2byte(int num) {
        String hexString = int2hex(num);
        return hexToByte(hexString);
    }

    public static int hex2int(String hexString) {
        return Integer.parseInt(hexString, 16);
    }

    // 从文件中读取字节流
    public synchronized static byte[] fileToByte(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        int count = inputStream.available();
        if (count == 0) {
            return null;
        }
        byte[] b = new byte[count];
        inputStream.read(b);
        return b;
    }

    // 从文件流中读取字节流
    public synchronized static byte[] streamToByte(InputStream stream) throws IOException {
        int count = stream.available();
        if (count == 0) {
            return null;
        }
        byte[] b = new byte[count];
        stream.read(b);
        return b;
    }

    /**
     * 截取byte数组   不改变原数组
     *
     * @param b      原数组
     * @param off    偏差值（索引）
     * @param length 长度
     * @return 截取后的数组
     */
    public static byte[] subBytes(byte[] b, int off, int length) {
        byte[] b1 = new byte[length];
        System.arraycopy(b, off, b1, 0, length);
        return b1;
    }

    /**
     * 合并多个字节数组
     *
     * @param a 字节数组,可变
     * @return 合并后的数组
     */

    public static byte[] combineArrays(byte[]... a) {
        int massLength = 0;
        for (byte[] b : a) {
            massLength += b.length;
        }
        byte[] c = new byte[massLength];
        byte[] d;
        int index = 0;
        for (byte[] anA : a) {
            d = anA;
            System.arraycopy(d, 0, c, 0 + index, d.length);
            index += d.length;
        }
        return c;
    }

    public static byte[] string2ByteArray(String arrayString){
        if (arrayString.contains("[")&&arrayString.contains(",")){
            arrayString=arrayString.substring(1,arrayString.length()-1);
            String[] strArray=arrayString.split(",");
            byte[] array=new byte[strArray.length];
            for (int i=0;i<strArray.length;i++){
                String s=strArray[i];
                if (s.contains(" ")) s=s.replace(" ","");
                int b = Integer.parseInt(s);
                array[i]= (byte) b;
            }
            return array;
        }
        return null;
    }

}
