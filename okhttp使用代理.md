# okhttp使用代理

http和https都可以。

OkHttpClient.Builder builder = new OkHttpClient.Builder();

//代理服务器的IP和端口号

builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8080)));

//代理的鉴权账号密码

final String userName = "";

final String password = "";

builder.proxyAuthenticator(new Authenticator() {
@Override

public Request authenticate(Route route, Response response) throws IOException {
//设置代理服务器账号密码

String credential = Credentials.basic(userName, password);

return response.request().newBuilder()

.header("Proxy-Authorization", credential)

.build();

}

});

okHttpClient = builder

//设置读取超时时间

.readTimeout(REQUEST_TIMEOUT_MS, TimeUnit.SECONDS)

//设置写的超时时间

.writeTimeout(REQUEST_TIMEOUT_MS, TimeUnit.SECONDS)

.connectTimeout(REQUEST_TIMEOUT_MS, TimeUnit.SECONDS).build();

ijkplayer设置代理和账号密码

mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"http_proxy", "http://"+host + ":" + port);

mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"headers", "Proxy-Authorization:"+credential);
