package com.ly.qcommesim.qcomm.helper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.fastble.fastble.data.BleDevice;
import com.ly.qcommesim.core.helper.BaseBleHelper;
import com.ly.qcommesim.qcomm.annotation.ConfirmationType;
import com.ly.qcommesim.qcomm.annotation.Enums;
import com.ly.qcommesim.qcomm.annotation.MessageType;
import com.ly.qcommesim.qcomm.annotation.State;
import com.ly.qcommesim.qcomm.annotation.Support;
import com.ly.qcommesim.qcomm.service.QcommBleService;
import com.ly.qcommesim.qcomm.upgrade.UpgradeError;
import com.ly.qcommesim.qcomm.upgrade.UploadProgress;
import com.xble.xble.core.CoreManager;
import com.xble.xble.core.cons.RssiLevel;
import com.xble.xble.core.log.Logg;
import com.xble.xble.core.utils.TimerHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public abstract class QualcommHelper2 {
    private static final String TAG = "QualcommHelper2";
    private Activity mContext;
    private String macAddress;
    private File mUpgradeFile;
    private BleDevice device;
    private QcommBleService qcommBleService;
    private boolean bondService = false;
    private TimerHelper connTimer;
    private TimerHelper rssiTimer;
    private RssiLevel rssiLevel;
    private int reconnTimes = 5;
    private boolean isOne = true;
    private boolean reStart = false;
    private boolean reSend = false;
    private BaseBleHelper baseBleHelper;
    private String upgradeFilePath;

    /**
     * 初始化必要参数
     *
     * @param context  上下文
     * @param mac      mac蓝牙地址
     * @param filePath 升级文件路径,需要体现出版本的信息,否则无法判断是否需要升级
     * @return
     */
    public QualcommHelper2(Application application, Activity context, String mac, String filePath) {
        this.mContext = context;
        this.macAddress = mac;
        this.mUpgradeFile = new File(filePath);
        this.upgradeFilePath = filePath;
        baseBleHelper = new BaseBleHelper(application);
    }

    /**
     * 读取蓝牙固件版本,service 和read uuid为蓝牙通用uuid
     * <p>
     * 可做版本校验
     */
    private void readCha() {
        baseBleHelper.setReadFailListener(error -> {
            printResult("读取蓝牙版本失败,原因: " + error);
        });
        baseBleHelper.setReadSuccessListener((version -> {
            if (upgradeFilePath.contains(version)) {
                printResult("当前版本不需要升级");
            } else {

            }
        }));
        baseBleHelper.read(device);
    }

    private void openTimer() {
        if (connTimer != null) {
            connTimer.stop();
        }
        connTimer = new TimerHelper() {
            @Override
            public void doSomething() {
                if (reconnTimes > 0) {
                    reconnTimes--;
                    doHandle();
                } else {
                    deviceNotFound();
                    connTimer.stop();
                }
            }
        };

        connTimer.start(5000);
    }

    private void rssiTimer(BleDevice device) {
        if (rssiTimer != null) {
            rssiTimer.stop();
        }
        rssiTimer = new TimerHelper() {
            @Override
            public void doSomething() {
                getRssi(device);
            }
        };

        rssiTimer.start(5000);
    }

    private void getRssi(BleDevice device) {
        baseBleHelper.getRssi(device, new int[]{-40, -60, -75, -92});
        baseBleHelper.setOnGetRssiFailedListener((exception -> {
            printResult(device.getMac() + " 获取rssi失败,原因: " + exception.getDescription());
            this.rssiLevel = RssiLevel.UNKNOWN;
        }));
        baseBleHelper.setOnGetRssiSuccessListener((rssi, rssiLevel) -> {
            this.rssiLevel = rssiLevel;
        });
    }

    private void doConnect() {
        baseBleHelper.setOnStartConneectListener(() -> {
        });
        baseBleHelper.setOnConnectFailedListener((bleDevice -> {
            openTimer();
        }));
        baseBleHelper.setOnConnectSuccessListener((bleDevice, bluetoothGatt) -> {
            rssiTimer(bleDevice);
            //            connTimerStop();
            device = bleDevice;
            bindQcommService();
        });
        baseBleHelper.setOnDisConnectedListener((isActive, bbleDevice, gatt) -> {
            device = null;
            //            qcommBleService.disconnectDevice();
            connTimerStop();
            rssiTimerStop();
            //            if (!isActive) {//不是主动断开
            //                if (rssiLevel == RssiLevel.BAD || rssiLevel == RssiLevel.WROSE) {//表示走远,打开timer持续连接
            //                    openTimer();
            //                }
            //            } else {
            //                if (reStart) {
            //                    reStart = false;
            //                    openTimer();
            //                }
            //            }
        });
        baseBleHelper.connect(macAddress);
    }


    /**
     * 绑定高通升级服务
     */
    private void bindQcommService() {
        Intent intent = new Intent(mContext, QcommBleService.class);
        mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
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


    private void prepare() {
        if (TextUtils.isEmpty(macAddress)) {
            macInvalidate();
            return;
        }
        if (!baseBleHelper.isBLEEnable()) {
            baseBleHelper.enableBle(mContext, 1001);
            bleIsUnable();
            return;
        }

        BleDevice tDevice = baseBleHelper.isTargetBLEExist(macAddress.toUpperCase());
        if (tDevice == null || device == null) {
            doHandle();
            return;
        }

        if (bondService) {
            onNext();
        } else {
            bindQcommService();
        }

    }


    /**
     * 是否为特殊机型:是则先扫描再连接,否则直接连接
     */
    private void doHandle() {
        if (CoreManager.specialModel.contains(Build.MODEL)) {
            scanDevice();
        } else {
            doConnect();
        }
    }

    /**
     * 扫描设备
     */
    private void scanDevice() {
        List<String> macs = new ArrayList<>();
        macs.add(macAddress);
        baseBleHelper.setOnScanFinishListener(this::scanFinish);
        baseBleHelper.scan(macs);
    }

    /**
     * 扫描结束
     *
     * @param devices
     */
    private void scanFinish(List<BleDevice> devices) {
        if (devices == null || devices.size() == 0) {//没找到设备
            deviceNotFound();
        } else {
            doConnect();
        }
    }

    /**
     * qualcomm service连接设备
     */
    private void onNext() {
        qcommBleService.connectToDevice(device.getDevice());
        addHandler();
    }

    /**
     * 开始升级
     */
    public final void start() {
        Log.e("mac----", macAddress + "");
        prepare();
    }

    public final void stop() {
        if (connection != null && bondService) {
            unBindService(mContext);
        }
        if (device != null) {
            baseBleHelper.disConnectBLE(device);
        }
        releaseHandler();
        rssiTimerStop();
        connTimerStop();
    }

    private void rssiTimerStop() {
        if (rssiTimer != null) {
            rssiTimer.stop();
        }
    }

    private void connTimerStop() {
        if (connTimer != null) {
            connTimer.stop();
        }
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
            Log.e("serviceConn---", "ok");
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

    /**
     * 接收QcommBleService回来的handler消息
     */
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
                        if (mHandler != null && isOne) {
                            qcommBleService.enableMaximumMTU(true);
                            mHandler.sendEmptyMessageDelayed(10001, 3000);
                            //                            //第一次发送升级指令很大几率没有反应,再重发一次
                            mHandler.sendEmptyMessageDelayed(10003, 30000);
                        }
                        //获取固件版本
                        readCha();
                    } else if (stateLabel.equalsIgnoreCase("DISCONNECTED")) {

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
                    connTimerStop();
                    rssiTimerStop();

                    break;

                case MessageType.UPGRADE_REQUEST_CONFIRMATION:
                    //1传传输完成 2//提交已上传的数据到板子上,进行更新 4询问是否确定升级当前bin版本 5低电量
                    @ConfirmationType int confirmation = (int) msg.obj;
                    //传输完成
                    isOne = false;

                    reStart = true;
                    if (confirmation == 1) {
                        connTimerStop();
                        rssiTimerStop();
                        //                            disConnectBLE(device);
                    } else if (confirmation == 4) {
                        Message message = mHandler.obtainMessage();
                        message.obj = confirmation;
                        mHandler.sendMessageDelayed(message, 1000);
                    }

                    Log.e("-----upgradeRequest", confirmation + "");
                    //                    handlerCallback.askForConfirmation(confirmation);
                    //                    askForConfirmation(confirmation);
                    //                    handleMessage.append("UPGRADE_REQUEST_CONFIRMATION: type is ").append(confirmation);
                    break;

                case MessageType.UPGRADE_STEP_HAS_CHANGED:
                    @Enums int step = (int) msg.obj; //0开始传输 2 传输完成
                    Log.e("-----upgrade step", step + "");
                    reStart = true;
                    //                    handlerCallback.updateStep(step);
                    Message message = mHandler.obtainMessage();
                    if (step == 0) {
                        showProgress();
                    } else if (step == 2) {
                        isOne = false;
                        //                        connTimerStop();
                        //                        rssiTimerStop();
                        //提交传输完成通知
                        message.obj = 2;
                        mHandler.sendMessageDelayed(message, 1000);
                        //                        qcommBleService.sendConfirmation(ConfirmationType.TRANSFER_COMPLETE, true);
                    } else if (step == 3) {
                        //                        connTimerStop();
                        //                        rssiTimerStop();
                        //提交升级通知,即蓝牙开始重启
                        message.obj = 3;
                        mHandler.sendMessageDelayed(message, 3000);
                        //                        qcommBleService.sendConfirmation(ConfirmationType.COMMIT, true);
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
                    Log.e("filePath---", mUpgradeFile.getAbsolutePath());
                    qcommBleService.startUpgrade(mUpgradeFile);
                    break;
                case 10002:
                    int cor = (int) msg.obj;
                    qcommBleService.sendConfirmation(cor, true);
                    break;
                case 10003:
                    if (!reSend) {
                        qcommBleService.startUpgrade(mUpgradeFile);
                    }
                    break;

            }

        }
    };


    /*******************必须要考虑或者肯定出现的情形 start********************/
    public abstract void macInvalidate(); //mac地址非法时调用,必须处理,

    //手机蓝牙开关未打开时调用,必须处理,tracker蓝牙未打开的状态可以根据property的bluetooth_on属性来判断
    public abstract void bleIsUnable();

    public abstract void deviceNotFound(); //没有发现设备,必须处理,否则无法交互

    public abstract void deviceDisconnects(boolean isActiveDisConnected, BleDevice device); //设备断开连接,必须处理获取给出提示,否则用户很惊讶

    /*******************必须要考虑或者肯定出现的情形 end********************/
    public void deviceConnected(BleDevice device) {
    }

    public void showProgress() {
    } //显示更新进度,即什么时候开始,方便弹出进度框

    public void updateProgress(double progress) {
    } //更新进度

    public void upgradeSuccess() {
    } //更新成功

    //版本回调
    public void versionCallback(String version) {
    }

    /*********************打印log方法***********************/

    /**
     * 打印过程 (只打印1次)
     *
     * @param text 内容
     */
    private void printLogOne(String text) {
        if (!Logg.isPrintOne) {
            Logg.t(TAG).recordLog(getClass(), text, Logg.VERBOSE);
            Logg.isPrintOne = true;
        }

    }

    /**
     * 打印过程
     *
     * @param text 内容
     */
    private void printLog(String text) {
        Logg.t(TAG).recordLog(getClass(), text, Logg.VERBOSE);
    }

    /**
     * 打印结果
     *
     * @param text 内容
     */
    private void printResult(String text) {
        Logg.t(TAG).recordLog(getClass(), text, Logg.INFO);
    }

    /**
     * 打印错误
     *
     * @param text 内容
     */
    private void printError(String text) {
        Logg.t(TAG).recordLog(getClass(), text, Logg.ERROR);
    }

}
