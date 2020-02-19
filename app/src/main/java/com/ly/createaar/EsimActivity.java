package com.ly.createaar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.ly.blehelper.esim.callbacks.EsimActiveCallback;
import com.ly.blehelper.esim.callbacks.EsimCancelCallback;
import com.ly.blehelper.esim.callbacks.EsimDataCallback;
import com.ly.blehelper.esim.callbacks.EsimProfileDeleteCallback;
import com.ly.blehelper.esim.helper.ESimActiveHelper;
import com.ly.blehelper.upgrade_core.fota_upgrade.utils.ActionUtils;
import com.ly.blehelper.utils.RetrofitWithCerUtils;
import com.ly.blehelper.widget.LoadingWidget;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import fastble.data.BleDevice;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class EsimActivity extends FragmentActivity {

    private BleDevice bleDevice;
    private ESimActiveHelper eSimHelper;
    private TextView showTxt;
    private String mUrl;
    private LoadingWidget loadingWidget;
    private EditText urlInput;
    private String testMac="88:9E:33:EE:A7:8A";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esim);
//        EventBus.getDefault().register(this);
        showTxt = findViewById(R.id.write);
        loadingWidget=findViewById(R.id.main_loading_widget);
        urlInput=findViewById(R.id.url_input);
        checkLocation();
        eSimHelper = new ESimActiveHelper(getApplication());
        setCallback();
    }

    private void setCallback() {

        eSimHelper.setEsimCancelCallback(new EsimCancelCallback() {
            @Override
            public void cancelResult(boolean isActivated) {
                if (isActivated) {
                    toast("Esim去活成功");
                } else {
                    toast("Esim去活失败");
                }
            }
        });
        eSimHelper.setEsimActiveCallback(new EsimActiveCallback() {
            @Override
            public void deviceNotFound() {
                super.deviceNotFound();
                //未能连接上设备的逻辑
                toast("未找到设备");
                loadingWidget.hide();
            }

            @Override
            public void activeResult(boolean isActivated) {
                //激活是否成功
                if (isActivated) {
                    toast("Esim激活成功");
                } else {
                    loadingWidget.hide();
                    toast("Esim激活失败");
                }
            }

            @Override
            public void notifyCallback(byte[] data) {
                super.notifyCallback(data);
                showTxt.setText(Arrays.toString(data)+"");
            }
        });
        eSimHelper.setEsimUrlListener(new EsimDataCallback.EsimUrlListener() {
            @Override
            public void urlSuccess(int step, String url) {
                Log.e("url----", url);
                mUrl = url;
                loadingWidget.setLoadingText("Downloading...");
                loadingWidget.show();
            }

            @Override
            public void urlFail(String des) {
                loadingWidget.hide();
            }
        });

        eSimHelper.setEsimUrlPostListener(new EsimDataCallback.EsimUrlPostListener() {
            @Override
            public void urlPostSuccess(int step, String json) {
                Log.e("post----", json);
                postToServer(json);
            }

            @Override
            public void urlPostFail(String des) {
                loadingWidget.hide();
            }

            @Override
            public void profileSuccess(int code) {
                loadingWidget.hide();
                if (code == 0) {
                    toast("profile下载成功");
                } else {
                    toast("profile下载失败");
                }
            }
        });

        eSimHelper.setEsimProfileDeleteCallback(new EsimProfileDeleteCallback() {
            @Override
            public void deleteResult(boolean isActivated) {
                loadingWidget.hide();
                if (isActivated) {
                    toast("profile删除成功");
                } else {
                    toast("profile删除失败");
                }
            }
        });
    }

    // todo
    private boolean checkLocation() {
        if (Build.VERSION.SDK_INT >= 23) {
            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        0x0010
                );
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    // todo
    private void toast(String msg) {
        Toast.makeText(this, "" + msg, Toast.LENGTH_LONG).show();
    }

    public void urlSet(View view){
        String url=urlInput.getText().toString();
        eSimHelper.setUrl(url);
    }

    public void deleteProfile(View view){
        loadingWidget.show();
        eSimHelper.deleteProfile(testMac);
    }

    /**
     * 设置激活的运营商(服务器)url
     * @param view
     */
    public void setUrl(View view){
        eSimHelper.setSMDPUrl(testMac);
    }

    /**
     *
     * 激活esim
     * @param view
     */
    public void notifyEsim(View view) {
        eSimHelper.esimActive(testMac);
    }
    /**
     *
     * 激活esim
     * @param view
     */
    public void cancelEsim(View view) {
        eSimHelper.esimCancel(testMac);
    }
    /**
     * 准备profile
     * @param view
     */
    public void activeEsim(View view) {
        loadingWidget.setLoadingText("Loading...");
        loadingWidget.show();
        eSimHelper.esimActiveFirst(testMac);
    }

    private void postToServer(String json) {
        String url = mUrl.substring(mUrl.lastIndexOf("/") + 1);
        String baseUrl = mUrl.substring(0, mUrl.lastIndexOf("/") + 1);
        SSLContext sslContext = RetrofitWithCerUtils.getSslContextForCertificateFile(this, "esim_https.cer");
        OkHttpClient client = null;
        try {
            X509TrustManager trustManager = RetrofitWithCerUtils.getTrustManager();
            client=new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(),trustManager)
//                    .hostnameVerifier(((hostname, session) -> true))
                    .build();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .build();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        RetrofitService service = retrofit.create(RetrofitService.class);
        Call<ResponseBody> call = service.authenticate(url, body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                try {
                    int code = response.code();
                    if (response.body() != null) {
                        String bodyStr = response.body().string();
                        eSimHelper.esimActiveNext(code, bodyStr);
                    }
                    if (code == 204) {
                        eSimHelper.esimActiveResult(code);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("err---", t.getMessage());
            }
        });


    }


//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void deverse(MsgBean msgBean) {
//        if (msgBean != null) {
//            Object o = msgBean.getObject();
//            String msg = msgBean.getMsg();
//            if (o instanceof BleDevice) {
//                BleDevice device = (BleDevice) o;
//                if (msg.equals(ActionUtils.ACTION_CONNECT_SUCCESS_S)) {
//                    connectSuccess(device);
//                } else if (msg.equals(ActionUtils.ACTION_CONNECT_FAIL_S)) {
//                    connetFail(device);
//                } else if (msg.equals(ActionUtils.ACTION_SCAN_SUCCESS_S)) {
//                    scanSuccess(device);
//                } else if (msg.equals(ActionUtils.ACTION_DISCONNECT_S)) {
//                    disconnect(device);
//                }
//            } else if (o == null) {
//                if (msg.equals(ActionUtils.ACTION_SCAN_FAIL_S)) {
//                    scanFail();
//                }
//            }
//        }
//
//    }

    // todo
    private void disconnect(BleDevice device) {
        this.bleDevice = null;
    }


    // todo
    public void scanFail() {
        toast("未发现蓝牙设备,请重启设备再试");
    }

    public void scanSuccess(BleDevice bleDevice) {

    }

    public void connetFail(BleDevice b) {
        toast("连接失败");
    }

    public void connectSuccess(BleDevice bleDevice) {
        this.bleDevice = bleDevice;
        Log.e("connect-success---", bleDevice.getMac());
//        loadingWidget.hide();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (loadingWidget.isShown()){
            loadingWidget.hide();
            return;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    interface RetrofitService {
        @POST("{lastPath}")
        Call<ResponseBody> authenticate(@Path("lastPath") String path, @Body RequestBody requestBody);
    }
}
