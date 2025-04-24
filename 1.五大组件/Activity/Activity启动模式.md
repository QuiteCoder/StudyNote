# 启动模式



总结：sandard，标准栈模式，先入后出，可重复创建activity
	   singleTop，栈顶复用模式，非栈顶activity可重复创建
           singleTask，栈内复用模式，栈内只存在一个实例，把栈顶对象清除，把自己放栈顶
	   singleInstance，单实例模式，独自在另一个栈中，并且不会存在第二个实例

Activity有四种加载模式：standard(默认)， singleTop， singleTask和 singleInstance。以下逐一举例说明他们的区别：

standard：Activity的默认加载方法，即使某个Activity在 Task栈中已经存在，另一个activity通过Intent跳转到该activity，同样会新创建一个实例压入栈中。例如：现在栈的情况为：A B C D，在D这个Activity中通过Intent跳转到D，那么现在的栈情况为： A B C D D 。此时如果栈顶的D通过Intent跳转到B，则栈情况为：A B C D D B。此时如果依次按返回键，D  D C B A将会依次弹出栈而显示在界面上。

singleTop：如果某个Activity的Launch mode设置成singleTop，那么当该Activity位于栈顶的时候，再通过Intent跳转到本身这个Activity，则将不会创建一个新的实例压入栈中。例如：现在栈的情况为：A B C D。D的Launch mode设置成了singleTop，那么在D中启动Intent跳转到D，那么将不会新创建一个D的实例压入栈中，此时栈的情况依然为：A B C D。但是如果此时B的模式也是singleTop，D跳转到B，那么则会新建一个B的实例压入栈中，因为此时B不是位于栈顶，此时栈的情况就变成了：A B C D B。

singleTask：如果某个Activity是singleTask模式，那么Task栈中将会只有一个该Activity的实例。例如：现在栈的情况为：A B C D。B的Launch mode为singleTask，此时D通过Intent跳转到B，则栈的情况变成了：A B。而C和D被弹出销毁了，也就是说位于B之上的实例都被销毁了。

singleInstance：将Activity压入一个新建的任务栈中。例如：Task栈1的情况为：A B C。C通过Intent跳转到D，而D的Launch mode为singleInstance，则将会新建一个Task栈2。此时Task栈1的情况还是为：A B C。Task栈2的情况为：D。此时屏幕界面显示D的内容，如果这时D又通过Intent跳转到D，则Task栈2中也不会新建一个D的实例，所以两个栈的情况也不会变化。而如果D跳转到C，则栈1的情况变成了：A B C C，因为C的Launch mode为standard，此时如果再按返回键，则栈1变成：A B C。也就是说现在界面还显示C的内容，不是D。

原文链接：https://blog.csdn.net/csdnlaoban/article/details/138447517