package com.ly.createaar;

import com.de.esim.rohttp.Rohttp;
import com.de.esim.rohttp.helper.callback.HttpCallback;
import com.ly.qcommesim.core.utils.TransformUtils;
import com.xble.xble.core.utils.CRC8Utils;
import com.xble.xble.core.utils.TimerHelper;

public class Test {
    private static byte[] urlComms=new byte[]{(byte) 0x51,(byte) 0x54,(byte) 0x57,(byte) 0x5A};
    private static TimerHelper rssiTimer;

    public static void main(String[] args){
        String testMac="";
//        System.out.println(Arrays.binarySearch(urlComms,(byte)0x52));
//        className(new Test());
//        if (Arrays.binarySearch(urlComms,(byte)0x51)>0){
//            System.out.println("good");
//        }
//        System.out.println(getDistance(-92));
//        rssiTimer();
//        testHttp();
//        testCrc();
        byte[] gps=new byte[]{00, 01 ,58 ,0x7a ,0x4c ,06, (byte) 0xca,0x4e, (byte)0xf6, 00};
        System.out.println(TransformUtils.byte2Int((byte)-92));
    }

    private static void testCrc(){
        byte[] testData=new byte[]{123, 10, 9, 34, 116, 114, 97, 110, 115, 97, 99, 116, 105, 111, 110, 73, 100, 34, 58, 9, 34, 48, 51, 50, 56, 48, 65, 55, 52, 52, 70, 48, 66, 68, 70, 53, 70, 48, 49, 53, 68, 56, 55, 65, 52, 52, 56, 70, 56, 67, 56, 57, 51, 34, 44, 10, 9, 34, 112, 114, 101, 112, 97, 114, 101, 68, 111, 119, 110, 108, 111, 97, 100, 82, 101, 115, 112, 111, 110, 115, 101, 34, 58, 9, 34, 118, 121, 71, 66, 110, 113, 67, 66, 109, 122, 66, 87, 103, 66, 65, 68, 75, 65, 112, 48, 84, 119, 118, 102, 88, 119, 70, 100, 104, 54, 82, 73, 43, 77, 105, 84, 88, 48, 108, 66, 66, 72, 108, 71, 98, 122, 106, 51, 56, 79, 120, 116, 80, 56, 100, 102, 66, 75, 77, 50, 47, 109, 80, 110, 51, 47, 57, 73, 78, 71, 52, 43, 104, 54, 82, 108, 85, 111, 50, 53, 86, 110, 90, 103, 113, 121, 108, 102, 79, 75, 78, 109, 74, 102, 121, 77, 78, 49, 118, 71, 47, 72, 105, 83, 105, 87, 72, 74, 111, 57, 50, 43, 82, 105, 105, 80, 89, 97, 52, 51, 51, 54, 113, 84, 55, 74, 116, 102, 78, 48, 68, 53, 87, 105, 122, 118, 71, 66, 57, 90, 121, 118, 106, 118, 109, 110, 113, 87, 90, 121, 90, 118, 78, 84, 51, 99, 83, 103, 101, 109, 54, 117, 53, 83, 120, 74, 76, 67, 102, 71, 52, 53, 72, 78, 85, 115, 65, 111, 55, 89, 53, 115, 74, 115, 66, 118, 88, 56, 70, 77, 80, 49, 85, 77, 80, 108, 67, 65, 103, 54, 88, 83, 108, 115, 101, 110, 79, 57, 84, 78, 100, 69, 75, 48, 116, 81, 34, 10};
        byte crc = CRC8Utils.caculate_crc8(testData);
        System.out.println(crc);
        System.out.println(TransformUtils.bytes2String(testData));

    }

    private static void className(Object o){
        System.out.println("className: "+o.getClass().getSimpleName());
    }
    public static double getDistance(int rssi) {
        int iRssi = Math.abs(rssi);
        double power = (iRssi - 72) / (10 * 2.0);
        return Math.pow(10, power);
    }

    private static void rssiTimer(){
        if (rssiTimer!=null){
            rssiTimer.stop();
        }
        rssiTimer=new TimerHelper(){
            @Override
            public void doSomething() {
                System.out.println("timer---run");
            }
        };

        rssiTimer.start(5000);
    }

    private static void testHttp(){
        String url="https://2f72b92a.cpolar.cn/gsma/rsp2/es9plus/initiateAuthentication";
        String param="{" +
                "    \"euiccInfo1\":\"vyBhggMCAgCpLAQUZloUM9Z8GixduLUsln8QoFe6XLIEFPOD/jxllz2v0A6m3YpLU9YkU9asqiwEFGZaFDPWfBosXbi1LJZ/EKBXulyyBBTzg/48ZZc9r9AOpt2KS1PWJFPWrA==\",\n" +
                "    \"euiccChallenge\":\"CaGd54Fy2RXTjohlJp43WQ==\"," +
                "    \"smdpAddress\":\"2f72b92a.cpolar.cn\"" +
                "    }";
        new Rohttp().roBaseUrl(url).roParam(param).roPost(new HttpCallback<String>() {
            @Override
            public void onSuccess(String result) {
                System.out.println(result);
//                printResult("第" + transferCount + "次http请求成功,result: " + result);
//                if (!TextUtils.isEmpty(result)) {
//                    int code = 200;
//                    String body = null;
//                    if (body != null) {
//                        printLog("开始第" + transferCount + "次json body写入");
//                        esimActiveNext(code, body);
//                    }
//                    if (code == 204) {
//                        esimActiveResult(code);
//                    }
//                }

            }

            @Override
            public void onFailed(Rohttp.FAILED failed, Throwable throwable) {
//                printResult("http request fail ,reason: " + throwable.getMessage());
            }

            @Override
            public void onFinished() {

            }
        });
    }
}
