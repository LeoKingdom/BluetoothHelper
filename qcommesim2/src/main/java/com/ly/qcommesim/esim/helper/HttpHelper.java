package com.ly.qcommesim.esim.helper;

import android.app.Application;

import com.de.esim.rohttp.Rohttp;
import com.de.esim.rohttp.helper.callback.HttpCallback;

public class HttpHelper {
    private HttpFinishListener finishListener;
    private HttpFailListener failListener;
    private HttpSuccessListener successListener;

    public HttpHelper(Application application) {
        Rohttp.initTrustAll(application);
    }

    /**
     * http请求
     *
     * @param url
     * @param param
     */
    public void httpRequest(String url, String param) {
        Rohttp rohttp = new Rohttp();
        rohttp.roBaseUrl(url);
        rohttp.roParam(param);
        rohttp.roPost(new HttpCallback<String>() {
            @Override
            public void onSuccess(String result) {
                successNext(result);
            }

            @Override
            public void onFailed(Rohttp.FAILED failed, Throwable throwable) {
                failNext(failed, throwable);
            }

            @Override
            public void onFinished() {
                finishNext();
            }
        });
    }

    private void successNext(String result) {
        if (successListener != null) {
            successListener.success(result);
        }
    }

    private void failNext(Rohttp.FAILED failed, Throwable throwable) {
        if (failListener != null) {
            failListener.fail(failed, throwable);
        }
    }

    private void finishNext() {
        if (finishListener != null) {
            finishListener.finish();
        }
    }

    public void setFinishListener(HttpFinishListener finishListener) {
        this.finishListener = finishListener;
    }

    public void setFailListener(HttpFailListener failListener) {
        this.failListener = failListener;
    }

    public void setSuccessListener(HttpSuccessListener successListener) {
        this.successListener = successListener;
    }

    public interface HttpSuccessListener {
        void success(String result);
    }

    public interface HttpFailListener {
        void fail(Rohttp.FAILED failed, Throwable throwable);
    }

    public interface HttpFinishListener {
        void finish();
    }
}
