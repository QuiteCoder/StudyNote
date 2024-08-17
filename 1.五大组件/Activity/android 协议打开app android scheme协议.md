# android 协议打开app android scheme协议

### 什么是 URL Scheme？

> [android](https://so.csdn.net/so/search?q=android&spm=1001.2101.3001.7020)中的scheme是一种页面内跳转协议，是一种非常好的实现机制，通过定义自己的scheme协议，可以非常方便跳转app中的各个页面；通过scheme协议，服务器可以定制化告诉App跳转那个页面，可以通过通知栏消息定制化跳转页面，可以通过H5页面跳转页面等。

### URL Scheme应用场景：

> 客户端应用可以向操作系统注册一个 URL scheme，该 scheme 用于从浏览器或其他应用中启动本应用。通过指定的 URL 字段，可以让应用在被调起后直接打开某些特定页面，比如商品详情页、活动详情页等等。也可以执行某些指定动作，如完成支付等。也可以在应用内通过 html 页来直接调用显示 app 内的某个页面。

#### 综上URL Scheme使用场景大致分以下几种：

- 服务器下发跳转路径，客户端根据服务器下发跳转路径跳转相应的页面
- H5页面点击锚点，根据锚点具体跳转路径APP端跳转具体的页面
- APP端收到服务器端下发的PUSH通知栏消息，根据消息的点击跳转路径跳转相关页面
- APP根据URL跳转到另外一个APP指定页面

#### URL Scheme协议格式：

先来个完整的URL Scheme协议格式：
**xl://goods:8888/goodsDetail?goodsId=10011002**

通过上面的路径 Scheme、Host、port、path、query全部包含，基本上平时使用路径就是这样子的。

- xl代表该Scheme 协议名称
- goods代表Scheme作用于哪个地址域
- goodsDetail代表Scheme指定的页面
- goodsId代表传递的参数
- 8888代表该路径的端口号

### URL Scheme如何使用：

#### 1.）在AndroidManifest.xml中对标签增加设置Scheme

```xml
<activity
          android:name=".GoodsDetailActivity"
          android:theme="@style/AppTheme">
    <!--要想在别的App上能成功调起App，必须添加intent过滤器-->
    <intent-filter>
        <!--协议部分，随便设置-->
        <data android:scheme="xl" android:host="goods" android:path="/goodsDetail" android:port="8888"/>
        <!--下面这几行也必须得设置-->
        <category android:name="android.intent.category.DEFAULT"/>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.BROWSABLE"/>
    </intent-filter>
</activity>
```

#### 2.）获取Scheme跳转的参数

```java
Uri uri = getIntent().getData();
if (uri != null) {
    // 完整的url信息
    String url = uri.toString();
    Log.e(TAG, "url: " + uri);
    // scheme部分
    String scheme = uri.getScheme();
    Log.e(TAG, "scheme: " + scheme);
    // host部分
    String host = uri.getHost();
    Log.e(TAG, "host: " + host);
    //port部分
    int port = uri.getPort();
    Log.e(TAG, "host: " + port);
    // 访问路劲
    String path = uri.getPath();
    Log.e(TAG, "path: " + path);
    List<String> pathSegments = uri.getPathSegments();
    // Query部分
    String query = uri.getQuery();
    Log.e(TAG, "query: " + query);
    //获取指定参数值
    String goodsId = uri.getQueryParameter("goodsId");
    Log.e(TAG, "goodsId: " + goodsId);
}
```

#### 3.）调用方式

网页上

```javascript
<a href="xl://goods:8888/goodsDetail?goodsId=10011002">打开商品详情</a>
```

原生调用

```java
Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse("xl://goods:8888/goodsDetail?goodsId=10011002"));
startActivity(intent);
```

#### 4.）如何判断一个Scheme是否有效

```java
PackageManager packageManager = getPackageManager();
Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("xl://goods:8888/goodsDetail?goodsId=10011002"));
List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
boolean isValid = !activities.isEmpty();
if (isValid) {
    startActivity(intent);
}
```

### 实际使用中的一些小细节

当自定义的URL配置在LAUNCHER对应的Activity上时，上述配置就足够了。

**但是当自定义的URL配置在非LAUNCHER对应的Activity时，还需要增加额外几步操作。**

> 问题一：使用自定义的URL启动Activity时，默认是已FLAG_ACTIVITY_NEW_TASK的方式启动的，所以可能存在URL启动的Activity跟应用已启动的Activity不再同一个堆栈的现象。
>
> 
>
> 解决方式：这种情况下，需要在manifest中将Activity多配置一个==taskAffinity==属性，约束URL启动的Activity与应用自身的启动的Activity在同一个堆栈中。

> 问题二：应用A使用url的方式唤起应用B的Activity时，可能存在应用B的Activity启动了，但是堆栈仍然在后台的现象，即应用B的Activity没有聚焦的问题。
>
> 
>
> 解决方式：这种情况下，应用B的Activity收到启动的请求后，可以主动将Activity对应的堆栈移动到最前端。

```java
ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);  
activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
```

使用这种方式需要注意的是该api是在Android 3.0（api 11）以后才提供的，现在基本上手机rom版本都是Android4.4以上了，就不太需要关注3.0一下怎么处理了，且使用这个需要在manifest中申请==android.permission.REORDER_TASKS==权限。