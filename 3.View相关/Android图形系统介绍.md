# Android图形系统介绍

## 一、背景

之前一直好奇一件事当我们把View控件渲染之后Android系统底层是如何把view显示到屏幕上面的。当同时打开多个app时，我们可以看到多个app的内容。Android底层系统到底是如何把多个组件、多个窗口、多个app的界面同时渲染到屏幕上的呢？之前一直在看这方面的知识，发现网上很多都是介绍Android图形系统源码分析的，关于图形系统整个数据流程的理论分析过程很少。最近又搜刮了大量播客，也找到了好多优质的博文，自己也总结下Android图形系统数据流过程的分析。最后也介绍下在应用层多媒体开发中常用的SurfaceView、GLSurfaceView、TextureView、SurfaceTexture、EGLSurface的使用场景和区别。

## 二、Android图形系统数据流分析

先上图，通过图先大致了解下Android图形系统数据流程：

![](E:\StudyNote\3.View相关\图形系统.jpg)

Android图形系统遵循着生产者消费者模型。生产者主要包括：View、OpenGL ES、MediaPlayer等。消费者主要是Surface。无论是View的绘制，还是OpenGL ES的绘制，或者播放器对视频数据的渲染，app进程最终数据流都会流向Surface。

### Surface

Surface 是一个窗口。一个App中常见的Surface窗口是：状态栏、导航栏、Activity的根View DecorView、PopupWindow等。状态栏、导航栏、Activity的根View DecorView、PopupWindow最终的绘制都会把数据通过ViewRootImpl渲染到各自的Surface上。Surface最终把数据放到BufferQueue。BufferQueue也是典型的生产者/消费者模型，负责连接Surface、Layer。最终我们app层渲染到Surface的数据到达了Layer层。

### BufferQueue

BufferQueue 类将可生成图形数据缓冲区的组件（生产方）连接到接受数据以便进行显示或进一步处理的组件（使用方）。几乎所有在系统中移动图形数据缓冲区的内容都依赖于 BufferQueue。图形生产者先向BufferQueue申请GraphicBuffer，填充完GraphicBuffer后，将GraphicBuffer移交给BufferQueue，BufferQueue会通知图形消费者（比如SurfaceFlinger、OMX等）有新的图形数据可以使用，图形消费者就可以从BufferQueue取出GraphicBuffer，使用完之后放回到BufferQueue以便循环使用。一般图形生产者和消费者是在不同进程中，BufferQueue采用了binder和共享内存机制，因此可以高效地在进程间传递图形数据。官方提供的BufferQueue模型图：

### Layer

Layer是SurfaceFlinger进行合成的基本操作单元。Layer在应用请求创建Surface的时候在SurfaceFlinger内部创建，因此一个Surface对应一个Layer。Layer其实是一个FrameBuffer,每个FrameBuffer中有两个GraphicBuffer记作FrontBuffer和BackBuffer。

### SurfaceFlinger

SurfaceFlinger负责先把所有注明GPU合成的Layer合成到一个输出Buffer，然后把这个输出Buffer和其他Layer（注明HWCOMPOSER合成的Layer）一起交给HWCOMPOSER，让HWCOMPOSER完成剩余Layer的合成和显示
官方提供的Android图像系统视频流管线：

## 三、SurfaceView、GLSurfaceView、TextureView、SurfaceTexture、EGLSurface的区别和使用场景

### SurfaceView:

SurfaceView 虽然本质上属于View，但它与View不同的是它拥有自己的Surface。我们上面介绍了Android图形系统的时候介绍过ViewRootImpl内部拥有一个Surface，而SurfaceView脱离DecorView拥有自己的Surface。这样的好处是对这个Surface的渲染可以放到单独线程去做，渲染时可以有自己的GL context。这对于一些游戏、视频等性能相关的应用非常有益，因为它不会影响主线程对事件的响应。但它也有缺点，因为这个Surface不在View hierachy中，它的显示也不受View的属性控制，所以不能进行平移，缩放等变换，也不能放在其它ViewGroup中，一些View中的特性也无法使用。

![](E:\StudyNote\3.View相关\图形层级.png)



### GLSurfaceView：

由于SurfaceView在游戏、视频等场景中使用的比较多，而这些游戏、视频场景中经常使用OpenGL对视频、游戏进行处理。从Android 1.5(API level 3)开始加入，作为SurfaceView的补充，Android官方提供了GLSurfaceView。GLSurfaceView在SurfaceView的基础上增加对OpenGL上下文及窗口的管理。我们可以直接在GLSurfaceView中操作OpenGL。当然GLSurfaceView也有他的不足，由于系统底层已经帮我们实现好了OpenGL上下文及窗口的管理，所以他的扩展性有些不足。所以我们常见的音视频场景是往往自己构建OpenGL上下文及窗口，SurfaceView+EGL+OpenGL。

### SurfaceTexture:

SurfaceTexture 是 Surface 和 OpenGL ES (GLES) 纹理的组合。SurfaceTexture 用于提供输出到 GLES 纹理的 Surface。SurfaceTexture 包含一个应用是其使用方的 BufferQueue。当生产方将新的缓冲区排入队列时，onFrameAvailable() 回调会通知应用。然后，应用调用 updateTexImage()，这会释放先前占有的缓冲区，从队列中获取新缓冲区并执行 EGL 调用，从而使 GLES 可将此缓冲区作为外部纹理使用。
SurfaceTexture经常使用的场景就是把视频流从Surface 转换为OpenGL 纹理，这样我们就可以通过OpenGL对视频流进行二次处理了。

### TextureView:

TextureView必须在硬件加速的场景中使用。TextureView底层实现就是View + SurfaceTexture。它和SurfaceView不同，它没有属于自己的Surface，而是作为View hierachy中的一个普通View，因此可以和其它普通View一样进行移动，旋转，缩放，动画等变化。TextureView继承自View，它与其它的View一样在View hierachy中管理与绘制。TextureView重载了draw()方法，其中主要把SurfaceTexture中收到的图像数据作为纹理更新到对应的HardwareLayer中。
无论是SurfaceView还是TextureView，它们的作用都是为了显示视频流。TextureView与SurfaceView虽然底层实现的原理上不同，但是TextureView也经常使用在视频场景中。TextureView最常用的用法是获取TextureView里的SurfaceTexture，通过把SurfaceTexture转换成Surface，再把Surface转换成EGLSurface。后面我们在通过OpenGL处理完视频流后就可以把视频流渲染到EGLSurface显示了。

### EGLSurface:

Android 使用 OpenGL ES (GLES) API 渲染图形。为了创建 GLES 上下文并为 GLES 渲染提供窗口系统，Android 使用 EGL 库。GLES 调用用于渲染纹理多边形，而 EGL 调用用于将渲染放到屏幕上。
在使用 GLES 进行绘制之前，您需要创建 GL 上下文。在 EGL 中，这意味着要创建一个 EGLContext 和一个 EGLSurface。 GLES 操作适用于当前上下文，该上下文通过线程局部存储访问，而不是作为参数进行传递。渲染代码应该在当前 GLES 线程（而不是界面线程）上执行。
EGLSurface 可以是由 EGL 分配的离屏缓冲区（称为“pbuffer”），也可以是由操作系统分配的窗口。调用 eglCreateWindowSurface() 函数可创建 EGL 窗口 Surface。 eglCreateWindowSurface() 将“窗口对象”作为参数，在 Android 上，该对象是 Surface。Surface 是 BufferQueue 的生产方。使用方（SurfaceView、SurfaceTexture、TextureView 或 ImageReader）创建 Surface。当您调用 eglCreateWindowSurface() 时，EGL 将创建一个新的 EGLSurface 对象，并将其连接到窗口对象的 BufferQueue 的生产方接口。此后，渲染到该 EGLSurface 会导致一个缓冲区离开队列、进行渲染，然后排队等待使用方使用。

一个 Surface 一次只能与一个 EGLSurface 关联（您只能将一个生产方连接到一个 BufferQueue），但是如果您销毁该 EGLSurface，它将与该 BufferQueue 断开连接，并允许其他内容连接到该 BufferQueue。
通过更改“当前”EGLSurface，指定线程可在多个 EGLSurface 之间进行切换。一个 EGLSurface 一次只能在一个线程上处于当前状态。

EGL 不是 Surface 的另一方面（如 SurfaceHolder）。EGLSurface 是一个相关但独立的概念。您可以在没有 Surface 作为支持的 EGLSurface 上绘制，也可以在没有 EGL 的情况下使用 Surface。EGLSurface 仅为 GLES 提供一个绘制的地方。





## 四、Window 与 SurfaceFlinger的关系

在 Android 系统中，**Window** 和 **SurfaceFlinger** 是图形渲染流程中两个关键角色，它们的关系涉及窗口管理、图形缓冲区的分配与合成。以下是两者的核心关系解析：

------

### **1. Window：应用窗口的抽象**

- **定义**：
  - Window 是 Android 中应用界面的逻辑容器，每个 Activity、Dialog 或悬浮窗都对应一个 Window。
  - Window 管理视图层级（如 `DecorView`），并通过 `Surface` 与底层图形系统交互。
- **核心作用**：
  - **Surface 的持有者**：每个 Window 关联一个 `Surface`，作为其绘制内容的载体。
  - **视图树的容器**：通过 `ViewRootImpl` 协调视图的测量、布局和绘制流程。

#### **Window 的创建流程**

1. **请求 Window**：当 Activity 启动时，向 `WindowManagerService (WMS)` 申请一个 Window。
2. **分配 Surface**：WMS 与 SurfaceFlinger 协作，为 Window 分配一个 `Surface`。
3. **绑定绘制环境**：应用通过 `Surface` 的 `Canvas` 或 OpenGL ES 进行内容绘制。

------

### **2. SurfaceFlinger：图形合成的核心**

- **定义**：
  - SurfaceFlinger 是 Android 系统的显示合成服务（System Server），负责将多个窗口的 `Surface` 合成为最终帧，输出到屏幕。
- **核心作用**：
  - **管理 BufferQueue**：每个 `Surface` 对应一个 `BufferQueue`，生产方（App）填充图形数据，消费方（SurfaceFlinger）合成。
  - **合成策略**：按窗口的 Z-order、透明度、位置等属性，通过 GPU 或硬件合成器（如 HWC）混合多个缓冲区。
  - **同步 VSync 信号**：在屏幕刷新周期（VSync）触发合成，避免画面撕裂。

#### **SurfaceFlinger 的工作流程**

1. **接收缓冲区**：应用将绘制完成的 `GraphicBuffer` 提交到 `Surface` 的 `BufferQueue`。
2. **合成准备**：SurfaceFlinger 遍历所有活跃的 `Layer`（对应 Window 的 Surface），收集待合成的缓冲区。
3. **执行合成**：通过 GPU 或 HWC 混合所有 Layer 的缓冲区，生成最终帧。
4. **提交到屏幕**：将合成后的帧写入显示设备的帧缓冲区（FrameBuffer）。

------

### **3. Window 与 SurfaceFlinger 的协作关系**

#### **(1) 窗口生命周期管理**

- **Window 创建**：
  - WMS 请求 SurfaceFlinger 为 Window 创建 `Surface`，并分配唯一的 `Layer`。
  - Surface 的 `BufferQueue` 由应用（生产者）和 SurfaceFlinger（消费者）共享。
- **Window 销毁**：
  - WMS 通知 SurfaceFlinger 释放对应的 `Layer` 和缓冲区资源。

#### **(2) 图形数据的流转**

- **应用侧**：

  ```java
  // 伪代码：应用绘制到 Surface
  Surface surface = window.getSurface();
  Canvas canvas = surface.lockCanvas();
  onDraw(canvas); // 绘制内容
  surface.unlockCanvasAndPost(canvas); // 提交到 BufferQueue
  ```

- **系统侧**：

  - SurfaceFlinger 监听 `BufferQueue`，当应用提交新缓冲区后，触发合成流程。

#### **(3) 合成与显示**

- **依赖 VSync**：SurfaceFlinger 在收到 VSync 信号后，开始合成当前所有准备好的缓冲区。
- **层级控制**：通过 WMS 维护的窗口 Z-order，SurfaceFlinger 确定 Layer 的叠加顺序。

------

### **4. 关键组件关系图**

```
+-------------------+       +---------------------+       +-------------------+
|      App          |       |  WindowManagerService|       |   SurfaceFlinger   |
|  (Activity/Window)|       |        (WMS)         |       |                    |
+-------------------+       +---------------------+       +-------------------+
         |                            |                              |
         | 1. 请求创建Window          |                              |
         |--------------------------->|                              |
         |                            | 2. 分配Surface (Layer)       |
         |                            |----------------------------->|
         |                            |                              |
         | 3. 获取Surface进行绘制      |                              |
         |<---------------------------|                              |
         |                            |                              |
         | 4. 提交Buffer到BufferQueue |                              |
         |---------------------------------------------------------->|
         |                            |                              |
         |                            | 5. VSync触发合成             |
         |                            |<------------------------------|
         |                            |                              |
         | 6. 显示合成后的帧            |                              |
         |<-----------------------------------------------------------|
```

------

### **5. 性能优化与常见问题**

#### **优化点**：

- **减少 Overdraw**：避免不必要的图层叠加，降低合成负载。
- **合理使用硬件加速**：利用 GPU 合成（如 HWC）降低 CPU 开销。
- **缓冲区管理**：复用 `GraphicBuffer`，避免频繁分配内存。

#### **常见问题**：

- **画面卡顿**：应用绘制超时（超过 16ms/60Hz），导致 SurfaceFlinger 丢帧。
- **层级错乱**：窗口 Z-order 设置错误，导致 Surface 被错误覆盖。
- **内存泄漏**：未释放销毁 Window 关联的 `Surface` 和 `BufferQueue`。

------

### **总结**

- **Window** 是应用界面的逻辑单元，通过 `Surface` 承载绘制内容。
- **SurfaceFlinger** 是系统的图形合成引擎，负责将多个 Window 的 `Surface` 合成为最终画面。
- **协作核心**：WMS 管理窗口元数据，SurfaceFlinger 管理图形数据，两者通过 `BufferQueue` 和 VSync 信号同步实现高效渲染。



## 五、ViewRootImpl中的Surface来自哪里？

```java
// ViewRootImpl.java
private int relayoutWindow(WindowManager.LayoutParams params, int viewVisibility,
            boolean insetsPending) throws RemoteException {
        ...
        int relayoutResult = mWindowSession.relayout(mWindow, mSeq, params,
                (int) (mView.getMeasuredWidth() * appScale + 0.5f),
                (int) (mView.getMeasuredHeight() * appScale + 0.5f), viewVisibility,
                insetsPending ? WindowManagerGlobal.RELAYOUT_INSETS_PENDING : 0, frameNumber,
                mTmpFrame, mPendingOverscanInsets, mPendingContentInsets, mPendingVisibleInsets,
                mPendingStableInsets, mPendingOutsets, mPendingBackDropFrame, mPendingDisplayCutout,
                mPendingMergedConfiguration, mSurfaceControl, mTempInsets);
        if (mSurfaceControl.isValid()) {
            // 从WMS获取Window中的Surface
            mSurface.copyFrom(mSurfaceControl);
        } else {
            // 销毁Surface
            destroySurface();
        }
}
```

**Surface的生命周期：**

- **创建**：当 `Window` 添加到 `WindowManager` 时，`ViewRootImpl` 通过 `relayoutWindow()` 请求 `WindowManagerService` 分配 `Surface`。
- **销毁**：`Window` 关闭时，`ViewRootImpl` 释放 `Surface` 并通知 `SurfaceFlinger` 回收资源。





## 六、TextureView会在Window上多创建出一个Surface吗？

在 Android 中，**`TextureView` 不会让 `Window` 额外创建出一个独立的 `Surface`**，而是通过 **共享 `Window` 的主 `Surface`** 并利用 `SurfaceTexture` 机制实现内容渲染。以下是关键点解析：

------

### 1. **`TextureView` 的核心机制**

- **依赖 `SurfaceTexture`**：
  `TextureView` 本身并不创建新的 `Surface`，而是通过内部的 [`SurfaceTexture`](https://developer.android.com/reference/android/graphics/SurfaceTexture)（一个基于 `BufferQueue` 的组件）接收图像流（如 OpenGL ES、视频或相机数据）。
- **共享 `Window` 的主 `Surface`**：
  所有内容最终会通过 `TextureView` 的 `onDraw()` 方法绘制到 `Window` 的主 `Surface` 上（即与普通 `View` 共享同一个 `Surface`）。

------

### 2. **与 `SurfaceView` 的对比**

| **特性**           | **`TextureView`**                                | **`SurfaceView`**                                 |
| :----------------- | :----------------------------------------------- | :------------------------------------------------ |
| **`Surface` 数量** | 不创建新 `Surface`，共享 `Window` 的主 `Surface` | 独立创建新的 `Surface`（可能引发 `Surface` 增加） |
| **合成方式**       | 通过 GPU 将内容合成到 `Window` 的 `Surface`      | 由系统单独合成到屏幕（通过 `SurfaceFlinger`）     |
| **层级限制**       | 受 `View` 层级约束（支持动画、变形等）           | 可脱离 `View` 层级（如 `setZOrderOnTop`）         |

------

### 3. **为什么 `TextureView` 不创建新 `Surface`？**

- **设计目标**：
  `TextureView` 的初衷是提供一种 **能嵌入 `View` 层级** 且支持硬件加速的动态内容渲染方案（如视频、动画），因此直接复用 `Window` 的主 `Surface` 更符合其定位。
- **性能权衡**：
  避免额外 `Surface` 带来的合成开销（减少 `SurfaceFlinger` 的工作量），但代价是内容更新需要通过 `View` 系统的重绘流程（可能引入 GPU 额外负载）。

------

### 4. **验证方法**

- **通过 `dumpsys SurfaceFlinger`**：
  检查当前 `Surface` 列表，会发现 `TextureView` 不会新增独立 `Surface`，而 `SurfaceView` 会显示为独立的 `Surface` 条目。
- **代码分析**：
  `TextureView` 的 `draw()` 方法最终通过 `Canvas` 将 `SurfaceTexture` 的内容绘制到 `Window` 的主 `Surface` 上（见 `TextureView#lockCanvas` 相关源码）。

------

### 5. **特殊情况与注意事项**

- **硬件加速必需**：
  `TextureView` 必须启用硬件加速（默认开启），否则无法使用 `SurfaceTexture`。
- **内存与性能**：
  虽然不增加 `Surface`，但 `SurfaceTexture` 的 `BufferQueue` 会占用显存，频繁更新可能引发 GPU 负载上升。