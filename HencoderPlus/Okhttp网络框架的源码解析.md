# Okhttp网络框架的源码解析

简单使用demo

```kotlin
val tv = findViewById<TextView>(R.id.tv_okhttp_response)
val url = "https://api.github.com/users/rengwuxian/repos"

val okHttpClient = OkHttpClient()
val request = Request.Builder()
        .url(url)
        .build()

//1、目的是看enqueue的实现，但是直接跳转过去看到是个接口，那么先看newCall构建出怎样的对象？
okHttpClient.newCall(request)
        .enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //网络请求错误回调
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread(Runnable {
                    tv.text = "response code = ${response.code().toString()}"
                })
            }
        })
```

## 一、初始化工作

okHttpClient.newCall(request) -->RealCall.newRealCall( )

```kotlin
final class RealCall implements Call {
    
  //2、实现了enqueue
  @Override public void enqueue(Callback responseCallback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    //3、这里面包含两部分，一是分析程序的错误，拿到异常的堆栈信息
    //二是enentListener监听如请求header、body开始发送，还有响应的header、body开始接收、连接的建立和关闭也能监听
    transmitter.callStart();
      
    //4、这里有三个部分dispatcher()返回什么？  .enqueue()的具体实现？  new AsyncCall()是什么？
    client.dispatcher().enqueue(new AsyncCall(responseCallback));
  }
}
```



### client.dispatcher( )

```java
public final class Dispatcher {
  //这是最大的请求数量，有提供接口设置
  private int maxRequests = 64;
  //每个主机的最大请求数量，有提供接口设置
  private int maxRequestsPerHost = 5;
    
    
  void enqueue(AsyncCall call) {
    synchronized (this) {
      //5、暂时把这些call放到list里边
      readyAsyncCalls.add(call);

      // Mutate the AsyncCall so that it shares the AtomicInteger of an existing running call to
      // the same host.
      /*
      * 6、翻译上方的注释：改变 AsyncCall 以便它共享现有运行调用的 AtomicInteger 到同一个主机,
      * 意思是：统计总请求数和每个主机的请求数，这个整数会在每个 AsyncCall 之间共享，
      * 因为Dispatcher对这两者有限制，就上面提到的maxRequests和maxRequestsPerHost
      */
      if (!call.get().forWebSocket) {
        AsyncCall existingCall = findExistingCallWithHost(call.host());
        if (existingCall != null) call.reuseCallsPerHostFrom(existingCall);
      }
    }
    promoteAndExecute();
  }
    
 private boolean promoteAndExecute() {
    assert (!Thread.holdsLock(this));

    List<AsyncCall> executableCalls = new ArrayList<>();
    boolean isRunning;
    synchronized (this) {
      //7、从readyAsyncCalls取call来执行了
      for (Iterator<AsyncCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
        AsyncCall asyncCall = i.next();

        //8、这里要经过最大请求数的判断，如果已经达到最大值了就不发起请求了
        if (runningAsyncCalls.size() >= maxRequests) break; // Max capacity.不超总请求数
        if (asyncCall.callsPerHost().get() >= maxRequestsPerHost) continue; // Host max capacity.//不超单个主机的最大请求数

        i.remove();
        asyncCall.callsPerHost().incrementAndGet();
        //9、放到list里边待会再执行
        executableCalls.add(asyncCall);
        //10、放到统计call的数量的集合里边
        runningAsyncCalls.add(asyncCall);
      }
      isRunning = runningCallsCount() > 0;
    }

    for (int i = 0, size = executableCalls.size(); i < size; i++) {
      AsyncCall asyncCall = executableCalls.get(i);
      //11、执行每个call了
      asyncCall.executeOn(executorService());
    }

    return isRunning;
  }
}
```



### RealCall 内部类 AsyncCall的executeOn方法

```java
//RealCall的executeOn方法
final class AsyncCall extends NamedRunnable {
  void executeOn(ExecutorService executorService) {
      assert (!Thread.holdsLock(client.dispatcher()));
      boolean success = false;
      try {
        //12、这是个线程，其继承Executor，我们关注NamedRunnable所重写的execute方法
        executorService.execute(this);
        success = true;
      } catch (RejectedExecutionException e) {
        //这里是处理异常信息了，不需要太关注
        InterruptedIOException ioException = new InterruptedIOException("executor rejected");
        ioException.initCause(e);
        transmitter.noMoreExchanges(ioException);
        responseCallback.onFailure(RealCall.this, ioException);
      } finally {
        if (!success) {
          client.dispatcher().finished(this); // This call is no longer running!
        }
      }
    }
    
  @Override protected void execute() {
      boolean signalledCallback = false;
      transmitter.timeoutEnter();
      try {
        //13、这里是拿到响应对象了，证明这里是做了网络连接并且发起了网络请求，这里是核心函数
        Response response = getResponseWithInterceptorChain();
        signalledCallback = true;
        responseCallback.onResponse(RealCall.this, response);
      } catch (IOException e) {
        if (signalledCallback) {
          // Do not signal the callback twice!
          Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
        } else {
          responseCallback.onFailure(RealCall.this, e);
        }
      } catch (Throwable t) {
        cancel();
        if (!signalledCallback) {
          IOException canceledException = new IOException("canceled due to " + t);
          canceledException.addSuppressed(t);
          responseCallback.onFailure(RealCall.this, canceledException);
        }
        throw t;
      } finally {
        client.dispatcher().finished(this);
      }
    }
}
```



## 二、发起网络连接，得到服务器响应

### AsyncCall的getResponseWithInterceptorChain方法

```java
Response getResponseWithInterceptorChain() throws IOException {
    // Build a full stack of interceptors.
    List<Interceptor> interceptors = new ArrayList<>();
    interceptors.addAll(client.interceptors());
    //这些拦截器相当于将一个网络请求分步骤来做，每个interceptor分工都不一样，下面一一分析
    interceptors.add(new RetryAndFollowUpInterceptor(client));
    interceptors.add(new BridgeInterceptor(client.cookieJar()));
    interceptors.add(new CacheInterceptor(client.internalCache()));
    interceptors.add(new ConnectInterceptor(client));
    if (!forWebSocket) {
      interceptors.addAll(client.networkInterceptors());
    }
    interceptors.add(new CallServerInterceptor(forWebSocket));

    //这是一个对拦截器的管理和执行，
    Interceptor.Chain chain = new RealInterceptorChain(interceptors, transmitter, null, 0,
        originalRequest, this, client.connectTimeoutMillis(),
        client.readTimeoutMillis(), client.writeTimeoutMillis());

    boolean calledNoMoreExchanges = false;
    try {
      //开始执行interceptors
      Response response = chain.proceed(originalRequest);
      if (transmitter.isCanceled()) {
        closeQuietly(response);
        throw new IOException("Canceled");
      }
      return response;
    } catch (IOException e) {
      calledNoMoreExchanges = true;
      throw transmitter.noMoreExchanges(e);
    } finally {
      if (!calledNoMoreExchanges) {
        transmitter.noMoreExchanges(null);
      }
    }
  }
```



### 1、RetryAndFollowUpInterceptor

```java
public final class RetryAndFollowUpInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    Transmitter transmitter = realChain.transmitter();

    int followUpCount = 0;
    Response priorResponse = null;
    //循环
    while (true) {
      transmitter.prepareToConnect(request);

      if (transmitter.isCanceled()) {
        throw new IOException("Canceled");
      }

      Response response;
      boolean success = false;
      try {
        //把一个request推到下一个intercepter中
        response = realChain.proceed(request, transmitter, null);
        success = true;
      } catch (RouteException e) {
        //如果某条线路连接失败就报RouteException，Route是路由的意思
        // The attempt to connect via a route failed. The request will not have been sent.
        if (!recover(e.getLastConnectException(), transmitter, false, request)) {
          throw e.getFirstConnectException();
        }
        continue;
      } catch (IOException e) {
        //连接超时会报IOException
        // An attempt to communicate with a server failed. The request may have been sent.
        boolean requestSendStarted = !(e instanceof ConnectionShutdownException);
        
        //能recover的话就执行continue重复一次请求，否则抛异常
        if (!recover(e, transmitter, requestSendStarted, request)) throw e;
        continue;
      } finally {
        // The network call threw an exception. Release any resources.
        if (!success) {
          transmitter.exchangeDoneDueToException();
        }
      }

      // Attach the prior response if it exists. Such responses never have a body.
      if (priorResponse != null) {
        response = response.newBuilder()
            .priorResponse(priorResponse.newBuilder()
                    .body(null)
                    .build())
            .build();
      }

        
      //下面就是处理重定向的工作了
      Exchange exchange = Internal.instance.exchange(response);
      Route route = exchange != null ? exchange.connection().route() : null;
      Request followUp = followUpRequest(response, route);

      if (followUp == null) {
        if (exchange != null && exchange.isDuplex()) {
          transmitter.timeoutEarlyExit();
        }
        return response;
      }

      RequestBody followUpBody = followUp.body();
      if (followUpBody != null && followUpBody.isOneShot()) {
        return response;
      }

      closeQuietly(response.body());
      if (transmitter.hasExchange()) {
        exchange.detachWithViolence();
      }

       //超过重定向的最大值就抛异常
      if (++followUpCount > MAX_FOLLOW_UPS) {
        throw new ProtocolException("Too many follow-up requests: " + followUpCount);
      }

      request = followUp;
      priorResponse = response;
    }
  }
}
```



### 2、BridgeInterceptor

他的主要工作是拼接请求头了，包括"Content-Type"、"Content-Length"、"Transfer-Encoding"、"Accept-Encoding"、"Cookie"

另外自动支持"gzip"，具有省流量、加快请求和响应的特点

```java
public final class BridgeInterceptor implements Interceptor {
  private final CookieJar cookieJar;

  public BridgeInterceptor(CookieJar cookieJar) {
    this.cookieJar = cookieJar;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    Request userRequest = chain.request();
    Request.Builder requestBuilder = userRequest.newBuilder();

    RequestBody body = userRequest.body();
    if (body != null) {
      MediaType contentType = body.contentType();
      if (contentType != null) {
        requestBuilder.header("Content-Type", contentType.toString());
      }

      long contentLength = body.contentLength();
      if (contentLength != -1) {
        requestBuilder.header("Content-Length", Long.toString(contentLength));
        requestBuilder.removeHeader("Transfer-Encoding");
      } else {
        requestBuilder.header("Transfer-Encoding", "chunked");
        requestBuilder.removeHeader("Content-Length");
      }
    }

    if (userRequest.header("Host") == null) {
      requestBuilder.header("Host", hostHeader(userRequest.url(), false));
    }

    if (userRequest.header("Connection") == null) {
      requestBuilder.header("Connection", "Keep-Alive");
    }

    // If we add an "Accept-Encoding: gzip" header field we're responsible for also decompressing
    // the transfer stream.
    boolean transparentGzip = false;
    if (userRequest.header("Accept-Encoding") == null && userRequest.header("Range") == null) {
      transparentGzip = true;
      requestBuilder.header("Accept-Encoding", "gzip");
    }

    List<Cookie> cookies = cookieJar.loadForRequest(userRequest.url());
    if (!cookies.isEmpty()) {
      requestBuilder.header("Cookie", cookieHeader(cookies));
    }

    if (userRequest.header("User-Agent") == null) {
      requestBuilder.header("User-Agent", Version.userAgent());
    }

    Response networkResponse = chain.proceed(requestBuilder.build());

    HttpHeaders.receiveHeaders(cookieJar, userRequest.url(), networkResponse.headers());

    Response.Builder responseBuilder = networkResponse.newBuilder()
        .request(userRequest);

    if (transparentGzip
        && "gzip".equalsIgnoreCase(networkResponse.header("Content-Encoding"))
        && HttpHeaders.hasBody(networkResponse)) {
      GzipSource responseBody = new GzipSource(networkResponse.body().source());
      Headers strippedHeaders = networkResponse.headers().newBuilder()
          .removeAll("Content-Encoding")
          .removeAll("Content-Length")
          .build();
      responseBuilder.headers(strippedHeaders);
      String contentType = networkResponse.header("Content-Type");
      responseBuilder.body(new RealResponseBody(contentType, -1L, Okio.buffer(responseBody)));
    }

    return responseBuilder.build();
  }
}
```



### 3、CacheInterceptor

只要工作是保存response和返回可用没过期的espouse



### 4、ConnectInterceptor

```java
public final class ConnectInterceptor implements Interceptor {
  public final OkHttpClient client;

  public ConnectInterceptor(OkHttpClient client) {
    this.client = client;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    Request request = realChain.request();
    Transmitter transmitter = realChain.transmitter();

    // We need the network to satisfy this request. Possibly for validating a conditional GET.
    boolean doExtensiveHealthChecks = !request.method().equals("GET");
    //这一步创建了Exchage，是重点
    Exchange exchange = transmitter.newExchange(chain, doExtensiveHealthChecks);

    return realChain.proceed(request, transmitter, exchange);
  }
}
```

```java
public final class Transmitter {
    ....
    ....
    Exchange newExchange(Interceptor.Chain chain, boolean doExtensiveHealthChecks) {
      synchronized (connectionPool) {
        if (noMoreExchanges) {
          throw new IllegalStateException("released");
        }
        if (exchange != null) {
          throw new IllegalStateException("cannot make a new request because the previous response "
              + "is still open: please call response.close()");
        }
      }
      //创建编解码器，http1.0与http2.0请求的编码器和解读响应的解码器都是不一样的
      ExchangeCodec codec = exchangeFinder.find(client, chain, doExtensiveHealthChecks);
      Exchange result = new Exchange(this, call, eventListener, exchangeFinder, codec);

      synchronized (connectionPool) {
        this.exchange = result;
        this.exchangeRequestDone = false;
        this.exchangeResponseDone = false;
        return result;
      }
    }
}
```



### 5、ExchangeFinder（做http连接的操作）

```java
final class ExchangeFinder {
    public ExchangeCodec find(
      OkHttpClient client, Interceptor.Chain chain, boolean doExtensiveHealthChecks) {
    int connectTimeout = chain.connectTimeoutMillis();
    int readTimeout = chain.readTimeoutMillis();
    int writeTimeout = chain.writeTimeoutMillis();
    int pingIntervalMillis = client.pingIntervalMillis();
    boolean connectionRetryEnabled = client.retryOnConnectionFailure();

    try {
      //查找可用的连接
      RealConnection resultConnection = findHealthyConnection(connectTimeout, readTimeout,
          writeTimeout, pingIntervalMillis, connectionRetryEnabled, doExtensiveHealthChecks);
      //这里返回的就是编解码器
      return resultConnection.newCodec(client, chain);
    } catch (RouteException e) {
      trackFailure();
      throw e;
    } catch (IOException e) {
      trackFailure();
      throw new RouteException(e);
    }
  }
    
  private RealConnection findHealthyConnection(int connectTimeout, int readTimeout,
      int writeTimeout, int pingIntervalMillis, boolean connectionRetryEnabled,
      boolean doExtensiveHealthChecks) throws IOException {
      
    //死循环取可用的连接
    while (true) {
      RealConnection candidate = findConnection(connectTimeout, readTimeout, writeTimeout,
          pingIntervalMillis, connectionRetryEnabled);

      // If this is a brand new connection, we can skip the extensive health checks.
      //从连接池中去可用的连接
      synchronized (connectionPool) {
        if (candidate.successCount == 0 && !candidate.isMultiplexed()) {
          return candidate;
        }
      }

      // Do a (potentially slow) check to confirm that the pooled connection is still good. If it
      // isn't, take it out of the pool and start again.
      //如果不健康的连接就继续findConnection
      if (!candidate.isHealthy(doExtensiveHealthChecks)) {
        candidate.noNewExchanges();
        continue;
      }

      return candidate;
    }
  }
  
  private RealConnection findConnection(int connectTimeout, int readTimeout, int writeTimeout,
      int pingIntervalMillis, boolean connectionRetryEnabled) throws IOException {
    boolean foundPooledConnection = false;
    RealConnection result = null;
    Route selectedRoute = null;
    RealConnection releasedConnection;
    Socket toClose;
    synchronized (connectionPool) {
      //如果我们手动取消了，就停止连接
      if (transmitter.isCanceled()) throw new IOException("Canceled");
      hasStreamFailure = false; // This is a fresh attempt.

      // Attempt to use an already-allocated connection. We need to be careful here because our
      // already-allocated connection may have been restricted from creating new exchanges.
      releasedConnection = transmitter.connection;
      toClose = transmitter.connection != null && transmitter.connection.noNewExchanges
          ? transmitter.releaseConnectionNoEvents()
          : null;

      if (transmitter.connection != null) {
        // We had an already-allocated connection and it's good.
        result = transmitter.connection;
        releasedConnection = null;
      }

      if (result == null) {
        // Attempt to get a connection from the pool.
        //从连接池里面取可用的连接，
        //注意：这里传递的List<Route>参数是null，因为Route包含proxy、ip、端口，所以这里是拿不到代理类型的连接的；  第二参数是RequestMulitplexed,boolean类型，传了false，代表不支持多路复用，多路复用是在http2.0才有的
        if (connectionPool.transmitterAcquirePooledConnection(address, transmitter, null, false)) {
          foundPooledConnection = true;
          result = transmitter.connection;
        } else if (nextRouteToTry != null) {
          selectedRoute = nextRouteToTry;
          nextRouteToTry = null;
        } else if (retryCurrentRoute()) {
          selectedRoute = transmitter.connection.route();
        }
      }
    }
    closeQuietly(toClose);

    if (releasedConnection != null) {
      eventListener.connectionReleased(call, releasedConnection);
    }
    if (foundPooledConnection) {
      eventListener.connectionAcquired(call, result);
    }
    if (result != null) {
      // If we found an already-allocated or pooled connection, we're done.
      return result;
    }

    // If we need a route selection, make one. This is a blocking operation.
    boolean newRouteSelection = false;
    if (selectedRoute == null && (routeSelection == null || !routeSelection.hasNext())) {
      newRouteSelection = true;
      routeSelection = routeSelector.next();
    }

    List<Route> routes = null;
    synchronized (connectionPool) {
      if (transmitter.isCanceled()) throw new IOException("Canceled");

      if (newRouteSelection) {
        // Now that we have a set of IP addresses, make another attempt at getting a connection from
        // the pool. This could match due to connection coalescing.
        routes = routeSelection.getAll();
        if (connectionPool.transmitterAcquirePooledConnection(
            address, transmitter, routes, false)) {
          foundPooledConnection = true;
          result = transmitter.connection;
        }
      }

      if (!foundPooledConnection) {
        if (selectedRoute == null) {
          selectedRoute = routeSelection.next();
        }

        // Create a connection and assign it to this allocation immediately. This makes it possible
        // for an asynchronous cancel() to interrupt the handshake we're about to do.
        result = new RealConnection(connectionPool, selectedRoute);
        connectingConnection = result;
      }
    }

    // If we found a pooled connection on the 2nd time around, we're done.
    if (foundPooledConnection) {
      eventListener.connectionAcquired(call, result);
      return result;
    }

    // Do TCP + TLS handshakes. This is a blocking operation.
    result.connect(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis,
        connectionRetryEnabled, call, eventListener);
    connectionPool.routeDatabase.connected(result.route());

    Socket socket = null;
    synchronized (connectionPool) {
      connectingConnection = null;
      // Last attempt at connection coalescing, which only occurs if we attempted multiple
      // concurrent connections to the same host.
      if (connectionPool.transmitterAcquirePooledConnection(address, transmitter, routes, true)) {
        // We lost the race! Close the connection we created and return the pooled connection.
        result.noNewExchanges = true;
        socket = result.socket();
        result = transmitter.connection;

        // It's possible for us to obtain a coalesced connection that is immediately unhealthy. In
        // that case we will retry the route we just successfully connected with.
        nextRouteToTry = selectedRoute;
      } else {
        connectionPool.put(result);
        transmitter.acquireConnectionNoEvents(result);
      }
    }
    closeQuietly(socket);

    eventListener.connectionAcquired(call, result);
    return result;
  }
}
```

## 附录：介绍OkHttpClient

```java
/**
 * Default call timeout (in milliseconds). By default there is no timeout for complete calls, but
 * there is for the connect, write, and read actions within a call.
 */
public int callTimeoutMillis() {
  return callTimeout;
}

/** Default connect timeout (in milliseconds). The default is 10 seconds. */
public int connectTimeoutMillis() {
  return connectTimeout;
}

/** Default read timeout (in milliseconds). The default is 10 seconds. */
public int readTimeoutMillis() {
  return readTimeout;
}

/** Default write timeout (in milliseconds). The default is 10 seconds. */
public int writeTimeoutMillis() {
  return writeTimeout;
}

/** Web socket and HTTP/2 ping interval (in milliseconds). By default pings are not sent. */
//网络连接的心跳机制，每过很短的时间客户端给服务器发一个小数据，确认与服务器连接是否正常，客户端发ping，服务器响应内容是pong，注意是Web socket and HTTP/2的，跟我们移动开发关系不大
public int pingIntervalMillis() {
  return pingInterval;
}

//网络代理，三种类型：DIRECT（直连）、HTTP、SOCKS，
public @Nullable Proxy proxy() {
  return proxy;
}

//判断Url是否需要代理还是直连
public ProxySelector proxySelector() {
  return proxySelector;
}

//cookie罐子，可以自行实现一个保存数据到本地或者内存
public CookieJar cookieJar() {
  return cookieJar;
}

//缓存请求数据
public @Nullable Cache cache() {
  return cache;
}

//
@Nullable InternalCache internalCache() {
  return cache != null ? cache.internalCache : internalCache;
}

//DNS 是域名系统 (Domain Name System) 的缩写
//把域名解析成ip地址，一个域名可以有多个ip地址的
//java原生就有api来找dns，源码：InetAddress.getAllByname(hostName),得到的是ip地址数组
public Dns dns() {
  return dns;
}

//创建网络连接的Socket，tcp连接，不安全
public SocketFactory socketFactory() {
  return socketFactory;
}

//创建网络连接的Socket，tcp+ssl加密连接，安全
//X509TrustManager是证书的验证器，https连接的时候服务器会发来证书，它就是对证书的验证，一是验证证书是否存在可信列表，而是验证证书的合法性，做秘钥的一些配对，X509是正式的格式，已经成为一个标准
//CertificateChainCleaner是X509TrustManager的操作员
public Builder sslSocketFactory(
        SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {
      if (sslSocketFactory == null) throw new NullPointerException("sslSocketFactory == null");
      if (trustManager == null) throw new NullPointerException("trustManager == null");
      this.sslSocketFactory = sslSocketFactory;
      this.certificateChainCleaner = CertificateChainCleaner.get(trustManager);
      return this;
}

//验证服务器返回的证书是不是我请求的目标主机返回的，比较证书的hostName与请求hostName
public HostnameVerifier hostnameVerifier() {
  return hostnameVerifier;
}

//在验证证书合法性之外，还可以对该证书进行hash值的对比验证++++++---++++
public CertificatePinner certificatePinner() {
  return certificatePinner;
}

//当服务器报401的时候，一般是token过期，我们可以用这个来刷新token
public Authenticator authenticator() {
  return authenticator;
}

//如果代理的token失效，就用这个来刷新token
public Authenticator proxyAuthenticator() {
  return proxyAuthenticator;
}
//连接池，内部由ThreadPoolExecutor管理网络连接，连接复用节省内存，每个tcp连接可以有多个请求，就是多路复用（multiplex）
public ConnectionPool connectionPool() {
  return connectionPool;
}

//协议切换的时候是否需要重定向？默认true，例如从https://xxx请求变成了http://xxx，由安全变成不安全
//有些流氓网站就是通过这种情况来窃取用户数据
public boolean followSslRedirects() {
  return followSslRedirects;
}
//是否需要重定向？默认true
public boolean followRedirects() {
  return followRedirects;
}
//tcp连接、请求失败是否重试？默认是true，但是服务器404不能重试
public boolean retryOnConnectionFailure() {
  return retryOnConnectionFailure;
}

public Dispatcher dispatcher() {
  return dispatcher;
}

//http协议的版本号，如：http/1.0、http/1.1、spdy/3.1、h2
public List<Protocol> protocols() {
  return protocols;
}

//连接标准、连接规范，连接服务器的时候，客户端给服务器发送的的TLS版本，比如TLS 1.0、1.1，还有支持什么加密套件，比如对称加密、非对称加密、Hash算法；安全性有三个等级：RESTRICTED_TLS > MODERN_TLS > COMPATIBLE_TLS，但是兼容性相反，因为安全性越高，对服务器的要求也越高，还有一种是CLEARTEXT，明文传输类型，http:请求
public List<ConnectionSpec> connectionSpecs() {
  return connectionSpecs;
}

/**
 * Returns an immutable list of interceptors that observe the full span of each call: from before
 * the connection is established (if any) until after the response source is selected (either the
 * origin server, cache, or both).
 */
public List<Interceptor> interceptors() {
  return interceptors;
}

/**
 * Returns an immutable list of interceptors that observe a single network request and response.
 * These interceptors must call {@link Interceptor.Chain#proceed} exactly once: it is an error for
 * a network interceptor to short-circuit or repeat a network request.
 */
public List<Interceptor> networkInterceptors() {
  return networkInterceptors;
}
//对连接事件的监听
public EventListener.Factory eventListenerFactory() {
  return eventListenerFactory;
}
```

