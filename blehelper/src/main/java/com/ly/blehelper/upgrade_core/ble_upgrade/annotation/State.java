package com.ly.blehelper.upgrade_core.ble_upgrade.annotation;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/25 14:54
 * version: 1.0
 */


import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>The possible values for the Bluetooth connection state of this service.</p>
 */
@IntDef({ State.CONNECTED, State.DISCONNECTED, State.CONNECTING, State.DISCONNECTING })
@Retention(RetentionPolicy.SOURCE)
public @interface State {
    int DISCONNECTED = 0;
    int CONNECTING = 1;
    int CONNECTED = 2;
    int DISCONNECTING = 3;
}
