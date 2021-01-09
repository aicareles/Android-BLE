package cn.com.heaton.blelibrary.ble.queue;

import cn.com.heaton.blelibrary.ble.model.BleDevice;


public class RequestTask {
    private String[] address;
    private BleDevice[] devices;
    private byte[] data;
    private long delay;

    RequestTask(String[] address, BleDevice[] devices, byte[] data, long delay) {
        this.address = address;
        this.devices = devices;
        this.data = data;
        this.delay = delay;
    }

    /**
     * @deprecated Use {@link Builder#getDevices()} instead.
     */
    public String[] getAddress() {
        return address;
    }

    public BleDevice[] getDevices() {
        return devices;
    }

    public byte[] getData() {
        return data;
    }

    public long getDelay() {
        return delay;
    }

    public static final class Builder {
        private String[] address;
        private BleDevice[] devices;
        private byte[] data;
        private long delay = 500;

        public Builder() {
        }

        /**
         * @deprecated Use {@link Builder#devices(BleDevice...)} instead.
         * this method will be removed in a later version
         */
        public Builder address(String... address) {
            this.address = address;
            return this;
        }

        /**
         * Array of connected devices
         * @param devices
         * @return
         */
        public Builder devices(BleDevice... devices) {
            this.devices = devices;
            return this;
        }

        public Builder data(byte[] data) {
            this.data = data;
            return this;
        }

        public Builder delay(long delay) {
            this.delay = delay;
            return this;
        }

        public RequestTask build() {
            return new RequestTask(address,devices,data,delay);
        }
    }
}
