# Hook

背景：系统api或者sdk的api无法满足业务的需求，需要修改api的逻辑，但是又不可能修改源码，这时候Hook（钩子）技术就非常实用，最早期的Hook框架就是Xposed，但是需要root手机，将Xposed安装到手机才能使用，之后出现了Dexposed框架，不需要root手机，无侵入地实现运行时方法拦截。

博客：https://weishu.me/2017/11/23/dexposed-on-art/

目前流行的Android ART Hook框架有：Epic，[YAHFA](http://rk700.github.io/2017/03/30/YAHFA-introduction/)

Epic：https://github.com/tiann/epic  Epic is the continuation of [Dexposed](https://github.com/alibaba/dexposed) on ART (Supports 5.0 ~ 11).

YAHFA：https://github.com/PAGalaxyLab/YAHFA  Supports 5.0 ~ 12



## Hook原理

1、核心原理是利用java[反射](https://so.csdn.net/so/search?q=反射&spm=1001.2101.3001.7020)+代理，这种模式就是动态代理设计模式

2、在java层实现一个method，在jni层替换method结构指针
