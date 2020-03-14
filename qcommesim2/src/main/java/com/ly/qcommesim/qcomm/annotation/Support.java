package com.ly.qcommesim.qcomm.annotation;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/25 14:46
 * version: 1.0
 */

/**
 * <p>All types of support the service can know for a the qualified element such as: a feature, a protocol, etc.</p>
 */
@IntDef(flag = true, value = {Support.NOT_SUPPORTED, Support.SUPPORTED, Support.DO_NOT_KNOW})
@Retention(RetentionPolicy.SOURCE)
@SuppressLint("ShiftFlags") // values are more readable this way
public @interface Support {
    /**
     * <p>The element is not supported.</p>
     */
    int NOT_SUPPORTED = 0;
    /**
     * <p>The element us supported.</p>
     */
    int SUPPORTED = 1;
    /**
     * <p>The service does not know if the element is supported.</p>
     */
    int DO_NOT_KNOW = 2;
}

