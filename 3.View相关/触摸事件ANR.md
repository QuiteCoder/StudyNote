# 触摸事件ANR分析

参考文章：https://maimai.cn/article/detail?fid=1448975106&efid=6P1FS0XgGjuyF9qwyxEN3g

​		https://duanqz.github.io/2015-10-12-ANR-Analysis

内核将原始事件写入到设备节点中，InputReader在其线程循环中不断地从EventHub中抽取原始输入事件，进行加工处理后将加工所得的事件放入InputDispatcher的派发发队列中。InputDispatcher则在其线程循环中将派发队列中的事件取出，查找合适的窗口，将事件写入到窗口的事件接收管道中。窗口事件接收线程的Looper从管道中将事件取出，交由窗口事件处理函数进行事件响应。

关键流程有：原始输入事件的读取与加工；输入事件的派发；输入事件的发送、接收与反馈。其中输入事件派发是指InputDispatcher不断的从派发队列取出事件、寻找合适的窗口进行发送的过程，输入事件的发送是InputDispatcher通过Connection对象将事件发送给窗口的过程。

InputDispatcher与窗口之间的跨进程通信主要通过InputChannel来完成。在InputDispatcher与窗口通过InputChannel建立连接之后，就可以进行事件的发送、接收与反馈；

## 一、发送，安装炸弹

```cpp
//默认分发超时间为5
sconst nsecs_t DEFAULT_INPUT_DISPATCHING_TIMEOUT = 5000 * 1000000LL;
int32_t InputDispatcher::handleTargetsNotReadyLocked(nsecs_t currentTime,
                                                     const EventEntry* entry,
                                                     const sp<InputApplicationHandle>& applicationHandle,
                                                     const sp<InputWindowHandle>& windowHandle,
                                                     nsecs_t* nextWakeupTime, const char* reason) {
    // 1.如果当前没有聚焦窗口，也没有聚焦的应用    
    if (applicationHandle == NULL && windowHandle == NULL) {
        ...
    } else {
        // 2.有聚焦窗口或者有聚焦的应用        
        if (mInputTargetWaitCause != INPUT_TARGET_WAIT_CAUSE_APPLICATION_NOT_READY) {
            // 获取等待的时间值            
            if (windowHandle != NULL) {
                // 存在聚焦窗口，DEFAULT_INPUT_DISPATCHING_TIMEOUT事件为5s                
                timeout = windowHandle->getDispatchingTimeout(DEFAULT_INPUT_DISPATCHING_TIMEOUT);
            } else if (applicationHandle != NULL) {
                // 存在聚焦应用，则获取聚焦应用的分发超时时间                
                timeout = applicationHandle->getDispatchingTimeout(DEFAULT_INPUT_DISPATCHING_TIMEOUT);
            } else {
                // 默认的分发超时时间为5s                timeout = DEFAULT_INPUT_DISPATCHING_TIMEOUT;
            }
        }
    }
    // 如果当前时间大于输入目标等待超时时间，即当超时5s时进入ANR处理流程    // currentTime 就是系统的当前时间，mInputTargetWaitTimeoutTime 是一个全局变量,    
    if (currentTime >= mInputTargetWaitTimeoutTime) {
        // 调用ANR处理流程        
        onANRLocked(currentTime, applicationHandle, windowHandle,
                    entry->eventTime, mInputTargetWaitStartTime, reason);
        // 返回需要等待处理        
        return INPUT_EVENT_INJECTION_PENDING;
    } 
}
```



## 二、接收反馈，拆除炸弹

![](E:\StudyNote\3.View相关\触摸时间超时机制.jpeg)

InputDispatcher需要经过以下复杂的调用关系，才能把一个输入事件派发出去(调用关系以按键事件为例，触屏事件的调用关系类似)：

```cpp
InputDispatcherThread::threadLoop()
└── InputDispatcher::dispatchOnce()
    └── InputDispatcher::dispatchOnceInnerLocked()
        └── InputDispatcher::dispatchKeyLocked()
            └── InputDispatcher::dispatchEventLocked()
                └── InputDispatcher::prepareDispatchCycleLocked()
                    └── InputDispatcher::enqueueDispatchEntriesLocked()
                        └── InputDispatcher::startDispatchCycleLocked()
                            └── InputPublisher::publishKeyEvent()
```

具体每个函数的实现逻辑此处不表。我们提炼出几个关键点：

- InputDispatcherThread是一个线程，它处理一次消息的派发
- 输入事件作为一个消息，需要排队等待派发，每一个Connection都维护两个队列：
  - outboundQueue: 等待发送给窗口的事件。每一个新消息到来，都会先进入到此队列
  - waitQueue: 已经发送给窗口的事件
- publishKeyEvent完成后，表示事件已经派发了，就将事件从outboundQueue挪到了waitQueue

事件经过这么一轮处理，就算是从InputDispatcher派发出去了，但事件是不是被窗口收到了，还需要等待接收方的“finished”通知。 在向InputDispatcher注册InputChannel的时候，同时会注册一个回调函数**handleReceiveCallback()**:

```cpp
int InputDispatcher::handleReceiveCallback(int fd, int events, void* data) {
    ...
    for (;;) {
        ...
        status = connection->inputPublisher.receiveFinishedSignal(&seq, &handled);
        if (status) {
            break;
        }
        d->finishDispatchCycleLocked(currentTime, connection, seq, handled);
        ...
    }
    ...
    d->unregisterInputChannelLocked(connection->inputChannel, notify);
}
```

当收到的status为OK时，会调用**finishDispatchCycleLocked()**来完成一个消息的处理：

```cpp
InputDispatcher::finishDispatchCycleLocked()
└── InputDispatcher::onDispatchCycleFinishedLocked()
    └── InputDispatcher::doDispatchCycleFinishedLockedInterruptible()
        └── InputDispatcher::startDispatchCycleLocked()
```

调用到**doDispatchCycleFinishedLockedInterruptible()**方法时，会将已经成功派发的消息从waitQueue中移除， 进一步调用会**startDispatchCycleLocked**开始派发新的事件。
