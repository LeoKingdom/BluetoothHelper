package com.ly.qcommesim.core.utils;

import com.xble.xble.core.log.Logg;

public class PrintLog {

    /**
     * 打印过程 (只打印1次)
     *
     * @param text 内容
     */
    public static void printLogOne(Class clazz,String text) {
        if (!Logg.isPrintOne) {
            Logg.t(clazz.getSimpleName()).recordLog(clazz, text, Logg.VERBOSE);
            Logg.isPrintOne = true;
        }

    }
    /**
     * 打印过程
     *
     * @param text 内容
     */
    public static void printLog(Class clazz, String text) {
        Logg.t(clazz.getSimpleName()).recordLog(clazz, text, Logg.VERBOSE);
    }

    /**
     * 打印结果
     *
     * @param text 内容
     */
    public static void printResult(Class clazz, String text) {
        Logg.t(clazz.getSimpleName()).recordLog(clazz, text, Logg.INFO);
    }

    /**
     * 打印错误
     *
     * @param text 内容
     */
    public static void printError(Class clazz, String text) {
        Logg.t(clazz.getSimpleName()).recordLog(clazz, text, Logg.ERROR);
    }
}
