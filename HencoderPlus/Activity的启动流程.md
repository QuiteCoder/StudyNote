# Activity的启动流程

博客地址：http://gityuan.com/2016/03/26/app-process-create/

## APP启动流程图：

![20190610223539273](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/Activity%E5%90%AF%E5%8A%A8%E6%B5%81%E7%A8%8B%E5%9B%BE.jpg)

```java
步骤1：当从桌面启动应用，则发起进程便是Launcher进程；Launcher进程的ActivityManagerProxy通过binder发送消息给system_server进程的ActivityManagerService；
步骤2：在ActivityManagerService中调用Process.start()方法，通过socket向zygote进程发送创建新进程的请求；
步骤3：zygote进程成功fork新进程之后，通过反射执行了ActivityThread的main方法；
步骤4：ActivityThread.main() -> ActivityThread.attach() -> 
//ActivityManagerProxy不是真实存在，它是aidl的IActivityManager类型；
//这一步把mAppThread（ApplicationThread）传到ActivityManagerService，
ActivityManagerProxy.attachApplication(mAppThread, startSeq) -> 
ActivityManagerService.attachApplication() -> ActivityManagerService.attachApplicationLocked() 
步骤6、7、8：
ActivityStackSupervisor.ScheduleLaunchActivity() -> ApplicationThreadProxy.ScheduleLaunchActivity() -> ApplicationThread.sendMessage() -> Instrumentation.CallActivityOnCreate() ->  Activity.performCreate() -> MyAcitivty.onCreate()
```





## 应用进程创建流程图：

![process-create](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/process-create.jpg)



## system_server进程：

​	通过Process.start()方法发起创建新进程请求，会先收集各种新进程uid、gid、nice-name等相关的参数，然后通过socket通道发送给zygote进程；



## zygote进程：

​	接收到system_server进程发送过来的参数后封装成Arguments对象，图中绿色框forkAndSpecialize()方法是进程创建过程中最为核心的一个环节，其具体工作是依次执行下面的3个方法：

- preFork()：先停止Zygote的4个Daemon子线程（java堆内存整理线程、对线下引用队列线程、析构线程以及监控线程）的运行以及初始化gc堆；

- nativeForkAndSpecialize()：调用linux的fork()出新进程，创建Java堆处理的线程池，重置gc性能数据，设置进程的信号处理函数，启动JDWP线程；

- postForkCommon()：在启动之前被暂停的4个Daemon子线程。

  

## 新进程：

​	进入handleChildProc()方法，设置进程名，打开binder驱动，启动新的binder线程；然后设置art虚拟机参数，再反射调用目标类的main()方法，即ActivityThread.main()方法。

## 超详代码时序图：

![超详细activity启动时序图](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/%E8%B6%85%E8%AF%A6%E7%BB%86activity%E5%90%AF%E5%8A%A8%E6%97%B6%E5%BA%8F%E5%9B%BE.png)