package com.ly.qcommesim.core.utils;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/9/24 13:42
 * version: 1.0
 */
public class ActionUtils {


    //---------------------------------Esim激活action start ------------------------------
    public static final int DATA_CHECK = 1001;//校验数据
    public static final int URL_RESPONSE = 1002;//回复url数据是否正确
    public static final int JSON_BODY_EACH_FRAME = 1003;//jsonBody数据分帧
    public static final int BEGIN_URL_TRANSFORM = 1005;//开始传输url
    public static final int ACTION_ESIM_ACTIVE_FIRST=19; //激活esim第一步
    public static final int ACTION_ESIM_ACTIVE_NEXT=20; //激活esim第二、三步
    public static final int ACTION_ESIM_ACTIVE_FORTH=22; //激活esim第四步
    public static final int ACTION_ESIM_ACTIVE=23; //激活esim,真正激活
    public static final int ACTION_ESIM_UNACTIVE=24; //去活esim
    public static final int ACTION_ESIM_URL=25; //设置激活esim的url
    public static final int ACTION_ESIM_PROFILE_DELETE = 26; //删除下载的 profile
    //---------------------------------Esim激活action end ------------------------------
}
