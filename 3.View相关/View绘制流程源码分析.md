#  View绘制流程源码分析

参考文章：https://blog.51cto.com/u_12228/10410097

## 背景 

为了研究activity的生命周期与view的measure、draw、layout的关联，和研究什么时候才会触发view的requestLayout方法，

因为掌握了这个知识点就可以用子线程更新UI了，已知View.requestLayout-->ViewRootImpl.checkThread才会报UI不能在子线程更新UI的Exception。

## 流程图

![image](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/View%E7%9A%84%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.png)



流程：

1、AMS通过binder告诉ActivityThread创建Activity

2、在performLaunchActivity()中委托Instrumentation通过反射构建出Activity的实例

3、然后Actiity实例先是调用了attach方法，构建出了PhoneWIndow

4、Actiity调用onCreate方法，开始setContentView()

5、执行PhoneWindow.installDecor()初始DecorView。注意：DecorView内部持有PhoneWindow和ViewRootImpl

6、Activity调用onResume方法，WindowManagerGlobal调用addView方法构建ViewRootImpl，并让其管理DecorView

7、ViewRootImpl.setView()，先执行requestLayout()，依次对view进行测量、布局、绘制，接着通过Bindler给WindowManagerSerivce.addWindow()







## ActivityThread、Activity

```java
//Activity的onCreate()堆栈
onCreate:19, MaterialEditTextActivity (com.hpf.customview.activity)
performCreate:7136, Activity (android.app)
performCreate:7127, Activity (android.app)
callActivityOnCreate:1271, Instrumentation (android.app)
performLaunchActivity:2893, ActivityThread (android.app)
handleLaunchActivity:3048, ActivityThread (android.app)
execute:78, LaunchActivityItem (android.app.servertransaction)
executeCallbacks:108, TransactionExecutor (android.app.servertransaction)
execute:68, TransactionExecutor (android.app.servertransaction)
handleMessage:1808, ActivityThread$H (android.app)
dispatchMessage:106, Handler (android.os)
loop:193, Looper (android.os)
main:6669, ActivityThread (android.app)
invoke:-1, Method (java.lang.reflect)
run:493, RuntimeInit$MethodAndArgsCaller (com.android.internal.os)
main:858, ZygoteInit (com.android.internal.os)
    
    
//Activity加载window流程
class ActivityThread {
    ...
    //1、构建activity的方法
    private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
        ...
       Activity activity = null;
       try {
           java.lang.ClassLoader cl = appContext.getClassLoader();
           activity = mInstrumentation.newActivity(cl, component.getClassName(), r.intent);
           ...
        } catch (Exception e) {}
        ...
        //Activity在此构建了Window，具体构建的是PhoneWindow
        activity.attach(appContext, this, getInstrumentation(), r.token,
                            r.ident, app, r.intent, r.activityInfo, title, r.parent,
                            r.embeddedID, r.lastNonConfigurationInstances, config,
                            r.referrer, r.voiceInteractor, window, r.configCallback,
                            r.assistToken);
        ...
        //调用activity的onCreate
        if (r.isPersistable()) {
            mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);
        } else {
            mInstrumentation.callActivityOnCreate(activity, r.state);
        }
        ...
        return activity;
    }
}

class Activity {
    ...
    final void attach() {
        ...
        //构建PhotoWindow
        mWindow = new PhoneWindow(this, window, activityConfigCallback);
    }
    
    //2、当在Activity的OnCreate()调用setContentView()实质上是调用了PhoneWindow的
    public void setContentView(@LayoutRes int layoutResID) {
        getWindow().setContentView(layoutResID);
        initWindowDecorActionBar();
    }
}

class PhoneWindow {
    @Override
    public void setContentView(int layoutResID) {
        ...
        if (mContentParent == null) {
            //3、初始化DecorView，它是继承自FrameLayout
            installDecor();
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            mContentParent.removeAllViews();
        }
    }
    
    /*
    1、generateDecor()就是new了一个DecorView对象，然后配置了一些stlyable
    2、generateLayout() 首先去配置window的一些styleable，例如window Flags,例如窗口的navigationBar、statusBar是否透明,是否全屏等等...
    再是配置DecorView的具体使用的layout文件： 
    layout文件会根据使用主题的不同选择不一样的layout文件，例如有actionBar的就选R.layout.screen_action_bar
        mDecor.onResourcesLoaded(mLayoutInflater, layoutResource);
    **/
    private void installDecor() {
        mForceDecorInstall = false;
        if (mDecor == null) {
            mDecor = generateDecor(-1);
            ...
        }
        ...
        if (mContentParent == null) {
            mContentParent = generateLayout(mDecor);
            ...
        }
    }
}


/**********************分割线***********************/


//Activity的onStart()堆栈，此时界面可见，但不能触控
onStart:35, MaterialEditTextActivity (com.hpf.customview.activity)
callActivityOnStart:1391, Instrumentation (android.app)
performStart:7157, Activity (android.app)
handleStartActivity:2937, ActivityThread (android.app)
....


//Activity的onResume()堆栈
onResume:39, MaterialEditTextActivity (com.hpf.customview.activity)
callActivityOnResume:1412, Instrumentation (android.app)
performResume:7292, Activity (android.app)
performResumeActivity:3776, ActivityThread (android.app)
handleResumeActivity:3816, ActivityThread (android.app)
....
//4、在Activity的onResume时，viewRootImpl开始对layout进行measure、layout、draw
class ActivityThread {
    @Override
    public void handleResumeActivity(...) {
        //a是一个activity对象，每个activity都有一个WindowManager,跟踪到Activity的源码发现，
        //这个wm其实是WindowManagerImpl，其内部的addView方法是调用了单例对象WindowManagerGlobal的addView方法
        ViewManager wm = a.getWindowManager();
        ...
        wm.addView(decor, l);
        ...
    }
}

class WindowManagerGlobal {
    //WindowManagerGlobal的addViwe方法
    public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow) {
        // 每一个window对应一个ViewRootImpl，ViewRootImpl管理View的一切活动
        root = new ViewRootImpl(view.getContext(), display);
        ...
        root.setView(view, wparams, panelParentView);
        ...
    }
    
    @UnsupportedAppUsage
    public static IWindowSession getWindowSession() {
        synchronized (WindowManagerGlobal.class) {
            if (sWindowSession == null) {
                try {
                    // Emulate the legacy behavior.  The global instance of InputMethodManager
                    // was instantiated here.
                    // TODO(b/116157766): Remove this hack after cleaning up @UnsupportedAppUsage
                    InputMethodManager.ensureDefaultInstanceForDefaultDisplayIfNecessary();
                    IWindowManager windowManager = getWindowManagerService();
                    sWindowSession = windowManager.openSession(
                            new IWindowSessionCallback.Stub() {
                                @Override
                                public void onAnimatorScaleChanged(float scale) {
                                    ValueAnimator.setDurationScale(scale);
                                }
                            });
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            return sWindowSession;
        }
    }
}


```

## ViewRootImpl

```java
//这个类就是控制view的生命周期
class ViewRootImpl {
    
    public ViewRootImpl(Context context, Display display) {
        ...
        mWindowSession = WindowManagerGlobal.getWindowSession();
        ...
        // 创建Choreographer对象，这个对象很重要，可以把它理解为一个Handler
        // Android 16.6ms刷新一次页面它启到了主要作用
        // 我们马上看看这个Choreographer是个什么
        mChoreographer = Choreographer.getInstance();
    }
    
    public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView,int userId) {
        ...
        requestLayout();
        ...
        //mWindowSession是WindowManagerService.openSession()构建的Session
        res = mWindowSession.addToDisplayAsUser(mWindow, mSeq, mWindowAttributes,
                            getHostVisibility(), mDisplay.getDisplayId(), userId, mTmpFrame,
                            mAttachInfo.mContentInsets, mAttachInfo.mStableInsets,
                            mAttachInfo.mDisplayCutout, inputChannel,
                            mTempInsets, mTempControls);
    }
    
    @Override
    public void requestLayout() {
        if (!mHandlingLayoutInLayoutRequest) {
            //检查是否主线程
            checkThread();
            mLayoutRequested = true;
            //执行测量、布局、绘制
            scheduleTraversals();
        }
    }
    
    //执行测量、布局、绘制
    void scheduleTraversals() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
            //调用Choreographer的postCallback方法
            //mTraversalRunnable就是一个处理绘制的任务
            mChoreographer.postCallback(
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
           .....
        }
    }


    final class TraversalRunnable implements Runnable {
        @Override
        public void run() {
            doTraversal();
        }
    }

    void doTraversal() {
            ......
            performTraversals();
            ......
    }

    //performTraversals最后会调用performMeasure，performLayout，performDraw
    //来处理界面的测量，布局和绘制流程
     private void performTraversals() {
           .....
 
            //处理测量流程
            performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
            .....
            
            //处理布局流程
            performLayout(lp, mWidth, mHeight);
            ...
            
            //处理绘制流程     
            performDraw();
    }
    
    //测量
    private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
        if (mView == null) {
            return;
        }
        Trace.traceBegin(Trace.TRACE_TAG_VIEW, "measure");
        try {
            mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_VIEW);
        }
    }
    //布局
    private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth,
            int desiredWindowHeight) {
        ...
        final View host = mView;
        if (host == null) {
            return;
        }
        ...
        host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
        ...
        view.requestLayout();
    }
    //绘制
    private void performDraw() {
        ...
        boolean canUseAsync = draw(fullRedrawNeeded);
    }
   
}

```

## WindowManagerService

```java
public class WindowManagerService extends IWindowManager.Stub {
    @Override
    public IWindowSession openSession(IWindowSessionCallback callback) {
        return new Session(this, callback);
    }
    ...
    public int addWindow(Session session, IWindow client, int seq,
            LayoutParams attrs, int viewVisibility, int displayId, Rect outFrame,
            Rect outContentInsets, Rect outStableInsets,
            DisplayCutout.ParcelableWrapper outDisplayCutout, InputChannel outInputChannel,
            InsetsState outInsetsState, InsetsSourceControl[] outActiveControls,
            int requestUserId) {
        
    }
}

class Session extends IWindowSession.Stub implements IBinder.DeathRecipient {
    @Override
    public int addToDisplayAsUser(IWindow window, int seq, WindowManager.LayoutParams attrs,
            int viewVisibility, int displayId, int userId, Rect outFrame,
            Rect outContentInsets, Rect outStableInsets,
            DisplayCutout.ParcelableWrapper outDisplayCutout, InputChannel outInputChannel,
            InsetsState outInsetsState, InsetsSourceControl[] outActiveControls) {
        //执行的就是WindowManagerService的addWindow()方法,真正的把Window交给显卡
        return mService.addWindow(this, window, seq, attrs, viewVisibility, displayId, outFrame,
                outContentInsets, outStableInsets, outDisplayCutout, outInputChannel,
                outInsetsState, outActiveControls, userId);
    }
}
```



## Choreographer

```java
public static Choreographer getInstance() {
        return sThreadInstance.get();
    }


    // Thread local storage for the choreographer.
    private static final ThreadLocal<Choreographer> sThreadInstance =
            new ThreadLocal<Choreographer>() {
        @Override
        protected Choreographer initialValue() {
           //注意了这里其实就是主线程的Looper
           //ViewRootImpl对象就是在主线程创建的
            Looper looper = Looper.myLooper();
            if (looper == null) {
                throw new IllegalStateException("The current thread must have a looper!");
            }
            //创建Choreographer对象
            Choreographer choreographer = new Choreographer(looper, VSYNC_SOURCE_APP);
            if (looper == Looper.getMainLooper()) {
                mMainInstance = choreographer;
            }
            return choreographer;
        }
    };

    private Choreographer(Looper looper, int vsyncSource) {
        mLooper = looper;
        //前面我们说了正常情况瞎这个looper对象就是主线程的looper对象
        //所以通过这个Handler发送的消息都是在主线程处理的
        mHandler = new FrameHandler(looper);
        
        //创建FrameDisplayEventReceiver
        //这个对象可以理解为一个任务
        mDisplayEventReceiver = USE_VSYNC
                ? new FrameDisplayEventReceiver(looper, vsyncSource)
                : null;
        mLastFrameTimeNanos = Long.MIN_VALUE;

        mFrameIntervalNanos = (long)(1000000000 / getRefreshRate());
        
        //可以理解为绘制任务队列
        mCallbackQueues = new CallbackQueue[CALLBACK_LAST + 1];
        for (int i = 0; i <= CALLBACK_LAST; i++) {
            mCallbackQueues[i] = new CallbackQueue();
        }
        // b/68769804: For low FPS experiments.
        setFPSDivisor(SystemProperties.getInt(ThreadedRenderer.DEBUG_FPS_DIVISOR, 1));
    }




 private final class FrameDisplayEventReceiver extends DisplayEventReceiver
            implements Runnable {
        private boolean mHavePendingVsync;
        private long mTimestampNanos;
        private int mFrame;

        public FrameDisplayEventReceiver(Looper looper) {
            super(looper);
        }
       
        //这个的onVsync方法是什么时候调用的呢？
        //我们知道Android没16.6ms都会刷新一次屏幕，原理其实就是这个Vsync导致的
        //这个Vsync信号是从底层传递上来的
        //onVsync这个方法也是通过jni从底层调用上来，这个方法不会被java层调用
        //每16.6ms调用一次
        @Override
        public void onVsync(long timestampNanos, int builtInDisplayId, int frame) {
            // Ignore vsync from secondary display.
            // This can be problematic because the call to scheduleVsync() is a one-shot.
            // We need to ensure that we will still receive the vsync from the primary
            // display which is the one we really care about.  Ideally we should schedule
            // vsync for a particular display.
            // At this time Surface Flinger won't send us vsyncs for secondary displays
            // but that could change in the future so let's log a message to help us remember
            // that we need to fix this.

            mTimestampNanos = timestampNanos;
            mFrame = frame;
            Message msg = Message.obtain(mHandler, this);
            msg.setAsynchronous(true);
            //通过Handler把自身作为一个任务发送到主线程的消息队列去做处理
            mHandler.sendMessageAtTime(msg, timestampNanos / TimeUtils.NANOS_PER_MS);
        }

        @Override
        public void run() {
            mHavePendingVsync = false;
            //当绘制任务被处理时调用doFrame方法
            doFrame(mTimestampNanos, mFrame);
        }
    }


 void doFrame(long frameTimeNanos, int frame) {
     .....
     .....
            if (frameTimeNanos < mLastFrameTimeNanos) {
                if (DEBUG_JANK) {
                    Log.d(TAG, "Frame time appears to be going backwards.  May be due to a "
                            + "previously skipped frame.  Waiting for next vsync.");
                }
                scheduleVsyncLocked();
                return;
            }
            
            //执行这一次队列中的绘制任务
            Trace.traceBegin(Trace.TRACE_TAG_VIEW, "Choreographer#doFrame");
            AnimationUtils.lockAnimationClock(frameTimeNanos / TimeUtils.NANOS_PER_MS);

            mFrameInfo.markInputHandlingStart();
            doCallbacks(Choreographer.CALLBACK_INPUT, frameTimeNanos);

            mFrameInfo.markAnimationsStart();
            doCallbacks(Choreographer.CALLBACK_ANIMATION, frameTimeNanos);

            mFrameInfo.markPerformTraversalsStart();
            doCallbacks(Choreographer.CALLBACK_TRAVERSAL, frameTimeNanos);

            doCallbacks(Choreographer.CALLBACK_COMMIT, frameTimeNanos);

      ......
      ......

   }

 void doCallbacks(int callbackType, long frameTimeNanos) {
        CallbackRecord callbacks;
         final long now = System.nanoTime();
           //根据callbackType取出mCallbackQueues的任务
            callbacks = mCallbackQueues[callbackType].extractDueCallbacksLocked(
                    now / TimeUtils.NANOS_PER_MS);
            if (callbacks == null) {
                return;
            }
        synchronized (mLock) {
   
        try {
            Trace.traceBegin(Trace.TRACE_TAG_VIEW, CALLBACK_TRACE_TITLES[callbackType]);
           //for循环执行需要绘制的任务
            for (CallbackRecord c = callbacks; c != null; c = c.next) {
                if (DEBUG_FRAMES) {
                    Log.d(TAG, "RunCallback: type=" + callbackType
                            + ", action=" + c.action + ", token=" + c.token
                            + ", latencyMillis=" + (SystemClock.uptimeMillis() - c.dueTime));
                }
                c.run(frameTimeNanos);
            }
        } 
    }
```



## 总结：

Activity页面什么时候被绘制？

Activity的onResume方法执行完成之后，我们的Activity页面绘制还没有开始。当系统后续调用WindowManagerImpl的addView方法时，系统会创建一个ViewRootImpl对象，创建ViewRootImpl对象的时候，系统也会去创建一个Choreographer对象。这个Choreographer对象类似于一个Handler只不过它接收的底层传上来的Vsync事件。ViewRootImpl对象创建好了之后，会调用ViewRootImpl的setView方法，这个方法最后会把绘制任务添加到Choreographer的成员变量mCallbackQueues中。当底层每隔16.6ms调用一次onVsync方法时，会根据任务的时间存入的想要执行的时间来执行绘制任务。且绘制任务的执行是在主线程，就算我们的Activity的onResume被执行完成了，然后主线程出现了卡顿，我们Activity页面的绘制任务可能出现延迟，甚至不执行。





## 提问环节

### 为什么子线程能刷新UI？

条件：在子线程能够刷新UI的终极条件是不能触发WindowManagerImpl的requestLayout()方法，  因为requestLayout的时候会调用**checkThread**方法检查线程；



**以下场景能子线程刷新UI：**

1、如果控件是固定大小的，子线程能刷新UI，但是关闭硬件加速之后依然会报错，原理也是一样，不支持硬件加速后会跑requestLayout，支持硬件加速时只调用scheduleTraversals()；

```kotlin
text.text = "onCreate thread = " + Thread.currentThread()
text.setOnClickListener {
     //view配置了固定大小，子线程刷新UI不会报错，注意清单文件要打开硬件加速android:hardwareAccelerated="true"
     thread {
         text.text = "onClick thread = " + Thread.currentThread()
     }
}
```

2、可以先在主线程requestLayout之后再放到主线程更新UI，原理是让ViewRootImpl的requestLayout时判断mHandlingLayoutInLayoutRequest=true，这是google防 止UI连续请求绘制做的一个优化，这样做就是在卡漏洞

```kotlin
text.text = "onCreate thread = " + Thread.currentThread()
text.setOnClickListener {
    //view配置了wrap_content，子线程刷新UI会报错
    //但是主线程更新完UI，立马给子线程更新是不会报错的
    text.text = "onCreate thread = " + Thread.currentThread()
    thread {
        text.text = "onClick thread = " + Thread.currentThread()
    }
}
```

3、由于ViewRootImpl的线程检测是用创建者线程与调用requestLayout线程做比较，如果要做子线程刷新UI，可以在子线程中创建ViewRootImpl，再让此线程刷新UI

```kotlin
thread {
    Looper.prepare()
    var button = Button(this)
    button.text = "init"
    val layoutParams = WindowManager.LayoutParams()
    // 这里新建了一个
    windowManager.addView(button,WindowManager.LayoutParams())
    button.setOnClickListener {
        button.text = "${Thread.currentThread().name}:${SystemClock.uptimeMillis()}"
    }
    Looper.loop()
}
```

