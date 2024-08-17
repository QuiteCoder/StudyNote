# Android Jetpack

Jetpack 、kotlin、flutter是未来安卓发展趋势



## 一、`LiveData` + `DataBinding`

1. MVC：分层不清晰，代码冗余，耦合度高；使用findViewbyId的方式把View保存在Activity中，onclick、setText、setBitmap都放到Activity中，导致Activity非常庞大，代码冗余，业务逻辑难以分离；
2. MVP：分层清晰，接口地狱；框架通过callback的方式把数据抽离至P层，P层通知M层请求数据，但是随着View数量越来越多，P层的callback接口也就越来越多；
3. MVVM：使用DataBinding代替P层解决接口地狱问题（早期版本 DataBinding，实时监听非常耗费性能）是在MVP的基础上做了小幅调整，Activity是V层，布局文件跟ViewModel形成VM层，M层依旧是数据本地化+网络请求。
4. MVVM+Jectpack标准化框架
   优点：
   - 避免界面销毁后内存泄漏。Activity实现了LifeCycleOwner接口，成为被观察者，观察者是MutableLiveData，会在处理业务的时候会先判断Activity的生命周期，非Destroyed状态才能继续执行。
   - 免除横竖屏切换数据恢复的逻辑。



### 用法：

步骤一：根据layout文件中的控件，新建MyViewModel类，继承ViewModel；

```java
public class HomeViewModel extends ViewModel {
    private MutableLiveData<String> mText;
    private MutableLiveData<Drawable> placeHolder;
    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("首页中心");
        
        placeHolder = new MutableLiveData<>();
        placeHolder.setValue(Utils.getApp().getResources().getDrawable(R.drawable.pictures_no));
    }

    public LiveData<String> getText() {
        return mText;
    }

    public MutableLiveData<Drawable> getPlaceHolder() {
        return placeHolder;
    }
}
```

步骤二：在layout文件的最外层加入<layout>标签和<data>属性标签，关联View控件与ViewMode；

步骤三：在Activity或Fragment中通过ViewModelProvider获取已经注册的ViewMode；

```java
HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
//绑定布局与ViewMode,XXX是布局data标签对应的name
binding.setXXX(homeViewModel);
```

步骤四：MutableLiveData注册观察者

```java
MutableLiveData.observe(getViewLifecycleOwner(), new Observer<HomeDataResult>() {
    @Override
    public void onChanged(HomeDataResult homeDataResult) {
    	//步骤五之后回调到这来
    }
}
```

步骤五：M层取数据之后用LiveData.postValue()主线程操作（内部使用Handler.post（Runnable）实现切线程） 或 setValue()子线程操作，将数据通知Observer



工程的部分源码：

```java
public class HomeFragment extends BaseFragment {

    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;
    private RequestHomeViewModel requestHomeViewModel;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //加载ViewMode@{
        //做交互业务用
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        //数据请求用，每个V可以有多个ViewMode
        requestHomeViewModel = new ViewModelProvider(this).get(RequestHomeViewModel.class);
        //@}
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        //databinding与布局关联
        binding = FragmentHomeBinding.bind(root);
		//绑定布局与ViewMode,布局中有声明皆可
        binding.setVm(homeViewModel);
        //绑定布局与类的调用,布局中有声明皆可
        binding.setHaha(new Haha());
        //databinding绑定生命周期
        binding.setLifecycleOwner(this);

        return root;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // homeDataResultMutableLiveData是被观察者，然后注册了观察者，回调是onChanged()
        requestHomeViewModel.getHomeDataResultMutableLiveData().observe(getViewLifecycleOwner(), new Observer<HomeDataResult>() {
            @Override
            public void onChanged(HomeDataResult homeDataResult) {
                String imagePath1 = homeDataResult.getData().getCompany_list().get(0).getImage();
                homeViewModel.getImageURL1().setValue(imagePath1);
            }
        });

        //触发网络请求，请求成功后让homeDataResultMutableLiveData修改数据，用postValue或者setValue
        requestHomeViewModel.touchOffHomeData();
    }
}

public class Haha {
    public void sayHaha(){
        System.out.println("haha");
    }
}

public class RequestHomeViewModel extends ViewModel {
    private MutableLiveData<HomeDataResult> homeDataResultMutableLiveData;
    public MutableLiveData<HomeDataResult> getHomeDataResultMutableLiveData() {
        if (homeDataResultMutableLiveData == null) {
            homeDataResultMutableLiveData = new MutableLiveData<>();
        }
        return homeDataResultMutableLiveData;
    }

    public void touchOffHomeData() {
        HttpRequestManager.getInstance().requestHomeData(getHomeDataResultMutableLiveData());
    }
}

/****************layout文件**************/
<?xml version="1.0" encoding="utf-8"?>
<layout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:derry="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <data>
        <variable
            name="vm"
            type="com.xiangxue.googleproject.change.HomeViewModel" />
        <variable
            name="haha"
            type="com.xiangxue.googleproject.ui.fragment.Haha" />
    </data>

    <ScrollView
        android:id="@+id/root_view"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/white"
        android:scrollbars="none"
        tools:context=".ui.fragment.HomeFragment"
        android:paddingBottom="80dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:focusable="true"
            android:focusableInTouchMode="true"
            android:orientation="vertical">

            <TextView
                android:id="@+id/text_home"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@{vm.text}"
                android:textSize="10sp"
                android:visibility="gone" />

            //自动关联到@BindingAdapter(value = {"imageUrl", "placeHolder"}
            //vm.imageURL1、vm.placeHolder会自动找到上面声明的HomeViewModel的成员变量
            <com.xiangxue.googleproject.ui.view.ProportionImageView
                android:id="@+id/iv_top"
                android:layout_width="match_parent"
                android:layout_height="0dp"
                derry:heightProportion="1"
                derry:widthProportion="2"
                imageUrl="@{vm.imageURL1}"
                placeHolder="@{vm.placeHolder}"
                android:onClick="@{()->haha.sayHaha()}"
                />            
        </LinearLayout>
    </ScrollView>

</layout>
                    
public class ImageAdapter {

    //编译器自动是被注解，找到并调用此类。
    //注解中"imageUrl"对应控件的imageUrl="@{...}"；"placeHolder"对应placeHolder="@{...}"
    @BindingAdapter(value = {"imageUrl", "placeHolder"}, requireAll = false)
    public static void loadUrl(ImageView view, String url, Drawable placeHolder) {
        if (url != null) {
            Glide.with(view.getContext()).load(url).placeholder(placeHolder).into(view);
        } else {
            Glide.with(view.getContext()).load(ServerConfigs.DEFAULT_IMG_URL2).placeholder(placeHolder).into(view);
        }
    }

}  
```



### LiveData数据粘性（横竖屏切换数据恢复的原因）

**前提是LiveData使用单例**

每个`LiveData`中保存了一个`Value`，有两个方式会通知`Observer`：

1、`performStart`被触发，横竖屏切换和`Activity`启动都会触发`performStart`

2、`postValue`、`setValue`

```java
//Activity触发performStart，最终调用Observer.onChanged
onChanged:26, LoginActivity$1 (com.xiangxue.googleproject.test)
considerNotify:131, LiveData (androidx.lifecycle)
dispatchingValue:144, LiveData (androidx.lifecycle)
activeStateChanged:443, LiveData$ObserverWrapper (androidx.lifecycle)
onStateChanged:395, LiveData$LifecycleBoundObserver (androidx.lifecycle)
dispatchEvent:361, LifecycleRegistry$ObserverWithState (androidx.lifecycle)
forwardPass:300, LifecycleRegistry (androidx.lifecycle)
sync:339, LifecycleRegistry (androidx.lifecycle)
moveToState:145, LifecycleRegistry (androidx.lifecycle)
handleLifecycleEvent:131, LifecycleRegistry (androidx.lifecycle)
dispatch:68, ReportFragment (androidx.lifecycle)
onActivityPostStarted:179, ReportFragment$LifecycleCallbacks (androidx.lifecycle)
dispatchActivityPostStarted:1298, Activity (android.app)
performStart:8149, Activity (android.app)
```





## 二、ViewModel横竖屏数据恢复原理

```java
//ViewModelProvider内实例化ViewModel，并用ViewModelStore保存起来
new ViewModelProvider(this).get(HomeViewModel.class);

//步骤一：实例化ViewModelProvider传入ViewModelStore
public ViewModelProvider(@NonNull ViewModelStoreOwner owner) {
    this(owner.getViewModelStore(), owner instanceof HasDefaultViewModelProviderFactory
            ? ((HasDefaultViewModelProviderFactory) owner).getDefaultViewModelProviderFactory()
            : NewInstanceFactory.getInstance());
}
public ViewModelProvider(@NonNull ViewModelStore store, @NonNull Factory factory) {
    mFactory = factory;
    mViewModelStore = store;
}

//步骤二：从mViewModelStore取ViewModel，如果有就直接返回，否则用mFactory创建，并存入mViewModelStore
@NonNull
@MainThread
public <T extends ViewModel> T get(@NonNull Class<T> modelClass) {
    String canonicalName = modelClass.getCanonicalName();
    if (canonicalName == null) {
        throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
    }
    return get(DEFAULT_KEY + ":" + canonicalName, modelClass);
}

@NonNull
@MainThread
public <T extends ViewModel> T get(@NonNull String key, @NonNull Class<T> modelClass) {
    ViewModel viewModel = mViewModelStore.get(key);

    if (modelClass.isInstance(viewModel)) {
        if (mFactory instanceof OnRequeryFactory) {
            ((OnRequeryFactory) mFactory).onRequery(viewModel);
        }
        return (T) viewModel;
    } else {
        //noinspection StatementWithEmptyBody
        if (viewModel != null) {
            // TODO: log a warning.
        }
    }
    if (mFactory instanceof KeyedFactory) {
        viewModel = ((KeyedFactory) (mFactory)).create(key, modelClass);
    } else {
        viewModel = (mFactory).create(modelClass);
    }
    mViewModelStore.put(key, viewModel);
    return (T) viewModel;
}

```



### ViewModelStore的来源

```java
    class ComponentActivity {
    	@NonNull
        @Override
        public ViewModelStore getViewModelStore() {
            if (getApplication() == null) {
                throw new IllegalStateException("Your activity is not yet attached to the "
                        + "Application instance. You can't request ViewModel before onCreate call.");
            }
            ensureViewModelStore();
            return mViewModelStore;
        }

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        void ensureViewModelStore() {
            if (mViewModelStore == null) {
                NonConfigurationInstances nc =
                        (NonConfigurationInstances) getLastNonConfigurationInstance();
                if (nc != null) {
                    // Restore the ViewModelStore from NonConfigurationInstances
                    mViewModelStore = nc.viewModelStore;
                }
                if (mViewModelStore == null) {
                    mViewModelStore = new ViewModelStore();
                }
            }
        }
    }
```



**保存viewModelStore**
界面销毁时：ComponentAcitivty.onRetainNonConfViewModeligurationInstance()
让NonConfigurationInstances保存了viewModelStore，NonConfigurationInstances传递到了ActivityThread中ActivityClientRecord.**lastNonConfigurationInstanceslast**

**取viewModelStore**
getViewModelStore()获取

ActivityThread中ActivityClientRecord管理lastNonConfigurationInstances
每次旋转屏幕都会让Activity销毁重新创建，在Activity.attach方法中传入**lastNonConfigurationInstances**



## 三、Lifecycles

观察Activity或者Fragment的生命周期

```java
getLifecycle().addObserver(LifecycleObserver);

public class CustomNetworkStateManager implements DefaultLifecycleObserver {

    private static final CustomNetworkStateManager S_MANAGER = new CustomNetworkStateManager();

    // 如果当网络状态发生变化时，让BaseFragment --  TODO 子类可以重写该方法，统一的网络状态通知和处理
    // public final CustomProjectLiveData<NetworkState> mNetworkStateCallback = new CustomProjectLiveData<>();

    private NetworkStateReceive mNetworkStateReceive;

    private CustomNetworkStateManager() {
    }

    public static CustomNetworkStateManager getInstance() {
        return S_MANAGER;
    }

    /**
     * 那么观察到 观察 BaseActivity 的 生命周期方法 后 做什么事情呢？
     * 答；就是注册一个 广播，此广播可以接收到信息（然后 输出 “网络不给力”）
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume(@NonNull LifecycleOwner owner) {
        mNetworkStateReceive = new NetworkStateReceive();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        if (owner instanceof AppCompatActivity) { // Activity
            ((AppCompatActivity) owner).registerReceiver(mNetworkStateReceive, filter);
        } else if (owner instanceof Fragment) { // Fragment
            requireNonNull(((Fragment) owner).getActivity())
                    .registerReceiver(mNetworkStateReceive, filter);
        }
    }

    /**
     * 那么观察到 观察 BaseActivity 的 生命周期方法 后 做什么事情呢？
     * 答；就是移除一个 广播
     * @param owner
     */
    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (owner instanceof AppCompatActivity) {
            ((AppCompatActivity) owner).unregisterReceiver(mNetworkStateReceive);
        } else if (owner instanceof Fragment) {
            requireNonNull(((Fragment) owner).getActivity())
                    .unregisterReceiver(mNetworkStateReceive);
        }
    }
```



## 四、Room

效率高，底层操作SQlite

ButterKnife、Dagger类似，编译期框架，通过注解生成代码，

**由3部分组成：**数据库的表（@Entity）、增删改查（@Dao）、数据库实例（@Database）

**数据库变化回调：**可以通过获取`Dao`层的`LiveData`添加`Observer`，数据更新之后自动回调`Observer`的`onChange()`

```java
@Entity  // 表
public class Student {

    @PrimaryKey(autoGenerate = true)  // 主键唯一，自动增长
    private int id;

    @ColumnInfo(name = "name")  // 优先使用这个
    private String name;

    @ColumnInfo(name = "age") // 优先使用这个
    private int age;

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
```

```java
@Dao   // Database access object   （增删改查 实际操作）
public interface StudentDao {

    // 插入
    @Insert
    void insertWords(Student... students); // 10条数据

    @Update
    void updateWords(Student... students);

    // 条件删除
    @Delete
    void deleteWords(Student... students);

    // 全部删除
    @Query("DELETE FROM student")
    void deleteAllWords();

    // 查询全部
    @Query("SELECT * FROM student ORDER BY ID DESC")
    // List<Student> getAllWords();
    // LiveData 就是为了被动的知道  数据的变化
    public LiveData<List<Student>> getAllStudentLive();   // insert 李四（dao.insret(李四)）
    
}
```

```java
@Database(entities = {Student.class}, version = 1, exportSchema = false)
public abstract class StudentDatabase extends RoomDatabase {

    // student_database.db

    private static StudentDatabase INSTANCE;
    public static synchronized StudentDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),StudentDatabase.class,"student_database")
                     // .allowMainThreadQueries() // 允许可以在 main线程运行(慎用，不推荐， 单元测试)
                    .build();
        }
        return INSTANCE;
    }

    // 对外暴露
    public abstract StudentDao getStudnetDao();
}
```

```java
StudentDatabase studentDatabase = StudentDatabase.getDatabase(context.getApplicationContext());
StudentDao studentDao = studentDatabase.getStudnetDao();
LiveData<List<Student>> studentsLive = studentDao.getAllStudentLive();
```
