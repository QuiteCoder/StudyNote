# java数据结构



## 一、集合框架

循环数组要比链表更高效;

循环数组是一个有界集合， 即容量有限;

如果程序中要收集的对象数量没有上限， 就最好使用链表来实现。

​		元素被访问的顺序取决于集合类型。 如果对 ArrayList 进行迭代， 迭代器将从索引 0开 始，每迭代一次，索引值加 1。

​		HashSet 中的元素， 每个元素将会按照某种随机的次序出现。虽然可以确定在迭代过程中能够遍历到集合中的所有元素，但却无法预知元素被访问的次序

## 二、链表 LinkedList

​		数组和数组列表都有一个重大的缺陷。这就是从数组的中间位置删除一个元素要付出很大的代价，其原因是数组中处于被删除元素之后的所有元素都要向数组的前端移动。在数组中间的位置上插入一个元素也是如此。

​		链表（ linked list) 解决了这个问题。尽管数组在连续的存储位置上存放对象引用， 但链表却将每个对象存放在独立的结点中。每个结点还存放着序列中下一个结点的引用。在 Java 程序设计语言中，所有链表实际上都是双向链接的(doubly linked)—即每个结点还存放着指向前驱结点的引用。

​		从链表中间删除一个元素是一个很轻松的操作， 只需更新被删除元素附近的链接。

**总结：增减元素效率高。**



## 三、数组列表ArrayList

ArrayList 封装了一个动态再分配的对象数组。

ArrayList : 线程不同步的

Vector ：线程同步的，在同步操作上耗费大量时间，因此效率不高



## 四、散列表HashSet

特点：无序的，可以快速地査找所需要的对象，没有重复的元素

注意点：尽可能的初始化集合的容量来提高性能

原理：散列表用链表数组实现。每个列表被称为桶（ **bucket**)， 要想査找表中对象的位置， 就要先计算它的散列码， 然后与桶的总数取余， 所得到的结果就是保存这个元素的桶的索引。例如， 如果某个对象的散列码为 76268, 并且有 128 个桶， 对象应该保存在第 108 号桶中（76268除以 128余 108 )。

​		很幸运的话， 在这个桶中没有其他元素，此时将元素直接插人到桶中就可以了。 

​		当然，有时候会遇到桶被占满的情况， 这也是不可避免的。这种现象被称为散列冲突（ hash collision) 。这时， 需要用新对象与桶中的所有对象进行比较，査看这个对象是否已经存在。如果散列码是合理且随机分布的， 桶的数目也足够大， 需要比较的次数就会很少。

​		在 JavaSE 8 中， 桶满时会从链表变为平衡二叉树。如果选择的散列函数不当， 会产生很多冲突，或者如果有恶意代码试图在散列表中填充多个有相同散列码的值， 这样就能提高性能。

​		如果大致知道最终会有多少个元素要插人到散列表中， 就可以设置桶数。通常， 将桶数设置为预计元素个数的 75% ~ 150%。有些研究人员认为：尽管还没有确凿的证据，但最好将桶数设置为一个素数， 以防键的集聚。标准类库使用的桶数是 2 的幂， 默认值为 16 (为表大小提供的任何值都将被自动地转换为 2 的下一个幂)。当然，并不是总能够知道需要存储多少个元素的， 也有可能最初的估计过低。如果散列表太满， 就需要再散列 （rehashed)。如果要对散列表再散列， 就需要创建一个桶数更多的表，并将所有元素插入到这个新表中，然后丢弃原来的表。装填因子（ load factor) 决定何时对散列表进行再散列。例如， 如果装填因子为 0.75 (默认值)，而表中超过 75%的位置已经填人元素， 这个表就会用双倍的桶数自动地进行再散列。对于大多数应用程序来说， 装填因子为0.75 是比较合理的。散列表可以用于实现几个重要的数据结构。 其中最简单的是 set 类型。set 是没有重复元素的元素集合。set 的 add方法首先在集中查找要添加的对象，如果不存在，就将这个对象添加进去。



## 五、树集TreeSet

特点：有序的，相比与散列表增减元素慢

**TreeSet** 类与散列集十分类似，但是，它比散列集有所改进



# java多线程

## 一、线程状态

线程可以有如下 6 种状态： 

- New (新创建） 
- Runnable (可运行） 
- Blocked (被阻塞） 
- Waiting (等待） 
- Timed waiting (计时等待） 
- Terminated (被终止）

要确定一个线程的当前状态， 可调用 getState 方法。

带有超时参数的方法有：
Thread.sleep 和 Object.wait、Thread.join、 Lock,tryLock 以及 Condition.await 的计时版

------



Thread.sleep(睡眠的时长，单位ms);//作用是让线程睡眠，交出cpu的资源

Thread.Sleep(0)的作用，就是“触发操作系统立刻重新进行一次CPU竞争”。竞争的结果也许是当前线程仍然获得CPU控制权，也许会换成别的线程获得CPU控制权

------

### Object.wait

| 方法名称        | 描述                                                         |
| --------------- | ------------------------------------------------------------ |
| notify()        | 通知一个在对象上等待的线程，使其从wait()返回，而返回的前提是该线程获取到了对象的锁。 |
| notifyAll()     | 通知所有等待在该对象上的线程。                               |
| wait()          | 调用该方法的线程进入WAITING状态，只有等待另外线程的通知或被中断才会返回，需要注意，调用wait()方法后，会释放对象的锁。 |
| wait(long)      | 超时等待一段时间，这里的参数是毫秒，也就是等待长达n毫秒，如果没有通知就超时返回。 |
| wait(long, int) | 对于超时时间更细粒度的控制，可以达到毫秒                     |

------

### Thread.join

[从join方法的源码来看，join方法的本质调用的是Object中的wait方法实现线程的阻塞](https://www.jianshu.com/p/fc51be7e5bc0)

线程A中调用join方法导致其他线程堵塞，当线程A结束时，会唤醒堵塞中的其他线程；

从源码看

> ```java
> //接下来在hotspot的源码中找到 thread.cpp，看看线程退出以后有没有做相关的事情来证明我们的猜想.
> 
> void JavaThread::exit(bool destroy_vm, ExitType exit_type) {
>       assert(this == JavaThread::current(),  "thread consistency check");
>       ...
>       // Notify waiters on thread object. This has to be done after exit() is called
>       // on the thread (if the thread is the last thread in a daemon ThreadGroup the
>       // group should have the destroyed bit set before waiters are notified).
>       ensure_join(this); 
>       assert(!this->has_pending_exception(), "ensure_join should have cleared");
>       ...
> }
>       
> //观察一下 ensure_join(this)这行代码上的注释，唤醒处于等待的线程对象，这个是在线程终止之后做的清理工作，这个方法的定义代码片段如下
>       
> static void ensure_join(JavaThread* thread) {
>       // We do not need to grap the Threads_lock, since we are operating on ourself.
>       Handle threadObj(thread, thread->threadObj());
>       assert(threadObj.not_null(), "java thread object must exist");
>       ObjectLocker lock(threadObj, thread);
>       // Ignore pending exception (ThreadDeath), since we are exiting anyway
>       thread->clear_pending_exception();
>       // Thread is exiting. So set thread_status field in  java.lang.Thread class to TERMINATED.
>       java_lang_Thread::set_thread_status(threadObj(), java_lang_Thread::TERMINATED);
>       // Clear the native thread instance - this makes isAlive return false and allows the join()
>       // to complete once we've done the notify_all below
>       //这里是清除native线程，这个操作会导致isAlive()方法返回false
>       java_lang_Thread::set_thread(threadObj(), NULL);
>       
>       lock.notify_all(thread);//注意这里，调用 lock.notify_all(thread); 唤醒所有等待thread锁的线程
>       
>       // Ignore pending exception (ThreadDeath), since we are exiting anyway
>       thread->clear_pending_exception();
> }
> 
> 
> 
> ```



### lock 与 tryLock

- `LOCK.lock()`: 此方式会始终处于等待中，**即使调用`B.interrupt()`也不能中断，除非线程A调用`LOCK.unlock()`释放锁。**
- `LOCK.lockInterruptibly()`: 此方式会等待，**但当调用`B.interrupt()`会被中断等待**，并抛出`InterruptedException`异常，否则会与`lock()`一样始终处于等待中，直到线程A释放锁。
- `LOCK.tryLock()`: 该处不会等待，**获取不到锁并直接返回false**，去执行下面的逻辑。
- `LOCK.tryLock(10, TimeUnit.SECONDS)`：该处会在10秒时间内处于等待中，但当调用`B.interrupt()`会被中断等待，并抛出`InterruptedException`。10秒时间内如果线程A释放锁，会获取到锁并返回true，否则10秒过后会获取不到锁并返回false，去执行下面的逻辑。





## 二、线程属性

### 线程优先级

注意：如果有几个高优先级的线程没有进入非活动状态， 低优先级的线程**可能**永远也不能执行。

• static void yield( )

函数功能：导致当前执行线程处于让步状态。如果有其他的可运行线程具有至少与此线程同样高

的优先级，那么这些线程接下来会被调度。注意，这是一个静态方法。



### 守护线程

- 可以通过thread.setDaemon(true);将线程转换成为守护线程。
- 守护线程的唯一用途是为其他线程提供服务，例如计时线程。
- 守护线程应该永远不去访问固有资源， 如文件、 数据库，因为它会在任何时候甚至在一个操作的中间发生中断。



## 三、线程同步

### synchronized关键字

声明一个域或者一个方法可以保证同步。



### Volatile关键字

可声明 对象、变量，让编译器知道这参与多线程修改的

**volatile** 关键字为实例域的同步访问提供了一种免锁机制。如果声明一个域为 **volatile** **,**那么编译器和虚拟机就知道该域是可能被另一个线程并发更新的。



### 原子性

java.util.concurrent.atomic 包中有很多类使用了很高效的机器级指令（而不是使用锁） 来保证其他操作的原子性



### 读/写锁  ReentrantReadWriteLock

如果很多线程从一个数据结构读取数据而很少线程修改其中数据的话， 后者是十分有用的。在这种情况下， 允许对读者线程共享访问是合适的。当然，写

者线程依然必须是互斥访问的。

•Lock readLock( )
得到一个可以被多个读操作共用的读锁， 但会排斥所有写操作。 
•Lock writeLock( )
得到一个写锁， 排斥所有其他的读操作和写操作。