# Retrofit源码解析

## 使用demo

```kotlin
val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

val service: GitHubService = retrofit.create<GitHubService>(GitHubService::class.java)
val repos: Call<List<Repo>> = service.listRepos("QuiteCoder")
//repos.execute()//同步
//因为看这里是一个接口方法，看不到具体实现，我们往上看retrofit.create，最终目的是看Call类型的enqueue函数调用
repos.enqueue(object : Callback<List<Repo>?> {
```

思路：retrofit.create构建出具体的代理类，再通过代理类的调用（service.listRepos("QuiteCoder")）得到有特定网络请求参数的call对象，进行call.enqueue网络请求，1、找出retrofit.create得到的代理对象；2、找出service.listRepos的call对象；3、看enqueue方法的具体实现

## 一、找出retrofit.create得到的代理对象，请看注释第8句



```java
/***
*
* ***************** 对注释8的解释********************
*
* service.listRepos("QuiteCoder")我们调用这一句就相当于下面这块代码，这就是大概的动态代理模型
* 就相当于invoke方法就是构建出Call对象，接着就是死磕invoke方法了
**/
public class ProxyGitHubService implements GitHubService{
    InvocationHandler invocationHandler = new InvocationHandler() {
        private final Platform platform = Platform.get();
        private final Object[] emptyArgs = new Object[0];

        @Override public @Nullable Object invoke(Object proxy, Method method,
                @Nullable Object[] args) throws Throwable {
            // If the method is a method from Object then defer to normal invocation.
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            if (platform.isDefaultMethod(method)) {
                return platform.invokeDefaultMethod(method, service, proxy, args);
            }
            return loadServiceMethod(method).invoke(args != null ? args : emptyArgs);
        }
    });
    @NotNull
    @Override
    public Call<List<Repo>> listRepos(@NotNull String user) {
        return invocationHandler.invoke(proxy, "listRepos", user);
    }
}
```



### Retrofit，动态代理，合法性检验

```kotlin

class Retrofit {
    public <T> T create(final Class<T> service) {
        //1、对service参数进行检查，请看步骤一
        validateServiceInterface(service);
        
        //8、这块是动态代理，核心部分
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new InvocationHandler() {
          private final Platform platform = Platform.get();
          private final Object[] emptyArgs = new Object[0];

          @Override public @Nullable Object invoke(Object proxy, Method method,
              @Nullable Object[] args) throws Throwable {
              //9、***核心中的核心*** @{
              // If the method is a method from Object then defer to normal invocation.
              //10、翻译上面注释的意思：如果调用方法来自于object类的如toString、hashCode，就直接调用了
              if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
              }
              //11、判断是不是default声明的方法
              if (platform.isDefaultMethod(method)) {
                return platform.invokeDefaultMethod(method, service, proxy, args);
              }
              //12、正常情况会走这里，代理包装调用方法，这就是你能用注解的方式调用网络请求的秘密了，
              //13、invoke才是最终的调用，跳转查看发现竟然是个抽象方法，看步骤二
              return loadServiceMethod(method).invoke(args != null ? args : emptyArgs);
              //  @}
          }
        });
    }
    
    /***2、步骤一、对service参数进行检查***/
    private void validateServiceInterface(Class<?> service) {
        //3、是不是interface类型？
        if (!service.isInterface()) {
          throw new IllegalArgumentException("API declarations must be interfaces.");
        }
        Deque<Class<?>> check = new ArrayDeque<>(1);
        check.add(service);
        while (!check.isEmpty()) {
          Class<?> candidate = check.removeFirst();

            //4、有没有带泛型？
          if (candidate.getTypeParameters().length != 0) {
            StringBuilder message = new StringBuilder("Type parameters are unsupported on ")
                .append(candidate.getName());
            if (candidate != service) {
              message.append(" which is an interface of ")
                  .append(service.getName());
            }
            throw new IllegalArgumentException(message.toString());
          }
          Collections.addAll(check, candidate.getInterfaces());
        }
        //5、validateEagerly是一个开关，开发时打开有助于快速发现问题
        if (validateEagerly) {
          Platform platform = Platform.get();
          for (Method method : service.getDeclaredMethods()) {
              //6、第一判断接口有没有存在具体实现的方法，第二判断接口有没有存在静态方法。PS：java8之前是不允许interface声明静态方法的
            if (!platform.isDefaultMethod(method) && !Modifier.isStatic(method.getModifiers())) {
               //7、这里的工作就是加载service接口边的方法，可以快速发现问题
              loadServiceMethod(method);
            }
          }
        }
      }
	    
  /***14、步骤二、分析loadServiceMethod***/
  //15、loadServiceMethod构建出什么对象再查看invoke的最终实现
  ServiceMethod<?> loadServiceMethod(Method method) {
    ServiceMethod<?> result = serviceMethodCache.get(method);
    if (result != null) return result;

    synchronized (serviceMethodCache) {
      result = serviceMethodCache.get(method);
      if (result == null) {
        //16、构建出一个ServiceMethod放到Map里，并返回，步骤三跟踪 parseAnnotations
        result = ServiceMethod.parseAnnotations(this, method);
        serviceMethodCache.put(method, result);
      }
    }
    return result;
  }
}

```



## 二、找出service.listRepos的call对象

### ServiceMethod，拼接网络请求的参数

```java
/***17、步骤三、构建ServiceMethod***/
abstract class ServiceMethod<T> {
  static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
    //18、这一步是解析interface里边的注解，拼接参数
    RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);

    Type returnType = method.getGenericReturnType();
    if (Utils.hasUnresolvableType(returnType)) {
      throw methodError(method,
          "Method return type must not include a type variable or wildcard: %s", returnType);
    }
    if (returnType == void.class) {
      throw methodError(method, "Service methods cannot return void.");
    }
    //19、将参数丢进里边构建call对象，就是用到okhttp了，接下来看步骤四
    return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
  }

  abstract @Nullable T invoke(Object[] args);
}
```



### HttpServiceMethod，invoke的最终实现，构建Call对象并且二次封装

```java
/***20、步骤四、构建HttpServiceMethod，他是继承自ServiceMethod，有实现了invoke方法***/
abstract class HttpServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {
  
  static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
      Retrofit retrofit, Method method, RequestFactory requestFactory) {
	
      //21、构建call的适配器
      CallAdapter<ResponseT, ReturnT> callAdapter =
        createCallAdapter(retrofit, method, adapterType, annotations);
      ...
      ...
      //如果不支Kotlin持协程方法
      if (!isKotlinSuspendFunction) {
        //22、构建了CallAdapted并传入了CallAdapter，用CallAdapted调用invoke方法
        return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
      }
  }
    
  /**************************** invoke *******************************/  
  //23、非常的惊喜，这里具体实现了invoke方法，构建了OkHttpCall对象，内部一定是封装了Okhttp
  @Override final @Nullable ReturnT invoke(Object[] args) {
    Call<ResponseT> call = new OkHttpCall<>(requestFactory, args, callFactory, responseConverter);
    //24、但是这里没有直接返回call对象，而是再经过adapt的包装，他到底有何作用呢？看CallAdapted的adapt方法
    return adapt(call, args);
  }
  /***************************** invoke ******************************/ 
    


  
  static final class CallAdapted<ResponseT, ReturnT> extends HttpServiceMethod<ResponseT, ReturnT> {
    private final CallAdapter<ResponseT, ReturnT> callAdapter;

    CallAdapted(RequestFactory requestFactory, okhttp3.Call.Factory callFactory,
        Converter<ResponseBody, ResponseT> responseConverter,
        CallAdapter<ResponseT, ReturnT> callAdapter) {
      super(requestFactory, callFactory, responseConverter);
      this.callAdapter = callAdapter;
    }
    //25、CallAdapter实现了adapt，
    //26、看createCallAdapter方法构建出CallAdapter的地方
    @Override protected ReturnT adapt(Call<ResponseT> call, Object[] args) {
      return callAdapter.adapt(call);
    }
  }
  
  //27、继续跟踪retrofit.callAdapter，看Retrofit类
  private static <ResponseT, ReturnT> CallAdapter<ResponseT, ReturnT> createCallAdapter(
      Retrofit retrofit, Method method, Type returnType, Annotation[] annotations) {
    try {
      //noinspection unchecked
      return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter(returnType, annotations);
    } catch (RuntimeException e) { // Wide exception range because factories are user code.
      throw methodError(method, e, "Unable to create call adapter for %s", returnType);
    }
  }
    
}
```

```java
class Retrofit {
    //28、看nextCallAdapter
  public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
    return nextCallAdapter(null, returnType, annotations);
  }
  
  public CallAdapter<?, ?> nextCallAdapter(@Nullable CallAdapter.Factory skipPast, Type returnType,
      Annotation[] annotations) {
    Objects.requireNonNull(returnType, "returnType == null");
    Objects.requireNonNull(annotations, "annotations == null");

    int start = callAdapterFactories.indexOf(skipPast) + 1;
    for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
      //29、从callAdapterFactories这个list集合拿到的adapter，重点看.get(i).get()方法，callAdapterFactories元素在哪里添加的？
      CallAdapter<?, ?> adapter = callAdapterFactories.get(i).get(returnType, annotations, this);
      if (adapter != null) {
        return adapter;
      }
    }
  }
    
  public Retrofit build() {
      ...
      ...
      List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(this.callAdapterFactories);
      //30、跟踪platform.defaultCallAdapterFactories(callbackExecutor)，跟踪get方法
      callAdapterFactories.addAll(platform.defaultCallAdapterFactories(callbackExecutor));
  }
}
```



### Platform，构建DefaultCallAdapterFactory和定义MainThreadExecutor

```java
class Platform {
    List<? extends CallAdapter.Factory> defaultCallAdapterFactories(
      @Nullable Executor callbackExecutor) {
    //31、跟踪DefaultCallAdapterFactory的get方法
    DefaultCallAdapterFactory executorFactory = new DefaultCallAdapterFactory(callbackExecutor);
    return hasJava8Types
        ? asList(CompletableFutureCallAdapterFactory.INSTANCE, executorFactory)
        : singletonList(executorFactory);
  }
    
static final class Android extends Platform {
    Android() {
      super(Build.VERSION.SDK_INT >= 24);
    }

    @Override public Executor defaultCallbackExecutor() {
      return new MainThreadExecutor();
    }

    //将callback切换到主线就靠他了
    static class MainThreadExecutor implements Executor {
      private final Handler handler = new Handler(Looper.getMainLooper());

      @Override public void execute(Runnable r) {
        handler.post(r);
      }
    }
  }
}
```



### DefaultCallAdapterFactory，包装Call对象，把网络请求放到主线程

```java
final class DefaultCallAdapterFactory extends CallAdapter.Factory {
    
  //32、这里发现了get方法，return new CallAdapter，我们看到adapt的最终实现最终
  @Override 
  public @Nullable CallAdapter<?, ?> get(
      Type returnType, Annotation[] annotations, Retrofit retrofit) {
    if (getRawType(returnType) != Call.class) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalArgumentException(
          "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>");
    }
    final Type responseType = Utils.getParameterUpperBound(0, (ParameterizedType) returnType);

    final Executor executor = Utils.isAnnotationPresent(annotations, SkipCallbackExecutor.class)
        ? null
        : callbackExecutor;

    return new CallAdapter<Object, Call<?>>() {
      @Override 
      public Type responseType() {
        return responseType;
      }

       //33、找到这里我们的目的完成了：注释22的invoke方法return adapt(call, args)的是一个Call对象，而这里他到底还要将传进来的Call对象做什么呢？
      @Override 
      public Call<Object> adapt(Call<Object> call) {
        return executor == null
            ? call
            : new ExecutorCallbackCall<>(executor, call);
      }
    };
  }
  
  //也是一个call，把OkhttpCall包装起来
  static final class ExecutorCallbackCall<T> implements Call<T> {
    final Executor callbackExecutor;
    final Call<T> delegate;
      
    ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
      this.callbackExecutor = callbackExecutor;
      this.delegate = delegate;
    }

    //调用这个函数的就是我们demo中的repos.enqueue()，通过代理一层层的实现了
    @Override 
    public void enqueue(final Callback<T> callback) {
      Objects.requireNonNull(callback, "callback == null");

      //34、delegate就是注释21的OkhttpCall的Call对象
      delegate.enqueue(new Callback<T>() {
        @Override 
        public void onResponse(Call<T> call, final Response<T> response) {
          //35、callbackExecutor来自Platform内部类Android的静态内部类MainThreadExecutor，作用就是把onFailure、onResponse回调放到主线
          callbackExecutor.execute(() -> {
            if (delegate.isCanceled()) {
              // Emulate OkHttp's behavior of throwing/delivering an IOException on cancellation.
              callback.onFailure(ExecutorCallbackCall.this, new IOException("Canceled"));
            } else {
              callback.onResponse(ExecutorCallbackCall.this, response);
            }
          });
        }

        @Override 
        public void onFailure(Call<T> call, final Throwable t) {
          callbackExecutor.execute(() -> callback.onFailure(ExecutorCallbackCall.this, t));
        }
      });
   }
}
```



## 三、看enqueue方法的具体实现

```java
//我们回到注释21的OkhttpCall的Call对象
new OkHttpCall
    
final class OkHttpCall<T> implements Call<T> {
    
//这个方法在ExecutorCallbackCall的enqueue方法中调用
@Override public void enqueue(final Callback<T> callback) {
    Objects.requireNonNull(callback, "callback == null");

    okhttp3.Call call;
    Throwable failure;

    synchronized (this) {
      if (executed) throw new IllegalStateException("Already executed.");
      executed = true;

      call = rawCall;
      failure = creationFailure;
      if (call == null && failure == null) {
        try {
          //重点是这一句，创建出来一个okhttp3的Call，跟踪到根源就是通过OkhttpClient创建的Call
          call = rawCall = createRawCall();
        } catch (Throwable t) {
          throwIfFatal(t);
          failure = creationFailure = t;
        }
      }
    }

    if (failure != null) {
      callback.onFailure(this, failure);
      return;
    }

    if (canceled) {
      call.cancel();
    }

    //okhttp的逻辑了
    call.enqueue(new okhttp3.Callback() {
      @Override public void onResponse(okhttp3.Call call, okhttp3.Response rawResponse) {
        Response<T> response;
        try {
          //用传进来的GsonConverterFactory，构建出responseConverter来解析重新包装Response
          //源码：T body = responseConverter.convert(catchingBody);
          response = parseResponse(rawResponse);
        } catch (Throwable e) {
          throwIfFatal(e);
          callFailure(e);
          return;
        }

        try {
          //ExecutorCallbackCall的callback，将数据放到主线程
          callback.onResponse(OkHttpCall.this, response);
        } catch (Throwable t) {
          throwIfFatal(t);
          t.printStackTrace(); // TODO this is not great
        }
      }

      @Override public void onFailure(okhttp3.Call call, IOException e) {
        callFailure(e);
      }

      private void callFailure(Throwable e) {
        try {
          //ExecutorCallbackCall的callback，将数据放到主线程
          callback.onFailure(OkHttpCall.this, e);
        } catch (Throwable t) {
          throwIfFatal(t);
          t.printStackTrace(); // TODO this is not great
        }
      }
    });
  }
}
```



## 总结：

Retrofit总结
1、Retrofit.create：首先对我们创建的请求interface检查合法性
是interface类型？没有带泛型？

2、动态代理：对传进来的interface反射，获取我们声明的方法

2.1、对反射获取的Method进行解析，拆分注解、重组请求参数

2.2、构建OkHttpCall对象，将请求参数、responseConverter传入

2.3、OkHttpCall又被CallAdapter包了一层，
而CallAdapter在DefaultCallAdapterFactory中的get方法创建，
但是发现真正包装OkHttpCall的是ExecutorCallbackCall

2.4、ExecutorCallbackCall：我们执行enqueue方法，
相当于让ExecutorCallbackCall去执行OkHttpCall的enqueue方法，
这就是代理的全貌！
目的：为了在子线程网络请求，接收到数据或者报错时，把信息通过
handler.post(runnable)的方式传递到主线程

3、response的解析重组
我们可以方便的使用java bean丢进去进行网络请求，最后能拿到带数据的java bean，
功劳就是 addConverterFactory(GsonConverterFactory.create())
这实现的数据的加工，底层也是通过gson来实现的

