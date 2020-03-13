package com.ly.qcommesim.esim;


import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothGatt;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.de.esim.rohttp.Rohttp;
import com.de.esim.rohttp.helper.callback.HttpCallback;
import com.fastble.fastble.data.BleDevice;
import com.fastble.fastble.exception.BleException;
import com.ly.qcommesim.core.callbacks.NotifyOpenCallback;
import com.ly.qcommesim.core.callbacks.ScanConnectCallback;
import com.ly.qcommesim.core.callbacks.WriteCallback;
import com.ly.qcommesim.core.helper.BleBaseHelper;
import com.ly.qcommesim.core.utils.ActionUtils;
import com.ly.qcommesim.core.utils.CRCCheckUtils;
import com.ly.qcommesim.core.utils.DataPacketUtils;
import com.ly.qcommesim.core.utils.OrderSetUtils;
import com.ly.qcommesim.core.utils.TransformUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/15 11:11
 * version: 1.0
 * <p>
 * esim卡工具类,主要为外围设备和中心设备围绕esim展开的交互(蓝牙)
 */
public abstract class ESimActiveHelper extends BleBaseHelper {
    private final int DATA_TIMEOUT = 1000;
    private final int DATA_CHECK = 1001;//校验数据
    private final int URL_RESPONSE = 1002;//回复url数据是否正确
    private final int JSON_BODY_EACH_FRAME = 1003;//jsonBody数据分帧
    private final int PREPARE_URL_TRANSFORM = 1004;//准备传输url
    private final int BEGIN_URL_TRANSFORM = 1005;//开始传输url
    private final int BEGIN_ACTIVE = 1006;//开始第一步下载profile
    private String mMac;
    private String mUrl;
    private BleDevice bleDevice;
    private int tLength = 0;
    private int CURRENT_ACTION = -1; //激活第一步,即请求获取url
    private byte[] dataBytes;
    private int packets; //每次回复需要接收的包数
    private int sumPackets = 0;//已收包数
    private List<Integer> sumPacketList = new ArrayList<>();
    private byte[] headBytes;
    private int totalFrame;
    private int currentFrame = 0;
    private byte[] jsonBodyBytes;
    private byte currentEventId;
    private String url;
    private String postData;
    private EsimEnum esimEnum;
    private String defualtUrl = "1$53d22361.cpolar.cn$04386-AGYFT-A74Y8-3F815";

    public enum EsimEnum {
        ESIM_ACTIVE,
        ESIM_CANCEL,
        ESIM_DELETE
    }


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
                            url = data;
                            byte[] responeBytes = TransformUtils.combineArrays(OrderSetUtils.ESIM_PROFILE_DOWNLOAD_URL_RESP, new byte[]{0, 0});
                            message.what = URL_RESPONSE;
                            message.obj = responeBytes;
                            handler.sendMessageDelayed(message, 200);
                        } else if (CURRENT_ACTION == 4) { //post数据正确
                            postData = data;
                            httpResponse(url, postData);
//                            esimUrlPostListener.urlPostSuccess(CURRENT_STEP, data);
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
                    CURRENT_ACTION = 3;//url校验回复,下一步获取请求的post param
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
                case PREPARE_URL_TRANSFORM: //写入设置url的指令
                    byte[] urlHeadByte = DataPacketUtils.frameHeadBytes(OrderSetUtils.ESIM_SM_DP, defualtUrl.getBytes(), 1, 1);
                    writeCharacteristic(bleDevice, urlHeadByte, writeListener);
                    break;
                case BEGIN_URL_TRANSFORM: //开始写入url
                    CURRENT_ACTION = BEGIN_URL_TRANSFORM;
                    byte[] urlByte = DataPacketUtils.splitEachFrameBytes(defualtUrl.getBytes());
                    writeCharacteristic(bleDevice, 20, urlByte, writeListener);
                    break;
                case BEGIN_ACTIVE:
                    writeCharacteristic(bleDevice, OrderSetUtils.ESIM_PROFILE_START, writeListener);
                    break;
            }
        }
    };
    private NotifyOpenCallback notifyListener = new NotifyOpenCallback() {
        @Override
        public void onNotifySuccess(BleDevice device) {
            Log.e("notifySuccess---", device + "");
            if (device.getMac().equalsIgnoreCase(mMac)) {
                notifyBesiness();
            }
        }

        @Override
        public void onNotifyFailed(BleException e) {
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
                            if (code == 0) { //下载成功,可以开始激活
                                esimActive();
                            } else {
                                profileDownloadFail();
                            }
                        }
                    }
                } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE) {//激活结果
                    if (data.length > 7) {
                        boolean moduleId = data[5] == (byte) 0x30;
                        boolean eventId = data[6] == (byte) 0x08;
                        if (moduleId && eventId) {
                            int code = 0;//激活成功
                            if (data[data.length - 1] != 0) {
                                //激活失败
                                code = -1;
                            }
                            esimActiveState(code == 0);
                        }
                    }
                } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_UNACTIVE) {//去活结果
//                    esimActiveCallback.notifyCallback(data);
                    if (data.length > 7) {
                        boolean moduleId = data[5] == (byte) 0x30;
                        boolean eventId = data[6] == (byte) 0x09;
                        if (moduleId && eventId) {
                            int code = 0;//去活成功
                            if (data[data.length - 1] != 0) {
                                //去活失败
                                code = -1;
                            }
                            esimCancelState(code == 0);
                        }
                    }
                } else if (CURRENT_ACTION == BEGIN_URL_TRANSFORM) {
                    if (data.length > 7) {
                        if (data[data.length - 1] == 0) { //设置成功回复
                            if (esimEnum== EsimEnum.ESIM_ACTIVE) {
                                esimActiveFirst();
                            } else if (esimEnum == EsimEnum.ESIM_CANCEL) {
                                CURRENT_ACTION = ActionUtils.ACTION_ESIM_UNACTIVE;
                                esimCancel();
                            } else if (esimEnum == EsimEnum.ESIM_DELETE) {
                                CURRENT_ACTION = ActionUtils.ACTION_ESIM_PROFILE_DELETE;
                                deleteProfile();
                            }
                        }
                    }
                } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_PROFILE_DELETE) {
                    if (data.length > 7) {
                        boolean moduleId = data[5] == (byte) 0x30;
                        boolean eventId = data[6] == (byte) 0x0A;
                        if (moduleId && eventId) {
                            int code = 0;//删除成功
                            if (data[data.length - 1] != 0) {
                                //删除失败
                                code = -1;
                            }
                            esimProfileDeleteState(code == 0);
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
                deviceNotFound();
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
            deviceDisconnects(isActiveDisConnected, device);
        }
    };

    public ESimActiveHelper(Application application, String mac, String url) {
        super(application);
        this.mMac = mac;
        this.mUrl = url;
    }

    /**
     * 开始操作esim
     *
     * @param type 需要进行的操作 =ESIM_ACTIVE-激活,ESIM_CANCEL-去活,ESIM_DELETE-删除profile
     */
    public final void start(EsimEnum type) {
        if (TextUtils.isEmpty(mMac)) {
            macInvalidate();
            return;
        }
        if (!isBleOpen()) {
            phoneBleDisable();
            return;
        }
        if (TextUtils.isEmpty(mUrl)) {
            urlInvalidate();
            return;
        }
        esimEnum=type;

        setSMDPUrl();
    }

    public final void stop() {
        releaseHandler();
        if (bleDevice != null && isConnected(mMac)) {
            closeNotify(bleDevice);
        }
    }

    private void releaseHandler() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    @Override
    public final void init() {
        //默认初始化的uuid
        String[] uuids = {"00005500-d102-11e1-9b23-00025b00a5a5", "00005501-d102-11e1-9b23-00025b00a5a5"};
        setService_UUID(uuids[0]).setWrite_UUID(uuids[1]).setNotify_UUID(uuids[1]);
    }


    private void notifyBesiness() {
        if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE_FIRST) {
            currentEventId = (byte) 0x50;
            handler.sendEmptyMessageDelayed(BEGIN_ACTIVE, 10000);
        } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE) {
            writeCharacteristic(bleDevice, OrderSetUtils.ESIM_ACTIVE, writeListener);
        } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_UNACTIVE) {
            writeCharacteristic(bleDevice, OrderSetUtils.ESIM_CANCEL, writeListener);
        } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_URL) {
            handler.sendEmptyMessageDelayed(PREPARE_URL_TRANSFORM, 20);
        } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_PROFILE_DELETE) {
            writeCharacteristic(bleDevice, OrderSetUtils.ESIM_PROFILE_DELETE, writeListener);
        }
    }

    /**
     * 准备和tracker建立连接或者传输数据
     *
     * @param data 已经连接的情况下,和设备交互的数据
     */
    private void prepare(byte[] data) {
        if (bleDevice == null) {
            BleDevice device = getConnectDevice(mMac);
            if (device == null) {
                scanAndConnect(true, mMac, "", handleListener);
            } else {
                this.bleDevice = device;
                setNotify(device, notifyListener);
            }
        } else {
            writeCharacteristic(bleDevice, data, writeListener);
        }
    }

    private void deleteProfile() {
        prepare(OrderSetUtils.ESIM_PROFILE_DELETE);
    }


    private void setSMDPUrl() {
        CURRENT_ACTION = ActionUtils.ACTION_ESIM_URL;
        byte[] urlHeadByte = DataPacketUtils.frameHeadBytes(OrderSetUtils.ESIM_SM_DP, defualtUrl.getBytes(), 1, 1);
        prepare(urlHeadByte);
    }

    /**
     *
     */
    private void esimActiveFirst() {
        CURRENT_ACTION = ActionUtils.ACTION_ESIM_ACTIVE_FIRST;
        notifyBesiness();
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
    private void esimActiveNext(int statusCode, String jsonBody) {
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
    private void esimActiveResult(int statusCode) {
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
    private void esimActive() {
        CURRENT_ACTION = ActionUtils.ACTION_ESIM_ACTIVE;
        writeCharacteristic(bleDevice, OrderSetUtils.ESIM_ACTIVE, writeListener);
    }

    /**
     * 去活,取消esim激活状态
     */
    private void esimCancel() {
        prepare(OrderSetUtils.ESIM_CANCEL);
    }

    private void httpResponse(String url, String param) {
        new Rohttp().roBaseUrl(url).roParam(param).roPost(new HttpCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.e("result----", result + "");
                if (!TextUtils.isEmpty(result)) {
                    int code = 200;
                    String body = null;
                    if (body != null) {
                        esimActiveNext(code, body);
                    }
                    if (code == 204) {
                        esimActiveResult(code);
                    }
                }

            }

            @Override
            public void onFailed(Rohttp.FAILED failed, Throwable throwable) {

            }

            @Override
            public void onFinished() {

            }
        });
    }

    /**************暴露给外部的方法,主要处理各种状态等业务 start****************/

    /*******************必须要考虑或者肯定出现的情形 start********************/
    public abstract void macInvalidate(); //mac地址非法时调用,必须处理,

    //手机蓝牙开关未打开时调用,必须处理,tracker蓝牙未打开的状态可以根据property的bluetooth_on属性来判断
    public abstract void phoneBleDisable();

    public abstract void deviceNotFound(); //没有发现设备,必须处理,否则无法交互

    public abstract void deviceDisconnects(boolean isActiveDisConnected, BleDevice device); //设备断开连接,必须处理获取给出提示,否则用户很惊讶

    /*******************必须要考虑或者肯定出现的情形 end********************/

    public abstract void urlInvalidate(); //url非法时调用,必须处理,否则无法和服务器交互

    public abstract void profileDownloadFail(); //profile下载失败,必须处理,给出相应提示

    public void esimActiveState(boolean isSuccess) {
    } //esim激活结果,根据需要重写

    public void esimCancelState(boolean isSuccess) {
    } //esim去活结果,根据需要重写

    public void esimProfileDeleteState(boolean isSuccess) {
    } //esim profile删除结果,根据需要重写

    /**************暴露给外部的方法,主要处理各种状态等业务 end****************/
}
