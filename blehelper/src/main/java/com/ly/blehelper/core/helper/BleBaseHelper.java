package com.ly.blehelper.core.helper;

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

import com.ly.blehelper.core.callbacks.ConnectCallback;
import com.ly.blehelper.core.callbacks.MTUCallback;
import com.ly.blehelper.core.callbacks.NotifyOpenCallback;
import com.ly.blehelper.core.callbacks.OpenListener;
import com.ly.blehelper.core.callbacks.ReadCallback;
import com.ly.blehelper.core.callbacks.ReadRssiCallback;
import com.ly.blehelper.core.callbacks.ScanCallback;
import com.ly.blehelper.core.callbacks.ScanConnectCallback;
import com.ly.blehelper.core.callbacks.WriteCallback;
import com.ly.blehelper.core.utils.Utils;

import java.util.List;
import java.util.Set;

import fastble.BleManager;
import fastble.callback.BleGattCallback;
import fastble.callback.BleIndicateCallback;
import fastble.callback.BleMtuChangedCallback;
import fastble.callback.BleNotifyCallback;
import fastble.callback.BleReadCallback;
import fastble.callback.BleRssiCallback;
import fastble.callback.BleScanAndConnectCallback;
import fastble.callback.BleScanCallback;
import fastble.callback.BleWriteCallback;
import fastble.data.BleDevice;
import fastble.exception.BleException;
import fastble.scan.BleScanRuleConfig;
import fastble.utils.BleLog;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/5/29 15:55
 * version: 1.0
 * <p>
 * 蓝牙辅助基类
 */
public abstract class BleBaseHelper {
    public static final String BLE_SCAN_START = "ble.scan.start";
    public static final String BLE_SCAN_FINISH = "ble.scan.finish";
    public static final String BLE_CONNECT_SUCCESS = "ble.conn.success";
    public static final String BLE_RECONNECT_SUCCESS = "ble.reconn.success";
    public static final String BLE_CONNECT_FAIL = "ble.conn.fail";
    public static final String BLE_DISCONNECT = "ble.disconnect";
    public static final String BLE_NOT_FOUND = "ble.not.found";
    public static final int BLE_SCAN_START_I = 0x1001;
    public static final int BLE_SCAN_FINISH_I = 0x1002;
    public static final int BLE_CONNECT_SUCCESS_I = 0x1003;
    public static final int BLE_CONNECT_FAIL_I = 0x1004;
    public static final int BLE_DISCONNECT_I = 0x1005;
    public static final int BLE_NOT_FOUND_I = 0x1006;
    public static final int BLE_RECONNECT_SUCCESS_I = 0x1007;
    public static final int START_TIMER = 0;
    private BleManager bleManager;
    private long scanTimeout = 10000;
    private String service_uuid;
    private String read_uuid;
    private String write_uuid;
    private String notify_uuid;
    private String indicate_uuid;

    public BleBaseHelper(Application application) {
        bleManager = BleManager.getInstance();
        bleManager.init(application);
        init();
    }

    /**
     * 初始化方法,主要为初始化一些必要的属性,如uuid
     */
    public abstract void init();

    /**
     * 是否打印日志
     *
     * @param open
     * @return
     */
    public BleBaseHelper openLog(boolean open) {
        bleManager.enableLog(open);
        return this;
    }

    /**
     * 设置重连次数,默认5次
     *
     * @param count
     * @return
     */
    public BleBaseHelper setReconnCount(int count) {
        bleManager.setReConnectCount(count);
        return this;
    }

    /**
     * 连接超时时间,默认10s
     *
     * @param time
     * @return
     */
    public BleBaseHelper setConnectOverTime(long time) {
        bleManager.setConnectOverTime(time);
        return this;
    }

    /**
     * 其他操作超时时间,默认5s
     *
     * @param timeout
     * @return
     */
    public BleBaseHelper setTimeout(int timeout) {
        bleManager.setOperateTimeout(timeout);
        return this;
    }

    /**
     * 扫描超时,默认10s,通常设置值不超过20s,因为扫描太耗内存
     *
     * @param timeout
     * @return
     */
    public BleBaseHelper setScanTimeout(long timeout) {
        this.scanTimeout = timeout;
        return this;
    }

    /**
     * 服务uuid
     *
     * @param service_uuid
     * @return
     */
    public BleBaseHelper setService_UUID(String service_uuid) {
        this.service_uuid = service_uuid;
        return this;
    }

    /**
     * 读特征uuid
     *
     * @param read_uuid
     * @return
     */
    public BleBaseHelper setRead_UUID(String read_uuid) {
        this.read_uuid = read_uuid;
        return this;
    }

    /**
     * 写特征uuid
     *
     * @param write_uuid
     * @return
     */
    public BleBaseHelper setWrite_UUID(String write_uuid) {
        this.write_uuid = write_uuid;
        return this;
    }

    /**
     * 通知uuid
     *
     * @param notify_uuid
     * @return
     */
    public BleBaseHelper setNotify_UUID(String notify_uuid) {
        this.notify_uuid = notify_uuid;
        return this;
    }

    /**
     * 通知uuid,区别于notify的是:indicate开启的通知从设备会回复一个确认包给主设备,notify则不会
     *
     * @param indicate_uuid
     * @return
     */
    public BleBaseHelper setIndicate_UUID(String indicate_uuid) {
        this.indicate_uuid = indicate_uuid;
        return this;
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
     * @param mac 蓝牙地址
     * @return
     */
    public boolean isBonded(String mac){
        Set<BluetoothDevice> bondList = bleManager.getBondDeviceList();
        for (BluetoothDevice bluetoothDevice:bondList){
            if (bluetoothDevice.getAddress().equalsIgnoreCase(mac)){
                return true;
            }
        }
        return false;
    }

    /**
     * 取消配对
     * @param mac 蓝牙地址
     */
    public void unBondDevice(String mac){
        Set<BluetoothDevice> bondList = bleManager.getBondDeviceList();
        for (BluetoothDevice bluetoothDevice:bondList){
            if (bluetoothDevice.getAddress().equalsIgnoreCase(mac)){
                Utils.unpairDevice(bluetoothDevice);
            }
        }
    }

    /**
     * 扫描蓝牙设备
     */
    public void scan(final ScanCallback scanListener) {
        if (!isBleOpen()) {
            scanListener.onBleDisable();
        }
        bleManager.scan(new BleScanCallback() {
            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                if (scanListener != null) {
                    scanListener.onScanFinished(scanResultList);
                }
            }

            @Override
            public void onScanStarted(boolean success) {
                if (scanListener != null) {
                    scanListener.onScanStart();
                }
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                if (scanListener != null) {
                    scanListener.onScanning(bleDevice);
                }
            }
        });
    }

    /**
     * 连接设备
     *
     * @param mac 蓝牙mac地址
     */
    public void connect(String mac, final ConnectCallback connectCallback) {
        BleScanRuleConfig config = new BleScanRuleConfig.Builder().setAutoConnect(true).build();
        bleManager.initScanRule(config);
        bleManager.connect(mac, new BleGattCallback() {

            /**
             * 开始连接
             */
            @Override
            public void onStartConnect() {

            }

            /**
             * 连接失败
             * @param bleDevice 连接设备
             * @param exception 异常信息
             */
            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
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
                if (!isBleOpen()) {
                    connectCallback.onBleDisable();
                } else if (connectCallback != null) {
                    connectCallback.onDisconnect(isActiveDisConnected, device);
                }
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
            connectCallback.onBleDisable();
        }
        bleManager.initScanRule(scanRule(isFuzzy, address, name, true));
        bleManager.scanAndConnect(new BleScanAndConnectCallback() {

            /**
             * 扫描结束
             * @param bleDevice 扫描到的设备,可以为null
             */
            @Override
            public void onScanFinished(BleDevice bleDevice) {
                if (connectCallback != null) {
                    connectCallback.onScanFinished(bleDevice);
                }
            }

            /**
             * 连接开始
             */
            @Override
            public void onStartConnect() {

            }

            /**
             * 连接失败
             * @param bleDevice 连接的设备,可以为null
             * @param exception 异常
             */
            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
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
                if (connectCallback != null) {
                    connectCallback.onConnectSuccess(bleDevice, gatt, status);
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
                if (bleDevice != null) {
                    BleLog.e("found device===" + bleDevice.getMac() + "/" + bleDevice.getName());
                }
            }

        });
    }

    /**
     * 设置最大传输单元,必须是主从两端支持才会生效
     * @param ob
     * @param mtu
     * @param mtuCallback
     */
    protected void setMTU(Object ob, int mtu, final MTUCallback mtuCallback) {
        BleDevice device = getDevice(ob);
        if (device == null) {
            mtuCallback.deviceNotConnect();
            return;
        }
        bleManager.setMtu(device, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
                if (mtuCallback != null) {
                    mtuCallback.setFail(exception.getDescription());
                }
            }

            @Override
            public void onMtuChanged(int mtu) {
                if (mtuCallback != null) {
                    mtuCallback.setSuccess(mtu);
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
    protected void disconnect(Object ob) {
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
     * 读取信号强度的回调
     *
     * @param ob
     * @param readRssiCallback
     */
    protected void readRssi(Object ob, final ReadRssiCallback readRssiCallback) {
        BleDevice device = getDevice(ob);
        if (device == null) {
            readRssiCallback.deviceNotConnect();
            return;
        }

        BleManager.getInstance().readRssi(
                device,
                new BleRssiCallback() {

                    @Override
                    public void onRssiFailure(BleException exception) {
                        // 读取设备的信号强度失败
                        readRssiCallback.onRssiFailure(exception);
                    }

                    @Override
                    public void onRssiSuccess(int rssi) {//对应了onReadRemoteRssi的回调
                        // 读取设备的信号强度成功
                        readRssiCallback.onRemoteRssi(rssi);
                    }
                });
    }
    /**
     * 通过rssi来算出距离
     *
     * @param rssi 设备的rssi值
     * @return 返回距离
     */
    public double getDistance(int rssi) {
        int iRssi = Math.abs(rssi);
        double power = (iRssi - 72) / (10 * 2.0);
        return Math.pow(10, power);
    }
    /**
     * 设置通知
     *
     * @param ob 设备or蓝牙地址
     *            notify 和indicate方法都可以设置通知,区别在于:indicate方法,从端收到通知会回发一个ACK包到主端
     */
    public void setNotify(Object ob, final NotifyOpenCallback notifyOpenCallback) {
        final BleDevice device = getDevice(ob);
        if (device == null) {
            notifyOpenCallback.deviceNotConnect();
            return;
        }
        if (TextUtils.isEmpty(notify_uuid) || TextUtils.isEmpty(service_uuid)) {
            notifyOpenCallback.uuidInvalid();
            return;
        }
        BleManager.getInstance().notify(device, service_uuid, notify_uuid, false, new BleNotifyCallback() {
            // 打开通知操作成功
            @Override
            public void onNotifySuccess() {//对应了onDescriptorWrite的回调
                if (notifyOpenCallback != null) {
                    notifyOpenCallback.onNotifySuccess(device);
                }
            }

            // 打开通知操作失败
            @Override
            public void onNotifyFailure(BleException exception) {
                if (notifyOpenCallback != null) {
                    notifyOpenCallback.onNotifyFailed(exception);
                }
            }

            // 打开通知后，设备发过来的数据将在这里出现
            @Override
            public void onCharacteristicChanged(String mac, byte[] data) {//对应了onCharacteristicChanged的回调
                if (notifyOpenCallback != null) {
                    notifyOpenCallback.onCharacteristicChanged(mac, data);
                }
            }
        });
    }

    public void closeNotify(BleDevice device){
        BleManager.getInstance().stopNotify(device,service_uuid,notify_uuid);
    }

    /**
     * 设置通知
     *
     * @param ob 设备or蓝牙地址
     *            notify 和indicate方法都可以设置通知,区别在于:indicate方法,从端收到通知会回发一个ACK包到主端
     */
    protected void setIndicate(Object ob, final NotifyOpenCallback notifyOpenCallback) {
        final BleDevice device = getDevice(ob);
        if (device == null) {
            notifyOpenCallback.deviceNotConnect();
            return;
        }
        if (TextUtils.isEmpty(indicate_uuid) || TextUtils.isEmpty(service_uuid)) {
            notifyOpenCallback.uuidInvalid();
            return;
        }
        BleManager.getInstance().indicate(device, service_uuid, indicate_uuid, false, new BleIndicateCallback() {
            // 打开通知操作成功
            @Override
            public void onIndicateSuccess() {//对应了onDescriptorWrite的回调
                if (notifyOpenCallback != null) {
                    notifyOpenCallback.onNotifySuccess(device);
                }
            }

            // 打开通知操作失败
            @Override
            public void onIndicateFailure(BleException exception) {
                if (notifyOpenCallback != null) {
                    notifyOpenCallback.onNotifyFailed(exception);
                }
            }

            // 打开通知后，设备发过来的数据将在这里出现
            @Override
            public void onCharacteristicChanged(String mac, byte[] data) {//对应了onCharacteristicChanged的回调
                if (notifyOpenCallback != null) {
                    notifyOpenCallback.onCharacteristicChanged(mac, data);
                }
            }
        });
    }
    /**
     * 读方法
     * 读取设备特征
     *
     * @param ob
     * @param readCallback
     */
    protected void readCharacteristic(Object ob, final ReadCallback readCallback) {
        BleDevice device = getDevice(ob);
        if (device == null) {
            readCallback.deviceNotConnect();
            return;
        }
        if (TextUtils.isEmpty(service_uuid) || TextUtils.isEmpty(read_uuid)) {
            readCallback.uuidInvalid();
            return;
        }
        BleManager.getInstance().read(
                device,
                service_uuid,
                read_uuid,
                new BleReadCallback() {
                    @Override
                    public void onReadSuccess(byte[] data) {//对应了onCharacteristicRead的回调
                        if (readCallback != null) {
                            readCallback.onReadSuccess(data);
                        }
                    }

                    @Override
                    public void onReadFailure(BleException exception) {
                        if (readCallback != null) {
                            readCallback.onReadFailure(exception);
                        }
                    }
                });
    }


    /**
     * 写数据的回调,默认大于20字节时，会分割数据，大于20字节直接是默认写完一次后回调了onCharacteristicWrite后，就继续发下一个数据了。
     *
     * @param ob           蓝牙设备or地址
     * @param datas         传输的字节数据
     * @param writeCallback 监听回调
     */
    public void writeCharacteristic(Object ob, byte[] datas, final WriteCallback writeCallback) {
        BleDevice device = getDevice(ob);
        if (device == null) {
            writeCallback.deviceNotConnect();
            return;
        }
        if (TextUtils.isEmpty(service_uuid) || TextUtils.isEmpty(write_uuid)) {
            writeCallback.uuidInvalid();
            return;
        }
        BleManager.getInstance().write(
                device,
                service_uuid,
                write_uuid,
                datas,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {//对应了onCharacteristicWrite的回调
                        writeCallback.writeSuccess(0, current, total, justWrite);
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        // 发送数据到设备失败
                        writeCallback.error(exception.getDescription());
                    }
                });
    }

    /**
     * 写数据的回调,默认大于20字节时，会分割数据，大于20字节直接是默认写完一次后回调了onCharacteristicWrite后，就继续发下一个数据了。
     *
     * @param ob                 蓝牙设备or地址
     * @param intervalBetweenTime 两包之间间隔时间  ms
     * @param dates               传输的字节数据
     * @param writeCallback       监听回调
     */
    public void writeCharacteristic(Object ob, long intervalBetweenTime, byte[] dates, final WriteCallback writeCallback) {
        BleDevice device = getDevice(ob);
        if (device == null) {
            writeCallback.deviceNotConnect();
            return;
        }
        if (TextUtils.isEmpty(service_uuid) || TextUtils.isEmpty(write_uuid)) {
            writeCallback.uuidInvalid();
            return;
        }
        BleManager.getInstance().write(
                device,
                service_uuid,
                write_uuid,
                dates,
                intervalBetweenTime,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {//对应了onCharacteristicWrite的回调
                        writeCallback.writeSuccess(0, current, total, justWrite);
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        // 发送数据到设备失败
                        writeCallback.error(exception.getDescription());
                    }
                });
    }

    /**
     * 写数据的回调,默认大于20字节时，会分割数据，大于20字节直接是默认写完一次后回调了onCharacteristicWrite后，就继续发下一个数据了。
     *
     * @param ob           蓝牙地址or设备
     * @param dates         传输的字节数据
     * @param writeCallback 监听回调
     */
    public void writeCharacteristic(Object ob, byte[] dates, boolean nextPacketSuccess, long betweenPacketInterval, final WriteCallback writeCallback) {
        BleDevice device = getDevice(ob);
        if (device == null) {
            writeCallback.deviceNotConnect();
            return;
        }
        if (TextUtils.isEmpty(service_uuid) || TextUtils.isEmpty(write_uuid)) {
            writeCallback.uuidInvalid();
            return;
        }
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
                        writeCallback.writeSuccess(0, current, total, justWrite);
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        // 发送数据到设备失败
                        writeCallback.error(exception.getDescription());
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


    private BleDevice getDevice(Object ob){
        BleDevice device=null;
        if (ob instanceof String){
            device=getConnectDevice(ob.toString());
        }else if (ob instanceof BleDevice){
            device=(BleDevice)ob;
        }
        return device;
    }

}
