# Glide

## 一、用法：

```java
Glide.with(context).load(imageUrl).into(imageView);
```



## 二、源码分析

## 1.Glide.with(context)做了什么？

a.获取RequestManager，每一个context对应着一个RequestManager；

b.让Glide与Fragment/Activity进行生命周期的绑定，根据生命周期让RequestManager管理是否继续请求数据和释放内存，防止内存泄漏





```java
class Glide {
    ...
    ...
    @NonNull
    public static RequestManager with(@NonNull Context context) {
        // getRetriever获取的是RequestManagerRetriever
        return getRetriever(context).get(context);
    }
}
```

```java
public class RequestManagerRetriever implements Handler.Callback {
    	...
        ...
	@NonNull
	public RequestManager get(@NonNull Context context) {
        // 根据context的类型进行分类，因为要通过Fragment/Activity类型来获取Lifecycle
        if (context == null) {
            throw new IllegalArgumentException("You cannot start a load on a null Context");
        } else if (Util.isOnMainThread() && !(context instanceof Application)) {
            if (context instanceof FragmentActivity) {
                return get((FragmentActivity) context);
            } else if (context instanceof ContextWrapper
                       // Only unwrap a ContextWrapper if the baseContext has a non-null application context.
                       // Context#createPackageContext may return a Context without an Application instance,
                       // in which case a ContextWrapper may be used to attach one.
                       && ((ContextWrapper) context).getBaseContext().getApplicationContext() != null) {
                return get(((ContextWrapper) context).getBaseContext());
            }
        }

        return getApplicationManager(context);
    }

    @NonNull
    public RequestManager get(@NonNull FragmentActivity activity) {
      	...
        ...
        return lifecycleRequestManagerRetriever.getOrCreate(
            activity,
            glide,
            activity.getLifecycle(),
            activity.getSupportFragmentManager(),
            isActivityVisible);
    }

    @NonNull
    public RequestManager get(@NonNull Fragment fragment) {
        ...
        ...
        return lifecycleRequestManagerRetriever.getOrCreate(
            context, glide, fragment.getLifecycle(), fm, fragment.isVisible());
    }
}
```

```java

final class LifecycleRequestManagerRetriever {
    ...
    ...
	RequestManager getOrCreate(
        Context context,
        Glide glide,
        final Lifecycle lifecycle,
        FragmentManager childFragmentManager,
        boolean isParentVisible) {
        Util.assertMainThread();
        RequestManager result = getOnly(lifecycle);
        if (result == null) {
            LifecycleLifecycle glideLifecycle = new LifecycleLifecycle(lifecycle);
            result =
                factory.build(
                glide,
                glideLifecycle,
                new SupportRequestManagerTreeNode(childFragmentManager),
                context);
            // 这是个HashMap类型，键是lifecycle，值是RequestManager，每一个context对应着一个RequestManager
            lifecycleToRequestManager.put(lifecycle, result);
            ...
        return result;
    }
}
```





## 2.Glide.with(context).load(imageUrl)做了什么？

只是构建一个RequestBuilder



## 3.Glide.with(context).load(imageUrl).into(imageView)做了什么？

a.获取imageView的ScaleType，构建requestOptions

b.通过上一步构建的RequestBuilder，让成员变量requestManager开始加载图片任务，再到SingleRequest



```java
public class RequestBuilder<TranscodeType> extends BaseRequestOptions<RequestBuilder<TranscodeType>>
    implements Cloneable, ModelTypes<RequestBuilder<TranscodeType>> {
    ...
    @NonNull
    public ViewTarget<ImageView, TranscodeType> into(@NonNull ImageView view) {
        Util.assertMainThread();
        Preconditions.checkNotNull(view);

        ...

        return into(
            glideContext.buildImageViewTarget(view, transcodeClass),
            /* targetListener= */ null,
            requestOptions,
            Executors.mainThreadExecutor());
    }

    private <Y extends Target<TranscodeType>> Y into() {
        ...
        // 1.这里构建出来的request类型是SingleRequest
        Request request = buildRequest(target, targetListener, options, callbackExecutor);
        ...
        requestManager.clear(target);
        target.setRequest(request);
        // 2.进行图片加载的下一步准备
        requestManager.track(target, request);

        return target;
    }

    private Request obtainRequest(
        Object requestLock,
        Target<TranscodeType> target,
        RequestListener<TranscodeType> targetListener,
        BaseRequestOptions<?> requestOptions,
        RequestCoordinator requestCoordinator,
        TransitionOptions<?, ? super TranscodeType> transitionOptions,
        Priority priority,
        int overrideWidth,
        int overrideHeight,
        Executor callbackExecutor) {
        return SingleRequest.obtain(
            context,
            glideContext,
            requestLock,
            model,
            transcodeClass,
            requestOptions,
            overrideWidth,
            overrideHeight,
            priority,
            target,
            targetListener,
            requestListeners,
            requestCoordinator,
            glideContext.getEngine(),
            transitionOptions.getTransitionFactory(),
            callbackExecutor);
    }
}
```

```java
public class RequestManager
    implements ComponentCallbacks2, LifecycleListener, ModelTypes<RequestBuilder<Drawable>> {
    ...
    synchronized void track(@NonNull Target<?> target, @NonNull Request request) {
        targetTracker.track(target);
        // 3.让requestTracker请求
        requestTracker.runRequest(request);
    }
}
```

```java
public class RequestTracker {
	...
	public void runRequest(@NonNull Request request) {
        requests.add(request);
        if (!isPaused) {
          // 4.回到步骤1，让SingleRequest调用begin
          request.begin();
        } else {
          request.clear();
          if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Paused, delaying request");
          }
          pendingRequests.add(request);
        }
	}
}
```



```java
public final class SingleRequest<R> implements Request, SizeReadyCallback, ResourceCallback {
    ...
        @Override
        public void begin() {
        synchronized (requestLock) {
            ...
                if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
                    // 5.获取有效的ImageView的宽高之后，通过engin加载图片，从缓存或者网络加载获取
                    onSizeReady(overrideWidth, overrideHeight);
                } else {
                    target.getSize(this);
                }

            ...
        }
    }

    @Override
    public void onSizeReady(int width, int height) {
        stateVerifier.throwIfRecycled();
        synchronized (requestLock) {
            ...
            // 6.让engin开始load
            loadStatus =
                engine.load(
                glideContext,
                model,
                requestOptions.getSignature(),
                this.width,
                this.height,
                requestOptions.getResourceClass(),
                transcodeClass,
                priority,
                requestOptions.getDiskCacheStrategy(),
                requestOptions.getTransformations(),
                requestOptions.isTransformationRequired(),
                requestOptions.isScaleOnlyOrNoTransform(),
                requestOptions.getOptions(),
                requestOptions.isMemoryCacheable(),
                requestOptions.getUseUnlimitedSourceGeneratorsPool(),
                requestOptions.getUseAnimationPool(),
                requestOptions.getOnlyRetrieveFromCache(),
                this,
                callbackExecutor);

            ...
        }
    }

}
```

```java
public class Engine
    implements EngineJobListener,
MemoryCache.ResourceRemovedListener,
EngineResource.ResourceListener {

    public <R> LoadStatus load(
        GlideContext glideContext,
        Object model,
        Key signature,
        int width,
        int height,
        Class<?> resourceClass,
        Class<R> transcodeClass,
        Priority priority,
        DiskCacheStrategy diskCacheStrategy,
        Map<Class<?>, Transformation<?>> transformations,
        boolean isTransformationRequired,
        boolean isScaleOnlyOrNoTransform,
        Options options,
        boolean isMemoryCacheable,
        boolean useUnlimitedSourceExecutorPool,
        boolean useAnimationPool,
        boolean onlyRetrieveFromCache,
        ResourceCallback cb,
        Executor callbackExecutor) {
        long startTime = VERBOSE_IS_LOGGABLE ? LogTime.getLogTime() : 0;

        EngineKey key =
            keyFactory.buildKey(
            model,
            signature,
            width,
            height,
            transformations,
            resourceClass,
            transcodeClass,
            options);

        EngineResource<?> memoryResource;
        synchronized (this) {
            // 7.从缓存中取资源，首先是acitve缓存，这块缓存是跟界面绑定的，如果没找到就去Lrucache中找，这块缓存是在整个Glide中管理的
            memoryResource = loadFromMemory(key, isMemoryCacheable, startTime);

            if (memoryResource == null) {
                // 10.如果内存缓存没有找到资源，则去磁盘中找，再没有就进行网络请求
                return waitForExistingOrStartNewJob(
                    glideContext,
                    model,
                    signature,
                    width,
                    height,
                    resourceClass,
                    transcodeClass,
                    priority,
                    diskCacheStrategy,
                    transformations,
                    isTransformationRequired,
                    isScaleOnlyOrNoTransform,
                    options,
                    isMemoryCacheable,
                    useUnlimitedSourceExecutorPool,
                    useAnimationPool,
                    onlyRetrieveFromCache,
                    cb,
                    callbackExecutor,
                    key,
                    startTime);
            }
        }

        // Avoid calling back while holding the engine lock, doing so makes it easier for callers to
        // deadlock.
        cb.onResourceReady(
            memoryResource, DataSource.MEMORY_CACHE, /* isLoadedFromAlternateCacheKey= */ false);
        return null;
    }

    @Nullable
    private EngineResource<?> loadFromMemory(
        EngineKey key, boolean isMemoryCacheable, long startTime) {
        if (!isMemoryCacheable) {
            return null;
        }

        // 8.首先是acitve缓存，使用HashMap保存弱引用数据，不适用强引用是为了防止内存泄漏
        EngineResource<?> active = loadFromActiveResources(key);
        if (active != null) {
            if (VERBOSE_IS_LOGGABLE) {
                logWithTimeAndKey("Loaded resource from active resources", startTime, key);
            }
            return active;
        }
		// 9.如果步骤8没找到就去Lrucache中找，使用的是LruResourceCache进行管理
        EngineResource<?> cached = loadFromCache(key);
        if (cached != null) {
            if (VERBOSE_IS_LOGGABLE) {
                logWithTimeAndKey("Loaded resource from cache", startTime, key);
            }
            return cached;
        }

        return null;
    }

}
```





## 三、扩展（LRU缓存）

Lru缓存机制，核心是用到LinkedHasdMap，在LruResourceCache的父类中使用的就是它

```java
public class LruCache<T, Y> {
    // 构造函数的第三个参数accessOrder为true，就是可以让访问到的元素调整存放到双向链表尾部，新的元素也是从尾部插入，淘汰掉链表头部的元素
    private final Map<T, Entry<Y>> cache = new LinkedHashMap<>(100, 0.75f, true);
}
```

