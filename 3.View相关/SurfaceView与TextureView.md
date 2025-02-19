`SurfaceView` 和 `TextureView` 是 Android 中用于高效渲染复杂 UI 或视频内容的两个重要组件。它们都提供了独立的绘制表面（Surface），可以在非主线程中进行渲染，适合处理高性能的图形或视频渲染任务。以下是对 `SurfaceView` 和 `TextureView` 的源码分析，包括它们的实现原理、使用场景和区别。

------

## 1. **SurfaceView**

### 1.1 **核心特性**

- **独立的 Surface**：`SurfaceView` 拥有一个独立的 Surface，与主窗口的 Surface 分离。
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

------

通过理解 `SurfaceView` 和 `TextureView` 的源码和特性，开发者可以根据具体需求选择合适的组件，以实现高效的图形或视频渲染。