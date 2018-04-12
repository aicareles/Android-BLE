### 对蓝牙感兴趣可以加群讨论学习(QQ：494309361)
### Email：jerryee0911@qq.com

#### 一、先来看张BleLib库的api之间的关系图：
![BleLib库结构图.png](http://upload-images.jianshu.io/upload_images/3884117-2c5a0b95cda75158.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/840)

### 1、BleDevice
```
该类的主要是来描述并记录蓝牙的属性和状态，如记录蓝牙名称、蓝牙MAC地址、蓝牙别名（即修改之后的名称）、蓝牙连接状态等,该类处在被保护状态，禁止外部随意生成该对象，
如果有特别需求可以通过BleFactory.create(...)进行创建该对象（并不建议）
```
### 2、BleStatus
```
该类是蓝牙状态类，定义了蓝牙扫描、连接、通知使能、发送、接收等状态的常量值（连接异常等状态码可参考该类）
```      
### 3、BluetoothLeService
```
该类是最重要的一个类，主要是蓝牙操作中用到的各个方法的实现类，是整个蓝牙的核心功能实现，BleManager是对外提供所有蓝牙操作接口的
管理类，当BluetoothLeService处理之后要把结果返回到BleManager中，然后再由BleManager对外提供接口。
```
### 4、Ble
```
该类提供了几乎所有你需要用到的方法，包括蓝牙扫描、连接、断开、蓝牙当前连接状态等等，管理了蓝牙操作的所有接口和方法。
```

### 二、历史版本介绍：
[![Version](https://img.shields.io/badge/BleLib-v2.1.2-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.1.2)
```
1、修复上个版本依赖地址变化，导致依赖时出现问题。
2、添加清理蓝牙缓存接口
```
[![Version](https://img.shields.io/badge/BleLib-v2.1.1-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.1.1)
```
适配更新5.0以上版本手机扫描的API
```
[![Version](https://img.shields.io/badge/BleLib-v2.1.0-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.1.0)
```
该版本添加跳转到测试界面（先连接设备），在测试界面操作该蓝牙对象的扫描、连接、断开、通知等回调的接口（任意界面都可以随心所欲的操作或者拿到mcu返回的数据）
1、在其他界面你也想拿到蓝牙设备传过来的数据，你可以这样做：（重要）
    //测试通知
    public void testNotify(BleDevice device) {
        if(device != null){
            mNotifyStatus.setText("设置通知监听成功！！！");
            mBle.startNotify(device, new BleNotiftCallback<BleDevice>() {
                @Override
                public void onChanged(BluetoothGattCharacteristic characteristic) {
                    Log.e(TAG, "onChanged: " + Arrays.toString(characteristic.getValue()));
                    mNotifyValue.setText("收到MCU通知值:\n"+Arrays.toString(characteristic.getValue()));
                }
            });
        }
    }
2、在其他界面也想连接或者断开上个界面的设备对象，你可以这么做：
    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //测试连接或断开
                final BleDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                if (mBle.isScanning()) {
                    mBle.stopScan();
                }
                if (device.isConnected()) {
                    //2.1.0版本新增接口（mBle.disconnect(device)接口仍可正常使用）
                    mBle.disconnect(device, connectCallback);
                } else if (!device.isConnectting()) {
                    mBle.connect(device, connectCallback);
                }
            }
        });
3、扫描、发送数据、读取数据等接口都可如上正常使用（与之前版本一样）
```
[![Version](https://img.shields.io/badge/BleLib-v2.0.5-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.0.5)
```
该版本添加根据蓝牙地址获取蓝牙对象的接口
```
[![Version](https://img.shields.io/badge/BleLib-v2.0.4-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.0.4)
```
该版本修复读、写、通知特征uuid相同的情况下，出现无法设置通知的BUG
```
[![Version](https://img.shields.io/badge/BleLib-v2.0.3-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.0.3)
```
该版本优化内部api结构，但并不影响外部接口调用
```
[![Version](https://img.shields.io/badge/BleLib-v2.0.2-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.0.2)
```
该版本增加主动读取数据以及读取远程RSSI值的接口
```
[![Version](https://img.shields.io/badge/BleLib-v2.0.0-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.0.0)
```
该版本重构V 1.x版本，作为全新升级版本，重大类、接口进行重命名，使各个接口完成单一功能，并使之得到单一功能的回调。
注：至此V 2.0.0版本以后不再维护1.x版本
```
[![Version](https://img.shields.io/badge/BleLib-v1.0.4-blue.svg)](https://bintray.com/superliu/maven/BleLib/1.0.4)
```
增加了自动连接的接口
```
[![Version](https://img.shields.io/badge/BleLib-v1.0.0-blue.svg)](https://bintray.com/superliu/maven/BleLib/1.0.0)
```
初始版本，可完成基本的BLE蓝牙一系列操作（扫描、连接、断开、设置通知、发送数据等等）
```

### 三、使用步骤：

#### 首先buidl.gradle中添加依赖：
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/superliu/maven/BleLib/images/download.svg)](https://bintray.com/superliu/maven/BleLib/_latestVersion)
```groovy
compile 'cn.com.superLei:blelibrary:2.1.2'
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
#### 6.OTA升级
```
//找到你需要升级文件的路径(一般情况都是保存再服务器上，一旦有更新会自动提示，然后APP下载并保存到本地，生成对应的file对象)
File file = new File(...);
//读写SD卡权限，此处略（6.0及以上需添加)
OtaManager mOtaManager = new OtaManager(BleActivity.this);
boolean result = mOtaManager.startOtaUpdate(file, (BleDevice) mBle.getConnetedDevices().get(0), mBle);
Log.e("OTA升级结果:", result + "");
```
### 四、Demo效果演示图：

![Demo预览图.gif](http://upload-images.jianshu.io/upload_images/3884117-49f080ad44b60946.gif?imageMogr2/auto-orient/strip)








































