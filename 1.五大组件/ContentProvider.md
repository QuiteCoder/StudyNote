# ContentProvider

内容提供者：

Android 的数据存储方式总共有五种，分别是：Shared Preferences、网络存储、文件存储、外储存储、SQLite。但一般这些存储都只是在单独的一个应用程序之中达到一个数据的共享，有时候我们需要操作其他应用程序的一些数据，就会用到 ContentProvider。而且 Android 为常见的一些数据提供了默认的 ContentProvider（包括音频、视频、图片和通讯录等）。