# Kotlin

## 一、变量常量与类型

Kotlin只有引用数据类型，java有基本数据类型和引用数据类型

注意：Kotlin生成字节码之后，是有基本数据类型的，之所以在写Kotlin代码的时候只有引用数据类型，是为了方便开发

1.var是可修改的变量，val是只读的变量

2.类型推断：不需要显式的声明变量的类型

```kotlin
fun main() {
    // var是可修改的变量，val是只读的变量
    var number : Int = 5
    val number2 :Int = 5

    // 类型推断：不需要显式的声明变量的类型
    var number3 = 5
}
```



## 二、条件语句

1.if/else跟java一样

2.rang表达式：in A..B，判断是否再A和B的区间范围内

```kotlin
var age = 1
while (age > 0) {
    // 控制台输入
    val sc = Scanner(System.`in`)
    // 读取控制台int数据
    age = sc.nextInt()
    // x >= 0 && x <= 3 
    if (age in 0..3) {
        println("婴幼儿")
    } else if (age in 3..12) {
        println("少儿")
    } else if (age in 12..18) {
        println("青少年")
    } else {
        println("成年人")
    }
}

输入：3
打印：婴幼儿

输入：4
打印：少儿
```

3.when条件语句

```kotlin
var school = "高中"
val s = when (school) {
    "幼儿园" -> "幼儿"
    "小学" -> "少儿"
    "初中", "高中" -> "青少年"
    "大学" -> "成人"
    else -> "未知"
}
println("result = $s")

打印：result = 青少年
```



## 三、String模板

```kotlin
fun main() {
    val man = "Jack"
    val woman = "Rose"
    val flage = true
    println("$man love $woman")
    println("Will $woman marry $man? ${if (flage) "Yes,she will！" else "No,she won't!"}")

    // 犯罪嫌疑人
    var suspect = "张三"
    println("$suspect 涉嫌电信诈骗。")
    // 想要去除张三后的空格，用{}隔离
    println("${suspect}涉嫌电信诈骗。")
}


输出结果：
Jack love Rose
Will Rose marry Jack? Yes,she will！
张三 涉嫌电信诈骗。
张三涉嫌电信诈骗。
```



## 四、函数

### 1.声明函数

```kotlin
// 可见性修饰符，函数声明关键字，函数名，函数参数，返回类型
private fun doSomeThing(age:Int, flag:Boolean):String {}
```

函数的特性

a.函数默认可见性修饰符是public，函数默认的返回值类型时Unit（无返回），相当于java的void

b.定义函数参数时可以给予初始化数据

c.给函数传参时，可以利用参数名赋值的方式，无视传入参数的顺序

d.当函数参数都有初始化数据时，不传参就是使用默认的参数数据

```kotlin
// a.函数默认可见性修饰符是public，函数默认的返回值类型时Unit（无返回），相当于java的void
public fun doSomething(age:Int, name:String):Unit {
    println("姓名：$name,年龄：$age")
}
// b.定义函数参数时可以给予初始化数据
private fun math(number1:Int=5, number2:Int=5):Int {
    println("result = $number1+$number2")
    return number1 + number2
}

fun main() {
    doSomething(18, "Jack")
    // c.给函数传参时，可以利用参数名赋值的方式，无视传入参数的顺序
    doSomething(name = "Jimmy", age = 22)
    math(1,2)
    math(number2 = 10)
    // d.当函数参数都有初始化数据时，不传参就是使用默认的参数数据
    math()
}

输出结果：
姓名：Jack,年龄：18
姓名：Jimmy,年龄：22
result = 1+2
result = 5+10
result = 5+5
```



### 2.反引号中的函数名

使用反引号可以定义含各种符号的函数名

```kotlin
fun `**~make something funtion~**`() {
    println("special method")
}

fun main() {
    `**~make something funtion~**`()
}

输出：
special method
```



kotlin通过反引号调用java函数

```kotlin
class MyJava {
    public void is(String msg) {
        System.out.println("This is java method with Sting param :" + msg);
    }
}

// 在kotlin中is属于关键字，使用反引号才能正常调用函数名为is的java函数
fun main() {
    MyJava.`is`("测试")
}

输出：
This is java method with Sting param :测试
```



### 3.匿名函数

待补充





## 五、Nothing类型

TODO函数的任务就是抛出异常，就是永远别指望它运行成功，返回Nothing类型。

```kotlin
fun main() {
    println("doSomething...")
    TODO("我不干了！")
    // TODO之后的代码不会继续运行
    println("after doSomething")
}

输出：
doSomething...
Exception in thread "main" kotlin.NotImplementedError: An operation is not implemented: 我不干了！
	at com.example.studykotlin.TODOTestKt.main(TODOTest.kt:6)
	at com.example.studykotlin.TODOTestKt.main(TODOTest.kt)
```





## 六、Unit类型

函数默认的返回值类型时Unit（无返回），java中用void表示



# Kotlin协程

协程只是编程历史发展过程中产生的对开发者友好的一种异步编程方式，相对于传统的多线程并发编程并不能提高额外的性能，协程并发编程的底层仍是用线程池实现，因为耗时的操作总要线程去执行，协程并不能减少额外的执行逻辑，另外协程因为创建了中间的一系列封装对象，比传统的多线程编程增加了额外的内存消耗，官方文档对此特定说明了编译器会进行一些优化，从而尽可能减少内存消耗，开发者无需担心，但协程所带来的开发维护成本大大减低、内存上的妥协带来的是对开发者的友好还是值得的。

**优势**

- **简洁** 更高抽象隐藏异步实现细节，实现写异步如同同步的顺序式写法
- **比闭包更好用的上下文参数** 传统回调方式严重依赖闭包特性，而要在一个连续逻辑中写出多个回调函数且共享相同的上下文，这样的代码难读懂、难写对、难维护，协程就简单多了，一个上下文抽象搞定
- **更方便处理并发逻辑** 父子协程概念 可以更容易的管理协程的执行



## 原理分析

简单的说就是通过封装`Continuation`（续体），编译器编译后会生成一个实现`Continuation`接口的类，这些续体会挂起，然后由`SuspendLambda` 管理状态机，

总结：尾随闭包被编译器生成SuspendLambda的子类，其中invokeSuspend方法就是闭包中的代码实现，最后会被封装成Runnable的成员变量，其中run方法执行的代码就是invokeSuspend的代码。

挂起：当遇到第一个挂起方法，如delay，withContext，会返回一个COROUTINE_SUSPENDED标记，让invokeSuspend方法就此结束。

恢复：当挂起方法执行完，就相当DispatchedTask的run方法执行完成，然后内部调用resumeWith对上级协程进行重新调用其invokeSuspend方法。



### 关键类的继承关系：

`SuspendLambda` 继承结构：

```
SuspendLambda > ContinuationImpl > BaseContinuationImpl > Continuation
    									|				  |
    									|                resume()
    							    resumeWith()	    resumeWith()
    							  invokeSuspend()->在SuspendLambda的子类中实现
```

`StandaloneCoroutine`继承结构：

```
LazyStandaloneCoroutine > StandaloneCoroutine > AbstractCoroutine
```

`DispatchCoroutine`继承结构：

```
DispatchedCoroutine > ScopeCoroutine > AbstractCoroutine
        |                   |
        |              afterResume()
   trySuspend()->挂起上一级协程
   tryResume()->恢复上一级协程，最终调用uCont的resumeWith() -> invokeSuspend()
```

`AbstractCoroutine`继承结构：

```
AbstractCoroutine > JobSupport, Job, Continuation, CoroutineScope
									   |
    								resume()
								   resumeWith()
```

`DispatchedContinuation`继承结构：

```java
// SchedulerTask是Task的别名
DispatchedContinuation > DispatchedTask > SchedulerTask > Task > Runnable
```



例子：

```kotlin
class MainActivity : ComponentActivity() {
    private val TAG: String = javaClass.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)
        val button = findViewById<Button>(R.id.main_btn).setOnClickListener(View.OnClickListener {
            Log.d(TAG, "onclick: start")
            val coroutineScope = CoroutineScope(Job())
            coroutineScope.launch(Dispatchers.Main)  {
                Log.d(TAG, "=====launch start=====")
                var string = getDataFromRemote()
                var button :Button = it as Button
                button.text = string
                Log.d(TAG, "=====launch start=====")
            }
            Log.d(TAG, "onclick: end")

        })
    }
    suspend fun getDataFromRemote(): String {
        withContext(Dispatchers.IO) {
            delay(1000)
        }
        return "data form remote."
    }
}
```



### 总结：

1.尾随闭包（launch后的{ }），被编译器生成了一个SuspendLambda的子类，实现了Function2的接口， 在show Kotlin ByteCode插件中可以看到：

final class com/example/studykotlin/MainActivity$onCreate$button$1$1 extends kotlin/coroutines/jvm/internal/SuspendLambda implements kotlin/jvm/functions/Function2{ invokeSuspend() }



2.最为关键的就是**invokeSuspend**这个方法，这是协程的入口。

> 其中使用状态机的机制，每个挂起函数都会对应一个状态，当挂起函数返回**COROUTINE_SUSPENDED**，协程被挂起，挂起方法下方的代码就无法被执行，比如button.text = string就无法被执行；
>
> 挂起函会在一个续体中执行完成，若顺利则返回RESUMED，恢复协程，再次调用invokeSuspend方法，将结果从方法参数中Object 类型的result 中返回，这时候挂起方法下方的代码就会获取到结果，button.text = string。



3.getDataFromRemote()方法中的withContext的尾随闭包也被编译器生成一个SuspendLambda的子类，同样实现了Function2的接口：

final class com/example/studykotlin/MainActivity$getDataFromRemote$2 extends kotlin/coroutines/jvm/internal/SuspendLambda implements kotlin/jvm/functions/Function2 { invokeSuspend() }



4.当调用getDataFromRemote()中的挂起函数**withContext**时，会返回一个枚举数据**COROUTINE_SUSPENDED**，这时候根协程的SuspendLambda的子类的**invokeSuspend**就会被return掉，终止执行，这就是所谓的 **“挂起”**。



反编译：

```kotlin
public final class MainActivity extends ComponentActivity {
   private final String TAG;

   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(layout.main_activity);
      ((Button)this.findViewById(id.main_btn)).setOnClickListener((View.OnClickListener)(new View.OnClickListener() {
         public final void onClick(final View it) {
            Log.d(MainActivity.this.TAG, "onclick: start");
            CoroutineScope coroutineScope = CoroutineScopeKt.CoroutineScope((CoroutineContext)JobKt.Job$default((Job)null, 1, (Object)null));
             // 1.尾随闭包（协程里的代码），被编译器生成了一个SuspendLambda的子类，实现了Function2的接口
             // 在show Kotlin ByteCode插件中可以看到：final class com/example/studykotlin/MainActivity$onCreate$button$1$1 extends kotlin/coroutines/jvm/internal/SuspendLambda implements kotlin/jvm/functions/Function2
            BuildersKt.launch$default(coroutineScope, (CoroutineContext)Dispatchers.getMain(), (CoroutineStart)null, (Function2)(new Function2((Continuation)null) {
               int label;

                
                // 2.最为关键的就是这个方法，这是协程的入口
               @Nullable
               public final Object invokeSuspend(@NotNull Object $result) {
                  Object var4 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                  Object var10000;
                  switch (this.label) {
                    // lable 默认是0，第一次调用invokeSuspend的话会走这里
                     case 0:
                        ResultKt.throwOnFailure($result);
                        Log.d(MainActivity.this.TAG, "=====launch start=====");
                        MainActivity var5 = MainActivity.this;
                        this.label = 1; // lable会被改成1
                      // 由于getDataFromRemote()中使用了withContext，会返回COROUTINE_SUSPENDED，他是枚举类型：
                      // internal enum class CoroutineSingletons { COROUTINE_SUSPENDED, UNDECIDED, RESUMED }
                        var10000 = var5.getDataFromRemote(this); 
                        if (var10000 == var4) { 
                           return var4; // 这里直接返回了COROUTINE_SUSPENDED，退出invokeSuspend了，这种情况就是所谓的 “挂起”
                        }
                        break;
                     case 1:
                        ResultKt.throwOnFailure($result);
                      //  下一次调用就会走这里了，从invokeSuspend的参数中获取到getDataFromRemote()的执行结果
                        var10000 = $result;
                        break;
                     default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                  }

                  String string = (String)var10000;
                  View var6 = it;
                  if (var6 == null) {
                     throw new NullPointerException("null cannot be cast to non-null type android.widget.Button");
                  } else {
                      // 如果结果不为null，则赋值给button，完成整个协程的调用
                     Button button = (Button)var6;
                     button.setText((CharSequence)string);
                     Log.d(MainActivity.this.TAG, "=====launch start=====");
                     return Unit.INSTANCE;
                  }
               }

               @NotNull
               public final Continuation create(@Nullable Object value, @NotNull Continuation completion) {
                  Intrinsics.checkNotNullParameter(completion, "completion");
                   // 这个方法的调用时机：CoroutineStart.DEFAULT-> startCoroutineCancellable -> createCoroutineUnintercepted -> create
                   // 目的是为了将协程的本体（completion）封装在SuspendLambda的子类当中。
                  Function2 var3 = new <anonymous constructor>(completion);
                  return var3;
               }

               public final Object invoke(Object var1, Object var2) {
                  return ((<undefinedtype>)this.create(var1, (Continuation)var2)).invokeSuspend(Unit.INSTANCE);
               }
            }), 2, (Object)null);
            Log.d(MainActivity.this.TAG, "onclick: end");
         }
      }));
      Unit button = Unit.INSTANCE;
   }

   @Nullable
   public final Object getDataFromRemote(@NotNull Continuation var1) {
      Object $continuation;
      label20: {
         if (var1 instanceof <undefinedtype>) {
            $continuation = (<undefinedtype>)var1;
            if ((((<undefinedtype>)$continuation).label & Integer.MIN_VALUE) != 0) {
               ((<undefinedtype>)$continuation).label -= Integer.MIN_VALUE;
               break label20;
            }
         }

         $continuation = new ContinuationImpl(var1) {
            // $FF: synthetic field
            Object result;
            int label;

            @Nullable
            public final Object invokeSuspend(@NotNull Object $result) {
               this.result = $result;
               this.label |= Integer.MIN_VALUE;
               return MainActivity.this.getDataFromRemote(this);
            }
         };
      }

      Object $result = ((<undefinedtype>)$continuation).result;
      Object var4 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
      switch (((<undefinedtype>)$continuation).label) {
         case 0:
            ResultKt.throwOnFailure($result);
            CoroutineContext var10000 = (CoroutineContext)Dispatchers.getIO();
          
            // withContext的尾随闭包最终也会被编译器生成一个继承SuspendLambda的子类，实现Function2接口，重新invokeSuspend方法
            Function2 var10001 = (Function2)(new Function2((Continuation)null) {
               int label;

               @Nullable
               public final Object invokeSuspend(@NotNull Object $result) {
                  Object var2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                  switch (this.label) {
                     case 0:
                        ResultKt.throwOnFailure($result);
                        this.label = 1;
                        if (DelayKt.delay(1000L, this) == var2) {
                           return var2;
                        }
                        break;
                     case 1:
                        ResultKt.throwOnFailure($result);
                        break;
                     default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                  }

                  return Unit.INSTANCE;
               }

               @NotNull
               public final Continuation create(@Nullable Object value, @NotNull Continuation completion) {
                  Intrinsics.checkNotNullParameter(completion, "completion");
                  Function2 var3 = new <anonymous constructor>(completion);
                  return var3;
               }

               public final Object invoke(Object var1, Object var2) {
                  return ((<undefinedtype>)this.create(var1, (Continuation)var2)).invokeSuspend(Unit.INSTANCE);
               }
            });
            ((<undefinedtype>)$continuation).label = 1;
            if (BuildersKt.withContext(var10000, var10001, (Continuation)$continuation) == var4) {
               return var4;
            }
            break;
         case 1:
            ResultKt.throwOnFailure($result);
            break;
         default:
            throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
      }

      return "data form remote.";
   }

   public MainActivity() {
      String var10001 = this.getClass().getName();
      Intrinsics.checkNotNullExpressionValue(var10001, "javaClass.name");
      this.TAG = var10001;
   }
}
```



### 协程的创建：

前置环境：GlobalSocpe.launch{ }，上下文信息默认，启动模式默认

调用链：

```java
CoroutineScope.launch -> StandaloneCoroutine.start() -> CoroutineStart.DEFAULT -> block.startCoroutineCancellable() -> startCoroutineCancellable -> createCoroutineUnintercepted -> create
// create方法在SuspendLambda的子类中实现，目的是为了将StandaloneCoroutine封装到SuspendLambda的子类当中
```

```java
createCoroutineUnintercepted(receiver, completion).intercepted().resumeCancellableWith(Result.success(Unit), onCancellation)
// intercepted()目的是为了将SuspendLambda的子类封装在DispatchedContinuation当中
// 而resumeCancellableWith就是为了触发invokeSuspend方法
```



### 调度：

#### 调度器继承关系：

```
Main:       MainCoroutineDispatcher > CoroutineDispatcher
defalut:    DefaultScheduler > ExperimentalCoroutineDispatcher > ExecutorCoroutineDispatcher > CoroutineDispatcher
IO:         LimitingDispatcher > ExecutorCoroutineDispatcher  > CoroutineDispatcher
Unconfined: Unconfined > CoroutineDispatcher
```



```kotlin
// DispatchedContinuation
inline fun resumeCancellableWith(
    result: Result<T>,
    noinline onCancellation: ((cause: Throwable) -> Unit)?
) {
    val state = result.toState(onCancellation)
    // undefined类型isDispatchNeeded()会返回false，Main，default，IO都会返回true
    if (dispatcher.isDispatchNeeded(context)) {
        _state = state
        resumeMode = MODE_CANCELLABLE
        dispatcher.dispatch(context, this)
    } else {
        executeUnconfined(state, MODE_CANCELLABLE) {
            if (!resumeCancelled(state)) {
                // undefined不调度就走这里
                resumeUndispatchedWith(result)
            }
        }
    }
}
```

（1）不做调度undefined

```kotlin
    inline fun resumeUndispatchedWith(result: Result<T>) {
        withContinuationContext(continuation, countOrElement) {
            // 最终走在SuspendLambda父类BaseContinuationImpl的resumeWith()
            continuation.resumeWith(result)
        }
    }
```

```kotlin
internal abstract class BaseContinuationImpl(
    public val completion: Continuation<Any?>?
) : Continuation<Any?>, CoroutineStackFrame, Serializable {
    public final override fun resumeWith(result: Result<Any?>) {
        var current = this
        var param = result
        while (true) {
            probeCoroutineResumed(current)
            with(current) {
                val completion = completion!! // fail fast when trying to resume continuation without completion
                val outcome: Result<Any?> =
                try {
                    // 协程的尾随闭包在这里被调用，如果其中存在挂起函数，就会返回COROUTINE_SUSPENDED，挂起协程
                    val outcome = invokeSuspend(param)
                    if (outcome === COROUTINE_SUSPENDED) return
                    Result.success(outcome)
                } catch (exception: Throwable) {
                    Result.failure(exception)
                }
                releaseIntercepted() // this state machine instance is terminating
                if (completion is BaseContinuationImpl) {
                    current = completion
                    param = outcome
                } else {
                    completion.resumeWith(outcome)
                    return
                }
            }
        }
    }

    // 在SuspendLambda子类中重写
    protected abstract fun invokeSuspend(result: Result<Any?>): Any?
    ...
    ...
}
```



（2）默认是DefaultScheduler

```kotlin
// dispatcher是CoroutineDispatcher的子类，如果是默认的话就是DefaultScheduler
// this 就是DispatchedContinuation自身，因为实现Runnable接口，所以可以传进去
dispatcher.dispatch(context, this)
```

`DefaultScheduler`父类`ExperimentalCoroutineDispatcher`内部通过`CoroutineScheduler`协程线程池进行分发，内部实现原理类似java的`ThreadPoolExecutor`

```kotlin
public open class ExperimentalCoroutineDispatcher(
    private val corePoolSize: Int,
    private val maxPoolSize: Int,
    private val idleWorkerKeepAliveNs: Long,
    private val schedulerName: String = "CoroutineScheduler"
) : ExecutorCoroutineDispatcher() {
    public constructor(
        corePoolSize: Int = CORE_POOL_SIZE,
        maxPoolSize: Int = MAX_POOL_SIZE,
        schedulerName: String = DEFAULT_SCHEDULER_NAME
    ) : this(corePoolSize, maxPoolSize, IDLE_WORKER_KEEP_ALIVE_NS, schedulerName)

    @Deprecated(message = "Binary compatibility for Ktor 1.0-beta", level = DeprecationLevel.HIDDEN)
    public constructor(
        corePoolSize: Int = CORE_POOL_SIZE,
        maxPoolSize: Int = MAX_POOL_SIZE
    ) : this(corePoolSize, maxPoolSize, IDLE_WORKER_KEEP_ALIVE_NS)

    override val executor: Executor
        get() = coroutineScheduler

    private var coroutineScheduler = createScheduler()

    override fun dispatch(context: CoroutineContext, block: Runnable): Unit =
        try {
            // 正常会走这里，这个block就是DispatchedContinuation，其子类DispatchedTask实现了run方法
            coroutineScheduler.dispatch(block)
        } catch (e: RejectedExecutionException) {
            ...
        }
  ...

    private fun createScheduler() = CoroutineScheduler(corePoolSize, maxPoolSize, idleWorkerKeepAliveNs, schedulerName)
	...
}
```



`DispatchedTask`的run( )方法

```kotlin
public final override fun run() {
    assert { resumeMode != MODE_UNINITIALIZED } // should have been set before dispatching
    val taskContext = this.taskContext
    var fatalException: Throwable? = null
    try {
        // delegate就是DispatchedContinuation本身，其内部对该参数进行赋值: delegate = this
        val delegate = delegate as DispatchedContinuation<T>
        // continuation就是withContext的闭包编译出来的SuspendLambda的子类
        val continuation = delegate.continuation
        withContinuationContext(continuation, delegate.countOrElement) {
            ...
            if (job != null && !job.isActive) {
                ...
            } else {
                if (exception != null) {
                    ...
                } else {
                    // resume是Continuation的扩展函数，还是调用到了resumeWith()，由BaseContinuationImpl实现
                    continuation.resume(getSuccessfulResult(state))
                }
            }
        }
    } catch (e: Throwable) {
        ...
    } finally {
        ...
    }
}
```



#### 协程线程池：

`CoroutineScheduler`就是协程中的线程池

源码：

```kotlin
// CoroutineScheduler内部

fun dispatch(block: Runnable, taskContext: TaskContext = NonBlockingContext, tailDispatch: Boolean = false) {
    trackTask() // this is needed for virtual time support
    // 对Task加工一下，例如配置超时的时长
    val task = createTask(block, taskContext)
    // 获取当前的线程Worker，再往里边添加Task到私有队列，若添加失败则抛出拒绝异常，跟java的线程池很像
    val currentWorker = currentWorker()
    val notAdded = currentWorker.submitToLocalQueue(task, tailDispatch)
    if (notAdded != null) {
        if (!addToGlobalQueue(notAdded)) {
            // Global queue is closed in the last step of close/shutdown -- no more tasks should be accepted
            throw RejectedExecutionException("$schedulerName was terminated")
        }
    }
    ...
}

fun createTask(block: Runnable, taskContext: TaskContext): Task {
    val nanoTime = schedulerTimeSource.nanoTime()
    if (block is Task) {
        block.submissionTime = nanoTime
        block.taskContext = taskContext
        return block
    }
    return TaskImpl(block, nanoTime, taskContext)
}
```



```kotlin
// CoroutineScheduler的内部类
internal inner class Worker private constructor() : Thread() {
    
    // 每个Worker都有一个队列，默认长度是128
    val localQueue: WorkQueue = WorkQueue()
    ...
    private fun runWorker() {
        var rescanned = false
        while (!isTerminated && state != WorkerState.TERMINATED) {
            // 从队列中获取Task
            val task = findTask(mayHaveLocalTasks)
            // Task found. Execute and repeat
            if (task != null) {
                rescanned = false
                minDelayUntilStealableTaskNs = 0L
                executeTask(task)
                continue
            } else { ...}
            ...
        }
        ...
    }

    private fun executeTask(task: Task) {
        val taskMode = task.mode
        ...
        ...
        // 真正调用Task的run方法
        runSafely(task)
        ...
    }
}
```

```kotlin
internal const val BUFFER_CAPACITY_BASE = 7
internal const val BUFFER_CAPACITY = 1 shl BUFFER_CAPACITY_BASE //1 往左位运算7位是128
internal class WorkQueue {
    private val buffer: AtomicReferenceArray<Task?> = AtomicReferenceArray(BUFFER_CAPACITY)
    private fun addLast(task: Task): Task? {
        if (task.isBlocking) blockingTasksInBuffer.incrementAndGet()
        if (bufferSize == BUFFER_CAPACITY - 1) return task
        val nextIndex = producerIndex.value and MASK
        while (buffer[nextIndex] != null) {
            Thread.yield()
        }
        buffer.lazySet(nextIndex, task)
        producerIndex.incrementAndGet()
        return null
    }
}
```



### （withContext）挂起：

以协程中的一个挂起函数为例：withContext{ }

源码：

```kotlin
public suspend fun <T> withContext(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return suspendCoroutineUninterceptedOrReturn sc@ { uCont ->
       
        if (newContext[ContinuationInterceptor] == oldContext[ContinuationInterceptor]) {
            // 调度器没有改变的话走这里
            val coroutine = UndispatchedCoroutine(newContext, uCont)
            // There are changes in the context, so this thread needs to be updated
            withCoroutineContext(coroutine.context, null) {
                return@sc coroutine.startUndispatchedOrReturn(coroutine, block)
            }
        }
        // 调度器与上级协程不一样就会构建新的协程， uCont就是上级协程
        val coroutine = DispatchedCoroutine(newContext, uCont)
        // 启动协程                                             
        block.startCoroutineCancellable(coroutine, coroutine)
                                                      
        // 会通过CAS修改协程的状态，通过trySuspend尝试挂起，若成功则返回COROUTINE_SUSPENDED标记，挂起上级协程
        coroutine.getResult()
    }
}
```



### （withContext）恢复：

block.startCoroutineCancellable(coroutine, coroutine) 源码：

```kotlin
// 我们眼熟的看到，这个方法就是上文介绍的，启动协程流程，的目的是：
// 1.构建SuspendLambda的子类，并将协程封装在其中；
// 2.构建DispatchedContinuation，并调用其中的resumeCancellableWith方法就是为了调度或者执行invokeSuspend()
internal fun <R, T> (suspend (R) -> T).startCoroutineCancellable(
    receiver: R, completion: Continuation<T>,
    onCancellation: ((cause: Throwable) -> Unit)? = null
) =
runSafely(completion) {
    createCoroutineUnintercepted(receiver, completion).intercepted().resumeCancellableWith(Result.success(Unit), onCancellation)
}
```

由于例子中的调度器是IO，那么就必然走到DefaultScheduler调度器中，接着DispatchedTask的run方法 最终走在SuspendLambda父类BaseContinuationImpl的resumeWith()

```kotlin
internal abstract class BaseContinuationImpl(
    public val completion: Continuation<Any?>?
) : Continuation<Any?>, CoroutineStackFrame, Serializable {
    public final override fun resumeWith(result: Result<Any?>) {
        var current = this
        var param = result
        while (true) {
            probeCoroutineResumed(current)
            with(current) {
                val completion = completion!! // fail fast when trying to resume continuation without completion
                val outcome: Result<Any?> =
                try {
                    // withContext的尾随闭包在这里被调用，例子中代码并没有继续嵌套挂起函数，就不会返回COROUTINE_SUSPENDED，那么会往下执行
                    val outcome = invokeSuspend(param)
                    if (outcome === COROUTINE_SUSPENDED) return
                    // 保存闭包中的运行结果
                    Result.success(outcome)
                } catch (exception: Throwable) {
                    Result.failure(exception)
                }
                releaseIntercepted() // this state machine instance is terminating
                if (completion is BaseContinuationImpl) {
                    current = completion
                    param = outcome
                } else {
                    // completion就是DispatchedCoroutine，会调用父类AbstractCoroutine的resumeWith()
                    completion.resumeWith(outcome)
                    return
                }
            }
        }
    }

    // 在SuspendLambda子类中重写
    protected abstract fun invokeSuspend(result: Result<Any?>): Any?
    ...
    ...
}
```

`AbstractCoroutine`的resumeWith()

```kotlin
public final override fun resumeWith(result: Result<T>) {
    val state = makeCompletingOnce(result.toState())
    if (state === COMPLETING_WAITING_CHILDREN) return
    // 在子类ScopeCoroutine中有重写
    afterResume(state)
}
```

`ScopeCoroutine`的afterResume()

```kotlin
override fun afterResume(state: Any?) {
    // uCont是上级协程SuspendLambda的子类，调用它的resumeWith()从而使得调用invokeSuspend，这样就恢复了协程，让协程进入下一个状态
    uCont.resumeWith(recoverResult(state, uCont))
}
```

