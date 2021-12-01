# udp-client

## 

## 介绍

一个简单的支持安卓平台的，广播和接收 `udp` 数据的插件。

## 使用文档

### 引入

```javascript
const client = uni.require('udp-client');
```

### 初始化

```javascript
/**
 * 初始化
 * @param listenPort 监听的端口
 * @param (res) => {} 返回消息 res: {host: string, port: number, data: string}
 * @param (errMsg) => {} 异常消息
 */
client.init(listenPort, (res) => {

}, (errMsg) => {

});
```

### 发送消息

```javascript
/**
 * 发送消息
 */
client.send({
  data: String,
  port: number,
  host: String, // 可选，不传默认为广播 255.255.255.255
});


```

### 释放

```javascript
/**
 * 释放，拿到数据之后手动释放，不然会占用资源
 */
client.release();
```

### 设置是否打印日志

```javascript
client.setIsDebug(false);
```
