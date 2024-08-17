# Activity的启动流程

## 一、从launcher创建app

博客地址：http://gityuan.com/2016/03/26/app-process-create/

### Launcher 启动 App 流程（Andorid 10）

*  `Launcher` 监听到点击app事件，调用 `Activity.startActivityForResult()` ,转到 `Instrumentation` 类的 `execStartActivity` 方法
*  `Instrumentation`  通过跨进程通信告诉 `AMS` 要启动应用的需求
*  `ATMS` 反馈 `Launcher` 让其进入 `Paused` 状态
*  随后 `ATMS` 转到 `ZygoteProcess` 类，通过 `socket` 与 `Zygote` 通信，告知 `Zygote` 需要新建进程
*  `Zygote fork` 进程，并调用 `ActivityThread` 的 `main` 方法，也就是app的入口
*  `ActivityThread` 创建 `ActivityThread` 实例，新建 `Looper` 实例，开始 `loop` 循环
*  同时 `ActivityThread` 告知 `AMS`，进程创建完毕，开始反射创建 `Application` `Provider`，并调用 `Application` 的 `attach` `onCreate` 的方法
*  最后创建上下文，反射创建 `Activity`，开始调用生命周期



### **Launcher 启动 App 流程（Andorid 9）**

*  `Launcher` 监听到点击app事件，调用 `Activity.startActivityForResult()` ,转到 `Instrumentation` 类的 `execStartActivity` 方法
*  `Instrumentation`  通过跨进程通信告诉 `AMS` 要启动应用的需求
*  `AMS` 反馈 `Launcher` 让其进入 `Paused` 状态
*  随后 `AMS` 转到 `ZygoteProcess` 类，通过 `socket` 与 `Zygote` 通信，告知 `Zygote` 需要新建进程
*  `Zygote fork` 进程，并调用 `ActivityThread` 的 `main` 方法，也就是app的入口
*  `ActivityThread` 创建 `ActivityThread` 实例，新建 `Looper` 实例，开始 `loop` 循环
*  同时 `ActivityThread` 告知 `AMS`，进程创建完毕，开始反射创建 `Application` ，并调用 `Application`的 `attachBaseConetxt`和`onCreate`的方法
*  最后创建上下文，反射创建 `Activity`，开始调用生命周期



ActivityThread.java

```java
private void handleBindApplication(AppBindData data) {
       
            .....
       		Application app;
    
    		......
             //
            app = data.info.makeApplication(data.restrictedBackupMode, null);

			//赋值全局变量
            mInitialApplication = app;

          	....
             //走Application的onCreate生命周期
             mInstrumentation.callApplicationOnCreate(app);
           ....
    }

private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
    ActivityInfo aInfo = r.activityInfo;
    ....

    ContextImpl appContext = createBaseContextForActivity(r);
    Activity activity = null;
    try {
        java.lang.ClassLoader cl = appContext.getClassLoader();
        activity = mInstrumentation.newActivity(
                cl, component.getClassName(), r.intent);
        ....
    

    try {
        //Application已在handleBindApplication()经构建好了
        Application app = r.packageInfo.makeApplication(false, mInstrumentation);

       	....
            activity.attach(appContext, this, getInstrumentation(), r.token,
                    r.ident, app, r.intent, r.activityInfo, title, r.parent,
                    r.embeddedID, r.lastNonConfigurationInstances, config,
                    r.referrer, r.voiceInteractor, window, r.configCallback);

            ....
            int theme = r.activityInfo.getThemeResource();
            if (theme != 0) {
                //设置主题
                activity.setTheme(theme);
            }
			//走Activity的OnCreate()
            if (r.isPersistable()) {
                mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);
            } else {
                mInstrumentation.callActivityOnCreate(activity, r.state);
            }
            

    return activity;
}
```



LoadedApk.java

```java
//创建Context和Application
public Application makeApplication(boolean forceDefaultAppClass,
        Instrumentation instrumentation) {
    .....

    Application app = null;
    
    String appClass = mApplicationInfo.className;
    if (forceDefaultAppClass || (appClass == null)) {
        appClass = "android.app.Application";
    }

    ....
        ContextImpl appContext = ContextImpl.createAppContext(mActivityThread, this);
        app = mActivityThread.mInstrumentation.newApplication(
                cl, appClass, appContext);
        appContext.setOuterContext(app);
	...
    mActivityThread.mAllApplications.add(app);
    mApplication = app;
    ...

    return app;
}
```

### Launcher 启动 APP 流程图：（Android 8 /API-27）

https://segmentfault.com/a/1190000019042206

![20190610223539273](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/Activity%E5%90%AF%E5%8A%A8%E6%B5%81%E7%A8%8B%E5%9B%BE.jpg)

```java
步骤1：当从桌面启动应用，则发起进程便是Launcher进程；Launcher进程的ActivityManagerProxy通过Binder发送消息给system_server进程的ActivityManagerService；
步骤2：在ActivityManagerService中调用Process.start()方法，通过socket向zygote进程发送创建新进程的请求；
步骤3：zygote进程成功fork新进程之后，通过反射执行了ActivityThread的main方法；
步骤4：ActivityThread.main() -> ActivityThread.attach() -> 
//ActivityManagerProxy不是真实存在，他是ApplicationThread继承于IApplicationThread.Stub，Binder类型，与AMS进行Binder通信；
//这一步把mAppThread（ApplicationThread）传到AMS，
ApplicationThread.attachApplication(mAppThread, startSeq) -> 
ActivityManagerService.attachApplication() -> ActivityManagerService.attachApplicationLocked() -> ApplicationThread.bindApplication()//new创建ContextImpl和反射创建了Application实例
步骤6、7、8：//这几步流程不适用
ActivityStackSupervisor.ScheduleLaunchActivity() -> ApplicationThread.ScheduleLaunchActivity() -> ApplicationThread.sendMessage() -> Instrumentation.CallActivityOnCreate() ->  Activity.performCreate() -> MyAcitivty.onCreate()
```





### 应用进程创建流程图：

![process-create](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/process-create.jpg)



#### system_server进程：

​	通过Process.start()方法发起创建新进程请求，会先收集各种新进程uid、gid、nice-name等相关的参数，然后通过socket通道发送给zygote进程；



#### zygote进程：

​	接收到system_server进程发送过来的参数后封装成Arguments对象，图中绿色框forkAndSpecialize()方法是进程创建过程中最为核心的一个环节，其具体工作是依次执行下面的3个方法：

- preFork()：先停止Zygote的4个Daemon子线程（java堆内存整理线程、对线下引用队列线程、析构线程以及监控线程）的运行以及初始化gc堆；

- nativeForkAndSpecialize()：调用linux的fork()出新进程，创建Java堆处理的线程池，重置gc性能数据，设置进程的信号处理函数，启动JDWP线程；

- postForkCommon()：在启动之前被暂停的4个Daemon子线程。

  

#### 新进程：

​	进入handleChildProc()方法，设置进程名，打开binder驱动，启动新的binder线程；然后设置art虚拟机参数，再反射调用目标类的main()方法，即ActivityThread.main()方法。

### 超详代码时序图：(Android 8 /API-27）

![超详细activity启动时序图](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/%E8%B6%85%E8%AF%A6%E7%BB%86activity%E5%90%AF%E5%8A%A8%E6%97%B6%E5%BA%8F%E5%9B%BE.png)





## 二、启动第二个Activity(Android 8)

https://blog.csdn.net/xgq330409675/article/details/78938926

用文字表达：

1、`App`的`MainActivity`调用`startActivity()`时，委托`Instrumentation`通知`AMS`需要创建`Activity`；

2、`AMS`通过`Binder`告诉`ActivityThread`，`MainActivity`需要走onPause生命周期，`ActivityThread`委托`Instrumentation`执行`Acitivty`的`onPause`；

3、ActivityThread通知AMS已经完成；

4、AMS继续走创建

```java
代码流程：
1.Activity.startActivity()->startActivityForResult()->
2.Instrumentation.execStartActivity()->ActivityManager.getService().startActivity()->//Binder通信

3.ActivityManagerService.startActivity()->startActivityAsUser()->

4.ActivityStartController.obtainStarter()->

5.ActivityStarter.execute()->startActivityMayWait()-> startActivityLocked(IApplicationThread..)->startActivityUnchecked()
6.ActivityStackSupervisor.resumeFocusedStackTopActivityLocked()->
7.ActivityStack.resumeTopActivityUncheckedLocked()-> resumeTopActivityInnerLocked()->startPausingLocked()
8.ApplicationThreadProxy->schedulePauseActivity()->//Binder通信
9.ApplicationThread->schedulePauseActivity()->
10.ActivityThread->handlePauseActivity()->
11.ActivityManageProxy.activityPaused()->//Binder通信
12.ActivityManagerService.activityPaused()->
13.ActivityStack.activityPausedLocked()->completePauseLocked()->//完成对发起方的Pause生命周期

14.ActivityStackSupervisor.resumeFocusedStackTopActivityLocked()->
15.ActivityStack.resumeTopActivityUncheckedLocked() -> resumeTopActivityInnerLocked() -> 16.ActivityStackSupervisor.startSpecificActivityLocked()->realStartActivityLocked()->
17.ApplicationThreadProxy.scheduleLaunchActivity()->//Binder通信
18.ApplicationThread.scheduleLaunchActivity()->
19.ActivityThread->handleLaunchActivity()->performLaunchActivity()
```



一个进程创建第二个Activity时的差别，请看下方startSpecificActivityLocked（）中的判断 if (app != null && app.thread != null)

ActivityStackSupervisor.java

```java
//启动Activity必经之路
void startSpecificActivityLocked(ActivityRecord r,
        boolean andResume, boolean checkConfig) {
    // Is this activity's application already running?
    ProcessRecord app = mService.getProcessRecordLocked(r.processName,
            r.info.applicationInfo.uid, true);

    r.getStack().setLaunchTime(r);
    //判断APP进程是否存在，如果这是第二次创建activity，那么app!=null成立
    if (app != null && app.thread != null) {
        try {
            if ((r.info.flags&ActivityInfo.FLAG_MULTIPROCESS) == 0
                    || !"android".equals(r.info.packageName)) {
                // Don't add this if it is a platform component that is marked
                // to run in multiple processes, because this is actually
                // part of the framework so doesn't make sense to track as a
                // separate apk in the process.
                app.addPackage(r.info.packageName, r.info.applicationInfo.versionCode,
                        mService.mProcessStats);
            }
            //创建第二个activity，并且return，防止执行mService.startProcessLocked创建进程
            realStartActivityLocked(r, app, andResume, checkConfig);
            return;
        } catch (RemoteException e) {
            Slog.w(TAG, "Exception when starting activity "
                    + r.intent.getComponent().flattenToShortString(), e);
        }

        // If a dead object exception was thrown -- fall through to
        // restart the application.
    }

    mService.startProcessLocked(r.processName, r.info.applicationInfo, true, 0,
            "activity", r.intent.getComponent(), false, false, true);
}
```



## 三、启动第二个Activity(Android 11)

https://cloud.tencent.com/developer/article/1760292?from=article.detail.1761888

https://cloud.tencent.com/developer/article/1761888

https://cloud.tencent.com/developer/article/1761889

1、ClientLifecycleManager.schedule()->

2、IApplicationThread.scheduleTransaction()->ActivityThread.scheduleTransaction()->

3、ActivityThread 的父类 ClientTransactionHandler.scheduleTransaction()->

```java
 void scheduleTransaction(ClientTransaction transaction) {
        transaction.preExecute(this);
     	//最终用ActivityThread的内部类H发送消息
        sendMessage(ActivityThread.H.EXECUTE_TRANSACTION, transaction);
    }
```

4、ActivityThread 的内部类 class H extends Handler

```javascript
public void handleMessage(Message msg) {
            if (DEBUG_MESSAGES) Slog.v(TAG, ">>> handling: " + codeToString(msg.what));
            switch (msg.what) {
      
                case EXECUTE_TRANSACTION:
                    final ClientTransaction transaction = (ClientTransaction) msg.obj;
                    mTransactionExecutor.execute(transaction);
                   ... ... 
                    break;
```

5、TransactionExecutor.execute()

```java
public void execute(ClientTransaction transaction) {
    if (DEBUG_RESOLVER) Slog.d(TAG, tId(transaction) + "Start resolving transaction");

    ...
    //创建Activity
    executeCallbacks(transaction);
    //走Activity生命周期
    executeLifecycleState(transaction);
    ...
}

private void executeLifecycleState(ClientTransaction transaction) {
    	//ActivityLifecycleItem继承自ClientTransctionItem，主要的子类有DestoryActivityItem、PauseActivityItem、StopActivityItem、ResumeActivityItem
    	//可参考：https://blog.csdn.net/weixin_42695485/article/details/108741913
        final ActivityLifecycleItem lifecycleItem = transaction.getLifecycleStateRequest();
        ...
        //走生命周期，参考：https://blog.csdn.net/u011386173/article/details/87857602
        // Cycle to the state right before the final requested state.
        cycleToPath(r, lifecycleItem.getTargetState(), true /* excludeLastState */, transaction);
		//走生命周期
        // Execute the final transition with proper parameters.
        lifecycleItem.execute(mTransactionHandler, token, mPendingActions);
        lifecycleItem.postExecute(mTransactionHandler, token, mPendingActions);
    }
```

TransactionExecutor.executeCallbacks() //取Binder传过来的ClientTransaction的callbacks

```java
public void executeCallbacks(ClientTransaction transaction) {
        final List<ClientTransactionItem> callbacks = transaction.getCallbacks();
        ...
        for (int i = 0; i < size; ++i) {
            //item是LaunchActivityItem，在ActivityStackSupervisor.realStartActivityLocked()内添加LaunchActivityItem，另外还有lifecycleItem
            //clientTransaction.addCallback(LaunchActivityItem.obtain(new Intent(r.intent),......
            //clientTransaction.setLifecycleStateRequest(lifecycleItem);
            final ClientTransactionItem item = callbacks.get(i);
           ...
            //mTransactionHandler是ActivityThread
            item.execute(mTransactionHandler, token, mPendingActions);
            item.postExecute(mTransactionHandler, token, mPendingActions);
            .....
        }
    }
```

6、LaunchActivityItem.execute()

```java
@Override
public void execute(ClientTransactionHandler client, IBinder token,
        PendingTransactionActions pendingActions) {
    Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
    ActivityClientRecord r = new ActivityClientRecord(token, mIntent, mIdent, mInfo,
            mOverrideConfig, mCompatInfo, mReferrer, mVoiceInteractor, mState, mPersistentState,
            mPendingResults, mPendingNewIntents, mIsForward,
            mProfilerInfo, client, mAssistToken, mFixedRotationAdjustments);
    client.handleLaunchActivity(r, pendingActions, null /* customIntent */);
    Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
}
```

7、ActivityThread.handleLaunchActivity()

```javascript
 @Override
    public Activity handleLaunchActivity(ActivityClientRecord r,
            PendingTransactionActions pendingActions, Intent customIntent) {
        ....
        //初始化WindowManagerService
        WindowManagerGlobal.initialize();
        //构建Activity
        final Activity a = performLaunchActivity(r, customIntent);
		...
		...
        return a;
    }
```

8、performLaunchActivity()

```java
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
    ....
        activity = mInstrumentation.newActivity(
                    cl, component.getClassName(), r.intent);
    ....
            // Activity resources must be initialized with the same loaders as the
            // application context.
            appContext.getResources().addLoaders(
                    app.getResources().getLoaders().toArray(new ResourcesLoader[0]));

            appContext.setOuterContext(activity);
            //创建PhoneWindow，执行attachBaseContext()
            activity.attach(appContext, this, getInstrumentation(), r.token,
                    r.ident, app, r.intent, r.activityInfo, title, r.parent,
                    r.embeddedID, r.lastNonConfigurationInstances, config,
                    r.referrer, r.voiceInteractor, window, r.configCallback,
                    r.assistToken);

            ...
            int theme = r.activityInfo.getThemeResource();
            if (theme != 0) {
                //设置主题
                activity.setTheme(theme);
            }

            activity.mCalled = false;
            if (r.isPersistable()) {
                //走两参数的onCreate方法
                mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);
            } else {
                //走一参数的onCreate方法
                mInstrumentation.callActivityOnCreate(activity, r.state);
            }
            .....

    return activity;
}
```

## 问题

1. `ActivityManagerService` 与 `ActivityTaskManagerService` 的区别?
   * 原本四大组件的通信都是 `AMS` 来处理，后期 `AMS` 过于臃肿，将 `Activity` 相关工作转移到 `ATMS` 中
2. 怎么判断应用进程存在？
   * 如果进程存在，`ATMS` 里面会保存有 `WindowProcessController` 信息，这个信息包括 `processName` 和 `uid`，`uid` 则是应用程序的 `id`，可以通过 `applicationInfo.uid` 获取。所以判断进程是否存在，根据 `processName` 和 `uid` 去判断是否有对应的 `WindowProcessController`，并且 `WindowProcessController` 里面的线程不为空。
3. 怎么创建进程，为什么要通过 socket 进行 IPC 通信而不是 binder？
   * 通过 `socket` 与 `Zygote` 进行通信，然后 `Zygote` 进行 `fork` 进程并返回新进程的 `pid`
   * fork() 方法是类 Unix 操作系统上创建进程的主要方法，用于创建当前进程的副本
   * 因为 binder 是多线程交互，而 fork 不允许存在多线程，原因是会出现死锁，例如主进程里的A线程持有锁后，fork 出的子进程同样将锁 fork 出来了，当子进程的线程需要用到该锁，那么就会出现死锁

**`ZygoteInit.java` 里做的业务：**

```java
public static main(String argv[]) {
	// 1. 预加载 frameworks/base/preloaded-classes 和 framework_res.apk 等系统资源
    // 应用在启动时需要大量调用系统资源，这里预先调用可以提高应用响应速度
    preloadClasses();
    preloadResources();
    preloadSharedLibraries();
    
    // 2. 启动 system_server 进程。该进程是 framework 核心
    if(argv[1].equals("start-system-server")) {
        startSystemServer();
    }
    
    // 3. 创建 Socket 服务
    registerZygoteSocket();
    
    // 4. 进入阻塞状态，等待连接，用以处理来自 AMS 申请进程创建的请求
    runSelectLoopMode();
}
```

**`SystemServer.java` 里做的业务：**

```java
public static void main(String argv[]) {
    // 创建系统服务管理者
    SystemServiceManager mSystemServiceManager = new SystemServiceManager(mSystemContext);
    // 启动引导服务
    startBootstrapServices(t);
    // 启动核心服务
    startCoreServices(t);
    // 启动其它一般服务
    startOtherServices(t);
    // 当所有服务启动完毕会调用ActivityManagerService.systemReady来启动launcher应用
    mActivityManagerService.systemReady()
}
```

#### 

