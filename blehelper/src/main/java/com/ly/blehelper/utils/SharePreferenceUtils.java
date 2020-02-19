package com.ly.blehelper.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/9/30 11:11
 * version: 1.0
 */
public class SharePreferenceUtils {
    private static final String SP_NAME = "SP_PRE_BLE";

    public static boolean setValue(Context context, String key, Object value) {
        SharedPreferences.Editor sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit();
        if (value instanceof String) {
            String values = (String) value;
            return sp.putString(key, values).commit();
        } else if (value instanceof Integer) {
            Integer values = (Integer) value;
            return sp.putInt(key, values).commit();
        } else if (value instanceof Boolean) {
            Boolean values = (Boolean) value;
            return sp.putBoolean(key, values).commit();
        } else if (value instanceof Float) {
            Float values = (Float) value;
            return sp.putFloat(key, values).commit();
        } else if (value instanceof Long) {
            Long values = (Long) value;
            return sp.putLong(key, values).commit();
        } else if (value instanceof Set) {
            Set<String> values = (Set<String>) value;
            return sp.putStringSet(key, values).commit();
        }
        return false;
    }

    public static Object getValue(Context context, String key, Object defaultValue) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        if (defaultValue instanceof String) {
            return sp.getString(key, (String) defaultValue);
        } else if (defaultValue instanceof Integer) {
            return sp.getInt(key, (Integer) defaultValue);
        } else if (defaultValue instanceof Boolean) {
            return sp.getBoolean(key, (Boolean) defaultValue);
        } else if (defaultValue instanceof Float) {
            return sp.getFloat(key, (Float) defaultValue);
        } else if (defaultValue instanceof Long) {
            return sp.getLong(key, (Long) defaultValue);
        } else if (defaultValue instanceof Set) {
            return sp.getStringSet(key, (Set) defaultValue);
        }
        return null;
    }
}
