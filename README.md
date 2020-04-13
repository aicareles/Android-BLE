####  对蓝牙感兴趣可以加群讨论学习(QQ：494309361)
### 有个人项目或者定制化需求的可加QQ:823581722 进行联系
### Email：jerryee0911@qq.com

### 扫描下载APK:(安装密码:android)
![二维码.png](
https://android-resource.oss-cn-qingdao.aliyuncs.com/GitClub/image/mJW4.png?Expires=1577947521&OSSAccessKeyId=TMP.hhbo7QSce5gPRayo3tJUYViA69964YTNBHGVNQd8PJ6L8PVwXatiaKGcL52pcneAzwAwv8jASidyskmj3g5HypuBK8AFrBRBN7Hi8krKU8pbGWNa4fxDaHsERWtJWC.tmp&Signature=ni2nOP8Ordl5srIw9LnvBJ1I5lY%3D)

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
[![Version](https://img.shields.io/badge/BleLib-v3.0.5-blue.svg)](https://bintray.com/superliu/maven/BleLib/3.0.5)
```
1.添加自定义BleDevice接口（实现BleDeviceFactory接口）
public class BleRssiDevice extends BleDevice{
    //添加自定义属性值
    private int deviceType;
    private int rssi;
    ...
}
必须在初始化时设置
    Ble.options().setFactory(new BleFactory() {//实现自定义BleDevice时必须设置
        @Override
        public BleRssiDevice create(String address, String name) {
            return new BleRssiDevice(address, name);//自定义BleDevice的子类
        }
    })

2.添加实现所有回调接口的Callback（可用于OTA升级等，完全解耦项目）

public class MyBleWrapperCallback extends BleWrapperCallback<BleDevice> {
    ...
}

Ble.options().setBleWrapperCallback(new MyBleWrapperCallback())
```
[![Version](https://img.shields.io/badge/BleLib-v3.0.0-blue.svg)](https://bintray.com/superliu/maven/BleLib/3.0.0)
```
1.添加写入队列(异步,可自定义队列每个任务延迟时间)
    @CheckConnect //检查是否连接
    private void writeQueue() {
        String address = ble.getConnetedDevices().get(0).getBleAddress();
        for (int i = 0; i < 30; i++) {
            //ble.writeQueueDelay(50, RequestTask.newWriteTask(address, "hello android".getBytes()));
            ble.writeQueue(RequestTask.newWriteTask(address, "hello android".getBytes()));
        }
    }
2.同时连接多个
    ble.connects(adapter.getDevices(), connectCallback);
3.取消连接多个设备(取消正在连接/已加入队列中还未连接的设备)
    ble.cancelConnecttings(adapter.getDevices());
4.更新部分:
    BleConnectCallback<BleDevice> connectCallback = new BleConnectCallback<BleDevice>() {
        @Override
        public void onReady(BleDevice device) {
            super.onReady(device);
            /*连接成功后，在次回调中设置通知,否则收不到设备的数据*/
            ble.startNotify(device, bleNotiftCallback);
        }
    };
```
[![Version](https://img.shields.io/badge/BleLib-v2.6.1-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.6.1)
```
添加自动分包发送接口(无需添加延迟，自动根据系统底层返回结果处理)
    EntityData entityData = new EntityData.Builder()
                .setLastPackComplete(true)//最后一包是否自动补零,默认false
                .setAutoWriteMode(autoWriteMode)//是否设置自动模式发送,默认false
                .setAddress(device.getBleAddress())
                .setData(data)//大数据文件的字节数组
                .setPackLength(20)//每包发送字节数
                .setDelay(50L)//自动模式下无需设置delay
                .build();
    mBle.writeEntity(entityData, new BleWriteEntityCallback<BleDevice>() {
         @Override
         public void onWriteSuccess() {
              ...
         }

         @Override
         public void onWriteFailed() {
              ...
         }
    });

```
[![Version](https://img.shields.io/badge/BleLib-v2.6.0-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.6.0)
```
优化频繁收到通知数据，偶尔丢包的问题
```
[![Version](https://img.shields.io/badge/BleLib-v2.5.4-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.5.4)
```
1、添加过滤扫描设备接口
    mBle = Ble.options()
                 .setLogBleExceptions(true)
                 .setThrowBleException(true)
                 .setAutoConnect(true)
                 .setFilterScan(true)//设置是否过滤扫描到的设备
                 .setConnectFailedRetryCount(3)
                 .setConnectTimeout(10 * 1000)
                 .setScanPeriod(12 * 1000)
                 .setUuidService(UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb"))
                 .setUuidWriteCha(UUID.fromString("d44bc439-abfd-45a2-b575-925416129600"))
                 .create(getApplicationContext());
 2、优化自动重连接口
 3、添加Android5.0发送广播的接口
    byte[] payload = new byte[16];
    payload[0] = 0x01;
    mBle.startAdvertising(payload);
 4、收到广播包并解析后的数据
    BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
            @Override
            public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
                Log.e(TAG, "onLeScan: "+device.getBleAddress());
                synchronized (mBle.getLocker()) {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            }

            //收到广播包并解析后的数据
            @Override
            public void onParsedData(BleDevice device, ScanRecord scanRecord) {
                super.onParsedData(device, scanRecord);
                byte[] data = scanRecord.getManufacturerSpecificData(65535);//参数为厂商id
                if (data != null){
                    Log.e(TAG, "onParsedData: "+ ByteUtils.BinaryToHexString(data));
                }
            }
        };


```

[![Version](https://img.shields.io/badge/BleLib-v2.5.3-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.5.3)
```
1、添加发送大数据量的进度回调
    private void sendEntityData() throws IOException {
            byte[] data = ByteUtils.toByteArray(getAssets().open("WhiteChristmas.bin"));
            Log.e(TAG, "sendEntityData: "+data.length);
            mBle.writeEntity(mBle.getConnetedDevices().get(0), data, 20, 50, new BleWriteEntityCallback<BleDevice>() {
                @Override
                public void onWriteSuccess() {
                    L.e("writeEntity", "onWriteSuccess");
                }

                @Override
                public void onWriteFailed() {
                    L.e("writeEntity", "onWriteFailed");
                }

                @Override
                public void onWriteProgress(double progress) {
                    Log.e("writeEntity", "当前发送进度: "+progress);
                }

                @Override
                public void onWriteCancel() {
                    Log.e(TAG, "onWriteCancel: ");
                }
            });
        }
2、修复持续接收通知回调过快导致数据重复的问题
3、修复多设备连接，同时断开所有设备，回调次数不完整的问题
```
[![Version](https://img.shields.io/badge/BleLib-v2.5.2%20beta-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.5.2-beta)
```
1、添加自动连接的接口(使用新的初始化写法)
    mBle = Ble.options()
                 .setLogBleEnable(true)
                 .setThrowBleException(true)
                 .setAutoConnect(true)//自动重连
                 .setConnectFailedRetryCount(3)
                 .setConnectTimeout(10 * 1000)
                 .setScanPeriod(12 * 1000)
                 .setUuidService(UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb"))
                 .setUuidWriteCha(UUID.fromString("d44bc439-abfd-45a2-b575-925416129600"))
                 .create(getApplicationContext());
2、优化断开连接后，自动移除通知监听的问题
```
[![Version](https://img.shields.io/badge/BleLib-v2.5.0-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.5.0)
```
1、添加了发送大数据包（如：文件等）的接口
    try {
        //获取整个文件的总字节
        byte[]data = toByteArray(getAssets().open("WhiteChristmas.bin"));
        //发送大数据量的包
        mBle.writeEntity(mBle.getConnetedDevices().get(0), data, 20, 50, new BleWriteEntityCallback<BleDevice>() {
            @Override
            public void onWriteSuccess() {
                L.e("writeEntity", "onWriteSuccess");
            }

            @Override
            public void onWriteFailed() {
                L.e("writeEntity", "onWriteFailed");
            }
        });
    } catch (IOException e) {
        e.printStackTrace();
    }
2、修复了正在扫描时，关闭蓝牙导致停止扫描onStop()不回调的问题
```
[![Version](https://img.shields.io/badge/BleLib-v2.3.0-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.3.0)
```
1、添加通过mac地址连接的接口:
    String address = device.getBleAddress();//或者  String address = "3E:9A:4A:71:F6:4D";
    mBle.connect(address, new BleConnCallback<BleDevice>() {
         @Override
         public void onConnectionChanged(BleDevice device) {

         }
       });
2、添加BLE4.2的设置MTU的接口:
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
        //此处第二个参数  不是特定的   比如你也可以设置500   但是如果设备不支持500个字节则会返回最大支持数
        mBle.setMTU(mBle.getConnetedDevices().get(0).getBleAddress(), 250, new BleMtuCallback<BleDevice>() {
            @Override
            public void onMtuChanged(BleDevice device, int mtu, int status) {
                super.onMtuChanged(device, mtu, status);
                ToastUtil.showToast("最大支持MTU："+mtu);
            }
        });
    }else {
        ToastUtil.showToast("设备不支持MTU");
    }

```
[![Version](https://img.shields.io/badge/BleLib-v2.2.0-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.2.0)
```
修复连接多个设备在onChanged()回调中判断设备对象
    /*设置通知的回调*/
    private BleNotiftCallback<BleDevice> bleNotiftCallback =  new BleNotiftCallback<BleDevice>() {
        @Override
        public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            Log.e(TAG, "onChanged==uuid:" + uuid.toString());
            Log.e(TAG, "onChanged==address:"+ device.getBleAddress());
            Log.e(TAG, "onChanged==data:" + Arrays.toString(characteristic.getValue()));
        }
    };

```
[![Version](https://img.shields.io/badge/BleLib-v2.1.5-blue.svg)](https://bintray.com/superliu/maven/BleLib/2.1.5)
```
更新低版本手机(初始化问题)报错问题
```
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
compile 'cn.com.superLei:blelibrary:latestVersion'
```

#### 1.初始化蓝牙(判断设备是否支持BLE，蓝牙是否打开以及6.0动态授权蓝牙权限等)<br>

```
    private void initBle() {
        //方式1
        mBle = Ble.options()//开启配置
                .setLogBleEnable(true)//设置是否输出打印蓝牙日志
                .setThrowBleException(true)//设置是否抛出蓝牙异常
                .setLogTAG("AndroidBLE")//设置全局蓝牙操作日志TAG
                .setAutoConnect(true)//设置是否自动连接
                .setFilterScan(false)//设置是否过滤扫描到的设备
                .setConnectFailedRetryCount(3)
                .setConnectTimeout(10 * 1000)//设置连接超时时长（默认10*1000 ms）
                .setScanPeriod(12 * 1000)//设置扫描时长（默认10*1000 ms）
                .setUuidService(UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb"))//主服务的uuid
                .setUuidWriteCha(UUID.fromString("d44bc439-abfd-45a2-b575-925416129600"))//可写特征的uuid
                .create(getApplicationContext());

        或者
        //方式2  使用默认配置
        mBle = Ble.create(getApplicationContext());
        //方式3(同方式1)
        Ble.Options options = Ble.options()
                        .setLogBleEnable(true)
                        .setAutoConnect(true)...;
        mBle = Ble.create(getApplicationContext(), options);

        注意：若进行数据交互，必须进行配置uuid的各个值
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
            public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
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
### 四、工具类截图：

![](https://www.pgyer.com/image/view/app_screenshots/d72c86ea0d47a770d353219afe63f5b3-528)
![](https://www.pgyer.com/image/view/app_screenshots/02d4a22f7e19aea2dccc0791c5fcd85b-528)
![](https://www.pgyer.com/image/view/app_screenshots/ff9aae940594298cb5c77e8f2a0e7658-528)

### 五、如果你觉得不错，对你有过帮助，请给我一点打赏鼓励，支持我维护的动力：
![](https://upload-images.jianshu.io/upload_images/3884117-5d22ae84180a93ed.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/320)
![](https://upload-images.jianshu.io/upload_images/3884117-1f6c1c0fb5885252.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/320)
```
注：打赏后可留言/联系本人，提供您的需求以及建议，本人会进行定期的更新优化库的体验，多谢支持!
```








































