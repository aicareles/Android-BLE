# 对蓝牙4.0感兴趣可以加群进行相互讨论学习
# QQ群：494309361

#### 先来看张BleLib库的api之间的关系图：
![BleLib库结构图.png](http://upload-images.jianshu.io/upload_images/3884117-2c5a0b95cda75158.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/840)

### 1、BleDevice
```
该类的主要是来描述并记录蓝牙的属性和状态，如记录蓝牙名称、蓝牙MAC地址、蓝牙别名（即修改之后的名称）、蓝牙连接状态等。
```
### 2、BleConfig
```
该类是蓝牙配置类，里面包含了蓝牙服务的UUID、蓝牙特征的UUID、描述的UUID、以及蓝牙状态的静态常量值的标记等等，其中蓝牙相关的
UUID的设置是对外提供了接口的，用的时候可以自行传入特定的UUID即可。
```      
### 3、BleLisenter
``` 
该类提供了蓝牙各个状态的接口，此处做成了抽象类，目的是为了可以让用户有条件的去实现想要实现的方法，比如说客户想要在蓝牙扫描开
始的时候添加一些动画效果，那么你就可以实现onStart()方法，然后在其中做你想做的事情，默认是不需要实现的，如果你想要在蓝牙设备
返回数据时做出反应，那就去实现onRead()方法，如果你想在蓝牙连接失败或者超时的情况下去做特殊的处理，你就去实现onError()或者
onConnectTimeOut()方法等等。（如果各位有更好的方式可以留言提示，不胜感激）。
```
### 4、BluetoothLeService
```
该类是最重要的一个类，主要是蓝牙操作中用到的各个方法的实现类，是整个蓝牙的核心功能实现，BleManager是对外提供所有蓝牙操作接口的
管理类，当BluetoothLeService处理之后要把结果返回到BleManager中，然后再由BleManager对外提供接口。
```
### 5、Ble
```
该类提供了几乎所有你需要用到的方法，包括蓝牙扫描、连接、断开、蓝牙当前连接状态等等，管理了蓝牙操作的所有接口和方法。
```

### 使用步骤：

#### 首先buidl.gradle中添加依赖：
[![Download](https://api.bintray.com/packages/superliu/maven/BleLib/images/download.svg)](https://bintray.com/superliu/maven/BleLib/_latestVersion)
```groovy
compile 'cn.com.superLei:blelibrary:2.0.4'
```

#### 1.初始化蓝牙(判断设备是否支持BLE，蓝牙是否打开以及6.0动态授权蓝牙权限等)<br>

```
  private void initBle() {
         mBle = Ble.getInstance();
         Ble.Options options = new Ble.Options();
         options.logBleExceptions = true;//设置是否输出打印蓝牙日志
         options.throwBleException = true;//设置是否抛出蓝牙异常
         options.autoConnect = false;//设置是否自动连接
         options.scanPeriod = 12 * 1000;//设置扫描时长
         options.connectTimeout = 10 * 1000;//设置连接超时时长
         options.uuid_service = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb");//设置主服务的uuid
         options.uuid_write_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");//设置可写特征的uuid
         mBle.init(getApplicationContext(), options);
     } 
```

#### 2.开始扫描
```
mBle.startScan(scanCallback);
```
### 扫描的回调
```
BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            Toast.makeText(BleActivity.this, "ssss", Toast.LENGTH_SHORT).show();
            synchronized (mBle.getLocker()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLeDeviceListAdapter.addDevice(device);
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    };
```
#### 3.开始连接
```
mBle.connect(device, connectCallback);               
```
#### 连接的回调
```
 private BleConnCallback<BleDevice> connectCallback = new BleConnCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {
            if (device.isConnected()) {
                setNotify(device);
            }
            Log.e(TAG, "onConnectionChanged: " + device.isConnected());
            mLeDeviceListAdapter.notifyDataSetChanged();
            setConnectedNum();
        }

        @Override
        public void onConnectException(BleDevice device, int errorCode) {
            super.onConnectException(device, errorCode);
            Toast.makeText(BleActivity.this, "连接异常，异常状态码:" + errorCode, Toast.LENGTH_SHORT).show();
        }
    };
```
#### 4.设置通知及回调
```
private void setNotify(BleDevice device) {
         /*连接成功后，设置通知*/
        mBle.startNotify(device, new BleNotiftCallback<BleDevice>() {
            @Override
            public void onChanged(BluetoothGattCharacteristic characteristic) {
                Log.e(TAG, "onChanged: " + Arrays.toString(characteristic.getValue()));
            }

            @Override
            public void onReady(BleDevice device) {
                Log.e(TAG, "onReady: ");
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt) {
                Log.e(TAG, "onServicesDiscovered is success ");
            }

            @Override
            public void onNotifySuccess(BluetoothGatt gatt) {
                Log.e(TAG, "onNotifySuccess is success ");
            }
        });
    }
```
#### 5.读取远程Rssi
```
mBle.readRssi(mBle.getConnetedDevices().get(0), new BleReadRssiCallback<BleDevice>() {
                    @Override
                    public void onReadRssiSuccess(int rssi) {
                        super.onReadRssiSuccess(rssi);
                        Log.e(TAG, "onReadRssiSuccess: " + rssi);
                        Toast.makeText(BleActivity.this, "onReadRssiSuccess:"+ rssi, Toast.LENGTH_SHORT).show();
                    }
                });
```
#### 5.写入数据
```
boolean result = mBle.write(device, changeLevelInner(), new BleWriteCallback<BleDevice>() {
            @Override
            public void onWriteSuccess(BluetoothGattCharacteristic characteristic) {
                Toast.makeText(BleActivity.this, "发送数据成功", Toast.LENGTH_SHORT).show();
            }
        });
        if (!result) {
            Log.e(TAG, "changeLevelInner: " + "发送数据失败!");
        }
```
#### 6.Demo效果演示图：

![Demo预览图.gif](http://upload-images.jianshu.io/upload_images/3884117-49f080ad44b60946.gif?imageMogr2/auto-orient/strip)








































