自定义view需要重写onMeasure,onlayout,onDraw

onMeasure测量view的宽高

onLayout设置view的x,y坐标

onDraw屏幕刷新每帧都会调用，一般在这里绘制内容



性能优化要注意的是，尽量不要触发onLayout，因为onLayout会让整个页面触发重新



android 自定义view尽量少调用onLayout。

在Android开发中，自定义View时需要注意`onLayout`方法的调用频率，因为`onLayout`是用于确定子视图在父视图中的位置和大小的。频繁调用`onLayout`可能会导致性能问题，尤其是在复杂的布局或者动画中。以下是一些优化`onLayout`调用频率的策略：

### 1. 延迟执行`onLayout`

如果可能的话，尽量延迟`onLayout`的调用。例如，在`onMeasure`方法中，可以先计算出所有的测量值，然后在`onLayout`方法中根据这些测量值来设置视图的位置和大小。这样可以减少在布局过程中的计算次数。

### 2. 使用`post`或`View.post(Runnable)`

如果你的操作需要在视图绘制完成之后执行，可以考虑使用`post(Runnable)`方法。这可以在UI线程的下一个绘制周期执行你的代码，这样可以避免在当前的绘制周期中直接调用`onLayout`。

```java
view.post(new Runnable() {
    @Override
    public void run() {
        // 执行需要延迟的布局操作
    }
});
```

### 3. 避免在`onDraw`中调用`onLayout`

通常，你应该避免在`onDraw`方法中调用`onLayout`，因为这会导致不必要的布局计算和绘制。如果需要在绘制时调整布局，可以考虑使用其他方法，如通过属性动画改变视图的位置或大小。

### 4. 优化布局层级

减少视图层级可以减少布局计算的复杂度。确保你的布局尽可能扁平，避免不必要的嵌套视图。使用`merge`标签或者移除不必要的布局容器可以减少层级。

### 5. 使用硬件加速

确保你的应用开启了硬件加速。在AndroidManifest.xml中为Activity设置`android:hardwareAccelerated="true"`可以提升绘制的性能。

```xml
<activity android:name=".MyActivity" android:hardwareAccelerated="true" />
```

### 6. 合理使用`invalidate()`和`requestLayout()`

- **尽量避免直接调用`onLayout()`**：通常，你应该通过调用`requestLayout()`来请求父视图进行布局，而不是直接调用`onLayout()`。
- 使用`invalidate()`来请求重绘，而不是直接调用`onDraw()`。这会让系统知道视图需要重新绘制，但不会触发布局过程。

### 7. 考虑使用ConstraintLayout或其它高效布局库

使用如ConstraintLayout这样的现代布局库可以减少手动布局的复杂性，同时这些库内部优化了性能，减少了不必要的布局计算。

通过上述方法，你可以有效地减少对`onLayout`的调用频率，从而提升你的应用的性能和响应速度。