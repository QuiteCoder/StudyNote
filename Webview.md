# Webview

## 一、Webview常见的一些坑

1.Android API 16（Android 4.2）及以前的版本存在远程代码执行安全漏洞，该漏洞源于程序没有正确限制使用Webview.addJavascriptInterface方法，远程攻击者可以通过使用Java Reflection API利用该漏洞执行任何Java对象的方法。

解决办法：

在4.2之后，由于Google增加了@JavascriptInterface，该漏洞得以解决。

```java
@JavascriptInterface
public class MyJavaScriptInterface {
    public void callMe(String message) {
        // 在这里执行JavaScript可以调用的方法
    }
}
```

解决该问题，最彻底的方式是在4.2以下放弃使用addJavascriptInterface，采用onJsPrompt或其它方法替换。或者使用一些方案来降低该漏洞导致的风险：如使用https并进行证书校验，如果是http则进行页面完整性校验，如上面所述移除隐藏接口等。



2.webview在布局中使用：webview写在其他容器中时，想要销毁，必须执行destory方法



3.jsbridge，在web端和native端建立一座桥，native端可以调用web端的方法



4.webviewClient.onPageFinished方法的缺点：

**不准确：**有时候页面可能在onPageFinished之后才完全加载完毕，特别是当页面包含大量的JavaScript或者复杂的CSS时

**多次触发：**会在webview跳转各种url的时候或者每个资源加载完成之后都触发一次；

**不一致的行为**：不同版本的Android系统可能会有不同的行为，尤其是在页面加载优化上。

可以使用WebChromeClient.onProgressChanged来判定页面是否已经加载完成；



5.后台耗电

Webview会在后台建立多条线程，没有正常关闭时会在后台持续耗电；一定要在页面销毁的时候把Webview销毁



6.硬件加速导致页面渲染问题

使用硬件加速会让画面更流畅，但是可能会出现加载白块渲染的问题，

解决方法：暂时手动关闭硬件加速



## 二、关于webview的内存泄漏问题

1.使用System.Exit( )的方式强制释放进程；

2.动态添加WebView到页面View中，传入弱引用的Context。

