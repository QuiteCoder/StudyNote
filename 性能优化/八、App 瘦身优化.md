# App 瘦身优化

#### Apk 组成

* 代码相关：classes.dex
* 资源相关：res、asserts、resources.arsc
* So 相关：lib

#### Apk 分析工具

* ApkTool 反编译工具
  * 官网：https://ibotpeaches.github.io/Apktool/
  * 使用：apktool d xx.apk
* Jadx 反编译工具
  * 地址：https://github.com/skylot/jadx
* Analyze APK Android studio 自带工具
  * 查看 Apk 组成、大小、占比
  * Apk 对比
* https://nimbledroid.com App 性能分析网站
  * 文件大小及排行
  * Dex 方法数、知名 SDK 方法数
  * 启动时间、内存等
* android-classyshark：二级制检查工具
  * 地址：https://github.com/google/android-classyshark
  * 支持多种格式：Apk、Jar、Class、So 等

#### 瘦身方案

* 代码混淆
* 三方库统一，减少相同功能的库
* 移除无用代码
  * 通过 AOP 的方式统计使用情况，可以切onCreate方法和构造方法来确定那个类没有被调用过了
* 移除冗余资源

  * 右键 -> Refactory -> Remove Unused Resource
* 图片压缩【可以有效的减少资源大小】
  * 压缩 png：https://tinify.cn/
  * 优先使用 webp
* 资源混淆
  * 地址：https://github.com/shwenzhang/AndResGuard
* 图片资源只保留一份
* 资源在线化，将一部分资源放在远端，结合预加载技术手段来减少apk自身的大小
*  So 移除，无需将所有平台的 So 都编译出来并使用
  * abiFilters：设置支持的 So 架构，一般选择 `armeabi` 【该架构为通用性架构，所有平台都可以使用，但是效率并不如专用架构高】
  * 【更优方案】对于性能敏感的模块，可以将对应架构的so库都放到 `armeabi` 目录，根据 CPU 类型加载对应架构的 So
* 其它：
  * So 动态下载：将部分可延迟加载的 So 库放到云端，在需要使用时进行动态下发
  * 插件化

