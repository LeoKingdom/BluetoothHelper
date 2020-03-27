package com.ly.qcommesim.esim;


import android.app.Application;
import android.bluetooth.BluetoothGatt;
import android.text.TextUtils;

import com.de.esim.rohttp.Rohttp;
import com.de.esim.rohttp.helper.callback.HttpCallback;
import com.fastble.fastble.data.BleDevice;
import com.ly.qcommesim.core.callbacks.WriteCallback;
import com.ly.qcommesim.core.helper.BaseHelper;
import com.ly.qcommesim.core.helper.BleBaseHelper;
import com.ly.qcommesim.core.utils.ActionUtils;
import com.ly.qcommesim.core.utils.CRCCheckUtils;
import com.ly.qcommesim.core.utils.DataPacketUtils;
import com.ly.qcommesim.core.utils.OrderSetUtils;
import com.ly.qcommesim.core.utils.TransformUtils;
import com.ly.qcommesim.core.utils.Utils;
import com.ly.qcommesim.esim.beans.EsimData;
import com.ly.qcommesim.esim.beans.EsimDevice;
import com.ly.qcommesim.esim.beans.OrderBean;
import com.xble.xble.core.cons.FunMode;
import com.xble.xble.core.log.Logg;
import com.xble.xble.core.utils.TimerHelper;
import com.ly.qcommesim.esim.ESimHelper.EsimEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2020/3/13 19:18
 * version: 2.0
 * <p>
 * esim卡工具类,主要为外围设备和中心设备围绕esim展开的交互(蓝牙)
 */
public abstract class ESimHelper2 {
    private static final String TAG = "ESimHelper";
    private final int DATA_TIMEOUT = 1000;
    private final int DATA_CHECK = 1001;//校验数据
    private final int URL_RESPONSE = 1002;//回复url数据是否正确
    private final int JSON_BODY_EACH_FRAME = 1003;//jsonBody数据分帧
    private final int PREPARE_URL_TRANSFORM = 1004;//准备传输url
    private final int BEGIN_URL_TRANSFORM = 1005;//开始传输url
    private final int BEGIN_ACTIVE = 1006;//开始第一步下载profile
    private EsimDevice esimDevice;
    private EsimData esimData;
    private int tLength = 0;
    private byte currentEventId;
    private String url;
    private String postData;
    private EsimEnum esimEnum;
    private BleBaseHelper bleBaseHelper;
    private String defualtUrl = "1$53d22361.cpolar.cn$04386-AGYFT-A74Y8-3F815";
    private BaseHelper baseHelper;
    private List<Integer> loseNums = new ArrayList<>();//丢包的序号集合
    private int failCount = 3;
    private int transferCount = 0;
    private int reconnCount = 5;
    private int reWriteCount = 0;
    private TimerHelper outTimeTimer;
    private byte[] urlComms=new byte[]{(byte) 0x51,(byte) 0x54,(byte) 0x57,(byte) 0x5A};
    private byte[] postComms=new byte[]{(byte) 0x52,(byte) 0x55,(byte) 0x58,(byte) 0x5B};

    private void writeData(byte[] datas, boolean isOrder) {
        bleBaseHelper.writeCharacteristic(esimDevice.getBleDevice(), Utils.betweenTimes, datas, new WriteCallback() {
            @Override
            public void writeSuccess(int actionType, int current, int total, byte[] justWrite) {
                printLog("数据输入: "+Arrays.toString(justWrite));
                reWriteCount = 0;
                if (esimData.getStepAction() == ActionUtils.ACTION_ESIM_URL) {
                    printLog("设置url指令写入成功");
                    printLog("开始写入url");
                    handleMessage(BEGIN_URL_TRANSFORM, null);
                } else if (esimData.getStepAction() == ActionUtils.ACTION_ESIM_ACTIVE_FIRST) {
                    transferCount++;
                    printLog("下载profile指令写入成功");
                    printLog("开始接收profile数据,第" + transferCount + "接收url");
                } else if (esimData.getStepAction() == JSON_BODY_EACH_FRAME) {
                    printLog("json body 写入成功: " + Arrays.toString(justWrite));
                }
            }

            @Override
            public void error(String err) {
                if (isOrder) {//如果写入的是指令
                    if (reWriteCount < 5 && bleBaseHelper.isConnected(esimDevice.getMac()))
                        writeData(datas, true);
                } else {
                    writeData(datas, false);
                }
            }
        });
        reWriteCount++;
    }

    private void outTimeTimerManage(int delay) {
        if (outTimeTimer != null) {
            outTimeTimer.stop();
            outTimeTimer = null;
        }

        outTimeTimer = new TimerHelper() {
            @Override
            public void doSomething() {
                //Todo 处理丢包逻辑,因为数据是被动接收的,所以用超时来处理
                handleLost();
            }
        }.startDelay(delay);
    }

    private void handleLost() {
        //在一定时间(根据接收的包数来计算时间)内,如果接收的包数不等于总包数表示已丢包
        if (esimData.getSumPackets()!=esimData.getPackets()){

        }
    }


    public ESimHelper2(Application application, String mac, String url) {
        bleBaseHelper = new BleBaseHelper(application,mac);
        esimDevice = new EsimDevice();
        esimDevice.setMac(mac);
        esimDevice.setUrl(url);
        baseHelper = new BaseHelper(application,mac);
    }

    /**
     * 开始操作esim
     *
     * @param type 需要进行的操作 =ESIM_ACTIVE-激活,ESIM_CANCEL-去活,ESIM_DELETE-删除profile
     */
    public final void start(ESimHelper.EsimEnum type) {
        if (TextUtils.isEmpty(esimDevice.getMac())) {
            macInvalidate();
            return;
        }
        if (!bleBaseHelper.isBleOpen()) {
            phoneBleDisable();
            return;
        }
        if (TextUtils.isEmpty(esimDevice.getUrl())) {
            urlInvalidate();
            return;
        }
        esimEnum = type;

        setSMDPUrl();
    }

    public final void stop() {

        if (esimDevice.getBleDevice() != null && bleBaseHelper.isConnected(esimDevice.getMac())) {
            bleBaseHelper.closeNotify(esimDevice.getBleDevice());
        }
    }


    private void notifyBesiness() {
        if (esimData.getStepAction() == ActionUtils.ACTION_ESIM_ACTIVE_FIRST) {
            currentEventId = (byte) 0x50;
            handleMessage(BEGIN_ACTIVE, OrderSetUtils.ESIM_PROFILE_START);
        } else if (esimData.getStepAction() == ActionUtils.ACTION_ESIM_ACTIVE) {
            writeData(OrderSetUtils.ESIM_ACTIVE, true);
        } else if (esimData.getStepAction() == ActionUtils.ACTION_ESIM_UNACTIVE) {
            writeData(OrderSetUtils.ESIM_CANCEL, true);
        } else if (esimData.getStepAction() == ActionUtils.ACTION_ESIM_URL) {
            handleMessage(PREPARE_URL_TRANSFORM, null);
        } else if (esimData.getStepAction() == ActionUtils.ACTION_ESIM_PROFILE_DELETE) {
            writeData(OrderSetUtils.ESIM_PROFILE_DELETE, true);
        }
    }

    /**
     * 准备和tracker建立连接或者传输数据
     *
     * @param data 已经连接的情况下,和设备交互的数据
     */
    private void prepare(byte[] data) {
        BleDevice device = bleBaseHelper.getConnectDevice(esimDevice.getMac());
        if (device != null) {
            notifyChange();
            baseHelper.notifys(device, FunMode.ESIM);
        } else {
            connectChange();
            baseHelper.connect(esimDevice.getMac());
        }
    }

    private void connectChange() {
        baseHelper.setOnStartConneectListener(this::connStart);
        baseHelper.setOnConnectFailedListener(this::connFail);
        baseHelper.setOnConnectSuccessListener(this::connSuccess);
        baseHelper.setOnDisConnectedListener(this::disconnect);
    }

    private void connStart() {
    }

    private void connFail(BleDevice device) {
        printError(device.getMac() + "连接失败");
    }

    private void connSuccess(BleDevice device, BluetoothGatt gatt) {
        printResult(device.getMac() + "连接成功");
        baseHelper.notifys(device, FunMode.ESIM);
    }

    private void disconnect(boolean isActive, BleDevice device, BluetoothGatt gatt) {

    }

    private void notifyChange() {
        baseHelper.setOnNotifySuccessListener(this::notifySuccess);
        baseHelper.setOnNotifyChangeByBitListener(this::notifyDataChange);
        baseHelper.setOnNotifyFailedListener(this::notifyFail);
    }

    /**
     * 通知开启失败
     */
    private void notifyFail() {
        printResult("notify open fail");
    }

    /**
     * 通知开启成功
     */
    private void notifySuccess() {
        printResult("notify open success");
        notifyBesiness();
    }

    /**
     * tracker和app进行交互的主要方法
     *
     * @param mac
     * @param data tracker返回的数据
     */
    private void notifyDataChange(String mac, byte[] data) {
        printLog("tracker 返回的数据: " + Arrays.toString(data));
        OrderBean orderBean = new OrderBean(data);
        switch (esimData.getStepAction()) {
            case 3:
            case ActionUtils.ACTION_ESIM_ACTIVE_FIRST: //帧头包
                if (data.length == 7 || data.length == 9) {
                    esimData.setOrderBean(orderBean);
                    esimData.setHeadBytes(data);
                    calcPackets();
                    outTimeTimerManage(esimData.getPackets() * 50 + 3000);
                }
                break;
            case 0:
            case 4: //3. 激活第三步,接收url和post数据包
                int sp = esimData.getSumPackets();
                sp++;
                esimData.setSumPackets(sp);
                //将原数据的第一个字节存入集合,序号
                loseNums.add(TransformUtils.byte2Int(data[0]));
                //截取url和post数据包(第一个字节为序号)
                data = Arrays.copyOfRange(data, 1, data.length);
                //组包开始
                byte[] dataBytes = esimData.getDataBytes();
                if (dataBytes == null) {
                    dataBytes = data;
                } else {
                    dataBytes = TransformUtils.combineArrays(esimData.getDataBytes(), data);
                }
                //组包结束
                esimData.setDataBytes(dataBytes);
                if (esimData.getSumPackets() == esimData.getPackets()) { //未丢包
                    handleMessage(DATA_CHECK, esimData.getDataBytes());
                    esimData.setSumPackets(0);
                    loseNums.clear();
                }
                break;
            case JSON_BODY_EACH_FRAME:
                handleMessage(JSON_BODY_EACH_FRAME, null);
                break;
            case ActionUtils.ACTION_ESIM_ACTIVE_FORTH:
                if (data.length > 7) {
                    boolean moduleId = data[5] == (byte) 0x30;
                    boolean eventId = data[6] == (byte) 0x5D;
                    if (moduleId && eventId) {
                        if (data[data.length - 1] != 0) {

                            //profile下载失败
                            profileDownloadFail();
                        } else {
                            esimActive();//下载成功,可以开始激活
                        }
                    }
                }
                break;
            case ActionUtils.ACTION_ESIM_ACTIVE:
                if (orderBean.esimAction() == EsimEnum.ESIM_ACTIVE) {
                    //激活结果
                    esimActiveState(data[data.length - 1] != 0);
                }
                break;
            case ActionUtils.ACTION_ESIM_UNACTIVE:

                if (orderBean.esimAction() == EsimEnum.ESIM_CANCEL) {
                    //去活结果
                    esimCancelState(data[data.length - 1] != 0);
                }
                break;
            case BEGIN_URL_TRANSFORM:
                if (data[data.length - 1] == 0) { //设置成功回复
                    if (esimEnum == EsimEnum.ESIM_ACTIVE) {
                        esimActiveFirst();
                    } else if (esimEnum == EsimEnum.ESIM_CANCEL) {
                        esimData.setStepAction(ActionUtils.ACTION_ESIM_UNACTIVE);
                        esimCancel();
                    } else if (esimEnum == EsimEnum.ESIM_DELETE) {
                        esimData.setStepAction(ActionUtils.ACTION_ESIM_PROFILE_DELETE);
                        deleteProfile();
                    }
                }
                break;
            case ActionUtils.ACTION_ESIM_PROFILE_DELETE:
                if (orderBean.esimAction() == EsimEnum.ESIM_DELETE) {
                    //删除成功
                    esimProfileDeleteState(data[data.length - 1] != 0);
                }
                break;
        }
    }

    private void handleMessage(int what, byte[] bytes) {
        switch (what) {
            case DATA_TIMEOUT: //超时,做丢包逻辑
                if (esimData.getSumPackets() != esimData.getPackets() && loseNums.size() > 0) {//丢包了
                    for (Integer i : loseNums) {
                        printLog("丢包序号: " + i.toString());
                    }
                    //具体怎么处理还没确定

                }
                break;
            case DATA_CHECK: //4.激活第四步 数据校验
                if (bytes == null) return;
                byte crcByte = bytes[bytes.length - 1];//最后一个字节为crc校验,tracker返回
                //每一帧的有效字节数据
                byte[] dataByte = Arrays.copyOfRange(bytes, 0, bytes.length - 1);
                //app计算的crc字节,用来和tracker返回的crc判断,相等则表示数据正确
                byte crcByte1 = CRCCheckUtils.calcCrc8(dataByte);
                //将字节数组转换成string
                String data = TransformUtils.bytes2String(dataByte);
                //重置每一帧数据

                esimData.setDataBytes(null);
                printLog("数据校验,app crc: " + crcByte1 + "tracker crc: " + crcByte);
                if (crcByte == crcByte1) { //app计算出的crc和tracker返回的crc相等,则表示数据正确

                    if (esimData.getStepAction() == 0) { //url数据正确
                        printResult("成功接收url,第" + transferCount + "次url为: " + url);
                        url = data;
                        //以下代码为url正确的回复,app->tracker
                        byte[] responeBytes = TransformUtils.combineArrays(OrderSetUtils.ESIM_PROFILE_DOWNLOAD_URL_RESP, new byte[]{0, 0});
                        handleMessage(URL_RESPONSE, responeBytes);
                    } else if (esimData.getStepAction() == 4) { //post数据正确
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
                        //进行下一次
                        esimData.setStepAction(3);
                    }
                } else {
                    //数据错误,有两种情况
                    // 1 丢包,因为数据错误通常是由于丢包引起的
                    // 2 crc计算出错
                    if (esimData.getStepAction() == 0) { //url数据错误
                        printResult("接收的url错误");
                    } else if (esimData.getStepAction() == 4) { //post数据错误
                        printResult("接收的post data错误");
                    }
                }
                break;
            case URL_RESPONSE:
                //url校验回复,下一步获取请求的post param
                esimData.setStepAction(3);
                if (bytes != null) {
                    bytes[2] = (byte) 0x02;
                    bytes[6] = currentEventId;
                    writeData(bytes, true);
                }
                break;
            case JSON_BODY_EACH_FRAME:
                esimData.setStepAction(JSON_BODY_EACH_FRAME);
                int currentFrame = esimData.getCurrentFrame();
                int totalFrame = esimData.getTotalFrame();
                currentFrame++;
                esimData.setCurrentFrame(currentFrame);
                if (currentFrame > totalFrame) return;
                printLog("开始写入json body,第" + currentFrame + "帧");
                //处理每帧数据
                byte[] requestBytes = DataPacketUtils.sortEachFrame(bytes, currentFrame, totalFrame);
                byte[] currentFrameBytes = DataPacketUtils.currentPacket(bytes, currentFrame, totalFrame);
                byte[] dataLengts = DataPacketUtils.dataLenght(currentFrameBytes.length);
                byte[] headBytes = new byte[]{-3, dataLengts[1], dataLengts[2], (byte) totalFrame, (byte) currentFrame, currentEventId};
                esimData.setHeadBytes(headBytes);
                //写入json body帧头
                writeData(headBytes, true);
                if (currentFrame == totalFrame) {
                    esimData.setStepAction(3);
                    esimData.setCurrentFrame(0);
                    esimData.setTotalFrame(0);
                }
                writeData(requestBytes, false);
                break;
            case PREPARE_URL_TRANSFORM: //写入设置url的指令
                byte[] urlHeadByte = DataPacketUtils.frameHeadBytes(OrderSetUtils.ESIM_SM_DP, defualtUrl.getBytes(), 1, 1);
                writeData(urlHeadByte, true);
                break;
            case BEGIN_URL_TRANSFORM: //开始写入url
                esimData.setStepAction(BEGIN_URL_TRANSFORM);
                byte[] urlByte = DataPacketUtils.splitEachFrameBytes(defualtUrl.getBytes());
                writeData(urlByte, false);
                break;
            case BEGIN_ACTIVE://1. 激活第一步,发送profile下载指令,无回复,直接返回接收第一次url指令
                writeData(bytes, true);
                break;
        }
    }

    /**
     * 2. 激活第二步
     * 此方法为解析 计算url和post回复指令(即反向帧头),主要获取url和post的字节长度
     *
     */
    private void calcPackets() {
        OrderBean order = esimData.getOrderBean();
        int tt = 0;
        if (order.getLongL() != 0) {
            tt = (order.getLongL() * 256) + order.getShortL();
        } else {
            tt = order.getShortL();
        }
        if (order.getModule() == (byte) 0x30) {
            byte eventId = order.getEvent();
            currentEventId = eventId;
            if (Arrays.binarySearch(urlComms,eventId)==0){
                //请求url的回复指令
                esimData.setStepAction(0);
            }else if (Arrays.binarySearch(postComms,eventId)==0){
                esimData.setStepAction(4); //请求POST的回复
            }
        }
        esimData.setPackets(tt);
    }

    private void deleteProfile() {
        prepare(OrderSetUtils.ESIM_PROFILE_DELETE);
    }


    private void setSMDPUrl() {
        esimData.setStepAction(ActionUtils.ACTION_ESIM_URL);
        byte[] urlHeadByte = DataPacketUtils.frameHeadBytes(OrderSetUtils.ESIM_SM_DP, defualtUrl.getBytes(), 1, 1);
        prepare(urlHeadByte);
    }

    /**
     *激活第一步,发送下载profile指令
     */
    private void esimActiveFirst() {
        esimData.setStepAction(ActionUtils.ACTION_ESIM_ACTIVE_FIRST);
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
        esimData.setStepAction(ActionUtils.ACTION_ESIM_ACTIVE_NEXT);
        if (esimData.getHeadBytes() == null) return;
        currentEventId++;
        byte[] codeBytes = TransformUtils.int2TwoBytes(statusCode);
        byte[] jsonBytes = jsonBody.getBytes();
        byte[] jsonBodyBytes = TransformUtils.combineArrays(codeBytes, jsonBytes);
        int totalFrame = jsonBodyBytes.length % 4096 == 0 ? jsonBodyBytes.length / 4096 : jsonBodyBytes.length / 4096 + 1;
        esimData.setTotalFrame(totalFrame);
        handleMessage(JSON_BODY_EACH_FRAME, jsonBodyBytes);

    }

    /**
     * 激活esim第四步,profile准备工作完成,这时候用户就可以激活esim了
     *
     * @param statusCode 服务器状态码
     */
    private void esimActiveResult(int statusCode) {
        esimData.setStepAction(ActionUtils.ACTION_ESIM_ACTIVE_FORTH);
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
        esimData.setStepAction(ActionUtils.ACTION_ESIM_ACTIVE);
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

            }

            @Override
            public void onFinished() {

            }
        });
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
