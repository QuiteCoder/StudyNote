# MVI框架

背景：app架构的演变过程，MVC -> MVP -> MVVM -> MVI

MVVM为什么会演变成MVI呢?

MVVM是model，view，viewMode，model负责管理数据，viewMode负责将数据与VIew的绑定

View 负责接收用户的输入事件，然后将事件传递给 ViewModel；
ViewModel 收到事件后，会进行业务分发，通知 Model 获取数据；
Model 区分数据来源，进而通过不同渠道获取数据，拿到数据后返回给 ViewModel；
ViewModel 进行后续处理，或者通知 View 更新 UI。

使用DataBinding的方式，在VIew的xml中与ViewMode进行绑定

优点：view和数据同步性强。

缺点：

1.代码量大时，xml臃肿，难以阅读；

2.难以复用布局XML文件，因为布局已经跟ViewMode、javaBean绑定；

3.排查问题困难，数据变化时更新View，有时候业务逻辑要根据View的状态去获取数据，但是获取完数据后又马上更新View，这样来回的响应不但消耗资源，更难于排查问题，而且不能debug，因为有逻辑在布局中。





## MVI：

View层通过携程channel给ViewMode中的携程channel单向传递Intent（意图），在ViewMode中可以解析Intent，分类执行各种业务逻辑，或者请求数据，在ViewMode中拿到数据后，或者处理完业务之后通过flow给viewState穿透数据，在让viewState给View穿透数据，保证数据单向传递，让程序易于溯源。

