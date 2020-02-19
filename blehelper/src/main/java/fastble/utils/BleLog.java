package fastble.utils;


import android.util.Log;

public final class BleLog {

    public static boolean isPrint = true;
    private static String defaultTag = "FastBle";

    public static void d(String msg) {
        if (msg == null) {
            msg = "null";
        }
        if (isPrint)
            Log.d(defaultTag, msg);
    }

    public static void i(String msg) {
        if (msg == null) {
            msg = "null";
        }
        if (isPrint)
            Log.i(defaultTag, msg);
    }

    public static void w(String msg) {
        if (msg == null) {
            msg = "null";
        }
        if (isPrint)
            Log.w(defaultTag, msg);
    }

    public static void e(String msg) {
        if (msg == null) {
            msg = "null";
        }
        if (isPrint)
            Log.e(defaultTag, msg);
    }

    public static void d(String tag, String msg) {
        if (msg == null) {
            msg = "null";
        }
        if (isPrint)
            Log.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (msg == null) {
            msg = "null";
        }
        if (isPrint)
            Log.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (msg == null) {
            msg = "null";
        }
        if (isPrint)
            Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (msg == null) {
            msg = "null";
        }
        if (isPrint)
            Log.e(tag, msg);
    }

}
