# Kotlin

## 一、变量常量与类型

Kotlin只有引用数据类型，java有基本数据类型和引用数据类型

注意：Kotlin生成字节码之后，是有基本数据类型的，之所以在写Kotlin代码的时候只有引用数据类型，是为了方便开发

1.var是可修改的变量，val是只读的变量

2.类型推断：不需要显式的声明变量的类型

```kotlin
fun main() {
    // var是可修改的变量，val是只读的变量
    var number : Int = 5
    val number2 :Int = 5

    // 类型推断：不需要显式的声明变量的类型
    var number3 = 5
}
```



## 二、条件语句

1.if/else跟java一样

2.rang表达式：in A..B，判断是否再A和B的区间范围内

```kotlin
var age = 1
while (age > 0) {
    // 控制台输入
    val sc = Scanner(System.`in`)
    // 读取控制台int数据
    age = sc.nextInt()
    // x >= 0 && x <= 3 
    if (age in 0..3) {
        println("婴幼儿")
    } else if (age in 3..12) {
        println("少儿")
    } else if (age in 12..18) {
        println("青少年")
    } else {
        println("成年人")
    }
}

输入：3
打印：婴幼儿

输入：4
打印：少儿
```

3.when条件语句

```kotlin
var school = "高中"
val s = when (school) {
    "幼儿园" -> "幼儿"
    "小学" -> "少儿"
    "初中", "高中" -> "青少年"
    "大学" -> "成人"
    else -> "未知"
}
println("result = $s")

打印：result = 青少年
```



## 三、String模板

```kotlin
fun main() {
    val man = "Jack"
    val woman = "Rose"
    val flage = true
    println("$man love $woman")
    println("Will $woman marry $man? ${if (flage) "Yes,she will！" else "No,she won't!"}")

    // 犯罪嫌疑人
    var suspect = "张三"
    println("$suspect 涉嫌电信诈骗。")
    // 想要去除张三后的空格，用{}隔离
    println("${suspect}涉嫌电信诈骗。")
}


输出结果：
Jack love Rose
Will Rose marry Jack? Yes,she will！
张三 涉嫌电信诈骗。
张三涉嫌电信诈骗。
```



## 四、函数

### 1.声明函数

```kotlin
// 可见性修饰符，函数声明关键字，函数名，函数参数，返回类型
private fun doSomeThing(age:Int, flag:Boolean):String {}
```

函数的特性

a.函数默认可见性修饰符是public，函数默认的返回值类型时Unit（无返回），相当于java的void

b.定义函数参数时可以给予初始化数据

c.给函数传参时，可以利用参数名赋值的方式，无视传入参数的顺序

d.当函数参数都有初始化数据时，不传参就是使用默认的参数数据

```kotlin
// a.函数默认可见性修饰符是public，函数默认的返回值类型时Unit（无返回），相当于java的void
public fun doSomething(age:Int, name:String):Unit {
    println("姓名：$name,年龄：$age")
}
// b.定义函数参数时可以给予初始化数据
private fun math(number1:Int=5, number2:Int=5):Int {
    println("result = $number1+$number2")
    return number1 + number2
}

fun main() {
    doSomething(18, "Jack")
    // c.给函数传参时，可以利用参数名赋值的方式，无视传入参数的顺序
    doSomething(name = "Jimmy", age = 22)
    math(1,2)
    math(number2 = 10)
    // d.当函数参数都有初始化数据时，不传参就是使用默认的参数数据
    math()
}

输出结果：
姓名：Jack,年龄：18
姓名：Jimmy,年龄：22
result = 1+2
result = 5+10
result = 5+5
```



### 2.反引号中的函数名

使用反引号可以定义含各种符号的函数名

```kotlin
fun `**~make something funtion~**`() {
    println("special method")
}

fun main() {
    `**~make something funtion~**`()
}

输出：
special method
```



kotlin通过反引号调用java函数

```kotlin
class MyJava {
    public void is(String msg) {
        System.out.println("This is java method with Sting param :" + msg);
    }
}

// 在kotlin中is属于关键字，使用反引号才能正常调用函数名为is的java函数
fun main() {
    MyJava.`is`("测试")
}

输出：
This is java method with Sting param :测试
```



### 3.匿名函数

待补充





## 五、Nothing类型

TODO函数的任务就是抛出异常，就是永远别指望它运行成功，返回Nothing类型。

```kotlin
fun main() {
    println("doSomething...")
    TODO("我不干了！")
    // TODO之后的代码不会继续运行
    println("after doSomething")
}

输出：
doSomething...
Exception in thread "main" kotlin.NotImplementedError: An operation is not implemented: 我不干了！
	at com.example.studykotlin.TODOTestKt.main(TODOTest.kt:6)
	at com.example.studykotlin.TODOTestKt.main(TODOTest.kt)
```





## 六、Unit类型

函数默认的返回值类型时Unit（无返回），java中用void表示



# Kotlin协程

协程只是编程历史发展过程中产生的对开发者友好的一种异步编程方式，相对于传统的多线程并发编程并不能提高额外的性能，协程并发编程的底层仍是用线程池实现，因为耗时的操作总要线程去执行，协程并不能减少额外的执行逻辑，另外协程因为创建了中间的一系列封装对象，比传统的多线程编程增加了额外的内存消耗，官方文档对此特定说明了编译器会进行一些优化，从而尽可能减少内存消耗，开发者无需担心，但协程所带来的开发维护成本大大减低、内存上的妥协带来的是对开发者的友好还是值得的。

**总结一下协程的优势**

- **简洁** 更高抽象隐藏异步实现细节，实现写异步如同同步的顺序式写法
- **比闭包更好用的上下文参数** 传统回调方式严重依赖闭包特性，而要在一个连续逻辑中写出多个回调函数且共享相同的上下文，这样的代码难读懂、难写对、难维护，协程就简单多了，一个上下文抽象搞定
- **更方便处理并发逻辑** 父子协程概念 可以更容易的管理协程的执行



## 原理分析

简单的说就是通过封装Continuation（续体），编译器编译后会生成一个继承`Continuation`的类，这些续体会挂起，然后由SuspendLambda 管理状态机，

`SuspendLambda` 继承结构如下

```java
SuspendLambda` > `ContinuationImpl` > `BaseContinuationImpl` > `Continuation
```

