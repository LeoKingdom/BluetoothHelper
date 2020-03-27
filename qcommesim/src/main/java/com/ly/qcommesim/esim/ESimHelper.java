package com.ly.qcommesim.esim;


import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothGatt;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

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
import com.ly.qcommesim.core.utils.Utils;
import com.xble.xble.core.BaseHelper;
import com.xble.xble.core.log.Logg;

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
public abstract class ESimHelper extends BaseHelper {
    private static final String TAG = "ESimHelper";
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
    private byte[] headBytes;
    private int totalFrame;
    private int currentFrame = 0;
    private byte[] jsonBodyBytes;
    private byte currentEventId;
    private String url;
    private String postData;
    private ESimHelper.EsimEnum esimEnum;
    private String defualtUrl = "1$53d22361.cpolar.cn$04386-AGYFT-A74Y8-3F815";
    private BleBaseHelper bleBaseHelper;
    private int failCount = 3;
    private int transferCount = 0;
    private int reconnCount = 5;
    private int reWriteCount = 0;
    private List<Integer> loseNums = new ArrayList<>();//丢包的序号集合
    public enum EsimEnum {
        ESIM_ACTIVE,
        ESIM_CANCEL,
        ESIM_DELETE
    }

    private void writeData(byte[] datas, boolean isOrder) {
        bleBaseHelper.writeCharacteristic(bleDevice, 20, datas, new WriteCallback() {
            @Override
            public void writeSuccess(int actionType, int current, int total, byte[] justWrite) {
                printLog(Arrays.toString(justWrite));
                reWriteCount = 0;
                if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_URL) {
                    printLog("设置url指令写入成功");
                    printLog("开始写入url");
                    handler.sendEmptyMessageDelayed(BEGIN_URL_TRANSFORM, 20);
                } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE_FIRST) {
                    transferCount++;
                    printLog("下载profile指令写入成功");
                    printLog("开始接收profile数据,第" + transferCount + "接收url");
                } else if (CURRENT_ACTION == JSON_BODY_EACH_FRAME) {
                    printLog("json body 写入成功: " + Arrays.toString(justWrite));
                }
            }

            @Override
            public void error(String err) {
                if (isOrder) {//如果写入的是指令
                    if (reWriteCount < 5 && bleBaseHelper.isConnected(mMac))
                        writeData(datas, true);
                } else {
                    writeData(datas, false);
                }
            }
        });
        reWriteCount++;
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DATA_TIMEOUT:
                    if (sumPackets != packets && loseNums.size() > 0) {//丢包了
                        for (Integer i : loseNums) {
                            printLog("丢包序号: " + i.toString());
                        }
                        //具体怎么处理还没确定

                    }
                    break;
                case DATA_CHECK: //数据校验
                    if (dataBytes == null) return;
                    byte crcByte = dataBytes[dataBytes.length - 1];//最后一个字节为crc校验,tracker返回
                    //每一帧的字节数据
                    byte[] dataByte = Arrays.copyOfRange(dataBytes, 0, dataBytes.length - 1);
                    //app计算的crc字节,用来和tracker返回的crc判断,相等则表示数据正确
                    byte crcByte1 = CRCCheckUtils.calcCrc8(dataByte);
                    //将字节数组转换成string
                    String data = TransformUtils.bytes2String(dataByte);
                    if (dataBytes != null) {
                        //重置每一帧
                        dataBytes = null;
                    }
                    printLog("数据校验,app crc: " + crcByte1 + "tracker crc: " + crcByte);
                    if (crcByte == crcByte1) { //app计算出的crc和tracker返回的crc相等,则表示数据正确
                        Message message = handler.obtainMessage();
                        if (CURRENT_ACTION == 0) { //url数据正确
                            printResult("成功接收url,第" + transferCount + "次url为: " + url);
                            url = data;
                            //以下代码为url正确的回复,app->tracker
                            byte[] responeBytes = TransformUtils.combineArrays(OrderSetUtils.ESIM_PROFILE_DOWNLOAD_URL_RESP, new byte[]{0, 0});
                            message.what = URL_RESPONSE;
                            message.obj = responeBytes;
                            handler.sendMessageDelayed(message, 200);
                        } else if (CURRENT_ACTION == 4) { //post数据正确
                            printResult("成功接收post data,第" + transferCount + "post data为: " + postData);
                            postData = data;
                            //接收完一次url和postdata则进行一次http请求,获取本次http的response code和result
                            printLog("开始第" + transferCount + "次http请求");
                            httpResponse(url, postData);
                            //以下代码为post data正确的回复,app->tracker
                            byte[] urlResBytes = OrderSetUtils.ESIM_PROFILE_DOWNLOAD_POST_RESP;
                            urlResBytes[2] = (byte) 0x02;
                            urlResBytes[6] = currentEventId;
                            byte[] urlResponseBytes = TransformUtils.combineArrays(urlResBytes, new byte[]{0, 0});
                            writeData(urlResponseBytes, true);
                            CURRENT_ACTION = 3;//进行下一次
                        }
                    } else {
                        //数据错误,有两种情况
                        // 1 丢包,因为数据错误通常是由于丢包引起的
                        // 2 crc计算出错
                        if (CURRENT_ACTION == 0) { //url数据错误
                            printResult("接收的url错误");
                        } else if (CURRENT_ACTION == 4) { //post数据错误
                            printResult("接收的post data错误");
                        }
                    }
                    break;
                case URL_RESPONSE:
                    CURRENT_ACTION = 3;//url校验回复,下一步获取请求的post param
                    printLog("开始第" + transferCount + "次接收post data");
                    if (msg.obj != null) {
                        byte[] resBytes = (byte[]) msg.obj;
                        printLog("写入指令: " + Arrays.toString(resBytes));
                        resBytes[2] = (byte) 0x02;
                        resBytes[6] = currentEventId;
                        writeData(resBytes, true);
                    }
                    break;
                case JSON_BODY_EACH_FRAME: //像tracker写入每次http请求的数据,每一帧json body数据
                    CURRENT_ACTION = JSON_BODY_EACH_FRAME;
                    currentFrame++;
                    if (currentFrame > totalFrame) return;
                    printLog("开始写入json body,第" + currentFrame + "帧");
                    //处理每帧数据
                    byte[] requestBytes = DataPacketUtils.sortEachFrame(jsonBodyBytes, currentFrame, totalFrame);
                    byte[] currentFrameBytes = DataPacketUtils.currentPacket(jsonBodyBytes, currentFrame, totalFrame);
                    byte[] dataLengts = DataPacketUtils.dataLenght(currentFrameBytes.length);
                    headBytes = new byte[]{(byte)0xFD, dataLengts[1], dataLengts[2], (byte) totalFrame, (byte) currentFrame, currentEventId};
                    //写入json body帧头
                    writeData(headBytes, true);
                    if (currentFrame == totalFrame) {//当前json body已传输完成
                        CURRENT_ACTION = 3;
                        currentFrame = 0;
                        totalFrame = 0;
                        transferCount++;
                    }
                    //写入json body数据
                    writeData(requestBytes, false);
                    break;
                case PREPARE_URL_TRANSFORM: //写入设置url的指令
                    byte[] urlHeadByte = DataPacketUtils.frameHeadBytes(OrderSetUtils.ESIM_SM_DP, defualtUrl.getBytes(), 1, 1);
                    printLog("写入设置url指令: " + Arrays.toString(urlHeadByte));
                    writeData(urlHeadByte, true);
                    break;
                case BEGIN_URL_TRANSFORM: //开始写入url
                    CURRENT_ACTION = BEGIN_URL_TRANSFORM;
                    byte[] urlByte = DataPacketUtils.splitEachFrameBytes(defualtUrl.getBytes());
                    printLog("url 原bytes: " + Arrays.toString(defualtUrl.getBytes()));
                    printLog("url 处理后的bytes: " + Arrays.toString(urlByte));
                    writeData(urlByte, false);
                    break;
                case BEGIN_ACTIVE:
                    //告诉tracker开始下载profile的指令
                    printLog("开始写入下载profile指令");
                    writeData(OrderSetUtils.ESIM_PROFILE_START, true);
                    break;
                case 1007:
                    bleBaseHelper.setNotify(bleDevice, notifyListener);
                    break;
            }
        }
    };
    private NotifyOpenCallback notifyListener = new NotifyOpenCallback() {
        @Override
        public void onNotifySuccess(BleDevice device) {

            printResult("notifySuccess-" + device.getMac() + "");
            if (device.getMac().equalsIgnoreCase(mMac)) {
                notifyBesiness();
            }
        }

        @Override
        public void onNotifyFailed(BleException e) {
            printResult("notify fail:" + e.getDescription());
            failCount--;
            if (failCount > 0) {
                handler.sendEmptyMessageDelayed(1007, 1000);
            } else {
                failCount = 0;
                printResult("notify 尝试3次开启失败");
            }
        }


        @Override
        public void onCharacteristicChanged(String mac, byte[] data) {
            printLog("CharacteristicCallback: " + TransformUtils.bytes2String(data));
            if (mac.equalsIgnoreCase(mMac)) {
                if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE_FIRST || CURRENT_ACTION == 3) {//接收url和postdata回复指令,主要是url/postdata的长度
                    if (data.length == 7 || data.length == 9) {
                        headBytes = data;
                        int length1 = TransformUtils.byte2Int(data[1]);
                        int length2 = TransformUtils.byte2Int(data[2]);
                        if (length1 != 0) {
                            tLength = (length1 * 256) + length2;
                        } else {
                            tLength = length2;
                        }
                        if (data[5] == (byte) 0x30) {//是否属于esim的module id
                            byte eventId = data[6];
                            currentEventId = eventId;
                            if (eventId == (byte) 0x51 || eventId == (byte) 0x54 || eventId == (byte) 0x57 || eventId == (byte) 0x5A) {
                                CURRENT_ACTION = 0;//请求url的回复指令,用来收包
                            } else if (eventId == (byte) 0x52 || eventId == (byte) 0x55 || eventId == (byte) 0x58 || eventId == (byte) 0x5B) {
                                CURRENT_ACTION = 4; //请求POST的回复,用来收包
                            }
                        }
                        //url/postdata总包数
                        packets = tLength % 19 == 0 ? tLength / 19 : tLength / 19 + 1;
                        handler.sendEmptyMessageDelayed(DATA_TIMEOUT, packets * 50 + 10000);//超时处理
                    }
                } else if (CURRENT_ACTION == 0 || CURRENT_ACTION == 4) {//处理url和postdata,即组包
                    sumPackets++;
                    //去除第一个字节,因为第一个字节为序号
                    data = Arrays.copyOfRange(data, 1, data.length);
                    loseNums.add(TransformUtils.byte2Int(data[0]));
                    if (dataBytes == null) {
                        //第一包
                        dataBytes = data;
                    } else {
                        //第二包开始拼接,即组包
                        dataBytes = TransformUtils.combineArrays(dataBytes, data);
                    }
                    if (sumPackets == packets) { //未丢包,收包完毕
                        //开始检查数据是否正确
                        handler.sendEmptyMessageDelayed(DATA_CHECK, packets * 50);
                        sumPackets = 0;
                        loseNums.clear();
                    }
                } else if (CURRENT_ACTION == JSON_BODY_EACH_FRAME) {//每一帧数据
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
                } else if (CURRENT_ACTION == BEGIN_URL_TRANSFORM) {//表示当前进行url设置操作
                    if (data.length > 7) {
                        if (data[data.length - 1] == 0) { //设置成功回复
                            printResult("esim url设置成功");
                            if (esimEnum == ESimHelper.EsimEnum.ESIM_ACTIVE) {
                                esimActiveFirst();
                            } else if (esimEnum == ESimHelper.EsimEnum.ESIM_CANCEL) {
                                CURRENT_ACTION = ActionUtils.ACTION_ESIM_UNACTIVE;
                                esimCancel();
                            } else if (esimEnum == ESimHelper.EsimEnum.ESIM_DELETE) {
                                CURRENT_ACTION = ActionUtils.ACTION_ESIM_PROFILE_DELETE;
                                deleteProfile();
                            }
                        } else {
                            failCount--;
                            if (failCount < 0) {
                                urlSettingFail();
                                printResult("esim url设置失败");
                            } else {
                                setSMDPUrl();
                                printResult("esim url设置失败,第" + (3 - failCount) + "重试");
                            }
                        }
                    }
                } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_PROFILE_DELETE) {//当前进行profile删除操作
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
            if (bleDevice == null) {
                printError("未扫描到设备");

            }
            deviceNotFound();
        }

        @Override
        public void onConnectSuccess(BleDevice device, BluetoothGatt gatt, int status) {
            printResult("设备连接成功");
            if (device != null) {
                bleDevice = device;
                bleBaseHelper.setNotify(device, notifyListener);
                if (reconnCount < 5) {
                    reconnCount = 5;
                }

            }
        }

        @Override
        public void onConnectFailed(BleDevice bleDevice, String description) {
            printResult("设备连接失败");
        }

        @Override
        public void onDisconnect(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt) {
            printError("设备断开连接");
            bleDevice = null;
            if (!isActiveDisConnected) {
                printLog("开始重新连接");
                if (reconnCount > 0) {
                    reconnCount--;
                    scanConnDevice();
                } else {
                    printResult("尝试重连失败");
                    deviceDisconnects(false, device);
                }
            } else {
                deviceDisconnects(true, device);
            }
        }
    };



    private void scanConnDevice() {
        BleDevice device = bleBaseHelper.getConnectDevice(mMac);
        if (device != null) {//设备已连接,直接开启notify
            bleDevice = device;
            bleBaseHelper.setNotify(device, notifyListener);
            return;
        }
        //设备已配对,直接连接
        if (bleBaseHelper.isBonded(mMac)) {
            doConnect();
            return;
        }
        bleBaseHelper.scanAndConnect( mMac, handleListener);
    }

    private void doConnect(){
        bleBaseHelper.setConnSuccessListener(this::connSuccess);
        bleBaseHelper.setConnFailListener(this::connFail);
        bleBaseHelper.setDisConnAwayListener(this::awayDisconnect);
        bleBaseHelper.setDisConnCodeListener(this::codeDisconnect);
        bleBaseHelper.setDisConnHandListener(this::handDisconnect);
        bleBaseHelper.setDisConnOtherListener(this::otherDisconnect);
        bleBaseHelper.connect(mMac);
    }

    private void connSuccess(BleDevice device){
        printResult("设备连接成功");
        if (device != null) {
            bleDevice = device;
            bleBaseHelper.setNotify(device, notifyListener);
            if (reconnCount < 5) {
                reconnCount = 5;
            }
        }
    }
    private void connFail(BleDevice device){
        printResult("设备连接失败");
    }
    private void awayDisconnect(boolean isActive,BleDevice device){

    }
    private void codeDisconnect(boolean isActive,BleDevice device){

    }
    private void handDisconnect(boolean isActive,BleDevice device){

    }
    private void otherDisconnect(boolean isActive,BleDevice device){

    }


    public ESimHelper(Application application, String mac, String url) {
        bleBaseHelper = new BleBaseHelper(application,mac);
        this.mMac = mac;
        this.mUrl = url;
        init();

    }

    /**
     * 第一步 开始操作esim
     *
     * @param type 需要进行的操作 =ESIM_ACTIVE-激活,ESIM_CANCEL-去活,ESIM_DELETE-删除profile
     */
    public final ESimHelper start(ESimHelper.EsimEnum type) {
        checkEffective();
        esimEnum = type;
        setSMDPUrl();

        return this;

    }

    private void checkEffective(){
        //传入的mac地址非法
        if (Utils.isMacAddress(mMac)) {
            printLog("esim  mac 不合法");
            macInvalidate();
            return;
        }
        if (!bleBaseHelper.isBleOpen()) {
            phoneBleDisable();
            return;
        }
        if (TextUtils.isEmpty(mUrl)) {
            printLog("esim  url 不可为空");
            urlInvalidate();
            return;
        }
    }

    /**
     * 手动断开蓝牙
     */
    private void disconnDevice() {
        if (bleDevice != null) {
            bleBaseHelper.disconnect(bleDevice);
        }
    }

    public final void stop() {
        printLog("调用stop方法");
        releaseHandler();
        if (bleDevice != null && bleBaseHelper.isConnected(mMac)) {
            printResult("关闭notify");
            bleBaseHelper.closeNotify(bleDevice);
        }
    }

    private void releaseHandler() {
        if (handler != null) {
            printResult("释放handler");
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }


    private void init() {
        //默认初始化的uuid
        String[] uuids = {"00005500-d102-11e1-9b23-00025b00a5a5", "00005501-d102-11e1-9b23-00025b00a5a5"};
        bleBaseHelper.setService_UUID(uuids[0]).setWrite_UUID(uuids[1]).setNotify_UUID(uuids[1]);
    }


    private void notifyBesiness() {
        //当前操作,开始发送profile下载指令
        if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE_FIRST) {
            currentEventId = (byte) 0x50;
            handler.sendEmptyMessageDelayed(BEGIN_ACTIVE, 100);

        } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_ACTIVE) { //真正激活esim,不确定能否用到
            writeData(OrderSetUtils.ESIM_ACTIVE, true);

        } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_UNACTIVE) {//去活esim
            writeData(OrderSetUtils.ESIM_CANCEL, true);

        } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_URL) { //开始设置url
            handler.sendEmptyMessageDelayed(PREPARE_URL_TRANSFORM, 20);

        } else if (CURRENT_ACTION == ActionUtils.ACTION_ESIM_PROFILE_DELETE) {//删除profile
            writeData(OrderSetUtils.ESIM_PROFILE_DELETE, true);
        }
    }

    /**
     * 准备和tracker建立连接或者传输数据
     *
     * @param data 已经连接的情况下,和设备交互的数据
     */
    private void prepare(byte[] data) {
        if (bleDevice == null) {
            BleDevice device = bleBaseHelper.getConnectDevice(mMac);
            if (device == null) {
                scanConnDevice();
            } else {
                this.bleDevice = device;
                //如果设备已连接,直接开启notify
                bleBaseHelper.setNotify(device, notifyListener);
            }
        } else {
            //表示此前已进行了连接和开启notify,所以直接写入
            writeData(data, true);
        }
    }

    private void deleteProfile() {
        prepare(OrderSetUtils.ESIM_PROFILE_DELETE);
    }

    /**
     * 第二步 统一先设置url再进行下一步
     */
    private void setSMDPUrl() {
        printLog("开始设置url");
        CURRENT_ACTION = ActionUtils.ACTION_ESIM_URL;
        byte[] urlHeadByte = DataPacketUtils.frameHeadBytes(OrderSetUtils.ESIM_SM_DP, defualtUrl.getBytes(), 1, 1);
        prepare(urlHeadByte);
    }

    /**
     * 第三步 开始发送下载profile指令,tracker下发
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
        //开始http数据传输
        CURRENT_ACTION = ActionUtils.ACTION_ESIM_ACTIVE_NEXT;
        if (headBytes == null) return;
        currentEventId++;
        //讲response code转为两个字节的byte
        byte[] codeBytes = TransformUtils.int2TwoBytes(statusCode);
        //string转为byte
        byte[] jsonBytes = jsonBody.getBytes();
        //code和json body拼接
        jsonBodyBytes = TransformUtils.combineArrays(codeBytes, jsonBytes);
        //计算总帧数
        totalFrame = jsonBodyBytes.length % 4096 == 0 ? jsonBodyBytes.length / 4096 : jsonBodyBytes.length / 4096 + 1;
        //发送数据
        printLog("第" + transferCount + "次json body数据为" + Arrays.toString(jsonBodyBytes));
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
        writeData(resultBytes, false);
    }

    /**
     * 激活,这一步才是最终的激活,前面四步都是为激活esim做准备
     */
    private void esimActive() {
        CURRENT_ACTION = ActionUtils.ACTION_ESIM_ACTIVE;
        writeData(OrderSetUtils.ESIM_ACTIVE, true);
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
                printResult("第" + transferCount + "次http请求成功,result: " + result);
                if (!TextUtils.isEmpty(result)) {
                    int code = 200;
                    String body = null;
                    if (body != null) {
                        printLog("开始第" + transferCount + "次json body写入");
                        esimActiveNext(code, body);
                    }
                    if (code == 204) {
                        esimActiveResult(code);
                    }
                }

            }

            @Override
            public void onFailed(Rohttp.FAILED failed, Throwable throwable) {
                printError("第" + transferCount + "次http请求失败,result: " + failed.getString());
                httpRequestFail();
            }

            @Override
            public void onFinished() {

            }
        });
    }
    /*********************打印log方法***********************/
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

    /**************暴露给外部的方法,主要处理各种状态等业务 start****************/

    /*******************必须要考虑或者肯定出现的情形 start********************/
    public abstract void macInvalidate(); //mac地址非法时调用,必须处理,

    //手机蓝牙开关未打开时调用,必须处理,tracker蓝牙未打开的状态可以根据property的bluetooth_on属性来判断
    public abstract void phoneBleDisable();

    public abstract void deviceNotFound(); //没有发现设备,必须处理,否则无法交互

    public abstract void deviceDisconnects(boolean isActiveDisConnected, BleDevice device); //设备断开连接,必须处理获取给出提示,否则用户很惊讶

    /*******************必须要考虑或者肯定出现的情形 end********************/
    //连接状态下主动关闭蓝牙开关时调用
    public void disconnByHand(boolean isActive, BleDevice device) {
    }

    ;

    //代码调用disconnect方法后调用
    public void disconnByCode(boolean isActive, BleDevice device) {
    }

    //由于tracker走远而断开连接时调用
    public void disconnByAway(boolean isActive, BleDevice device) {
    }

    //其他异常情况导致连接断开时调用
    public void disconnByOther(boolean isActive, BleDevice device) {
    }

    public abstract void urlInvalidate(); //url非法时调用,必须处理,否则无法和服务器交互

    public abstract void urlSettingFail(); //url设置失败调用,必须处理,否则无法和服务器交互

    public abstract void httpRequestFail(); //url访问失败调用,必须处理,否则无法进行下一步

    public abstract void profileDownloadFail(); //profile下载失败,必须处理,给出相应提示

    public void esimActiveState(boolean isSuccess) {
    } //esim激活结果,根据需要重写

    public void esimCancelState(boolean isSuccess) {
    } //esim去活结果,根据需要重写

    public void esimProfileDeleteState(boolean isSuccess) {
    } //esim profile删除结果,根据需要重写

    /**************暴露给外部的方法,主要处理各种状态等业务 end****************/
}
