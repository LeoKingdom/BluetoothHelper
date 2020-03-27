package com.ly.qcommesim.qcomm.helper;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothGatt;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.fastble.fastble.data.BleDevice;
import com.ly.qcommesim.core.callbacks.ScanConnectCallback;
import com.ly.qcommesim.core.helper.BleBaseHelper;
import com.ly.qcommesim.qcomm.annotation.ConfirmationType;
import com.ly.qcommesim.qcomm.annotation.Enums;
import com.ly.qcommesim.qcomm.annotation.MessageType;
import com.ly.qcommesim.qcomm.annotation.State;
import com.ly.qcommesim.qcomm.annotation.Support;
import com.ly.qcommesim.qcomm.service.QcommBleService;
import com.ly.qcommesim.qcomm.upgrade.UpgradeError;
import com.ly.qcommesim.qcomm.upgrade.UploadProgress;

import java.io.File;


public abstract class QualcommHelper extends BleBaseHelper {
    private Context mContext;
    private String macAddress;
    private File mUpgradeFile;
    private BleDevice device;
    private QcommBleService qcommBleService;
    private boolean bondService = false;
    /**
     * 初始化必要参数
     *
     * @param context     上下文
     * @param mac         mac蓝牙地址
     * @param filePath 升级文件路径
     * @return
     */
    public QualcommHelper(Application application,Context context, String mac, String filePath) {
        super(application);
        this.mContext = context;
        this.macAddress = mac;
        this.mUpgradeFile = new File(filePath);
    }



    private void scanDevice() {
        scanAndConnect(true, macAddress.toUpperCase(), "", new ScanConnectCallback() {
            @Override
            public void onScanFinished(BleDevice bleDevice) {

                if (bleDevice == null) {
                    deviceNotFound();
                }else {
                    device = bleDevice;
                    bindQcommService(mContext);
                }
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
//                deviceStateCallback.connectSuccess(bleDevice);
                Log.e("ble---",bleDevice+"");
                deviceConnected(bleDevice);

            }

            @Override
            public void onConnectFailed(BleDevice bleDevice, String description) {
//                deviceStateCallback.connectFail(bleDevice);
            }

            @Override
            public void onDisconnect(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt) {
//                deviceStateCallback.disconnect(isActiveDisConnected, device);
                deviceDisconnects(isActiveDisConnected, device);
                qcommBleService.disconnectDevice();
            }
        });
    }

    /**
     * 绑定高通升级服务
     */
    private void bindQcommService(Context context) {
        Intent intent = new Intent(context, QcommBleService.class);
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 取消服务
     *
     * @param context
     */
    private void unBindService(Context context) {
        if (context != null && connection != null) {
            context.unbindService(connection);
        }
    }

    private void scanAndConnectDevice() {
        scanDevice();
    }

    private void prepare() {
        if (TextUtils.isEmpty(macAddress)) {
            macInvalidate();
            return;
        }
        if (!isBleOpen()) {
            phoneBleDisable();
            return;
        }

        BleDevice tDevice = getConnectDevice(macAddress.toUpperCase());
        if (tDevice == null || device == null) {
            scanDevice();
            return;
        }

        if (bondService) {
            onNext();
        } else {
            bindQcommService(mContext);
        }

    }

    private void onNext() {
        qcommBleService.connectToDevice(device.getDevice());
        addHandler();
    }

    /**
     * 开始升级
     */
    public final void start() {
        Log.e("mac----",macAddress+"");
        prepare();
    }

    public final void stop(){
        unBindService(mContext);
        releaseHandler();
    }

    private void addHandler() {
        if (mHandler != null && qcommBleService != null) {
            qcommBleService.addHandler(mHandler);
        }
    }


    private void releaseHandler() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("serviceConn---","ok");
            bondService = true;
            QcommBleService.LocalBinder binder = (QcommBleService.LocalBinder) service;
            qcommBleService = binder.getService();
            onNext();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bondService = false;
        }
    };

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
                    if (stateLabel.equalsIgnoreCase("CONNECTED")) {
//                        qcommBleService.startUpgrade(mUpgradeFile);
                        qcommBleService.enableMaximumMTU(true);
                        mHandler.sendEmptyMessageDelayed(10001,3000);
                    }else if (stateLabel.equalsIgnoreCase("DISCONNECTED")){

                    }
//                    handlerCallback.deviceState(stateLabel);
                    Log.e("state-----", stateLabel + "");
//                    handleMessage.append("CONNECTION_STATE_HAS_CHANGED: ").append(stateLabel);
                    break;
                case MessageType.UPGRADE_FINISHED:
                    //蓝牙固件升级完成
                    upgradeSuccess();
//                    handlerCallback.updateSuccess();
//                    displayUpgradeComplete();
//                    handleMessage.append("UPGRADE_FINISHED");
                    Log.e("upgrade-----", " finish");
                    break;

                case MessageType.UPGRADE_REQUEST_CONFIRMATION:
                    //1传传输完成 2//提交已上传的数据到板子上,进行更新 4询问是否确定升级当前bin版本 5低电量
                    @ConfirmationType int confirmation = (int) msg.obj;
                    Log.e("-----upgradeRequest", confirmation + "");
//                    handlerCallback.askForConfirmation(confirmation);
//                    askForConfirmation(confirmation);
//                    handleMessage.append("UPGRADE_REQUEST_CONFIRMATION: type is ").append(confirmation);
                    break;

                case MessageType.UPGRADE_STEP_HAS_CHANGED:
                    @Enums int step = (int) msg.obj; //0开始传输 2 传输完成
                    Log.e("-----upgrade step", step + "");
//                    handlerCallback.updateStep(step);
                    if (step == 0) {
                        showProgress();
                    } else if (step == 2) {
                        //提交传输完成通知
                        qcommBleService.sendConfirmation(ConfirmationType.TRANSFER_COMPLETE, true);
                    } else if (step == 3) {
                        //提交升级通知,即蓝牙开始重启
                        qcommBleService.sendConfirmation(ConfirmationType.COMMIT, true);
                    }
                    break;

                case MessageType.UPGRADE_ERROR:
                    UpgradeError error = (UpgradeError) msg.obj;
                    Log.e("upgrade-err-----", error.getString() + "----");
//                    handlerCallback.manageError(error);
                    break;

                case MessageType.UPGRADE_UPLOAD_PROGRESS:
                    UploadProgress progress = (UploadProgress) msg.obj;
                    updateProgress(progress.getPercentage());
//                    handlerCallback.displayTransferProgress(progress, progress.getPercentage());
                    break;
                case MessageType.TRANSFER_FAILED:
                    //传输完成后,用户不进行下一步,等待超时也会给此提示
                    Log.e("transfer---", "fail");
                    break;
                case MessageType.UPGRADE_SUPPORT:
                    @Support int upgradeSupport = (int) msg.obj;
                    boolean upgradeIsSupported = upgradeSupport == Support.SUPPORTED;
                    break;
                case MessageType.RWCP_SUPPORTED:
                    boolean supported = (boolean) msg.obj;
                    break;

                case MessageType.RWCP_ENABLED:
                    boolean enabled = (boolean) msg.obj;
                    break;
                case MessageType.MTU_SUPPORTED:
                    boolean mtuSupported = (boolean) msg.obj;
                    Log.e("mtuSupport---", "" + mtuSupported);
                    break;

                case MessageType.MTU_UPDATED:
                    //返回设备支持最大MTU
                    int mtu = (int) msg.obj;
                    Log.e("maxMtu---", "" + mtu);
                    break;
                case 10001:
                    qcommBleService.startUpgrade(mUpgradeFile);
                    break;

            }

        }
    };

    /*******************必须要考虑或者肯定出现的情形 start********************/
    public abstract void macInvalidate(); //mac地址非法时调用,必须处理,

    //手机蓝牙开关未打开时调用,必须处理,tracker蓝牙未打开的状态可以根据property的bluetooth_on属性来判断
    public abstract void phoneBleDisable();

    public abstract void deviceNotFound(); //没有发现设备,必须处理,否则无法交互

    public abstract void deviceDisconnects(boolean isActiveDisConnected, BleDevice device); //设备断开连接,必须处理获取给出提示,否则用户很惊讶
    /*******************必须要考虑或者肯定出现的情形 end********************/
    public void deviceConnected(BleDevice device){}

    public void showProgress() {
    } //显示更新进度,即什么时候开始,方便弹出进度框

    public void updateProgress(double progress) {
    } //更新进度

    public void upgradeSuccess() {
    } //更新成功

}
