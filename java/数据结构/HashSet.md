# HashSet

## HashSet是啥？

- 保障元素都是唯一的;

- 是一个空间换时间的玩意，占地面积大，存取速度快;

- 它不像Map那样是K，V的键值对，它只有一个V
- 一般常用的操作为放入元素，检查是否存在元素，遍历整个Set，遍历有个缺点，它是无序的，如果要求遍历有序时可以换用`LinkedHashSet`

使用场景：需要使用集合保存一组对象时，如果要求对象不重复，并且对存取的速度快的场景，就可以使用HashSet。



## 原理：

- HashSet实际上是一个HashMap实例，都是一个存放链表的数组

- 它不保证存储元素的迭代顺序，此类**允许使用null元素**

- HashSet中不允许有重复元素，这是因为HashSet是基于HashMap实现的，HashSet中的元素都存放在HashMap的key上面，而value中的值都是统一的一个固定对象private static final Object PRESENT = new Object()

- HashSet中add方法调用的是底层HashMap中的put()方法，而如果是在HashMap中调用put，首先会判断key是否存在，如果key存在则修改value值，如果key不存在这插入这个key-value。而在set中，因为value值没有用，也就不存在修改value值的说法，因此往HashSet中添加元素，首先判断元素（也就是key）是否存在，如果不存在这插入，如果存在则不插入，这样HashSet中就不存在重复值

-  所以判断key是否存在就要重写元素的类的equals()和hashCode()方法，当向Set中添加对象时，首先调用此对象所在类的hashCode()方法，计算次对象的哈希值，此哈希值决定了此对象在Set中存放的位置；若此位置没有被存储对象则直接存储，若已有对象则通过对象所在类的equals()比较两个对象是否相同，相同则不能被添加