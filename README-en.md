
# Android-BLE
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/superliu/maven/BleLib/images/download.svg)](https://bintray.com/superliu/maven/BleLib/_latestVersion)

Android-BLE Bluetooth framework, including scanning, connecting, enabling / disabling notifications, sending / reading data, receiving data, reading rssi, setting mtu and other Bluetooth-related operation interfaces, internally optimized connection queue, and fast write queue,
And support multi-service communication, can be extended to configure Bluetooth related operations.

## Android-BLE   API
* **Ble** - The most important class provides all Bluetooth operation interfaces to the outside world.
* **BleDevice** - Bluetooth object class, including Bluetooth connection status and basic Bluetooth information.
* **BleLog** - Internal log class, open in the development environment to view Bluetooth related operation information.
* **BleStates** - Bluetooth operation abnormal status code information class. (Abnormal status codes such as scan, connection, read and write)
* **ByteUtils** - Various byte data conversion tools

## Documentation/[中文](https://github.com/aicareles/Android-BLE/wiki/BLE%E5%BA%93%E4%BD%BF%E7%94%A8%E6%AD%A5%E9%AA%A4)
### 1. Edit **build.gradle** file and add dependency.
``` groovy
implementation 'cn.com.superLei:blelibrary:latestVersion'
```
### 2. Init the Bluetooth library in Application.
```
private void initBle() {
        Ble ble = Ble.options()//Open configuration
                .setLogBleEnable(true)//Set whether to print Bluetooth log
                .setThrowBleException(true)//Set whether to throw Bluetooth exception
                .setAutoConnect(false)
                .setIgnoreRepeat(false)//filter the scanned devices
                .setConnectTimeout(10 * 1000)//connection timeout
                .setMaxConnectNum(7)//Maximum number of connections
                .setScanPeriod(12 * 1000)//scan duration
                .setScanFilter(scanFilter)//scan filter
               .setUuidService(UUID.fromString(UuidUtils.uuid16To128("fd00")))//uuid of main service (Required)
                .setUuidWriteCha(UUID.fromString(UuidUtils.uuid16To128("fd01")))//uuid for writable features (Required)
                .setUuidReadCha(UUID.fromString(UuidUtils.uuid16To128("fd02")))//uuid for readable features (Optional)
                .setUuidNotifyCha(UUID.fromString(UuidUtils.uuid16To128("fd03")))//uuid for notification feature (Optional)
               .setFactory(new BleFactory() {
                    @Override
                    public MyDevice create(String address, String name) {
                        return new MyDevice(address, name);
                    }
                })
                .setBleWrapperCallback(new MyBleWrapperCallback())
                .create(mApplication, new Ble.InitCallback() {
                    @Override
                    public void success() {
                        BleLog.e("MainApplication", "init success");
                    }

                    @Override
                    public void failed(int failedCode) {
                        BleLog.e("MainApplication", "init failed：" + failedCode);
                    }
                });
     }
```
### 3. Start use.
#### 1.scan
```
ble.startScan(scanCallback);
```
#### scan callback (Note: turn on bluetooth and check bluetooth permissions)
```
BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            //Scanned devices
        }

       @Override
        public void onStart() {
            super.onStart();
        }

        @Override
        public void onStop() {
            super.onStop();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed: "+errorCode);
        }
    };
```
#### 2.connect/disconnect
```
//Connect a device
ble.connect(device, connectCallback);

//Connect multiple devices
ble.connects(devices, connectCallback);

//Cancel the connecting device
ble.cancelConnecting(device);

//Cancel the connecting devices
ble.cancelConnectings(devices);

//disconnect a device
ble.disconnect(device);

//disconnect all devices
ble.disconnectAll();
```
#### connect/disconnect callback
```
private BleConnCallback<BleDevice> connectCallback = new BleConnCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {

        }

        @Override
        public void onConnectTimeOut(BleDevice device) {
            super.onConnectTimeOut(device);
            Log.e(TAG, "onConnectTimeOut: " + device.getBleAddress());
        }

        @Override
        public void onConnectCancel(BleDevice device) {
            super.onConnectCancel(device);
            Log.e(TAG, "onConnectCancel: " + device.getBleName());
        }

        @Override
        public void onServicesDiscovered(BleDevice device, BluetoothGatt gatt) {
            super.onServicesDiscovered(device, gatt);
        }

        @Override
        public void onReady(BleDevice device) {
            super.onReady(device);
            //connect successful to enable notification
            ble.enableNotify(...);
        }

        @Override
        public void onConnectException(BleDevice device, int errorCode) {
            super.onConnectException(device, errorCode);

        }
    };
```
#### 3.enable/disable notification
```
ble.enableNotify(device, true, new BleNotifyCallback<BleDevice>() {
        @Override
        public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            BleLog.e(TAG, "onChanged==uuid:" + uuid.toString());
            BleLog.e(TAG, "onChanged==data:" + ByteUtils.toHexString(characteristic.getValue()));
        }

        @Override
        public void onNotifySuccess(BleDevice device) {
            super.onNotifySuccess(device);
            BleLog.e(TAG, "onNotifySuccess: "+device.getBleName());
        }
    });
```
#### 4.read data
```
ble.read(device, new BleReadCallback<BleRssiDevice>() {
            @Override
            public void onReadSuccess(BleRssiDevice dedvice, BluetoothGattCharacteristic characteristic) {
                super.onReadSuccess(dedvice, characteristic);
            }

            @Override
            public void onReadFailed(BleRssiDevice device, int failedCode) {
                super.onReadFailed(device, failedCode);
            }
        })
```
#### 5.write data
```
//write a package payload
ble.write(device, data, new BleWriteCallback<BleRssiDevice>() {
    @Override
    public void onWriteSuccess(BleRssiDevice device, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void onWriteFailed(BleRssiDevice device, int failedCode) {
        super.onWriteFailed(device, failedCode);
    }
});

//write large file/payload
byte[]data = toByteArray(getAssets().open("WhiteChristmas.bin"));
ble.writeEntity(mBle.getConnectedDevices().get(0), data, 20, 50, new BleWriteEntityCallback<BleDevice>() {
    @Override
    public void onWriteSuccess() {

    }

    @Override
    public void onWriteFailed() {

    }

    override void onWriteProgress(double progress) {

    }

    override void onWriteCancel() {

    }
});

//write data to the queue (The default interval is 50ms)
ble.writeQueue(RequestTask.newWriteTask(address, data));
//write data to the queue with delay
ble.writeQueueDelay(delay, RequestTask.newWriteTask(address, data));

//Custom write data by service and characteristic uuid
ble.writeByUuid(device, data, serviceUuid, charUuid, new BleWriteCallback<BleRssiDevice>() {
    @Override
    public void onWriteSuccess(BleRssiDevice device, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void onWiteFailed(BleRssiDevice device, int failedCode) {
        super.onWiteFailed(device, failedCode);
    }
});
```
#### 6. remove callback (scan、connect)
```
ble.cancelCallback(connectCallback);
or
ble.cancelCallback(scanCallback);
```
#### 8. release
```
 ble.released();
```

## History version introduction：
[History version](https://github.com/aicareles/Android-BLE/wiki/BLE%E5%BA%93%E5%8E%86%E5%8F%B2%E7%89%88%E6%9C%AC%E4%BB%8B%E7%BB%8D)

## BLE Common problems and solutions
Please see this [Wiki BLE Page][Wiki] for more infos.

[Wiki]:https://github.com/aicareles/Android-BLE/wiki#连接常见问题

## Java-Sample Screenshot：

![2](https://github.com/aicareles/Android-BLE/blob/master/screenshots/2.jpeg)
![3](https://github.com/aicareles/Android-BLE/blob/master/screenshots/3.jpeg)
![4](https://github.com/aicareles/Android-BLE/blob/master/screenshots/4.jpeg)

## *License*
```
Copyright 2016 jerry

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contribute：
![](https://upload-images.jianshu.io/upload_images/3884117-5d22ae84180a93ed.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/320)
![](https://upload-images.jianshu.io/upload_images/3884117-1f6c1c0fb5885252.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/320)








































