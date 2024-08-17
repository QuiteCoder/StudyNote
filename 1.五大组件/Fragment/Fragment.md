# 一、Fragment为什么被称为第五大组件

我们都知道android有主要的四大组件：`activity`,`service`,`broadcastReceiver`,`Contentprovider`.所以引发我们的思考fragment为什么称为android第五大组件呢？
1.随着android越来越成熟，各方面的性能优化，如内存优化，界面性能优化方面，使用`fragment`所占用的内存少，界面比较顺滑。
2.在使用频率上，`fragment`是不输与其他四大组件的，它具有自己的生命周期，同时可以灵活的动态，静态的加载activity的内容，所以成为第五大组件。





# 二、Fragment加载到Activity中的两种方式

## 1.动态加载（最常用）

```java
public class MainActivity extends AppCompatActivity {

    private Button btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.btn);
        //获取fragmentManager管理者 如果继承为FragmentActivity则:
    FragmentManager fragmentManager = getSupportFragmentManager();
    final FragmentManager fragmentManager = getFragmentManager();
        //开启FragmentTransaction事务
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        //通过事务向Activity的布局中添加MyFragment
        fragmentTransaction.add(R.id.fragment_content, new Fragment1());
        fragmentTransaction.commit();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //开启事务
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_content,new Fragment2());
                fragmentTransaction.commit();
            }
        });
    }
}
```
注:
1.如果mainActivity继承Activity则Fragment为 android.app.Fragment，如果继承FragmentActivity则是android.support.v4.app.Fragment；
2.fragment01和fragment02需动态的继承import android.app.Fragment或import android.support.v4.app.Fragment;
3.fragmentTransaction需要再次进行add,replace,remove操作的时候，需重新beginTransaction()，然后进行commit提交操作。如果直接进行commit会报commit already called异常.



## 2.静态加载

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    tools:context="com.trip.fragmenttest.MainActivity">

    <fragment
        android:id="@+id/lblFragment1"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:name="com.trip.fragmenttest.Fragment1"/>
</LinearLayout>
```



# 三、FragmentPagerAdapter和FragmentStatePagerAdapter的区别

```java
public abstract class FragmentPagerAdapter extends PagerAdapter {
    ...
    ...
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        if (DEBUG) Log.v(TAG, "Detaching item #" + getItemId(position) + ": f=" + object
                + " v=" + ((Fragment)object).getView());
        // 只是解除了ViewPager与Fragment的关联，Fragment并没有销毁，内存没有变化
        mCurTransaction.detach((Fragment)object);
    } 
        
}
```

```java
public abstract class FragmentStatePagerAdapter extends PagerAdapter {
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        if (DEBUG) Log.v(TAG, "Removing item #" + position + ": f=" + object
                         + " v=" + ((Fragment)object).getView());
        while (mSavedState.size() <= position) {
            mSavedState.add(null);
        }
        mSavedState.set(position, fragment.isAdded()
                        ? mFragmentManager.saveFragmentInstanceState(fragment) : null);
        mFragments.set(position, null);
		// 让Fragment从内存中移除，真正释放内存
        mCurTransaction.remove(fragment);
    }
}
```

总结：

1.FragmentPagerAdapter让不再需要的Fragment脱离，而不是remove，Fragment的对象没有销毁；

​	FragmentStatePagerAdapter让不在需要的Fragment销毁，直接从内存中释放；

2.FragmentPagerAdapter适合页面少的场景，而FragmentStatePagerAdapter则适合页面多的场景，后者更省内存。





# 四、生命周期

![Fragment生命周期](D:\学习资料\StudyNote\1.五大组件\Fragment\Fragment生命周期.jpg)

# 五、Fragment与Activity之间的通信

1.在Fragment中调用Activity中的方法：getActivity

2.在Activity中调用Fragment中的方法接口回调

3.在Fragment中调用Fragment中的方法findFragmentById

例如：

```java
public class MyFragment extends Fragment {
    public void doSomething() {
        // 这是Fragment要执行的操作
    }
}
```

```java
public class MyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
 
        // 假设你的Fragment在布局文件中有一个id为R.id.my_fragment的容器
        FragmentManager fragmentManager = getSupportFragmentManager();
        MyFragment myFragment = (MyFragment) fragmentManager.findFragmentById(R.id.my_fragment);
 
        if (myFragment != null) {
            // 调用Fragment的方法
            myFragment.doSomething();
        }
    }
}
```





