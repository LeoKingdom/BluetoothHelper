package com.ly.createaar;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.de.esim.rohttp.Rohttp;
import com.de.esim.rohttp.helper.callback.HttpCallback;
import com.de.esim.rohttp.helper.utils.TrustAllCerts;

import java.util.HashMap;

import androidx.annotation.Nullable;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/12/31 13:48
 * version: 1.0
 */
public class Main extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initHttp(getApplication());
    }

    public void toEsim(View view){
        testEsim();
        //startActivity(new Intent(this,EsimActivity.class));
    }
    public void toUpgrade(View view){
//        startActivity(new Intent(this, UpgradeActivity.class));
    }
    public void toGTUpgrade(View view){
        startActivity(new Intent(this, QcommUpgradeActivity2.class));
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

    private static final String mUrl = "https://2f72b92a.cpolar.cn/gsma/rsp2/es9plus/initiateAuthentication";
    //请求体
    private static final String JSONString = "{" +
            "\"euiccInfo1\": \"vyBhggMCAgCpLAQUZloUM9Z8GixduLUsln8QoFe6XLIEFPOD/jxllz2v0A6m3YpLU9YkU9asqiwEFGZaFDPWfBosXbi1LJZ/EKBXulyyBBTzg/48ZZc9r9AOpt2KS1PWJFPWrA==\"," +
            "\"euiccChallenge\": \"CaGd54Fy2RXTjohlJp43WQ==\"," +
            "\"smdpAddress\": \"2f72b92a.cpolar.cn\"" +
            "}";

    /**
     * 测试Esim模块
     */
    private void testEsim(){
        new Rohttp<String>()
                .roBaseUrl(mUrl)
                //.roParam(new EsimBean())
                .roParam(JSONString)
                .roPost(new HttpCallback<String>() {

                    @Override
                    public void onFinished() {
                        Log.e("yhd-","onFinished");
                    }

                    @Override
                    public void onSuccess(String result) {
                        Log.e("yhd-",result);
                    }

                    @Override
                    public void onFailed(Rohttp.FAILED failed, Throwable ex) {
                        Log.e("yhd-","failed："+failed.toString()+" ex:"+ex.toString());
                    }
                });
    }
}
