package com.ly.createaar;

import com.xble.xble.core.utils.TimerHelper;

import java.util.Arrays;

public class Test {
    private static byte[] urlComms=new byte[]{(byte) 0x51,(byte) 0x54,(byte) 0x57,(byte) 0x5A};
    private static TimerHelper rssiTimer;

    public static void main(String[] args){
        String testMac="";
        System.out.println(Arrays.binarySearch(urlComms,(byte)0x52));
        className(new Test());
        if (Arrays.binarySearch(urlComms,(byte)0x51)>0){
            System.out.println("good");
        }
        System.out.println(getDistance(-92));
        rssiTimer();
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
}
