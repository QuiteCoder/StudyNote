# 自定义View

在 Android 开发中，布局测量流程是 View 系统的重要组成部分，用于确定每个 View 及其子 View 的大小和位置。这个过程主要涉及 `measure`、`layout` 和 `draw` 三个阶段。以下是布局测量流程的详细说明：

## View的加载流程

### 1. **Measure 阶段**

- **目的**：确定 View 及其子 View 的尺寸。
- **关键方法**：`measure(int widthMeasureSpec, int heightMeasureSpec)`
- **流程**：
  1. **测量规格（MeasureSpec）**：`MeasureSpec` 是一个 32 位的 int 值，包含模式和尺寸信息。模式有三种：
     - `UNSPECIFIED`：父 View 未对子 View 施加任何限制。
     - `EXACTLY`：父 View 为子 View 指定了精确的尺寸。
     - `AT_MOST`：子 View 的尺寸不能超过父 View 指定的最大尺寸。
  2. **测量过程**：
     - 父 View 调用 `measure()` 方法，传入 `MeasureSpec` 参数。
     - View 根据 `MeasureSpec` 计算自身尺寸，并调用 `setMeasuredDimension(int width, int height)` 保存结果。
     - 如果 View 是 ViewGroup，它会递归调用子 View 的 `measure()` 方法，确保所有子 View 完成测量。
  3. **自定义 View**：在自定义 View 时，通常需要重写 `onMeasure(int widthMeasureSpec, int heightMeasureSpec)` 方法，处理测量逻辑。

### 2. **Layout 阶段**

- **目的**：确定 View 及其子 View 的位置。
- **关键方法**：`layout(int l, int t, int r, int b)`
- **流程**：
  1. **布局过程**：
     - 父 View 调用 `layout()` 方法，传入四个参数：左、上、右、下边界。
     - View 根据这些参数确定自身位置，并调用 `setFrame(int l, int t, int r, int b)` 保存位置信息。
     - 如果 View 是 ViewGroup，它会递归调用子 View 的 `layout()` 方法，确保所有子 View 完成布局。
  2. **自定义 View**：在自定义 View 时，通常需要重写 `onLayout(boolean changed, int l, int t, int r, int b)` 方法，处理布局逻辑。

### 3. **Draw 阶段**

- **目的**：将 View 绘制到屏幕上。
- **关键方法**：`draw(Canvas canvas)`
- **流程**：
  1. **绘制过程**：
     - View 调用 `draw()` 方法，传入 `Canvas` 对象。
     - `draw()` 方法依次调用以下方法：
       - `drawBackground(Canvas canvas)`：绘制背景。
       - `onDraw(Canvas canvas)`：绘制内容。
       - `dispatchDraw(Canvas canvas)`：绘制子 View（适用于 ViewGroup）。
       - `onDrawForeground(Canvas canvas)`：绘制前景（如滚动条）。
  2. **自定义 View**：在自定义 View 时，通常需要重写 `onDraw(Canvas canvas)` 方法，处理绘制逻辑。

### 4. **总结**

- **Measure 阶段**：确定 View 的尺寸。
- **Layout 阶段**：确定 View 的位置。
- **Draw 阶段**：将 View 绘制到屏幕上。

### 5. **注意事项**

- **性能优化**：频繁的测量和布局操作会影响性能，应尽量减少不必要的测量和布局。
- **自定义 View**：在自定义 View 时，需正确处理 `onMeasure()`、`onLayout()` 和 `onDraw()` 方法，确保 View 的尺寸、位置和绘制效果符合预期。



## 用法技巧

### 1.resolveSize

```kotlin
class CircleView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    val TAG:String = javaClass.simpleName
    val PADDING :Float = 100f
    val RADIUS :Float = 100f
    val paint : Paint = Paint().apply {
        isAntiAlias = true
        color = Color.BLUE
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = (PADDING + RADIUS).toInt() *2
        // measureSpecMode跟布局中View的宽高属性
        // android:layout_width="200dp" 或者match_parent 对应 MeasureSpec.EXACTLY
        // wrap_content 对应 MeasureSpec.AT_MOST
        // ScrollView 或 ListView 等可滚动容器中，子view的测量模式对应 MeasureSpec.UNSPECIFIED
        val measureSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val measureSpecWidthSize = MeasureSpec.getSize(widthMeasureSpec)
        // resolveSize获取的值跟 注释掉的那块代码计算出来的width结果是一样的
        val resolveSize = resolveSize(size, widthMeasureSpec);
//        val width = when (measureSpecMode) {
//            MeasureSpec.EXACTLY -> measureSpecWidthSize
//            MeasureSpec.AT_MOST -> if (size > measureSpecWidthSize) measureSpecWidthSize else size
//            else -> size
//        }
        setMeasuredDimension(resolveSize, size)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas!!.drawCircle(PADDING + RADIUS, PADDING + RADIUS, RADIUS, paint)
    }
}
```



### 2.Canvas范围裁切

clipRect：切成一个矩形

clipPath：根据Path来切想要的形状

clipOutRect：切除正方形的内容，留下正方形以外的内容，是clipRect的反向调用

clipOutPath：根据Path来切想要的形状，是clipPath的反向调用



### 3.Canvas几何变换

translate(x, y)：平移

roate(degree)：旋转

scale(x, y)：缩放

skew(x, y)：斜切

```java
 /**
 * @params sx 将画布在x方向上倾斜相应的角度，sx倾斜角度的tan值，其实就是将y逆时针旋转相应的角度
 * @params sy 将画布在y方向上倾斜相应的角度，sx倾斜角度的tan值，其实就是将x顺时针旋转相应的角度
 *
 * 水平错切
 */
canvas.skew(sx, sy);
canvas.drawRect(rect2, mPaint);
```



## XferMode

Xfermode 是 Android 中用于控制图形混合模式的核心类，主要用于处理两张图片或图形叠加时的像素混合效果。它通过 `PorterDuffXfermode` 实现，支持多种混合模式（如 `SRC_IN`、`DST_IN`、`MULTIPLY` 等），广泛应用于圆角图片、遮罩效果、橡皮擦、刮刮卡等场景。

------

### **1. Xfermode 的基本概念**

- **Xfermode 的作用**：控制源图像（SRC）和目标图像（DST）的像素混合方式，生成最终的图像效果16。
- **PorterDuff.Mode**：Xfermode 的核心是 `PorterDuff.Mode`，它定义了 16 种混合模式，每种模式对应不同的像素混合规则19。
- **SRC 和 DST**：
  - **SRC**：源图像，通常是后绘制的图形。
  - **DST**：目标图像，通常是先绘制的图形112。

------

### **2. 常用混合模式**

以下是几种常见的 `PorterDuff.Mode` 及其应用场景：

#### **2.1 SRC 类模式**

- **SRC**：只显示源图像，忽略目标图像16。
- **SRC_IN**：在源图像和目标图像相交的区域显示源图像，常用于圆角图片和遮罩效果17。
- **SRC_OUT**：在源图像和目标图像不相交的区域显示源图像，常用于橡皮擦效果68。
- **SRC_ATOP**：在相交区域显示源图像，在不相交区域显示目标图像，适合实现倒影效果68。

#### **2.2 DST 类模式**

- **DST_IN**：在相交区域显示目标图像，且受源图像透明度影响，适合心电图效果和不规则水波纹68。
- **DST_OUT**：在不相交区域显示目标图像，且受源图像透明度影响68。

#### **2.3 其他混合模式**

- **MULTIPLY**：正片叠底，将源图像和目标图像的颜色值相乘，适合实现阴影效果16。
- **LIGHTEN**：变亮，取源图像和目标图像中较亮的像素69。
- **CLEAR**：清除相交区域的像素612。

------

### **3. Xfermode 的使用方法**

#### **3.1 基本步骤**

1. **创建 Paint 对象**：

   ```java
   Paint paint = new Paint();
   ```

2. **设置 Xfermode**：

   ```java
   paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
   ```

3. **绘制图形**：

   - 先绘制目标图像（DST）。
   - 再绘制源图像（SRC），应用 Xfermode 效果17。

#### **3.2 使用 `saveLayer`**

为了确保 Xfermode 效果正确，通常需要使用 `saveLayer` 创建一个离屏缓冲区：

```java
int layerId = canvas.saveLayer(0, 0, width, height, null, Canvas.ALL_SAVE_FLAG);
// 绘制目标图像
canvas.drawBitmap(dstBitmap, 0, 0, paint);
// 设置 Xfermode
paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
// 绘制源图像
canvas.drawBitmap(srcBitmap, 0, 0, paint);
// 恢复画布
canvas.restoreToCount(layerId);
```

`saveLayer` 可以避免直接在当前画布上绘制时受到背景影响27。

------

### **4. 实际应用示例**

#### **4.1 圆角图片**

使用 `SRC_IN` 模式实现圆角图片：

```java
@Override
protected void onDraw(Canvas canvas) {
    // 创建圆角遮罩
    Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas maskCanvas = new Canvas(mask);
    maskCanvas.drawRoundRect(0, 0, width, height, radius, radius, paint);

    // 应用 Xfermode
    int layerId = canvas.saveLayer(0, 0, width, height, null, Canvas.ALL_SAVE_FLAG);
    canvas.drawBitmap(originalBitmap, 0, 0, paint);
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(mask, 0, 0, paint);
    canvas.restoreToCount(layerId);
}
```

效果：将图片裁剪为圆角78。

#### **4.2 橡皮擦效果**

使用 `SRC_OUT` 模式实现橡皮擦：

```java
@Override
protected void onDraw(Canvas canvas) {
    int layerId = canvas.saveLayer(0, 0, width, height, null, Canvas.ALL_SAVE_FLAG);
    // 绘制目标图像
    canvas.drawBitmap(dstBitmap, 0, 0, paint);
    // 设置 Xfermode
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
    // 绘制擦除路径
    canvas.drawPath(erasePath, paint);
    canvas.restoreToCount(layerId);
}
```

效果：通过手势擦除图片811。

#### **4.3 刮刮卡效果**

使用 `SRC_OUT` 模式实现刮刮卡：

```java
@Override
protected void onDraw(Canvas canvas) {
    int layerId = canvas.saveLayer(0, 0, width, height, null, Canvas.ALL_SAVE_FLAG);
    // 绘制底层文字
    canvas.drawBitmap(textBitmap, 0, 0, paint);
    // 设置 Xfermode
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
    // 绘制刮痕路径
    canvas.drawPath(scrapePath, paint);
    canvas.restoreToCount(layerId);
}
```

效果：通过手势刮开涂层显示文字811。

------

### **5. 注意事项**

1. **关闭硬件加速**：部分 Xfermode 模式（如 `CLEAR`、`DARKEN`）在硬件加速下可能无效，需通过 `setLayerType(View.LAYER_TYPE_SOFTWARE, null)` 关闭硬件加速49。
2. **性能优化**：`saveLayer` 会占用较多资源，尽量避免在频繁绘制的场景中使用912。
3. **透明度处理**：Xfermode 的效果受图像透明度影响，需确保图像的 Alpha 通道正确68。

------

### **6. 总结**

Xfermode 是 Android 中实现图形混合的强大工具，通过 `PorterDuff.Mode` 可以灵活控制图像的叠加效果。掌握其使用方法和常见模式，能够实现丰富的视觉效果，如圆角图片、橡皮擦、刮刮卡等。在实际开发中，需注意性能优化和硬件加速的影响。