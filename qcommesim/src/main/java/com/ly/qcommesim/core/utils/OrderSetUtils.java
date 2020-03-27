package com.ly.qcommesim.core.utils;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/9/19 9:23
 * version: 1.0
 * <p>
 * IOT通讯指令集,内部使用
 */
public class OrderSetUtils {
    public static final byte[] ORDER_HEADER = {(byte) 0xFD}; //头部指令
    //    public static final byte[] ORDER_OAD={(byte)0xFD,0,7,0,0,32,2,1,1}; //进入ota升级指令,最后两个字节表示升级的bin文件(有可能有多个),total和current
    public static final byte[] ORDER_LOCATION = {(byte) 0xFD, 0, 0, 0, 0, 16, 5}; //获取定位指令
    public static final byte[] ORDER_LOCATION_SUCCESS = {(byte) 0xFD, 0, 9, 0, 0, 16, 5, 0}; //成功获取定位指令
    public static final byte[] ORDER_LOCATION_FAIL = {(byte) 0xFD, 0, 1, 0, 0, 16, 5, 1}; //获取定位失败指令
    public static final byte[] ORDER_VERSION = {(byte) 0xFD, 0, 0, 0, 0, 32, 1}; //获取设备版本号指令
    public static final byte[] ORDER_OAD = {(byte) 0xFD, 0, 7, 0, 0, 32, 2}; //进入ota升级指令,最后两个字节表示升级的bin文件(有可能有多个),total和current
    public static final byte[] ORDER_OAD_DATA_SEND = {(byte) 0xFD, 0, 0, 0, 0, 32, 3}; //准备发送ota升级包指令
    /***************************ESIM对应指令******************************/
    public static final byte[] ESIM_EID = {(byte) 0xFD, 0, 0, 0, 0, 48, 1}; //获取EID指令
    public static final byte[] ESIM_IMEI = {(byte) 0xFD, 0, 0, 0, 0, 48, 2}; //获取IMEI指令
    //第一次 start
    public static final byte[] ESIM_PROFILE_START = {(byte) 0xFD, 0, 0, 0, 0, 48, 80}; //下载Profile指令  开始 app->tracker
    public static final byte[] ESIM_PROFILE_URL_RESP = {(byte) 0xFD, 0, 0, 0, 0, 48, 81}; //profile url回复  第一次 tracker->app
    public static final byte[] ESIM_PROFILE_URL_RESP_ACK_R = {(byte) 0xFD, 0, 2, 0, 0, 48, 81, 0, 0}; //profile url确认包正确  app->tracker
    public static final byte[] ESIM_PROFILE_URL_RESP_ACK_E = {(byte) 0xFD, 0, 2, 0, 0, 48, 81, 0, 1}; //profile url确认包错误  app->tracker
    public static final byte[] ESIM_PROFILE_POST_RESP = {(byte) 0xFD, 0, 0, 0, 0, 48, 82}; //profile post回复  tracker->app 附:这一步之后进行第一次网络请求
    public static final byte[] ESIM_PROFILE_POST_SEND = {(byte) 0xFD, 0, 0, 0, 0, 48, 83}; //profile post传输  app->tracker 向tracker传输第一次网络请求返回的jsonBody
    //end
    //第二次 start
//    public static final byte[] ESIM_PROFILE_URL_RESP={(byte)0xFD,0,0,0,0,48,81}; //profile url回复  第一次 tracker->app
//    public static final byte[] ESIM_PROFILE_URL_RESP_ACK_R={(byte)0xFD,0,2,0,0,48,81,0,0}; //profile url确认包正确  app->tracker
//    public static final byte[] ESIM_PROFILE_URL_RESP_ACK_E={(byte)0xFD,0,2,0,0,48,81,0,1}; //profile url确认包错误  app->tracker
//    public static final byte[] ESIM_PROFILE_POST_RESP={(byte)0xFD,0,0,0,0,48,82}; //profile post回复  tracker->app 附:这一步之后进行第一次网络请求
    //end
    public static final byte[] ESIM_PROFILE_DOWNLOAD = {(byte) 0xFD, 0, 0, 1, 1, 48, 3}; //下载Profile指令  app->ble
    public static final byte[] ESIM_PROFILE_DOWNLOAD_URL_RESP = {(byte) 0xFD, 0, 0, 0, 0, 48, 4}; //下载Profile 的URL回复指令  ble->app
    public static final byte[] ESIM_PROFILE_DOWNLOAD_POST_RESP = {(byte) 0xFD, 0, 0, 0, 0, 48, 5}; //下载Profile 的post请求参数回复指令  ble->app
    public static final byte[] ESIM_ACTIVE_NOTICE_RESP = {(byte) 0xFD, 0, 0, 0, 0, 48, 6}; //是否激活通知回复指令  ble->app
    public static final byte[] ESIM_INFO = {(byte) 0xFD, 0, 0, 0, 0, 48, 7}; //获取ESIM信息指令
    public static final byte[] ESIM_ACTIVE = {(byte) 0xFD, 0, 0, 0, 0, 48, 8}; //激活ESIM指令
    public static final byte[] ESIM_CANCEL = {(byte) 0xFD, 0, 0, 0, 0, 48, 9}; //去活ESIM指令
    public static final byte[] ESIM_PROFILE_DELETE = {(byte) 0xFD, 0, 0, 0, 0, 48, 10}; //删除Profile指令
    public static final byte[] ESIM_SET_NICKNAME = {(byte) 0xFD, 0, 0, 0, 0, 48, 11}; //设置ESIM昵称指令
    public static final byte[] ESIM_SERVER_DOMAIN = {(byte) 0xFD, 0, 0, 0, 0, 48, 12}; //获取服务器地址指令
    public static final byte[] ESIM_SM_DP = {(byte) 0xFD, 0, 0, 0, 0, 48, 13}; //设置SM-DP地址指令
    public static final byte[] ESIM_SMDS = {(byte) 0xFD, 0, 0, 0, 0, 48, 14}; //设置SMDS地址指令
//    public static final byte[] ORDER_NOTICE_DELETE={(byte)0xFD,0,0,0,0,48,13}; //删除通知指令
//    public static final byte[] ORDER_ESIM_RESTORE={(byte)0xFD,0,0,0,0,48,14}; //恢复ESIM出厂设置指令

}
