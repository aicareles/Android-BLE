
### 个人项目或者定制化需求加v:liulei633001 进行联系，如果需要单独咨询相关问题，记得先请喝杯咖啡哦！！！
### Email：jerryee0911@qq.com

### [下载APK](https://github.com/aicareles/Android-BLE/blob/master/apk/BLE-v3.3.0.apk)


# Android-BLE
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/superliu/maven/BleLib/images/download.svg)](https://bintray.com/superliu/maven/BleLib/_latestVersion)

Android-BLE蓝牙框架,提供了扫描、连接、使能/除能通知、发送/读取数据、接收数据,读取rssi,设置mtu等蓝牙相关的所有操作接口,内部优化了连接队列,以及快速写入队列,
并支持多服务通讯,可扩展配置蓝牙相关操作。

## Android-BLE   API
* **Ble** - 最重要的类,对外提供所有的蓝牙操作接口.
* **BleDevice** - 封装了蓝牙对象类,包含蓝牙连接状态以及基本蓝牙信息.
* **BleLog** - 内部日志类,开发环境下打开可查看蓝牙相关操作信息.
* **BleStates** - 蓝牙操作异常状态码信息类.(扫描、连接、读写等异常状态码).
* **ByteUtils** - 各种字节数据转换的工具类.
* **CrcUtils** - 字节校验的crc各种算法的工具类.
* **UuidUtils** - 蓝牙服务/特征uuid转换工具类.

## 异常状态码
[BleStates](https://github.com/aicareles/Android-BLE/blob/master/core/src/main/java/cn/com/heaton/blelibrary/ble/BleStates.java)

## 接入前提示
```
1. 如果项目中的设备是统一类型(服务,特征uuid相同),则推荐在初始化时把服务,特征的uuid配置完整。
2. 如果项目中需要兼容多种设备类型(服务,特征uuid不相同),则在通信时需要使用byUuid的方式进行。
```

## 接入文档
### 1. 在 **build.gradle** 中添加下面依赖. [![Download](https://api.bintray.com/packages/superliu/maven/BleLib/images/download.svg)](https://bintray.com/superliu/maven/BleLib/_latestVersion)
``` groovy
implementation 'cn.com.superLei:blelibrary:latestVersion'
```

### android12 权限适配
``` xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
``` 
### 2. 在Application中初始化.
```
private void initBle() {
    Ble.options()//开启配置
        .setLogBleEnable(true)//设置是否输出打印蓝牙日志（非正式打包请设置为true，以便于调试）
        .setThrowBleException(true)//设置是否抛出蓝牙异常 （默认true）
        .setAutoConnect(false)//设置是否自动连接 （默认false）
        .setIgnoreRepeat(false)//设置是否过滤扫描到的设备(已扫描到的不会再次扫描)
        .setConnectTimeout(10 * 1000)//设置连接超时时长（默认10*1000 ms）
        .setMaxConnectNum(7)//最大连接数量
        .setScanPeriod(12 * 1000)//设置扫描时长（默认10*1000 ms）
        .setScanFilter(scanFilter)//设置扫描过滤
        .setUuidService(UUID.fromString(UuidUtils.uuid16To128("fd00")))//设置主服务的uuid（必填）
        .setUuidWriteCha(UUID.fromString(UuidUtils.uuid16To128("fd01")))//设置可写特征的uuid （必填,否则写入失败）
        .setUuidReadCha(UUID.fromString(UuidUtils.uuid16To128("fd02")))//设置可读特征的uuid （选填）
        .setUuidNotifyCha(UUID.fromString(UuidUtils.uuid16To128("fd03")))//设置可通知特征的uuid （选填，库中默认已匹配可通知特征的uuid）
        .setUuidServicesExtra(new UUID[]{BATTERY_SERVICE_UUID})//设置额外的其他服务组，如电量服务等
        .setFactory(new BleFactory() {//实现自定义BleDevice时必须设置
            @Override
            public MyDevice create(String address, String name) {
                return new MyDevice(address, name);//自定义BleDevice的子类
            }
        })
        .setBleWrapperCallback(new MyBleWrapperCallback())//设置全部蓝牙相关操作回调（例： OTA升级可以再这里实现,与项目其他功能逻辑完全解耦）
        .create(mApplication, new Ble.InitCallback() {
            @Override
            public void success() {
                BleLog.e("MainApplication", "初始化成功");
            }

            @Override
            public void failed(int failedCode) {
                BleLog.e("MainApplication", "初始化失败：" + failedCode);
            }
        });
     }
```
### 3. 开始使用.
#### 1.扫描
```
ble.startScan(scanCallback);
```
#### 扫描回调 (注: 记得打开蓝牙并检查是否授予蓝牙权限)
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
#### 2.连接/断开
```
//连接设备
ble.connect(device, connectCallback);

//连接多个设备
ble.connects(devices, connectCallback);

//取消正在连接的设备
ble.cancelConnecting(device);

//取消正在连接的多个设备
ble.cancelConnectings(devices);

//断开设备
ble.disconnect(device);

//断开所有设备
ble.disconnectAll();
```
#### 连接/断开回调
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
#### 3.使能/除能通知
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
#### 4.读取数据
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
#### 5.写入数据
```
//写入一包数据
ble.write(device, data, new BleWriteCallback<BleRssiDevice>() {
    @Override
    public void onWriteSuccess(BleRssiDevice device, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void onWriteFailed(BleRssiDevice device, int failedCode) {
        super.onWriteFailed(device, failedCode);
    }
});

//写入大数据(文件、图片等)
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

//写入数据到队列中 (默认发送间隔50ms)
ble.writeQueue(RequestTask.newWriteTask(address, data));
//写入数据到队列中 (自定义间隔时间)
ble.writeQueueDelay(delay, RequestTask.newWriteTask(address, data));

//通过特定服务和特征值uuid写入数据
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
#### 6. 移除监听(scan、connect)
```
ble.cancelCallback(connectCallback);
或
ble.cancelCallback(scanCallback);
```
#### 8. 释放资源
```
 ble.released();
```

## 历史版本介绍：
[历史版本](https://github.com/aicareles/Android-BLE/wiki/BLE%E5%BA%93%E5%8E%86%E5%8F%B2%E7%89%88%E6%9C%AC%E4%BB%8B%E7%BB%8D)

## BLE蓝牙常见问题及解决方案
请通过该 [Wiki BLE Page][Wiki] 了解更多信息.

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








































