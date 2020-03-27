package com.ly.createaar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.fastble.fastble.data.BleDevice;
import com.ly.qcommesim.core.widget.LoadingWidget;
import com.ly.qcommesim.core.widget.ProgressDialogWidget;
import com.ly.qcommesim.qcomm.annotation.Enums;
import com.ly.qcommesim.qcomm.helper.QualcommHelper2;
import com.ly.qcommesim.qcomm.service.QcommBleService;
import com.ly.qcommesim.qcomm.widget.VMUpgradeDialog;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class QcommUpgradeActivity2 extends FragmentActivity implements VMUpgradeDialog.UpgradeDialogListener {

    private BleDevice bleDevice;
    private QcommBleService otaUpgradeService1;
    private DecimalFormat decimalFormat;
    //    private VMUpgradeDialog vmUpgradeDialog;
    private List<String> fileNameList = new ArrayList<>();
    private boolean isBond = false;
    private String testMac = "88:9e:33:ee:a7:93";
    private QualcommHelper2 qualcommHelper;
    private TextView showTxt;
    private String filePath;
    private String mUrl;
    private ProgressDialogWidget progressDialogWidget;
    private LoadingWidget loadingWidget;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qualcomm2);
        progressDialogWidget = findViewById(R.id.progress_dialog);
        loadingWidget = findViewById(R.id.main_loading_widget);
        decimalFormat = new DecimalFormat("0.00");
        //        combinePacket();
        initHelper();
        checkLocation();

    }

    private void loadingHide(String desc){
        if (loadingWidget.isShown()){
            runOnUiThread(()->{
                loadingWidget.hide();
                Toast.makeText(QcommUpgradeActivity2.this, desc, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void initHelper() {
        String filePathName = Environment.getExternalStorageDirectory().getPath()
                + File.separatorChar + "test-data";
        File file1 = new File(filePathName);
        String fn;
        if (file1.exists()) {
            for (File file2 : file1.listFiles()) {
                String fileName = file2.getName();
                if (fileName.endsWith("03.bin")) {
                    fn = fileName;
                    filePath = file2.getAbsolutePath();
                }
            }

        }
        qualcommHelper = new QualcommHelper2(getApplication(), this, testMac.toUpperCase(), filePath) {
            @Override
            public void macInvalidate() {

            }

            @Override
            public void phoneBleDisable() {
                loadingHide("phone ble disable");
            }

            @Override
            public void deviceNotFound() {
                loadingHide("device not found ");

            }


            @Override
            public void deviceDisconnects(boolean isActiveDisConnected, BleDevice device) {
                loadingHide("device disconnect ");
            }

            @Override
            public void showProgress() {
                super.showProgress();
                runOnUiThread(()->{
                    loadingWidget.hide();
                    progressDialogWidget.show();
                });

            }

            @Override
            public void updateProgress(double progress) {
                super.updateProgress(progress);
                progressDialogWidget.getProgressNumTv().setText(decimalFormat.format(progress) + "%");
            }
        };
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


    @Override
    protected void onResume() {
        super.onResume();
    }

    //connect to device
    public void start(View view) {

        loadingWidget.show();
        qualcommHelper.start();

    }

    public void disconn(View view) {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qualcommHelper != null) {
            qualcommHelper.stop();
        }
    }

    @Override
    public void abortUpgrade() {
        otaUpgradeService1.abortUpgrade();
    }

    @Override
    public int getResumePoint() {
        return (otaUpgradeService1 != null) ? otaUpgradeService1.getResumePoint() : Enums.DATA_TRANSFER;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        if (requestCode==1001){
            loadingWidget.hide();
        }
    }
}
