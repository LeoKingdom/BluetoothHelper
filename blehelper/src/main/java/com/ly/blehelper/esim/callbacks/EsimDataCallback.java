package com.ly.blehelper.esim.callbacks;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/12/16 10:46
 * version: 1.0
 */
public class EsimDataCallback {
    public interface EsimUrlListener{
        void urlSuccess(int currentStep, String url);
        void urlFail(String des);
    }

    public interface EsimUrlPostListener{
        void urlPostSuccess(int currentStep, String json);
        void urlPostFail(String des);
        void profileSuccess(int code);//非0为失败
    }

}
