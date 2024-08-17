

一、Glide（https://github.com/bumptech/glide）

是Google推荐的图片加载和缓存框架，他完全是基于 Picasso 的，沿袭了 Picasso 的简洁风格，但是在此做了大量优化与改进。


1.总体设计及流程
整个库分为RequestManager(请求管理器)、Engine(数据获取引擎)、Fetcher(数据获取器)、MemoryCache(内存缓存)、DiskLRUCache、Transformation(图片处理)、Encoder(本地缓存存储)、Registry(图片类型及解析器配置)、Target(目标)等模块

简单来说,Glide收到加载及显示图片的任务,创建Request并将它交给RequestManager,Request启动Engine去数据源获取资源(通过Fetcher),获取后由Transformation处理后交给Target

Glide依赖于DiskLRUCache、GifDecoder等开源库去完成本地缓存和Gif图片解码工作



2.Glide优点
(1).图片缓存-->媒体缓存
Glide不仅可以图片缓存,它支持Gif、WebP、缩略图，甚至是Video,所以更应该是一个媒体缓存

(2).支持优先级处理
(3).与Activity/Fragment生命周期一致,支持trimMemory：Glide对每个context都保持一个RequestManager,通过FragmentTransaction保持与Activity/Fragment生命周期一致,并且有对应的trimMemory接口实现可供调用
(4).支持okhttp、Volley
Glide默认通过HttpURLConnection获取数据,可配合okhttp或Volley使用（ImageLoader、Picasso也都支持okhttp、Volley）

(5).内存友好
a.Glide的内存缓存有个active的设计
从内存缓存中获取数据时,不像一般的实现用get,而是用remove,再将这个缓存数据放到一个value为软引用的activeResources map中,并计数引用数,在图片加载完成后进行判断,如果引用计数为空则回收掉
b.内存缓存更小图片
Glide以url、view_width、view_height、屏幕的分辨率等作为联合主键(key),将处理后的图片缓存在内存缓存中,而不是原始图片以节省大小
c.与Activity/Fragment声明周期一致,支持trimMemory
d.图片默认使用RGB_565而不是ARGB_888。虽然清晰度差些,但是图片更小,也可配置到ARGB_888

其他：Glide可通过signature或不适用本地缓存支持url过期



备注：Glide框架的使用请参考  Glide框架的基本使用


二、Picasso（https://github.com/square/picasso  ） 
是Square公司开源的一个Android平台上的图片加载框架，简单易用


Downloader

DownLoader就是下载用的工具类，在Picasso当中，如果OKHttp可以使用的话，就会默认使用OKHttp，如果无法使用的话，就会使用UrlConnectionDownloader(默认使用HttpURLConnection实现)。

Cache

默认实现为LruCache，就是使用LinkedHashMap实现的一个Cache类，注意的一个地方就是，在其他的地方，我们一般默认的是限制的capacity，但是这个地方我们是限制的总共使用的内存空间。因此LruCache在实现的时候，其实简单理解就是将LinkedHashMap封装，然后基于LinkedHashMap的方法实现Cache的方法，在Cache的set()方法的时候，会不断计算当前还可以使用的空间大小，要是超出范围，则删除之前保存的数据。

ExecutorService

默认的实现为PicassoExecutorService，该类也比较简单，其实就是ThreadPoolExecutor，在其功能的基础上继续封装，在其中有一个比较细心的功能就是，Picasso通过PicassoExecutorService设置线程数量，来调整在2G/3G/4G/WiFi不同网络情况下的不同表现。

RequestTransformer

ReqeustTransformer是一个接口，用来预处理Reqeust，可以用来将请求进行预先处理，比如改个域名啥的。

Stats

主要是一些统计信息，比如cache hit/miss，总共下载的文件大小，下载过的图片数量，转换的图片数量等等。

Dispatcher

Picasso当中，分发任务的线程，这是我们以后要重点研究的一个类，先标记一下，这个Dispatcher主要做了以下的事情：

启动了一个DispatcherThread线程初始化了一个用来处理消息的DispatcherHandler，注意，根据Dispatcher中默认配置，该Handler所有数据的处理是在DispatcherThread之上。初始化并注册了一个网络状态广播接收器。


1.总体设计及流程
整个库分为Dispatcher、RequestHandler、Downloader、PicassoDrawable等模块,Dispatcher负责分发和处理Action,包括提交、暂停、继续、取消、网络状态变化、重试等等。

简单来说,Picasso收到加载及显示图片的任务,创建Request并将它交给Dispatcher，Dispatcher分发任务到具体RequestHandler,任务通过MemoryCache和Handler(数据获取接口)获取图片,图片获取成功后通过PicassoDrawable显示到Target中

注:上面的Data的File system部分,Picasso没有自定义本地缓存的接口,默认使用http本地缓存,API9以上使用okhttp,以下使用HttpURLConnection，因此如果要自定义 本地缓存就要冲定义Downloader。



2、图片加载流程：

1）.初始化Picasso，实例化其唯一的对象。

2）.根据传入的Url、File、resource Id，构建ReqeustCreator对象

3）.根据ReqeustCreator构建Request对象，同时根据Reqeust属性，尝试从Cache中访问数据

4）.Cache Hit，则通过回调，设置Target或者ImageView，完成该Reqeust

5）.如果Cache Miss，那么则构建相应的Action，并提交到DispatcherThread当中。

6）.Dispatcher中的Handler接收到相应的Message，调用dispatcher.performSubmit(action)进行处理。

7）.创建BitmapHunter对象，并提交到PicassoExecutorService线程池

8）.再次检查Memory Cache中已经有缓存，如果Hit，则读取缓存中的Bitmap

9）.如果Cache miss，则交给Action对应的ReqeustHandler进行处理，比如网络请求，或者从File读取图片

10）.返回结果之后，通知Dispatcher中的Handler处理结果。

11）.DispatcherThread中将BitmapHunter的结果打包(batch)，最快200ms打包一次。通知主线程HANDLER进行处理

12）.主线程HANDLER接收打包的BitmapHunter，对最后的结果进行分发。

注意：Picasso框架没有实现磁盘缓存，配合OkHttp进行实现。



3.Picasso优点
(1).自带统计监控功能：支持图片缓存使用的监控，包括缓存命中率、已使用内存大小、节省的流量等
(2).支持优先级处理：每次任务调度前会选择优先级高的任务,比如App页面中Banner的优先级高于Icon时就很使用
(3).支持延迟到图片尺寸计算完成加载
(4).支持飞行模式、并发线程数根据网络类型而变：手机切换到飞行模式或网络类型变换时会自动调整线程池最大并发数,比如wifi最大并发为4,4g为3,3g为2。这里Picasso根据网络类型来决定最大并发数,而不是CPU的核数
(5)."无"本地缓存：不是说没有本地缓存,而是Picasso自己没有实现,交给了okhttp去实现,这样的好处是可以通过请求Response Header中的Cache-Control及Expired控制图片的过期时间

(6)Picasso实现了图片的异步加载,并解决了Android中加载图片时常见的一些问题,它有以下特点:

在Adapter中取消了不在视图范围内的ImageView的资源加载,因为可能会产生图片错位
使用复杂的图片转换技术降低内存的使用
自带内存和硬盘的二级缓存机制


归纳：

优点：图片质量高
缺点：加载速度一般
特点：只缓存一个全尺寸的图片，根据需求的大小在压缩转换

4.Picasso提供了以下功能
 提供内存和磁盘缓存，默认开启，可以设置不进行缓存  
图片加载过程中默认显示的图片 
图片加载失败或出错后显示的图片 
图片加载成功或失败的回调 
自定义图片大小、自动测量ImageView大小、裁剪、旋转图片等 
对图片进行转换 
标签管理，暂停和恢复图片加载 
请求优先级管理 
可以从不同来源加载图片，网络、Resources、assets、files、content providers 
更加丰富的扩展功能

备注：Picasso 框架的使用请参考   Picasso框架的基本使用

三、Fresco （https://github.com/facebook/fresco  ）

是 Facebook 出品，他是新一代的图片加载库，我们知道 Android 应用程序可用的内存有限，经常会因为图片加载导致 OOM，所以Fresco他将图片放到一个特别的内存区域叫 Ashmem 区，就是属于 Native 堆，图片将不再占用 App 的内存，能大大的减少 OOM。



1.基本概念
(1)RequestManager:请求生成和管理模块
(2)Engine:引擎部分,负责创建任务(获取数据),并调度执行
(3)GetDataInterface:数据获取接口,负责从各个数据源获取数据,比如:从内存缓存获取数据,从本地缓存获取数据,下载器从网络获取数据等
(4)Displayer:资源(图片)显示器,用于显示或操作资源
(5)Processor:资源(图片)处理器,负责处理图片,比如旋转、压缩、截取等

注:以上概念的称呼在不同的框架中可能不同,如:Displayer在ImageLoader中叫ImageAware,在Picasoo和Glide中叫Target



2.Fresco优点

(1)两个内存缓存和Native缓存构成了三级缓存
(2)支持流式,可以类似网页上模糊渐进式显示图片

(3)十分强大，使用起来非常流畅，内存管理不用愁，不用担心OOM

(4)图片加载时可在布局中直接设置加载动画等等,代码量大大减少
(5)支持多帧动画图片,如gif,WebP


Fresco是一个强大的图片加载组件
Fresco中设计有一个叫做image pipeline 的模块。他负责从网络，从本地文件系统，本地资源加载图片。为了最大限度上节省空间和CPU时间，它含有3级缓存的设计(额，没三级能拿出手？)
Fresco中设计有一个叫做Drawees模块，方便地显示loading图，当图片不再显示在屏幕上时，及时地释放内存和空间占用。
Fresco支持Android2.3及以上系统
支持图像渐进式呈现，大公司出品，后期维护有保障

缺点：
1,必须使用fresco自定义的控件,如果需求更换,想要更换其他图片加载框架会有一定的麻烦,比如必须要改布局
2,方法数太多,多达近4k方法,对于比较大的项目来说简直是压死骆驼的最后一个稻草,整项目方法数超过65k,不 得不分包.而且打包之后整个项目整整多了3M.确实大得很.
3,必须全套使用fresco的图片加载,否则连获取简简单单的一个缓存中的bitmap都异常费劲

特点：有两级内存一级文件的缓存机制，并且有自己特别的内存区域来处理缓存，避免oom



备注：Fresco框架的使用请参考  Fresco 框架的基本使用 


四、Universal-Image-Loader（https://github.com/nostra13/Android-Universal-Image-Loader ）


ImageLoader 图片加载器，对外的主要 API，采取了单例模式，用于图片的加载和显示。
MemoryCache图片内存换成。默认使用了 LRU 算法。 LRU: Least Recently Used 近期最少使用算法, 选用了基于链表结构的 LinkedHashMap 作为存储结构。假设情景：内存缓存设置的阈值只够存储两个 bitmap 对象，当 put 第三个 bitmap 对象时，将近期最少使用的 bitmap 对象移除。

DiskCache图片磁盘缓存，默认使用LruDiskCache算法，在缓存满时删除最近最少使用的图片；缓存目录下名为journal的文件记录缓存的所有操作



1.总体设计及流程
整个库分为ImageLoaderEngine,Cache、ImageDownloader、ImageDecoder、BitmapDisplayer、BitmapProcessor五大模块,其中,Cache分为MemoryCache和DiskCache两部分

简单来说,ImageLoader收到加载及显示图片的任务,将任务交给ImageLoaderEngine,ImageLoaderEngine分发任务到具体线程池去执行,任务通过Cache及ImageDownloader获取图片,中间可能经过BitmapProcessor和ImageDecoder处理,最终转换为Bitmap交给BitmapDisplayer在ImageAware中显示



2、图片加载流程

1）.判断图片的内存缓存是否存在，若存在直接执行步骤 8；
2）.判断图片的磁盘缓存是否存在，若存在直接执行步骤 5；

3.ImageDownloader从网络上下载图片；

4.将图片缓存在磁盘上；

5.ImageDecoder将图片 decode 成 bitmap 对象；

6.BitmapProcessor根据DisplayImageOptions配置对图片进行预处理(Pre-process Bitmap)；

7.将 bitmap 对象缓存到内存中；

8.根据DisplayImageOptions配置对图片进行后处理(Post-process Bitmap)；

9.执行DisplayBitmapTask将图片显示在相应的控件上。



3.ImageLoader优点

(1).支持下载进度监听
(2).可在View滚动中暂停图片加载
通过PauseOnScrollListener接口可在View滚动中暂停图片加载
(3).默认实现多种内存缓存算法：如Size最大先删除、使用最少先删除、最近最少使用、先进先删除、时间最长先删除等
(4).支持本地缓存文件名规则定义


归纳

优点：丰富的配置选项
缺点：2016年已停止更新了
特点：三级缓存的策略

备注：Universal-Image-Loader框架的使用请参考Universal-Image-Loader框架的基本使用


总结
1、Glide 默认的 Bitmap 格式是 RGB_565 格式，而 Picasso 默认的是 ARGB_8888 格式，这个内存开销要小一半。
2、Glide 支持加载 Gif 动态图，而 Picasso 不支持该特性。
3、在磁盘缓存方面，Picasso 只会缓存原始尺寸的图片，而 Glide 缓存的是多种规格，也就意味着 Glide 会根据你 ImageView 的大小来缓存相应大小的图片尺寸，比如你 ImageView 大小是200*200，原图是 400*400 ，而使用 Glide 就会缓存 200*200 规格的图，而 Picasso 只会缓存 400*400 规格的。这个改进就会导致 Glide 比 Picasso 加载的速度要快，毕竟少了每次裁剪重新渲染的过程。

4、Fresco确实强大，加载大图Fresco最屌，但比较庞大，推荐在主要都是图片的app中使用，一般的app使用Glide和Picasso就够了！

5、Glide使自身内部已经实现了缓存策略，使得开发者摆脱Android图片加载的琐碎事务，专注逻辑业务的代码。短短几行简单明晰的代码，即可完成大多数图片从网络（或者本地）加载、显示的功能需求。

6、Unibersal-ImageLoader功能多，灵活使用配置

7 、 Picasso使用复杂的图片压缩转换来尽可能的减少内存消耗
————————————————

                            版权声明：本文为博主原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接和本声明。

原文链接：https://blog.csdn.net/SiwenYY/article/details/75212528