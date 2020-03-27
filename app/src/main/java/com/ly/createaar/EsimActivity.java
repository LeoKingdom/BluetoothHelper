package com.ly.createaar;

import androidx.fragment.app.FragmentActivity;

//
//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.EditText;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.core.app.ActivityCompat;
//import androidx.fragment.app.FragmentActivity;
//
//import com.fastble.fastble.data.BleDevice;
//import com.ly.qcommesim.core.widget.LoadingWidget;
//import com.ly.qcommesim.esim.ESimHelper;
//
//import okhttp3.RequestBody;
//import okhttp3.ResponseBody;
//import retrofit2.Call;
//import retrofit2.http.Body;
//import retrofit2.http.POST;
//import retrofit2.http.Path;
//
public class EsimActivity extends FragmentActivity {
//
//    private BleDevice bleDevice;
//    private ESimHelper eSimHelper;
//    private TextView showTxt;
//    private String mUrl;
//    private LoadingWidget loadingWidget;
//    private EditText urlInput;
////    private String testMac="88:9E:33:EE:A7:93";
//    private String testMac="0C:C3:B4:5A:D1:84";
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_esim);
////        EventBus.getDefault().register(this);
//        showTxt = findViewById(R.id.write);
//        loadingWidget=findViewById(R.id.main_loading_widget);
//        urlInput=findViewById(R.id.url_input);
//        eSimHelper=new ESimHelper(getApplication(),testMac,defualtUrl) {
//
//
//            @Override
//            public void macInvalidate() {
//
//            }
//
//            @Override
//            public void phoneBleDisable() {
//
//            }
//
//            @Override
//            public void urlInvalidate() {
//
//            }
//
//            @Override
//            public void urlSettingFail() {
//
//            }
//
//            @Override
//            public void httpRequestFail() {
//
//            }
//
//            @Override
//            public void deviceNotFound() {
//
//            }
//
//            @Override
//            public void deviceDisconnects(boolean isActiveDisConnected, BleDevice device) {
//                Log.e("disConn---", device + "/" + isActiveDisConnected);
//            }
//
//            @Override
//            public void profileDownloadFail() {
//
//            }
//
//            @Override
//            public void esimActiveState(boolean isSuccess) {
//                super.esimActiveState(isSuccess);
//
//            }
//
//
//        };
//        checkLocation();
//    }
//
//
//
//    // todo
//    private boolean checkLocation() {
//        if (Build.VERSION.SDK_INT >= 23) {
//            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
//            if (permission != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(
//                        this,
//                        new String[]{
//                                Manifest.permission.ACCESS_COARSE_LOCATION,
//                                Manifest.permission.ACCESS_FINE_LOCATION,
//                                Manifest.permission.READ_EXTERNAL_STORAGE,
//                                Manifest.permission.WRITE_EXTERNAL_STORAGE
//                        },
//                        0x0010
//                );
//                return false;
//            } else {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    // todo
//    private void toast(String msg) {
//        Toast.makeText(this, "" + msg, Toast.LENGTH_LONG).show();
//    }
//    private String defualtUrl = "1$369f3f19.cpolar.cn$04386-AGYFT-A74Y8-3F815";
//    public void urlSet(View view){
//        String url=urlInput.getText().toString();
////        eSimHelper.setUrl(url);
//    }
//
//    public void deleteProfile(View view){
//        loadingWidget.show();
//        eSimHelper.start(ESimHelper.EsimEnum.ESIM_DELETE);
//    }
//
//    /**
//     * 设置激活的运营商(服务器)url
//     * @param view
//     */
//    public void setUrl(View view){
//        eSimHelper.start(ESimHelper.EsimEnum.ESIM_ACTIVE);
//    }
//
//    /**
//     *
//     * 激活esim
//     * @param view
//     */
//    public void notifyEsim(View view) {
////        eSimHelper.esimActive();
//    }
//    /**
//     *
//     * 激活esim
//     * @param view
//     */
//    public void cancelEsim(View view) {
//        eSimHelper.start(ESimHelper.EsimEnum.ESIM_CANCEL);
//    }
//    /**
//     * 准备profile
//     * @param view
//     */
//    public void activeEsim(View view) {
//        loadingWidget.setLoadingText("Loading...");
//        loadingWidget.show();
////        eSimHelper.esimActiveFirst(testMac);
//    }
//
//
//    // todo
//    private void disconnect(BleDevice device) {
//        this.bleDevice = null;
//    }
//
//
//    // todo
//    public void scanFail() {
//        toast("未发现蓝牙设备,请重启设备再试");
//    }
//
//    public void scanSuccess(BleDevice bleDevice) {
//
//    }
//
//    public void connetFail(BleDevice b) {
//        toast("连接失败");
//    }
//
//    public void connectSuccess(BleDevice bleDevice) {
//        this.bleDevice = bleDevice;
//        Log.e("connect-success---", bleDevice.getMac());
////        loadingWidget.hide();
//    }
//
//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        if (loadingWidget.isShown()){
//            loadingWidget.hide();
//            return;
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//    }
//
//    interface RetrofitService {
//        @POST("{lastPath}")
//        Call<ResponseBody> authenticate(@Path("lastPath") String path, @Body RequestBody requestBody);
//    }
}
