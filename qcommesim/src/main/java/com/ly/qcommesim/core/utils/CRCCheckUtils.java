package com.ly.qcommesim.core.utils;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/9/27 17:38
 * version: 1.0
 */
public class CRCCheckUtils {
    static byte[] crc8_tab0 = {(byte) 0, (byte) 94, (byte) 188, (byte) 226, (byte) 97, (byte) 63, (byte) 221, (byte) 131, (byte) 194, (byte) 156, (byte) 126, (byte) 32, (byte) 163, (byte) 253, (byte) 31, (byte) 65, (byte) 157, (byte) 195, (byte) 33, (byte) 127, (byte) 252, (byte) 162, (byte) 64, (byte) 30, (byte) 95, (byte) 1, (byte) 227, (byte) 189, (byte) 62, (byte) 96, (byte) 130, (byte) 220, (byte) 35, (byte) 125, (byte) 159, (byte) 193, (byte) 66, (byte) 28, (byte) 254, (byte) 160, (byte) 225, (byte) 191, (byte) 93, (byte) 3, (byte) 128, (byte) 222, (byte) 60, (byte) 98, (byte) 190, (byte) 224, (byte) 2, (byte) 92, (byte) 223, (byte) 129, (byte) 99, (byte) 61, (byte) 124, (byte) 34, (byte) 192, (byte) 158, (byte) 29, (byte) 67, (byte) 161, (byte) 255, (byte) 70, (byte) 24,
            (byte) 250, (byte) 164, (byte) 39, (byte) 121, (byte) 155, (byte) 197, (byte) 132, (byte) 218, (byte) 56, (byte) 102, (byte) 229, (byte) 187, (byte) 89, (byte) 7, (byte) 219, (byte) 133, (byte) 103, (byte) 57, (byte) 186, (byte) 228, (byte) 6, (byte) 88, (byte) 25, (byte) 71, (byte) 165, (byte) 251, (byte) 120, (byte) 38, (byte) 196, (byte) 154, (byte) 101, (byte) 59, (byte) 217, (byte) 135, (byte) 4, (byte) 90, (byte) 184, (byte) 230, (byte) 167, (byte) 249, (byte) 27, (byte) 69, (byte) 198, (byte) 152, (byte) 122, (byte) 36, (byte) 248, (byte) 166, (byte) 68, (byte) 26, (byte) 153, (byte) 199, (byte) 37, (byte) 123, (byte) 58, (byte) 100, (byte) 134, (byte) 216, (byte) 91, (byte) 5, (byte) 231, (byte) 185, (byte) 140, (byte) 210, (byte) 48, (byte) 110, (byte) 237,
            (byte) 179, (byte) 81, (byte) 15, (byte) 78, (byte) 16, (byte) 242, (byte) 172, (byte) 47, (byte) 113, (byte) 147, (byte) 205, (byte) 17, (byte) 79, (byte) 173, (byte) 243, (byte) 112, (byte) 46, (byte) 204, (byte) 146, (byte) 211, (byte) 141, (byte) 111, (byte) 49, (byte) 178, (byte) 236, (byte) 14, (byte) 80, (byte) 175, (byte) 241, (byte) 19, (byte) 77, (byte) 206, (byte) 144, (byte) 114, (byte) 44, (byte) 109, (byte) 51, (byte) 209, (byte) 143, (byte) 12, (byte) 82, (byte) 176, (byte) 238, (byte) 50, (byte) 108, (byte) 142, (byte) 208, (byte) 83, (byte) 13, (byte) 239, (byte) 177, (byte) 240, (byte) 174, (byte) 76, (byte) 18, (byte) 145, (byte) 207, (byte) 45, (byte) 115, (byte) 202, (byte) 148, (byte) 118, (byte) 40, (byte) 171, (byte) 245, (byte) 23, (byte) 73, (byte) 8,
            (byte) 86, (byte) 180, (byte) 234, (byte) 105, (byte) 55, (byte) 213, (byte) 139, (byte) 87, (byte) 9, (byte) 235, (byte) 181, (byte) 54, (byte) 104, (byte) 138, (byte) 212, (byte) 149, (byte) 203, (byte) 41, (byte) 119, (byte) 244, (byte) 170, (byte) 72, (byte) 22, (byte) 233, (byte) 183, (byte) 85, (byte) 11, (byte) 136, (byte) 214, (byte) 52, (byte) 106, (byte) 43, (byte) 117, (byte) 151, (byte) 201, (byte) 74, (byte) 20, (byte) 246, (byte) 168, (byte) 116, (byte) 42, (byte) 200, (byte) 150, (byte) 21, (byte) 75, (byte) 169, (byte) 247, (byte) 182, (byte) 232, (byte) 10, (byte) 84, (byte) 215, (byte) 137, (byte) 107, 53};
    static byte[] crc8_tab = {
            (byte)0x00, (byte)0x07, (byte)0x0e, (byte)0x09, (byte)0x1c, (byte)0x1b, (byte)0x12, (byte)0x15, (byte)0x38,
            (byte)0x3f, (byte)0x36, (byte)0x31, (byte)0x24, (byte)0x23, (byte)0x2a, (byte)0x2d, (byte)0x70, (byte)0x77,
            (byte)0x7e, (byte)0x79, (byte)0x6c, (byte)0x6b, (byte)0x62, (byte)0x65, (byte)0x48, (byte)0x4f, (byte)0x46,
            (byte)0x41, (byte)0x54, (byte)0x53, (byte)0x5a, (byte)0x5d, (byte)0xe0, (byte)0xe7, (byte)0xee, (byte)0xe9,
            (byte)0xfc, (byte)0xfb, (byte)0xf2, (byte)0xf5, (byte)0xd8, (byte)0xdf, (byte)0xd6, (byte)0xd1, (byte)0xc4,
            (byte)0xc3, (byte)0xca, (byte)0xcd, (byte)0x90, (byte)0x97, (byte)0x9e, (byte)0x99, (byte)0x8c, (byte)0x8b,
            (byte)0x82, (byte)0x85, (byte)0xa8, (byte)0xaf, (byte)0xa6, (byte)0xa1, (byte)0xb4, (byte)0xb3, (byte)0xba,
            (byte)0xbd, (byte)0xc7, (byte)0xc0, (byte)0xc9, (byte)0xce, (byte)0xdb, (byte)0xdc, (byte)0xd5, (byte)0xd2,
            (byte)0xff, (byte)0xf8, (byte)0xf1, (byte)0xf6, (byte)0xe3, (byte)0xe4, (byte)0xed, (byte)0xea, (byte)0xb7,
            (byte)0xb0, (byte)0xb9, (byte)0xbe, (byte)0xab, (byte)0xac, (byte)0xa5, (byte)0xa2, (byte)0x8f, (byte)0x88,
            (byte)0x81, (byte)0x86, (byte)0x93, (byte)0x94, (byte)0x9d, (byte)0x9a, (byte)0x27, (byte)0x20, (byte)0x29,
            (byte)0x2e, (byte)0x3b, (byte)0x3c, (byte)0x35, (byte)0x32, (byte)0x1f, (byte)0x18, (byte)0x11, (byte)0x16,
            (byte)0x03, (byte)0x04, (byte)0x0d, (byte)0x0a, (byte)0x57, (byte)0x50, (byte)0x59, (byte)0x5e, (byte)0x4b,
            (byte)0x4c, (byte)0x45, (byte)0x42, (byte)0x6f, (byte)0x68, (byte)0x61, (byte)0x66, (byte)0x73, (byte)0x74,
            (byte)0x7d, (byte)0x7a, (byte)0x89, (byte)0x8e, (byte)0x87, (byte)0x80, (byte)0x95, (byte)0x92, (byte)0x9b,
            (byte)0x9c, (byte)0xb1, (byte)0xb6, (byte)0xbf, (byte)0xb8, (byte)0xad, (byte)0xaa, (byte)0xa3, (byte)0xa4,
            (byte)0xf9, (byte)0xfe, (byte)0xf7, (byte)0xf0, (byte)0xe5, (byte)0xe2, (byte)0xeb, (byte)0xec, (byte)0xc1,
            (byte)0xc6, (byte)0xcf, (byte)0xc8, (byte)0xdd, (byte)0xda, (byte)0xd3, (byte)0xd4, (byte)0x69, (byte)0x6e,
            (byte)0x67, (byte)0x60, (byte)0x75, (byte)0x72, (byte)0x7b, (byte)0x7c, (byte)0x51, (byte)0x56, (byte)0x5f,
            (byte)0x58, (byte)0x4d, (byte)0x4a, (byte)0x43, (byte)0x44, (byte)0x19, (byte)0x1e, (byte)0x17, (byte)0x10,
            (byte)0x05, (byte)0x02, (byte)0x0b, (byte)0x0c, (byte)0x21, (byte)0x26, (byte)0x2f, (byte)0x28, (byte)0x3d,
            (byte)0x3a, (byte)0x33, (byte)0x34, (byte)0x4e, (byte)0x49, (byte)0x40, (byte)0x47, (byte)0x52, (byte)0x55,
            (byte)0x5c, (byte)0x5b, (byte)0x76, (byte)0x71, (byte)0x78, (byte)0x7f, (byte)0x6a, (byte)0x6d, (byte)0x64,
            (byte)0x63, (byte)0x3e, (byte)0x39, (byte)0x30, (byte)0x37, (byte)0x22, (byte)0x25, (byte)0x2c, (byte)0x2b,
            (byte)0x06, (byte)0x01, (byte)0x08, (byte)0x0f, (byte)0x1a, (byte)0x1d, (byte)0x14, (byte)0x13, (byte)0xae,
            (byte)0xa9, (byte)0xa0, (byte)0xa7, (byte)0xb2, (byte)0xb5, (byte)0xbc, (byte)0xbb, (byte)0x96, (byte)0x91,
            (byte)0x98, (byte)0x9f, (byte)0x8a, (byte)0x8d, (byte)0x84, (byte)0x83, (byte)0xde, (byte)0xd9, (byte)0xd0,
            (byte)0xd7, (byte)0xc2, (byte)0xc5, (byte)0xcc, (byte)0xcb, (byte)0xe6, (byte)0xe1, (byte)0xe8, (byte)0xef,
            (byte)0xfa, (byte)0xfd, (byte)0xf4, (byte)0xf3
    };

    /**
     * 计算数组的CRC8校验值
     *
     * @param data 需要计算的数组
     * @return CRC8校验值
     */
    public static byte calcCrc8(byte[] data) {
        return calcCrc8(data, 0, data.length, (byte) 0);
    }

    /**
     * 计算CRC8校验值
     *
     * @param data   数据
     * @param offset 起始位置
     * @param len    长度
     * @return 校验值
     */
    public static byte calcCrc8(byte[] data, int offset, int len) {
        return calcCrc8(data, offset, len, (byte) 0);
    }

    /**
     * 计算CRC8校验值
     *
     * @param data   数据
     * @param offset 起始位置
     * @param len    长度
     * @param preval 之前的校验值
     * @return 校验值
     */
    public static byte calcCrc8(byte[] data, int offset, int len, byte preval) {
        byte ret = preval;
        for (int i = offset; i < (offset + len); ++i) {
            ret = crc8_tab0[(0x00ff & (ret ^ data[i]))];
        }
        return ret;
    }

}
