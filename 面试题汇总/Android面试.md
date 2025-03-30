# 面试题整理





## 1、Android 系统内存分配机制？分配大小多少？

​	在Android里，程序内存被分为2部分：native和dalvik，dalvik就是我们普通的java使用内存，也就是我们上一篇文章分析堆栈的时候使用的内存。我们创建的对象是在这里面分配的，对于内存的限制是 native+dalvik 不能超过最大限制。android程序内存一般限制在16M，也有的是24M（早期的Android系统G1，就是只有16M）。具体看定制系统的设置，在Linux初始化代码里面Init.c，可以查到到默认的内存大小。

<application
   .....
   android:label="XXXXXXXXXX"
   **android:largeHeap="true">

**  .......
</application>

**cat /system/build.prop  //读取这些值
getprop dalvik.vm.heapsize //如果build.prop里面没有heapsize这些值，可以用这个抓取默认值
setprop dalvik.vm.heapsize 256m //设置**

-----------------------  build.prop 部分内容 ---------------------

dalvik.vm.heapstartsize=8m
dalvik.vm.heapgrowthlimit=96m
dalvik.vm.heapsize=384m
dalvik.vm.heaputilization=0.25
dalvik.vm.heapidealfree=8388608
dalvik.vm.heapconcurrentstart=2097152
ro.setupwizard.mode=OPTIONAL
ro.com.google.gmsversion=4.1_r6
net.bt.name=Android
dalvik.vm.stack-trace-file=/data/anr/traces.txt



## 2、Android的Bundle机制？

`Bundle`实现了`Parcelable`接口，可以被序列化和反序列化，如`base64`编码一样，将字符转换成二进制数据；

**Intent使用Bundle：**

```java
intent.putExtra("key","value");
Bundle bundle = intent.getExtras();
if (bundle != null) {
    Object value = bundle.get("key");
}
```



**Message使用Bundle：**

常用于轻量级跨进程通信：`Messenger`

```java
Message msg = Message.obtain(null,1);
Bundle bundle = new Bundle();
bundle.putString("reply","服务端已经收到了消息啦!");
msg.setData(bundle);
```





## 3.Parcelable和Serializable底层实现原理

java中最常用的序列化标识是Serializable，定义一个Bean时实现Serializable接口，不需要实现什么方法，Serializable从源码看没有任何方法，但是注释说明很多很多。Serializable可以实现数据持久化，保存对象到本地文件、数据库、网络流传输。
Parcelable的设计初衷是因为Serializable效率过慢，为了在程序内不同组件间以及不同Android程 序间(AIDL)高效的传输数据而设计，这些数据仅在内存中存在，Parcelable是通过IBinder通信的消息的载体。

**原理：**
Serializable由于使用了反射机制，在序列化操作的时候会产生大量的临时变量,从而导致GC的频繁调用。
Parcelable是以Ibinder作为信息载体的，内存开销较小。

**特点：**
在读写数据的时候,Parcelable是在内存中直接进行读写,
Serializable是通过使用IO流的形式将数据读写入在硬盘上。

### Serializable的一些说明：

- 对象的序列化处理非常简单，只需对象实现了Serializable 接口即可（该接口仅是一个标记，没有方法）
- 序列化的对象包括基本数据类型，所有集合类以及其他许多东西，还有Class 对象
- 对象序列化不仅保存了对象的“全景图”，而且能追踪对象内包含的所有句柄并保存那些对象；接着又能对每个对象内包含的句柄进行追踪
- 使用transient关键字修饰的的变量，在序列化对象的过程中，该属性不会被序列化。



#### **序列化的步骤：**

- 首先要创建某些OutputStream对象：OutputStream outputStream = new FileOutputStream("output.txt")
- 将其封装到ObjectOutputStream对象内：ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
- 此后只需调用writeObject()即可完成对象的序列化，并将其发送给OutputStream：objectOutputStream.writeObject(Object);
- 最后不要忘记关闭资源：objectOutputStream.close(), outputStream .close();

 

####  **反序列化的步骤**：

- 首先要创建某些OutputStream对象：InputStream inputStream= new FileInputStream("output.txt")
- 将其封装到ObjectInputStream对象内：ObjectInputStream objectInputStream= new ObjectInputStream(inputStream);
- 此后只需调用readObject()即可完成对象的反序列化：objectInputStream.readObject();
- 最后不要忘记关闭资源：objectInputStream.close()，inputStream.close();

```java
import java.io.Serializable;

public class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private int age;
    private transient String password;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
        this.password = "default";
    }

    // Getter和Setter方法
    public String getName() { return name; }
    public int getAge() { return age; }
}
```

```java
// 序列化到文件
try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("person.ser"))) {
    oos.writeObject(person);
}

// 反序列化
try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("person.ser"))) {
    Person loadedPerson = (Person) ois.readObject();
}
```



### Parcelable

创建一个类实现Parcelable接口，用于AIDL，序列化成二进制数据于内存中，通过Binder跨进程通信。

从客户端传数据给服务端writeToParcel，服务端传给客户端readFromParcel



#### 序列化步骤：

```java
@Override
public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
}
```



#### 反序列化步骤：

```java
protected Book(Parcel in) {
    id = in.readInt();
    name = in.readString();
}

public void readFromParcel(Parcel in) {
    id = in.readInt();
    name = in.readString();
}

```



**速度对比：**

在大多数场景下，Parcelable确实存在性能优势，Serializable的性能缺陷来自：无论序列化还反序列化，都要创建**ObjectStreamClass**，在其构造方法中使用反射手段创建字段的实例。

```java
// ObjectStreamClass.java
// 使用反射手段创建字段的实例
private ObjectStreamClass(final Class<?> cl) {
    this.cl = cl;
    name = cl.getName();
    isEnum = Enum.class.isAssignableFrom(cl);
    serializable = Serializable.class.isAssignableFrom(cl);
    externalizable = Externalizable.class.isAssignableFrom(cl);
    ..
        suid = getDeclaredSUID(cl);
}
```

构建**ObjectStreamClass**类型的描述信息的过程，有缓存机制，能够大量复用缓存的使用场景下，Serializable反而会存在优势。

Parcelable原本需要自行实现序列化和反序列化的接口，在易用性上存在短板，但是Kotlin的Parcelize可通过注解的方式自行生成序列化和反序列化的代码。

```kotlin
@Parcelize
class User(
	var name: String,
	var age:Int) : Parcelable
```



## 4、Dalvik和ART虚拟机区别

- **Dalvik**：
  - 是 Android 早期版本（4.4 及之前）的默认虚拟机。
  - 支持较老的设备和应用。
- **ART**：
  - 从 Android 5.0（Lollipop）开始成为默认虚拟机。
  - 完全兼容 Dalvik 的 DEX 文件格式，但需要重新编译应用以充分利用 ART 的优势。

Dalvik虚拟机下运行Java时，要将字节码通过即时编译器（just in time ，JIT）转换为机器码（机器码才是能真正运行的），这会拖慢应用的运行效率。

ART(Android Runtime)，应用在第一次安装的时候，字节码就会预先编译成机器码，使其成为真正的本地应用。这个过程叫做预编译（AOT,Ahead-Of-Time）。这样的话，应用的启动(首次)和执行都会变得更加快速。

ART分为2种模式： 解释模式和AOT机器码模式。  

解释模式类似于JavaScript， 就是取出Dex代码并逐条解释，运行仍然会慢；

机器码模式就是在安装app时就编译好Dex对应的机器码，运行很快， 这是Android5.0及后续版本的默认方式。

### 字节码编译器的区别

- Dalvik是即时编译（JIT），效率低

- ART是预编译（AOT），安装app时就完成编译后缀为oat的文件，以后打开应用时直接读取oat文件执行即可

### 垃圾回收方面的区别

- **Dalvik**仅固定一种内存回收算法，标记清除法，导致碎片化严重，GC 时会导致应用暂停（Stop-The-World），影响用户体验

- **ART**改进了垃圾回收机制，支持并发 GC，减少应用暂停时间。

- 提供了更好的内存管理，减少内存碎片。

  ### 1. **并发垃圾回收（Concurrent GC）**

  ART 引入了并发垃圾回收机制，这意味着垃圾回收过程可以与应用程序线程并发执行，减少了应用暂停时间（Stop-The-World）。

  - **优点**：
    - 减少应用卡顿，提升用户体验。
    - 垃圾回收过程对应用性能的影响更小。
  - **实现方式**：
    - 在应用运行的同时，垃圾回收器会标记和清理不再使用的对象。

  ------

  ### 2. **分代垃圾回收（Generational GC）**

  ART 采用了分代垃圾回收策略，基于对象的生命周期将堆内存分为不同的代（Generations）。

  - **堆内存分区**：
    - **年轻代（Young Generation）**：存放新创建的对象。
    - **老年代（Old Generation）**：存放存活时间较长的对象。
  - **回收策略**：
    - 年轻代使用 **复制算法**，频繁回收短期存活的对象。
    - 老年代使用 **标记-清除（Mark-Sweep）** 或 **标记-整理（Mark-Compact）** 算法，减少回收频率。
  - **优点**：
    - 针对不同生命周期的对象采用不同的回收策略，提高垃圾回收效率。
    - 减少全局垃圾回收的频率。



## 5、glide三层缓存机制？

链接：https://www.jianshu.com/p/171793326b94

LruCache 和磁盘缓存 DiskLruCache，两者都是基于 LRU（Lest Resently Used）算法并使用 LinkedHashMap 实现的，不同的是前者是保存在内存中，后者是保存在磁盘文件中。

假设我们在一开始设置了 Glide 支持磁盘缓存，且原图和编码后（即压缩或转换）的图片都要缓存；

- ###### 缓存读取顺序：

  弱引用缓存 -> LruCache -> DiskLruCache

  当原始图片缓存和修改后的图片缓存都存在时，首先会获取经过压缩或转换后的图片，然后才去获取原始图片，毕竟原始图片一般比经过处理后的图片大且占据更多内存空间

  

- ###### 缓存写入顺序：

  DiskLruCache 缓存原图 -> 弱引用缓存 -> LruCache -> DiskLruCache 缓存编码后的图片

- ###### 注意：

  弱引用缓存和 LruCache 之间存在缓存的转换关系，图片从正在使用状态转为不使用状态，Glide 将图片从弱引用缓存移除然后缓存到 LruCache 中，假如 LruCache 中的某张图片现在需要使用，则图片从 LruCache 中移除缓存到弱引用缓存中，弱引用缓存中保存的是正在使用的图片。



**LruCache原理：**

- 内部使用LinkedHashMap数据结构，加载因子是0.75

- 构造函数可以设置缓存容量

- 当缓存元素被访问，这个元素就会被移到链表的头部

- 新增元素达到最大容量时，会移除链表尾部元素



## 6、Activity的4种启动模式

- standard：每次激活Activity时(startActivity)，都创建Activity实例，并放入任务栈。

- singleTop：如果某个Activity自己激活自己，即任务栈栈顶就是该Activity，则不需要创建，其余情况都要创建Activity实例。

- singleTask：如果要激活的那个Activity在任务栈中存在该实例，则不需要创建，只需要把此Activity放入栈顶，并把该Activity以上的Activity实例都pop。

- singleInstance：如果应用1的任务栈中创建了MainActivity实例，如果应用2也要激活MainActivity，则不需要创建，两应用共享该Activity实例。

![](E:\StudyNote\Activity启动模式.webp)



## 7、TCP与UDP的区别

参考：https://www.cnblogs.com/fundebug/p/differences-of-tcp-and-udp.html

TCP和UDP都是属于传输层

### TCP

- 三次握手四次挥手
- 客户端连接服务端一对一
- 面向字节流，不保留报文边界的情况下以字节流方式进行传输。
- 传输可靠，使用流量控制和拥塞控制

### UDP

- 面向无连接，监听端口就行
- 一对多、多对多、多对一
- 面向报文
- 传输是不可靠的，不使用流量控制和拥塞控制



## 8、binder通信，客户端如何匹配到服务的，如何做身份校验

1、客户端通过Intent(context, Service.class)绑定并启动服务bindService(intent)

2、在客户端的Connection中会执行IBookManager.Stub.asInterface(ibinder)，这时候在asInterface方法中会执行queryLocalInterface方法，如果Service在本进程中，那么就直接创建Service实例，否则就创建Remote进程的Service，并在ServiceManager注册Binder服务。

3.服务端成功注册并启动之后会被调用onBind(Intent intent)，我们可以根据Intent中的信息做**安全身份校验**，防止恶意启动，

4.onBind方法return IBinder，asInterface返回Remote进程的Service代理对象，就是这个IBinder的代理对象。



## 9、如何跨APP启动Activity？有哪些注意事项？

（1）共享uid，app使用相同签名，通过显式意图启动

两个应用的sharedUserId属性必须一致：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.hpf.customview"
    android:sharedUserId="com.hpf">
```

通过显示意图启动：

```kotlin
fun jumpOtherApp(view: View) {
    val intent = Intent()
    // intent.setComponent(ComponentName("com.hpf.javademotest",
    //                    "com.hpf.javademotest.rxjavatest.RetrofitTestActivtiy"))
    intent.setClassName("com.hpf.javademotest", 
                        "com.hpf.javademotest.rxjavatest.RetrofitTestActivtiy")
    startActivity(intent)
}
```

（2）使用exported属性，暴露本app的Activity，可以被任何app启动。不推荐使用这种方式，非常不安全。

（3）使用IntentFilter

被调用的Activity需要声明action和category：

```xml
<activity android:name=".rxjavatest.RetrofitTestActivtiy">
    <intent-filter>
        <action android:name="com.hpf.action.HTTP" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

不需要包名和activity名也能够启动

```kotlin
fun jumpOtherApp(view: View) {
    val intent = Intent()
        intent.setAction("com.hpf.action.HTTP");
    startActivity(intent)
}
```



（4）使用权限进行加锁

```xml
<activity android:name=".rxjavatest.RetrofitTestActivtiy" 
    android:permission="com.hpf.permission.HTTP">
    <intent-filter>
        <action android:name="..." />
        <category android:name="..." />
    </intent-filter>
</activity>
```

调用方需要添加权限才能够调用

```xml
<uses-permission android:name="com.hpf.permission.HTTP"/>
```



（5）拒绝服务漏洞

```kotlin
// ActivityA 跳转到ActivityB
fun jumpOtherApp(view: View) {
    val intent = Intent()
    intent.setAction("com.hpf.action.HTTP");
    val bundle = Bundle()
    // MySerializableData 和 MyParcelableData都是
    bundle.putSerializable("data1", MySerializableData())
    intent.putExtras(bundle)
    startActivity(intent)
}
```

```kotlin
// ActivityB中的onCreate方法
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.rxjava_activity)

    var intent = getIntent()
    if (intent != null) {
        try {
            val serializableExtra = intent.getSerializableExtra("data1")
            Log.d("", "onCreate, data1 = " + serializableExtra);
        } catch (e: Exception) {
            e.printStackTrace()
        }
 }
```

```kotlin
// 如果没有捕获异常的话，程序就会崩溃，BadParcelableException
android.os.BadParcelableException: Parcelable encountered ClassNotFoundException reading a Serializable object (name = com.hpf.customview.data.MySerializableData)
...
Caused by: java.lang.ClassNotFoundException: com.hpf.customview.data.MySerializableData
```



## 10、如何在任意地方为当前Activity添加View

```java
class MyActivityLifecycleCallbacks implements ActivityLifecycleCallbacks {

    // 注意使用弱引用，防止内存泄露
    private WeakReference<Activity> weakReferenceActivity;

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        Log.d("application", "onActivityCreated,activity = " +activity);
        weakReferenceActivity = new WeakReference<>(activity);
        ViewGroup view = weakReferenceActivity.get().findViewById(R.id.cover_view_container);
        view.addView(new TextView(weakReferenceActivity.get()));
    }
 
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}
}
```

```java
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 注册Activity的生命周期回调
        registerActivityLifecycleCallbacks(MyActivityLifecycleCallbacks());
    }
```



## 11、如是实现微信右滑返回的效果

（1）如果是fragment之间的切换：

```java
FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
transaction.replace(R.id.fragment_container, newFragment);
transaction.addToBackStack(null);
transaction.commit();
```

（2）如果是View之间的切换，可以使用属性动画，平移隐藏

（3）如果是Activity之间的切换：

```xml
// 目的是让activityB的Window透明，启动activityB时就不会出现当前activity黑屏的情况
<style name="AppTranslucentTheme" parent="AppTheme">
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:windowIsTranslucent">true</item>
</style>
```

```kotlin
// ActivityA
fun shutterButton(view: View) {
    val intent = Intent(this, ActiityB::class.java)
    startActivity(intent)
    // 启动ActiityB时指定动画
    overridePendingTransition(R.anim.activity_enter_anim, R.anim.activity_exit_anim)
}
```

```kotlin
// ActivityB
override fun onPause() {
    super.onPause()
    // 这样调用才能够让退出动画有效
    overridePendingTransition(R.anim.activity_enter_anim, R.anim.activity_exit_anim)
}
```



## 12、关于Context

（1）应用里有多少个Context？

答：Application的个数 + Activity的个数 + Service的个数

（2）不同Context有什么区别？

答：Activity用于UI展示，继承ContextThemeWrapper，内部有主题相关的资源，Application和Service继承ContextWrapper

（3）Activity里的this和getBaseContext有什么区别？

this就是Activity本身，父类是Context，而getBaseContext返回的是ContextWrapper中的mBase，mBase就是ContextImpl，在创建Activity实例之前被创建，然后传给Activity的attach方法，成为Activity的全局变量，里边包含了Resource、packageinfo、classLoder、displayId等信息。

（4）getApplication 和 getApplicationContext有什么区别？

这两个方法都是返回Application，getApplication 是activity和service特有的方法，而getApplicationContext 是 Context的抽象方法，可以在Application、Activity、Service中重写。



## 13、Android加载Resource资源

https://blog.csdn.net/dayong198866/article/details/103264951

（1）加载文本不会用到sPreloadedDrawables缓存

打包apk 时内部有 resources.arsc 文件

通过 Context 上下文类获取 Resources 类，然后通过资源管理类（Resources）加载资源文件，在 Resources 类内部的 impl（ResourcesImpl） 进行加载资源，而 ResourcesImpl 内部则通过 AssetManager 进行资源真正的加载操作；AssetManager 内部主要是 native 方法，所以资源的加载操作主要在 native 层进行的。

```java
boolean getResourceValue(int resId, int densityDpi, TypedValue outValue ...) {
    synchronized (this) {
    	// native 加载资源
        final int cookie = nativeGetResourceValue(
                mObject, resId, (short) densityDpi, outValue, resolveRefs);
        // 资源未找到
        if (cookie <= 0) {
            return false;
        }

        if (outValue.type == TypedValue.TYPE_STRING) {
        	// 对 string 数据进行解析
            if ((outValue.string = getPooledStringForCookie(cookie, outValue.data)) == null) {
                return false;
            }
        }
        return true;
    }
}


链接：https://juejin.cn/post/7388441764874813459
```



（2）sPreloadedDrawables预加载缓存

缓存的是系统的drawable和colorStateList资源，而非应用资源

```java
//frameworks/base/core/java/com/android/internal/os/ZygoteInit.java
private static void preloadResources() {
    final VMRuntime runtime = VMRuntime.getRuntime();
	...
    try {
        System.gc();
        runtime.runFinalizationSync();
        /**
             * 这句会触发对framework-res.apk的加载
             * Resources、AssetManager、ResTable的创建等等
             */
        mResources = Resources.getSystem();
        mResources.startPreloading();
        if (PRELOAD_RESOURCES) {//true
            ...
            //预加载drawable资源
            TypedArray ar = mResources.obtainTypedArray(
                com.android.internal.R.array.preloaded_drawables);
            int N = preloadDrawables(runtime, ar);
            ar.recycle();
            ...
            ar = mResources.obtainTypedArray(
                com.android.internal.R.array.preloaded_color_state_lists);
            //预加载ColorStateList资源
            N = preloadColorStateLists(runtime, ar);
            ...
        }
        mResources.finishPreloading();
    } catch (RuntimeException e) {
        Log.w(TAG, "Failure preloading resources", e);
    } finally {
        Debug.stopAllocCounting();
    }
}

```

`Resources.getDrawable()` → `ResourcesImpl.loadDrawable()` → 检查预加载缓存 → 若命中则通过 `getPreloadedDrawables` 返回。



应用资源的缓存在framwork中实现，缓存在AssetManager.cpp中

```cpp
//frameworks/base/libs/androidfw/AssetManager.cpp
Asset* AssetManager::ZipSet::getZipResourceTableAsset(const String8& path)
{
    //先拿到APK包，可能走缓存哦
    int idx = getIndex(path);
    sp<SharedZip> zip = mZipFile[idx];
    if (zip == NULL) {
        zip = SharedZip::get(path);
        mZipFile.editItemAt(idx) = zip;
    }
    //再从SharedZip的缓存里找resources.arsc对应的缓存
    return zip->getResourceTableAsset();
}

//frameworks/base/libs/androidfw/AssetManager.cpp
Asset* AssetManager::SharedZip::getResourceTableAsset()
{
    ALOGV("Getting from SharedZip %p resource asset %p\n", this, mResourceTableAsset);
    return mResourceTableAsset;
}

```



## 14、图片缓存算法

选择算法需要考虑的问题：哪些应该保存？哪些应该丢弃？什么时候丢弃？

选择数据结构需要考虑：写入成本、读取成本，缓存都会大量读写操作，优先选择HashMap，但是需要对缓存元素进行排序的话，就选择有序的LinkHashMap

Lurcache的类有多个：

(1) android.util.LruCache

这个类存在缺陷，在初始化LinkedHashMap时，传的accessOrder参数 = true，意思是访问过的元素都会放到链表的尾部，其中trimToSize方法却移除了链表尾端的元素

```java
private void trimToSize(int maxSize) {
    while (true) {
        ...
        synchronized (this) {
            ...
            Map.Entry<K, V> toEvict = null;
            // map就是LinkHashMap
            for (Map.Entry<K, V> entry : map.entrySet()) {
                // 遍历完成得到的这个元素是链表尾端元素
                toEvict = entry;
            }
            ...
            key = toEvict.getKey();
            map.remove(key);
            ...
        }
	    ...
    }
}
```

(2) androidx.collection.LruCache

这个类就比较实用，淘汰头节点的元素

```java
public void trimToSize(int maxSize) {
    while (true) {
        ...
        synchronized (this) {
            ....
            // 获取第一个元素
            Map.Entry<K, V> toEvict = map.entrySet().iterator().next();
            ...
            key = toEvict.getKey();
            // 淘汰链表首位元素
            map.remove(key);
            ...
        }
        ...
    }
}
```

(3) Glide-  LruCache

```java
protected synchronized void timToSize(long size) {
    Map.Entry<T,Y> last;
    Iterator<Map.Entry<T,Y>> cacheIterator;
    while(currentSize > size) {
        cacheIterator = cache.entrySet().iterator();
        last = cacheIterator.next();
        ..
        // 淘汰链表首位元素
        cacheIterator.remove;
        ..
    }
}
```





## 15、如何计算图片占用内存大小

例如：图片宽200 x 300像素

（1）png图片，推荐使用ARGB_8888解码，带有透明通道，并且颜色都是8bit，一个通道占用了一个字节，占用内存大小为：200 x 300 x 占用字节数 。

ARGB_4444， 占16bit，2个字节

RGB_565，占16bit，2个字节

ALPHA_8，占8bit，1个字节

jpg图片，推荐使用RGB_565，占用内存大小是：200 x 300 x 2



（2）将图片放在不同的drawable目录下，还有屏幕的尺寸，最终计算出来占用内存不一样，会有一定倍数的缩放。

缩放公式：图片原大小 x 资源目录缩放倍数 x 屏幕densityDpi

资源目录缩放倍数：
drawable  1
drawable - mdpi  1
drawable - hdpi   1.5
drawable - xdpi    2
drawable - xxdpi  3
drawable - xxxdpi 4

drawable - nodpi  让系统加载原图片大小   
