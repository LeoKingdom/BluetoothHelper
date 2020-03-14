package com.ly.qcommesim.core.utils;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/9/24 13:42
 * version: 1.0
 */
public class ActionUtils {

    //---------------------------------升级action start ------------------------------
    //升级类型
    public static final int ACTION_UPGRADE_9205 =10001 ;//升级9205
    public static final int ACTION_UPGRADE_BLE =10002 ;//升级蓝牙芯片
    public static final int ACTION_UPGRADE_BLE_AND_9205 =10003 ;//两者都升级
    public static final String ACTION_DEVICE_SCAN = "ACTION_DEVICE_SCAN";
    public static final String ACTION_DEVICE_CONN = "ACTION_DEVICE_CONN";
    public static final int ACTION_OTA_ORDER_I = 0; //bin文件的正确性校验
    public static final int ACTION_OTA_DATA_HEAD_I = 1; //数据帧帧头命令
    public static final int ACTION_OTA_DATA_DATA_I = 2; //数据帧
    public static final int ACTION_OTA_DATA_LOSE_I = 3; //数据帧丢包
    public static final int ACTION_OTA_DATA_RESEND_I = 4; //重发数据帧,丢包过多
    public static final String ACTION_OTA_DATA_S = "ACTION_OTA_DATA";
    public static final String ACTION_SCAN_SUCCESS_S = "ACTION_SCAN_SUCCESS";
    public static final int ACTION_SCAN_SUCCESS_I = 5;
    public static final String ACTION_SCAN_FAIL_S = "ACTION_SCAN_FAIL";
    public static final int ACTION_SCAN_FAIL_I = 6;
    public static final String ACTION_CONNECT_SUCCESS_S = "ACTION_CONNECT_SUCCESS";
    public static final int ACTION_CONNECT_SUCCESS_I = 7;
    public static final String ACTION_CONNECT_FAIL_S = "ACTION_CONNECT_FAIL";
    public static final int ACTION_CONNECT_FAIL_I = 8;
    public static final int ACTION_DISCONNECT_I = 10;
    public static final String ACTION_DISCONNECT_S = "ACTION_DISCONNECT_S";
    public static final int ACTION_OTA_NOTIFY = 9; //开启通知(蓝牙)
    public static final int ACTION_NOTIFY = 14; //开启通知(蓝牙)
    public static final int ACTION_OTA_VALIFY_OUTTIME = 11; //bin校验超时
    public static final int ACTION_OTA_PACK_INTERVAL_TIME = 12; //
    public static final int ACTION_OTA_NEXT_BIN = 13; //下一个bin数据流
    public static final int ACTION_OTA_RECONNECT = 14; //断开连接
    public static final int ACTION_OTA_RECONNECT_SEND = 15; //断开连接,重连继续发送
    public static final int ACTION_DEVICE_RECONNECT = 16;//重新连接设备
    public static final int ACTION_OPEN_NOTIFY = 17;
    //先取消配对再连接,防止扫描不到而无法连接
    //为什么不直接连接? --->当设备不在范围内或其他原因,直接连接会失败,因为原因可能很多,所以需要做很多判断,故采取暴力解决的办法
    public static final int ACTION_UNPAIR_AND_CONNECT = 18;
    //---------------------------------升级action end ------------------------------

    //---------------------------------Esim激活action start ------------------------------
    public static final int ACTION_ESIM_ACTIVE_FIRST=19; //激活esim第一步
    public static final int ACTION_ESIM_ACTIVE_NEXT=20; //激活esim第二、三步
    public static final int ACTION_ESIM_ACTIVE_FORTH=22; //激活esim第四步
    public static final int ACTION_ESIM_ACTIVE=23; //激活esim,真正激活
    public static final int ACTION_ESIM_UNACTIVE=24; //去活esim
    public static final int ACTION_ESIM_URL=25; //设置激活esim的url
    public static final int ACTION_ESIM_PROFILE_DELETE = 26; //删除下载的 profile
    //---------------------------------Esim激活action end ------------------------------
}
