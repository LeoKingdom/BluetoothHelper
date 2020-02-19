package com.ly.blehelper.core.helper;

import java.util.UUID;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/5/31 10:39
 * version: 1.0
 * <p>
 * 蓝牙的各种uuid
 */
public class BleUuidHelper {
    //服务UUID组
    private UUID[] serviceUuids;
    //单个服务UUID
    private String serviceUuid;
    //通知UUID
    private String notiyUuid;
    //读操作UUID
    private String readUuid;
    //读特征UUID
    private String readChaUuid;
    //写操作UUID
    private String writeUuid;
    //写特征UUID
    private String writeChaUuid;


    public UUID[] getServiceUuids() {
        return serviceUuids;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    public String getNotiyUuid() {
        return notiyUuid;
    }

    public String getReadUuid() {
        return readUuid;
    }

    public String getReadChaUuid() {
        return readChaUuid;
    }

    public String getWriteUuid() {
        return writeUuid;
    }

    public String getWriteChaUuid() {
        return writeChaUuid;
    }

    public static class Builder {
        private UUID[] serviceUuids;
        private String serviceUuid;
        private String notiyUuid;
        private String readUuid;
        private String readChaUuid;
        private String writeUuid;
        private String writeChaUuid;

        public Builder setServiceUuids(UUID[] serviceUuids) {
            this.serviceUuids = serviceUuids;
            return this;
        }

        public Builder setServiceUuid(String serviceUuid) {
            this.serviceUuid = serviceUuid;
            return this;
        }

        public Builder setNotiyUuid(String notiyUuid) {
            this.notiyUuid = notiyUuid;
            return this;
        }

        public Builder setReadUuid(String readUuid) {
            this.readUuid = readUuid;
            return this;
        }

        public Builder setReadChaUuid(String readChaUuid) {
            this.readChaUuid = readChaUuid;
            return this;
        }

        public Builder setWriteUuid(String writeUuid) {
            this.writeUuid = writeUuid;
            return this;
        }

        public Builder setWriteChaUuid(String writeChaUuid) {
            this.writeChaUuid = writeChaUuid;
            return this;
        }

        private void setUuids(BleUuidHelper uuidHelper) {
            uuidHelper.serviceUuids = this.serviceUuids;
            uuidHelper.serviceUuid = this.serviceUuid;
            uuidHelper.notiyUuid = this.notiyUuid;
            uuidHelper.readUuid = this.readUuid;
            uuidHelper.readChaUuid = this.readChaUuid;
            uuidHelper.writeUuid = this.writeUuid;
            uuidHelper.writeChaUuid = this.writeChaUuid;
        }

        public BleUuidHelper build() {
            BleUuidHelper bleUuidHelper = new BleUuidHelper();
            setUuids(bleUuidHelper);
            return bleUuidHelper;
        }
    }

}
