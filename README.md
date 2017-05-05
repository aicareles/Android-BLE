# BleDemo
## android BLE蓝牙项目<br>
### 作者：liulei<br>
#### android蓝牙BLE库<br>

 # BLE4.0蓝牙见解：api解读<br>
 # 一、了解api及概念

 ## 1.1 BluetoothGatt<br>
 继承BluetoothProfile，通过BluetoothGatt可以连接设备（connect）,发现服务（discoverServices），并把相应地属性返回到BluetoothGattCallback
 ## 1.2 BluetoothGattCharacteristic<br>
 相当于一个数据类型，它包括一个value和0~n个value的描述（BluetoothGattDescriptor）
 ## 1.3 BluetoothGattDescriptor<br>
 描述符，对Characteristic的描述，包括范围、计量单位等
 ## 1.4 BluetoothGattService<br>
 服务，Characteristic的集合。
 ## 1.5 BluetoothProfile<br>
  一个通用的规范，按照这个规范来收发数据。
 ## 1.6 BluetoothManager<br>
  通过BluetoothManager来获取BluetoothAdapter
 BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
 ## 1.7 BluetoothAdapter<br>
 一个Android系统只有一个BluetoothAdapter ，通过BluetoothManager 获取
 BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
 ## 1.8 BluetoothGattCallback<br>
 已经连接上设备，对设备的某些操作后返回的结果。这里必须提醒下，已经连接上设备后的才可以返回，没有返回的认真看看有没有连接上设备。<br>
 private BluetoothGattCallback GattCallback = new BluetoothGattCallback() {
     // 这里有9个要实现的方法，看情况要实现那些，用到那些就实现那些
     public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){};
     public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){};
 };
 BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);<br>
 BluetoothGatt gatt = device.connectGatt(this, false, mGattCallback);<br>

 ## 1.8.1:notification对应onCharacteristicChanged;<br>
 gatt.setCharacteristicNotification(characteristic, true);

 ## 1.8.2:readCharacteristic对应onCharacteristicRead;<br>
 gatt.readCharacteristic(characteristic);

 ## 1.8.3:writeCharacteristic对应onCharacteristicWrite;<br>
 gatt.wirteCharacteristic(mCurrentcharacteristic);

 ## 1.8.4:连接蓝牙或者断开蓝牙 对应 onConnectionStateChange;<br>

 ## 1.8.5:readDescriptor对应onDescriptorRead;<br>

 ## 1.8.6:writeDescriptor对应onDescriptorWrite;<br>

 gatt.writeDescriptor(descriptor);

 ## 1.8.7:readRemoteRssi对应onReadRemoteRssi;<br>
 gatt.readRemoteRssi()

 ## 1.8.8:executeReliableWrite对应onReliableWriteCompleted;<br>

 ## 1.8.9:discoverServices对应onServicesDiscovered<br>
 gatt.discoverServices()

 ## 1.9:BluetoothDevice<br>
 扫描后发现可连接的设备，获取已经连接的设备

 # 二、开启蓝牙权限<br>
 <uses-permission android:name="android.permission.BLUETOOTH"/>
 <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
 <uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
 如果 android.hardware.bluetooth_le设置为false,可以安装在不支持的设备上使用，判断是否支持蓝牙4.0用以下代码就可以了
 if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
     Toast.makeText(this, "设备不支持蓝牙4.0", Toast.LENGTH_SHORT).show();
     finish();
 }
 # 三、对蓝牙的启动关闭操作<br>

 ## 1、利用系统默认开启蓝牙对话框<br>
 if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
     Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
     startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
 }
 ## 2、后台打开蓝牙，不做任何提示，这个也可以用来自定义打开蓝牙对话框啦<br>
 mBluetoothAdapter.enable();<br>
 ## 3、后台关闭蓝牙<br>
 mBluetoothAdapter.disable();<br>

--------------------------------------------******************************************************************************----------------------------------------------------<br>
## 本文的api介绍：（blelibrary库）<br>
### 1、iQppCallback和QppApi这个两个类封装了完整的读写数据，设置通知等操作   此demo中并未用到这两个接口，此列出方便以后调用<br>
### 2、BleDevice类为蓝牙对象，其中可以设置蓝牙的基本属性，以及连接状态等（可以继承该类进行扩展）<br>
### 3、BleConfig类中主要是放置一些静态值，如连接超时时长、扫描时长、服务及特征的uuid，以及验证硬件发送的广播包以便进行过滤扫描到的设备<br>
### 4、BleLisenter包含了ble蓝牙操作的所有接口   如开始扫描、停止扫描、扫描到设备、获取到服务、读取硬件返回的数据、向硬件写入数据、设置通知、蓝牙连接改变、蓝牙连接出错（在四此处设置同时最多可连接多少设备）等回调<br>
### 5、BluetoothLeService实现所有的上述回调方法<br>

### 使用：见DEMO





































