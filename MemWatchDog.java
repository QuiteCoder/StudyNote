package com.baidu.naviauto.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import com.baidu.mapautosdk.util.storage.StorageSettings;
import com.baidu.navisdk.util.common.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;

/**
 * FileName: MemWatchDog
 * Author: chentao22
 * Date: 2023/5/17 20:34
 * Description: 内存监控
 * History:
 */
public class MemWatchDog {
    private static final String LOG_TAG = "MemWatchDog";
    private final HandlerThread mHandlerThread;
    private final Handler mWatchHandler;
    private static final long FREQ = 60 * 1000;
    /**
     * 开始dump的内存 单位kb
     */
    private static final long START_DUMP = 1400 * 1024;
    private static final long DUMP_GAP = 50 * 1024;
    private long mLastDumpMem = 0;
    private ActivityManager mActivityManager;
    private boolean mIsRun = false;

    private static class InnerClass {
        private static MemWatchDog sInstance = new MemWatchDog();
    }

    public static MemWatchDog getInstance() {
        return InnerClass.sInstance;
    }

    private Runnable mWatchRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsRun) {
                return;
            }
            watch();
            mWatchHandler.postDelayed(mWatchRunnable, FREQ);
        }
    };

    private void watch() {
        if (mActivityManager == null) {
            return;
        }
        int memory = getMemory();
        LogUtil.e(LOG_TAG, "watch mem = " + memory);
        if (memory < START_DUMP) {
            return;
        }
        if (mLastDumpMem == 0
                || (memory - mLastDumpMem) > DUMP_GAP) {
            mLastDumpMem = memory;
            dump();
        }
    }

    public void dump() {
        LogUtil.e(LOG_TAG, "dum start");
        File hprofFileDir = getHprofFileDir();
        String pidStr = String.valueOf(Process.myPid());
        File[] list = hprofFileDir.listFiles();
        for (File file : list) {
            if (!file.getName().contains(pidStr)) {
                file.delete();
            }
        }
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        String time = "_" + month + "_" + day + "_" + hour + "_" + min;
        File hprofFile = new File(hprofFileDir, "hprof_" + (mLastDumpMem / 1024) +
                "_" + pidStr + time + ".hprof");
        try {
            String absolutePath = hprofFile.getAbsolutePath();
            LogUtil.e(LOG_TAG, "dumpHprof path = " + absolutePath);
            Debug.dumpHprofData(hprofFile.getAbsolutePath());
        } catch (IOException e) {
            LogUtil.e(LOG_TAG, "dumpHprof failed error = " + e.getMessage());
        }
    }

    private File getHprofFileDir() {
        String dataPath = StorageSettings.getInstance().getCurrentStorage().getDataPath();
        File dataFile = new File(dataPath);
        File hprofFile = new File(dataFile, "hprof");
        if (!hprofFile.exists()) {
            hprofFile.mkdirs();
        }
        return hprofFile;
    }

    private int getMemory() {
        int mem = 0;
        try {
            Debug.MemoryInfo memory = new Debug.MemoryInfo();
            Debug.getMemoryInfo(memory);
            Map<String, String> memoryStats = memory.getMemoryStats();
            for (Map.Entry<String, String> entry : memoryStats.entrySet()) {
                LogUtil.d(LOG_TAG, "getMemory key = " + entry.getKey() + " value = " + entry.getValue());
            }
            return memory.getTotalPss();
        } catch (Exception err) {
            LogUtil.e(LOG_TAG, "getMemory failed error = " + err.getMessage());
        }

        return mem;
    }

    private MemWatchDog() {
        mHandlerThread = new HandlerThread("MemWatchDog");
        mHandlerThread.start();
        Looper looper = mHandlerThread.getLooper();
        mWatchHandler = new Handler(looper);
    }

    public void init(Context context) {
        mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public void startWatch() {
        mIsRun = true;
        mWatchHandler.postDelayed(mWatchRunnable, FREQ);
    }

    public void stopWatch() {
        mIsRun = false;
        mWatchHandler.removeCallbacks(mWatchRunnable);
    }

}
