# HandlerThread

## 一、用途

子线程处理业务，继承于Thread，内部封装了Looper，通过外部创建Handler的方式引用其内部的Looper，让调用者可以通过Handler发消息来发起后台任务，然后在dispatchMessage的回调中填写任务内容，这套逻辑简化了后台处理业务的方式。



## 二、用法

```java
public class MainActivity extends AppCompatActivity {
 
    private TextView tvMain;
    private HandlerThread mHandlerThread;
    //子线程中的handler
    private Handler mThreadHandler;
    //UI线程中的handler
    private Handler mMainHandler = new Handler();
 
    //以防退出界面后Handler还在执行
    private boolean isUpdateInfo;
    //用以表示该handler的常熟
    private static final int MSG_UPDATE_INFO = 0x110;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvMain = (TextView) findViewById(R.id.tv_main);
        initThread();
    }
 
 
    private void initThread() {
        mHandlerThread = new HandlerThread("check-message-coming");
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                update();//模拟数据更新
                if (isUpdateInfo) {
                    mThreadHandler.sendEmptyMessage(MSG_UPDATE_INFO);
                }
            }
        };
    }
 
    private void update() {
        try {
            //模拟耗时
            Thread.sleep(2000);
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    String result = "每隔2秒更新一下数据：";
                    result += Math.random();
                    tvMain.setText(result);
                }
            });
 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
 
    @Override
    protected void onResume() {
        super.onResume();
        //开始查询
        isUpdateInfo = true;
        mThreadHandler.sendEmptyMessage(MSG_UPDATE_INFO);
    }
 
    @Override
    protected void onPause() {
        super.onPause();
        //停止查询
        //以防退出界面后Handler还在执行
        isUpdateInfo = false;
        mThreadHandler.removeMessages(MSG_UPDATE_INFO);
    }
 
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放资源
        mHandlerThread.quit();
    }
}
```



## 三、Q&A

### 1.为什么要使用HandlerThread？

答： a.开发中如果多次使用类似new Thread(){...}.start()

这种方式开启一个子线程，会创建多个匿名线程，使得程序运行起来越来越慢，

而HandlerThread自带Looper使他可以通过消息来多次重复使用当前线程，节省开支；



b.android系统提供的Handler类内部的Looper默认绑定的是UI线程的消息队列，

对于非UI线程又想使用消息机制，那么HandlerThread内部的Looper是最合适的，它不会干扰或阻塞UI线程



### 2.HandlerThread内部的Looper为什么是子线程？

答：因为HandlerThread本身是一个子线程，所以在子线程获取的Looper就会是子线程的。



### 3.为什么在Activity里new的Handler内部的Looper是主线程？

答：因为在Activity中所运行的生命周期都是在ActivityThread的Looper中运行的，此Looper就是进程诞生时同时new出来的sMainLooper，

我们子啊Activity中通过getMainLooper()获取的对象就是sMainLooper。







