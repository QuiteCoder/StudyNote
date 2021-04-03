# View绘制流程源码分析

## 背景 

为了研究activity的生命周期与view的measure、draw、layout的关联，和研究什么时候才会出发view的requestLayout方法，

因为掌握了这个知识点就可以用子线程更新UI了，已知View.requestLayout-->ViewRootImpl.checkThread才会报UI不能在子线程更新UI的Exception。

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
        //a是一个activity对象，每个activity都有一个WindowManager,跟踪到Activity的源码发现，这个wm其实是WindowManagerImpl，其内部的addView方法是调用了单例对象WindowManagerGlobal的addViwe方法
        ViewManager wm = a.getWindowManager();
        ...
        wm.addView(decor, l);
        ...
    }
}

//
class WindowManagerGlobal {
    public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow) {
        root = new ViewRootImpl(view.getContext(), display);
        ...
        root.setView(view, wparams, panelParentView);
        ...
    }
}


```

