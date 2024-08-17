# BroadcastReceiver



## 一、定义

观察者模式

可以进程间通讯

底层是Binder实现的



## 二、实现方式

1.静态注册

AndroidManifest文件注册

特点：就算进程没有启动，也能接收到广播



2.动态注册

Activity或者Service中注册，通过registerReceiver接口注册

特点：生命周期跟随Activity或者Service



## 三、底层实现

1.通过Binder机制向AMS进行注册；

2.广播发送者通过Binder机制向AMS发送广播；

3.AMS查找符合相应条件的BroadcastReceiver，将广播发送到BroadcastReceiver相应的消息循环队列中；

4.消息循环执行拿到此广播，回调BroadcastReceiver中的onReceiver( )方法中。



## 四、LocalBroadcastManager（本地广播）

1.使用它发送的广播将只在自身app内传播，因此不必担心泄漏隐私数据；

2.其他app无法对你的app发送该广播，因为你的app根本不可能接收到非自身应用发送的该广播，因此你不必但系有安全漏洞；

3.比系统的全局广播更加高效；





