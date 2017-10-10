/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.com.heaton.blelibrary.spp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by LiuLei on 2017/9/14.
 * 这个类负责管理蓝牙设备和其他设备的初始化和连接操作
 * 一个线程用来监听连接进入
 * 一个线程用来监听连接请求
 * 一个线程用来传输数据.
 */
public class BluetoothSPPService implements Handler.Callback
{
    // 调试
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // SDP记录名称
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // 通用UUID用于这个应用.默认采用加密的UUID.未加密的可能是错误的
    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //fa87c0d0-afac-11de-8a39-0800200c9a66
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//8ce255c0-200a-11e0-ac64-0800200c9a66

    // 成员变量
    private final BluetoothAdapter mAdapter;
    private final Context mContext;

    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private BluetoothDeviceListener mBluetoothDeviceListener;
    private Handler mHandler;
    private int mState;

    // 常量,显示当前的连接状态
    public static final int STATE_NONE = 0;       // 什么都不做
    public static final int STATE_LISTEN = 1;     // 现在监听传入的连接
    public static final int STATE_CONNECTING = 2; // 现在即将开始连接
    public static final int STATE_CONNECTED = 3;  // 已连接到一个远程设备


    private static final int MESSAGE_STATE_CHANGE = 0x11;
    private static final int MESSAGE_DEVICE_NAME = 0x12;
    private static final int MESSAGE_READ = 0x13;
    private static final int MESSAGE_WRITE = 0x14;
    private static final int MESSAGE_ERROR = 0x15;
    /**
     * 构造函数。准备一个新的BluetoothChat会话.
     * @param context  UI活动背景
     */
    public BluetoothSPPService(Context context, BluetoothDeviceListener bluetoothDeviceListener) {
        mContext = context;
        mHandler = new Handler(mContext.getMainLooper(),this);
        mAdapter = BluetoothAdapter.getDefaultAdapter();    //获取蓝牙Adapter
        mState = STATE_NONE;                                //设置状态
        mBluetoothDeviceListener = bluetoothDeviceListener;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if(msg == null){
            return false;
        }
        switch(msg.what){
            case MESSAGE_STATE_CHANGE:
                if(mBluetoothDeviceListener != null){
                    mBluetoothDeviceListener.onStateChanged(msg.arg1);
                }
                break;
            case MESSAGE_DEVICE_NAME:
                if(mBluetoothDeviceListener != null){
                    if(msg.obj != null && msg.obj instanceof String){
                        BluetoothDevice device = mAdapter.getRemoteDevice((String) msg.obj);
                        mBluetoothDeviceListener.onConnected(device);
                    }
                }
                break;
            case MESSAGE_READ:
                if(mBluetoothDeviceListener != null){
                    mBluetoothDeviceListener.onRead((byte[]) msg.obj);
                }
                break;
            case MESSAGE_WRITE:
                if(mBluetoothDeviceListener != null){
                    mBluetoothDeviceListener.onWrite((byte[]) msg.obj);
                }
                break;
            case MESSAGE_ERROR:
                if(mBluetoothDeviceListener != null){
                    mBluetoothDeviceListener.onError(String.valueOf(msg.obj));
                }
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * 设置当前的状态
     * @param state  一个整形变量用来标志当前的状态
     */
    private synchronized void setState(int state) {
        if (D)
            Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        // 将新状态发给句柄.通知UI更新
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, 0).sendToTarget();
    }

    /**
     * 返回当前的连接状态. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * 启动服务. 启动AcciceptThread 启动一个任务来监听在服务模式
     * Activity onResume()之后调用 */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // 如果请求链接线程已经存在.先取消
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // 当前已经链接
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        //设置状态为监听
        setState(STATE_LISTEN);

        // 启动线程BluetoothServerSocket听
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /**
     * 启动ConnectThread发起一个连接到远程设备.
     * @param device  需要链接的设备
     * @param secure Socket 安全类型 - 安全 (true) , 不安全 (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure)
    {
        if (D) Log.d(TAG, "connect to: " + device);

        // 取消任何线程试图建立连接
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // 取消任何线程正在运行一个连接
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // 启动线程与给定的设备
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        //更新状态
        setState(STATE_CONNECTING);
    }

    /**
     * 启动ConnectedThread开始管理一个蓝牙连接
     * @param socket  链接socket
     * @param device  已经链接的蓝牙设备
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

        // 完成连接之后取消链接的线程
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // 取消任何线程正在运行一个连接
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // 取消接受线程,因为我们只希望连接到一个设备
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // 启动线程管理连接和执行传输
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // 连接设备的名称发送回UI的活动
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME, device.getAddress());
        mHandler.sendMessage(msg);

        //更新链接状态
        setState(STATE_CONNECTED);
    }

    /**
     * 停止所有线程
     */
    public synchronized void stop()
    {

        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        //更新状态
        setState(STATE_NONE);
    }

    /**
     * 用非同步的方式写入到 ConnectedThread
     * @param out 字节写
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // 创建临时对象
        ConnectedThread r;
        // 同步一份拷贝
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // 执行未同步的写操作
        r.write(out);
    }

    /**
     * 表明,该连接请求失败并通知UI的活动。
     */
    private void connectionFailed()
    {
        // 发送链接失败返回给UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_ERROR,"无法连接到设备");
        mHandler.sendMessage(msg);
        // 进入listen模式
        BluetoothSPPService.this.start();
    }

    /**
     * 指示连接断开.通知UI更新显示.
     */
    private void connectionLost() {
        // 发送一个失败消息返回给Activity
        Message msg = mHandler.obtainMessage(MESSAGE_ERROR,"设备连接已断开");
        mHandler.sendMessage(msg);

        // 在重新启动监听模式启动该服务
        BluetoothSPPService.this.start();
    }

    /**
     * 这个线程运行,监听传入的连接. 他像一个服务器端的客户端,当连接建立时.会进入运行状态
     * (或者是.直到取消).
     */
    private class AcceptThread extends Thread
    {
        // 本地Server Socket
        private final BluetoothServerSocket mmServerSocket;

        //socket的类型
        private String mSocketType;

        //构造函数
        public AcceptThread(boolean secure)
        {
            //Tmp变量
            BluetoothServerSocket tmp = null;

            //初始化类型
            mSocketType = secure ? "Secure":"Insecure";

            // 创建一个服务的Listener
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        //进入线程的While
        public void run()
        {
            if (D) Log.d(TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            //
            BluetoothSocket socket = null;

            // 如果没有进入已链接状态的话.会持续的进入此操作
            while (mState != STATE_CONNECTED)
            {
                try {
                    // 这是一个阻塞的链接.除非链接完成.或者是发生异常
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }
                // 如果一个连接被接受
                if (socket != null)
                {
                    synchronized (BluetoothSPPService.this)
                    {
                        switch (mState)
                        {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // 状态正常.开始连接线程。
                            connected(socket, socket.getRemoteDevice(), mSocketType);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // 没有准备好或已经连接。终止新的套接字
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }
        //取消此线程
        public void cancel() {
            if (D) Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * 这个线程运行而试图使一个外向与设备连接. 它会持续到;链接成功或者是失败.
     */
    private class ConnectThread extends Thread
    {
        //socket
        private final BluetoothSocket mmSocket;
        //远程设备
        private final BluetoothDevice mmDevice;
        //socket 类型
        private String mSocketType;

        //构造函数
        public ConnectThread(BluetoothDevice device, boolean secure)
        {
            //链接
            mmDevice = device;
            //
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run()
        {
            //socket类型
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);

            //设置名字
            setName("ConnectThread" + mSocketType);

            // 取消扫描.因为会影响设备的链接速度
            mAdapter.cancelDiscovery();

            // 链接
            try {
                // 这是一个阻塞调用,只返回一个成功连接上或发生异常
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                //处理消息
                connectionFailed();
                return;
            }

            // 重置ConnectThread
            synchronized (BluetoothSPPService.this) {
                mConnectThread = null;
            }

            // 开始连接线程
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * 这个线程运行在一个远程设备的连接.
     * 它处理所有传入和传出的传输.
     */
    private class ConnectedThread extends Thread
    {
        //socket
        private final BluetoothSocket mmSocket;
        //输入流
        private final InputStream mmInStream;
        //输出流
        private final OutputStream mmOutStream;
        //
        public ConnectedThread(BluetoothSocket socket, String socketType)
        {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // 获取输入输出流
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            Log.i(TAG, "BEGIN mConnectedThread");
            //创建缓冲区
            byte[] buffer = new byte[1024];
            int bytes;

            // 持续读取输入流
            while (true) {
                try {
                    // 输入流
                    bytes = mmInStream.read(buffer);
                    byte[] b;
                    if(bytes <0 ){
                        b = new byte[0];
                    }else{
                        b = new byte[bytes];
                        System.arraycopy(buffer,0,b,0,bytes);
                    }

                    // 发送到UI
                    mHandler.obtainMessage(MESSAGE_READ, b).sendToTarget();
                } catch (IOException e) {

                    //链接断开
                    Log.e(TAG, "disconnected", e);

                    //链接丢失
                    connectionLost();

                    // 启动服务在监听模式
                    BluetoothSPPService.this.start();
                    break;
                }
            }
        }

        /**
         * 写入到输出流
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // 发送消息到UI
                mHandler.obtainMessage(MESSAGE_WRITE, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        //取消线程
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public interface  BluetoothDeviceListener{
        public void onStateChanged(int state);
        public void onConnected(BluetoothDevice device);
        public void onError(String msg);
        public void onRead(byte[] buffer);
        public void onWrite(byte[] buffer);
    }
}
