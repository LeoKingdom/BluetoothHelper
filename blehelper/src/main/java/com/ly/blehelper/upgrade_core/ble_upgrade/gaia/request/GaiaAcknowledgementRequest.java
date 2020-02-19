/**************************************************************************************************
 * Copyright 2016 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.ly.blehelper.upgrade_core.ble_upgrade.gaia.request;


import com.ly.blehelper.upgrade_core.ble_upgrade.gaia.GAIA;

/**
 * The data structure to define an acknowledgement request.
 */
public class GaiaAcknowledgementRequest extends GaiaRequest {

    /**
     * The status for the acknowledgement.
     */
    public @GAIA.Status final int status;
    /**
     * Any data to add to the ACK.
     */
    public final byte[] data;

    /**
     * To build a new request of type acknowledgement.
     */
    public GaiaAcknowledgementRequest(@GAIA.Status int status, byte[] data) {
        super(GaiaRequest.Type.ACKNOWLEDGEMENT);
        this.status = status;
        this.data = data;
    }
}
