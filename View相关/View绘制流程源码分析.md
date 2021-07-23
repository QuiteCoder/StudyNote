#  View绘制流程源码分析

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

5、初始DecorView，PhoneWindow.installDecor()

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
    
    //2、当在Activity的OnCreate()调用setContentView()实质上是调用了PhoneWindowd的
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


//Activity的onStart()堆栈
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
//4、在Activity的onResume时，windowRootImpl开始对layout进行measure、layout、draw
class ActivityThread {
    @Override
    public void handleResumeActivity(...) {
        //a是一个activity对象，每个activity都有一个WindowManager,跟踪到Activity的源码发现，
        //这个wm其实是WindowManagerImpl，其内部的addView方法是调用了单例对象WindowManagerGlobal的addViwe方法
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

//这个类就是控制view的生命周期
class ViewRootImpl {
    
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
        //执行的就是WindowManagerService的addWindow()方法
        return mService.addWindow(this, window, seq, attrs, viewVisibility, displayId, outFrame,
                outContentInsets, outStableInsets, outDisplayCutout, outInputChannel,
                outInsetsState, outActiveControls, userId);
    }
}
```

//接下来就是解释为什么子线程能刷新UI
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
    windowManager.addView(button,WindowManager.LayoutParams())
    button.setOnClickListener {
        button.text = "${Thread.currentThread().name}:${SystemClock.uptimeMillis()}"
    }
    Looper.loop()
}
```

