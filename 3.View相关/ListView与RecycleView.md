# ListView

## 基本用法

```java
// 在 Activity 或 Fragment 中
ListView listView = findViewById(R.id.listView);
CustomAdapter adapter = new CustomAdapter(this, dataList);
listView.setAdapter(adapter);
```



```java
class CustomAdapter extends BaseAdapter {
    private Context context;
    private List<String> dataList;

    public CustomAdapter(Context context, List<String> dataList) {
        this.context = context;
        this.dataList = dataList;
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 使用 ViewHolder保存convertView中的view对象，减少findviewById优化性能
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
            holder = new ViewHolder();
            holder.textView = convertView.findViewById(R.id.textView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // 绑定数据
        holder.textView.setText(dataList.get(position));
        return convertView;
    }

    static class ViewHolder {
        TextView textView;
    }
}
```



# RecycleView

## 基本用法

```java
public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
    private Context context;
    private List<String> dataList;

    // 构造函数
    public CustomAdapter(Context context, List<String> dataList) {
        this.context = context;
        this.dataList = dataList;
    }

    // 根据RecycleView的缓存情况，没有缓存时就创建ViewHolder
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    // 有缓存时，取出ViewHoler绑定数据，滑动列表时，更新列表中即将出现在屏幕上的Item
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = dataList.get(position);
        holder.textView.setText(item);

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(context, "You clicked: " + item, Toast.LENGTH_SHORT).show();
        });
    }

    // 返回数据项数量
    @Override
    public int getItemCount() {
        return dataList.size();
    }

    // ViewHolder 类
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
        }
    }
}
```



# 两者使用场景对比

`ListView` 和 `RecyclerView` 是 Android 中用于显示列表数据的两种常用组件，但它们在设计、功能和性能上有显著区别。以下是它们的详细对比：

------

### 1. **设计架构**

- **ListView**：
  - 是 Android 早期提供的列表组件，属于 `AbsListView` 的子类。
  - 功能相对简单，直接使用 `Adapter` 绑定数据。
  - 没有强制要求使用 ViewHolder 模式（但推荐使用以提升性能）。
- **RecyclerView**：
  - 是 Android 5.0（API 21）引入的更先进的列表组件，属于 `androidx.recyclerview` 库。
  - 基于 `ViewHolder` 模式，强制使用 `RecyclerView.ViewHolder` 来管理 item 视图。
  - 采用更灵活的架构，支持多种布局管理器（`LayoutManager`）、动画和装饰器。

------

### 2. **性能**

- **ListView**：
  - 性能较差，尤其是在列表项复杂或数据量大的情况下。
  - 默认不支持局部刷新，调用 `notifyDataSetChanged()` 会刷新整个列表。
  - 需要手动优化（如使用 ViewHolder 减少 `findViewById` 的调用）。
- **RecyclerView**：
  - 性能更优，默认支持局部刷新（如 `notifyItemChanged()`、`notifyItemInserted()` 等）。
  - 通过 `ViewHolder` 模式减少视图查找和绑定开销。
  - 支持更高效的回收机制，适合处理大量数据。

------

### 3. **功能扩展性**

- **ListView**：
  - 功能较为基础，扩展性有限。
  - 不支持复杂的布局（如网格、瀑布流等）。
  - 需要手动实现分隔线、点击事件等功能。
- **RecyclerView**：
  - 功能强大，扩展性极强。
  - 支持多种布局（线性、网格、瀑布流等），通过 `LayoutManager` 实现。
  - 内置 `ItemDecoration` 支持分隔线、`ItemAnimator` 支持动画效果。
  - 使用ItemTouchHelper可支持拖拽排序、滑动删除等高级功能。

------

### 4. **使用复杂度**

- **ListView**：
  - 使用简单，适合快速实现简单的列表需求。
  - 代码量较少，但功能有限。
- **RecyclerView**：
  - 使用相对复杂，需要配置 `LayoutManager`、`Adapter` 和 `ViewHolder`。
  - 代码量较多，但功能更强大且灵活。

------

### 5. **动画支持**

- **ListView**：
  - 不支持内置的 item 动画。
  - 需要手动实现动画效果。
- **RecyclerView**：
  - 内置 `ItemAnimator`，支持默认的添加、删除、移动动画。
  - 可以自定义动画效果。

------

### 6. **分隔线支持**

- **ListView**：
  - 通过 `android:divider` 属性或代码设置分隔线。
  - 功能较为简单。
- **RecyclerView**：
  - 通过 `ItemDecoration` 实现分隔线，支持高度自定义。
  - 可以灵活控制分隔线的样式和位置。

------

### 7. **点击事件**

- **ListView**：
  - 通过 `setOnItemClickListener` 设置 item 点击事件。
  - 功能较为简单。
- **RecyclerView**：
  - 需要手动在 `ViewHolder` 中实现点击事件。
  - 支持更灵活的点击事件处理（如长按、双击等）。

------

### 8. **数据绑定**

- **ListView**：
  - 使用 `Adapter` 的 `getView()` 方法绑定数据。
  - 需要手动优化性能（如复用 `convertView` 和使用 `ViewHolder`）。
- **RecyclerView**：
  - 使用 `RecyclerView.Adapter` 的 `onBindViewHolder()` 方法绑定数据。
  - 强制使用 `ViewHolder` 模式，性能更优。

------

### 9. **兼容性**

- **ListView**：
  - 是 Android 原生组件，兼容所有 Android 版本。
- **RecyclerView**：
  - 需要引入 `androidx.recyclerview` 库，但兼容性良好（支持到 API 7）。

------

### 总结对比表：

| 特性           | ListView          | RecyclerView             |
| :------------- | :---------------- | :----------------------- |
| **性能**       | 较差              | 更优                     |
| **扩展性**     | 有限              | 强大                     |
| **布局支持**   | 仅支持线性布局    | 支持线性、网格、瀑布流等 |
| **动画支持**   | 不支持            | 内置支持                 |
| **分隔线支持** | 简单              | 高度自定义               |
| **点击事件**   | 简单              | 灵活                     |
| **使用复杂度** | 简单              | 较复杂                   |
| **兼容性**     | 所有 Android 版本 | 需引入库，支持到 API 7   |

------

### 使用场景：

- **ListView**：
  - 适合简单的列表需求，数据量较小且不需要复杂功能。
  - 适合对性能要求不高的场景。
- **RecyclerView**：
  - 适合复杂的列表需求，数据量大且需要高性能。
  - 适合需要自定义布局、动画、分隔线等高级功能的场景。

------

### 结论：

- 如果是新项目，推荐使用 `RecyclerView`，因为它功能更强大、性能更优。
- 如果是维护旧项目且需求简单，可以继续使用 `ListView`。

# 两者的缓存对比

### 1. **ListView 的缓存机制**

`ListView` 的缓存机制基于 `RecycleBin`，它是 `AbsListView` 的内部类，用于管理 item 视图的回收和复用。

#### 缓存层级：

- **Active Views（活动视图）**：
  - 当前屏幕上可见的 item 视图。
  - 这些视图不会被回收，直接用于显示。
- **Scrap Views（废弃视图）**：
  - 滑出屏幕的 item 视图会被放入 `ScrapViews` 缓存中。
  - 当新 item 进入屏幕时，优先从 `ScrapViews` 中复用视图。

#### 工作原理：

1. 当列表滚动时，滑出屏幕的 item 视图会被放入 `ScrapViews` 缓存。
2. 当新 item 进入屏幕时，`ListView` 会优先从 `ScrapViews` 中获取视图并复用。
3. 如果 `ScrapViews` 中没有可复用的视图，则会调用 `Adapter` 的 `getView()` 方法创建新的视图。

#### 优点：

- 实现简单，适合处理简单的列表需求。
- 通过 `ViewHolder` 模式可以进一步提升性能。

#### 缺点：

- 缓存层级较少，复用机制相对简单。
- 对于复杂列表或大数据量场景，性能可能不足。

------

### 2. **RecyclerView 的缓存机制**

`RecyclerView` 的缓存机制更加复杂和高效，基于 `Recycler` 类实现，支持多级缓存和局部刷新。

#### 缓存层级：

- **Scrap（废弃缓存）**：
  - 类似于 `ListView` 的 `ScrapViews`，用于临时存储滑出屏幕的 item 视图。
  - 这些视图可能会被重新绑定到新数据。
- **Cache（缓存）**：
  - 用于存储刚刚滑出屏幕的 item 视图。
  - 这些视图不会被重新绑定，直接复用。
- **ViewCacheExtension（自定义缓存）**：
  - 允许开发者自定义缓存逻辑（一般不常用）。
- **RecycledViewPool（回收池）**：
  - 用于存储完全被回收的 item 视图。
  - 多个 `RecyclerView` 可以共享同一个 `RecycledViewPool`，提升复用效率。

#### 工作原理：

1. 当 item 滑出屏幕时，`RecyclerView` 会将其放入 `Scrap` 或 `Cache` 中。
2. 当新 item 进入屏幕时，优先从 `Cache` 中获取视图并复用。
3. 如果 `Cache` 中没有可复用的视图，则从 `RecycledViewPool` 中获取。
4. 如果 `RecycledViewPool` 中也没有可复用的视图，则调用 `Adapter` 的 `onCreateViewHolder()` 方法创建新的视图。

#### 优点：

- 多级缓存机制，复用效率更高。
- 支持局部刷新，减少不必要的视图绑定。
- 适合处理复杂列表和大数据量场景。

#### 缺点：

- 实现相对复杂，需要配置 `LayoutManager`、`Adapter` 和 `ViewHolder`。

------

### 3. **缓存策略对比**

| 特性           | ListView                              | RecyclerView                                  |
| :------------- | :------------------------------------ | :-------------------------------------------- |
| **缓存层级**   | 两级缓存（Active Views、Scrap Views） | 多级缓存（Scrap、Cache、RecycledViewPool 等） |
| **复用机制**   | 简单                                  | 高效                                          |
| **局部刷新**   | 不支持                                | 支持                                          |
| **共享缓存**   | 不支持                                | 支持（通过 RecycledViewPool）                 |
| **自定义缓存** | 不支持                                | 支持（通过 ViewCacheExtension）               |

------

### 4. **性能优化建议**

#### 对于 ListView：

- 使用 `ViewHolder` 模式减少 `findViewById` 的调用。
- 避免在 `getView()` 中执行耗时操作。
- 尽量复用 `convertView`。

#### 对于 RecyclerView：

- 使用 `ViewHolder` 模式（强制使用）。
- 尽量使用局部刷新方法（如 `notifyItemChanged()`）。
- 共享 `RecycledViewPool` 以提升复用效率。
- 根据需求自定义 `ItemDecoration` 和 `ItemAnimator`。

------

### 总结

- `ListView` 的缓存机制简单，适合处理简单的列表需求。
- `RecyclerView` 的缓存机制更高效，适合处理复杂列表和大数据量场景。
- 在新项目中，推荐使用 `RecyclerView`，因为它功能更强大且性能更优。