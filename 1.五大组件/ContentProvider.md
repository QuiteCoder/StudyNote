# ContentProvider

内容提供者：

Android 的数据存储方式总共有五种，分别是：Shared Preferences、网络存储、文件存储、外储存储、SQLite。但一般这些存储都只是在单独的一个应用程序之中达到一个数据的共享，有时候我们需要操作其他应用程序的一些数据，就会用到 ContentProvider。而且 Android 为常见的一些数据提供了默认的 ContentProvider（包括音频、视频、图片和通讯录等）。



## 创建过程：

在 AMS 的attachApplication方法中，会进一步调用 ApplicationThread 的bindApplication方法，这个过程同样是跨进程完成的。bindApplication方法的逻辑会经过 ActivityThread 中的mH Handler 切换到 ActivityThread 中去执行，具体执行的方法是handleBindApplication。在handleBindApplication方法中，ActivityThread 会创建 Application 对象，并加载 ContentProvider。这里需要注意的是，ActivityThread 会先加载 ContentProvider，然后再调用 Application 的onCreate方法。这意味着 ContentProvider 的初始化会先于 Application，确保在应用正式启动前，ContentProvider 已经准备好提供数据服务。

在加载 ContentProvider 的过程中，ActivityThread 会遍历当前进程的 ProviderInfo 列表，并一一调用installProvider方法来启动每个 ContentProvider。在installProvider方法中，会通过类加载器完成 ContentProvider 对象的创建，然后通过 ContentProvider 的attachInfo方法来调用它的onCreate方法。至此，ContentProvider 已经被创建并且onCreate方法被调用，意味着 ContentProvider 已经成功启动。之后，ActivityThread 会将已经启动的 ContentProvider 发布到 AMS 中，AMS 会把它们存储在 ProviderMap 中，这样一来外部调用者就可以直接从 AMS 中获取 ContentProvider 了。整个启动过程确保了 ContentProvider 能够在应用启动时顺利初始化并准备好提供数据共享服务，为应用之间的数据交互奠定了基础。



```java
// ActivityThread.java
private void handleBindApplication(AppBindData data) {
    Application app;
    ...
	// 创建Applicaton实例
    app = data.info.makeApplicationInner(data.restrictedBackupMode, null);
    ...
	if (!data.restrictedBackupMode) {
    	if (!ArrayUtils.isEmpty(data.providers)) {
            // 创建ContentProvider实例
        	installContentProviders(app, data.providers);
         }
    }
    ...
}

private void installContentProviders(
    Context context, List<ProviderInfo> providers) {
    final ArrayList<ContentProviderHolder> results = new ArrayList<>();

    for (ProviderInfo cpi : providers) {
        if (DEBUG_PROVIDER) {
            StringBuilder buf = new StringBuilder(128);
            buf.append("Pub ");
            buf.append(cpi.authority);
            buf.append(": ");
            buf.append(cpi.name);
            Log.i(TAG, buf.toString());
        }
        ContentProviderHolder cph = installProvider(context, null, cpi,
                                                    false /*noisy*/, true /*noReleaseNeeded*/, true /*stable*/);
        if (cph != null) {
            cph.noReleaseNeeded = true;
            results.add(cph);
        }
    }

    try {
        ActivityManager.getService().publishContentProviders(
            getApplicationThread(), results);
    } catch (RemoteException ex) {
        throw ex.rethrowFromSystemServer();
    }
}
```



## 用法：

### 1. **创建ContentProvider子类**

继承`ContentProvider`并实现核心方法：

```Java
public class StudentProvider extends ContentProvider {
    private static final String AUTHORITY = "com.example.student.provider";
    private static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/students");
    private static final int STUDENTS_DIR = 1;
    private static final int STUDENTS_ITEM = 2;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    
    static {
        uriMatcher.addURI(AUTHORITY, "students", STUDENTS_DIR);
        uriMatcher.addURI(AUTHORITY, "students/#", STUDENTS_ITEM);
    }
    
    private SQLiteDatabase db;

    @Override
    public boolean onCreate() {
        // 初始化数据库
        SQLiteOpenHelper helper = new SQLiteOpenHelper(getContext(), "student.db", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE students (_id INTEGER PRIMARY KEY, name TEXT)");
            }
            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
        };
        db = helper.getWritableDatabase();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (uriMatcher.match(uri)) {
            case STUDENTS_DIR:
                // 查询所有学生
                return db.query("students", projection, selection, selectionArgs, null, null, sortOrder);
            case STUDENTS_ITEM:
                // 查询指定ID的学生
                String id = uri.getLastPathSegment();
                return db.query("students", projection, "_id=?", new String[]{id}, null, null, sortOrder);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    // 实现其他方法：insert(), update(), delete(), getType()
}
```

### 2. **注册ContentProvider**

在`AndroidManifest.xml`中添加：

```xml
<provider
    android:name=".StudentProvider"
    android:authorities="com.example.student.provider"
    android:exported="true" <!-- 允许其他应用访问 -->
    android:readPermission="com.example.permission.READ_STUDENT"
    android:writePermission="com.example.permission.WRITE_STUDENT"/>
```



运行 HTML

### 3. **定义URI与数据操作**

- **URI结构**：`content://authority/path/id`
- **使用UriMatcher**匹配不同请求类型，如区分查询全部或单个记录。

### 4. **客户端访问ContentProvider**

通过`ContentResolver`操作数据：

```Java
// 查询所有学生
Cursor cursor = getContentResolver().query(
    Uri.parse("content://com.example.student.provider/students"),
    new String[]{"_id", "name"}, null, null, null);

// 插入新学生
ContentValues values = new ContentValues();
values.put("name", "Alice");
Uri newUri = getContentResolver().insert(
    Uri.parse("content://com.example.student.provider/students"), values);
```

### 5. **处理权限**

- **声明权限**：在提供方应用的`AndroidManifest.xml`中定义权限。
- **请求权限**：客户端应用需在`AndroidManifest.xml`中添加相应权限：

```xml
<uses-permission android:name="com.example.permission.READ_STUDENT"/>
<uses-permission android:name="com.example.permission.WRITE_STUDENT"/>
```



运行 HTML

### 6. **通知数据变化**

在`ContentProvider`中更新数据后，通知观察者：

```Java
getContext().getContentResolver().notifyChange(uri, null);
```

### 7. **MIME类型**

实现`getType()`方法：

```java
@Override
public String getType(Uri uri) {
    switch (uriMatcher.match(uri)) {
        case STUDENTS_DIR:
            return "vnd.android.cursor.dir/vnd.com.example.student";
        case STUDENTS_ITEM:
            return "vnd.android.cursor.item/vnd.com.example.student";
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
    }
}
```

