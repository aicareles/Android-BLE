package cn.com.heaton.blelibrary.ble.queue;

public class RequestTask {
    private String address;
    private byte[] data;

    private RequestTask(String address, byte[] data) {
        this.address = address;
        this.data = data;
    }

    public static RequestTask newWriteTask(String address, byte[] data){
        return new RequestTask(address, data);
    }

    public static RequestTask newConnectTask(String address){
        return new RequestTask(address, null);
    }

    public String getAddress() {
        return address;
    }

    public byte[] getData() {
        return data;
    }

}
