# UncaughtExceptionHandler



参考：https://juejin.cn/post/6904283635856179214#heading-11

```kotlin
//定义CrashHandler
class CrashHandler private constructor(): Thread.UncaughtExceptionHandler {
    private var context: Context? = null
    fun init(context: Context?) {
        this.context = context
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {}

    companion object {
        val instance: CrashHandler by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            CrashHandler() }
    }
}

//Application中初始化
class MyApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        CrashHandler.instance.init(this)
    }
}

//Activity中触发异常
class ExceptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exception)
        
        btn.setOnClickListener {
            throw RuntimeException("主线程异常")
        }
        btn2.setOnClickListener {
            Thread {
                throw NumberFormatException("子线程异常")
            }.start()
        }
    }
}
```

