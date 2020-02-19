package com.ly.createaar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.ly.blehelper.core.callbacks.WriteCallback;
import com.ly.blehelper.esim.helper.ESimActiveHelper;
import com.ly.blehelper.upgrade_core.ble_upgrade.annotation.ConfirmationType;
import com.ly.blehelper.upgrade_core.ble_upgrade.annotation.Enums;
import com.ly.blehelper.upgrade_core.ble_upgrade.annotation.ErrorTypes;
import com.ly.blehelper.upgrade_core.ble_upgrade.annotation.MessageType;
import com.ly.blehelper.upgrade_core.ble_upgrade.annotation.State;
import com.ly.blehelper.upgrade_core.ble_upgrade.annotation.Support;
import com.ly.blehelper.upgrade_core.ble_upgrade.upgrade.UpgradeError;
import com.ly.blehelper.upgrade_core.ble_upgrade.upgrade.UploadProgress;
import com.ly.blehelper.upgrade_core.ble_upgrade.upgrade.codes.ReturnCodes;
import com.ly.blehelper.upgrade_core.ble_upgrade.utils.Consts;
import com.ly.blehelper.upgrade_core.ble_upgrade.utils.Utils;
import com.ly.blehelper.upgrade_core.fota_upgrade.callbacks.DataCallback;
import com.ly.blehelper.upgrade_core.fota_upgrade.callbacks.NotifyCallback;
import com.ly.blehelper.upgrade_core.fota_upgrade.callbacks.ProgressCallback;
import com.ly.blehelper.upgrade_core.fota_upgrade.utils.ActionUtils;
import com.ly.blehelper.upgrade_core.fota_upgrade.utils.TransformUtils;
import com.ly.blehelper.upgrade_core.service.TrackerBleService;
import com.ly.blehelper.utils.ZipUtils;
import com.ly.blehelper.widget.LoadingWidget;
import com.ly.blehelper.widget.ProgressDialogWidget;
import com.ly.blehelper.widget.VMUpgradeDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import fastble.data.BleDevice;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class UpgradeActivity extends FragmentActivity implements VMUpgradeDialog.UpgradeDialogListener {

    private BleDevice bleDevice;
    private EditText editText;
    private EditText macEt;
    private ProgressDialogWidget progressDialogWidget;
    private LoadingWidget loadingWidget;
    private TrackerBleService otaUpgradeService1;
    private DecimalFormat decimalFormat;
    private VMUpgradeDialog vmUpgradeDialog;
    private List<String> fileNameList = new ArrayList<>();
    private Button otaBtn;
    private Button gtBtn;
    private boolean isBond = false;
    private String testMac="88:9e:33:ee:a7:93";
    //与service之间的各种消息回调
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MessageType.CONNECTION_STATE_HAS_CHANGED:
                    @State int connectionState = (int) msg.obj;
//                    onConnectionStateChanged(connectionState);
                    String stateLabel = connectionState == State.CONNECTED ? "CONNECTED"
                            : connectionState == State.CONNECTING ? "CONNECTING"
                            : connectionState == State.DISCONNECTING ? "DISCONNECTING"
                            : connectionState == State.DISCONNECTED ? "DISCONNECTED"
                            : "UNKNOWN";
                    Log.e("state-----", stateLabel + "");
//                    handleMessage.append("CONNECTION_STATE_HAS_CHANGED: ").append(stateLabel);
                    break;

                case MessageType.GAIA_SUPPORT:
                    @Support int gaiaSupport = (int) msg.obj;
                    boolean gaiaIsSupported = gaiaSupport == Support.SUPPORTED;
                    if (!gaiaIsSupported) {
//                        Toast.makeText(this, getString(R.string.toast_gaia_not_supported), Toast.LENGTH_SHORT).show();
                    }
                    Log.e("-----gaiaSupport", gaiaIsSupported + "");
//                    displayGaiaSupported(gaiaIsSupported);
//                    handleMessage.append("GAIA_SUPPORT: ").append(gaiaIsSupported);
                    break;

                case MessageType.UPGRADE_SUPPORT:
                    @Support int upgradeSupport = (int) msg.obj;
                    boolean upgradeIsSupported = upgradeSupport == Support.SUPPORTED;
                    if (!upgradeIsSupported) {
//                        Toast.makeText(this, getString(R.string.toast_upgrade_not_supported), Toast.LENGTH_SHORT).show();
                    }
                    Log.e("-----upgradeIsSupported", upgradeIsSupported + "");
//                    displayUpgradeSupported(upgradeIsSupported);
//                    handleMessage.append("UPGRADE_SUPPORT: ").append(upgradeIsSupported);
                    break;

                case MessageType.UPGRADE_FINISHED:
                    //蓝牙固件升级完成
//                    displayUpgradeComplete();
//                    handleMessage.append("UPGRADE_FINISHED");
                    Log.e("upgrade-----", " finish");
                    break;

                case MessageType.UPGRADE_REQUEST_CONFIRMATION:
                    @ConfirmationType int confirmation = (int) msg.obj;
                    Log.e("-----upgradeRequest", confirmation + "");
                    askForConfirmation(confirmation);
//                    handleMessage.append("UPGRADE_REQUEST_CONFIRMATION: type is ").append(confirmation);
                    break;

                case MessageType.UPGRADE_STEP_HAS_CHANGED:
                    @Enums int step = (int) msg.obj;
                    Log.e("-----upgrade step", step + "");
                    vmUpgradeDialog.updateStep(step);
//                    handleMessage.append("UPGRADE_STEP_HAS_CHANGED: ").append(ResumePoints.getLabel(step));
                    break;

                case MessageType.UPGRADE_ERROR:
                    UpgradeError error = (UpgradeError) msg.obj;
                    Log.e("upgrade-err-----", error.getString() + "----");
                    manageError(error);
//                    handleMessage.append("UPGRADE_ERROR");
                    break;

                case MessageType.UPGRADE_UPLOAD_PROGRESS:
                    UploadProgress progress = (UploadProgress) msg.obj;
                    Log.e("upgrade-progress----", progress.getPercentage() + "-----progress");
                    vmUpgradeDialog.displayTransferProgress(progress);
//                    handleMessage.append("UPGRADE_UPLOAD_PROGRESS");
                    break;

                case MessageType.DEVICE_BOND_STATE_HAS_CHANGED:
                    int state = (int) msg.obj;
//                    displayBondState(state);
//                    handleMessage.append("DEVICE_BOND_STATE_HAS_CHANGED");
                    break;

                case MessageType.RWCP_SUPPORTED:
                    boolean supported = (boolean) msg.obj;
                    Log.e("-----rwcp support", supported + "");
//                    onRWCPSupported(supported);
//                    handleMessage.append("RWCP_SUPPORTED: ").append(supported);
                    break;

                case MessageType.RWCP_ENABLED:
                    boolean enabled = (boolean) msg.obj;
                    Log.e("----rwcp enable", enabled + "");
//                    onRWCPEnabled(enabled);
//                    handleMessage.append("RWCP_ENABLED: ").append(enabled);
                    break;

                case MessageType.TRANSFER_FAILED:
                    // The transport layer has failed to transmit bytes to the device using RWCP
                    if (vmUpgradeDialog.isAdded()) {
                        vmUpgradeDialog.displayError(getString(R.string.dialog_upgrade_transfer_failed));
                    }
                    Log.e("transfer---", "fail");
//                    handleMessage.append("TRANSFER_FAILED");
                    break;

                case MessageType.MTU_SUPPORTED:
                    boolean mtuSupported = (boolean) msg.obj;

                    Log.e("mtu support---", mtuSupported + "");
//                    onMTUSupported(mtuSupported);
//                    handleMessage.append("MTU_SUPPORTED: ").append(mtuSupported);
                    break;

                case MessageType.MTU_UPDATED:
                    //返回设备支持最大MTU
                    int mtu = (int) msg.obj;
                    Log.e("max mtu----", mtu + "");
//                    onMTUUpdated(mtu);
//                    handleMessage.append("MTU_UPDATED: size=").append(mtu);
                    break;

                case 100001:
                    String path = (String) msg.obj;
                    String filePathName0 = Environment.getExternalStorageDirectory().getPath()
                            + File.separatorChar + "test-data" + File.separatorChar + "unzip" + File.separatorChar + "update";
                    File file3 = new File(filePathName0);
                    if (file3.exists() && file3.isDirectory()) {
                        path = filePathName0;
                    }
                    otaUpgradeService1.initData(path);
//                    handleMessage.append("UNKNOWN MESSAGE: ").append(msg);
                    break;
            }

        }
    };
    private AlertDialog mDialogReconnection;
    private ESimActiveHelper eSimHelper;
    private TextView showTxt;
    private ServiceConnection connection1 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrackerBleService.LocalBinder binder = (TrackerBleService.LocalBinder) service;
            otaUpgradeService1 = binder.getService();
            otaUpgradeService1.addHandler(mHandler);
            isBond = true;
            String filePathName = Environment.getExternalStorageDirectory().getPath()
                    + File.separatorChar + "test-data" + File.separatorChar + "unzip";
            String filePathName1 = Environment.getExternalStorageDirectory().getPath()
                    + File.separatorChar + "test-data" + File.separatorChar + "update.zip";
            File file = new File(filePathName1);
            File file1 = new File(filePathName);
            if (file1.exists()) {
                for (File file2 : file1.listFiles()) {
                    file2.delete();
                }
            } else {
                file1.mkdirs();
            }
            if (file.exists()) {
                try {
                    ZipUtils.unzipFolder(filePathName1, filePathName);
                    Message message = mHandler.obtainMessage();
                    message.what = 100001;
                    message.obj = filePathName;
                    mHandler.sendMessageDelayed(message, 2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            setCallback();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBond = false;
        }
    };
    private String mUrl;

    /**
     * <p>This method allows the Upgrade process to be able to ask the user any confirmation to be able to carry on
     * the upgrade process.</p>
     * 该方法允许升级过程能够要求用户进行任何确认才能进行升级过程。
     *
     * @param confirmation The type of confirmation which has to be asked.
     */
    private void askForConfirmation(@ConfirmationType final int confirmation) {
        switch (confirmation) {
            case ConfirmationType.COMMIT:
                displayConfirmationDialog(confirmation, R.string.alert_upgrade_commit_title, R.string.alert_upgrade_commit_message);
                break;
            case ConfirmationType.IN_PROGRESS:
                // no obligation to ask for confirmation as the commit confirmation will happen next
                otaUpgradeService1.sendConfirmation(confirmation, true);
                break;
            case ConfirmationType.TRANSFER_COMPLETE:
                //升级包传输完成
                displayConfirmationDialog(confirmation, R.string.alert_upgrade_transfer_complete_title,
                        R.string.alert_upgrade_transfer_complete_message);
                break;
            case ConfirmationType.BATTERY_LOW_ON_DEVICE:
                //设备电量低
                displayConfirmationDialog(confirmation, R.string.alert_upgrade_low_battery_title,
                        R.string.alert_upgrade_low_battery_message);
                break;
            case ConfirmationType.WARNING_FILE_IS_DIFFERENT:
                displayConfirmationDialog(confirmation, R.string.alert_upgrade_sync_id_different_title,
                        R.string.alert_upgrade_sync_id_different_message);
                break;
        }
    }

    private void displayConfirmationDialog(@ConfirmationType final int confirmation, int title, int
            message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton(R.string.button_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        otaUpgradeService1.sendConfirmation(confirmation, true);
                    }
                })
                .setNegativeButton(R.string.button_abort, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        otaUpgradeService1.sendConfirmation(confirmation, false);
//                        showUpgradeDialog(false);
                    }
                });
        builder.show();
    }

    private void initReconnectionDialog() {
        // build the dialog to show a progress bar when we try to reconnect.
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(UpgradeActivity.this);
        dialogBuilder.setTitle(getString(R.string.alert_reconnection_title));

        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") // the root can be null as "attachToRoot" is false
                View dialogLayout = inflater.inflate(R.layout.dialog_progress_bar, null, false);
        dialogBuilder.setView(dialogLayout);
        dialogBuilder.setCancelable(false);
        mDialogReconnection = dialogBuilder.create();

    }

    private void manageError(UpgradeError error) {
        switch (error.getError()) {
            case ErrorTypes.AN_UPGRADE_IS_ALREADY_PROCESSING:
                // nothing should happen as there is already an upgrade processing.
                // in case it's not already displayed, we display the Upgrade dialog
                showUpgradeDialog(true);
                break;

            case ErrorTypes.ERROR_BOARD_NOT_READY:
                // display error message + "please try again later"
                if (vmUpgradeDialog.isAdded()) {
                    vmUpgradeDialog.displayError(getString(R.string.dialog_upgrade_error_board_not_ready));
                }
                break;

            case ErrorTypes.EXCEPTION:
                // display that an error has occurred?
                if (vmUpgradeDialog.isAdded()) {
                    vmUpgradeDialog.displayError(getString(R.string.dialog_upgrade_error_exception));
                }
                break;

            case ErrorTypes.NO_FILE:
                displayFileError();
                break;

            case ErrorTypes.RECEIVED_ERROR_FROM_BOARD:
                if (vmUpgradeDialog.isAdded()) {
                    vmUpgradeDialog.displayError(ReturnCodes.getReturnCodesMessage(error.getReturnCode()),
                            Utils.getIntToHexadecimal(error.getReturnCode()));
                }
                break;

            case ErrorTypes.WRONG_DATA_PARAMETER:
                if (vmUpgradeDialog.isAdded()) {
                    vmUpgradeDialog.displayError(getString(R.string.dialog_upgrade_error_protocol_exception));
                }
                break;
        }
    }

    private void displayFileError() {
        showUpgradeDialog(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_file_error_message)
                .setTitle(R.string.alert_file_error_title)
                .setPositiveButton(R.string.button_ok, null);
        builder.show();
    }

    private void showUpgradeDialog(boolean show) {
        if (show && !vmUpgradeDialog.isAdded()) {
//            vmUpgradeDialog.show(getSupportFragmentManager(), getResources().getString(R.string.dialog_upgrade_title));
        } else if (!show && vmUpgradeDialog.isAdded()) {
            vmUpgradeDialog.dismiss();
        }
    }

    private void setCallback() {
        otaUpgradeService1.setNotifyCallback(new NotifyCallback() {
            @Override
            public void characteristicChange(int action, byte[] backBytes) {
                String callback = TransformUtils.bytesToHexString(backBytes);
                showTxt.setText(callback + "");
            }

            @Override
            public void deviceReconn() {
                //重新连接
                progressDialogWidget.getCurrentPacket().setText("连接成功");
                progressDialogWidget.showCloseBtnWithText("继续");
                progressDialogWidget.getCloseBtn().setOnClickListener(v -> {
//                    otaUpgradeService1.sendMsg(otaUpgradeService1.getHandler(), ActionUtils.ACTION_OTA_RECONNECT_SEND, 3000);
                });

            }

            @Override
            public void onNext() {
                Log.e("notifyOpen---", "success");
                otaUpgradeService1.enableMaximumMTU(true);
                otaBtn.setEnabled(true);
                gtBtn.setEnabled(true);
            }

            @Override
            public void error(String o) {
                Log.e("notifyOpen---", "fail---" + o);
            }

        });

        otaUpgradeService1.setWriteCallback(new WriteCallback() {
            @Override
            public void writeSuccess(int actionType, int current, int total, byte[] justWrite) {
                if (actionType == ActionUtils.ACTION_OTA_ORDER_I) {

                } else if (actionType == ActionUtils.ACTION_OTA_DATA_HEAD_I) {

                } else {

                }
            }
        });

        otaUpgradeService1.setProgressCallback(new ProgressCallback() {
            @Override
            public void setMax(int max) {
                progressDialogWidget.setProgressMax(max);
                progressDialogWidget.setProgressNumTvText(0 + "%");
                progressDialogWidget.show();
            }

            @Override
            public void setProgress(float percent, int current, int totalFrame, int currentFrame, int currentBin, int totalBin) {
                progressDialogWidget.getCurrentPacket().setText("共" + totalBin + "个文件,当前传送: 第" + currentBin + "个文件,共" + totalFrame + "帧,第" + currentFrame + "帧,第" + current + "包");
                progressDialogWidget.getProgressBar().setProgress((int) percent);
                progressDialogWidget.getProgressNumTv().setText(decimalFormat.format(percent) + "%");
            }

        });

        otaUpgradeService1.setDataCallback(new DataCallback() {
            @Override
            public void done() {
                //传输完成
                progressDialogWidget.setProgressNumTvText("传输完成!");
                progressDialogWidget.showCloseBtnWithText("Done");
            }

            @Override
            public void checkOutTime() {
                loadingWidget.hide();
                toast("data loading fail,try again");
            }

            @Override
            public void binChecking() {
                loadingWidget.setLoadingText("data loading...");
                loadingWidget.show();
            }

            @Override
            public void binCheckDone(boolean isBin) {
                loadingWidget.hide();
                if (!isBin) {
                    toast("It is not a bin file ");
                } else {

                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        EventBus.getDefault().register(this);
        editText = findViewById(R.id.edittext);
        macEt = findViewById(R.id.mac_et);
        showTxt = findViewById(R.id.write);
        progressDialogWidget = findViewById(R.id.progress_dialog);
        loadingWidget = findViewById(R.id.main_loading_widget);
        otaBtn = findViewById(R.id.ot_btn);
        gtBtn = findViewById(R.id.gt_btn);
        TextView txtTagProgress = progressDialogWidget.getProgressNumTv();
        decimalFormat = new DecimalFormat("0.0000");
//        combinePacket();
        vmUpgradeDialog = VMUpgradeDialog.newInstance(this);
        initReconnectionDialog();
        checkLocation();
        copyAssetAndWrite("mkn0_bt_uart_v03.bin");
        otaBtn.setOnClickListener(v -> {
            if (bleDevice == null) {
                toast("设备未连接");
            } else {
                otaUpgradeService1.startUpgrade(null);
            }
        });
        gtBtn.setOnClickListener(v -> {
            //蓝牙芯片升级
//            if (bleDevice==null){
//                toast("设备未连接");
//            }else {
//                loadingWidget.hide();
//                progressDialogWidget.hide();
//                File file = new File(getCacheDir(), "mkn0_bt_uart_v03.bin");
//                otaUpgradeService1.startUpgrade(file);
//                showUpgradeDialog(true);
//            }
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

    // todo
    public void clear(View view) {
        editText.setText("");
    }

    private void bindMyService(String mac) {
        Intent intent = new Intent(this, TrackerBleService.class);
        intent.setAction(ActionUtils.ACTION_DEVICE_SCAN);
        intent.putExtra("mac_address", mac);
//        intent.putExtra("mac_address", "01:02:04:05:A6:14");
        bindService(intent, connection1, Context.BIND_AUTO_CREATE);
    }

    // todo
    public void write(View view) {
        if (bleDevice == null) {
            toast("连接未连接");
            loadingWidget.setLoadingText("device connecting...");
            loadingWidget.show();
//            otaUpgradeService1.connectDevice(false);
            return;
        }
        loadingWidget.setLoadingText("data loading...");
        loadingWidget.show();
        otaUpgradeService1.startUpgrade(null);
    }

    public void time_set(View view) {
        String timeStr = editText.getText().toString().trim();
        if (TextUtils.isEmpty(timeStr)) {
            toast("请输入数值");
            return;
        }
        long time = Long.valueOf(timeStr);
        Consts.betweenTimes = time;
        if (time != 200) {
            toast("设置成功");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        eSimHelper = new ESimActiveHelper(getApplication());
    }

    //connect to device
    public void connect(View view) {
        String macAddress = macEt.getText().toString().trim();
        if (TextUtils.isEmpty(macAddress)) {
            macAddress = testMac;
//            macAddress = "01:02:04:05:06:09";
        }
        if (isBond) {
//            otaUpgradeService1.handleConnect(testMac);
//            otaUpgradeService1.scanAndConn(macAddress);
        } else {
            bindMyService(macAddress);
        }
    }

    public void read(View view) {
        if (otaUpgradeService1 != null) {
            File file = new File(getCacheDir(), "mkn0_bt_uart_v03.bin");
            otaUpgradeService1.startUpgrade(file);
            showUpgradeDialog(true);
            return;
        }
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
        loadingWidget.hide();
        progressDialogWidget.setProgressNumTvText("");
        progressDialogWidget.getCurrentPacket().setText("设备已断开");
        progressDialogWidget.showCloseBtnWithText("重试");
        progressDialogWidget.getCloseBtn().setOnClickListener((v -> {
//            otaUpgradeService1.sendMsg(otaUpgradeService1.getHandler(), ActionUtils.ACTION_DEVICE_RECONNECT, 300);
        }));
        toast("设备已断开");
        otaBtn.setEnabled(false);
        gtBtn.setEnabled(false);
    }

    private boolean copyAssetAndWrite(String fileName) {
        try {
            File cacheDir = getCacheDir();
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            File outFile = new File(cacheDir, fileName);
            if (!outFile.exists()) {
                boolean res = outFile.createNewFile();
                if (!res) {
                    return false;
                }
            } else {
                if (outFile.length() > 10) {//表示已经写入一次
                    return true;
                }
            }
            InputStream is = getAssets().open(fileName);
            FileOutputStream fos = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
            is.close();
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    // todo
    public void scanFail() {
        toast("未发现蓝牙设备,请重启设备再试");
        loadingWidget.hide();
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
    protected void onDestroy() {
        super.onDestroy();
        if (otaUpgradeService1 != null) {
            unbindService(connection1);
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

    interface RetrofitService {
        @POST("{lastPath}")
        Call<ResponseBody> authenticat(@Path("lastPath") String path, @Body RequestBody requestBody);
    }
}
