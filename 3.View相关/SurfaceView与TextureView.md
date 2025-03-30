`SurfaceView` 和 `TextureView` 是 Android 中用于高效渲染复杂 UI 或视频内容的两个重要组件。它们都提供了独立的绘制表面（Surface），可以在非主线程中进行渲染，适合处理高性能的图形或视频渲染任务。以下是对 `SurfaceView` 和 `TextureView` 的源码分析，包括它们的实现原理、使用场景和区别。

------

## 1. **SurfaceView**

### 1.1 **核心特性**

- **独立的 Surface**：`SurfaceView` 拥有一个独立的 Surface，与DecorView窗口的 Surface 分离。
- **双缓冲机制**：支持双缓冲，减少画面撕裂。
- **适合高性能渲染**：常用于游戏、视频播放等场景。

### 1.2 **源码分析**

#### 1.2.1 **SurfaceView 的创建**

- `SurfaceView` 继承自 `View`，但它并不直接在 View 的绘制流程中渲染内容。
- 它通过 `SurfaceHolder` 管理一个独立的 Surface。

```java
public class SurfaceView extends View {
    private SurfaceHolder mSurfaceHolder;
    private final Surface mSurface = new Surface();

    public SurfaceView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mSurfaceHolder = new SurfaceHolder(this);
        setWillNotDraw(true); // 禁用 View 的默认绘制
    }
}
```

#### 1.2.2 **Surface 的生命周期**

- `SurfaceView` 的 Surface 生命周期由 `SurfaceHolder.Callback` 管理。
- 主要回调方法：
  - `surfaceCreated()`：Surface 创建时调用。
  - `surfaceChanged()`：Surface 大小或格式变化时调用。
  - `surfaceDestroyed()`：Surface 销毁时调用。

```java
public interface SurfaceHolder {
    void addCallback(Callback callback);

    interface Callback {
        void surfaceCreated(SurfaceHolder holder);
        void surfaceChanged(SurfaceHolder holder, int format, int width, int height);
        void surfaceDestroyed(SurfaceHolder holder);
    }
}
```

#### 1.2.3 **渲染流程**

- 通过 `SurfaceHolder.lockCanvas()` 获取 Canvas，在非 UI 线程中绘制内容。
- 绘制完成后，调用 `SurfaceHolder.unlockCanvasAndPost()` 提交内容。

```java
SurfaceHolder holder = surfaceView.getHolder();
Canvas canvas = holder.lockCanvas();
if (canvas != null) {
    // 在 Canvas 上绘制内容
    canvas.drawColor(Color.BLACK);
    holder.unlockCanvasAndPost(canvas);
}
```

#### 1.2.4 **双缓冲机制**

- `SurfaceView` 内部维护了一个双缓冲机制，确保渲染的流畅性。
- 通过 `Surface.lockCanvas()` 和 `Surface.unlockCanvasAndPost()` 实现。

------

### 1.3 **使用场景**

- **游戏开发**：适合需要高性能渲染的场景。
- **视频播放**：用于播放视频流。
- **相机预览**：用于显示相机预览画面。

------

## 2. **TextureView**

### 2.1 **核心特性**

- **基于 View 的 Surface**：`TextureView` 使用 View 的硬件加速层（Hardware Layer）进行渲染。
- **支持动画和变换**：可以应用 View 的动画和变换（例如平移、缩放、旋转）。
- **适合动态 UI**：适合需要与 View 系统交互的场景。

### 2.2 **源码分析**

#### 2.2.1 **TextureView 的创建**

- `TextureView` 继承自 `View`，并通过 `SurfaceTexture` 管理渲染表面。
- `SurfaceTexture` 是一个基于 OpenGL 的纹理，用于接收图像流。

```java
public class TextureView extends View {
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;

    public TextureView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mSurfaceTexture = new SurfaceTexture(0);
        mSurface = new Surface(mSurfaceTexture);
    }
}
```

#### 2.2.2 **SurfaceTexture 的生命周期**

- `TextureView` 的渲染表面由 `SurfaceTexture` 管理。
- 主要回调方法：
  - `onSurfaceTextureAvailable()`：SurfaceTexture 可用时调用。
  - `onSurfaceTextureSizeChanged()`：SurfaceTexture 大小变化时调用。
  - `onSurfaceTextureDestroyed()`：SurfaceTexture 销毁时调用。

```java
public class TextureView extends View implements SurfaceTextureListener {
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // SurfaceTexture 可用
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // SurfaceTexture 大小变化
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // SurfaceTexture 销毁
        return true;
    }
}
```

#### 2.2.3 **渲染流程**

- 通过 `SurfaceTexture` 获取图像流，并在 `TextureView` 上渲染。
- 可以使用 `Canvas` 或 OpenGL 进行绘制。

```java
SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
Surface surface = new Surface(surfaceTexture);

Canvas canvas = surface.lockCanvas(null);
if (canvas != null) {
    // 在 Canvas 上绘制内容
    canvas.drawColor(Color.BLACK);
    surface.unlockCanvasAndPost(canvas);
}
```

#### 2.2.4 **硬件加速**

- `TextureView` 依赖于 View 的硬件加速层，支持复杂的动画和变换。

------

### 2.3 **使用场景**

- **动态 UI**：适合需要与 View 系统交互的场景。
- **视频播放**：用于播放视频流，并支持动画和变换。
- **相机预览**：用于显示相机预览画面，并支持动态效果。

------

## 3. **SurfaceView 和 TextureView 的区别**

| 特性           | SurfaceView              | TextureView                 |
| :------------- | :----------------------- | :-------------------------- |
| **渲染表面**   | 独立的 Surface           | 基于 View 的硬件加速层      |
| **双缓冲机制** | 支持                     | 不支持                      |
| **动画和变换** | 不支持                   | 支持                        |
| **性能**       | 更高，适合高性能渲染     | 较低，适合动态 UI           |
| **使用场景**   | 游戏、视频播放、相机预览 | 动态 UI、视频播放、相机预览 |

------

## 4. **源码分析总结**

### 4.1 **SurfaceView**

- 使用独立的 Surface，适合高性能渲染。
- 通过 `SurfaceHolder` 管理 Surface 的生命周期。
- 支持双缓冲机制，减少画面撕裂。

### 4.2 **TextureView**

- 基于 View 的硬件加速层，支持动画和变换。
- 通过 `SurfaceTexture` 管理渲染表面。
- 适合需要与 View 系统交互的场景。

------

## 5. **如何选择 SurfaceView 和 TextureView**

- **选择 SurfaceView**：
  - 需要高性能渲染（例如游戏、视频播放）。
  - 不需要复杂的动画和变换。
- **选择 TextureView**：
  - 需要支持动画和变换（例如动态 UI）。
  - 需要与 View 系统交互。





## 6、Canvas

在 Android 的 `View` 系统中，`onDraw(Canvas canvas)` 方法中的 `Canvas` 对象是由系统框架层创建并传递的，其来源与 Android 的渲染机制密切相关。具体来说：

------

### **Canvas 的来源**

1. **软件绘制（Software Rendering）**
   在未启用硬件加速的传统模式下：

   - **Canvas 与 Surface**：每个窗口（如 `Activity` 的根视图）对应一个 `Surface`，`Surface` 是底层图形缓冲区（`BufferQueue`）的封装。
   - **绘制流程**：
     1. 当视图需要绘制时（例如调用 `invalidate()`），`ViewRootImpl` 会通过 `Surface.lockCanvas()` 从 `SurfaceFlinger` 申请一个图形缓冲区，并绑定到一个 `Canvas` 对象。
     2. 该 `Canvas` 直接操作缓冲区的像素数据（通过关联的 `Bitmap`）。
     3. 所有视图的 `onDraw()` 方法依次在此 `Canvas` 上绘制，最终通过 `Surface.unlockCanvasAndPost()` 将缓冲区提交给 `SurfaceFlinger` 合成并显示。

   java

   复制

   ```
   // 伪代码：ViewRootImpl 中软件绘制的逻辑
   Surface surface = getSurface();
   Canvas canvas = surface.lockCanvas(dirtyRect);
   try {
       view.draw(canvas); // 递归调用 View 树的 draw() 方法
   } finally {
       surface.unlockCanvasAndPost(canvas);
   }
   ```

2. **硬件加速绘制（Hardware Acceleration）**
   当启用硬件加速（API 14+ 默认开启）：

   - **Canvas 的抽象化**：`Canvas` 不再直接操作像素，而是记录绘制命令到显示列表（`DisplayList`）。
   - **绘制流程**：
     1. 系统为视图树生成一个 `DisplayListCanvas`，用于记录绘制操作（如 `drawRect()`, `drawText()`）。
     2. `onDraw()` 方法中的 `Canvas` 实际上是 `DisplayListCanvas`，其操作被转换为 GPU 指令。
     3. 所有显示列表由 `RenderThread` 线程异步提交给 GPU 渲染，最终通过 `SurfaceFlinger` 合成。

   java

   复制

   ```
   // 伪代码：硬件加速下的 Canvas 记录
   DisplayListCanvas canvas = View.startRecording();
   view.onDraw(canvas); // 记录绘制命令到显示列表
   View.finishRecording();
   RenderThread.queueDisplayList(displayList); // 提交给 GPU
   ```

------

### **关键点总结**

| **场景**     | **Canvas 类型**     | **底层机制**                        | **性能特点**            |
| :----------- | :------------------ | :---------------------------------- | :---------------------- |
| **软件绘制** | `SoftwareCanvas`    | 直接操作像素缓冲区（Bitmap）        | 主线程阻塞，适合简单 UI |
| **硬件加速** | `DisplayListCanvas` | 记录 GPU 指令（OpenGL ES / Vulkan） | 异步渲染，支持复杂动画  |

------

### **常见疑问解答**

1. **为什么 `Canvas` 是只读的？**

   - 在硬件加速下，`Canvas` 是显示列表的记录器，不支持直接修改像素（如 `getPixel()`）。
   - 软件绘制中，虽然可以操作像素，但直接修改缓冲区可能破坏渲染流程。

2. **自定义 `Canvas` 是否可能？**

   - 可以创建离屏 `Canvas`（如通过 `Bitmap.createBitmap()`）：

     ```java
     Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
     Canvas customCanvas = new Canvas(bitmap);
     ```

   - 但视图系统的 `onDraw()` 中的 `Canvas` 由系统管理，不可替换。

3. **Canvas 的生命周期**

   - 每次重绘（`invalidate()`）时，系统会重新生成或复用 `Canvas` 对象，开发者无需手动管理。

------

### **使用场景建议**

- **软件绘制**：兼容旧设备或需要直接操作像素（如自定义绘图工具）。
- **硬件加速**：现代应用默认选择，支持复杂动画和高效渲染。