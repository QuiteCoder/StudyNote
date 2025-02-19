package com.baidu.naviauto;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.baidu.navisdk.util.common.LogUtil;

/**
 * 此类用于监控应用的主线程的耗时任务
 * 在新日志开关打开，或者debug模式下才启动
 * 主要原理参照SystemServer的WatchDog
 * 1.每隔1秒钟往主线程塞一个500ms后的任务
 * 2.一秒钟之后检测到任务是否执行到
 * 3.如果任务没有执行到，那么打印主线程正在执行的堆栈
 * 4.如果任务有执行到，那么继续往主线程塞一个任务
 * 对于ANR的发生，可以检测Touch事件无响应ANR发生前此类是否有打印堆栈，
 * 如果有打印堆栈，说明主线程耗时，从打印的堆栈中定位耗时的原因
 * 如果没有打印堆栈，说明主线程不耗时，是系统的相关调用有问题
 */
public class WatchDog {
    public static final String IS_WATCH_OPEN = "IsWatchDogOpen";
    private static final String LOG_TAG = "WatchDog";
    private boolean mHasInit = false;
    private Handler mMainHandler;
    private Handler mWatchHandler;
    private boolean hasTaskInMain;
    private HandlerThread mHandlerThread;
    private long mPostTime;

    private static class InnerClass {
        private static final WatchDog INSTANCE = new WatchDog();
    }

    private WatchDog() {

    }

    public static WatchDog getInstance() {
        return InnerClass.INSTANCE;
    }

    public void init() {
        Log.e(LOG_TAG, "init mHasInit = " + mHasInit);
        if (mHasInit) {
            return;
        }
        mHasInit = true;
        mMainHandler = new Handler(Looper.getMainLooper());
        mHandlerThread = new HandlerThread("NaviWatchDog");
        mHandlerThread.start();
        Looper looper = mHandlerThread.getLooper();
        mWatchHandler = new Handler(looper);
        startWatch();
    }

    public void unInit() {
        Log.e(LOG_TAG, "unInit mHasInit = " + mHasInit);
        if (!mHasInit) {
            return;
        }
        try {
            mHandlerThread.quit();
        } catch (Exception e) {

        }
        mHandlerThread = null;
        mHasInit = false;
    }

    private void startWatch() {
        if (!mHasInit) {
            return;
        }
        if (hasTaskInMain) {
            dumpMainStack();
        }
        if (mMainHandler == null || mWatchHandler == null) {
            return;
        }
        if (!hasTaskInMain) {
            mPostTime = System.currentTimeMillis();
            hasTaskInMain = true;
            mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hasTaskInMain = false;
                }
            }, 500);
        }
        mWatchHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startWatch();
            }
        }, 1000);
    }

    private void dumpMainStack() {
        Thread thread = Looper.getMainLooper().getThread();
        StringBuffer sb = new StringBuffer();
        StackTraceElement[] stackElements = thread.getStackTrace();
        if (stackElements != null) {
            for (int i = 0; i < stackElements.length; ++i) {
                sb.append("at " + stackElements[i].getClassName() + "."
                        + stackElements[i].getMethodName() + "(" + stackElements[i].getFileName()
                        + ":" + stackElements[i].getLineNumber() + ")\n");
            }
        }
        String stack = sb.toString();
        LogUtil.d(LOG_TAG, "dumpMainStack cost " + (System.currentTimeMillis() - mPostTime));
        LogUtil.e(LOG_TAG, "stack = " + stack);
    }
}
