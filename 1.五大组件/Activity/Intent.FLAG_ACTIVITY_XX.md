

# Intent.FLAG_ACTIVITY_NEW_TASK 是什么

## 一、对话内容

Android启动模式，startActivity中的intent新增flag `Activity.FLAG.ACTIVITY_NEW_TASK` 是否会对目标Activity的启动模式造成影响。 因为非Activity类型的context需要添加这个标志，如果目标activity是`SingleTask`/`SingleTop`/`SingleInstance`启动模式，是否会有影响呢？
`Activity.FLAG.ACTIVITY_NEW_TASK` 标志主要用于确保当从一个非 Activity 类型的 Context（例如，一个 Service 或 Application）启动 Activity 时，该 Activity 会被放置在一个新的任务（task）栈中。

然而，这个标志并不会直接改变目标 Activity 的启动模式（即 `singleTask`、`singleTop`、`standard` 或 `singleInstance`）。启动模式是在 `AndroidManifest.xml` 文件中为 <activity> 标签定义的，它决定了 Activity 如何与任务（task）中的其他 Activity 交互。

但 `Activity.FLAG.ACTIVITY_NEW_TASK` 与启动模式之间有一些交互作用：

singleTask：如果目标 Activity 已经在当前任务栈中存在，并且它是该栈的根 Activity，那么系统会将这个 Activity 带到栈顶，而不是启动一个新的实例。但是，如果 `Activity.FLAG.ACTIVITY_NEW_TASK`  被设置，并且目标 Activity 不在当前任务栈中，系统会在一个新的任务栈中启动这个 Activity。
singleTop：如果目标 Activity 已经在当前任务栈的栈顶，那么系统不会启动新的实例，而是复用现有的实例。但是，如果 `Activity.FLAG.ACTIVITY_NEW_TASK`  被设置，并且目标 Activity 不在当前任务栈中（或不在栈顶），系统会启动一个新的任务栈并将 `Activity` 放置在其中。
singleInstance：在这种模式下，Activity 总是其自己的任务栈的根。这意味着无论 `Activity.FLAG.ACTIVITY_NEW_TASK`  是否被设置，该 Activity 都会在一个新的任务栈中启动。
standard：在这种模式下，Activity 会被放置在启动它的 Activity 所在的任务栈中。但是，如果 `Activity.FLAG.ACTIVITY_NEW_TASK`  被设置，Activity 会在一个新的任务栈中启动。
总的来说，`Activity.FLAG.ACTIVITY_NEW_TASK`  会影响 Activity 被放置在哪个任务栈中，但它不会直接改变 Activity 的启动模式。目标 Activity 的启动模式仍然会按照 AndroidManifest.xml 中的定义来影响它与任务栈中其他 Activity 的交互。

任务栈里面除了activity，还包含什么
在Android中，任务栈（Task）主要用于管理一个或多个Activity。传统上，任务栈里主要存放的是Activity实例，这些Activity按照它们被启动的顺序依次排列在栈中。然而，除了Activity之外，任务栈本身并不直接包含其他类型的组件（如`Service`、`BroadcastReceiver`、`ContentProvider`）。

但要注意的是，Activity可能与这些其他组件进行交互。例如，一个Activity可能会启动一个Service来处理后台任务，或者注册一个`BroadcastReceiver`来监听系统广播。然而，这些组件本身并不直接存储在任务栈中。

另外，任务栈的概念与Android应用的进程模型相关。一个应用可能包含多个任务栈，每个任务栈都与用户执行的某项任务相关联。例如，用户可能在同一个应用中执行了多个不同的操作或流程，每个操作或流程都可能对应一个单独的任务栈。

总结来说，任务栈主要存放的是Activity实例，用于管理用户与应用之间的交互。而其他组件如Service、BroadcastReceiver等虽然与Activity交互，但它们本身并不直接存储在任务栈中。

那非Activity的context启动另外一个activity，必须要添加`Activity.FLAG.ACTIVITY_NEW_TASK` 来表明是新开一个栈，为什么还要开发手动添加这样一个flag，直接系统实现了不就好了么
非Activity的Context（如`Service`、`Application`等）启动Activity时要求添加`Activity.FLAG_ACTIVITY_NEW_TASK`标志的原因与Android的设计哲学和用户体验有关。

首先，Android系统是基于任务和返回栈（Back Stack）的概念来管理用户与Activity之间的交互的。每个任务通常与一个或多个Activity相关联，并且这些Activity按照它们被启动的顺序在返回栈中排列。当用户与Activity交互时（如点击按钮启动新Activity），这些Activity会被推入（push）到返回栈中。当用户按下后退按钮时，它们又会从栈中弹出（pop）并销毁。

在标准的用户界面中，Activity通常是由另一个Activity启动的。在这种情况下，新启动的Activity会自然地被推入到启动它的Activity所在的任务栈中。然而，当非Activity的Context（如`Service`或`Application`）尝试启动Activity时，系统就面临一个挑战：它不知道应该将这个Activity推入哪个任务栈，因为没有明确的“父”Activity来确定其归属。

为了解决这个问题，Android引入了`Activity.FLAG_ACTIVITY_NEW_TASK`标志。当这个标志被设置时，系统知道需要为这个Activity创建一个新的任务栈（如果它还不存在的话），并将这个Activity作为新栈的根Activity。这确保了Activity能够被正确地管理，并且用户可以通过标准的任务切换机制（如长按Home键或使用多任务按钮）来访问它。

至于为什么Android不直接为这种情况实现默认行为，这可能与灵活性和控制性有关。通过要求开发者显式地设置这个标志，Android允许开发者更精细地控制Activity的启动和管理。开发者可以根据应用的具体需求和用户体验来决定是否创建新的任务栈，或者是否将Activity推入到现有的任务栈中。这种灵活性使得Android应用能够适应各种复杂的场景和用例。

既然如此，那么添加了这个标志，如果是非activity的context来启动，无论如何，被启动的activity都是在一个新栈中，即便它之前是存在另外一个栈且是`singleTask`/`singleTop`启动模式？
当使用非Activity的Context（如`Service`或`Application`）启动一个Activity，并设置了`Intent.FLAG_ACTIVITY_NEW_TASK`标志时，确实会创建一个新的任务栈（如果该Activity不在当前任何任务栈中），并将这个Activity作为新栈的根。但关于`singleTask`和`singleTop`启动模式，我们需要进一步澄清。

singleTask：

如果被启动的Activity已经在某个任务栈中存在，并且是栈根的Activity，那么系统会将它带到栈顶，并清空它之上的所有Activity。这是singleTask启动模式的基本行为。
但是，如果非Activity的Context启动了这个Activity，并且设置了`Intent.FLAG_ACTIVITY_NEW_TASK`，那么即使这个Activity已经在另一个任务栈中存在，系统也会在新的任务栈中启动它，而不是将它带到现有任务栈的栈顶。这是因为`FLAG_ACTIVITY_NEW_TASK`指示系统创建一个新任务（如果必要的话）。
singleTop：

如果被启动的Activity已经在任务栈的栈顶，那么singleTop模式会阻止系统创建新的Activity实例，而是复用现有的实例。
但是，当使用非Activity的Context启动并设置了`Intent.FLAG_ACTIVITY_NEW_TASK`时，即使该Activity已经在另一个任务栈的栈顶，系统仍然会在新的任务栈中启动它。这是因为`FLAG_ACTIVITY_NEW_TASK`的优先级高于singleTop模式。
总结来说，当使用非Activity的Context启动Activity并设置了`Intent.FLAG_ACTIVITY_NEW_TASK`时，被启动的Activity会在新的任务栈中启动，即使它之前已经存在另一个任务栈中，且其启动模式是`singleTask`或`singleTop`。这是因为`FLAG_ACTIVITY_NEW_TASK`会强制创建新任务（如果需要的话）。如果希望按照Activity的启动模式来管理任务栈，最好从现有的Activity启动该Activity，而不是从非Activity的Context启动。

在新的任务栈启动，是指将原本存在的Activity从另外一个栈移动到新的栈，还是值在新的栈新创建一个目标activity？
当使用非Activity的Context（如`Service`或`Application`）启动一个Activity，并设置了`Intent.FLAG_ACTIVITY_NEW_TASK`标志时，Android系统不会将原本存在于另一个任务栈中的Activity移动到新的任务栈。相反，它会在新的任务栈中新创建一个目标Activity的实例。

这是因为`FLAG_ACTIVITY_NEW_TASK`的主要目的是指示系统，如果必要的话，创建一个新的任务来容纳被启动的Activity。这与Activity的启动模式（如`singleTask`或`singleTop`）是分开处理的。启动模式决定了Activity如何与任务栈中的其他Activity交互，而`FLAG_ACTIVITY_NEW_TASK`则决定了Activity应该被放置在哪个任务栈中。

所以，即使目标Activity之前已经存在于另一个任务栈中，并且其启动模式是singleTask或singleTop，当使用非Activity的Context并设置`FLAG_ACTIVITY_NEW_TASK`来启动它时，Android系统仍然会在新的任务栈中创建一个新的Activity实例。这意味着用户将看到一个全新的Activity实例，而不是之前已经存在的那个实例。



## 二、其他FLAG

FLAG_ACTIVITY_SINGLE_TOP

这个FLAG就相当于加载模式中的singletop，比如说原来栈中情况是A,B,C,D在D中启动D，栈中的情况还是A,B,C,D

FLAG_ACTIVITY_CLEAR_TOP

这个FLAG就相当于加载模式中的SingleTask，这种FLAG启动的Activity会把要启动的Activity之上的Activity全部弹出栈空间。类如：原来栈中的情况是A,B,C,D这个时候从D中跳转到B，这个时候栈中的情况就是A,B了

FLAG_ACTIVITY_BROUGHT_TO_FRONT

这个网上很多人是这样写的。如果activity在task存在，拿到最顶端,不会启动新的Activity。这个有可能会误导大家！ 他这个FLAG其实是这个意思！比如说我现在有A，在A中启动B，此时在Ａ中Intent中加上这个标记。此时B就是以FLAG_ACTIVITY_BROUGHT_TO_FRONT方式启动，此时在B中再启动C，D（正常启动C,D），如果这个时候在D中再启动B，这个时候最后的栈的情况是 A,C,D,B。如果在A,B,C,D正常启动的话，不管B有没有用FLAG_ACTIVITY_BROUGHT_TO_FRONT启动，此时在D中启动B的话，还是会变成A,C,D,B的。

FLAG_ACTIVITY_NO_USER_ACTION

onUserLeaveHint()作为activity周期的一部分，它在activity因为用户要跳转到别的activity而要退到background时使用。比如,在用户按下Home键，它将被调用。比如有电话进来（不属于用户的选择），它就不会被调用。

那么系统如何区分让当前activity退到background时使用是用户的选择？

它是根据促使当前activity退到background的那个新启动的Activity的Intent里是否有FLAG_ACTIVITY_NO_USER_ACTION来确定的。

注意：调用finish()使该activity销毁时不会调用该函数

FLAG_ACTIVITY_NO_HISTORY

意思就是说用这个FLAG启动的Activity，一旦退出，它不会存在于栈中，比方说！原来是A,B,C这个时候再C中以这个FLAG启动D的，D再启动E，这个时候栈中情况为A,B,C,E

原文链接：https://blog.csdn.net/csdnlaoban/article/details/138447517



## 三、总结

activity启动目标activity时，有无该标志没有任何影响
非activity启动目标activity时，必须添加该标志
添加的作用：告诉系统新开任务栈并将新建activity放入其中