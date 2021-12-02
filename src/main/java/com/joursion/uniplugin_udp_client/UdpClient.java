package com.joursion.uniplugin_udp_client;

import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.bridge.JSCallback;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class UdpClient extends UniModule {
    private DatagramSocket socket = null;
    private ResponseListenerThread responseListenerThread = null;
    private boolean isDebug = false;

    // region callback
    private JSCallback onErrorCallback; // 错误回调
    // endregion

    @UniJSMethod(uiThread = false)
    public void init(int listenPort, UniJSCallback messageCallback, UniJSCallback errorCallback) {
        print("init listenPort: " + listenPort);
        onErrorCallback = errorCallback;

        if (messageCallback == null || errorCallback == null) {
            throwError("messageCallback or errorCallback undefined");
            return;
        }
        try {
            socket = new DatagramSocket();
        } catch (IOException e) {
            throwError(e.getMessage());
        }
        responseListenerThread = new ResponseListenerThread(listenPort, messageCallback, onErrorCallback);
        responseListenerThread.start();
    }

    @UniJSMethod(uiThread = false)
    public void send(JSONObject object) {
        if (socket == null) {
            throwError("未初始化，请先调用 init");
            return;
        }
        int port = GetValue(object, "port", -1);
        if (port == -1) {
            throwError("未指定 port");
            return;
        }
        String msg = GetValue(object, "data", "");
        // 若没有指定 ip，255.255.255.255 默认为广播
        String host = GetValue(object, "host", "255.255.255.255");

        new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName(host);
                byte[] buf = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
            } catch (IOException e) {
                throwError(e.getMessage());
            }
        }).start();
    }

    @UniJSMethod(uiThread = false)
    public boolean isConnected() {
        return socket.isConnected();
    }

    @UniJSMethod(uiThread = false)
    public void release() {
        if (socket != null) {
            socket.disconnect();
        }
        socket = null;
        if (responseListenerThread != null) {
            responseListenerThread.exit();
        }
        responseListenerThread = null;
    }

    // region listenerThread
    private static class ResponseListenerThread extends Thread {
        private final int port;
        private final JSCallback messageCallback;
        private final JSCallback errorCallback;
        private boolean isFinish = false;
        DatagramSocket socket;
        public ResponseListenerThread(int port, JSCallback messageCallback, JSCallback errorCallback) {
            this.port = port;
            this.messageCallback = messageCallback;
            this.errorCallback = errorCallback;
        }
        @Override
        public void run() {
            super.run();
            try {
                socket = new DatagramSocket(port);
                while(!isFinish) {
                    byte[] buf = new byte[512];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String host = packet.getAddress().getHostAddress();
                    int port = packet.getPort();
                    int length = packet.getLength();
                    String data = new String(buf, 0, length);

                    JSONObject res = new JSONObject();
                    res.put("host", host);
                    res.put("port", port);
                    res.put("data", data);
                    if (this.messageCallback != null) {
                        this.messageCallback.invokeAndKeepAlive(res);
                    }
                }
            } catch (Exception e) {
                if (this.errorCallback != null) {
                    this.errorCallback.invokeAndKeepAlive(e.getMessage());
                }
            } finally {
                exit();
            }
        }
        public void exit() {
            isFinish = true;
            if (socket != null){
                socket.close();
                socket = null;
            }
        }
    }
    // endregion

    private void throwError(String msg) {
        if (onErrorCallback != null) {
            onErrorCallback.invokeAndKeepAlive(msg);
        }
        print(msg);
    }

    @UniJSMethod()
    public void setIsDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    // region common
    private void print(String message){
        if (isDebug) {
            System.out.println("UdpClient : " + message);
        }
    }

    private int GetValue(JSONObject object, String key, int defaultValue){
        object = object == null?new JSONObject():object;
        return object.containsKey(key)?object.getInteger(key):defaultValue;
    }

    private String GetValue(JSONObject object, String key, String defaultValue){
        object = object == null?new JSONObject():object;
        return object.containsKey(key)?object.getString(key):defaultValue;
    }
    // endregion
}
