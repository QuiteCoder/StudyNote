# JVM真题解答

## 一、Android平台的虚拟机是基于栈吗？

### 更快！更省内存！

- 1.指令条数少
- 2.数据移动次数少，临时结果存放次数少
- 3.映射真实机器的寄存器



### Android不需要解决移植性问题

- 因为Android平台的操作系统是统一的



### 用其他方式解决了代码体积问题

- dex文件优化





## 详细解析：

### 1.JVM是基于栈的虚拟机

- 这里的“栈”指的是操作数栈，
- 操作数栈+局部变量表+方法出口（动态链接） = 一个运行时栈帧
- 一个方法（foo（））对应一个运行时栈帧

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/stack.jpg)



### 2.Dalvik是基于寄存器的虚拟机：

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/virtual_register.jpg)

- 虚拟寄存器代替了操作数栈、局部变量表、方法出口
- 但是单条指令长度相对较长，翻了1倍，一条指令操作更多事情，目的是为了减少操作次数，提高效率



详细对比如下：

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/%E8%99%9A%E6%8B%9F%E6%9C%BA%E7%89%B9%E7%82%B9%E6%AF%94%E8%BE%83.jpg)

注意：第一行 “字节码单元长度” jvm 8位（1字节） Dalvik 16位（2字节），意思是单条指令所占用的bit位数或者说字节数是多少





## 二、如何看待基于栈的虚拟机设计？

### JVM基于栈去设计的初衷之一，是压缩代码体积

- Java设计之初最重要的一个特性就是跨平台，支持嵌入式设备和手持设备（J2ME，例如机顶盒，手机）。当时的设备没有大容量存储空间，早期的手机只有几MB的存储容量。
- Java设计之初，另一个特性是，支持远程传输执行字节码，要降低传输开销。



### 基于栈的虚拟机，字节码实现简单

- 指的是生成字节码的过程简单，而不是虚拟机本身简单，或者说编译器的实现简单。
- 操作时，不用考虑寄存器的地址（绝对位置），只需要把想要操作的数据出栈、入栈，然后再实现如何针对栈进行操作就可以了。



### 基于栈的虚拟机，可移植性高

- 为了提高效率，虚拟寄存器要映射到真实机器的寄存器，增加移植的难度，因为硬件平台不一样的话，寄存器的操控指令是不一样的。
- 代码移植到其他硬件平台的时候，不用考虑真实机器寄存器的差异，因为操作栈的指令是通用的。



## 三、如何看待Android平台基于寄存器的设计

### 更快！更省内存！

- 指令条数少
- 数据移动次数少、临时结果存放次数少
- 能够映射真实机器的寄存器，速度更快



### Android不需要解决移植性问题

- Android平台的操作系统是统一的



### 用其他方式解决了代码体积问题

- dex文件优化



## 四、为什么dex文件比class文件更适合移动端

Class文件与dex文件的结构对比：

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/class%E4%B8%8Edex%E5%AF%B9%E6%AF%94.jpg)

可以看到两者有巨大的差别

class文件对每个类都做了常量、方法名、字段的记录，就算是子类与父类共有的方法和变量、常量都单独保存了一份在常量池中。

而dex则对重复的部分用原型来保存一份，存放在数据区，再通过索引来区分该调用原型数据。

意思是子类就不会保存父类中的方法名和变量、常量字段信息，他们的数据信息都会统一记录到数据区中，然后再索引区中各种数据结构的排列组合记录着，这样能够节约移动端安装包的体积。

再例如：代码中多个参数相同，返回值相同，但是名字不同，或者所在类不同的方法，就会通过索引区中的DexMethodId和DexProtoId的组合来查找数据区的数据

**dex文件索引区和数据区的组合相当于class文件中的常量池**



结论：

class文件每个类都有独立的常量池，导致代码冗余

dex文件常量统一记录在数据区，所有类都通过索引获取常量的值，相同的常量可以合并，大大降低了代码的冗余





## 五、你能不能自己写一个叫java.lang.object的类？

主要考ClassLoader的双亲委派模型，带着这些疑问来分析：

这是做什么的？

加载过程？

加载给谁？

这样做的好处？

有些情况需要打破双亲委派模型？

### Java双亲委派模型：

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/java%E7%9A%84%E5%8F%8C%E4%BA%B2%E5%A7%94%E6%B4%BE%E6%A8%A1%E5%9E%8B.jpg)



### Android双亲委派模型：

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/%E5%8F%8C%E4%BA%B2%E5%A7%94%E6%B4%BE%E6%A8%A1%E5%9E%8B.jpg)

Android少了ExtensionClassLoader

自定义ClassLoader则变成了DexClassLoader，因为Android是用dex文件的，不再是class文件



### 双亲委派模型加载过程：

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/%E5%8F%8C%E4%BA%B2%E5%A7%94%E6%B4%BE%E6%A8%A1%E5%9E%8B%E5%8A%A0%E8%BD%BD%E8%BF%87%E7%A8%8B.jpg)



### 好处：

- 能够对类划分优先级层次关系
- 避免类的重复加载
- 沙箱安全机制，避免代码被篡改



### 结论：

回到问题本身，Android自身的双亲委派机制是不能加载到自己自定义写的“Java.lang.Object”

如果通过重写loadClass方法，让自定义的ClassLoader直接加载“Java.lang.Object”，这样就防止了BootClassLoader去加载Framework的。

```java
class CustomDexClassLoader extends BaseDexClassLoader {

    public CustomDexClassLoader(String dexPath, File optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }
}
```

或者使用热更新的做法，将dex插在dexElement数组靠前的位置

```java
/**
* dex作为插件加载
*/
private void dexPlugin(){
    //插件包文件
    File file = new File(“/sdcard/hotfix.dex”);
    if (!file.exists()) {
        Log.i(“MApplication”, “插件hotfix不存在”);
        return;
    }
    try {
        //获取到 BaseDexClassLoader 的 pathList字段
        // private final DexPathList pathList;
        Field pathListField = BaseDexClassLoader.class.getDeclaredField(“pathList”);
        //破坏封装，设置为可以调用
        pathListField.setAccessible(true);
        //拿到当前ClassLoader的pathList对象
        Object pathListObj = pathListField.get(getClassLoader());    
        //获取当前ClassLoader的pathList对象的字节码文件（DexPathList ）
        Class<?> dexPathListClass = pathListObj.getClass();
        //拿到DexPathList 的 dexElements字段
        // private final Element[] dexElements；
        Field dexElementsField = dexPathListClass.getDeclaredField("dexElements");
        //破坏封装，设置为可以调用
        dexElementsField.setAccessible(true);

        //使用插件创建 ClassLoader
        DexClassLoader pathClassLoader = new DexClassLoader(file.getPath(), getCacheDir().getAbsolutePath(), null, getClassLoader());
        //拿到插件的DexClassLoader 的 pathList对象
        Object newPathListObj = pathListField.get(pathClassLoader);
        //拿到插件的pathList对象的 dexElements变量
        Object newDexElementsObj = dexElementsField.get(newPathListObj);

        //拿到当前的pathList对象的 dexElements变量
        Object dexElementsObj=dexElementsField.get(pathListObj);

        int oldLength = Array.getLength(dexElementsObj);
        int newLength = Array.getLength(newDexElementsObj);
        //创建一个dexElements对象
        Object concatDexElementsObject = Array.newInstance(dexElementsObj.getClass().getComponentType(), oldLength + newLength);
        //先添加新的dex添加到dexElement
        for (int i = 0; i < newLength; i++) {
            Array.set(concatDexElementsObject, i, Array.get(newDexElementsObj, i));
        }
        //再添加之前的dex添加到dexElement
        for (int i = 0; i < oldLength; i++) {
            Array.set(concatDexElementsObject, newLength + i, Array.get(dexElementsObj, i));
        }
        //将组建出来的对象设置给 当前ClassLoader的pathList对象
        dexElementsField.set(pathListObj, concatDexElementsObject);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```




## 六、被new出来的实例是否都存在堆中？

答案：否

涉及知识点：标量替换，栈上分配，逃逸分析

这里需要思考几个问题：

- 堆和栈的区别是什么？
- 实例放在栈中的好处是什么？
- 什么情况下，实例适合放在栈中？



标量替换

把堆中的数据，等价平移到栈的局部变量表中的操作



栈上分配

在栈上分配空间，存放实例的操作



什么情况下，实例适合存放在栈中？

如下图的两种写法

左：foo方法需要返回一个user实例供外部访问的，user实例不能够在此栈帧销毁，那么实例就不适合放在栈中。

右：foo方法只是返回一个String实例，user实例实例并没参与外部访问，那么实例就适合存放在栈中，好处是不用GC，实例会跟随栈帧销毁

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/%E9%80%83%E9%80%B8%E5%88%86%E6%9E%90.jpg)

