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

        // 8.首先是acitve缓存，使用HashMap保存弱引用数据，不是用强引用是为了防止内存泄漏
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



## 三、三级缓存是哪三级？

第一级：正在使用中的资源，使用Map保存，Map<Key, WeakReference<EngineResource<?>>> activeResources

第二级：LruCache，当View销毁时，缓存从第一级传到第二级

第三级：磁盘+网络，当第一、二级缓存没有找到，就走磁盘或者网络



在 Glide 中，`loadFromActiveResources` 是 **内存缓存** 的一部分，它负责从 **活动资源缓存（Active Resources Cache）** 中加载图片资源。活动资源缓存是 Glide 内存缓存的第一层，用于存储当前正在使用的图片资源。

------

### 1. **活动资源缓存的作用**

活动资源缓存的主要作用是：

- 存储当前正在使用的图片资源。
- 使用 **弱引用（WeakReference）** 来持有这些资源，确保当资源不再被使用时，可以被垃圾回收器回收，从而避免内存泄漏。
- 作为内存缓存的第一层，优先于 **LRU 内存缓存（Memory Cache）** 被检查。

------

### 2. **活动资源缓存的结构**

活动资源缓存的核心是一个 `Map`，其键是图片资源的缓存键（`Key`），值是对资源的弱引用（`WeakReference<EngineResource<?>>`）。

```java
final Map<Key, WeakReference<EngineResource<?>>> activeResources;
```

- **Key**：缓存键，由图片的 URL、尺寸、转换选项等生成，用于唯一标识一个资源。
- **WeakReference<EngineResource<?>>**：对资源的弱引用，确保资源不再被使用时可以被回收。

------

### 3. **loadFromActiveResources 的实现**

`loadFromActiveResources` 方法的实现如下：

```java
private EngineResource<?> loadFromActiveResources(Key key, boolean isMemoryCacheable) {
    if (!isMemoryCacheable) {
        return null;
    }
    WeakReference<EngineResource<?>> activeRef = activeResources.get(key);
    if (activeRef != null) {
        EngineResource<?> active = activeRef.get();
        if (active != null) {
            return active;
        } else {
            activeResources.remove(key);
        }
    }
    return null;
}
```

#### 3.1 **方法逻辑**

1. 检查是否启用了内存缓存（`isMemoryCacheable`），如果未启用，则直接返回 `null`。
2. 根据缓存键（`Key`）从 `activeResources` 中获取对应的弱引用（`WeakReference`）。
3. 如果弱引用存在，尝试获取其持有的资源（`EngineResource`）。
   - 如果资源存在，则返回该资源。
   - 如果资源已被回收（弱引用为 `null`），则从 `activeResources` 中移除该键。
4. 如果没有找到资源，则返回 `null`。

------

### 4. **活动资源缓存的生命周期**

活动资源缓存中的资源生命周期如下：

1. **资源被加载**：
   - 当图片资源被加载并显示时，Glide 会将其放入活动资源缓存中。
   - 此时，资源被 `Target`（例如 `ImageViewTarget`）持有，同时活动资源缓存也持有其弱引用。
2. **资源被释放**：
   - 当资源不再被 `Target` 持有时（例如 `ImageView` 被销毁或加载了新的图片），Glide 会将其从活动资源缓存中移除。
   - 如果资源仍然有效（未被垃圾回收），则将其放入 LRU 内存缓存中，供后续使用。
3. **资源被回收**：
   - 如果资源没有被任何 `Target` 持有，且活动资源缓存中的弱引用被垃圾回收器回收，则资源会被彻底释放。

------

### 5. **活动资源缓存与 LRU 内存缓存的关系**

Glide 的内存缓存分为两层：

1. **活动资源缓存（Active Resources Cache）**：
   - 存储当前正在使用的资源。
   - 使用弱引用，避免内存泄漏。
   - 优先级高于 LRU 内存缓存。
2. **LRU 内存缓存（Memory Cache）**：
   - 存储最近使用过的资源。
   - 使用 LRU（最近最少使用）算法管理缓存。
   - 当活动资源缓存中的资源被释放时，会被移动到 LRU 内存缓存中。

------

### 6. **总结**

`loadFromActiveResources` 加载的是 **当前正在使用的图片资源**，这些资源存储在活动资源缓存中。活动资源缓存是 Glide 内存缓存的第一层，使用弱引用确保资源在不被使用时可以被回收。它的主要作用是：

- 提高内存缓存的命中率。
- 避免重复加载正在使用的资源。
- 与 LRU 内存缓存配合，实现高效的内存管理。

通过活动资源缓存，Glide 能够在保证性能的同时，最大限度地减少内存占用。



## 三、扩展（LRU缓存）

Lru缓存机制，核心是用到LinkedHasdMap，在LruResourceCache的父类中使用的就是它

LRU 算法的核心思想是：

- 当缓存空间不足时，优先移除最近最少使用的数据。
- 通过维护一个双向链表LinkedHashMap，记录数据的访问顺序：
  - 每次访问数据时，将其移动到链表的头部。
  - 当需要移除数据时，从链表的尾部移除。

```java
public class LruCache<T, Y> {
    // 构造函数的第三个参数accessOrder为true，就是可以让访问到的元素调整存放到双向链表头部，当需要移除元素，就从尾部移除
    private final Map<T, Entry<Y>> cache = new LinkedHashMap<>(100, 0.75f, true);
}
```

