package com.ly.qcommesim.esim.helper;


import android.app.Application;
import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.text.TextUtils;

import com.de.esim.rohttp.Rohttp;
import com.fastble.fastble.data.BleDevice;
import com.ly.qcommesim.core.beans.OrderBean;
import com.ly.qcommesim.core.helper.BaseHelper;
import com.ly.qcommesim.core.utils.ActionUtils;
import com.ly.qcommesim.core.utils.CRCCheckUtils;
import com.ly.qcommesim.core.utils.DataPacketUtils;
import com.ly.qcommesim.core.utils.OrderSetUtils;
import com.ly.qcommesim.core.utils.TransformUtils;
import com.ly.qcommesim.esim.bean.EsimData;
import com.ly.qcommesim.esim.bean.EsimDevice;
import com.ly.qcommesim.esim.enums.EsimEnum;
import com.ly.qcommesim.esim.utils.HelperDataUtils;
import com.xble.xble.core.CoreManager;
import com.xble.xble.core.cons.FunMode;
import com.xble.xble.core.log.Logg;
import com.xble.xble.core.utils.TimerHelper;

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
public abstract class ESimHelper {
    private static final String TAG = "ESimHelper";
    private EsimDevice esimDevice;
    private EsimData esimData;
    private EsimEnum esimEnum;
    private BaseHelper baseHelper;
    private List<Integer> loseNums = new ArrayList<>();//丢包的序号集合
    private int transferCount = 1;
    private int reWriteCount = 0;
    private TimerHelper outTimeTimer;
    private byte[] urlComms = new byte[]{(byte) 0x51, (byte) 0x54, (byte) 0x57, (byte) 0x5A, (byte) 0xA1};
    private byte[] postComms = new byte[]{(byte) 0x52, (byte) 0x55, (byte) 0x58, (byte) 0x5B, (byte) 0xA2};
    private OrderBean orderBean = new OrderBean();
    private HttpHelper httpHelper;


    public ESimHelper(Application application, String mac, String url) {
        esimDevice = new EsimDevice();
        esimData = new EsimData();
        esimDevice.setMac(mac);
        esimDevice.setUrl(url);
        baseHelper = new BaseHelper(application, mac);
        httpHelper = new HttpHelper(application);
    }

    /**
     * 开始操作esim
     *
     * @param type 需要进行的操作 =ESIM_ACTIVE-激活,ESIM_CANCEL-去活,ESIM_DELETE-删除profile
     */
    public final void start(EsimEnum type) {
        if (TextUtils.isEmpty(esimDevice.getMac())) {
            macInvalidate();
            return;
        }
        if (!baseHelper.isBLEEnable()) {
            bleIsUnable();
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

        if (esimDevice.getBleDevice() != null && baseHelper.isTargetBLEConnect(esimDevice.getMac())) {
            stopOutTimer();
        }
    }


    private void notifyBesiness() {
        byte[] datas = null;
        switch (esimData.getStepAction()) {
            case ActionUtils.ACTION_ESIM_ACTIVE_FIRST:
                orderBean.setEvent((byte) 0x50);
                datas = OrderSetUtils.ESIM_PROFILE_START;
                break;
            case ActionUtils.ACTION_ESIM_ACTIVE:
                datas = OrderSetUtils.ESIM_ACTIVE;
                break;
            case ActionUtils.ACTION_ESIM_UNACTIVE:
                datas = OrderSetUtils.ESIM_CANCEL;
                break;
            case ActionUtils.ACTION_ESIM_URL:
                datas = DataPacketUtils.frameHeadBytes(OrderSetUtils.ESIM_SM_DP, esimDevice.getUrl().getBytes(), 1, 1);
                orderBean.setEvent((byte) 0x0D);
                break;
            case ActionUtils.ACTION_ESIM_PROFILE_DELETE:
                datas = OrderSetUtils.ESIM_PROFILE_DELETE;
                break;
        }
        writeData(datas, true);
    }

    /**
     * 准备和tracker建立连接或者传输数据
     */
    private void prepare() {
        BleDevice device = baseHelper.isTargetBLEExist(esimDevice.getMac());
        printLog("device: " + device);
        if (device != null && baseHelper.isBLEConnected(esimDevice.getBleDevice())) {
            notifyChange();
        } else {
            doHandle();
        }
    }

    /**
     * 是否为特殊机型:是则先扫描再连接,否则直接连接
     */
    private void doHandle(){
        if (CoreManager.specialModel.contains(Build.MODEL)){
            scanDevice();
        }else {
            connectChange();
        }
    }

    /**
     * 扫描设备
     */
    private void scanDevice() {
        List<String> macs = new ArrayList<>();
        macs.add(esimDevice.getMac());
        baseHelper.setOnScanFinishListener(this::scanFinish);
        baseHelper.scan(macs);
    }

    private void scanFinish(List<BleDevice> devices) {
        if (devices == null || devices.size() == 0) {//没找到设备
            deviceNotFound();
        } else {
            connectChange();
        }
    }

    /**
     * 连接设备
     */
    private void connectChange() {
        baseHelper.setOnStartConneectListener(this::connStart);
        baseHelper.setOnConnectFailedListener(this::connFail);
        baseHelper.setOnConnectSuccessListener(this::connSuccess);
        baseHelper.setOnDisConnectedListener(this::disconnect);
        baseHelper.connect(esimDevice.getMac());
    }

    private void connStart() {
    }

    private void connFail(BleDevice device) {
        printError(device.getMac() + "连接失败");
    }

    /**
     * 连接成功,开启通知
     *
     * @param device
     * @param gatt
     */
    private void connSuccess(BleDevice device, BluetoothGatt gatt) {
        printResult(device.getMac() + "连接成功");
        //        timer();
        esimDevice.setBleDevice(device);
        notifyChange();
    }

    private void disconnect(boolean isActive, BleDevice device, BluetoothGatt gatt) {

    }

    /**
     * 开启通知
     */
    private void notifyChange() {
        baseHelper.setOnNotifySuccessListener(this::notifySuccess);
        baseHelper.setOnNotifyChangeByBitListener(this::notifyDataChange);
        baseHelper.setOnNotifyFailedListener(this::notifyFail);
        baseHelper.notifys(esimDevice.getBleDevice(), FunMode.ESIM);
    }

    /**
     * 通知开启失败
     */
    private void notifyFail() {
        printResult("notify open fail");

    }

    /**
     * 通知开启成功,处理当前进行的业务
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
        printLog("notify data back: " + Arrays.toString(data));
        OrderBean orderBean = null;
        switch (esimData.getStepAction()) {
            case 3:
            case ActionUtils.ACTION_ESIM_ACTIVE_FIRST:
                orderBean = new OrderBean(data);
                esimData.setOrderBean(orderBean);
                esimData.setHeadBytes(data);
                calcPackets();
                outTimeTimerManage(esimData.getPackets() * 100);//开启超时,超时时间按包数来计算
                break;
            case 0:
            case 4:
                int sp = esimData.getSumPackets();
                sp++;
                esimData.setSumPackets(sp);
                //将原数据的第一个字节存入集合,序号
                loseNums.add(TransformUtils.byte2Int(data[0]));
                esimData.setDataBytes(HelperDataUtils.combinePacket(data, esimData.getDataBytes()));
                if (esimData.getSumPackets() == esimData.getPackets()) { //未丢包
                    handleMessage(ActionUtils.DATA_CHECK, esimData.getDataBytes());
                    esimData.setSumPackets(0);
                    loseNums.clear();
                }
                break;
            case ActionUtils.JSON_BODY_EACH_FRAME:
                handleMessage(ActionUtils.JSON_BODY_EACH_FRAME, esimData.getDataBytes());
                break;
            case ActionUtils.ACTION_ESIM_ACTIVE_FORTH:
                if (data[data.length - 1] != 0) {
                    //profile下载失败
                    profileDownloadFail();
                } else {
                    esimActive();//下载成功,可以开始激活
                }
                break;
            case ActionUtils.ACTION_ESIM_ACTIVE:
                //激活结果
                esimActiveState(data[data.length - 1] != 0);
                break;
            case ActionUtils.ACTION_ESIM_UNACTIVE:

                //去活结果
                esimCancelState(data[data.length - 1] != 0);
                break;
            case ActionUtils.BEGIN_URL_TRANSFORM: //4. 第四步,设置url回复,0为成功
                if (data.length == 8 && data[data.length - 1] == 0) { //设置成功回复
                    if (esimEnum == EsimEnum.ESIM_ACTIVE) { //5. 第五步,激活,根据用户操作进行
                        esimData.setStepAction(ActionUtils.ACTION_ESIM_ACTIVE_FIRST);
                        esimActiveFirst();
                    }
                } else {
                    urlSettingFail();
                }
                break;
            case ActionUtils.ACTION_ESIM_PROFILE_DELETE:
                //删除成功
                esimProfileDeleteState(data[data.length - 1] != 0);
                break;
        }
    }

    private void outTimeTimerManage(int delay) {
        stopOutTimer();
        outTimeTimer = new TimerHelper() {
            @Override
            public void doSomething() {
                receieveOuttime();
            }
        }.startDelay(delay);
    }

    private void stopOutTimer() {
        if (outTimeTimer != null) {
            outTimeTimer.stop();
            outTimeTimer = null;
        }
    }

    private boolean isContains(byte[] checks, byte dest) {
        for (byte d : checks) {
            if (dest == d) {
                return true;
            }
        }
        return false;
    }

    private void writeData(byte[] datas, boolean isOrder) {
        baseHelper.write(esimDevice.getBleDevice(), FunMode.ESIM, datas);
        baseHelper.setOnWriteFailedListener((e -> {
            if (isOrder) {//如果写入的是指令
                if (reWriteCount < 5 && baseHelper.isBLEConnected(esimDevice.getBleDevice()))
                    writeData(datas, true);
            } else {
                writeData(datas, false);
            }
        }));
        baseHelper.setOnWriteSuccessListener((current, total, justWrite, progress) -> {
            printLog("数据写入成功" + Arrays.toString(justWrite));
            reWriteCount = 0;
            if (esimData.getStepAction() == ActionUtils.ACTION_ESIM_URL) {
                printLog("设置url指令写入成功");
                printLog("开始写入url");
                handleMessage(ActionUtils.BEGIN_URL_TRANSFORM, null);
            }
        });
        reWriteCount++;
    }

    /**
     * 以每包100ms时间计算,超过总时间还没接收完全部数据,当做超时处理,重新开始
     */
    private void receieveOuttime() {
        if (loseNums.size() > 0) {//丢包了
            printLog("丢包数: " + loseNums.size());
            //重新开始
            setSMDPUrl();
        } else {
            //停止本次计时
            stopOutTimer();
        }
    }

    private void handleMessage(int what, byte[] bytes) {
        switch (what) {
            case ActionUtils.DATA_CHECK: //数据校验
                printLog("总接收字节数: " + bytes.length);
                checkData(bytes);
                //重置每一帧数据
                esimData.setDataBytes(null);
                break;
            case ActionUtils.URL_RESPONSE:
                //url校验回复,下一步获取请求的post param
                esimData.setStepAction(3);
                bytes[2] = (byte) 0x02;
                bytes[6] = orderBean.getEvent();
                writeData(bytes, true);
                break;
            case ActionUtils.JSON_BODY_EACH_FRAME:
                esimData.setStepAction(ActionUtils.JSON_BODY_EACH_FRAME);
                eachJsonWrite(bytes);
                break;
            case ActionUtils.BEGIN_URL_TRANSFORM: //4. 第四步,开始写入url
                esimData.setStepAction(ActionUtils.BEGIN_URL_TRANSFORM);
                byte[] urlByte = DataPacketUtils.splitEachFrameBytes(esimDevice.getUrl().getBytes());
                writeData(urlByte, false);
                break;
        }
    }

    /**
     * 写入每一帧json body数据
     *
     * @param bytes
     */
    private void eachJsonWrite(byte[] bytes) {
        int currentFrame = esimData.getCurrentFrame();
        int totalFrame = esimData.getTotalFrame();
        currentFrame++;
        esimData.setCurrentFrame(currentFrame);
        if (currentFrame > totalFrame) return;
        printLog("开始写入json body,第" + currentFrame + "帧");
        byte[] requestBytes = DataPacketUtils.sortEachFrame(bytes, currentFrame, totalFrame);
        byte[] headBytes = HelperDataUtils.frameHeadBytes(bytes, currentFrame, totalFrame, orderBean.getModule(), orderBean.getEvent());
        esimData.setHeadBytes(headBytes);
        //写入json body帧头
        writeData(headBytes, true);
        //重置数据
        if (currentFrame == totalFrame) {
            esimData.setStepAction(3);
            esimData.setCurrentFrame(0);
            esimData.setTotalFrame(0);
            esimData.setDataBytes(null);
            transferCount++;
        }
        writeData(requestBytes, false);
    }

    private void calcPackets() {
        OrderBean order = esimData.getOrderBean();
        printLog("orderBean---:" + order.toString());
        int tt = HelperDataUtils.calTotalPackets(order.getLongL(), order.getShortL());
        printLog("该接收总字节数: " + tt);
        printLog("moduleId: " + order.getModule());
        byte eventId = order.getEvent();
        printLog("eventId: " + order.getEvent());
        orderBean.setEvent(eventId);
        printLog("url接收指令: " + isContains(urlComms, eventId));
        printLog("post接收指令: " + isContains(postComms, eventId));
        if (isContains(urlComms, eventId)) {
            //请求url的回复指令
            esimData.setStepAction(0);
        } else if (isContains(postComms, eventId)) {
            esimData.setStepAction(4); //请求POST的回复
        }
        esimData.setPackets(tt);
    }


    private void checkData(byte[] data) {
        byte crcByte = data[data.length - 1];//最后一个字节为crc校验,tracker返回
        //每一帧的有效字节数据
        byte[] dataByte = Arrays.copyOfRange(data, 0, data.length - 1);
        //app计算的crc字节,用来和tracker返回的crc判断,相等则表示数据正确
        byte crcByte1 = CRCCheckUtils.calcCrc8(dataByte);
        //将字节数组转换成string
        String datas = TransformUtils.bytes2String(dataByte);
        printLog("数据校验,app crc: " + crcByte1 + "tracker crc: " + crcByte);
        if (crcByte == crcByte1) {
            if (esimData.getStepAction() == 0) { //url数据正确
                printResult("成功接收url,第" + transferCount + "次url为: " + datas);
                esimDevice.setUrl(datas);
                //以下代码为url正确的回复,app->tracker
                byte[] responeBytes = TransformUtils.combineArrays(OrderSetUtils.ESIM_PROFILE_DOWNLOAD_URL_RESP, new byte[]{0, 0});
                handleMessage(ActionUtils.URL_RESPONSE, responeBytes);
            } else if (esimData.getStepAction() == 4) { //post数据正确
                printResult("成功接收post data,第" + transferCount + "次post data为: " + datas);
                //接收完一次url和postdata则进行一次http请求,获取本次http的response code和result
                printLog("开始第" + transferCount + "次http请求");
                httpResponse(esimDevice.getUrl(), datas);
                //以下代码为post data正确的回复,app->tracker
                byte[] urlResBytes = OrderSetUtils.ESIM_PROFILE_DOWNLOAD_POST_RESP;
                urlResBytes[2] = (byte) 0x02;
                urlResBytes[6] = orderBean.getEvent();
                byte[] urlResponseBytes = TransformUtils.combineArrays(urlResBytes, new byte[]{0, 0});
                writeData(urlResponseBytes, true);
                //进行下一次
                esimData.setStepAction(3);
            }
        } else {
            //数据错误,有两种情况
            // 1 丢包,因为数据错误通常是由于丢包引起的
            // 2 crc计算出错
            // 0->url 4->post data
            printResult("接收的url或者post data错误: " + esimData.getStepAction());
            //重新开始
            setSMDPUrl();
        }

    }

    /**
     * 1. 第一步 准备设置url
     */
    private void setSMDPUrl() {
        if (esimEnum == EsimEnum.ESIM_CANCEL) {//5. 第五步,去活,根据用户操作进行
            esimData.setStepAction(ActionUtils.ACTION_ESIM_UNACTIVE);
        } else if (esimEnum == EsimEnum.ESIM_DELETE) {//5. 第五步,删除profile,根据用户操作进行
            esimData.setStepAction(ActionUtils.ACTION_ESIM_PROFILE_DELETE);
        } else if (esimEnum == EsimEnum.ESIM_ACTIVE) {
            esimData.setStepAction(ActionUtils.ACTION_ESIM_URL);
        }
        esimData.setHeadBytes(OrderSetUtils.ESIM_SM_DP);
        orderBean = new OrderBean(OrderSetUtils.ESIM_SM_DP);
        prepare();
    }

    /**
     * 5. 第五步,激活,根据用户操作进行
     */
    private void esimActiveFirst() {
        printLog("下载profile指令开始写入");
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
        printLog("headBytes---" + Arrays.toString(esimData.getHeadBytes()));
        byte eventId = orderBean.getEvent();
        orderBean.setEvent(++eventId);
        byte[] codeBytes = TransformUtils.int2TwoBytes(statusCode);
        byte[] jsonBytes = jsonBody.getBytes();
        byte[] jsonBodyBytes = TransformUtils.combineArrays(codeBytes, jsonBytes);
        int totalFrame = HelperDataUtils.calTotalFrame(jsonBodyBytes.length);
        esimData.setTotalFrame(totalFrame);
        esimData.setDataBytes(jsonBodyBytes);
        printLog("当前第" + transferCount + "次写入json body,共" + totalFrame + "帧");
        handleMessage(ActionUtils.JSON_BODY_EACH_FRAME, jsonBodyBytes);

    }

    /**
     * 激活esim第四步,profile准备工作完成,这时候用户就可以激活esim了
     *
     * @param statusCode 服务器状态码
     */
    private void esimActiveResult(int statusCode) {
        esimData.setStepAction(ActionUtils.ACTION_ESIM_ACTIVE_FORTH);
        byte eventId = orderBean.getEvent();
        eventId++;
        orderBean.setEvent(eventId);
        byte[] resultOrderBytes = OrderSetUtils.ESIM_PROFILE_DOWNLOAD_POST_RESP;
        byte[] codeBytes = TransformUtils.int2TwoBytes(statusCode);
        byte[] resultBytes = TransformUtils.combineArrays(resultOrderBytes, codeBytes);
        resultBytes[2] = (byte) 0x02;
        resultBytes[6] = eventId;
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
     * http请求
     *
     * @param url
     * @param param
     */
    private void httpResponse(String url, String param) {
        printResult("第" + transferCount + "次http请求,url: " + url);
        printResult("第" + transferCount + "次http请求,postData: " + param);
        httpHelper.setFailListener(this::httpFail);
        httpHelper.setSuccessListener(this::httpSuccess);
        httpHelper.httpRequest(url, param);
    }

    /**
     * http 请求失败回调
     * 注意!204 205也会走此回调
     *
     * @param failed
     * @param throwable
     */
    private void httpFail(Rohttp.FAILED failed, Throwable throwable) {
        if (failed.getResponseCode() == 204) {
            esimActiveResult(204);
        } else {
            httpRequestFail();
        }
        printResult("http request fail ,reason: " + throwable.getMessage());
    }

    /**
     * http请求成功回调
     *
     * @param result
     */
    private void httpSuccess(String result) {
        printResult("第" + transferCount + "次http请求成功,result: " + result);
        if (!TextUtils.isEmpty(result)) {
            printLog("开始第" + transferCount + "次json body写入");
            esimActiveNext(200, result);
        }
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
    public abstract void bleIsUnable();

    public abstract void deviceNotFound(); //没有发现设备,必须处理,否则无法交互

    public abstract void deviceDisconnects(boolean isActiveDisConnected, BleDevice device); //设备断开连接,必须处理获取给出提示,否则用户很惊讶

    /*******************必须要考虑或者肯定出现的情形 end********************/

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
