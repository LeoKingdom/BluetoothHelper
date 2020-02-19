package com.ly.blehelper.esim.helper;


import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothGatt;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;


import com.ly.blehelper.core.callbacks.NotifyOpenCallback;
import com.ly.blehelper.core.callbacks.ScanConnectCallback;
import com.ly.blehelper.core.callbacks.WriteCallback;
import com.ly.blehelper.core.helper.BleBaseHelper;
import com.ly.blehelper.esim.callbacks.EsimActiveCallback;
import com.ly.blehelper.esim.callbacks.EsimCancelCallback;
import com.ly.blehelper.esim.callbacks.EsimDataCallback;
import com.ly.blehelper.esim.callbacks.EsimProfileDeleteCallback;
import com.ly.blehelper.upgrade_core.fota_upgrade.utils.ActionUtils;
import com.ly.blehelper.upgrade_core.fota_upgrade.utils.CRCCheckUtils;
import com.ly.blehelper.upgrade_core.fota_upgrade.utils.DataPacketUtils;
import com.ly.blehelper.upgrade_core.fota_upgrade.utils.OrderSetUtils;
import com.ly.blehelper.upgrade_core.fota_upgrade.utils.TransformUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fastble.data.BleDevice;
import fastble.exception.BleException;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/15 11:11
 * version: 1.0
 * <p>
 * esim卡工具类,主要为外围设备和中心设备围绕esim展开的交互(蓝牙)
 */
public class ESimActiveHelper extends BleBaseHelper {
    private final int DATA_TIMEOUT = 1000;
    private final int DATA_CHECK = 1001;//校验数据
    private final int URL_RESPONSE = 1002;//回复url数据是否正确
    private final int JSON_BODY_EACH_FRAME = 1003;//jsonBody数据分帧
    private final int PREPARE_URL_TRANSFORM = 1004;//准备传输url
    private final int BEGIN_URL_TRANSFORM = 1005;//开始传输url
    private String mMac;
    private String serviceUuid;
    private String notifyUuid;
    private String writeUuid;
    private BleDevice bleDevice;
    private int tLength = 0;
    private int CURRENT_ACTION = -1; //激活第一步,即请求获取url
    private int CURRENT_STEP = -1; //当前进行激活的第几步
    private byte[] dataBytes;
    private int packets; //每次回复需要接收的包数
    private int sumPackets = 0;//已收包数
    private List<Integer> sumPacketList = new ArrayList<>();
    private byte[] headBytes;
    private int totalFrame;
    private int currentFrame = 0;
    private byte[] jsonBodyBytes;
    private byte currentEventId;
    private boolean notifyOpen = false;
    private EsimActiveCallback esimActiveCallback;
    private EsimCancelCallback esimCancelCallback;
    private EsimProfileDeleteCallback esimProfileDeleteCallback;
    private EsimDataCallback.EsimUrlListener esimUrlListener;
    private EsimDataCallback.EsimUrlPostListener esimUrlPostListener;
    //    private String testUrl = "1$oemgsma-lpa-json.demo.gemalto.com$DD6036B4D647DAC642BFCE7A73490CBFEF46904605EBED7572BBABD5AEA44185";
    private String testUrl = "1$369f3f19.cpolar.cn$04386-AGYFT-A74Y8-3F815";
    private WriteCallback writeListener = new WriteCallback() {
        @Override
        public void writeSuccess(int actionType, int current, int total, byte[] justWrite) {
            Log.e("write----", Arrays.toString(justWrite));
            if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_URL) {
                handler.sendEmptyMessageDelayed(BEGIN_URL_TRANSFORM, 20);
            }
        }

        @Override
        public void error(String err) {
        }
    };
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DATA_TIMEOUT: //超时,做丢包逻辑
//                    CURRENT_ACTION = 1; //将url字节转换
                    if (sumPackets == 0 && sumPackets == packets) { //未丢包,下一步做处理
                        return;
                    }
                    if (sumPackets != 0) { //丢包处理

                    }
                    break;
                case DATA_CHECK: //数据校验
                    if (dataBytes == null) return;
//                    Log.e("bytes---", TransformUtils.bytesToHexString(dataBytes));
                    byte crcByte = dataBytes[dataBytes.length - 1];//最后一个字节为crc校验
                    byte[] dataByte = Arrays.copyOfRange(dataBytes, 0, dataBytes.length - 1);
                    Log.e("bytes---1", TransformUtils.bytesToHexString(dataByte));
                    byte crcByte1 = CRCCheckUtils.calcCrc8(dataByte);
                    Log.e("crcByte---", TransformUtils.byte2Int(crcByte) + "/" + TransformUtils.byte2Int(crcByte1));
                    String data = TransformUtils.bytes2String(dataByte);
                    if (dataBytes != null) {
                        dataBytes = null;
                    }
                    if (crcByte == crcByte1) { //数据正确
                        Message message = handler.obtainMessage();
                        if (CURRENT_ACTION == 0) { //url数据正确
                            esimUrlListener.urlSuccess(CURRENT_STEP, data);
                            byte[] responeBytes = TransformUtils.combineArrays(OrderSetUtils.ESIM_PROFILE_DOWNLOAD_URL_RESP, new byte[]{0, 0});
                            message.what = URL_RESPONSE;
                            message.obj = responeBytes;
                            handler.sendMessageDelayed(message, 200);
                        } else if (CURRENT_ACTION == 4) { //post数据正确
                            esimUrlPostListener.urlPostSuccess(CURRENT_STEP, data);
                            byte[] urlResBytes = OrderSetUtils.ESIM_PROFILE_DOWNLOAD_POST_RESP;
                            urlResBytes[2] = (byte) 0x02;
                            urlResBytes[6] = currentEventId;
                            byte[] urlResponseBytes = TransformUtils.combineArrays(urlResBytes, new byte[]{0, 0});
                            writeCharacteristic(bleDevice, urlResponseBytes, writeListener);
                            CURRENT_ACTION = 3;
                        }
                    } else { //数据错误
                        if (CURRENT_ACTION == 0) { //url数据错误

                        } else if (CURRENT_ACTION == 4) { //post数据错误

                        }
                    }
//                    CURRENT_ACTION = 2; //数据校验
                    break;
                case URL_RESPONSE:
                    CURRENT_ACTION = 3;//url校验回复
                    if (msg.obj != null) {
                        byte[] resBytes = (byte[]) msg.obj;
                        resBytes[2] = (byte) 0x02;
                        resBytes[6] = currentEventId;
                        writeCharacteristic(bleDevice, resBytes, writeListener);
                    }
                    break;
                case JSON_BODY_EACH_FRAME:
                    CURRENT_ACTION = JSON_BODY_EACH_FRAME;
                    currentFrame++;
                    if (currentFrame > totalFrame) return;
                    byte[] requestBytes = DataPacketUtils.sortEachFrame(jsonBodyBytes, currentFrame, totalFrame);
                    byte[] currentFrameBytes = DataPacketUtils.currentPacket(jsonBodyBytes, currentFrame, totalFrame);
                    byte[] dataLengts = DataPacketUtils.dataLenght(currentFrameBytes.length);
                    headBytes[1] = dataLengts[1];
                    headBytes[2] = dataLengts[2];
                    headBytes[3] = (byte) totalFrame;
                    headBytes[4] = (byte) currentFrame;
                    headBytes[6] = currentEventId;
                    writeCharacteristic(bleDevice, headBytes, writeListener);
                    if (currentFrame == totalFrame) {
                        CURRENT_ACTION = 3;
                        currentFrame = 0;
                        totalFrame = 0;
                    }
//                    Log.e("dataL---", currentFrameBytes.length + "/" + requestBytes.length + "/" + Arrays.toString(headBytes));
                    writeCharacteristic(bleDevice, 20, requestBytes, writeListener);
                    break;
                case PREPARE_URL_TRANSFORM:
                    byte[] urlHeadByte = DataPacketUtils.frameHeadBytes(OrderSetUtils.ESIM_SM_DP, testUrl.getBytes(), 1, 1);
                    writeCharacteristic(bleDevice, urlHeadByte, writeListener);
                    break;
                case BEGIN_URL_TRANSFORM:
                    CURRENT_ACTION = BEGIN_URL_TRANSFORM;
                    byte[] urlByte = DataPacketUtils.splitEachFrameBytes(testUrl.getBytes());
                    writeCharacteristic(bleDevice, 20, urlByte, writeListener);
                    break;
            }
        }
    };
    private NotifyOpenCallback notifyListener = new NotifyOpenCallback() {
        @Override
        public void onNotifySuccess(BleDevice device) {
//            Log.e("notifySuccess---", device + "");
            notifyOpen = true;
            if (device.getMac().equalsIgnoreCase(mMac)) {
                notifyBesiness();
            }
        }

        @Override
        public void onNotifyFailed(BleException e) {
            notifyOpen = false;
            if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE_FIRST) {
                setNotify(bleDevice, notifyListener);
            }
        }

        @Override
        public void onCharacteristicChanged(String mac, byte[] data) {
            Log.e("dataCallback---", TransformUtils.bytesToHexString(data));
            if (mac.equalsIgnoreCase(mMac)) {
                if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE_FIRST || CURRENT_ACTION == 3) {//获取url和postdata回复
                    if (data.length == 7 || data.length == 9) {
                        headBytes = data;
                        int length1 = TransformUtils.byte2Int(data[1]);
                        int length2 = TransformUtils.byte2Int(data[2]);
                        if (length1 != 0) {
                            tLength = (length1 * 256) + length2;
                        } else {
                            tLength = length2;
                        }
                        if (data[5] == (byte) 0x30) {
                            byte eventId = data[6];
                            currentEventId = eventId;
                            if (eventId == (byte) 0x51 || eventId == (byte) 0x54 || eventId == (byte) 0x57 || eventId == (byte) 0x5A) {
                                CURRENT_ACTION = 0;//请求url的回复指令
                            } else if (eventId == (byte) 0x52 || eventId == (byte) 0x55 || eventId == (byte) 0x58 || eventId == (byte) 0x5B) {
                                CURRENT_ACTION = 4; //请求POST的回复
                            }
                        }
                        packets = tLength % 19 == 0 ? tLength / 19 : tLength / 19 + 1;
                        handler.sendEmptyMessageDelayed(DATA_TIMEOUT, packets * 50 + 3000);//超时处理
                    }
                } else if (CURRENT_ACTION == 0 || CURRENT_ACTION == 4) {//处理url和postdata,即组包
                    sumPackets++;
                    sumPacketList.add((int) data[0]);
                    data = Arrays.copyOfRange(data, 1, data.length);
                    if (dataBytes == null) {
                        dataBytes = data;
                    } else {
                        dataBytes = TransformUtils.combineArrays(dataBytes, data);
                    }
                    if (sumPackets == packets) { //未丢包
                        handler.sendEmptyMessageDelayed(DATA_CHECK, packets * 50);
                        sumPackets = 0;
                        sumPacketList.clear();
                    }
                } else if (CURRENT_ACTION == JSON_BODY_EACH_FRAME) {
                    handler.sendEmptyMessageDelayed(JSON_BODY_EACH_FRAME, 50);
                } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE_FORTH) {//下载profile结果
                    if (data.length > 7) {
                        boolean moduleId = data[5] == (byte) 0x30;
                        boolean eventId = data[6] == (byte) 0x5D;
                        if (moduleId && eventId) {
                            int code = 0; //profile下载成功
                            if (data[data.length - 1] != 0) {
                                //profile下载失败
                                code = -1;
                            }
                            esimUrlPostListener.profileSuccess(code);
                        }
                    }
                } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE) {//激活结果
                    esimActiveCallback.notifyCallback(data);
                    if (data.length > 7) {
                        boolean moduleId = data[5] == (byte) 0x30;
                        boolean eventId = data[6] == (byte) 0x08;
                        if (moduleId && eventId) {
                            int code = 0;//激活成功
                            if (data[data.length - 1] != 0) {
                                //激活失败
                                code = -1;
                            }
                            esimActiveCallback.activeResult(code == 0);
                        }
                    }
                } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_UNACTIVE) {//去活结果
                    esimActiveCallback.notifyCallback(data);
                    if (data.length > 7) {
                        boolean moduleId = data[5] == (byte) 0x30;
                        boolean eventId = data[6] == (byte) 0x09;
                        if (moduleId && eventId) {
                            int code = 0;//去活成功
                            if (data[data.length - 1] != 0) {
                                //去活失败
                                code = -1;
                            }
                            esimCancelCallback.cancelResult(code == 0);
                        }
                    }
                } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_URL) {//设置url的ack包
                    if (data.length > 7) {
                        if (data[data.length - 1] == 0) { //成功回复,开始设置url
                            handler.sendEmptyMessageDelayed(PREPARE_URL_TRANSFORM, 20);
                        }
                    }
                } else if (CURRENT_ACTION == BEGIN_URL_TRANSFORM) {
                    esimActiveCallback.notifyCallback(data);
                }else if (CURRENT_ACTION==ActionUtils.ACTION_ESIM_PROFILE_DELETE){
                    esimActiveCallback.notifyCallback(data);
                    if (data.length > 7) {
                        boolean moduleId = data[5] == (byte) 0x30;
                        boolean eventId = data[6] == (byte) 0x0A;
                        if (moduleId && eventId) {
                            int code = 0;//去活成功
                            if (data[data.length - 1] != 0) {
                                //去活失败
                                code = -1;
                            }
                            esimProfileDeleteCallback.deleteResult(code == 0);
                        }
                    }
                }
            }
        }
    };
    private ScanConnectCallback handleListener = new ScanConnectCallback() {

        @Override
        public void onScanFinished(BleDevice bleDevice) {
            Log.e("scanFinish---", bleDevice + "");
            if (bleDevice == null) {
                if (esimActiveCallback != null) {
                    esimActiveCallback.deviceNotFound();
                }
            }
        }

        @Override
        public void onConnectSuccess(BleDevice device, BluetoothGatt gatt, int status) {
            Log.e("connSuccess---", device + "");
            if (device != null) {
                bleDevice = device;
                setNotify(device, notifyListener);
            }
        }

        @Override
        public void onConnectFailed(BleDevice bleDevice, String description) {
            Log.e("connFail---", description + "");
        }

        @Override
        public void onDisconnect(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt) {
            Log.e("disConn---", device + "/" + isActiveDisConnected);
            notifyOpen = false;
        }
    };

    public ESimActiveHelper(Application application) {
        super(application);
    }

    public void initUuid(String sUUID, String wUUID, String nUUID) {
        this.serviceUuid = sUUID;
        this.writeUuid = wUUID;
        this.notifyUuid = nUUID;
    }

    @Override
    public void init() {
        //默认初始化的uuid
        String[] uuids = {"00005500-d102-11e1-9b23-00025b00a5a5", "00005501-d102-11e1-9b23-00025b00a5a5"};
        setService_UUID(serviceUuid == null ? uuids[0] : serviceUuid).setWrite_UUID(writeUuid == null ? uuids[1] : writeUuid).setNotify_UUID(notifyUuid == null ? uuids[1] : notifyUuid);
    }

    public void setUrl(String url) {
        if (!TextUtils.isEmpty(url)) {
            testUrl = url;
        }
    }

    private void notifyBesiness() {
        if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE_FIRST) {
            currentEventId = (byte) 0x50;
            writeCharacteristic(bleDevice, OrderSetUtils.ESIM_PROFILE_START, writeListener);
        } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE) {
            writeCharacteristic(bleDevice, OrderSetUtils.ESIM_ACTIVE, writeListener);
        } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_UNACTIVE) {
            writeCharacteristic(bleDevice, OrderSetUtils.ESIM_CANCEL, writeListener);
        } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_URL) {
            handler.sendEmptyMessageDelayed(PREPARE_URL_TRANSFORM, 20);
        }else if (CURRENT_ACTION==ActionUtils.ACTION_ESIM_PROFILE_DELETE){
            writeCharacteristic(bleDevice, OrderSetUtils.ESIM_PROFILE_DELETE, writeListener);
        }
    }

    private void prepare(byte[] data) {

        if (bleDevice == null) {
            BleDevice device = getConnectDevice(mMac);
            if (device == null) {
                if (esimActiveCallback != null) {
                    esimActiveCallback.deviceNotConnect();
                }
                scanAndConnect(true, mMac, "", handleListener);
            } else {
                this.bleDevice = device;
                setNotify(device, notifyListener);
            }
        } else {
            writeCharacteristic(bleDevice, data, writeListener);
        }
    }

    public void deleteProfile(String mac){
        this.mMac=mac;
        CURRENT_ACTION=ActionUtils.ACTION_ESIM_PROFILE_DELETE;
        prepare(OrderSetUtils.ESIM_PROFILE_DELETE);
    }

    public void setSMDPUrl(String mac) {
        this.mMac = mac;
        CURRENT_ACTION = ActionUtils.ACTION_ESIM_URL;
        byte[] urlHeadByte = DataPacketUtils.frameHeadBytes(OrderSetUtils.ESIM_SM_DP, testUrl.getBytes(), 1, 1);
        prepare(urlHeadByte);
    }

    /**
     * @param mac 设备的蓝牙地址
     */
    public void esimActiveFirst(String mac) {
        this.mMac = mac;
        CURRENT_ACTION = ActionUtils.ACTION_ESIM_ACTIVE_FIRST;
        if (TextUtils.isEmpty(mac)) {
            if (esimActiveCallback != null) {
                esimActiveCallback.macInvalidate();
            }
        } else {
            BleDevice device = getConnectDevice(mac);
            if (device == null) {
                if (esimActiveCallback != null) {
                    esimActiveCallback.deviceNotConnect();
                }
                scanAndConnect(true, mac, "", handleListener);
            } else {
                this.bleDevice = device;
                if (notifyOpen) {
                    notifyBesiness();
                } else {
                    setNotify(device, notifyListener);
                }
            }
        }
    }

    /**
     * 此方法会调用三次
     * 第一次: 激活esim的第一步,验证esim服务器, tracker->server
     * 第二次: 激活esim第二步,服务器验证tracker, server->tracker
     * 第三次: 激活esim第三步,传输profile数据
     *
     * @param statusCode 服务器状态码
     * @param jsonBody   使用第一步和第二步的url请求得到的body
     */
    public void esimActiveNext(int statusCode, String jsonBody) {
        CURRENT_ACTION = ActionUtils.ACTION_ESIM_ACTIVE_NEXT;
        if (bleDevice == null) {
            scanAndConnect(true, mMac, "", handleListener);
            return;
        }
        if (headBytes == null) return;
        currentEventId++;
        Log.e("dataLength---", jsonBody.length() + "/" + currentEventId);
        byte[] codeBytes = TransformUtils.int2TwoBytes(statusCode);
        byte[] jsonBytes = jsonBody.getBytes();
        jsonBodyBytes = TransformUtils.combineArrays(codeBytes, jsonBytes);
        totalFrame = jsonBodyBytes.length % 4096 == 0 ? jsonBodyBytes.length / 4096 : jsonBodyBytes.length / 4096 + 1;
        handler.sendEmptyMessageDelayed(JSON_BODY_EACH_FRAME, 20);

    }

    /**
     * 激活esim第四步,profile准备工作完成,这时候用户就可以激活esim了
     *
     * @param statusCode 服务器状态码
     */
    public void esimActiveResult(int statusCode) {
        CURRENT_ACTION = ActionUtils.ACTION_ESIM_ACTIVE_FORTH;
        currentEventId++;
        byte[] resultOrderBytes = OrderSetUtils.ESIM_PROFILE_DOWNLOAD_POST_RESP;
        byte[] codeBytes = TransformUtils.int2TwoBytes(statusCode);
        byte[] resultBytes = TransformUtils.combineArrays(resultOrderBytes, codeBytes);
        resultBytes[2] = (byte) 0x02;
        resultBytes[6] = currentEventId;
        writeCharacteristic(bleDevice, resultBytes, writeListener);
    }

    /**
     * 激活,这一步才是最终的激活,前面四步都是为激活esim做准备
     */
    public void esimActive(String mac) {
        this.mMac = mac;
        CURRENT_ACTION = ActionUtils.ACTION_ESIM_ACTIVE;
        if (bleDevice == null) {
            BleDevice device = getConnectDevice(mac);
            if (device == null) {
                if (esimActiveCallback != null) {
                    esimActiveCallback.deviceNotConnect();
                }
                scanAndConnect(true, mac, "", handleListener);
            } else {
                if (notifyOpen) {
                    notifyBesiness();
                } else {
                    setNotify(device, notifyListener);
                }
            }
        } else {
            writeCharacteristic(bleDevice, OrderSetUtils.ESIM_ACTIVE, writeListener);
        }
    }

    /**
     * 去活,取消esim激活状态
     */
    public void esimCancel(String mac) {
        this.mMac = mac;
        CURRENT_ACTION = ActionUtils.ACTION_ESIM_UNACTIVE;
        if (bleDevice == null) {
            BleDevice device = getConnectDevice(mac);
            if (device == null) {
                if (esimActiveCallback != null) {
                    esimActiveCallback.deviceNotConnect();
                }
                scanAndConnect(true, mac, "", handleListener);
            } else {
                this.bleDevice = device;
                setNotify(device, notifyListener);
            }
        } else {
            writeCharacteristic(bleDevice, OrderSetUtils.ESIM_CANCEL, writeListener);
        }
    }

    /**
     * 下载profile的url获取监听
     *
     * @param esimUrlListener
     */
    public void setEsimUrlListener(EsimDataCallback.EsimUrlListener esimUrlListener) {
        this.esimUrlListener = esimUrlListener;
    }

    /**
     * 下载profile的url post参数获取监听
     *
     * @param esimUrlPostListener
     */
    public void setEsimUrlPostListener(EsimDataCallback.EsimUrlPostListener esimUrlPostListener) {
        this.esimUrlPostListener = esimUrlPostListener;
    }

    /**
     * 激活esim的各种监听,主要为各种状态的监听
     *
     * @param esimActiveCallback
     */
    public void setEsimActiveCallback(EsimActiveCallback esimActiveCallback) {
        this.esimActiveCallback = esimActiveCallback;
    }

    public void setEsimCancelCallback(EsimCancelCallback esimCancelCallback) {
        this.esimCancelCallback = esimCancelCallback;
    }

    public void setEsimProfileDeleteCallback(EsimProfileDeleteCallback esimProfileDeleteCallback) {
        this.esimProfileDeleteCallback = esimProfileDeleteCallback;
    }
}
