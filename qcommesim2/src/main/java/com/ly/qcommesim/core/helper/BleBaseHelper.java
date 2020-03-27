package com.ly.qcommesim.core.helper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;
import android.text.TextUtils;

import com.fastble.fastble.BleManager;
import com.fastble.fastble.callback.BleGattCallback;
import com.fastble.fastble.callback.BleNotifyCallback;
import com.fastble.fastble.callback.BleRssiCallback;
import com.fastble.fastble.callback.BleScanAndConnectCallback;
import com.fastble.fastble.callback.BleWriteCallback;
import com.fastble.fastble.data.BleDevice;
import com.fastble.fastble.exception.BleException;
import com.fastble.fastble.scan.BleScanRuleConfig;
import com.fastble.fastble.utils.BleLog;
import com.ly.qcommesim.core.callbacks.ConnectCallback;
import com.ly.qcommesim.core.callbacks.NotifyOpenCallback;
import com.ly.qcommesim.core.callbacks.OpenListener;
import com.ly.qcommesim.core.callbacks.ScanConnectCallback;
import com.ly.qcommesim.core.callbacks.WriteCallback;
import com.ly.qcommesim.core.utils.Utils;
import com.xble.xble.core.FastCore;
import com.xble.xble.core.log.Logg;

import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/5/29 15:55
 * version: 1.0
 * <p>
 * 蓝牙辅助基类
 */
public class BleBaseHelper {
    private static final String TAG = "BleBaseHelper";
    private static boolean ENABLE_LOG = true;// 是否记录日志
    private static int RE_CONNECT_COUNT = 1;// 重连次数
    private static int RE_CONNECT_INTERVAL = 5 * 1000;// 间隔时间
    private static int CONNECT_OVERTIME = 10 * 1000;// 连接超时
    private static int INTERVAL_PKG_TIME = 20;// 每个包间隔20毫秒
    private static int SCAN_TIMEOUT = 10 * 1000;// 操作超时
    private BleManager bleManager;
    private long scanTimeout = 10000;
    private String service_uuid;
    private String write_uuid;
    private String notify_uuid;

    public BleBaseHelper(Application application) {
        bleManager = BleManager.getInstance();
        bleManager.init(application);
        init(application);
        initUuid();
    }

    private void initUuid() {
        printLogOne("----uuid 赋值");
        String[] uuids = {"00005500-d102-11e1-9b23-00025b00a5a5", "00005501-d102-11e1-9b23-00025b00a5a5"};
        service_uuid=uuids[0];
        notify_uuid=uuids[1];
        write_uuid=uuids[2];
    }

    /**
     * 初始化方法,主要为初始化一些主要的属性
     */
    public void init(Application app) {
        printLogOne("----- 触发初始化");
        // 1.初始化fastble蓝牙引擎
        BleManager.getInstance().init(app);
        printLogOne("1.启动蓝牙");

        // 2.判断当前系统是否支持蓝牙
        if (BleManager.getInstance().isSupportBle()) {
            printLogOne("2.当前系统支持蓝牙");

            // 3.提示用户打开蓝牙
            if (!BleManager.getInstance().isBlueEnable()) {
                // 未使用, 某些版本不支持打开蓝牙框, 改用业务自己根据需求实现
                // BleManager.getInstance().enableBluetooth();
                printLogOne("3.检测到扫描时未打开蓝牙");
            } else {
                printLogOne("3.蓝牙功能已生效");
            }


            // 4.初始化蓝牙基本配置1
            BleManager.getInstance()// manager
                    .enableLog(ENABLE_LOG)// 打印log
                    .setReConnectCount(RE_CONNECT_COUNT, RE_CONNECT_INTERVAL)// 重连次数, 间隔时间
                    .setSplitWriteNum(FastCore.SPLIT_WRITER_NUM)// 默认每次传输字节20KB
                    .setConnectOverTime(CONNECT_OVERTIME)// 连接数
                    .setOperateTimeout(FastCore.OPERATE_TIMEOUT);// 操作超时
            BleManager.getInstance().setIntervalBetweenPacket(INTERVAL_PKG_TIME);// 每个包间隔20毫秒

            printLogOne("4.配置基本信息完毕");
            printLogOne("4.[重连次数]: " + RE_CONNECT_COUNT // t1
                    + "; [间隔时间]: " + RE_CONNECT_INTERVAL // t2
                    + "; [默认传输最大字节]: " + FastCore.SPLIT_WRITER_NUM // t3
                    + "; [连接数]: " + CONNECT_OVERTIME // t4
                    + "; [操作超时]: " + FastCore.OPERATE_TIMEOUT // t5

            );

            // 5.初始化扫描规则
            BleScanRuleConfig.Builder builder = new BleScanRuleConfig.Builder();
            BleScanRuleConfig config = builder.setScanTimeOut(SCAN_TIMEOUT).setAutoConnect(true).build();
            BleManager.getInstance().initScanRule(config);
            printLogOne("5.配置扫描规则完毕");
            printLogOne("5.[扫描超时]: " + SCAN_TIMEOUT + "; [断联后自动重连]: " + true);

        } else {
            /* 2.系统不支持蓝牙 */
            systemBleNotSupportNext();
        }
    }


    /**
     * 判断是否打开蓝牙
     *
     * @return
     */
    public boolean isBleOpen() {
        return BleManager.getInstance().isBlueEnable();
    }

    /**
     * 打开蓝牙
     */
    public void enableBle() {
        BleManager.getInstance().enableBluetooth();
    }

    /**
     * 关闭蓝牙
     */
    public void closeBle() {
        BleManager.getInstance().disableBluetooth();
    }

    /**
     * 打开蓝牙,监听返回
     *
     * @param context
     * @param requestCode
     */
    public void enableBle(Activity context, int requestCode) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        context.startActivityForResult(enableBtIntent, requestCode);
    }

    /**
     * 判断GPS是否打开
     *
     * @param context
     * @return
     */
    private boolean isGpsOpen(Context context) {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 提醒用户去设置页打开gps,监听返回
     *
     * @param activity
     * @param requestCode
     */
    public void openGPS(Activity activity, int requestCode) {
        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivityForResult(locationIntent, requestCode);
    }

    /**
     * 检查gps和蓝牙开关是否有开
     *
     * @param listener OpenListener回调
     */
    public void checkGpsAndBle(Context context, OpenListener listener) {
        boolean isOpenGps = isGpsOpen(context);
        boolean isOpenBle = isBleOpen();
        listener.open(isOpenBle, isOpenGps);
    }


    /**
     * 根据蓝牙地址判断设备是否已配对(绑定)
     *
     * @param mac 蓝牙地址
     * @return
     */
    public boolean isBonded(String mac) {
        Set<BluetoothDevice> bondList = bleManager.getBondDeviceList();
        for (BluetoothDevice bluetoothDevice : bondList) {
            if (bluetoothDevice.getAddress().equalsIgnoreCase(mac)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 取消配对
     *
     * @param mac 蓝牙地址
     */
    public void unBondDevice(String mac) {
        Set<BluetoothDevice> bondList = bleManager.getBondDeviceList();
        for (BluetoothDevice bluetoothDevice : bondList) {
            if (bluetoothDevice.getAddress().equalsIgnoreCase(mac)) {
                Utils.unpairDevice(bluetoothDevice);
            }
        }
    }


    /**
     * 连接设备
     * 知道mac地址可以直接连接,扫描目的就是在未知mac情况下发现设备
     *
     * @param mac 蓝牙mac地址
     */
    public void connect(String mac, final ConnectCallback connectCallback) {
        if (!isBleOpen()) {
            printError("蓝牙未打开...");
            return;
        }

        BleScanRuleConfig config = new BleScanRuleConfig.Builder().setAutoConnect(true).build();
        bleManager.initScanRule(config);
        bleManager.connect(mac, new BleGattCallback() {

            /**
             * 开始连接
             */
            @Override
            public void onStartConnect() {
                printLog(mac + "-开始连接");
            }

            /**
             * 连接失败
             * @param bleDevice 连接设备
             * @param exception 异常信息
             */
            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                printResult(mac + "连接失败");
                if (!isBleOpen()) {
                    connectCallback.onBleDisable();
                } else if (connectCallback != null) {
                    connectCallback.onConnectFail(bleDevice, exception);
                }
            }

            /**
             * 连接成功
             * @param bleDevice 连接成功的设备
             * @param gatt 蓝牙gatt
             * @param status 状态码
             */
            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                printResult(mac + "连接成功");
                if (connectCallback != null) {
                    connectCallback.onConnectSuccess(bleDevice, gatt);
                }
            }

            /**
             * 断开连接
             * @param isActiveDisConnected 是否是之前自己要求主动断开的
             * @param device 断开设备
             * @param gatt gatt
             * @param status 状态
             */
            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                printResult(device.getMac() + "断开连接,是否主动-" + isActiveDisConnected);
                connectCallback.onDisconnect(isActiveDisConnected, device);

            }
        });
    }

    /**
     * 扫描并连接匹配的蓝牙设备
     *
     * @param isFuzzy true,模糊扫描:name="模糊name",address=null/"" ;false,根据蓝牙地址搜索,name="" 不能为null
     * @param address 蓝牙mac地址
     * @param name    蓝牙名称,不准确,一般不使用,除非蓝牙名称已知且不可更改
     */

    public void scanAndConnect(boolean isFuzzy, String address, String name, final ScanConnectCallback connectCallback) {
        if (!isBleOpen()) {
            printError("蓝牙未打开...");
            connectCallback.onBleDisable();
            return;
        }

        bleManager.initScanRule(scanRule(isFuzzy, address, name, true));
        bleManager.scanAndConnect(new BleScanAndConnectCallback() {

            /**
             * 扫描结束
             * @param bleDevice 扫描到的设备,可以为null
             */
            @Override
            public void onScanFinished(BleDevice bleDevice) {
                printResult("2-扫描结束,扫到的设备-" + (bleDevice != null ? bleDevice.getMac() : "Null"));
                if (connectCallback != null) {
                    connectCallback.onScanFinished(bleDevice);
                }
            }

            /**
             * 连接开始
             */
            @Override
            public void onStartConnect() {
                printLog("3-开始连接" + address);
            }

            /**
             * 连接失败
             * @param bleDevice 连接的设备,可以为null
             * @param exception 异常
             */
            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                printResult("4-连接失败" + bleDevice.getMac());
                if (!isBleOpen()) {
                    connectCallback.onBleDisable();
                } else if (connectCallback != null) {
                    connectCallback.onConnectFailed(bleDevice, exception.getDescription());
                }
            }

            /**
             * 连接成功
             * @param bleDevice 连接成功的设备
             * @param gatt 蓝牙gatt
             * @param status gatt状态
             */
            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                printResult("4-连接成功" + bleDevice.getMac());
                if (connectCallback != null) {
                    connectCallback.onConnectSuccess(bleDevice, gatt, status);
                }
                printLog("当前设备的蓝牙信号强度为: " + bleDevice.getRssi());

            }

            /**
             * 断开连接
             * @param isActiveDisConnected 是否是之前自己要求主动断开的
             * @param device 断开设备
             * @param gatt gatt
             * @param status 状态
             */
            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                printResult(device.getMac() + "断开连接,是否主动-" + isActiveDisConnected);
                if (!isBleOpen()) {
                    connectCallback.onBleDisable();
                } else if (connectCallback != null) {
                    connectCallback.onDisconnect(isActiveDisConnected, device, gatt);
                }

            }

            /**
             * 扫描开始
             * @param success 是否已开始
             */
            @Override
            public void onScanStarted(boolean success) {
                printLog("1-开始扫描,设备mac地址为-" + address);
                if (connectCallback != null) {
                    connectCallback.onScanStarted(success);
                }
            }

            /**
             * 扫描中
             * @param bleDevice
             */
            @Override
            public void onScanning(BleDevice bleDevice) {
                printLog("扫描中,设备hash-" + bleDevice.hashCode());
                if (bleDevice != null) {
                    BleLog.e("found device===" + bleDevice.getMac() + "/" + bleDevice.getName());
                }
            }

        });
    }


    /**
     * 扫描取消
     */
    protected void cancelScan() {
        BleManager.getInstance().cancelScan();
    }

    /**
     * 手动断开设备连接
     *
     * @param ob 可以是mac地址或者ble设备
     */
    public void disconnect(Object ob) {
        BleDevice device = getDevice(ob);
        bleManager.disconnect(device);
    }

    /**
     * 查看已连接的所有设备
     *
     * @return
     */
    protected List<BleDevice> getConnectedDeviceList() {
        return BleManager.getInstance().getAllConnectedDevice();
    }

    /**
     * 根据蓝牙地址获取已连接的设备
     *
     * @return
     */
    public BleDevice getConnectDevice(String mac) {
        List<BleDevice> deviceList = getConnectedDeviceList();
        for (BleDevice device : deviceList) {
            if (device.getMac().equalsIgnoreCase(mac)) {
                return device;
            }
        }
        return null;
    }

    /**
     * 连接已绑定的设备
     * 但有时候会出现连接不上的情况,即使设备在连接范围内
     *
     * @param macAddress
     * @param connectCallback
     */
    @SuppressLint("MissingPermission")
    public void connectOnBondDevice(String macAddress, ConnectCallback connectCallback) {
        Set<BluetoothDevice> bondList = bleManager.getBondDeviceList();
        if (bondList != null) {
            for (BluetoothDevice device : bondList) {
                if (macAddress.equals(device.getAddress())) {
                    connect(macAddress, connectCallback);
                }
            }
        }
    }

    /**
     * 查看设备是否连接
     *
     * @param mac
     * @return
     */
    public boolean isConnected(String mac) {
        return BleManager.getInstance().isConnected(mac);
    }

    /**
     * 设置通知
     *
     * @param ob 设备or蓝牙地址
     *           notify 和indicate方法都可以设置通知,区别在于:indicate方法,从端收到通知会回发一个ACK包到主端
     */
    public void setNotify(Object ob, final NotifyOpenCallback notifyOpenCallback) {
        if (!isBleOpen()) {
            printError("蓝牙未打开...");
            return;
        }
        printLog("开启蓝牙通知...");
        final BleDevice device = getDevice(ob);
        if (device == null) {
            notifyOpenCallback.deviceNotConnect();
            return;
        }
        if (TextUtils.isEmpty(notify_uuid) || TextUtils.isEmpty(service_uuid)) {
            printError("uuid不能为空,notify uuid:" + notify_uuid + " , service uuid:" + service_uuid);
            notifyOpenCallback.uuidInvalid();
            return;
        }
        BleManager.getInstance().notify(device, service_uuid, notify_uuid, false, new BleNotifyCallback() {
            // 打开通知操作成功
            @Override
            public void onNotifySuccess() {//对应了onDescriptorWrite的回调
                printResult("设备蓝牙通知开启成功-" + device.getMac());
                if (notifyOpenCallback != null) {
                    notifyOpenCallback.onNotifySuccess(device);
                }
            }

            // 打开通知操作失败
            @Override
            public void onNotifyFailure(BleException exception) {
                printResult("设备蓝牙通知开启失败-" + device.getMac() + ",失败原因:" + exception.getDescription());
                if (notifyOpenCallback != null) {
                    notifyOpenCallback.onNotifyFailed(exception);
                }
            }

            // 打开通知后，设备发过来的数据将在这里出现
            @Override
            public void onCharacteristicChanged(String mac, byte[] data) {//对应了onCharacteristicChanged的回调
                printLog("设备" + mac + "通知返回的数据: " + Arrays.toString(data));
                if (notifyOpenCallback != null) {
                    notifyOpenCallback.onCharacteristicChanged(mac, data);
                }
            }
        });
    }

    public void closeNotify(BleDevice device) {
        BleManager.getInstance().stopNotify(device, service_uuid, notify_uuid);
    }

    /**
     * 写数据的回调,默认大于20字节时，会分割数据，大于20字节直接是默认写完一次后回调了onCharacteristicWrite后，就继续发下一个数据了。
     *
     * @param ob            蓝牙设备or地址
     * @param datas         传输的字节数据
     * @param writeCallback 监听回调
     */
    public void writeCharacteristic(Object ob, byte[] datas, final WriteCallback writeCallback) {
        if (!isBleOpen()) {
            printError("蓝牙未打开...");
            return;
        }
        BleDevice device = getDevice(ob);
        if (device == null) {
            writeCallback.deviceNotConnect();
            return;
        }
        if (TextUtils.isEmpty(service_uuid) || TextUtils.isEmpty(write_uuid)) {
            printError("uuid不能为空,write uuid:" + notify_uuid + " , service uuid:" + service_uuid);
            writeCallback.uuidInvalid();
            return;
        }

        printLog(device.getMac() + "开始写入数据...");
        BleManager.getInstance().write(
                device,
                service_uuid,
                write_uuid,
                datas,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {//对应了onCharacteristicWrite的回调
                        printResult(device.getMac() + "数据写入成功,写入数据为: " + Arrays.toString(justWrite));
                        writeCallback.writeSuccess(0, current, total, justWrite);
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        // 发送数据到设备失败
                        printResult(device.getMac() + "数据写入失败,原因: " + exception.getDescription());
                        writeCallback.error(exception.getDescription());
                    }
                });
    }

    /**
     * 写数据的回调,默认大于20字节时，会分割数据，大于20字节直接是默认写完一次后回调了onCharacteristicWrite后，就继续发下一个数据了。
     *
     * @param ob                  蓝牙设备or地址
     * @param intervalBetweenTime 两包之间间隔时间  ms
     * @param dates               传输的字节数据
     * @param writeCallback       监听回调
     */
    public void writeCharacteristic(Object ob, long intervalBetweenTime, byte[] dates, final WriteCallback writeCallback) {
        if (!isBleOpen()) {
            printError("蓝牙未打开...");
            return;
        }
        BleDevice device = getDevice(ob);
        if (device == null) {
            writeCallback.deviceNotConnect();
            return;
        }
        if (TextUtils.isEmpty(service_uuid) || TextUtils.isEmpty(write_uuid)) {
            printError("uuid不能为空,write uuid:" + notify_uuid + " , service uuid:" + service_uuid);
            writeCallback.uuidInvalid();
            return;
        }
        printLog(device.getMac() + "开始写入数据...");
        BleManager.getInstance().write(
                device,
                service_uuid,
                write_uuid,
                dates,
                intervalBetweenTime,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {//对应了onCharacteristicWrite的回调
                        printResult(device.getMac() + "数据写入成功,写入数据为: " + Arrays.toString(justWrite));
                        writeCallback.writeSuccess(0, current, total, justWrite);
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        // 发送数据到设备失败
                        printResult(device.getMac() + "数据写入失败,原因: " + exception.getDescription());
                        writeCallback.error(exception.getDescription());
                    }
                });
    }

    /**
     * 写数据的回调,默认大于20字节时，会分割数据，大于20字节直接是默认写完一次后回调了onCharacteristicWrite后，就继续发下一个数据了。
     *
     * @param ob            蓝牙地址or设备
     * @param dates         传输的字节数据
     * @param writeCallback 监听回调
     */
    public void writeCharacteristic(Object ob, byte[] dates, boolean nextPacketSuccess, long betweenPacketInterval, final WriteCallback writeCallback) {
        if (!isBleOpen()) {
            printError("蓝牙未打开...");
            return;
        }
        BleDevice device = getDevice(ob);
        if (device == null) {
            writeCallback.deviceNotConnect();
            return;
        }
        if (TextUtils.isEmpty(service_uuid) || TextUtils.isEmpty(write_uuid)) {
            printError("uuid不能为空,write uuid:" + notify_uuid + " , service uuid:" + service_uuid);
            writeCallback.uuidInvalid();
            return;
        }
        printLog(device.getMac() + "开始写入数据...");
        BleManager.getInstance().setIntervalBetweenPacket(betweenPacketInterval);
        BleManager.getInstance().setWhenNextPacketSuccess(nextPacketSuccess);
        BleManager.getInstance().write(
                device,
                service_uuid,
                write_uuid,
                dates,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {//对应了onCharacteristicWrite的回调
                        printResult(device.getMac() + "数据写入成功,写入数据为: " + Arrays.toString(justWrite));
                        writeCallback.writeSuccess(0, current, total, justWrite);
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        // 发送数据到设备失败
                        printResult(device.getMac() + "数据写入失败,原因: " + exception.getDescription());
                        writeCallback.error(exception.getDescription());
                    }
                });
    }


    public void getRssi(BleDevice device){
        if (device==null){
            printError("获取rssi,device = null");
            return;
        }
        BleManager.getInstance().readRssi(device, new BleRssiCallback() {
            @Override
            public void onRssiFailure(BleException e) {
                printResult("获取rssi失败: "+e.getDescription());
            }

            @Override
            public void onRssiSuccess(int i) {
                rssiSuccess(i);
            }
        });
    }


    /**
     * 配置扫描规则
     *
     * @param isFuzzy       是否模糊扫描
     * @param address       需要扫描的mac地址
     * @param name          需要扫描的蓝牙名称
     * @param isAutoConnect 是否自动连接,通常是扫描连接两个操作一起执行时设置为true
     * @return
     */
    private BleScanRuleConfig scanRule(boolean isFuzzy, String address, String name, boolean isAutoConnect) {
        BleScanRuleConfig ruleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(null)
                .setDeviceMac(address)
                .setDeviceName(isFuzzy, name)
                .setAutoConnect(isAutoConnect)
                .setScanTimeOut(scanTimeout)
                .build();
        return ruleConfig;
    }


    private BleDevice getDevice(Object ob) {
        BleDevice device = null;
        if (ob instanceof String) {
            device = getConnectDevice(ob.toString());
        } else if (ob instanceof BleDevice) {
            device = (BleDevice) ob;
        }
        return device;
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

    // 系统不支持
    private BleNotSupportListener bleNotSupportListener;

    // Inteerface--> 接口OnSystemNotSupportListener
    public interface BleNotSupportListener {
        void systemBleSupport();
    }

    // 对外方式setOnSystemNotSupportListener
    public void setBleNotSupportListener(BleNotSupportListener bleNotSupportListener) {
        this.bleNotSupportListener = bleNotSupportListener;
    }

    // 封装方法systemNotSupportNext
    private void systemBleNotSupportNext() {
        if (bleNotSupportListener != null) {
            bleNotSupportListener.systemBleSupport();
        }
    }

    private BleRssiSuccessListener rssiSuccessListener;
    public interface BleRssiSuccessListener{
        void rssiSuccess(int rssi);
    }

    public void setRssiSuccessListener(BleRssiSuccessListener rssiSuccessListener){
        this.rssiSuccessListener=rssiSuccessListener;
    }
    private void rssiSuccess(int rssi){
        if (rssiSuccessListener!=null){
            rssiSuccessListener.rssiSuccess(rssi);
        }
    }

}
