package com.ly.createaar;

import android.app.Application;

import com.de.esim.rohttp.Rohttp;
import com.de.esim.rohttp.helper.utils.TrustAllCerts;

import java.util.HashMap;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
//        initHttp(this);
        Rohttp.initTrustAll(this);
    }


    /**
     * 初始化网络协议
     */
    private void initHttp(Application application){
        //初始化Application
        Rohttp.init(application);
        //定义全局请求头
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json");
        //设置请求头
        Rohttp.HEADER = headerMap;
        //发送https请求时信任所有证书
        Rohttp.SSL_SOCKET_FACTORY = TrustAllCerts.createSSLSocketFactory();
        //发送https请求时信任所有域名
        Rohttp.HOSTNAME_VERRIFY = new TrustAllCerts.TrustAllHostnameVerifier();
    }
}
