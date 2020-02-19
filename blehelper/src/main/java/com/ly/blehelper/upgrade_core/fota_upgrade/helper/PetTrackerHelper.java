package com.ly.blehelper.upgrade_core.fota_upgrade.helper;

import android.app.Application;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ly.blehelper.core.helper.BleBaseHelper;
import com.ly.blehelper.upgrade_core.fota_upgrade.callbacks.U_ConnectCallback;
import com.ly.blehelper.upgrade_core.fota_upgrade.callbacks.U_NotifyOpenCallback;
import com.ly.blehelper.upgrade_core.fota_upgrade.callbacks.U_ReadRssiCallback;
import com.ly.blehelper.upgrade_core.fota_upgrade.callbacks.U_ScanCallback;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import fastble.data.BleDevice;
import fastble.exception.BleException;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/6/5 10:25
 * version: 1.0
 */
public class PetTrackerHelper extends BleBaseHelper {
    //action String
    private static final String TAG = "BluetoothService";
    private static final int BASE_SCAN_TIME = 10000;
    private static PetTrackerHelper bleConnectHelper;
    private Map<String, BleDevice> connDeviceMap = new HashMap<>();//第一个参数是address,第二个参数是BleDevice，已连接的设备
    private Map<String, Integer> readRssiMap = new HashMap<>();//第一个参数是address,第二个参数是rrsi值，读rssi的设备
    private Map<String, String> reconnDeviceMap = new HashMap<>();//第一个参数是address,第二个参数是name,需要重连的设备
    private Set<String> needConnectMap = new HashSet<>();//需要连接的设备
    private List<String> macaddrList = new ArrayList<>(); //扫描到的所以设备mac列表
    private ReconnHandler reConnHandler = new ReconnHandler(this);
    private Timer timer;
    private TimerTask timerTask;
    private double maxDistance;
    /**
     * 扫描开始监听
     */
    private OnScanStartListener scanStartListener;
    /**
     * 扫描完成监听
     */
    private OnScanFinishListener scanFinishListener;
    /**
     * 连接成功监听
     */
    private OnConnectSuccessListener connectSuccessListener;
    /**
     * 连接失败监听
     */
    private OnConnectFailListener connectFailListener;
    /**
     * 设备间断开蓝牙连接监听
     */
    private OnDisconnectListener disconnectListener;
    /**
     * 重连成功监听
     */
    private OnReconnectSuccessListener reconnectSuccessListener;
    /**
     * 重连失败监听
     */
    private OnReconnectFailListener reconnectFailListener;
    /**
     * 自身设备关闭蓝牙监听
     */
    private OnDeviceSelfDisableListener deviceSelfDisableListener;
    /**
     * 设备是否走远监听
     */
    private OnDeviceAwayListener deviceAwayListener;
    /**
     * 特征值变化监听
     */
    private OnCharacteristicChangeListener onCharacteristicChangeListener;
    /**
     * 通知开启成功监听
     */
    private NotifySuccessListener notifySuccessListener;
    /**
     * 通知开启失败监听
     */
    private NotifyFailListener notifyFailListener;
    /**
     * 通知回复(特征值变化)监听
     */
    private NotifyCharacteristicChangeListener notifyCharacteristicChangeListener;
    /**
     * 成功写入数据监听
     */
    private WriteSuccessListener writeSuccessListener;
    /**
     * 写入数据失败监听
     */
    private WriteFailListener writeFailListener;

    private U_ConnectCallback connectCallback = new U_ConnectCallback() {
        @Override
        public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt) {
            if (bleDevice != null) {
                if (isReConnectDevice(bleDevice.getMac())) {
                    getReconnectSuccessNext(bleDevice);
                } else {
                    getConnectSuccessNext(bleDevice, gatt);
                }
                addConnectDevice(bleDevice.getMac(), bleDevice);//加入已连接的列表中
                removeReConnectDevice(bleDevice.getMac());//从断开连接的列表中移除
                readRssi();//读rssi
            }
        }

        @Override
        public void onConnectFail(BleDevice bleDevice, BleException e) {
            getConnectFailNext(bleDevice, e.getDescription());
            addReConnectDevice(bleDevice.getMac(), bleDevice.getName());//加入重连列表中
            startReconnect(bleDevice.getMac(), BASE_SCAN_TIME);//重新连接
        }

        @Override
        public void onDisconnect(boolean isActiveDis, BleDevice device) {
            getDisconnectNext(isActiveDis, device, null);
            removeConnectDevice(device.getMac());//从已连接的列表中移除
            if (!isActiveDis) {//不是主动断开的
                addReConnectDevice(device.getMac(), device.getName());//加入重连列表中
                startReconnect(device.getMac(), 100);//开启重连
            } else {//主动断开连接的话，移除rssimap和reconnectmap
                removeReConnectDevice(device.getMac());
                removeRssiDevice(device.getMac());
            }
        }
    };

    private PetTrackerHelper(Application application) {
        super(application);
    }

    /**
     * 获取单例对象
     *
     * @return 返回单例对象
     */
    public static PetTrackerHelper getInstance(Application application) {
        if (bleConnectHelper == null) {
            synchronized (PetTrackerHelper.class) {
                if (bleConnectHelper == null) {
                    bleConnectHelper = new PetTrackerHelper(application);
                }
            }
        }
        return bleConnectHelper;
    }

    public Map<String, String> getReconnDeviceMap() {
        return reconnDeviceMap;
    }

    public Map<String, Integer> getReadRssiMap() {
        return readRssiMap;
    }

    @Override
    public void init() {
        //默认初始化的uuid
        String[] uuids = {"00005500-d102-11e1-9b23-00025b00a5a5", "00005501-d102-11e1-9b23-00025b00a5a5"};
        setService_UUID(uuids[0]).setWrite_UUID(uuids[1]).setNotify_UUID(uuids[1]);
    }


    /**
     * 设置tracker允许离开最大距离
     *
     * @param maxDistance 最大连接距离
     * @return
     */
    public PetTrackerHelper init(double maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }

    /**
     * 清楚定时器
     * 销毁handler
     */
    public void destroyHelper() {
        clearRssiTimer();
        cancelHandler();
    }

    /**
     * 扫描设备
     *
     * @param macList 需要匹配的设备list
     */
    public void scanList(final Map<String, String> macList) {
        if (macList == null || macList.size() == 0) {
            return;
        }

        macaddrList.clear();
        for (Map.Entry entry : macList.entrySet()) {
            String macKey = entry.getKey().toString();
            needConnectMap.add(macKey);
            if (connDeviceMap.containsKey(macKey)) {
                removeConnectDevice(macKey);//先移除掉，因为重新开始了
            }
            if (reconnDeviceMap.containsKey(macKey)) {
                removeReConnectDevice(macKey);//先移除掉，因为重新开始了
            }
            unBondDevice(macKey); //如果已经配对则先取消,因为android可能会出现已绑定的设备不会被扫描到的情况
        }
        scan(new U_ScanCallback() {
            @Override
            public void onScanFinished(List<BleDevice> bleDeviceList) {
                try {
                    for (BleDevice device : bleDeviceList) {
                        macaddrList.add(device.getMac().toUpperCase());
                    }
                    for (String mac : needConnectMap) {
                        if (!isConnected(mac)) {
                            if (macaddrList.contains(mac)) {
                                BleDevice bleDevice = null;
                                for (BleDevice device : bleDeviceList) {
                                    if (device.getMac().equalsIgnoreCase(mac)) {
                                        bleDevice = device;
                                    }
                                }
                                getScanFinishNext(bleDevice);
                                connect(bleDevice.getMac(), connectCallback);
                                checkRssi(bleDevice, mac, macList.get(mac));//检查rssi
                            } else {
                                checkRssi(null, mac, macList.get(mac));//检查rssi
//                            if (!connDeviceMap.containsKey(mac)) {//如果没扫描到，过段时间继续扫描
                                addReConnectDevice(mac, macList.get(mac));//加入重连列表中
                                startReconnect(mac, BASE_SCAN_TIME);//重新连接
                                getScanFinishNext(null);//
//                            }
                            }
                        } else {
                            connDeviceMap.put(mac, getConnectDevice(mac));
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void openNotify(final BleDevice device) {
        setNotify(device, new U_NotifyOpenCallback() {
            @Override
            public void onNotifySuccess(BleDevice device) {
                notifySuccess(device);
            }

            @Override
            public void onNotifyFailed(BleException e) {
                notifyFail(device, e.getDescription());
            }

            @Override
            public void onCharacteristicChanged(String mac, byte[] data) {
                notifyChange(mac, data);
            }
        });
    }

    public void closeNotify(BleDevice device) {
        super.closeNotify(device);
    }

    /**
     * 检查rssi来判断是否走远了
     *
     * @param bleDevice 蓝牙bleDevice
     * @param address   蓝牙mac地址
     */
    private void checkRssi(BleDevice bleDevice, String address, String name) {
        Log.e(TAG, "deviceInfo----" + bleDevice + "/" + address + "/" + readRssiMap.size() + "/" + connDeviceMap.size());
        if (bleDevice == null) {//如果扫描不到了
            if (readRssiMap.containsKey(address)) {//之前有存rssi的话，说明之前扫描到过
                Integer valueRssi = readRssiMap.get(address);
                double distance = getDistance(valueRssi);//判断距离
                distance = new BigDecimal(distance).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
                Log.e(TAG, "distance----" + distance + "/" + maxDistance);
                if (distance > maxDistance) {//如果距离大于10米了，那就走远了
                    getDeviceAwayNext(address, name);
                } else { //1米范围内扫描不到,统一定义为离线
                    deviceSelfDisableListener.deviceSelfDisable(address);
                }
            }
        }
        if (bleDevice != null) {//说明还能扫描到
            readRssiMap.put(bleDevice.getMac(), bleDevice.getRssi());
        }
    }

    /**
     * 开启读rssi
     */
    public void readRssi() {
        clearRssiTimer();//清除之前的
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (connDeviceMap.size() > 0) {//只检查已成功连接的设备
                    for (final Map.Entry<String, BleDevice> device : connDeviceMap.entrySet()) {
                        readRssi(device.getValue(), new U_ReadRssiCallback() {
                            @Override
                            public void onRemoteRssi(int rssi) {
                                readRssiMap.put(device.getKey(), rssi);
                            }
                        });
                    }
                }
            }
        };
        timer.schedule(timerTask, 0, 2000);
    }

    /**
     * 清除timer
     */
    public void clearRssiTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    //----------------对外listeners----------------------

    /**
     * 开始重连
     *
     * @param address      蓝牙mac地址
     * @param delaymiillis 延时时间
     */
    private void startReconnect(String address, long delaymiillis) {
        if (reConnHandler==null) return;
        Message message = reConnHandler.obtainMessage(START_TIMER);
        message.obj = address;
        reConnHandler.sendMessageDelayed(message, delaymiillis);
    }

    public void cancelHandler() {
        if (reConnHandler != null) {
            reConnHandler.removeCallbacksAndMessages(null);
            reConnHandler = null;
        }
    }

    /**
     * 开启重连
     *
     * @param key 蓝牙地址
     */
    private void reconnListening(String key) {
        Map<String, String> reconnDeviceMap = getReconnDeviceMap();
        Set<Map.Entry<String, String>> keys = reconnDeviceMap.entrySet();
        Map<String, String> scanMap = new HashMap<>();
        if (reconnDeviceMap != null && reconnDeviceMap.size() > 0) {//遍历需要重连的设备
            for (Map.Entry<String, String> entry : keys) {
                String key1 = entry.getKey();
                if (key1.equals(key)) {
                    scanMap.put(key1, entry.getValue());
                }
            }
            scanList(scanMap);
        } else {

        }
    }

    /**
     * 加入重连的设备中
     *
     * @param address 蓝牙地址
     * @param name    蓝牙名称
     */
    private void addReConnectDevice(String address, String name) {
        reconnDeviceMap.put(address, name);
    }

    /**
     * 移除重连的设备
     *
     * @param address 蓝牙地址
     */
    public void removeReConnectDevice(String address) {
        reconnDeviceMap.remove(address);
    }

    /**
     * 检查是否在重连的列表中
     *
     * @param address 蓝牙地址
     * @return
     */
    public boolean isReConnectDevice(String address) {
        return reconnDeviceMap.containsKey(address);
    }

    /**
     * 加入已连接的设备
     *
     * @param address   蓝牙地址
     * @param bleDevice 蓝牙设备
     */
    public void addConnectDevice(String address, BleDevice bleDevice) {
        connDeviceMap.put(address, bleDevice);
    }

    /**
     * 移除已连接的设备
     *
     * @param address 蓝牙地址
     */
    public void removeConnectDevice(String address) {
        connDeviceMap.remove(address);
    }

    public void removeRssiDevice(String address) {
        readRssiMap.remove(address);
    }

    public void removeDevice(String mac) {
        needConnectMap.remove(mac);
        readRssiMap.remove(mac);
        connDeviceMap.remove(mac);
        reconnDeviceMap.remove(mac);
    }

    public void setScanStartListener(OnScanStartListener scanStartListener) {
        this.scanStartListener = scanStartListener;
    }

    private void getScanStartNext() {
        if (scanStartListener != null) {
            scanStartListener.scanStart();
        }
    }

    public void setScanFinishListener(OnScanFinishListener scanFinishListener) {
        this.scanFinishListener = scanFinishListener;
    }

    private void getScanFinishNext(BleDevice bleDevice) {
        if (scanFinishListener != null) {
            scanFinishListener.scanFinish(bleDevice);
        }
    }

    public void setConnectSuccessListener(OnConnectSuccessListener connectSuccessListener) {
        this.connectSuccessListener = connectSuccessListener;
    }

    private void getConnectSuccessNext(BleDevice bleDevice, BluetoothGatt gatt) {
        if (connectSuccessListener != null) {
            connectSuccessListener.connectSuccess(bleDevice, gatt);
        }
    }

    public void setConnectFailListener(OnConnectFailListener connectFailListener) {
        this.connectFailListener = connectFailListener;
    }

    private void getConnectFailNext(BleDevice bleDevice, String description) {
        if (connectFailListener != null) {
            connectFailListener.connectFail(bleDevice, description);
        }
    }

    public void setDisconnectListener(OnDisconnectListener disconnectListener) {
        this.disconnectListener = disconnectListener;
    }

    private void getDisconnectNext(boolean isActiveDisConnect, BleDevice bleDevice, BluetoothGatt gatt) {
        if (disconnectListener != null) {
            disconnectListener.disconnect(isActiveDisConnect, bleDevice, gatt);
        }
    }

    public void setReconnectSuccessListener(OnReconnectSuccessListener reconnectSuccessListener) {
        this.reconnectSuccessListener = reconnectSuccessListener;
    }

    private void getReconnectSuccessNext(BleDevice bleDevice) {
        if (reconnectSuccessListener != null) {
            reconnectSuccessListener.reconnectSuccess(bleDevice);
        }
    }

    public void setReconnectFailListener(OnReconnectFailListener reconnectFailListener) {
        this.reconnectFailListener = reconnectFailListener;
    }

    private void getReconnectFailNext(BleDevice bleDevice) {
        if (reconnectFailListener != null) {
            reconnectFailListener.reconnectFail(bleDevice);
        }
    }

    public void setDeviceSelfDisableListener(OnDeviceSelfDisableListener deviceSelfDisableListener) {
        this.deviceSelfDisableListener = deviceSelfDisableListener;
    }

    private void getDeviceSelfDisableNext(String mac) {
        if (deviceSelfDisableListener != null) {
            deviceSelfDisableListener.deviceSelfDisable(mac);
        }
    }

    public void setDeviceAwayListener(OnDeviceAwayListener deviceAwayListener) {
        this.deviceAwayListener = deviceAwayListener;
    }

    private void getDeviceAwayNext(String address, String name) {
        if (deviceAwayListener != null) {
            deviceAwayListener.deviceAway(address, name);
        }
    }

    private void notifySuccess(BleDevice device) {
        if (notifySuccessListener != null) {
            notifySuccessListener.notifySuccess(device);
        }
    }

    private void notifyFail(BleDevice device, String err) {
        if (notifyFailListener != null) {
            notifyFailListener.notifyFail(device, err);
        }
    }

    private void notifyChange(String mac, byte[] data) {
        if (notifyCharacteristicChangeListener != null) {
            notifyCharacteristicChangeListener.characteristicChange(mac, data);
        }
    }

    private void writeSuccesss(int crr, int tl, byte[] data) {
        if (writeSuccessListener != null) {
            writeSuccessListener.writeSuccess(crr, tl, data);
        }
    }

    private void writeFail(String err) {
        if (writeFailListener != null) {
            writeFailListener.writeFail(err);
        }
    }

    public void setNotifySuccessListener(NotifySuccessListener notifySuccessListener) {
        this.notifySuccessListener = notifySuccessListener;
    }

    public void setNotifyFailListener(NotifyFailListener notifyFailListener) {
        this.notifyFailListener = notifyFailListener;
    }

    public void setNotifyCharacteristicChangeListener(NotifyCharacteristicChangeListener notifyCharacteristicChangeListener) {
        this.notifyCharacteristicChangeListener = notifyCharacteristicChangeListener;
    }

    public void setWriteSuccessListener(WriteSuccessListener writeSuccessListener) {
        this.writeSuccessListener = writeSuccessListener;
    }

    public void setWriteFailListener(WriteFailListener writeFailListener) {
        this.writeFailListener = writeFailListener;
    }

    /**
     * 设备返回来的数据变化
     *
     * @param charactoristicChangeListener
     */
    public void setCharacteristicChangeListener(OnCharacteristicChangeListener charactoristicChangeListener) {
        this.onCharacteristicChangeListener = charactoristicChangeListener;
    }

    private void getCharacteristicChange(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (onCharacteristicChangeListener != null) {
            onCharacteristicChangeListener.onCharacteristicChange(gatt, characteristic);
        }
    }

    public interface OnScanStartListener {
        void scanStart();
    }

    public interface OnScanFinishListener {
        void scanFinish(BleDevice bleDevice);
    }

    public interface OnConnectSuccessListener {
        void connectSuccess(BleDevice bleDevice, BluetoothGatt gatt);
    }

    public interface OnConnectFailListener {
        void connectFail(BleDevice bleDevice, String description);
    }

    public interface OnDisconnectListener {
        void disconnect(boolean isActiveDisConnect, BleDevice bleDevice, BluetoothGatt gatt);
    }

    public interface OnReconnectSuccessListener {
        void reconnectSuccess(BleDevice bleDevice);
    }

    public interface OnReconnectFailListener {
        void reconnectFail(BleDevice bleDevice);
    }

    public interface OnDeviceSelfDisableListener {
        void deviceSelfDisable(String mac);
    }

    public interface OnDeviceAwayListener {
        void deviceAway(String address, String name);
    }

    public interface OnCharacteristicChangeListener {
        void onCharacteristicChange(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
    }

    public interface NotifySuccessListener {
        void notifySuccess(BleDevice device);
    }

    public interface NotifyFailListener {
        void notifyFail(BleDevice device, String err);
    }

    public interface NotifyCharacteristicChangeListener {
        void characteristicChange(String mac, byte[] data);
    }

    public interface WriteSuccessListener {
        void writeSuccess(int current, int total, byte[] packet);
    }

    public interface WriteFailListener {
        void writeFail(String err);
    }

    //重连的hanlder
    public static class ReconnHandler extends Handler {
        private WeakReference<PetTrackerHelper> reconnectHelperWeakReference;

        public ReconnHandler(PetTrackerHelper reconnectHelper) {
            reconnectHelperWeakReference = new WeakReference<>(reconnectHelper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            PetTrackerHelper connect = reconnectHelperWeakReference.get();
            switch (msg.what) {
                case START_TIMER:
                    String key = (String) msg.obj;//key为蓝牙地址
                    if (key != null) {
                        connect.reconnListening(key);
                    }
                    break;
            }
        }
    }

}
