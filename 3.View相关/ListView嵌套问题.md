# ScrollView布局中嵌套Listview

#### 问题描述：

- 一个滑动控件或布局里面，添加另外一个可以滑动的控件；
- 工作中遇到的，在ScrollView布局中嵌套Listview显示不正常，和在Listview中嵌套Listview的滑动冲突的问题。

#### 解决方法：

1.ScrollView布局中[嵌套](https://so.csdn.net/so/search?q=嵌套&spm=1001.2101.3001.7020)Listview显示不正常：

(1)自定义一个Listview，继承自Listview：

```java
public class ListViewForScrollView extends ListView {
    //三个构造方法
    public ListViewForScrollView(Context context) {
        super(context);
    }
    public ListViewForScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ListViewForScrollView(Context context, AttributeSet attrs,int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
   \* 需要重写onMeasure方法即可
   */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // MeasureSpec.UNSPECIFIED 这个就是说，当前组件，可以随便用空间，不受限制。
        // MeasureSpec.EXACTLY 精确模式，尺寸的值是多少，那么这个组件的长或宽就是多少。
        // MeasureSpec.AT_MOST 这个也就是父组件，能够给出的最大的空间，当前组件的长或宽最大只能为这么大，当然也可以比这个小
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
                                                     MeasureSpec.AT_MOST);
        // 测量时给ListView一个最大的高度
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
```

 这种方法是将ListView的允许高度设置为极大值,比较简单,但是有一个后果是会造成adapter的getView方法调用多次,也就是多次重绘计算高度.导致的结果就是加载卡顿,如果item中有EditText的话可能会造成数据显示混乱

(2)重新计算Listview的高度

- 问题描述： 
  - 在一个滑动布局中添加一个滑动控件，滑动控件的高度因为不能计算，所以“只能显示一个Item”
- 解决方法： 
  - 若要解决这个问题，我们可以“重新计算Listview的高度”，调用一个的静态方法即可。

```java
/**
 \* scrollview嵌套listview显示不全，采用计算所有ListViewd的Item方式解决
 */
public static void setListViewHeightBasedOnChildren(ListView listView) {
    //获取ListView适配器
    ListAdapter listAdapter = listView.getAdapter();
    if (listAdapter == null) {
        return;
    }
    int totalHeight = 0;
    //遍历获取所有适配器中的孩子（item）的高度
    for (int i = 0; i < listAdapter.getCount(); i++) {
        View listItem = listAdapter.getView(i, null, listView);
        //逐个绘制计算。
        listItem.measure(0, 0);
        //累加得到绘制高度
        totalHeight += listItem.getMeasuredHeight();
    }
    //设置参数
    ViewGroup.LayoutParams params = listView.getLayoutParams();
    //参数的高度赋值（加上分隔线的高度：总个数（比条目少1）*每条的高度）
    params.height = totalHeight
        \+ (listView.getDividerHeight() * (listAdapter.getCount() - 1));
    //listView设置参数即可。
    listView.setLayoutParams(params);
}
```

 

2.在Listview中嵌套Listview的滑动冲突
问题描述： 
在一个listview的footer（根部，即写item时底下是有ListView）中添加一个的Listview
由于触摸冲突问题，小的listview不能滑动
解决方法：

自定义一个控件继承自listview
重写onInterceptTouchEvent方法，对触摸事件进行分发。
说明：

onInterceptTouchEvent()是ViewGroup的一个方法，目的是在系统向该ViewGroup及其各个childView触发onTouchEvent()之前对相关事件进行一次拦截。
我们可在这个方法里对子控件或者是父控件获取触摸处理权限进行分发，从而完成各自的滑动任务。

```java
public class InnerListview extends ListView {
    //3个构造方法
    public InnerListview(Context context) {
        super(context);
    }
    public InnerListview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public InnerListview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
                // 当手指触摸listview时，让父控件交出ontouch权限,不能滚动
            case MotionEvent.ACTION_DOWN:
                setParentScrollAble(false);
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 当手指松开时，让父控件重新获取onTouch权限
                setParentScrollAble(true);
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }
    // 设置父控件是否可以获取到触摸处理权限
    private void setParentSlidAble(boolean flag) {
        //请求不要拦截     
        getParent().requestDisallowInterceptTouchEvent(!flag);
    }
}
```

效果：

外面的listview和里面的小listview就都可以实现各自的滑动了。