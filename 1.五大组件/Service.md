# 一、Service是什么？

不需要界面也可以长时间在后台运行的组件，但是不能做耗时的操作。



# 二、Service和Thread的区别？

## 1.定义

Service和Thread是两个完全没有任何关系的东西；

servie是系统的组件，它由系统进程托管（servicemanager）；它们之间的通信类似于client和server，是一种轻量级的ipc通信，这种通信的载体是binder，它是在linux层交换信息的一种ipc。而thread是由本应用程序托管。

如果给Service设定一个进程id，那就成为了Remote Service，不跟随APP进程存亡，是一个独立运行的进程。

Thread 是程序执行的最小单元，它是分配CPU的基本单位。可以用 Thread 来执行一些异步的操作。



## 2.使用场景

什么时候使用Service什么时候使用Thread呢 ?

两者可以同时使用，Service的使用场景主要是让一些业务即使不打开界面也要使用的时候，在后台默默运行，如果要做的业务是耗时操作，那还需要使用Thread在子线程操作，例如后台更新数据或者IO操作。

a.音乐播放器：后台播放音乐

b.视频播放器：后台播放视频

C.下载文件：后台下载

d.定时任务：执行定时任务，如定时备份数据

f.即时通讯：微信接收消息，需要后台维护长连接，接收和处理消息



## 3.启动方式

1).startService

```java
import android.content.Intent;
 
public class MyActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
 
        // 创建Intent对象，指定要启动的Service
        Intent intent = new Intent(this, MyService.class);
 
        // 启动Service
        startService(intent);
    }
}
```



2).bindService

这种方式一般在Activity中进行，发起绑定的是客户端，Service就是服务端，这种方式可以让CS之间有更多的交互，在Activity中可以对Service操作。

```java
private ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // 服务已连接，可以进行交互
    }
 
    @Override
    public void onServiceDisconnected(ComponentName name) {
        // 服务断开连接
    }
};


Intent intent = new Intent(this, MyService.class); // MyService是你想要启动的服务类
boolean isBound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
 
if (!isBound) {
    // 绑定服务失败的处理
}
```



