# 触摸事件ANR分析

参考文章：https://maimai.cn/article/detail?fid=1448975106&efid=6P1FS0XgGjuyF9qwyxEN3g

内核将原始事件写入到设备节点中，InputReader在其线程循环中不断地从EventHub中抽取原始输入事件，进行加工处理后将加工所得的事件放入InputDispatcher的派发发队列中。InputDispatcher则在其线程循环中将派发队列中的事件取出，查找合适的窗口，将事件写入到窗口的事件接收管道中。窗口事件接收线程的Looper从管道中将事件取出，交由窗口事件处理函数进行事件响应。关键流程有：原始输入事件的读取与加工；输入事件的派发；输入事件的发送、接收与反馈。其中输入事件派发是指InputDispatcher不断的从派发队列取出事件、寻找合适的窗口进行发送的过程，输入事件的发送是InputDispatcher通过Connection对象将事件发送给窗口的过程。

InputDispatcher与窗口之间的跨进程通信主要通过InputChannel来完成。在InputDispatcher与窗口通过InputChannel建立连接之后，就可以进行事件的发送、接收与反馈；

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

