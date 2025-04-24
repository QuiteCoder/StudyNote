## Hook

背景：系统api或者sdk的api无法满足业务的需求，需要修改api的逻辑，但是又不可能修改源码，这时候Hook（钩子）技术就非常实用，最早期的Hook框架就是Xposed，但是需要root手机，将Xposed安装到手机才能使用，之后出现了Dexposed框架，不需要root手机，无侵入地实现运行时方法拦截。

博客：https://weishu.me/2017/11/23/dexposed-on-art/

目前流行的Android ART Hook框架有：Epic，[YAHFA](http://rk700.github.io/2017/03/30/YAHFA-introduction/)

Epic：https://github.com/tiann/epic  Epic is the continuation of [Dexposed](https://github.com/alibaba/dexposed) on ART (Supports 5.0 ~ 11).

YAHFA：https://github.com/PAGalaxyLab/YAHFA  Supports 5.0 ~ 12



## Hook原理

在 Android 的 ART（Android Runtime）中，`ArtMethod` 是表示 Java 方法的底层结构体，但 Java 层本身无法直接访问该结构。需要通过 **JNI（Java Native Interface）** 结合 Native 代码（C/C++）来间接操作。以下是获取 `ArtMethod` 的详细步骤和代码示例：

------

### **1. 基本原理**

- **`jmethodID` 的本质**：在 ART 中，`jmethodID` 实际是指向 `ArtMethod` 结构体的指针。
- **Native 层操作**：通过 JNI 获取 `jmethodID`，再将其转换为 `ArtMethod*` 指针。

------

### **2. 实现步骤**

#### **(1) 定义 Native 方法**

在 Java 类中声明一个 Native 方法，用于获取 `ArtMethod` 的指针（以 `long` 类型返回）：

```java
public class ArtUtils {
    // 获取方法的 ArtMethod 指针
    public static native long getArtMethod(Method method);
}
```

#### **(2) 实现 JNI 函数**

在 Native 代码（C++）中，将 `jmethodID` 转换为 `ArtMethod*`：

```cpp
#include <jni.h>
#include <cstdint>

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_ArtUtils_getArtMethod(JNIEnv* env, jclass clazz, jobject method) {
    // 将 Java 的 Method 对象转换为 jmethodID
    jmethodID methodId = env->FromReflectedMethod(method);
    
    // jmethodID 本质是 ArtMethod* 指针，直接转换
    return reinterpret_cast<jlong>(methodId);
}
```

#### **(3) 使用示例**

在 Java 中调用该方法获取 `ArtMethod` 指针：

```java
import java.lang.reflect.Method;

public class Main {
    static {
        System.loadLibrary("artutils");
    }

    public static void main(String[] args) throws Exception {
        Method targetMethod = String.class.getDeclaredMethod("length");
        long artMethodPtr = ArtUtils.getArtMethod(targetMethod);
        System.out.println("ArtMethod Pointer: 0x" + Long.toHexString(artMethodPtr));
    }
}
```

------

### **3. 访问 ArtMethod 的字段**

若需进一步操作 `ArtMethod` 的字段（如入口点 `entry_point_`），需知道其在结构体中的偏移量。不同 Android 版本的偏移量可能不同，需动态适配。

#### **(1) 获取入口点（entry_point_）**

以 Android 10（API 29）为例，`entry_point_` 偏移量为 `8`：

```cpp
// 获取 entry_point_ 的地址
uintptr_t entryPoint = *reinterpret_cast<uintptr_t*>(artMethodPtr + 8);
```

#### **(2) 动态计算偏移量（兼容性方案）**

通过解析 `libart.so` 的符号或特征码，动态获取偏移量（需 root 权限）：

```cpp
// 伪代码：通过符号查找偏移量
uintptr_t getEntryPointOffset() {
    void* artMethodSym = dlsym(RTLD_DEFAULT, "_ZN3art9ArtMethod13entry_point_");
    return reinterpret_cast<uintptr_t>(artMethodSym);
}
```

------

### **4. 完整示例：Hook 方法入口**

通过修改 `entry_point_` 实现方法拦截：

#### **(1) Native 代码**

cpp

复制

```cpp
#include <jni.h>
#include <cstdint>
#include <sys/mman.h>

// 定义函数指针类型
typedef void (*JNICallBridge)(void* art_method, ...);

// 备份原入口点
static JNICallBridge originalEntry = nullptr;

// Hook 函数
void hookEntry(void* art_method, ...) {
    // 自定义逻辑
    printf("Method Hooked!\n");
    // 调用原逻辑
    originalEntry(art_method, ...);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_ArtUtils_hookMethod(JNIEnv* env, jclass clazz, jlong artMethodPtr) {
    // 计算 entry_point_ 字段地址（假设偏移量为 8）
    uintptr_t entryPointAddr = artMethodPtr + 8;
    
    // 修改内存权限为可写
    mprotect((void*)(entryPointAddr & ~0xFFF), 0x1000, PROT_READ | PROT_WRITE);
    
    // 备份原入口点
    originalEntry = *reinterpret_cast<JNICallBridge*>(entryPointAddr);
    
    // 写入新入口点
    *reinterpret_cast<void**>(entryPointAddr) = reinterpret_cast<void*>(hookEntry);
    
    // 清除指令缓存（ARM 平台）
    __builtin___clear_cache(reinterpret_cast<char*>(entryPointAddr), 
                            reinterpret_cast<char*>(entryPointAddr + sizeof(void*)));
}
```

#### **(2) Java 调用**

```java
// 调用 Native 方法进行 Hook
ArtUtils.hookMethod(artMethodPtr);
```

------

### **5. 注意事项**

1. **兼容性**：不同 Android 版本的 `ArtMethod` 结构差异极大，需适配偏移量。
2. **内存权限**：修改 `ArtMethod` 前需调用 `mprotect` 修改内存页权限（可能需要 root）。
3. **稳定性**：直接操作 `ArtMethod` 可能导致崩溃或不可预知行为，需充分测试。
4. **仅限 Debug/Research**：此技术适用于调试或研究，不建议用于生产环境。

------

### **6. 开源方案参考**

- **Epic**：通过替换 `ArtMethod` 实现 Hook，支持 Android 5.0~12。
  GitHub: [tiann/epic](https://github.com/tiann/epic)
- **YAHFA**：动态生成代理方法实现 Hook。
  GitHub: [PAGalaxyLab/YAHFA](https://github.com/PAGalaxyLab/YAHFA)
