# 深入理解JVM

## 字节结构

使用hexdump命令可以用来查看我们的16进制字节码。
如下图所示，字节码文件不使用分隔符

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/class%E5%8D%81%E5%85%AD%E8%BF%9B%E5%88%B6%E5%AE%9E%E5%86%B5.png)

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/class%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F%E8%A1%A8.png)

​                                                                                 class文件格式表

U4代表占4个字节，U2代表占2个字节，一个字节是8bit
magic：魔数，占用4个字节，相当于图中的cafebabe
minor_version：小版本号，占2字节
major_version：大版本号，占2字节
constant_pool_count：常量池数量，占2字节
constant_pool[constant_pool_count -1]：常量池数组，含一个或多个不同类型的常量（int、boolean、string、object等等）
access_flags：访问描述符，表示的是这个class或者接口的访问权限
this_class和super_class：他们都是对常量池的引用，分别指向本类的和父类
interfaces_count和interfaces[]：接口的数目和接口的具体信息数组
fields_count和fields[]：字段数目和字段具体的数组信息，这里的字段包括类变量和实例变量
methods_count和methods[]：方法信息
attributes_count和attributes[]：attributes被用在ClassFile, field_info, method_info和Code_attribute这些结构体中

## 反编译字节码

1、使用javap反编译可以看到文件结构

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/javap%E5%91%BD%E4%BB%A4%E5%8F%8D%E7%BC%96%E8%AF%91class%E6%96%87%E4%BB%B6.png)



2、使用javap -c会得到每个方法的Code信息

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/javap%20-c%E5%91%BD%E4%BB%A4%E8%8E%B7%E5%8F%96%E6%96%B9%E6%B3%95%E7%9A%84Code%E4%BF%A1%E6%81%AF.png)



3、使⽤ javap -v 可以看到更详细的内容

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/javap%20-v%E6%9B%B4%E8%AF%A6%E7%BB%86%E7%9A%84%E5%86%85%E5%AE%B9.png)



4、在 Code 信息中可以看到我们的操作数栈的⼤⼩,本地变量表的⼤⼩和传⼊参数的数量

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/%E6%93%8D%E4%BD%9C%E6%A0%88%E5%A4%A7%E5%B0%8F%E3%80%81%E6%96%B9%E6%B3%95%E5%8F%82%E6%95%B0%E6%95%B0%E9%87%8F.png)
                                                                                 构造方法

stack的大小是由方法内部的成员决定的，例如a = 1+2+3，那么操作数栈的大小就是3，要把1、2、3同时放得下。
locals=1和args_size=1是因为每个非静态方法的内部都有一个隐藏变量this

语法糖：1、成员变量是在构造函数中初始化的；

2、java内部类有成员变量持有外部类的引用；外部类new用内部类的时候先把自身calss引用操作入栈，接着通过构造函数的方式传递给内部类；匿名内部类也一样。

3、Kotlin如果在内部类中没有使⽤到外部类，那么不会持有外部类，这是给java做的优化。

4、泛型
Java 的泛型擦除并不是将所有泛型信息全部都擦除了，会将类上和⽅法上声明的泛型信息保 存在字节码中的 Signature 属性中，这也是反射能够获取泛型的原因，如retrofit。但是在⽅法中的泛型 信息是完全擦除了

5、synchronized 关键字
synchronized 会在⽅法调⽤前后通过 monitor 来进⼊和退出锁



## JVM内存结构（java8）

![java8内存结构](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/java8%E5%86%85%E5%AD%98%E7%BB%93%E6%9E%84.jpg)

![java8内存结构2](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/java8%E5%86%85%E5%AD%98%E7%BB%93%E6%9E%842.jpeg)

### 1.JVM的主要组成部分及其作用

- 类加载器（ClassLoader）
- 运行时数据区（Runtime Data Area）
- 执行引擎（Execution Engine）
- 本地库接口（Native Interface）

　　组件的作用： 首先通过类加载器（ClassLoader）会把 Java 代码转换成字节码，运行时数据区（Runtime Data Area）再把字节码加载到内存中，而字节码文件只是 JVM 的一套指令集规范，并不能直接交个底层操作系统去执行，因此需要特定的命令解析器执行引擎（Execution Engine），将字节码翻译成底层系统指令，再交由 CPU 去执行，而这个过程中需要调用其他语言的本地库接口（Native Interface）来实现整个程序的功能。

### 2.堆栈的区别

1. 栈内存存储的是局部变量，堆内存存储的是实体；
2. 栈内存的更新速度要快于堆内存，因为局部变量的生命周期很短；
3. 栈内存存放的变量生命周期一旦结束就会被释放，而堆内存存放的实体会被垃圾回收机制不定时的回收。

### 3.双亲委派

　　在介绍双亲委派模型之前先说下类加载器。对于任意一个类，都需要由加载它的类加载器和这个类本身一同确立在 JVM 中的唯一性，每一个类加载器，都有一个独立的类名称空间。类加载器就是根据指定全限定名称将 class 文件加载到 JVM 内存，然后再转化为 class 对象。

类加载器分类：

- 启动类加载器（Bootstrap ClassLoader）：是虚拟机自身的一部分，用来加载Java_HOME/lib/目录中的，或者被 -Xbootclasspath 参数所指定的路径中并且被虚拟机识别的类库；
- 扩展类加载器（Extension ClassLoader）：负责加载/lib/ext目录或Java. ext. dirs系统变量指定的路径中的所有类库；
- 应用程序类加载器（Application ClassLoader）：负责加载用户类路径（classpath）上的指定类库，我们可以直接使用这个类加载器。一般情况，如果我们没有自定义类加载器默认就是用这个加载器。

　　双亲委派模型：如果一个类加载器收到了类加载的请求，它首先不会自己去加载这个类，而是把这个请求委派给父类加载器去完成，每一层的类加载器都是如此，这样所有的加载请求都会被传送到顶层的启动类加载器中，只有当父加载无法完成加载请求（它的搜索范围中没找到所需的类）时，子加载器才会尝试去加载类。

[【深入理解JVM】：类加载器与双亲委派模型](https://blog.csdn.net/u011080472/article/details/51332866)

### 4.类加载的执行过程

类加载分为以下 5 个步骤：

1. **加载**：根据查找路径找到相应的 class 文件然后导入；
2. **检查**：检查加载的 class 文件的正确性；
3. **准备**：给类中的静态变量分配内存空间；
4. **解析**：虚拟机将常量池中的符号引用替换成直接引用的过程。符号引用就理解为一个标示，而在直接引用直接指向内存中的地址；
5. **初始化**：对静态变量和静态代码块执行初始化工作。

### 5.怎么判断对象是否可以被回收，如何确定垃圾

- 引用计数器：为每个对象创建一个引用计数，有对象引用时计数器 +1，引用被释放时计数 -1，当计数器为 0 时就可以被回收。它有一个缺点不能解决循环引用的问题
- 可达性分析：从 GC Roots 开始向下搜索，搜索所走过的路径称为引用链。当一个对象到 GC Roots 没有任何引用链相连时，则证明此对象是可以被回收的。

### 6.垃圾回收算法

1. **标记-清除算法**（Mark-Sweep）：最基础的垃圾回收算法，分为两个阶段，**标注**和**清除**。标记阶段标记出所有需要回收的对象，清除阶段回收被标记的对象所占用的空间。

   缺点：该算法最大的问题是内存碎片化严重，后续可能发生大对象不能找到可利用空间的问题。

   

2. **复制算法**（Copying）：为了解决标记-清除算法内存碎片化的缺陷而被提出的算法。按内存容量将内存划分为等大小的两块。每次只使用其中一块，当这一块内存满后将尚存活的对象复制到另一块上去，把已使用的内存清掉。

   优点：这种算法虽然实现简单，内存效率高，不易产生碎片。

   缺点：但是最大的问题是可用内存被压缩到了原本的一半，且存活对象增多的话，Copying 算法的效率会大大降低。

   

3. **标记-整理算法**（Mark-Compact）：结合了Mark-Sweep和Copying两个算法，为了避免缺陷而提出。标记阶段和Mark-Sweep算法相同。标记后不是清理对象，而是将存活对象移向内存的一端，然后清除端边界外的对象。

   

4. **分代算法**：分代收集法是目前大部分 JVM 所采用的方法，其核心思想是根据对象存活的不同生命周期将内存
   划分为不同的域。一般情况下将 GC 堆划分为老生代(Tenured/Old Generation)和新生代(Young Generation)。老生代的特点是每次垃圾回收时只有少量对象需要被回收，新生代的特点是每次垃圾回收时都有大量垃圾需要被回收，因此可以根据不同区域选择不同的算法。

   新生代：每次收集时都有大批对象死去，只有少量存活，选用**复制算法**，只需要复制出少量存活对象就可以快速完成收集。
   老年代：对象存活率高、没有额外空间对它进行分配担保，一般使用**标记-清理**或**标记-整理算法**。

### 7.JVM垃圾回收器

1. **Serial**：最早的单线程串行垃圾回收器。
2. **Serial Old**：Serial 垃圾回收器的老年代版本，同样也是单线程的，可以作为 CMS 垃圾回收器的备选预案。
3. **ParNew**：是 Serial 的多线程版本。
4. **Parallel** ：和 ParNew 收集器类似是多线程的，但 Parallel 是吞吐量优先的收集器，可以牺牲等待时间换取系统的吞吐量。
5. **Parallel Old**：是 Parallel 老年代版本，Parallel 使用的是复制的内存回收算法，Parallel Old 使用的是标记-整理的内存回收算法。
6. **CMS**：一种以获得最短停顿时间为目标的收集器，非常适用 B/S 系统。
7. **G1**：一种兼顾吞吐量和停顿时间的 GC 实现，是 JDK 9 以后的默认 GC 选项。

[![img](http://ww1.sinaimg.cn/large/75a4a8eegy1g5w3nqkn0jj21400u0dre.jpg)](http://ww1.sinaimg.cn/large/75a4a8eegy1g5w3nqkn0jj21400u0dre.jpg)

- 新生代回收器：Serial、ParNew、Parallel Scavenge
- 老年代回收器：Serial Old、Parallel Old、CMS
- 整堆回收器：G1

　　新生代垃圾回收器一般采用的是**复制算法**，复制算法的优点是效率高，缺点是内存利用率低；老年代回收器一般采用的是**标记-整理算法**进行垃圾回收。

默认垃圾收集器：

- JDK1.7：Parallel Scavenge + Serial Old
- JDK1.8：Parallel Scavenge + Serial Old
- JDK1.9：G1
- JDK10：G1

### 8.分代垃圾回收器工作原理

　　分代回收器有两个分区：老生代和新生代，新生代默认的空间占比总空间的 1/3，老生代的默认占比是 2/3。

　　新生代使用的是复制算法，新生代里有 3 个分区：Eden、To Survivor、From Survivor，它们的默认占比是 8:1:1，它的执行流程如下：

- 把 Eden + From Survivor 存活的对象放入 To Survivor 区；
- 清空 Eden 和 From Survivor 分区；
- From Survivor 和 To Survivor 分区交换，From Survivor 变 To Survivor，To Survivor 变 From Survivor。

　　每次在 From Survivor 到 To Survivor 移动时都存活的对象，年龄就 +1，当年龄到达 15（默认配置是 15）时，升级为老生代。大对象也会直接进入老生代。

　　老生代当空间占用到达某个值之后就会触发全局垃圾收回，一般使用标记-整理的执行算法。以上这些循环往复就构成了整个分代垃圾回收的整体执行流程。

### 9.JVM调优工具

　　JDK 自带了很多监控工具，都位于 JDK 的 bin 目录下，其中最常用的是 jconsole 和 jvisualvm 这两款视图监控工具。

- jconsole：用于对 JVM 中的内存、线程和类等进行监控；
- jvisualvm：JDK 自带的全能分析工具，可以分析：内存快照、线程快照、程序死锁、监控内存的变化、gc 变化等。

### 10.常量池

1.全局常量池在每个VM中只有一份，存放的是字符串常量的引用值，真实的字符串实例会在堆开辟内存存放
2.class常量池是在编译的时候每个class都有的，在编译阶段，存放的是常量的符号引用。
3.运行时常量池是在类加载完成之后，将每个class常量池中的符号引用值转存到运行时常量池中，也就是说，每个class都有一个运行时常量池，类在解析之后，将符号引用替换成直接引用，与全局常量池中的引用值保持一致。

### 11、对象在堆中的结构

​	11.1、对象头
​	包含三部分：MarkWord、类型指针、数组长度（对象是数组时）

​	11.2、具体数据

### 12、MarkWork

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/MarkWork%E7%A0%81%E8%A1%A8.jpg)

JDK1.6里锁一共有四种状态，无锁状态，偏向锁状态，轻量级锁状态和重量级锁状态，它会随着竞争情况逐渐升级。`锁可以升级但不能降级`，意味着偏向锁升级成轻量级锁后不能降级成偏向锁。这种锁升级却不能降级的策略，`目的是为了提高获得锁和释放锁的效率.`

#### 无锁态：

​	程序不会有锁的竞争。那么这种情况我们不需要加锁，所以这种情况下对象锁状态为无锁。

#### 偏向锁：

​	如果在运行过程中，同步锁只有一个线程访问，不存在多线程争用的情况，则线程是不需要触发同步的，这种情况下，就会给线程加一个偏向锁。线程第二次到达同步代码块时，会判断此时持有锁的线程是否就是自己，如果是则正常往下执行。由于之前没有释放锁，这里也就不需要重新加锁。如果自始至终使用锁的线程只有一个，很明显偏向锁几乎没有额外开销，性能极高。
​    如果在运行过程中，遇到了其他线程抢占锁，则持有偏向锁的线程会被挂起，JVM会消除它身上的偏向锁，将锁恢复到标准的轻量级锁。偏向锁通过消除资源无竞争情况下的同步原语，进一步提高了程序的运行性能。一旦有第二个线程加入锁竞争，偏向锁就升级为轻量级锁（自旋锁）。升级为轻量级锁的时候需要撤销偏向锁，撤销偏向锁的时候会导致STW(stop the word)操作；

#### 轻量级锁（自旋锁）：

​	自旋锁原理非常简单，如果持有锁的线程能在很短时间内释放锁资源，那么那些等待竞争锁的线程就不需要做内核态和用户态之间的切换进入阻塞挂起状态，它们只需要等一等（自旋），等持有锁的线程释放锁后即可立即获取锁，这样就避免用户线程和内核的切换的消耗。
​	在轻量级锁状态下继续锁竞争，没有抢到锁的线程将自旋，即不停地循环判断锁是否能够被成功获取。长时间的自旋操作是非常消耗资源的，一个线程持有锁，其他线程就只能在原地空耗CPU，执行不了任何有效的任务，这种现象叫做忙等（busy-waiting）。如果锁竞争情况严重，某个达到最大自旋次数的线程，会将轻量级锁升级为重量级锁。

#### 重量级锁：

​	当后续线程尝试获取锁时，发现被占用的锁是重量级锁，则自己挂起，等待将来被唤醒。
​	重量级锁的特点：其他线程试图获取锁时，都会被堵塞，只有持有锁的线程释放锁之后才会唤醒。

### GC：

​	堆内存按GC规则分两大块：新生代和老年代

​	详细解析参考博客：https://blog.csdn.net/future234/article/details/80677140

#### 新生代：

​	eden区占80%，form区占10%，To区占10%

​	对象会在新⽣代的 eden 区域中创建（⼤对象会直接进⼊⽼年代），第⼀次eden 区满了以后进⾏ minor GC 将存货对象 age + 1，然后放⼊ from 区域，将Eden 区域内存清空。
以后每次 minor GC 都将 eden 和 from 中的存活对象 age + 1 ，然后放⼊ to 区域，然后将 to 区 域和 from 区域互相调换。



#### 老年代：

​	在 age 达到⼀定值时会移动到⽼年代

​	在 minor GC 时，存活对象⼤于 to 区域，那么会直接进⼊⽼年代

#### 关于跨代引⽤：

​	为了防⽌不能确定新⽣代的对象是否被⽼年代的对象引⽤⽽需要进⾏ full GC 。

​	通过 card table 将⽼年代分成若⼲个区域，所以在 minor GC 时只需要对⽼年代表中记录有年轻代引用table进⾏扫描就可以了。

