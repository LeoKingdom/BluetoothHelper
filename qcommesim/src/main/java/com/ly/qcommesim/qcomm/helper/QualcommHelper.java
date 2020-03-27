package com.ly.qcommesim.qcomm.helper;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;

import com.fastble.fastble.data.BleDevice;
import com.ly.qcommesim.core.helper.BleBaseHelper;
import com.ly.qcommesim.core.utils.PrintLog;
import com.ly.qcommesim.qcomm.annotation.ConfirmationType;
import com.ly.qcommesim.qcomm.annotation.Enums;
import com.ly.qcommesim.qcomm.annotation.MessageType;
import com.ly.qcommesim.qcomm.annotation.State;
import com.ly.qcommesim.qcomm.annotation.Support;
import com.ly.qcommesim.qcomm.service.QcommBleService;
import com.ly.qcommesim.qcomm.upgrade.UpgradeError;
import com.ly.qcommesim.qcomm.upgrade.UploadProgress;

import java.io.File;


public abstract class QualcommHelper {
    private Context mContext;
    private String macAddress;
    private File mUpgradeFile;
    private BleDevice device;
    private QcommBleService qcommBleService;
    private boolean bondService = false;
    private BleBaseHelper bleBaseHelper;
    /**
     * 初始化必要参数
     *
     * @param context  上下文
     * @param mac      mac蓝牙地址
     * @param filePath 升级文件路径
     * @return
     */
    public QualcommHelper(Application application, Context context, String mac, String filePath) {
        bleBaseHelper = new BleBaseHelper(application, mac);
        this.mContext = context;
        this.macAddress = mac;
        this.mUpgradeFile = new File(filePath);
    }


    private void doConnect() {
        bleBaseHelper.setConnStartListener(this::connStart);
        bleBaseHelper.setConnSuccessListener(this::connSuccess);
        bleBaseHelper.setConnFailListener(this::connFail);
        bleBaseHelper.setDisConnAwayListener(this::awayDisconnect);
        bleBaseHelper.setDisConnCodeListener(this::codeDisconnect);
        bleBaseHelper.setDisConnHandListener(this::handDisconnect);
        bleBaseHelper.setDisConnOtherListener(this::otherDisconnect);
        bleBaseHelper.connect(macAddress);
    }

    private void connStart() {
        bindQcommService(mContext);
    }

    private void connSuccess(BleDevice device) {
//        printResult("设备连接成功");
        this.device=device;
        deviceConnected(device);

    }

    private void connFail(BleDevice device) {
//        printResult("设备连接失败");
    }

    /**
     * 设备走远导致蓝牙断开,处理对应业务
     * @param isActive
     * @param device
     */
    private void awayDisconnect(boolean isActive, BleDevice device) {

    }

    /**
     * 调用代码断开连接
     * @param isActive
     * @param device
     */
    private void codeDisconnect(boolean isActive, BleDevice device) {

    }

    /**
     * 手动关闭蓝牙开关(主端设备开关)导致断开
     * @param isActive
     * @param device
     */
    private void handDisconnect(boolean isActive, BleDevice device) {

    }

    /**
     * 其他异常情况导致连接断开
     * @param isActive
     * @param device
     */
    private void otherDisconnect(boolean isActive, BleDevice device) {

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

    /**
     * 准备工作的判断,主要是mac地址,升级文件,蓝牙开关
     */
    private void prepare() {

        if (TextUtils.isEmpty(macAddress)) {
            macInvalidate();
            PrintLog.printLog(getClass(), "mac 地址为空");
            return;
        }
        if (!bleBaseHelper.isBleOpen()) {
            phoneBleDisable();
            PrintLog.printLog(getClass(), "蓝牙开关未打开");
            return;
        }

        if (mUpgradeFile == null || !mUpgradeFile.exists()) {
            PrintLog.printError(getClass(), "升级文件为空或不存在");
            return;
        }

        if (!mUpgradeFile.getName().endsWith(".bin")) {
            PrintLog.printError(getClass(), "升级文件不是一个bin文件, fileName: " + mUpgradeFile.getName());
            return;
        }
        PrintLog.printLog(getClass(), "文件地址: " + mUpgradeFile.getAbsolutePath());

        BleDevice tDevice = bleBaseHelper.getConnectDevice(macAddress.toUpperCase());
        if (tDevice == null || device == null) {
            doConnect();
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
        prepare();
    }

    public final void stop() {
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
            PrintLog.printResult(getClass(), "Service 绑定成功,已建立连接");
            bondService = true;
            QcommBleService.LocalBinder binder = (QcommBleService.LocalBinder) service;
            qcommBleService = binder.getService();
            onNext();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            PrintLog.printResult(getClass(), "Service 断开连接");
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
                    String stateLabel = connectionState == State.CONNECTED ? "CONNECTED"
                            : connectionState == State.CONNECTING ? "CONNECTING"
                            : connectionState == State.DISCONNECTING ? "DISCONNECTING"
                            : connectionState == State.DISCONNECTED ? "DISCONNECTED"
                            : "UNKNOWN";
                    if (stateLabel.equalsIgnoreCase("CONNECTED")) {
                        qcommBleService.enableMaximumMTU(true);
                        mHandler.sendEmptyMessageDelayed(10001, 3000);
                    } else if (stateLabel.equalsIgnoreCase("DISCONNECTED")) {
                        PrintLog.printError(getClass(), "蓝牙断开连接");
                    }
                    PrintLog.printLog(getClass(), "当前连接状态: " + stateLabel);
                    break;
                case MessageType.UPGRADE_FINISHED:
                    //蓝牙固件升级完成
                    upgradeSuccess();
                    PrintLog.printResult(getClass(), "蓝牙升级完成");
                    break;

                case MessageType.UPGRADE_REQUEST_CONFIRMATION:
                    //询问提示返回标志
                    //1传传输完成
                    // 2提交已上传的数据到板子上,进行更新
                    // 4当之前已存在升级进程而再次开始不同bin升级时询问是否确定升级当前bin版本
                    // 5低电量
                    @ConfirmationType int confirmation = (int) msg.obj;
                    PrintLog.printLog(getClass(), "提示用户: " + confirmation);
                    if (confirmation == ConfirmationType.BATTERY_LOW_ON_DEVICE) {
                        PrintLog.printError(getClass(), "升级设备电量不足,请注意");
                    }else if (confirmation==ConfirmationType.WARNING_FILE_IS_DIFFERENT){
                        PrintLog.printLog(getClass(),"您有一次未完成的升级任务,已被覆盖");
                    }
                    break;

                case MessageType.UPGRADE_STEP_HAS_CHANGED: //当前进行的步骤
                    @Enums int step = (int) msg.obj; //0开始传输 2 传输完成
                    PrintLog.printLog(getClass(), "当前步骤: " + step);
                    if (step == 0) {
                        PrintLog.printLog(getClass(), "开始传输...");
                        showProgress();
                    } else if (step == 2) {
                        //提交传输完成通知
                        PrintLog.printLog(getClass(), "传输完成...");
                        qcommBleService.sendConfirmation(ConfirmationType.TRANSFER_COMPLETE, true);
                    } else if (step == 3) {
                        //提交升级通知,即蓝牙开始重启
                        PrintLog.printLog(getClass(), "蓝牙正在重启...");
                        qcommBleService.sendConfirmation(ConfirmationType.COMMIT, true);
                    }
                    break;

                case MessageType.UPGRADE_ERROR:
                    UpgradeError error = (UpgradeError) msg.obj;
                    PrintLog.printError(getClass(), "更新过程出错: " + error.getString());

                    break;

                case MessageType.UPGRADE_UPLOAD_PROGRESS:
                    UploadProgress progress = (UploadProgress) msg.obj;
                    updateProgress(progress.getPercentage());
                    PrintLog.printLog(getClass(), "当前更新进度: " + progress.getPercentage());
//                    handlerCallback.displayTransferProgress(progress, progress.getPercentage());
                    break;
                case MessageType.TRANSFER_FAILED:
                    //传输完成后,用户不进行下一步,等待超时也会给此提示
                    PrintLog.printError(getClass(), "传输终止,升级失败");
                    break;
                case MessageType.UPGRADE_SUPPORT:
                    @Support int upgradeSupport = (int) msg.obj;
                    boolean upgradeIsSupported = upgradeSupport == Support.SUPPORTED;
                    PrintLog.printLog(getClass(), "是否支持升级: " + upgradeIsSupported);
                    break;
                case MessageType.RWCP_SUPPORTED:
                    boolean supported = (boolean) msg.obj;
                    //RWCP: 可靠的写命令协议
                    PrintLog.printResult(getClass(), "是否支持RWCP协议:" + supported);
                    break;

                case MessageType.RWCP_ENABLED:
                    boolean enabled = (boolean) msg.obj;
                    PrintLog.printResult(getClass(), "RWCP协议是否可用:" + enabled);
                    break;
                case MessageType.MTU_SUPPORTED:
                    boolean mtuSupported = (boolean) msg.obj;
                    //MTU: 最大传输单元, 主从端都支持才能使用
                    PrintLog.printResult(getClass(), "MTU是否支持设置:" + mtuSupported);
                    break;

                case MessageType.MTU_UPDATED:
                    //返回设备支持最大MTU
                    int mtu = (int) msg.obj;
                    PrintLog.printResult(getClass(), "设备支持的最大MTU为:" + mtu);
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
    public void deviceConnected(BleDevice device) {
    }

    public void showProgress() {
    } //显示更新进度,即什么时候开始,方便弹出进度框

    public void updateProgress(double progress) {
    } //更新进度

    public void upgradeSuccess() {
    } //更新成功

}
