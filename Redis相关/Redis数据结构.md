# Redis 底层数据结构
我们都知道Redis支持五种数据类型，分别是字符串、哈希表（map）、列表（list）、集合（set）和有序集合，和Java的集合框架类似，不同数据类型的数据结构实也是不一样的。

## redisObject
Redis 是用C编写的，内部有一个结构体 redisObject，
```
/*
 * Redis 对象
 */
typedef struct redisObject {
 
    // 类型
    unsigned type:4;        
 
    // 不使用(对齐位)
    unsigned notused:2;
 
    // 编码方式
    unsigned encoding:4;
 
    // LRU 时间（相对于 server.lruclock）
    unsigned lru:22;
 
    // 引用计数
    int refcount;
 
    // 指向对象的值
    void *ptr;
 
} robj;
```
属性含义：
* type: redisObject的类型，字符串、列表、集合、有序集、哈希表
* encoding: 底层实现结构，字符串、整数、跳跃表、压缩列表等
* ptr: 指向实际保存值的数据结构
## Redis对象底层数据结构
Redis 底层有八种数据结构
* REDIS_ENCODING_INT: long 类型的整数
* REDIS_ENCODING_EMBSTR: embstr 编码的简单动态字符串
* REDIS_ENCODING_RAW: 动态字符串
* REDIS_ENCODING_HT: 字典
* REDIS_ENCODING_LINKEDLIST: 双端列表
* REDIS_ENCODING_ZIPLIST: 压缩列表
* REDIS_ENCODING_INTSET: 整数集合
* REDIS_ENCODING_SKIPLIST: 跳跃表

## 字符串对象
字符串对象的编码可以是INT、EMBSTR和RAW。

如果一个字符串的内容可以转换为long，那么该字符串就会被转换成long类型，对象的ptr就会指向该long，type也用REDIS_ENCODING_INT表示。

普通字符串有两种，EMBSTR和RAW。从下面这段代码可以看出，如果字符串对象的长度 <= 44字节，就用embstr对象，否则用传统的raw对象。
```
// Redis 版本为 Redis 3.2.100 (00000000/0) 64 bit
#define REDIS_ENCODING_EMBSTR_SIZE_LIMIT 44
robj *createStringObject(char *ptr, size_t len) {
    if (len <= REDIS_ENCODING_EMBSTR_SIZE_LIMIT)
        return createEmbeddedStringObject(ptr,len);
    else
        return createRawStringObject(ptr,len);
}
```
embstr有以下几点好处
1. embstr的创建只需分配一次内存，而raw为两次（一次为sds分配对象，另一次为objet分配对象，embstr省去了第一次）
2. 相对地，释放内存的次数也由两次变为一次
3. embstr的objet和sds放在一起，更好地利用缓存带来的优势

需要注意的是，redis并未提供任何修改embstr的方式，即embstr是只读的形式。对embstr的修改实际上是先转换为raw再进行修改。
## 列表对象
列表对象的编码可以是linkedlist和ziplist。

ziplist是一种压缩列表，相比linkedlist更节省空间，因为它所存储的内容都是在连续的内存区域当中的。在Redis配置文件中有这一项：
```
# -5: max size: 64 Kb  <-- not recommended for normal workloads
# -4: max size: 32 Kb  <-- not recommended
# -3: max size: 16 Kb  <-- probably not recommended
# -2: max size: 8 Kb   <-- good
# -1: max size: 4 Kb   <-- good
list-max-ziplist-size -2
```
意味着list元素不超过8kb时，就采用ziplist存储。
但当数据量过大时就ziplist就不是那么好用了，因为为了保证存储内容在内存中的连续性，插入的复杂度是O(N)，即每次插入都会重新进行realloc。
对象结构中ptr所指向的就是一个ziplist。整个ziplist只需要malloc一次，它们在内存中是一块连续的区域。

linkedlist是一种双向链表。它的结构比较简单，节点中存放pre和next两个指针，还有节点相关的信息。当每增加一个node的时候，就需要重新malloc一块内存。
## 哈希对象
哈希对象的底层可以是ziplist和hashtable。

ziplist中的哈希对象是按照key1,value1,key2,value2这样的顺序存放来存储的。当对象数目不多且内容不大时，这种方式效率是很高的。

hashtable的是由dict这个结构来实现的
```
typedef struct dict {
    dictType *type;
    void *privdata;
    dictht ht[2];
    long rehashidx; /* rehashing not in progress if rehashidx == -1 */
    int iterators; /* number of iterators currently running */
} dict;
```
其中的指针dicht ht[2] 指向了两个哈希表
```
typedef struct dictht {
    dictEntry **table;
    unsigned long size;
    unsigned long sizemask;
    unsigned long used;
} dictht;
```
dicht[0] 是用于真正存放数据，dicht[1]一般在哈希表元素过多进行rehash的时候用于中转数据。

dictht中的table用于真正存放元素了，每个key/value对用一个dictEntry表示，放在dictEntry数组中。
## 集合对象
集合对象的编码可以是intset或者hashtable。

intset是一个整数集合，里面存的为某种同一类型的整数，支持如下三种长度的整数：
* define INTSET_ENC_INT16 (sizeof(int16_t))
* define INTSET_ENC_INT32 (sizeof(int32_t))
* define INTSET_ENC_INT64 (sizeof(int64_t))

intset是一个有序集合，查找元素的复杂度为O(logN)，但插入时不一定为O(logN)，因为有可能涉及到升级操作。
比如当集合里全是int16_t型的整数，这时要插入一个int32_t，那么为了维持集合中数据类型的一致，那么所有的数据都会被转换成int32_t类型，涉及到内存的重新分配，这时插入的复杂度就为O(N)了。
intset不支持降级操作。
## 有序集合对象
有序集合的编码可能两种，一种是ziplist，另一种是skiplist与dict的结合。

ziplist作为集合和作为哈希对象是一样的，member和score顺序存放。按照score从小到大顺序排列。

skiplist是一种跳跃表，它实现了有序集合中的快速查找，在大多数情况下它的速度都可以和平衡树差不多（具体可以看看[跳跃表](https://redisbook.readthedocs.io/en/latest/internal-datastruct/skiplist.html?spm=a2c4e.11153940.blogcont331962.13.fd244473uYknAj)）。