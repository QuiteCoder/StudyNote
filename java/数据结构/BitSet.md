# BitSet

> Java平台上存放位序列的集合。
> 如果需要高效的存储位序列（例如：标志）就可以使用位集。
> 由于位集将位包装在字节里，所以使用位集比使用Boolean对象的ArrayList更加高效。

##### 常用函数

```java
int length()	 		// 返回最高位为1的索引
boolean get(int bit)	// 获得一个位，返回true则为开
void set(int bit)		// 设置一个位为开
void clear(int bit)		// 设置一个位为关
void clear()			// 将所有设置为关
```

##### 计算素数个数

```java
// 计算2~2000000之间的所有素数
long start = System.currentTimeMillis();
int n = 200_0000;
int count = 0;
int i;
BitSet bit = new BitSet(n + 1);
for (i = 2; i <= n; i++) {
    bit.set(i);
}
i = 2;
int k;
while (i * i <= n) {
    if (bit.get(i)) {
        count++;
        k = i * 2;
        while (k <= n) {
            bit.clear(k);
            k += i;
        }
    }
    i++;
}
while (i <= n) {
    if (bit.get(i)) {
        count++;
    }
    i++;
}
long end = System.currentTimeMillis();
System.out.println("count: " + count); // count: 148933
System.out.println("time: " + (end - start)); 
```