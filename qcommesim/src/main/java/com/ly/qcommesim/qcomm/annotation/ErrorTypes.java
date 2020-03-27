package com.ly.qcommesim.qcomm.annotation;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/25 15:12
 * version: 1.0
 */

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>All different errors which can occur when using the vumpgrade library.</p>
 */
@IntDef(flag = true, value = {ErrorTypes.ERROR_BOARD_NOT_READY, ErrorTypes.WRONG_DATA_PARAMETER, ErrorTypes
        .RECEIVED_ERROR_FROM_BOARD, ErrorTypes.EXCEPTION, ErrorTypes.AN_UPGRADE_IS_ALREADY_PROCESSING,
        ErrorTypes.NO_FILE })
@Retention(RetentionPolicy.SOURCE)
@SuppressLint("ShiftFlags") // values are more readable this way
public @interface ErrorTypes {

    /**
     * 尝试启动升级但未准备好处理主板时，会发生此错误。
     */
    int ERROR_BOARD_NOT_READY = 1;
    /**
     *
     * 当从板上收到的VMU数据包与预期数据不匹配时，就会发生此错误：信息过多，信息不足等。
     */
    int WRONG_DATA_PARAMETER = 2;
    /**
     *
     * 当主板在升级过程中通知内部发生错误或警告时，就会发生此错误。
     */
    int RECEIVED_ERROR_FROM_BOARD = 3;
    /**
     *其他异常情况
     */
    int EXCEPTION = 4;
    /**
     *
     * 尝试启动升级但VMUManager已经在处理升级时，将发生此错误。
     */
    int AN_UPGRADE_IS_ALREADY_PROCESSING = 5;
    /**
     *
     * 当要上传的文件为空或不存在时，会发生此错误。
     */
    int NO_FILE = 6;
}
