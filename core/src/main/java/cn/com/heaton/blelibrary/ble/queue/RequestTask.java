package cn.com.heaton.blelibrary.ble.queue;

public class RequestTask {
    private String address;
    private byte[] data;
    private long delay;

    RequestTask(String address, byte[] data, long delay) {
        this.address = address;
        this.data = data;
        this.delay = delay;
    }

    public String getAddress() {
        return address;
    }

    public byte[] getData() {
        return data;
    }

    public long getDelay() {
        return delay;
    }

    public static final class Builder {
        private String address;
        private byte[] data;
        private long delay = 500;

        public Builder() {
        }

        public Builder address(String address) {
            this.address = address;
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
            return new RequestTask(address,data,delay);
        }
    }
}
