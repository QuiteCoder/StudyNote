# Kotlin面试总结

## 一、协程

Kotlin 协程（Coroutines）是 Kotlin 语言中用于简化异步编程的轻量级并发工具。它通过挂起（suspend）和恢复（resume）机制，允许开发者以同步的方式编写异步代码，从而避免回调地狱（Callback Hell）和复杂的线程管理。

以下是 Kotlin 协程的实现与原理的详细说明：

------

### 1. **Kotlin 协程的核心概念**

#### 1.1 挂起函数（Suspend Function）

- 挂起函数是 Kotlin 协程的核心。它通过 `suspend` 关键字标记，表示该函数可以在不阻塞线程的情况下暂停执行，并在稍后恢复。
- 挂起函数只能在协程或其他挂起函数中调用。

```kotlin
suspend fun fetchData(): String {
    delay(1000) // 模拟耗时操作
    return "Data fetched"
}
```

#### 1.2 协程构建器（Coroutine Builders）

- Kotlin 提供了几种协程构建器来启动协程：
  - `launch`：启动一个不会返回结果的协程（类似于 `Thread`）。
  - `async`：启动一个可以返回结果的协程（类似于 `Future`）。
  - `runBlocking`：阻塞当前线程，直到协程执行完毕（主要用于测试或主函数中）。

```kotlin
fun main() = runBlocking {
    val job = launch {
        val data = fetchData()
        println(data)
    }
    job.join() // 等待协程完成
}
```

#### 1.3 协程上下文（Coroutine Context）

- 协程上下文是协程运行的环境，包含以下关键元素：
  - **Dispatcher**：决定协程在哪个线程上运行（如 `Dispatchers.IO`、`Dispatchers.Main`）。
  - **Job**：协程的生命周期控制器，可以取消协程。
  - **CoroutineExceptionHandler**：处理协程中的异常。

```kotlin
fun main() = runBlocking {
    launch(Dispatchers.IO) {
        println("Running on IO thread")
    }
}
```

#### 1.4 结构化并发（Structured Concurrency）

- Kotlin 协程通过结构化并发管理协程的生命周期。父协程可以控制子协程的生命周期，确保资源不会泄漏。
- 当父协程被取消时，所有子协程也会被取消。

```kotlin
fun main() = runBlocking {
    val parentJob = launch {
        launch {
            delay(1000)
            println("Child 1")
        }
        launch {
            delay(2000)
            println("Child 2")
        }
    }
    delay(500)
    parentJob.cancel() // 取消父协程，子协程也会被取消
}
```

------

### 2. **Kotlin 协程的实现原理**

#### 2.1 挂起与恢复机制

- Kotlin 协程的挂起和恢复是通过 **状态机** 实现的。
- 编译器会将挂起函数转换为一个状态机，每个挂起点（如 `delay`）对应一个状态。
- 当协程挂起时，当前状态和局部变量会被保存；当协程恢复时，状态机会从上次挂起的位置继续执行。

#### 示例：挂起函数的编译结果

```kotlin
suspend fun fetchData(): String {
    delay(1000)
    return "Data fetched"
}
```

编译器会将 `fetchData` 转换为类似以下的状态机：

```kotlin
class FetchDataStateMachine : Continuation<String> {
    var state: Int = 0
    var result: String? = null

    override fun resumeWith(result: Result<String>) {
        when (state) {
            0 -> {
                state = 1
                delay(1000, this) // 挂起
            }
            1 -> {
                this.result = "Data fetched"
                // 恢复并返回结果
            }
        }
    }
}
```

#### 2.2 协程调度器（Dispatcher）

- 协程调度器决定协程在哪个线程上运行。
- Kotlin 提供了几种默认调度器：
  - `Dispatchers.Main`：在主线程运行（通常用于 UI 更新）。
  - `Dispatchers.IO`：适用于 IO 密集型任务。
  - `Dispatchers.Default`：适用于 CPU 密集型任务。
  - `Dispatchers.Unconfined`：不限制线程，协程在调用线程启动，在恢复时可能切换到其他线程。



## 3、协程启动模式

DEFAULT：协程创建后，立即开始调度，在调度前如果协程被取消，其将直接进入取消响应的状态。

ATOMIC：协程创建后，立即开始调度，协程执行到第一个挂起点之前不响应取消。

LAZY：只有协程被需要时，包括主动调用协程的start、join或者await等函数时才开始调度，如果调度前就被取消，那么该协程将直接进入异常结束状态（抛出异常）。

UNDISPATCHED：协程创建后立即在当前函数调用栈中执行，直到遇到第一个真正挂起的点。

```kotlin
// 面试题：如果在Dispatchers.IO调度器中的协程，在主线程执行？
val job1 = GlobalScope.launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
        // 立即在主线程调度执行
        println("current Thread = ${Thread.currentThread().name} ")
}
```

输出结果：

```
current Thread = main
```



协程常用的相关API：

GlobalScope，生命周期是porcess级别，即使Activity或Fragment已经被销毁，协程仍然再执行;

MainScope，再Activity中使用，可以再onDestory()中取消协程;

viewModelScope，只能挂在ViewModel中使用，绑定ViewModel的生命周期；

lifecycleScope，只能再Activity、Fragment中使用，会绑定Activity和Fragment的生命周期。



## 4、**协程的作用域构建器：**

runBloking：是常规函数，**阻塞**当前线程来等待子协程的结束。

coroutineScope：是挂起函数，**挂起**函数会释放底层线程用于其他用途，一个子协程挂了，其他子协程会一起挂掉。

supervisorScope：是挂起函数，一个子协程挂了，其他子协程不受影响，继续执行。但是在父作用域中发生错误，全部子协程都会退出。

```kotlin
runBlocking {
    coroutineScope {

    }
    // supervisorScope方式一：构建协程作用域
    supervisorScope {
        
    }
    // supervisorScope方式二：获取一个协程对象，结构化并发编程，精确控制每个协程。
    var supervisorJob = CoroutineScope(SupervisorJob())
    // 开启一个子协程
    supervisorJob.launch {  }
    // 取消协程
    supervisorJob.cancel()
}
```

```kotlin
runBlocking {
        try {
            supervisorScope {
                launch {
                    try {
                        println("The child is sleeping")
                        delay(Long.MAX_VALUE)
                    } finally {
                        println("The child is cancelled")
                    }
                }
                yield() // 让父协程（supervisorScope）让步一下cpu资源，给子协程执行
                println("Throwing an exception form the scope")
                // 如果在父协程发生错误，子协程就会退出
                throw AssertionError()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
```



## 5、协程的异常捕获

使用**CoroutineExceptionHandler**对协程的异常进行捕获

需要满足以下条件：

- 时机：异常是被自动抛出异常的协程所抛出的（使用launch，而不是async时）；
- 位置：在CoroutineScope的CoroutineContext中或者在一个**根协程**（CoroutineScope或者supervisorScope的直接子协程）中。

### （1）**什么是根协程？**

类似GlobalScope、CoroutineScope(Job())、CoroutineScope(SupervisorJob())就是根协程，
他们直接创建的子协程就是直接子协程，handler放在这才能捕获到异常。

```kotlin
runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
                                             println("CoroutineExceptionHandler: $exception")
                                            }
    val job = GlobalScope.launch(handler) {
        throw NullPointerException()
    }
    job.join()


    val customScope = CoroutineScope(Job())
    val job2 = customScope.launch(handler) {
        throw ArithmeticException()
    }
    // 他的异常并不能捕获，因为这个不是SupervisorJob，一个子协程抛出异常，其他子协程会被取消
    val job3 = customScope.launch(handler) {
        throw IndexOutOfBoundsException()
    }
    job2.join()
    job3.join()

    val supervisorJob = CoroutineScope(SupervisorJob())
    val job4 = supervisorJob.launch(handler) {
        throw NullPointerException()
    }
    val job5 = supervisorJob.launch {
        // 这种情况不会被捕获到异常，handler必须配置到根协程的直接子协程中
        launch(handler) {
            throw ArrayIndexOutOfBoundsException()
        }
    }
    job4.join()
    job5.join()
}
```

async启动的协程需要这样捕获异常：

```kotlin
try {
    deferred.await()
} catch (e: Exception) {
    println("catch exception: $e")
}
```



### （2）Android全局异常处理

全局异常处理器可以获取到所有协程未处理的未捕获异常，不过它**不能对异常进行捕获**，虽然**不能阻止程序的crash**，全局异常处理器在程序调试和异常上报等场景中仍然有非常大的用处。

我们需要在classpath下面创建resources/META-INF/services目录,resources与res目录同级，并在其中创建一个名为kotlinx.coroutines.CoroutineExceptionHandler的文件，文件内容就是我们的全局异常处理器的全类名。

```
com.example.studykotlin.GlobalCoroutineException
```

GlobalCoroutineException的实现类：

```kotlin
class GlobalCoroutineException : CoroutineExceptionHandler {
    override val key = CoroutineExceptionHandler

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        Log.e("hpf","Uncaught exception: $exception")
    }
}
```

能够获取的条件：使用launch，根协程的子协程抛出的异常。

```kotlin
// 这种情况抛出的异常不被获取到
runBlocking {
    launch {
        throw ArrayStoreException()
    }
}
```





### （3）异常聚合

当协程的多个子协程因为异常而失败时，一般情况下取第一个异常进行处理。在第一个异常之后发生的所有其他异常，都被**绑定到第一个异常之上**。

```kotlin
val handler = CoroutineExceptionHandler { _, exception ->
       // exception.suppressed.contentToString()可以获取到第一个以外的异常
       println("CoroutineExceptionHandler:$exception  ${exception.suppressed.contentToString()}")}

runBlocking {
    val job = GlobalScope.launch(handler) {
        launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                throw NullPointerException()
            }
        }
        launch {
            delay(100)
            throw IndexOutOfBoundsException()
        }
    }
    job.join()
}
```

输出结果：

```
CoroutineExceptionHandler:java.lang.IndexOutOfBoundsException  [java.lang.NullPointerException]
```



## 6、冷数据流Flow

StateFlow

SharedFlow



## 7、热数据流Channel

## 8、多路复用

## 9、并发安全



## 问题：

### **（1）launch 和 async的区别？**

launch 示例

```kotlin
val job = GlobalScope.launch {
    // 执行一些工作，但没有返回值
    delay(1000)
    println("Work done!")
}
// 可以取消job或等待它完成
job.join()
```

async 示例

```kotlin
val deferred = GlobalScope.async {
    // 执行一些工作并返回结果
    delay(1000)
    "Result"
}
// 获取结果（会挂起直到结果就绪）
val result = deferred.await()
println(result) // 输出: Result
```

关键点

1. **异常处理差异**：

   - `launch` 的未捕获异常会立即传播并取消父协程
   - `async` 的异常会存储在 `Deferred` 中，只在调用 `await()` 时抛出

2. **结构化并发**：

   - 两者都遵循结构化并发原则
   - 父协程取消时，子协程也会被取消

3. **组合使用**：

   ```kotlin
   val result1 = async { fetchData1() }
   val result2 = async { fetchData2() }
   val combined = result1.await() + result2.await()
   ```

通常，当你需要并行执行多个任务并组合它们的结果时使用 `async`，而只是执行后台任务不需要结果时使用 `launch`。



### **（2）协程调度器Dispatchers的底层实现逻辑？**

总结：

Dispatchers.Main：通过 AndroidDispatcherFactory 构建 HandlerContext，并把mainLooper传进去，最终通过handler.post()的方式完成调度。

Dispatchers.Default：实例化DefaultScheduler 配置 线程池参数，底层由**CoroutineScheduler**实现线程池机制，调度任务。

Dispatchers.IO：由LimitedParallelism对**CoroutineScheduler**的扩展用法，根据任务的最大并行数量，对新任务排队控制。

Dispatchers.Unconfined：isDispatchNeeded()方法直接返回false，不做调度处理，在当前线程执行任务。

```kotlin
public actual object Dispatchers {
    @JvmStatic
    public actual val Main: MainCoroutineDispatcher get() = MainDispatcherLoader.dispatcher
    @JvmStatic
    public val IO: CoroutineDispatcher = DefaultIoScheduler
    @JvmStatic
    public actual val Default: CoroutineDispatcher = DefaultScheduler
    @JvmStatic
    public actual val Unconfined: CoroutineDispatcher = kotlinx.coroutines.Unconfined
}
```

**Dispatchers.Main的创建流程：**

总结：通过 AndroidDispatcherFactory 构建 HandlerContext，并把mainLooper传进去，最终通过handler.post()的方式完成调度。

```kotlin
// 通过 AndroidDispatcherFactory 构建 HandlerContext，并把mainLooper传进去
internal class AndroidDispatcherFactory : MainDispatcherFactory {

    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher {
        val mainLooper = Looper.getMainLooper() ?: throw IllegalStateException("The main looper is not available")
        return HandlerContext(mainLooper.asHandler(async = true))
    }
}
```

Dispatchers.Main进行调度的时候dispatch() 方法，只不过是用handler往主线程Looper中post任务。

```kotlin
internal class HandlerContext private constructor(
    private val handler: Handler,
    private val name: String?,
    private val invokeImmediately: Boolean
) : HandlerDispatcher(), Delay {
   
    constructor(
        handler: Handler,
        name: String? = null
    ) : this(handler, name, false)

    ...

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        // handler内部是mainLooper，所以任务都往主线程放
        if (!handler.post(block)) {
            cancelOnRejection(context, block)
        }
    }
}
```



**Dispatchers.Default创建流程**

总结：实例化DefaultScheduler 配置 线程池参数，底层由coroutineScheduler实现线程池机制，调度任务。

```kotlin
// 指向的就是DefaultScheduler，底层有线程池
internal object DefaultScheduler : SchedulerCoroutineDispatcher(
    CORE_POOL_SIZE, MAX_POOL_SIZE, // 仅仅配置线程池的参数
    IDLE_WORKER_KEEP_ALIVE_NS, DEFAULT_SCHEDULER_NAME
) {
    ...
}
```

```kotlin
internal open class SchedulerCoroutineDispatcher(
    private val corePoolSize: Int = CORE_POOL_SIZE,
    private val maxPoolSize: Int = MAX_POOL_SIZE,
    private val idleWorkerKeepAliveNs: Long = IDLE_WORKER_KEEP_ALIVE_NS,
    private val schedulerName: String = "CoroutineScheduler",
) : ExecutorCoroutineDispatcher() {

    override val executor: Executor
    get() = coroutineScheduler

    // This is variable for test purposes, so that we can reinitialize from clean state
    private var coroutineScheduler = createScheduler()

    private fun createScheduler() =
    CoroutineScheduler(corePoolSize, maxPoolSize, idleWorkerKeepAliveNs, schedulerName)

    // 任务会调度到线程池中
    override fun dispatch(context: CoroutineContext, block: Runnable): Unit = coroutineScheduler.dispatch(block)
}
```



**Dispatchers.IO的创建流程：**

总结：由limitedParallelism对coroutineScheduler的扩展用法，根据任务的最大并行数量，对新任务排队控制。

```kotlin
// 直接指向DefaultIoScheduler
internal object DefaultIoScheduler : ExecutorCoroutineDispatcher(), Executor {
    // default就是LimitedDispatcher
    private val default = UnlimitedIoScheduler.limitedParallelism(
        // systemProp这个方法是jvm根据CPU核心数计算的最大并行任务数量
        systemProp(
            IO_PARALLELISM_PROPERTY_NAME,
            64.coerceAtLeast(AVAILABLE_PROCESSORS)
        )
    )
    
    ...
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        default.dispatch(context, block)
    }
}
```

```kotlin
public open fun limitedParallelism(parallelism: Int): CoroutineDispatcher {
    parallelism.checkParallelism()
    // this 就是 UnlimitedIoScheduler，当调用LimitedDispatcher的dispatch(),也会调用的UnlimitedIoScheduler的dispatch()
    return LimitedDispatcher(this, parallelism)
}
```

```kotlin
private object UnlimitedIoScheduler : CoroutineDispatcher() {

    @InternalCoroutinesApi
    override fun dispatchYield(context: CoroutineContext, block: Runnable) {
        DefaultScheduler.dispatchWithContext(block, BlockingContext, true)
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        // 最终会走到coroutineScheduler中
        DefaultScheduler.dispatchWithContext(block, BlockingContext, false)
    }
}
```



**Dispatchers.Unconfined的创建流程：**

总结：isDispatchNeeded()方法直接返回false，不做调度处理，在当前线程执行任务。

```kotlin
internal object Unconfined : CoroutineDispatcher() {

    // 在调用dispatch()之前会判断isDispatchNeeded()是否返回true，否则不进行调度，在当前线程执行任务
    override fun isDispatchNeeded(context: CoroutineContext): Boolean = false

  
}
```

```kotlin
// DispatchedContinuation
verride fun resumeWith(result: Result<T>) {
        val context = continuation.context
        val state = result.toState()
    	// 调度器Dispatchers.Unconfined，isDispatchNeeded()直接返回false
        if (dispatcher.isDispatchNeeded(context)) {
            _state = state
            resumeMode = MODE_ATOMIC
            dispatcher.dispatch(context, this)
        } else {
            // 将任务进行缓存，串行执行任务
            executeUnconfined(state, MODE_ATOMIC) {
                withCoroutineContext(this.context, countOrElement) {
                    continuation.resumeWith(result)
                }
            }
        }
    }
```



## 二、内联的用途和实现原理

Kotlin 中的**内联函数**（Inline Functions）是一种优化技术，主要用于减少高阶函数（Higher-Order Functions）带来的运行时开销。通过将函数体直接插入到调用处，内联函数可以避免创建额外的匿名类和对象，从而提高性能。

以下是 Kotlin 内联函数的详细说明：

------

### **内联函数的作用**

在 Kotlin 中，高阶函数（即接受函数作为参数或返回函数的函数）会带来一定的运行时开销，因为每个函数参数都会被编译成一个匿名类的实例。内联函数通过将函数体直接复制到调用处，避免了这种开销。



## 三.kotlin 内联inline、crossinline、noinline的区别

在 Kotlin 中，`inline`、`crossinline` 和 `noinline` 是用于修饰高阶函数中 lambda 表达式的关键字，它们的作用如下：

### 1. `inline`

`inline` 关键字用于修饰高阶函数，表示该函数的 lambda 参数会被内联到调用处。内联可以减少函数调用的开销，尤其是在 lambda 表达式频繁使用时。

- **优点**：
  - 减少函数调用的开销。
  - 允许在 lambda 中使用 `return` 直接返回外层函数（非局部返回）。
- **缺点**：
  - 内联函数的代码会被复制到每个调用处，可能导致生成的字节码变大。

```kotlin
inline fun doSomething(action: () -> Unit) {
    println("Before action")
    action()
    println("After action")
}

fun main() {
    doSomething {
        println("Action performed")
    }
}
```

### 2. `crossinline`

`crossinline` 用于修饰内联函数中的 lambda 参数，表示该 lambda 不允许非局部返回（即不能在 lambda 中使用 `return` 直接返回外层函数）。

- **使用场景**：
  - 当 lambda 表达式会被传递给另一个执行上下文（如另一个函数或异步任务）时，防止非局部返回。

```kotlin
inline fun doSomething(crossinline action: () -> Unit) {
    println("Before action")
    action()
    println("After action")
}

fun main() {
    doSomething {
        println("Action performed")
        // return  // 这里会报错，因为使用了 crossinline
    }
}
```

### 3. `noinline`

`noinline` 用于修饰内联函数中的 lambda 参数，表示该 lambda 不会被内联。

- **使用场景**：
  - 当某个 lambda 参数不需要内联时（例如，lambda 会被传递给另一个非内联函数），可以使用 `noinline` 来避免内联。

```kotlin
inline fun doSomething(action1: () -> Unit, noinline action2: () -> Unit) {
    println("Before action1")
    action1()
    println("After action1")
    someOtherFunction(action2)  // action2 不会被内联
}

fun someOtherFunction(action: () -> Unit) {
    action()
}

fun main() {
    doSomething(
        { println("Action1 performed") },
        { println("Action2 performed") }
    )
}
```

### 总结

- **`inline`**：内联函数，减少调用开销，允许非局部返回。
- **`crossinline`**：禁止 lambda 中的非局部返回，通常用于 lambda 会被传递给其他执行上下文的情况。
- **`noinline`**：禁止某个 lambda 参数的内联，通常用于 lambda 会被传递给非内联函数的情况。



## 四、whit、run、let、apply、also

## 核心区别总结

| 函数    | 上下文对象引用 | 返回值     | 是否扩展函数 | 主要用途      |
| :------ | :------------- | :--------- | :----------- | :------------ |
| `let`   | `it`           | 代码块结果 | 是           | 非空处理/转换 |
| `run`   | `this`         | 代码块结果 | 是           | 对象配置+计算 |
| `with`  | `this`         | 代码块结果 | 否           | 集中操作对象  |
| `apply` | `this`         | 对象本身   | 是           | 对象初始化    |
| `also`  | `it`           | 对象本身   | 是           | 附加操作      |

## 详细解析

### 1. `let`

**特点**：

- 通过 `it` 引用对象
- 返回代码块的最后一行结果
- 是扩展函数

**典型使用场景**：

```kotlin
// 1. 非空检查
val length = nullableString?.let { 
    println("Not null: $it")
    it.length  // 返回值
} ?: 0

// 2. 转换对象
val userDto = user.let { 
    UserDto(it.name, it.age) 
}
```

### 2. `run`

**特点**：

- 通过 `this` 引用对象（可省略）
- 返回代码块的最后一行结果
- 是扩展函数

**典型使用场景**：

```kotlin
// 1. 对象配置+计算
val result = service.run {
    port = 8080
    query()  // 返回值
}

// 2. 替代无接收者的代码块
val hex = run {
    val digits = "0-9A-F"
    //...复杂计算
    result.toString(16)
}

// 空安全检查
val nullableString: String? = "Hello"
val result = nullableString?.run {
    println("非空值: $this")
    length
} ?: 0
```

### 3. `with`

**特点**：

- 通过 `this` 引用对象（可省略）
- 返回代码块的最后一行结果
- 不是扩展函数

**典型使用场景**：

```kotlin
val person = Person()
with(person) {
    name = "Alice"
    age = 30
    // 集中配置对象
    toString()  // 可选返回值
}
```

### 4. `apply`

**特点**：

- 通过 `this` 引用对象（可省略）
- 返回对象本身
- 是扩展函数

**典型使用场景**：

```kotlin
// 对象初始化
val dialog = AlertDialog.Builder(context).apply {
    setTitle("Title")
    setMessage("Message")
    setPositiveButton("OK") { _, _ -> }
}.create()
```

### 5. `also`

**特点**：

- 通过 `it` 引用对象
- 返回对象本身
- 是扩展函数

**典型使用场景**：

```kotlin
// 附加操作
val file = File("test.txt").also {
    println("Creating file: ${it.path}")
    check(!it.exists()) { "File already exists" }
}.also {
    it.createNewFile()
}
```

## 选择指南

1. **需要返回值吗？**
   - 需要返回值：`let`、`run`、`with`
   - 不需要返回值（返回对象本身）：`apply`、`also`
2. **如何引用对象？**
   - 偏好 `this`（直接访问成员）：`run`、`with`、`apply`
   - 偏好 `it`（显式引用）：`let`、`also`
3. **具体场景**：
   - 非空检查：`let`
   - 对象初始化：`apply`
   - 链式调用中的附加操作：`also`
   - 复杂计算：`run`
   - 集中操作已有对象：`with`

## 记忆技巧

- **A**pply 和 **A**lso 都返回对象本身（A开头）
- **L**et 和 **R**un 都返回代码块结果（L/R开头）
- With 是唯一非扩展函数
- 带i的函数(it)有：let、also
- 带t的函数(this)有：run、with、apply