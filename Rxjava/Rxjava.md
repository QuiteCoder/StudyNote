# Rxjava

简介：RxJava是基于观察者模式和迭代器模式的高级API，用于构建异步和基于事件的程序。它主要用于处理复杂的事件流，例如用户输入、网络响应等。

​	这是一种**响应式编程**，旨在处理**异步数据流**和**变化的状态**，以便能够对这些变化做出响应。它通过引入数据流和变化的概念，简化了对事件、异步操作和数据的管理。响应式编程通常与“观察者模式”密切相关，允许对象在状态变化时自动更新。



## 基本用法

```java
//1.通过 create() 方法创建一个 Observable 对象，并在 subscribe() 方法中定义事件的产生过程。
Observable.create(new ObservableOnSubscribe<Integer>() {

    @Override
    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
    // 2.Observable 对象通过调用 onNext() 方法来发射事件
        emitter.onNext(1);
        emitter.onNext(2);
        emitter.onNext(3);
        emitter.onComplete();
    }
})
.subscribeOn(Schedulers.io())   //指定被观察者的工作线程，事件产生在 IO 线程
.observeOn(AndroidSchedulers.mainThread()) //指定观察者对象的工作线程，事件处理在主线程
 // 3.通过创建一个 Observer 对象， Observer 对象通过回调方法来处理事件。
 // 4.通过调用 subscribe() 方法，将 Observable 对象与 Observer 对象进行订阅关联。即：observable.subscribe(observer);
.subscribe(new Observer<Integer>() {
    @Override
    public void onSubscribe(Disposable d) {
    // 订阅时调用
    }

    @Override
    public void onNext(Integer value) {
        // 接收到事件时调用
    }

    @Override
    public void onError(Throwable e) {
        // 发生错误时调用
    }

    @Override
    public void onComplete() {
        // 完成时调用
    }
});

```



## 原理解析

```java
// 步骤1.创建ObservableCreate，并将ObservableOnSubscribe作为参数“source”传给了ObservableCreate
Observable.create(new ObservableOnSubscribe<String>() {

            @Override
            public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Throwable {
                emitter.onNext("1768896476");
            }
        })
    // 步骤2.操作符map，作用是将上一个节点的数据转换成目标类型，这里是将String类型转换成int类型
    .map(new Function<String, Integer>() {
            @Override
            public Integer apply(String s) throws Throwable {
                // 2.1数据转换的逻辑是由自己写的
                return Integer.parseInt(s);
            }
        })
    // 步骤3.开始运作以上的节点，并将最终结果输出到以下回调中
    // 这个Observer也称为“终点”
    .subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                Log.d(TAG, "onSubscribe: ");
            }

            @Override
            public void onNext(@NonNull Integer integer) {
                Log.d(TAG, "onNext: integer = " + integer);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                Log.d(TAG, "onError: " + e);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete: ");
            }
        });
    }
```



### **一、链式调用**

```java
public abstract class Observable<@NonNull T> implements ObservableSource<T> {
    // 步骤1  所对应的源码，调用create方法
    public static <T> Observable<T> create(@NonNull ObservableOnSubscribe<T> source) {
        Objects.requireNonNull(source, "source is null");
        // 创建ObservableCreate对象，并将ObservableOnSubscribe作为参数“source”传给了ObservableCreate
        return RxJavaPlugins.onAssembly(new ObservableCreate<>(source));
    }
    
    // 步骤2  所对应的源码，map操作符
    public final <R> Observable<R> map(@NonNull Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        // 创建ObservableMap对象，并将步骤1的ObservableCreate对象和Function对象传给了ObservableMap
        return RxJavaPlugins.onAssembly(new ObservableMap<>(this, mapper));
    }
    
     // 步骤3  所对应的源码，subscribe操作符，此时相当于ObservableMap.subscribe(new Observer<Integer>() {...})
    // 因为步骤2返回的是ObservableMap对象
    public final void subscribe(@NonNull Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        try {
            ...

            ...
            // 结合以上分析，那么此时调用的是ObservableMap的subscribeActual()方法
            // 意味着封包的开始，与拆包裹的开始
            subscribeActual(observer);
        } catch (NullPointerException e) {
            throw e;
        } catch (Throwable e) {
           ...
        }
    }
}

```

### 二、洋葱模型，封包裹

封包裹的过程是从下往上的，从最后一个操作符subscribe的链式调用结束，意味着封包裹的开始

ObservableMap的源码解析：

```java
public final class ObservableMap<T, U> extends AbstractObservableWithUpstream<T, U> {
    
    public ObservableMap(ObservableSource<T> source, Function<? super T, ? extends U> function) {
        // 这个source就是步骤1中的ObservableCreate
        super(source);
        this.function = function;
    }

    @Override
    public void subscribeActual(Observer<? super U> t) {
        // 作用是将终点封装成第一级包裹：MapObserver
        // source.subscribe()相当于调用ObservableCreate的父类subscribe方法，最终会调用到本体ObservableCreate的subscribeActual()方法
        source.subscribe(new MapObserver<T, U>(t, function));
    }
}
```

ObservableCreate的源码解析：

```java
public final class ObservableCreate<T> extends Observable<T> {
    
    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        // 接收上一级包裹MapObserver进行封包裹处理，生成第二级包裹CreateEmitter
        CreateEmitter<T> parent = new CreateEmitter<>(observer);
        ...

        try {
            // 这里的source是ObservableOnSubscribe，步骤1中的emitter.onNext("1768896476")是在source.subscribe()中调用的，即emitter = parent，开始拆包裹
            source.subscribe(parent);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            parent.onError(ex);
        }
    }
}
```

最后是分析Rxjava从上往下拆包裹调用

### 三、拆包裹

接上面的调用：source.subscribe(parent)，触发了subscribe()方法

```java
Observable.create(new ObservableOnSubscribe<String>() {

    @Override
    public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Throwable {
        // 我们手动触发了onNext开始拆包裹
        emitter.onNext("1768896476");
    }
})
```



```java
// CreateEmitter是ObservableCreate的内部类，这里属于第二级包裹
static final class CreateEmitter<T>
    extends AtomicReference<Disposable>
    implements ObservableEmitter<T>, Disposable {=
    ...
    @Override
    public void onNext(T t) {
        ...
        if (!isDisposed()) {
            // 执行第一级包裹，即observer = MapObserver，接着再关注一下MapObserver
            observer.onNext(t);
        }
    }
}


```

```java
public final class ObservableMap<T, U> extends AbstractObservableWithUpstream<T, U> {
    final Function<? super T, ? extends U> function;

    // MapObserver是ObservableMap的内部类
    static final class MapObserver<T, U> extends BasicFuseableObserver<T, U> {
        final Function<? super T, ? extends U> mapper;

        MapObserver(Observer<? super U> actual, Function<? super T, ? extends U> mapper) {
            // 这个actual就是步骤3的observer对象“终点”，传给了父类，在父类中赋值给了downstream，即donwnstream = “终点”
            super(actual);
            // 这个mapper是第一级包裹的Function，自己实现apply方法的那个对象
            this.mapper = mapper;
        }

        @Override
        public void onNext(T t) {
            ...
            try {
                // 这里真是调用了mapper.apply
                v = Objects.requireNonNull(mapper.apply(t), "The mapper function returned a null value.");
            } catch (Throwable ex) {
                fail(ex);
                return;
            }
            
            // donwnstream = “终点”
            // 最终在步骤3的onNext方法回调中输出了int数据：1768896476
            downstream.onNext(v);
        }
    }
}
```

