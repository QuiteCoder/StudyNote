# java面试总结



## 1、String、StringBuilder、SringBuffer的区别？

- String底层使用 **final** 修饰的char[ ]， 每次都要新创建新的对象，频繁触发GC，效率最低，线程安全；

- StringBuilder底层使用char[ ]，是为了解决String不能拼接的问题，线程不安全；

- `StringBuffer`和`StringBuiler`之间的最大不同在于**`StringBuilder`的方法不是线程安全的**。

  由于`StringBuilder`相比较于`StringBuffer`有速度优势，所以多数情况下建议使用`StringBuilder`类。而在应用程序中要求线程安全的情况下，则必须使用`StringBuffer`类。

- `StringBuffer`为什么线程安全？

```java
//拼接字符串用到同步锁
public synchronized StringBuffer append(String str) {
    toStringCache = null;
    super.append(str);
    return this;
}
```





## 2、ArrayList 、ArrayMap、 HashMap区别

- ArrayList是数组结构，线程不安全；

- ArrayMap线程不安全，双数组结构，一个数组记录key的hash值，另外一个数组记录Value值，它和SparseArray一样，也会对key使用二分法进行从小到大排序，在添加、删除、查找数据的时候都是先使用二分查找法得到相应的index，然后通过index来进行添加、查找、删除等操作，所以，应用场景和SparseArray的一样，如果在数据量比较大的情况下，那么它的性能将退化至少50%。

- HashMap线程不安全；

- 结构：
  JDK7与JDK8及以后的HashMap结构与存储原理有所不同：
  Jdk1.7：数组 + 链表 ( 当数组下标相同，则会在该下标下使用链表)
  Jdk1.8：数组 + 链表 + 红黑树 (预值为8 如果链表长度 >=8则会把链表变成红黑树 )
  Jdk1.7中链表新元素添加到链表的头结点,先加到链表的头节点,再移到数组下标位置
  Jdk1.8中链表新元素添加到链表的尾结点
  (数组通过下标索引查询，所以查询效率非常高，链表只能挨个遍历，效率非常低。jdk1.8及以
  上版本引入了红黑树，当链表的长度大于或等于8的时候则会把链表变成红黑树，以提高查询效率)

  **存储元素：**以key的hash值在数组中排序，产生hash冲突时，会在发生冲突的链表中遍历，key不为null的前提下，通过equals判断链表是否存在相同key？有则替换整个Node，没有就往链表/红黑树添加Node，JDK1.7插在链表头，JDK1.8插在链表未端；不产生hash冲突继续往数组添加Node。





## 3、ConcurrentHashMap

线程安全的HashMap

参考：[ConcurrentHashMap.md](java/集合/ConcurrentHashMap.md)



## 4、多线程同步有哪些？

共7种方式：原文链接：https://blog.csdn.net/love284969214/article/details/7231246

### **1、同步方法**

即有synchronized关键字修饰的方法。

 代码如： 

```java
public synchronized void save(){}
```

### **2.同步代码块** 

  即有synchronized关键字修饰的语句块。

  代码如： 

```java
  synchronized(object){ }
```

### **3.使用特殊域变量(volatile)实现线程同步**

- volatile关键字为域变量的访问提供了一种免锁机制

- 使用volatile修饰域相当于告诉虚拟机该域可能会被其他线程更新 

- 因此每次使用该域就要重新计算，而不是使用寄存器中的值 

- volatile不会提供任何原子操作，它也不能用来修饰final类型的变量 

代码示例：

```java
//只给出要修改的代码，其余代码与上同
class Bank {
 
    //需要同步的变量加上volatile
    private volatile int account = 100;
 
    public int getAccount() {
        return account;
    }
 
    //这里不再需要synchronized 
    public void save(int money) {
        account += money;
    }
}
```

### **4.使用重入锁实现线程同步**

在JavaSE5.0中新增了一个java.util.concurrent包来支持同步。 
ReentrantLock类是可重入、互斥、实现了Lock接口的锁， 
它与使用synchronized方法和快具有相同的基本行为和语义，并且扩展了其能力

 ReenreantLock类的常用方法有：

​    ReentrantLock() : 创建一个ReentrantLock实例 
​    lock() : 获得锁 
​    unlock() : 释放锁 

 注：ReentrantLock()还有一个可以创建公平锁的构造方法，但由于能大幅度降低程序运行效率，不推荐使用 ；

​		 wait()、notifyAll()也有类似效果，但是只能用于同步方法或同步代码块中；



代码实例： 

```java
//只给出要修改的代码，其余代码与上同
class Bank {            
    private int account = 100;
            
    //需要声明这个锁
    private Lock lock = new ReentrantLock();
    
    public int getAccount() {
        return account;
    }
 
    //这里不再需要synchronized
    public void save(int money) {
        lock.lock();
        try{
            account += money;
        }finally{
            lock.unlock();
        }      
    }
｝
```

注：关于Lock对象和synchronized关键字的选择： 
    a.最好两个都不用，使用一种java.util.concurrent包提供的机制， 
        能够帮助用户处理所有与锁相关的代码。 
    b.如果synchronized关键字能满足用户的需求，就用synchronized，因为它能简化代码 
    c.如果需要更高级的功能，就用ReentrantLock类，此时要注意及时释放锁，否则会出现死锁，通常在finally代码释放锁   

### 5、**使用ThreadLocal管理变量、原子变量、线程安全的数据结构** 

**原子变量：**

AtomicInteger、AtomicBoolean、AtomicLong、AtomicReference



**线程安全数据结构：**

Vector，StringBuffer，HashTable，ConcurrentHashMap，CopyOnWriteArrayList，CopyOnWriteArraySet，ConcurrentLinkedQueue，LinkedBlockingQueue，



## **5、启动线程的方式有？**

第一种：new Thread().start();

第二种：new Thread(Runnable).start();

第三种：通过线程池可以启动一个新的线程，其实线程池也是用的两种方式之一，Executors.newCachedThreadPool()或者FutureTask+Callable



## 6、强、软、弱、虚引用

- **强引用（StrongReference）**垃圾回收器绝不会回收它。当内存空 间不足，Java虚拟机宁愿抛出OutOfMemoryError错误，使程序异常终止，也不会靠随意回收具有强引用的对象来解决内存不足问题。
- **软引用（SoftReference）**如果内存空间足够，垃圾回收器就不会回收它，如果内存空间不足了，就会回收这些对象的内存。只要垃圾回收器没有回收它，该对象就可以被程序使用。软引用可用来实现内存敏感的高速缓存。

- **弱引用（WeakReference）**一旦发现了只具有弱引用的对象，不管当前内存空间足够与否，都会回收它的内存。不过，由于垃圾回收器是一个优先级很低的线程， 因此不一定会很快发现那些只具有弱引用的对象。 
- **虚引用（PhantomReference）**在任何时候都可能被垃圾回收。

软、弱引用可以和一个引用队列（ReferenceQueue）联合使用，如果软引用所引用的对象被垃圾回收，JAVA虚拟机就会把这个软引用加入到与之关联的引用队列中。

软引用和弱引用适合用缓存，Glide内部使用的就是弱引用进行缓存



## 7、单例模式中，饿汉式、懒汉式的区别

（1）线程安全：饿汉式在线程还没出现之前就已经实例化了，所以饿汉式一定是线程安全的。

（2）执行效率：饿汉式没有加任何的锁，因此执行效率比较高。懒汉式一般使用都会加同步锁，效率比饿汉式差。

（3）内存使用：饿汉式在一开始类加载的时候就实例化，无论使用与否，都会实例化，所以会占据空间，浪费内存。懒汉式什么时候用就什么时候实例化，不浪费内存。



## 8、有哪些classLoader

### JVM中的类加载器：

（1）启动类加载器（Bootstrap ClassLoader）用来加载java核心类库，无法被java程序直接引用。
（2）扩展类加载器（extensions class loader）：它用来加载Java的扩展库。Java虚拟机的实现会提供一个扩展库目录。
该类加载器在此目录里面查找并加载 Java类。
（3）系统类加载器（system class loader）也叫应用类加载器：它根据Java应用的类路径（CLASSPATH）来加载Java类。一般来说，Java应用的类都是由它来完成加载的。可以通过ClassLoader.getSystemClassLoader0来获取它。
（4）用户自定义类加载器，通过继承 java.lang.ClassLoader类的方式实现。



### Android中的类加载器：

参考：https://www.jianshu.com/p/f454b2e25e93

- BootClassLoader：主要加载Android Framework层的字节码文件

- PathClassLoader：主要加载已经安装到系统中的apk文件中的字节码文件

- DexClassLoader：主要加载没有安装到系统中的apk，jar文件中的字节码文件

- BaseDexClassLoader：是PathClassLoader和DexClassLoader的父类，真正实现功能的代码都在这个ClassLoader中

### ClassLoader的双亲委托模型

1. 首先查找当前类加载器是否加载过这个类，如果找到则返回这个类

2. 如果找不到，就会调用父类的方法去查找是否加载过这个类，如果父类没有加载过就去查找祖父类加载器是否加载过，按照这样的逻辑一直查找到最上层的类加载器（始祖类加载器），因为始祖加载器没有 parent 了，就用引导类加载器去查找。

3. 如果还没有找到就说明这个类 JVM 确实没有加载过，然后尝试使用引导类加载器加载这个类，如果成功则返回类，加载失败就再次尝试使用始祖类加载器，依次类推如果当前类加载器都加载失败则抛出异常

   

**小结：这样做保证的类只会加载一次**



## 9、泛型的面试题

### a、**Java中的泛型是什么？使用泛型的好处是什么？**‌

泛型是JDK5后引入的特性，本质是参数化类型。它提供了编译时类型安全检测机制，只支持引用数据类型。使用泛型的好处包括：

‌**类型安全**‌：将运行时的类型转换问题提前到编译时，避免`ClassCastException`。

**泛型的缺陷**：不支持基本数据类型

总结原因：1、为了兼容旧版本； 2、内存模型设计就只能存储引用类型的泛型数据

| **原因**         | **说明**                                                     |
| :--------------- | :----------------------------------------------------------- |
| 类型擦除机制     | 泛型在编译后擦除为`Object`，无法兼容基本类型的内存布局。     |
| 内存模型限制     | 基本类型与引用类型的内存表示方式不同，无法统一处理。         |
| 代码膨胀风险     | 支持基本类型需为每个类型生成独立类，增加冗余。               |
| 兼容性与设计哲学 | 保持与旧版本兼容，避免JVM核心修改。                          |
| 替代方案的可行性 | 通过包装类（如`Integer`）和专用工具类（如`IntStream`）实现近似功能。 |



### b、**Java泛型是如何工作的？什么是类型擦除？**

泛型是通过类型擦除来实现的。编译器在编译时擦除所有类型相关的信息，所以在运行时不存在任何类型相关的信息。例如，`List<String>`在运行时仅用一个`List`来表示。

> **问题：运行时没有泛型，那么可以往里边添加非String的元素吗？**
>
> 答：可以，添加元素的检查是编译阶段进行的，List<String>的泛型会保存在**符号表**的Signature属性中，编译过程会对元素的类型进行合法性检查，所以插入不合法的类型的元素会在编译期报错。
>
> 另外，即使有幸绕过了编译器成功插入不合法的元素，运行时读取元素时会做第二道检查，通过编译器生成的**桥接方法**，内部使用**checkCast**指令对元素进行类型兼容性检查。
>
> **问题：instanceof运算符跟checkCast指令的关系？**
>
> 答：**instanceof**的底层实现是使用了**checkCast**指令，内部使用较为复杂的算法，对两者的实现方法、遍历super链（继承链）一直到Object，进行类型对比。



###  c、类型擦除的作用？

1、确保与Java 5之前的版本兼容。

2、为了节省运行时内存，减少方法区的内存压力。



### d、**什么是限定通配符和非限定通配符？**‌

限定通配符对类型进行了限制。有两种限定通配符：`<? extends T>`和`<? super T>`。

`<? extends T>`通过确保类型必须是T的子类来设定类型的上界；

`<? super T>`通过确保类型必须是T的父类来设定类型的下界。非限定通配符`<?>`表示可以用任意类型来替代‌。



### e、**List<?>、List和List<? extends Object>之间有什么区别？**‌

- ‌**List<?>**‌：可以包含任何类型元素的列表，但不能添加元素，只能读取。

```java
public static void main(String[] args) {
        ArrayList<?> objects = new ArrayList<>(Arrays.asList("Apple", "Banana", "Orange"));
        for (int i = 0; i <objects.size(); i++) {
            // ArrayList<?>，ArrayList<? extends xxx> 这两种写法只能get，add会编译报错
            Object o = objects.get(i);
            System.out.println(o.toString());
        }
 }
```



- ‌**List<Object>**‌：可以包含任何Object类型的元素，可以自由添加任何Object类型的元素。
- ‌**List<? extends Object>**&zwnj;：可以包含任何Object子类类型的元素，类似于List<?>，但明确表示了Object的上界。实际应用中，`List<?>`不能添加元素，`List<Object>`可以添加任何对象，而`List<? extends Object>`可以读取但不能写入‌。



### f、**Java泛型中的协变与逆变有什么区别？**‌

- ‌**协变（extends T）**‌：允许泛型类型成为特定类型T的子类型。通常在你想从结构中读取而不修改时使用。例如，`List<? extends Number>`可以接受`List<Integer>`或`List<Double>`。
- ‌**逆变（super T）**‌：允许泛型类型成为特定类型T的超类型。当你想写入结构时使用。例如，`List<? super Integer>`可以接受`List<Integer>`、`List<Number>`或`List<Object>`。

```java
// 只能重新接收子类泛型的ArrayList对象，不能add元素，可以get元素
ArrayList<? extends Honda> objects2 = new ArrayList<Honda>(Arrays.asList(new Honda("1")));
objects2 = new ArrayList<Acura>(Arrays.asList(new Acura("1")));
// 不能接收父类类型的ArrayList对象
objects2 = new ArrayList<Car>(Arrays.asList(new Car("1"))); // 这句代码会编译报错
// 也不能add元素，<? extends T>的泛型都一样
objects2.add(new Acura("2")); // 这句代码会编译报错
for (int i = 0; i < objects2.size(); i++) {
    Object o = objects2.get(i);
    System.out.println(o.toString());
}
```



## 10、synchronized、volatile、final的原理

JVM规范中定义了一种Java内存 模型（Java Memory Model，即JMM）来屏蔽掉各种硬件和操作系统的内存访问差异，以实现让Java程序在各种平台下都能达到一致的并发效果。Java内存模型的主要目标就是**定义程序中各个变量的访问规则，即在虚拟机中将变量存储到内存和从内存中取出变量这样的细节**。

线程 --> 工作内存 -- （save 或者 load 方法）--> 主内存

**volatile 的实现原理:**

1. 当一个线程修改一个volatile变量的值时,它会在变量修改后立即刷新回主内存（堆内存）。
2. 当一个线程读取一个volatile变量的值时,它会直接从主内存读取,而不是从工作内存读取。
3. 它会在读后和写前加入内存屏障,以保证指令重排不会将内存操作重排到屏障另一侧。

volatile关键字主要保证**可见性、有序性（禁止指令重排）**，在DCL单例中有对单例修饰。

volatile**不能保证原子性**,要配合synchronized或Atomic类解决。

> **问题：工作内存、堆、主存的关系？**
>
> 工作内存：jvm的层面来说，是线程的私有缓存区域，硬件层面是cpu的L1、L2、L3级缓存。
>
> 主存：主内存是 JMM 中的抽象概念，对应物理硬件的**全局内存（RAM）**，是所有线程共享的最终数据存储位置。
>
> 堆：属于主存的一部分，是划分给JVM的内存区域，volatile修饰的变量在被修改时，相当于当前线程通过内存屏障对堆内存的直接修改。



**final 关键字的原理**

由于final关键字修饰的变量只能赋值一次，如果定义在类中的全局变量，那么会随着类的实例化在构造函数中就被初始化了。

如果想对final类型的变量二次赋值，编译器会报错，保证数据的可见性。

还可以禁止final关键字修饰指令重排序，导致初始化变量结果不正确的问题。

> 参考：https://blog.csdn.net/riemann_/article/details/96390511



**synchronized** 的实现原理：参考：https://www.cnblogs.com/aspirant/p/11470858.html

前置知识点：对象在内存中的状态是什么样？

对象实例存在堆内存中，一个完整的对象包括3部分：

a.对象头（hashcode，Mark word.）

b.对象数据实例（包含类中声明字段，而方法存放在方法区，不属于对象数据）

c.对齐数据（64位jvm要求对象的总大小是8Byte的倍数，32位系统对应4Byte的倍数，如果不够就开辟空间来补齐）

数据对齐有什么优点：（待补充）



Synchronized总共有三种用法：

```java
public class Main {
    static Car car = new Car("123");
    // 就算是静态变量也存在并发问题
    static int i = 1;
    // 1.当synchronized作用在实例方法时，监视器锁（monitor）便是对象实例（this）
    public synchronized void doSomething() {
        i++;
    }

    // 2.当synchronized作用在静态方法时，监视器锁（monitor）便是对象的Class实例，因为Class数据存在于永久代，因此静态方法锁相当于该类的一个全局锁
    public synchronized static void doSomething2() {
        i++;
    }

    public void doSomething3() {
        // 3.当synchronized作用在某一个对象实例时，监视器锁（monitor）便是括号括起来的对象实例:car
        synchronized (car) {
            i++;
        }
    }
}
```

字节码显示：被synchronized修饰的方法或者代码块存在：**monitorEnter**，**monitorExit** 一对指令。



### 锁升级（锁膨胀）

在对象刚被创建时，处于无状态（对象状态：无状态->偏向锁->轻量锁->重量锁），对象头并没有Monitor的地址数据。

当对象存在关键字synchronized标记时，或者new对象4秒钟之后，锁状态从无状态标记为**偏向锁，**单条线程访问时只需要记录线程ID到mark word，访问速度较快。

当访问线程数达到两条或者以上，如T2加入，T2通过CAS（CompareAndSet）修改对象头中的线程ID失败，锁状态变为**轻量级锁**（也可以称为：自旋锁，JDK1.6引入自适应自旋锁），这时候T1还未释放锁，T2线程还想获取锁时，会触发自旋锁，线程自旋（类似while(ture)）申请锁，循环一定的次数（次数由jvm设定）后依然获取锁失败，T2修改对象头中的**重量级锁指针**，指向ObjectMonitor，

> **偏向锁出现的时机：**
>
> 1、被synchronized修饰时；
>
> 2、某些jvm默认延时4s自动开启偏向锁，对象被new出来4秒后自动被标记启用偏向锁，可通过 -XX:BiasedLockingStartupDelay = 0取消延时；如果不要偏向锁，可通过-XX:-UseBiasedLocking = false 来设置
>
> **轻量级锁出现的时机：**如果对象被new出来时就自带偏向锁，那么被synchronized修饰时就升级为轻量级锁
>
> **ObjectMonitor创建的时机：**在T2自旋获取锁失败后。

对象一个Monitor与之关联，在对象头**Mark Word**中的重量级锁指针保存Monitor的地址，具体是由**ObjectMonitor**实现的

```java
ObjectMonitor() {
    _count        = 0; // 记录访问线程重入锁的次数，被synchronized修饰的方法有可能嵌套调用
   ...
    _owner        = NULL; // 记录当前获取到锁的线程
    _WaitSet      = NULL; // 获得锁但是处于wait状态的线程，会被加入到_WaitSet，接着释放锁，_count 减 1
   ...
    _EntryList    = NULL ; // 尚未获得锁，处于等待锁block状态的线程，会被加入到该列表等待
   ...
  }
```



当线程访问到monitorEnter时，ObjectMonitor的`_count` 加1，相同线程**可重入锁**，`_count` 继续加1，

线程访问同步代码块结束时 `_count`减1，当线程完全执行完所有同步代码时，_count = 0。

进入_EntryList的线程都会被Blocked，jvm通过Parker.cpp对线程进行

> 注意：ObjectMonitor底层使用pthread_mutex_t（互斥量）和pthread_cond_t（条件变量）对线程的blocked，
>
> 跟ReentrantLock中的park()方法的最低层实现一样。



**线程栈：**

- **同一概念的不同视角**：
  - **JVM栈**：从JVM内存模型的角度描述，属于规范层面的术语。
  - **线程栈**：从线程私有性的角度描述，强调其归属于特定线程。
- **每个线程对应一个JVM栈**：所有线程的JVM栈共同构成JVM的栈内存区域。

每一个线程都有一个Lock Record列表，记录对象的mark word，想获取锁的时候需要与mark word记录的线程ID做比较，

如果发现mark word中的线程ID不是自己，并且当前处于偏向锁，那么就升级轻量级锁。



## 11.ReentrantLock的底层原理

关键点：CAS锁机制，park()的原理，公平锁，非公平锁，

### a、CAS锁机制

全称：Compare And Swap 对比和交换，底层由Unsafe.cpp实现，Linux根据不同的操作系统调用不同的汇编指令进行加锁操作，

从而实现对状态的原子性修改操作。

例如Unsafe 的 CAS的用法：

```java
public final class Unsafe {   

    // 在AtomicInteger中使用，会消耗cpu让线程死循环等待CAS，如果修改数据成功，则返回V，否则一直死循环
    public final int getAndAddInt(Object o, long offset, int delta) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, v + delta));
        return v;
    }

	// jvm底层根据不同cpu平台使用不同的汇编指令进行上锁和修改操作
    public final native boolean compareAndSwapInt(Object o, long offset,
                                                  int expected,
                                                  int x);
}
```

> **误区：**CAS自身不会自旋，没有循环获取锁的功能



### b、自旋锁（spinlock）：

jvm中的自旋锁是由汇编指令实现，包括了循环+CAS的机制

在java层，Unsafe（do{ } while( )）内部都有循环逻辑的实现

> **注意：**自旋锁不适合给耗时操作加锁，例如给I\O操作上自旋锁，这样会导致cpu空转，浪费资源



### c、park()的原理

`park()` 是 Java 并发编程中 `LockSupport` 类提供的核心方法，用于阻塞当前线程，跟 `unpark()`配合使用，

park是等待一个许可，unpark是为某线程提供一个许可。如果某线程A调用park，那么除非另外一个线程调用unpark(A)给A一个许可，否则线程A将阻塞在park操作上。每次调用一次park，需要有一个unpark来解锁。

其底层是unsafe.cpp实现的，每个线程都会关联一个自己的 Parker 对象 。

```cpp
class Parker : public os::PlatformParker {  
private:  
  volatile int _counter ;  // 当park时，这个变量被设置为0，当unpark时，这个变量被设置为1
  ...  
public:  
  void park(bool isAbsolute, jlong time);  
  void unpark();  
  ...  
}  
class PlatformParker : public CHeapObj<mtInternal> {  
  protected:  
    pthread_mutex_t _mutex [1] ;  // 当park时，阻塞线程，线程状态变为 Blocked
    pthread_cond_t  _cond  [1] ;  
    ...  
}
```



### d.公平锁和非公平锁

公平锁： 在尝试获取锁时（`tryAcquire()` 方法）会检查等待队列是否为空。若队列非空，则当前线程直接进入队列等待，接着自旋一次判断是否头节点，是，继续CAS获取锁，否则在队列中等上一线程释放锁后调用unpark唤醒。

非公平锁：在尝试获取锁时（`tryAcquire()` 方法）会直接尝试 CAS 操作，如果修改state成功，则直接获取到锁（插队成功），若失败则进入队列等待，排到队列的尾部，接着自旋一次判断是否头节点，是，继续CAS获取锁，否则在队列中等上一线程释放锁后调用unpark唤醒。



| **特性**       | **公平锁**                       | **非公平锁**                       |
| :------------- | :------------------------------- | :--------------------------------- |
| **锁分配顺序** | 严格按请求顺序分配               | 允许新线程插队（可能抢占成功）     |
| **吞吐量**     | 较低（频繁唤醒线程）             | 较高（减少线程切换）               |
| **线程饥饿**   | 完全避免                         | 可能发生（某些线程长期无法获取锁） |
| **适用场景**   | 需要严格公平性（如资源按序分配） | 追求高吞吐量（如大多数并发场景）   |



## 12.AtomicInteger

内部使用**Volatile**修饰成员变量int i , 只保证了数据的可见性，会出现ABA的问题，可以使用AtomicStampedReference（不但会判断原值，还会比较版本信息）

使用Unsafe类，实现**CAS锁**机制，保证数据的原子性。





## 13、方法参数传递问题

```java
public static void main(String[] args) {
    String a = "1";
    int b = 1;
    Integer c = new Integer(2);
    StringBuilder sb = new StringBuilder("abc");
    changeA(a);
    changeB(b);
    changeC(c);
    changeSB(sb);
    System.out.println("a = " + a);
    System.out.println("b = " + b);
    System.out.println("c = " + c);
    System.out.println("sb = " + sb);
}
private static void changeA(String a) {
    a = "10";
}
private static void changeB(int b) {
    b = 10;
}
private static void changeC(Integer c) {
    c = 2;
}
private static void changeSB(StringBuilder sb) {
    sb.append("d");
}
```

```
输出结果：
a = 1
b = 1
c = 1
sb = abcd
```

总结：

String，int，Integer等包装类具有不变性，方法传递进来这些数据类型不会将他们的地址传递进来，所以修改不了他们的值，而StringBuilder是对象引用，传递到方法中也是引用的传递，可以修改原对象的值。





## 14.ThreadLocal内存泄露

基本使用：

```java
public class RequestContextHolder {
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    public static void setUser(User user) {
        currentUser.set(user);
    }

    public static User getUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
```

```java
public class ThreadLocal {
    ..
    public void remove() {
        // 类似Map的数据据结构
        ThreadLocalMap m = getMap(Thread.currentThread());
        if (m != null)
            // 如果key被gc回收就无法清除ThreadLocalMap中的元素
            m.remove(this);
    }
}
```

每条线程中都保存ThreadLocalMap对象，**key**是ThreadLocal对象，使用弱引用存储，内存不足时会被回收，**value**是强引用，线程没有销毁都不会被回收。

出现内存泄露的情况：
当线程执行完毕后，回到线程池，key被gc了，value还在，已经没办法通过remove()方法移除value





## 15、线程池（代补充）

[线程池]: E:\StudyNote\多线程\多线程.md





## 16、java中char字符怎么储存在内存？

前置知识：java为了完美兼容跨平台，使用的**Unicode**字符集，兼容了ASCII， char 类型的长度为 2 个字节（16 位）,使用**UTF-16的**编码方式储存二进制数据。

**ASCII**（发音： [/ˈæski/](https://zh.wikipedia.org/wiki/Help:英語國際音標) [*ASS-kee*](https://zh.wikipedia.org/wiki/Wikipedia:發音重拼)[[1\]](https://zh.wikipedia.org/wiki/ASCII#cite_note-1)，**A**merican **S**tandard **C**ode for **I**nformation **I**nterchange，**美国信息交换标准代码**



**问题一：为什么不用UTF-8进行存储？**

JVM 中 char 类型的长度为 2 个字节（16 位），这是 Java 选择 UTF-16 的直接反映。相比 UTF-8 这种可变长度的编码方式，UTF-16 的定长特性让 JVM 在处理字符串时更容易实现高效的内存访问和操作。假设 JVM 采用 UTF-8 编码，由于 UTF-8 的每个字符可能占用 1 到 4 个字节，这就意味着 JVM 在处理字符串时必须为每个字符动态计算其偏移量。相比之下，UTF-16 的定长特性允许 JVM 直接通过字符索引快速定位和访问字符串中的任意字符，极大地简化了字符串的操作逻辑。



**问题二：Char 和 String的关系？**

**类型本质与存储结构**

- **`char`**

  - **基本数据类型**：表示单个16位Unicode字符（范围：`\u0000` ~ `\uFFFF`）。
  - **存储方式**：直接存储字符的Unicode值，如 `char c = 'A';`（对应Unicode码点`U+0041`）。

- **`String`**

  - **对象类型**：表示不可变的字符序列。

  - **内部实现**：JDK8及之前，String.java使用`char[]`数组存储字符，而在方法区中是通过CONTANT_Utf8_info数据结构保存在常量池，内部也是byte[ ]。例如，`String s = "Hello";`等价于`char[] value = {'H', 'e', 'l', 'l', 'o'};`。
    JDK9及之后String.java直接使用`byte[]`数组存储字符。

    ```c++
    CONSTANT_Utf8_info {
        u1 tag;
        u2 length; // 数组长度，占两个字节，2的16次方 -1 = 65535
        u1 bytes[length];
    }
    ```



## 17、String可以声明多长？

### 一、从字节码反映到内存的角度：

例如：String s = “AA...AA”;   如果全部是拉丁字符，一个字符对应一个理论可以存2的16次方个字符，javac的源码中有对字符串长度进行检查，必须**小于**Pool.MAX_STRING_LENGTH(值 = 2^16 - 1 = 65535)。
而对纯中文

结论：可以定义纯拉丁字符65534/2个；就是65534/2个char，就是65534个byte。
	   可以定义纯生僻字中文字符25535/4个，因为一个生僻字中文字符占4个字节

JDK8中，String底层是使用char[ ]实现，一个拉丁字符就是一个char，大部分汉字1个char，生僻字会用到两个char

```java
public static void main(String[] args) {
    String s1 = "烫";      // length() = 1
    String s2 = "𠀀";      // length() = 2
    System.out.println("s1 = " + s1.length() + ", s2 = " + s2.length());
}
```

```java
输出结果：
s1 = 1，s2 = 2
```



问题：为什么一个汉字的byte[ ]数组长度是4？

```java
byte[] bytes = "烫".getBytes(StandardCharsets.UTF_16);
System.out.println(bytes.length);
for (byte b: bytes) {
    System.out.print(Integer.toHexString(Byte.toUnsignedInt(b)));
    System.out.print(" ");
}
```

```java
输出结果：
4
fe ff 70 eb 
```

1. **UTF-16 的 BOM（字节顺序标记）**

- **BOM 的作用**：UTF-16 编码有两种字节序（大端序 `BE` 或小端序 `LE`），BOM 用于标识字节序。
- **BOM 的值**：
  - 大端序（Big-Endian）：`0xFE 0xFF`
  - 小端序（Little-Endian）：`0xFF 0xFE`
- **Java 的默认行为**：`StandardCharsets.UTF_16` 会在编码时自动添加 BOM，占 **2 个字节**。

------

2. **字符 "烫" 的编码过程**

- **字符 "烫" 的 Unicode 码点**：`U+70EB`（属于基本多文种平面 BMP）。

- **UTF-16 编码**：BMP 中的字符直接用 **2 个字节**表示。

- **完整字节数组**：

  ```
  BOM (2 字节) + 字符编码 (2 字节) = 4 字节
  ```

  例如：

  ```java
  byte[] bytes = "烫".getBytes(StandardCharsets.UTF_16);
  // 实际字节内容为：FE FF 70 EB（大端序）
  ```

------

3. **如何避免 BOM？**

- 如果不需要 BOM，可以指定明确的字节序：

  - **`UTF_16BE`**（大端序，无 BOM）：

    ```java
    byte[] bytes = "烫".getBytes(StandardCharsets.UTF_16BE); // 长度 = 2
    ```

  - **`UTF_16LE`**（小端序，无 BOM）：

    ```java
    byte[] bytes = "烫".getBytes(StandardCharsets.UTF_16LE); // 长度 = 2
    ```



### 二、从堆内存的角度

例如：

```java
byte[] bytes = loadFromFile(new File("superLongText.txt"));
String superLongString = new String(bytes);
```

```java
// String的源码
public String(byte bytes[], int offset, int length) {
    checkBounds(bytes, offset, length);
    // 将byte数组进行重新编码，然后给char[] value赋值
    this.value = StringCoding.decode(bytes, offset, length);
}
```

由于char[ ]的最大容量是Integer.MAX_VALUE，不爆内存的情况下理论上最多就能存Integer.MAX_VALUE（2^31-1）个char字符，约等于4GB。



## 18、匿名内部类

一、匿名内部类没有人类认知层面的名字

```java
public class JavaTest {
    public static void main(String[] args) {
        Food food = new Food() {
            @Override
            public int getColor() {
                return 0;
            }
        };
        // 输出匿名内部类的名字
        System.out.println("food = " + food.getClass().getName());
    }
}
```

```java
输出结果：
food = com.hpf.javademotest.JavaTest$1
```

结论：内部类的名字是：外部类类名.$ + N，N是匿名内部类的顺序



二、匿名内部类只能够继承一个父类或者实现一个接口

例如上文中 new Food()，匿名内部类 com.hpf.javademotest.JavaTest$1实际上是继承了Food，或者实现了Food接口，不能够同时extends 父类和implement 接口。

但是kotlin可以做到：

```kotlin
val runnableFoo = object:Foo(),Runnable {
	override fun run() { }
}
```



三、匿名内部类构造方法是谁决定？

是由编译器决定的，或者说外部类决定的。

```java
// JavaTest.java
public class JavaTest {
    static int c = 100;
    Carr car2 = new Carr("456");
    public static void main(String[] args) {
        Carr car = new Carr("123");
        // 这个
        Foo food = new Foo() {
            @Override
            public int getColor() {
                String deviceId = car.getDevice_id();
                return 0;
            }
        };
    }
    
    interface Foo {
        int getColor();
    }
}
```

```java
// 这是通过javac命令对JavaTest.java进行编译生成的class文件
final class JavaTest$1 implements JavaTest.Foo {
    // 这是匿名内部类的构造函数，由于内部使用外部类的局部变量Carr，所以编译器在构造方法中声明了这个参数
    JavaTest$1(Carr var1) {
        this.val$car = var1;
    }

    public int getColor() {
        String var2 = this.val$car.getDevice_id();
        return 0;
    }
}
```

结论：如果在静态方法中实例化匿名内部类，那么构造方法就不会声明外部类的引用，但是会声明外部类成员对象的引用。



```java
// JavaTest.java
public class JavaTest {
    static int c = 100;
    Carr car2 = new Carr("456");
    public static void main(String[] args) { }

    public void test() {
        Foo foo = new Foo() {
            @Override
            public int getColor() {
                String deviceId = car2.getDevice_id();
                return 0;
            }
        };
    }

    interface Foo {
        int getColor();
    }
}
```

```java
// 这是通过javac命令对JavaTest.java进行编译生成的class文件
class JavaTest$2 implements JavaTest.Foo {
    // 这是匿名内部类的构造函数，由于内部使用外部类的全局变量Carr，所以编译器在构造方法中声明了外部类引用
    JavaTest$2(JavaTest var1) {
        this.this$0 = var1;
    }

    public int getColor() {
        int var1 = JavaTest.c;
        String var2 = this.this$0.car2.getDevice_id();
        System.out.println("b = " + var1);
        System.out.println("deviceId = " + var2);
        return 0;
    }
}

```

结论：当创建匿名内部类实例时，在构造方法中会声明外部类的引用，可以通过外部类的引用使用外部类的成员变量。



四、Lambda表达式创建匿名内部类

需要满足SAM（single abstract method）规则，只能是interface类型并且只由一个接口





## 19、startActivityForResult的设计合理吗？

```java
public class Activity_A {
    public void login() {
        // 在Activity_A通过startActivityForResult启动Activity_B
        Intent intent = new Intent();
        intent.setClass(this, Activity_B.class);
        intent.setAction("action.login");
        startActivityForResult(intent, 100);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 当
        switch (resultCode) {
            ...
        }
    }
}



public class Activity_B {
    
    botton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // 在Activity_B中通过
            setResult(200, new Intent("action.login"));
            finish();
        }
    });
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            ...
        }
    }
}
```

1 Activity的onActivityResult使用起来非常麻烦，为什么不设计成回调?
答：（1）onActivityResult的用法是：ActivityA调用startActivityForResult(intent, requestCode)启动了ActivityB；当ActivityB给ActivityA回复时调用setResult设置返回的结果，然后调用finish；最后在ActivityA的onActivityResult只不过处理B的返回结果；

（2）onActivityResult使用很麻烦，存在以下使用的问题：①代码处理逻辑分离，容易出现遗漏和不一致的地方；②写法不够直观，且结果数据没有类型安全保障；③当结果种类比较多时，onActivityResult会逐渐臃肿且难以维护；

（3）onActivityResult不能设计成回调。假设onActivityResult可以设计成回调，由于匿名内部类会持有外部类的引用，当ActivityA被意外销毁时，由于其对象被匿名内部类OnResultCallback持有，可能会导致其无法正常被GC。当ActivityB给ActivityA回复时，OnResultCallback被调用，新的ActivityA*被恢复，但是onResult方法持有的依旧是被销毁的ActivityA的引用，回调没有任何意义，而且还会造成内存泄露。

> 但是也可以解决内存泄露，通过判断OnResultCallback的内部类来判断，外部类是否 == 匿名内部类中持有的外部类引用，如果false，则使用反射的手段，将匿名内部类持有的外部类引用从新赋值成此时的外部类引用。



## 20、Thread为什么stop方法会被弃用

（1）锁的问题，由于线程可能涉及到获取锁，如果中途被stop的话，很可能无法释放锁，这就会导致死锁的发生。

（2）可以通过interrupt( ) 让线程的中断标记设为true，根据中断标记结束线程中的任务。

```java
Thread thread = new Thread();
thread.start();
thread.interrupt();
```

interrupt()在thread.cc的底层实现：

```c
bool Thread::Interrupted(){
	MutexLock mu(Thread::Current(),*wait_mutex_);
	bool interrupted = IsInterruptedLocked();
	SetInterruptedLocked(false);//清除当前中断状态
	return interrupted;
}
bool Thread::IsInterrupted(){
	MutexLock mu(Thread::Current)
	return IsInterrupterdlocked();
}

void Thread::Interrupt(Thread*self){
    MutexLock mu(self,*wait_mutex_);
    // 底层就通过一个bool 值来实现，触发中断时让
    if(interrupted_){
        return;
    }
    interrupted_ = true;
    NotifyLocked(self);
}
```

```java
@Override
public void run() {
    for (int i = 0; i < 10; i++) {
        if (i == 2) {
            // 触发中断，修改thread.cc中的interrupted_ = true
            Thread.currentThread().interrupt();
        }
        // 通过isInterrupted()获取到了中断标记的值 = true
        System.out.println(" i = " + i + ", isInterrupted = " + Thread.currentThread().isInterrupted());
        try {
            // 会清除中断标记，让interrupted_ = false，并抛出InterruptedException
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.out.println("sleep, isInterrupted = " + Thread.currentThread().isInterrupted());
        }
    }
}
});
thread.start();
```

```java
输出结果：
 i = 0, isInterrupted = false
 i = 1, isInterrupted = false
 i = 2, isInterrupted = true
sleep, isInterrupted = false
```

总结：

```java
Thread.currentThread().interrupt(); // 让中断状态 = true
Thread.currentThread().isInterrupted() // 获取中断状态
Thread.interrupted() // 获取中断状态，并恢复中断状态
Thread.sleep(1000); // 让线程处于TIMED_WAITING，不会释放锁，会造成死锁
Thread.Sleep(0); // “触发操作系统立刻重新进行一次CPU竞争”。竞争的结果也许是当前线程仍然获得CPU控制权，也许会换成别的线程获得CPU控制权
```



## 21、HashTable 与 HashMap的区别

（1）HashTable是线程安全的，源码看到在所有读写操作的方法使用synchronized关键字修饰，这就导致并发的执行效率较低。

（2）初始化容器的时机不一样：HashTable 在构造方法中初始化，长度是11，
	HashMap是在首次执行put操作的时候初始化，长度是16，定义的默认值是通过位运算1 << 4，结果 = 1x 2 的4次方 = 16。

  (3) HashTable不支持null的键和值，HashMap支持

> HashTable的put方法判断value == null时会抛出空指针异常，而key为空时，会在key.hashCode()执行时也会抛出空指针异常。
>
> HashMap没有对value进行null的判断，是可以直接存储的，然而对key的判断，如果=null，那么key = 0，否则才会去计算hashCode

（4）hashCode计算的不同：HashTable直接使用key.hashCode()，而 HashMap是key.hashCode() 跟 自身右移16位做异或运算，称为扰动函数，为了减少桶碰撞，达到数据更加分散的效果。

```java
// 扰动函数
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```



## 22、ConcurrentHashMap如何实现线程安全

jdk5 - jdk7 使用分段锁segment，默认16个锁，继承于ReentrantLock，是重量级锁，每个锁管理一个或者多个桶。

jdk 6 对 分段锁的管理算法进行优化，防止一个segment管理桶的不够分散，退化成HashTable。

jdk7 对segment进行懒加载，使用volatile保证segment的可见性

jdk8 废弃segment，使用了CAS和synchronied来管理，进一步降低锁的开销。



## 23、AtomicReference 和 AtomicReferenceFieldUpdater 有何区别？

### **1. 设计目的**

#### **AtomicReference**

- **直接封装对象引用**：通过包装一个对象引用，提供原子性的 `get`、`set`、`compareAndSet` 等操作。

- **直接持有状态**：需要将字段显式声明为 `AtomicReference` 类型，适用于需要直接管理原子状态的场景。

- **示例**：

  ```java
  public class MyClass {
      private AtomicReference<String> value = new AtomicReference<>("init");
      
      public void update() {
          value.compareAndSet("init", "updated");
      }
  }
  ```

#### **AtomicReferenceFieldUpdater**

- **间接操作现有字段**：基于反射机制，原子更新某个类中已有的 `volatile` 引用字段，**无需修改原有类的结构**。

- **适用于已有代码的原子化改造**：当无法直接修改类的字段类型（例如使用第三方库的类时），可以用它实现原子操作。

- **示例**：

  ```java
  public class MyClass {
      public volatile String value = "init";
  }
  
  // 使用 FieldUpdater 操作 MyClass 的 value 字段
  public class UpdaterDemo {
      private static final AtomicReferenceFieldUpdater<MyClass, String> updater =
              AtomicReferenceFieldUpdater.newUpdater(MyClass.class, String.class, "value");
  
      public void update(MyClass obj) {
          updater.compareAndSet(obj, "init", "updated");
      }
  }
  ```

------

### **2. 使用场景**

| **特性**     | **AtomicReference**                       | **AtomicReferenceFieldUpdater**                |
| :----------- | :---------------------------------------- | :--------------------------------------------- |
| **侵入性**   | 需要显式声明字段为 `AtomicReference` 类型 | 无需修改原有类，直接操作已有的 `volatile` 字段 |
| **内存开销** | 每个实例持有一个 `AtomicReference` 对象   | 无额外内存开销（通过静态 `Updater` 操作字段）  |
| **适用场景** | 新代码设计，直接管理原子状态              | 旧代码改造、第三方类字段的原子操作             |



## 24、反射的原理

### **反射的核心原理**

通过包名+类名（全限定类名），找到.class文件，并加载到内存的方法区中，这是一个Class对象，通过java中的api对这个对象进行访问，访问的通道是native本地方法。

#### 1. **类加载与 Class 对象**

- **类加载过程**：当 JVM 加载一个类时，会将类的字节码（`.class`文件）加载到内存，并在方法区（Method Area）中生成一个**Class 对象**，存储类的元信息（如字段、方法、构造方法等）。
- **Class 对象**：每个类在 JVM 中都有一个唯一的 `Class` 对象，反射的所有操作都基于这个对象。

```java
// 获取 Class 对象的三种方式
Class<?> clazz1 = Class.forName("java.lang.String"); // 通过全限定类名
Class<?> clazz2 = String.class;                      // 通过类字面量
Class<?> clazz3 = "Hello".getClass();                // 通过对象实例
```

#### 2. **元数据存储**

- JVM 在方法区中存储类的以下信息：
  - 类名、包名、父类、接口。
  - 字段（`Field`）、方法（`Method`）、构造方法（`Constructor`）的元数据。
  - 注解、泛型等附加信息。
- 反射通过 `Class` 对象访问这些元数据。

#### 3. **动态访问与操作**

- **字段访问**：通过 `Field` 类动态读写对象的字段值（包括私有字段）。
- **方法调用**：通过 `Method` 类动态调用对象的方法（包括私有方法）。
- **对象创建**：通过 `Constructor` 类动态创建对象实例（即使构造方法是私有的）。

```java
// 示例：反射调用私有方法
public class MyClass {
    private void secretMethod() {
        System.out.println("私有方法被调用！");
    }
}

public static void main(String[] args) throws Exception {
    MyClass obj = new MyClass();
    Class<?> clazz = obj.getClass();
    Method method = clazz.getDeclaredMethod("secretMethod");
    method.setAccessible(true);    // 突破私有访问限制
    method.invoke(obj);            // 输出："私有方法被调用！"
}
```

------

### **反射的实现机制**

#### 1. **动态解析类信息**

- 反射 API（如 `Class.forName()`）触发类加载过程，JVM 解析字节码生成 `Class` 对象。
- 类的元信息在内存中以数据结构（如 C++ 的 `Klass` 对象）存储，反射通过 Native 方法访问这些数据。

#### 2. **反射的性能开销**

- **动态解析**：反射需要动态查找类信息，涉及 JVM 内部的数据结构遍历。
- **安全检查**：每次反射调用会检查访问权限（可通过 `setAccessible(true)` 关闭）。
- **方法调用**：反射调用方法时，JVM 需额外处理参数和返回值，效率低于直接调用。

```java
// 反射调用方法的性能优化：缓存 Method 对象
Method method = clazz.getDeclaredMethod("methodName");
method.setAccessible(true);
for (int i = 0; i < 1000; i++) {
    method.invoke(obj);  // 避免重复调用 getDeclaredMethod()
```



