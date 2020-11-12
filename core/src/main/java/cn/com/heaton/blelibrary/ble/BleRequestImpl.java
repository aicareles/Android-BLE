package cn.com.heaton.blelibrary.ble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.v4.os.HandlerCompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.com.heaton.blelibrary.BuildConfig;
import cn.com.heaton.blelibrary.ble.callback.wrapper.ConnectWrapperCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.DescWrapperCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.MtuWrapperCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.NotifyWrapperCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.ReadRssiWrapperCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.ReadWrapperCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.WriteWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.request.ConnectRequest;
import cn.com.heaton.blelibrary.ble.request.DescriptorRequest;
import cn.com.heaton.blelibrary.ble.request.MtuRequest;
import cn.com.heaton.blelibrary.ble.request.NotifyRequest;
import cn.com.heaton.blelibrary.ble.request.ReadRequest;
import cn.com.heaton.blelibrary.ble.request.ReadRssiRequest;
import cn.com.heaton.blelibrary.ble.request.Rproxy;
import cn.com.heaton.blelibrary.ble.request.WriteRequest;
import cn.com.heaton.blelibrary.ble.utils.ByteUtils;
import cn.com.heaton.blelibrary.ota.OtaListener;


public final class BleRequestImpl<T extends BleDevice> {

    private final static String TAG = BleRequestImpl.class.getSimpleName();

    private static BleRequestImpl instance;
    private Handler handler = BleHandler.of();
    private Ble.Options options;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private final Object locker = new Object();
    private List<BluetoothGattCharacteristic> notifyCharacteristics = new ArrayList<>();//Notification attribute callback array
    private int notifyIndex = 0;//Notification feature callback list
    private BluetoothGattCharacteristic otaWriteCharacteristic;//Ota ble send the object
    private boolean otaUpdating = false;//Whether the OTA is updated
    private Map<String, BluetoothGattCharacteristic> writeCharacteristicMap = new HashMap<>();
    private Map<String, BluetoothGattCharacteristic> readCharacteristicMap = new HashMap<>();
    //Multiple device connections must put the gatt object in the collection
    private Map<String, BluetoothGatt> gattHashMap = new HashMap<>();
    //The address of the connected device
    private List<String> connectedAddressList = new ArrayList<>();
    private ConnectWrapperCallback<T> connectWrapperCallback;
    private NotifyWrapperCallback<T> notifyWrapperCallback;
    private MtuWrapperCallback<T> mtuWrapperCallback;
    private ReadRssiWrapperCallback<T> readRssiWrapperCallback;
    private ReadWrapperCallback<T> readWrapperCallback;
    private DescWrapperCallback<T> descWrapperCallback;
    private WriteWrapperCallback<T> writeWrapperCallback;
    private OtaListener otaListener;//Ota update operation listener

    private BleRequestImpl(){}

    //在各种状态回调中发现连接更改或服务
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice device = gatt.getDevice();
            if (device == null){
                return;
            }
            String address = device.getAddress();
            //remove timeout callback
            cancelTimeout(address);
            T bleDevice = getBleDeviceInternal(address);
            //There is a problem here Every time a new object is generated that causes the same device to be disconnected and the connection produces two objects
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedAddressList.add(device.getAddress());
                    if (connectWrapperCallback != null){
                        bleDevice.setConnectionState(BleDevice.CONNECTED);
                        connectWrapperCallback.onConnectionChanged(bleDevice);
                    }
                    BleLog.d(TAG, "onConnectionStateChange:----device is connected.");
                    BluetoothGatt bluetoothGatt = gattHashMap.get(device.getAddress());
                    if (null != bluetoothGatt){
                        // Attempts to discover services after successful connection.
                        BleLog.d(TAG, "trying to start service discovery");
                        bluetoothGatt.discoverServices();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    BleLog.d(TAG, "onConnectionStateChange:----device is disconnected.");
                    if (connectWrapperCallback != null){
                        bleDevice.setConnectionState(BleDevice.DISCONNECT);
                        connectWrapperCallback.onConnectionChanged(bleDevice);
                    }
                    close(device.getAddress());
                }
            } else {
                //Occurrence 133 or 257 19 Equal value is not 0: Connection establishment failed due to protocol stack
                BleLog.e(TAG, "onConnectionStateChange----: " + "Connection status is abnormal:" + status);
                close(device.getAddress());
                if (connectWrapperCallback != null){
                    int errorCode = getErrorCode(bleDevice);
                    connectWrapperCallback.onConnectException(bleDevice, errorCode);
                    bleDevice.setConnectionState(BleDevice.DISCONNECT);
                    connectWrapperCallback.onConnectionChanged(bleDevice);
                }
            }

        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status){
            if (gatt != null && gatt.getDevice() != null) {
                BleLog.d(TAG, "onMtuChanged mtu=" + mtu + ",status=" + status);
                if (null != mtuWrapperCallback){
                    mtuWrapperCallback.onMtuChanged(getBleDeviceInternal(gatt.getDevice().getAddress()), mtu, status);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Empty the notification attribute list
                notifyCharacteristics.clear();
                notifyIndex = 0;
                displayGattServices(gatt);
            } else {
                BleLog.e(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (gatt == null || gatt.getDevice() == null)return;
            BleLog.d(TAG, "onCharacteristicRead:" + status);
            T bleDevice = getBleDeviceInternal(gatt.getDevice().getAddress());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (null != readWrapperCallback){
                    readWrapperCallback.onReadSuccess(bleDevice, characteristic);
                }
            }else {
                if (null != readWrapperCallback){
                    readWrapperCallback.onReadFailed(bleDevice, status);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (gatt == null || gatt.getDevice() == null)return;
            BleLog.d(TAG, gatt.getDevice().getAddress() + "-----write success----- status: " + status);
            synchronized (locker) {
                T bleDevice = getBleDeviceInternal(gatt.getDevice().getAddress());
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (null != writeWrapperCallback){
                        writeWrapperCallback.onWriteSuccess(bleDevice, characteristic);
                    }
                    if (options.uuid_ota_write_cha.equals(characteristic.getUuid())) {
                        if (otaListener != null) {
                            otaListener.onWrite();
                        }
                    }
                }else {
                    if (null != writeWrapperCallback){
                        writeWrapperCallback.onWriteFailed(bleDevice, status);
                    }
                }
            }
        }

        /**
         * 当连接成功的时候会回调这个方法，这个方法可以处理发送密码或者数据分析
         * 当setnotify（true）被设置时，如果MCU（设备端）上的数据改变，则该方法被回调。
         * @param gatt 蓝牙gatt对象
         * @param characteristic 蓝牙通知特征对象
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            synchronized (locker) {
                if (gatt == null || gatt.getDevice() == null)return;
                BleLog.d(TAG, gatt.getDevice().getAddress() + " -- onCharacteristicChanged: "
                        + (characteristic.getValue() != null ? ByteUtils.toHexString(characteristic.getValue()) : ""));
                T bleDevice = getBleDeviceInternal(gatt.getDevice().getAddress());
                if (notifyWrapperCallback != null) {
                    notifyWrapperCallback.onChanged(bleDevice, characteristic);
                }
                if (options.uuid_ota_write_cha.equals(characteristic.getUuid()) || options.uuid_ota_notify_cha.equals(characteristic.getUuid())) {
                    if (otaListener != null) {
                        otaListener.onChange(characteristic.getValue());
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            if (gatt == null || gatt.getDevice() == null)return;
            UUID uuid = descriptor.getCharacteristic().getUuid();
            BleLog.d(TAG, "write descriptor uuid:" + uuid);
            synchronized (locker) {
                T bleDevice = getBleDeviceInternal(gatt.getDevice().getAddress());
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (null != descWrapperCallback){
                        descWrapperCallback.onDescWriteSuccess(bleDevice, descriptor);
                    }
                    if (notifyCharacteristics.size() > 0 && notifyIndex < notifyCharacteristics.size()) {
                        BleLog.d(TAG, "set characteristic notification, notify_index is "+notifyIndex);
                        setCharacteristicNotification(gatt.getDevice().getAddress(), true);
                    } else {
                        BleLog.d(TAG, "set characteristic notification is completed");
                        if (notifyWrapperCallback != null) {
                            if (Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                                    || Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)){
                                notifyWrapperCallback.onNotifySuccess(bleDevice);
                            }else if (Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)){
                                notifyWrapperCallback.onNotifyCanceled(bleDevice);
                            }

                        }
                    }
                }else {
                    if (null != descWrapperCallback){
                        descWrapperCallback.onDescWriteFailed(bleDevice, status);
                    }
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            if (gatt == null || gatt.getDevice() == null)return;
            UUID uuid = descriptor.getCharacteristic().getUuid();
            BleLog.d(TAG, "read descriptor uuid:" + uuid);
            T bleDevice = getBleDeviceInternal(gatt.getDevice().getAddress());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (null != descWrapperCallback){
                    descWrapperCallback.onDescReadSuccess(bleDevice, descriptor);
                }
            }else {
                if (null != descWrapperCallback){
                    descWrapperCallback.onDescReadFailed(bleDevice, status);
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            BleLog.d(TAG, "read remoteRssi, rssi: "+rssi);
            if (gatt == null || gatt.getDevice() == null)return;
            if (null != readRssiWrapperCallback){
                readRssiWrapperCallback.onReadRssiSuccess(getBleDeviceInternal(gatt.getDevice().getAddress()), rssi);
            }
        }
    };

    private int getErrorCode(T bleDevice) {
        if (bleDevice.isConnected()) {//Mcu connection is broken or the signal is weak and other reasons disconnect
            return BleStates.ConnectException;
        } else if (bleDevice.isConnecting()) {//Connection failed
            return BleStates.ConnectFailed;
        } else {//Abnormal state (in theory, there is no such situation)
            return BleStates.ConnectError;
        }
    }

    private T getBleDeviceInternal(String address) {
        ConnectRequest<T> request = Rproxy.getRequest(ConnectRequest.class);
        return request.getBleDevice(address);
    }

    /**
     *
     * @return 已经连接的设备集合
     */
    public List<BluetoothDevice> getConnectedDevices() {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null){
            return bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        }
        return null;
    }

    public static <T extends BleDevice> BleRequestImpl<T> getBleRequest(){
        if (instance == null){
            instance = new BleRequestImpl();
        }
        return instance;
    }

    void initialize(Context context) {
        this.connectWrapperCallback = Rproxy.getRequest(ConnectRequest.class);
        this.notifyWrapperCallback = Rproxy.getRequest(NotifyRequest.class);
        this.mtuWrapperCallback = Rproxy.getRequest(MtuRequest.class);
        this.readWrapperCallback = Rproxy.getRequest(ReadRequest.class);
        this.readRssiWrapperCallback = Rproxy.getRequest(ReadRssiRequest.class);
        this.writeWrapperCallback = Rproxy.getRequest(WriteRequest.class);
        this.descWrapperCallback = Rproxy.getRequest(DescriptorRequest.class);
        this.context = context;
        this.options = Ble.options();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    void release() {
        connectWrapperCallback = null;
        mtuWrapperCallback = null;
        notifyWrapperCallback = null;
        readRssiWrapperCallback = null;
        readWrapperCallback = null;
        writeWrapperCallback = null;
        handler.removeCallbacksAndMessages(null);
        BleLog.d(TAG, "BleRequestImpl is released");
    }

    public void cancelTimeout(String address){
        handler.removeCallbacksAndMessages(address);
    }

    /**
     * 连接蓝牙
     *
     * @param bleDevice BleDevice
     * @return Connection result
     */
    public boolean connect(final T bleDevice) {
        String address = bleDevice.getBleAddress();
        if (connectedAddressList.contains(bleDevice.getBleAddress()) && bleDevice.isConnected()) {
            BleLog.e(TAG, "this is device already connected.");
            connectWrapperCallback.onConnectException(bleDevice, BleStates.ConnectedAlready);
            return false;
        }
        if (bluetoothAdapter == null) {
            BleLog.e(TAG, "bluetoothAdapter not available");
            connectWrapperCallback.onConnectException(bleDevice, BleStates.NotAvailable);
            return false;
        }
        // getRemoteDevice(address) will throw an exception if the device address is invalid,
        // so it's necessary to check the address
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            BleLog.e(TAG, "the device address is invalid");
            connectWrapperCallback.onConnectException(bleDevice, BleStates.InvalidAddress);
            return false;
        }
        // Previously connected device. Try to reconnect. ()
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            BleLog.e(TAG, "no device");
            connectWrapperCallback.onConnectException(bleDevice, BleStates.DeviceNull);
            return false;
        }
        //10s after the timeout prompt
        HandlerCompat.postDelayed(handler, new Runnable() {
            @Override
            public void run() {
                connectWrapperCallback.onConnectTimeOut(bleDevice);
                close(device.getAddress());
            }
        }, device.getAddress(), options.connectTimeout);
        bleDevice.setConnectionState(BleDevice.CONNECTING);
        bleDevice.setBleName(device.getName());
        connectWrapperCallback.onConnectionChanged(bleDevice);
        // We want to directly connect to the device, so we are setting the autoConnect parameter to false
        BluetoothGatt bluetoothGatt;
        //TODO 连接双通道蓝牙必须设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            bluetoothGatt = device.connectGatt(context, false, gattCallback);
        }
        if (bluetoothGatt != null) {
            gattHashMap.put(address, bluetoothGatt);
            BleLog.d(TAG, "Trying to create a new connection.");
            return true;
        }
        return false;
    }

    /**
     * 断开蓝牙
     *
     * @param address 蓝牙地址
     */
    public void disconnect(String address) {
        BluetoothGatt gatt = gattHashMap.get(address);
        if (gatt != null){
            gatt.disconnect();
        }
        notifyIndex = 0;
        notifyCharacteristics.clear();
        writeCharacteristicMap.remove(address);
        readCharacteristicMap.remove(address);
        otaWriteCharacteristic = null;
    }

    /**
     * 清除蓝牙蓝牙连接设备的指定蓝牙地址
     *
     * @param address 蓝牙地址
     */
    public void close(String address) {
        BluetoothGatt gatt = gattHashMap.get(address);
        if (gatt != null) {
            gatt.close();
            gattHashMap.remove(address);
        }
        connectedAddressList.remove(address);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean setMtu(String address, int mtu){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mtu>20){
            BluetoothGatt gatt = gattHashMap.get(address);
            if (gatt != null) {
                boolean result = gatt.requestMtu(mtu);
                BleLog.d(TAG,"requestMTU "+mtu+" result="+result);
                return result;
            }
        }
        return false;
    }

    /**
     * 清除所有可连接的设备
     */
    public void close() {
        for (String address : connectedAddressList) {
            BluetoothGatt gatt = gattHashMap.get(address);
            if (gatt != null) {
                gatt.close();
            }
        }
        gattHashMap.clear();
        connectedAddressList.clear();
    }

    /**
     * 清理蓝牙缓存
     */
    public boolean refreshDeviceCache(String address) {
        BluetoothGatt gatt = gattHashMap.get(address);
        if (gatt != null) {
            try {
                Method localMethod = gatt.getClass().getMethod(
                        "refresh", new Class[0]);
                if (localMethod != null) {
                    boolean bool = ((Boolean) localMethod.invoke(
                            gatt, new Object[0])).booleanValue();
                    return bool;
                }
            } catch (Exception localException) {
                BleLog.e(TAG, "An exception occured while refreshing device");
            }
        }
        return false;
    }


    /**
     * 写入数据
     *
     * @param address 蓝牙地址
     * @param value   发送的字节数组
     * @return 写入是否成功(这个是客户端的主观认为)
     */
    public boolean writeCharacteristic(String address, byte[] value) {
        BluetoothGattCharacteristic gattCharacteristic = writeCharacteristicMap.get(address);
        if (gattCharacteristic != null) {
            if (options.uuid_write_cha.equals(gattCharacteristic.getUuid())) {
                gattCharacteristic.setValue(value);
                boolean result = gattHashMap.get(address).writeCharacteristic(gattCharacteristic);
                BleLog.d(TAG, address + " -- write result:" + result);
                return result;
            }
        }else {
            if (null != writeWrapperCallback){
                writeWrapperCallback.onWriteFailed(getBleDeviceInternal(address), BleStates.NotInitUuid);
            }
        }
        return false;
    }

    public boolean writeCharacteristicByUuid(String address, byte[] value, UUID serviceUUID, UUID characteristicUUID) {
        BluetoothGatt bluetoothGatt = gattHashMap.get(address);
        BluetoothGattCharacteristic characteristic = gattCharacteristic(bluetoothGatt, serviceUUID, characteristicUUID);
        if (characteristic != null) {
            characteristic.setValue(value);
            boolean result = bluetoothGatt.writeCharacteristic(characteristic);
            BleLog.d(TAG, address + " -- write result:" + result);
            return result;
        }
        return false;
    }

    public BluetoothGattCharacteristic gattCharacteristic(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID){
        if (gatt == null){
            BleLog.e(TAG, "BluetoothGatt is null");
            return null;
        }
        BluetoothGattService gattService = gatt.getService(serviceUUID);
        if (gattService == null){
            BleLog.e(TAG, "serviceUUID is null");
            return null;
        }
        BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(characteristicUUID);
        if (characteristic == null){
            BleLog.e(TAG, "characteristicUUID is null");
            return null;
        }
        return characteristic;
    }

    /**
     * 读取数据
     *
     * @param address 蓝牙地址
     * @return 读取是否成功(这个是客户端的主观认为)
     */
    public boolean readCharacteristic(String address) {
        BluetoothGattCharacteristic gattCharacteristic = readCharacteristicMap.get(address);
        if (gattCharacteristic != null) {
            if (options.uuid_read_cha.equals(gattCharacteristic.getUuid())) {
                boolean result = gattHashMap.get(address).readCharacteristic(gattCharacteristic);
                BleLog.d(TAG, "read result:" + result);
                return result;
            }
        }else {
            if (null != readWrapperCallback){
                readWrapperCallback.onReadFailed(getBleDeviceInternal(address), BleStates.NotInitUuid);
            }
        }
        return false;
    }

    public boolean readCharacteristicByUuid(String address, UUID serviceUUID, UUID characteristicUUID) {
        BluetoothGatt bluetoothGatt = gattHashMap.get(address);
        BluetoothGattCharacteristic gattCharacteristic = gattCharacteristic(bluetoothGatt, serviceUUID, characteristicUUID);
        if (gattCharacteristic != null) {
            boolean result = bluetoothGatt.readCharacteristic(gattCharacteristic);
            BleLog.d(TAG, address + " -- read result:" + result);
            return result;
        }
        return false;
    }

    public boolean readDescriptor(String address, UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID){
        BluetoothGatt bluetoothGatt = gattHashMap.get(address);
        BluetoothGattCharacteristic gattCharacteristic = gattCharacteristic(bluetoothGatt, serviceUUID, characteristicUUID);
        if (gattCharacteristic != null){
            BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(descriptorUUID);
            if (descriptor != null){
                return bluetoothGatt.readDescriptor(descriptor);
            }
        }
        return false;
    }

    public boolean writeDescriptor(String address, byte[] data, UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID){
        BluetoothGatt bluetoothGatt = gattHashMap.get(address);
        BluetoothGattCharacteristic gattCharacteristic = gattCharacteristic(bluetoothGatt, serviceUUID, characteristicUUID);
        if (gattCharacteristic != null){
            BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(descriptorUUID);
            if (descriptor != null){
                descriptor.setValue(data);
                return bluetoothGatt.writeDescriptor(descriptor);
            }
        }
        return false;
    }

    /**
     * 读取远程rssi
     * @param address 蓝牙地址
     * @return 是否读取rssi成功
     */
    public boolean readRssi(String address) {
        boolean result = gattHashMap.get(address).readRemoteRssi();
        BleLog.d(TAG, address + "read result:" + result);
        return result;
    }

    /**
     * 启用或禁用给定特征的通知
     *
     * @param address        蓝牙地址
     * @param enabled   是否设置通知使能
     */
    public void setCharacteristicNotification(String address, boolean enabled) {
        if (notifyCharacteristics.size() > 0 && notifyIndex < notifyCharacteristics.size()){
            BluetoothGattCharacteristic characteristic = notifyCharacteristics.get(notifyIndex++);
            setCharacteristicNotificationInternal(gattHashMap.get(address), characteristic, enabled);
        }
    }

    public void setCharacteristicNotificationByUuid(String address, boolean enabled, UUID serviceUUID, UUID characteristicUUID) {
        BluetoothGatt bluetoothGatt = gattHashMap.get(address);
        BluetoothGattCharacteristic characteristic = gattCharacteristic(bluetoothGatt, serviceUUID, characteristicUUID);
        setCharacteristicNotificationInternal(bluetoothGatt, characteristic, enabled);
    }

    private void setCharacteristicNotificationInternal(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean enabled){
        if (characteristic == null){
            BleLog.e(TAG, "characteristic is null");
            if (notifyWrapperCallback != null){
                notifyWrapperCallback.onNotifyFailed(getBleDeviceInternal(gatt.getDevice().getAddress()), BleStates.CharaUuidNull);
            }
            return;
        }
        gatt.setCharacteristicNotification(characteristic, enabled);
        //If the number of descriptors in the eigenvalue of the notification is greater than zero
        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
        if (!descriptors.isEmpty()) {
            //Filter descriptors based on the uuid of the descriptor
            for(BluetoothGattDescriptor descriptor : descriptors){
                if (descriptor != null) {
                    //Write the description value
                    if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0){
                        descriptor.setValue(enabled?BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE:BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }else if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0){
                        //两个都是通知的意思，notify和indication的区别在于，notify只是将你要发的数据发送给手机，没有确认机制，
                        //不会保证数据发送是否到达。而indication的方式在手机收到数据时会主动回一个ack回来。即有确认机制，只有收
                        //到这个ack你才能继续发送下一个数据。这保证了数据的正确到达，也起到了流控的作用。所以在打开通知的时候，需要设置一下。
                        descriptor.setValue(enabled?BluetoothGattDescriptor.ENABLE_INDICATION_VALUE:BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }
                    gatt.writeDescriptor(descriptor);
                    BleLog.d(TAG, "setCharacteristicNotificationInternal is "+enabled);
                }
            }
        }
    }

    private void displayGattServices(BluetoothGatt gatt) {
        //是否设置了service_uuid,且service_uuid是否存在
        boolean service_uuid_exist = false;
        BluetoothDevice device = gatt.getDevice();
        List<BluetoothGattService> gattServices = gatt.getServices();
        if (gattServices == null || device == null) {
            BleLog.e(TAG, "displayGattServices gattServices or device is null");
            if (device != null){
                close(device.getAddress());
            }
            return;
        }
        if (gattServices.isEmpty()) {
            BleLog.e(TAG, "displayGattServices gattServices size is 0");
            disconnect(device.getAddress());
            return;
        }
        if (connectWrapperCallback != null) {
            T bleDevice = getBleDeviceInternal(device.getAddress());
            connectWrapperCallback.onServicesDiscovered(bleDevice, gatt);
        }
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            String uuid = gattService.getUuid().toString();
            BleLog.d(TAG, "discovered gattServices: " + uuid);
            if (uuid.equals(options.uuid_service.toString()) || isContainUUID(uuid)) {
                service_uuid_exist = true;
                BleLog.i(TAG, "service_uuid is set up successfully:" + uuid);
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    String char_uuid = gattCharacteristic.getUuid().toString();
                    BleLog.d(TAG, "characteristic_uuid: " + char_uuid);
                    int charaProp = gattCharacteristic.getProperties();
                    StringBuilder properties_builder = new StringBuilder();
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                        properties_builder.append("write,");
                    }
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                        properties_builder.append("write_no_response,");
                    }
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                        properties_builder.append("read,");
                    }
                    //Auto obtain Notification feature
                    if ((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        notifyCharacteristics.add(gattCharacteristic);
                        properties_builder.append("notify,");
                    } if((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0){
                        notifyCharacteristics.add(gattCharacteristic);
                        properties_builder.append("indicate,");
                    }
                    int length = properties_builder.length();
                    if (length > 0){
                        properties_builder.deleteCharAt(length-1);
                        BleLog.d(TAG, properties_builder.insert(0,"characteristic properties is ").toString());
                    }

                    if (char_uuid.equals(options.uuid_write_cha.toString())) {
                        BleLog.i(TAG,"write characteristic set up successfully:"+char_uuid);
                        writeCharacteristicMap.put(device.getAddress(), gattCharacteristic);
                        //Notification feature
                    } if (char_uuid.equals(options.uuid_read_cha.toString())) {
                        BleLog.i(TAG,"read characteristic set up successfully:"+char_uuid);
                        readCharacteristicMap.put(device.getAddress(), gattCharacteristic);
                    }
                }
            }
        }
        if (!service_uuid_exist){
            BleLog.e(TAG, "init Ble.options().setUuidService(uuid_service) error, and uuid_service not the uuid of your device");
        }
        if (null != connectWrapperCallback){
            connectWrapperCallback.onReady(getBleDeviceInternal(device.getAddress()));
        }
    }

    //是否包含该uuid
    private boolean isContainUUID(String uuid) {
        for (UUID u : options.uuid_services_extra){
            if(u != null && uuid.equals(u.toString())){
                return true;
            }
        }
        return false;
    }

    /**
     * 获取可写特征对象
     * @param address 蓝牙地址
     * @return  可写特征对象
     */
    public BluetoothGattCharacteristic getWriteCharacteristic(String address) {
        synchronized (locker) {
            if (writeCharacteristicMap != null) {
                return writeCharacteristicMap.get(address);
            }
            return null;
        }
    }

    /**
     * 获取可读特征对象
     * @param address 蓝牙地址
     * @return  可读特征对象
     */
    public BluetoothGattCharacteristic getReadCharacteristic(String address) {
        synchronized (locker) {
            if (readCharacteristicMap != null) {
                return readCharacteristicMap.get(address);
            }
            return null;
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @param address ble address
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(String address) {
        if (gattHashMap.get(address) == null){
            return null;
        }
        return gattHashMap.get(address).getServices();
    }

    public BluetoothGatt getBluetoothGatt(String address){
        return gattHashMap.get(address);
    }

    /**
     * 写入OTA数据
     *
     * @param address 蓝牙地址
     * @param value   发送字节数组
     * @return 写入是否成功
     */
    public boolean writeOtaData(String address, byte[] value) {
        try {
            if (otaWriteCharacteristic == null) {
                otaUpdating = true;
                BluetoothGattService bluetoothGattService = gattHashMap.get(address).getService(options.uuid_ota_service);
                if (bluetoothGattService == null) {
                    return false;
                } else {
                    BluetoothGattCharacteristic mOtaNotifyCharacteristic = bluetoothGattService.getCharacteristic(options.uuid_ota_notify_cha);
                    if (mOtaNotifyCharacteristic != null) {
                        gattHashMap.get(address).setCharacteristicNotification(mOtaNotifyCharacteristic, true);
                    }
                    otaWriteCharacteristic = bluetoothGattService.getCharacteristic(options.uuid_ota_write_cha);
                }

            }
            if (otaWriteCharacteristic != null && options.uuid_ota_write_cha.equals(otaWriteCharacteristic.getUuid())) {
                otaWriteCharacteristic.setValue(value);
                boolean result = writeCharacteristic(gattHashMap.get(address), otaWriteCharacteristic);
                BleLog.d(TAG, address + " -- write data:" + Arrays.toString(value));
                BleLog.d(TAG, address + " -- write result:" + result);
                return result;
            }
            return true;
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            close();
            return false;
        }
    }

    //The basic method of writing data
    private boolean writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        synchronized (locker) {
            return !(gatt == null || characteristic == null) && gatt.writeCharacteristic(characteristic);
        }
    }

    /**
     * OTA升级完成
     */
    public void otaUpdateComplete() {
        otaUpdating = false;
    }

    /**
     * 设置OTA是否正在升级
     *
     * @param updating 升级状态
     */
    public void setOtaUpdating(boolean updating) {
        this.otaUpdating = updating;
    }

    /**
     * 设置OTA更新状态监听
     *
     * @param otaListener 监听对象
     */
    public void setOtaListener(OtaListener otaListener) {
        this.otaListener = otaListener;
    }
}
