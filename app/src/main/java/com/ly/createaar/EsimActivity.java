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

import com.de.esim.rohttp.Rohttp;
import com.de.esim.rohttp.helper.callback.HttpCallback;
import com.de.esim.rohttp.helper.utils.TrustAllCerts;
import com.fastble.fastble.data.BleDevice;
import com.ly.createaar.wedgit.LoadingWidget;
import com.ly.qcommesim.esim.helper.ESimHelper;
import com.ly.qcommesim.esim.enums.EsimEnum;

import java.util.HashMap;

//
//
public class EsimActivity extends FragmentActivity {
//
private static final String TAG = "EsimActivity";
    private BleDevice bleDevice;
    private ESimHelper eSimHelper;
    private TextView showTxt;
    private String mUrl="1$2f72b92a.cpolar.cn$04386-AGYFT-A74Y8-3F815";
    private LoadingWidget loadingWidget;
    private EditText urlInput;
//    private String testMac="88:9E:33:EE:A7:93";
    private String testMac="06:C3:B4:5A:D1:84";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esim);
//        EventBus.getDefault().register(this);
        showTxt = findViewById(R.id.write);
//        initHttp();
        loadingWidget=findViewById(R.id.main_loading_widget);
        urlInput=findViewById(R.id.url_input);
        eSimHelper=new ESimHelper(getApplication(),testMac,defualtUrl) {


            @Override
            public void macInvalidate() {

            }

            @Override
            public void bleIsUnable() {

            }

            @Override
            public void urlInvalidate() {

            }

            @Override
            public void urlSettingFail() {

            }

            @Override
            public void httpRequestFail() {

            }

            @Override
            public void deviceNotFound() {

            }

            @Override
            public void deviceDisconnects(boolean isActiveDisConnected, BleDevice device) {
                Log.e("disConn---", device + "/" + isActiveDisConnected);
            }

            @Override
            public void profileDownloadFail() {
                Log.e(TAG,"profile 下载失败");
                Toast.makeText(EsimActivity.this,"Esim profile下载失败",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void esimActiveState(boolean isSuccess) {
                super.esimActiveState(isSuccess);
                loadingWidget.hide();
                Log.e(TAG,"Esim激活成功");
                Toast.makeText(EsimActivity.this,"Esim激活成功",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void esimProfileDeleteState(boolean isSuccess) {
                super.esimProfileDeleteState(isSuccess);
                loadingWidget.hide();
                Log.e(TAG,"Esim profile删除成功");
                Toast.makeText(EsimActivity.this,"profile删除成功",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void esimCancelState(boolean isSuccess) {
                super.esimCancelState(isSuccess);
                loadingWidget.hide();
                Log.e(TAG,"Esim 去活成功");
                Toast.makeText(EsimActivity.this,"去活成功",Toast.LENGTH_SHORT).show();
            }
        };
        checkLocation();
    }

    private void initHttp(){
        //初始化Application
        Rohttp.init(getApplication());
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

    private  void testHttp(){

        String url="https://2f72b92a.cpolar.cn/gsma/rsp2/es9plus/handleNotification";
         final String JSONString = "{" +
                 "    \"pendingNotification\":\"vzeBrL8nZoAQUt/alPuuZBI5djuCh/2Pab8vJ4ABK4ECB4AMEjJmNzJiOTJhLmNwb2xhci5jbloKmAAQMlR2mBAyNAYOKwYBBAGB+AIBgVxkZmOiGKEWgAEFgQEMgg4wDKAIMAaAAQWBAQSBAF83QGqkoMglAAacExbSMuFSiumb9Wo2ZbA+YsYmJmZVVX8n+cLDTEPsKzqyBE9D5l9kEBEYSB3unUscP2t8yBinACM=\"" +
                 "    }";
        new Rohttp().roBaseUrl(url).roParam(JSONString).roPost(new HttpCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.e("Esim-----",result);
                //                printResult("第" + transferCount + "次http请求成功,result: " + result);
                //                if (!TextUtils.isEmpty(result)) {
                //                    int code = 200;
                //                    String body = null;
                //                    if (body != null) {
                //                        printLog("开始第" + transferCount + "次json body写入");
                //                        esimActiveNext(code, body);
                //                    }
                //                    if (code == 204) {
                //                        esimActiveResult(code);
                //                    }
                //                }

            }

            @Override
            public void onFailed(Rohttp.FAILED failed, Throwable throwable) {
//                if (failed.getResponseCode())
                //                printResult("http request fail ,reason: " + throwable.getMessage());
                Log.e("Esim-----",throwable.getMessage()+"/"+ failed.getResponseCode());
            }

            @Override
            public void onFinished() {

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
    private String defualtUrl = "1$2f72b92a.cpolar.cn$04386-AGYFT-A74Y8-3F815";
    public void urlSet(View view){
        String url=urlInput.getText().toString();
//        eSimHelper.setUrl(url);
    }

    public void deleteProfile(View view){
        loadingWidget.show();
        eSimHelper.start(EsimEnum.ESIM_DELETE);
    }

    /**
     * 设置激活的运营商(服务器)url
     * @param view
     */
    public void setUrl(View view){
//        testHttp();
        eSimHelper.start(EsimEnum.ESIM_ACTIVE);
    }

    /**
     *
     * 激活esim
     * @param view
     */
    public void notifyEsim(View view) {
//        eSimHelper.esimActive();
    }
    /**
     *
     * 激活esim
     * @param view
     */
    public void cancelEsim(View view) {
        eSimHelper.start(EsimEnum.ESIM_CANCEL);
    }
    /**
     * 准备profile
     * @param view
     */
    public void activeEsim(View view) {
        loadingWidget.setLoadingText("Loading...");
        loadingWidget.show();
//        eSimHelper.esimActiveFirst(testMac);
    }


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
//        @POST("{lastPath}")
//        Call<ResponseBody> authenticate(@Path("lastPath") String path, @Body RequestBody requestBody);
    }
}
