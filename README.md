# 对蓝牙4.0感兴趣可以加群进行相互讨论学习
# QQ群：494309361

### 作者：liulei
#### Android蓝牙BLE库

## 本文的api介绍：（BleLib库）
#### 先来看张BleLib库的api之间的关系图：
![BleLib库结构图.png](http://upload-images.jianshu.io/upload_images/3884117-2c5a0b95cda75158.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/840)
### 1、iQppCallback、QppApi
```
这个两个类封装了完整的读写数据，设置通知等操作   此demo中并未用到这两个接口，此列出方便以后调用。
```
### 2、BleDevice
```
该类的主要是来描述并记录蓝牙的属性和状态，如记录蓝牙名称、蓝牙MAC地址、蓝牙别名（即修改之后的名称）、蓝牙连接状态等。
```
### 3、BleConfig
```
该类是蓝牙配置类，里面包含了蓝牙服务的UUID、蓝牙特征的UUID、描述的UUID、以及蓝牙状态的静态常量值的标记等等，其中蓝牙相关的
UUID的设置是对外提供了接口的，用的时候可以自行传入特定的UUID即可。
```      
### 4、BleLisenter
``` 
该类提供了蓝牙各个状态的接口，此处做成了抽象类，目的是为了可以让用户有条件的去实现想要实现的方法，比如说客户想要在蓝牙扫描开
始的时候添加一些动画效果，那么你就可以实现onStart()方法，然后在其中做你想做的事情，默认是不需要实现的，如果你想要在蓝牙设备
返回数据时做出反应，那就去实现onRead()方法，如果你想在蓝牙连接失败或者超时的情况下去做特殊的处理，你就去实现onError()或者
onConnectTimeOut()方法等等。（如果各位有更好的方式可以留言提示，不胜感激）。
```
### 5、BluetoothLeService
```
该类是最重要的一个类，主要是蓝牙操作中用到的各个方法的实现类，是整个蓝牙的核心功能实现，BleManager是对外提供所有蓝牙操作接口的
管理类，当BluetoothLeService处理之后要把结果返回到BleManager中，然后再由BleManager对外提供接口。
```
### 6、BleManager
```
该类提供了几乎所有你需要用到的方法，包括蓝牙扫描、连接、断开、蓝牙当前连接状态等等，管理了蓝牙操作的所有接口和方法。
```

### 使用步骤：

#### AndroidStudio最新版本依赖地址：compile 'cn.com.superLei:blelibrary:2.0.0'
#### 1.初始化蓝牙(判断设备是否支持BLE，蓝牙是否打开以及6.0动态授权蓝牙权限等)<br>

```
 private void initBle() {
                try {
                    mManager = BleManager.getInstance(this);
                    mManager.registerBleListener(mLisenter);
                    boolean result = false;
                    if (mManager != null) {
                        result = mManager.startService();
                        if (!mManager.isBleEnable()) {//蓝牙未打开
                            mManager.turnOnBlueTooth(this);
                        } else {//已打开
                            requestPermission(new String[]{Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION},
                             getString(R.string.ask_permission), new GrantedResult() {
                                @Override
                                public void onResult(boolean granted) {
                                    if (!granted) {
                                        finish();
                                    } else {
                                        //开始扫描
                                        mManager.scanLeDevice(true);
                                    }
                                }
                            });
                        }
                    }
                    if (!result) {
                        Logger.e("服务绑定失败");
                        if (mManager != null) {
                            mManager.startService();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }       
```

#### 2.设置各种状态及结果的回调监听
```
mManager.registerBleListener(mLisenter);
```

#### 3.拿到各状态的回调结果
```
@Override
public void onStart() {
                 ...
                //代表开始扫描的回调方法
             }
 
             @Override
             public void onStop() {
                 ...
               //代表结束扫描的回调方法
             }
 
             @Override
             public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
                 ...
                 //代表扫描到设备的回调方法
             }
 
             @Override
             public void onReady(BluetoothDevice device) {
                   ...
                 //代表准备就绪，可以发送数据的回调方法
                 注：连接成功不代表可以立即发送数据（下面会讲解原因）
             }
 
              @Override
             public void onChanged(BluetoothGattCharacteristic characteristic) {
                 Logger.e("data===" + Arrays.toString(characteristic.getValue()));
                 //可以选择性实现该方法   不需要则不用实现
                 //代表mcu返回数据的回调方法
             }
             
             ...   
```

#### 4.Demo效果演示图：

![Demo预览图.gif](http://upload-images.jianshu.io/upload_images/3884117-49f080ad44b60946.gif?imageMogr2/auto-orient/strip)








































