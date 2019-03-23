# Innodb索引和B+树
InnoDB 存储引擎在绝大多数情况下使用 B+ 树建立索引，这是关系型数据库中查找最为常用和有效的索引。

### Innodb存储引擎索引概述
Innodb存储引擎支持两种常见的索引：B+树索引和哈希索引。
Innodb支持哈希索引是自适应的，Innodb会根据表的使用情况自动生成哈希索引。
B+树索引就是传统意义上的索引，是关系型数据库中最常用最有效的索引。B+树是从最早的平衡二叉树演变而来，但是B+树不是一个二叉树。B+中的B不代表二叉(Binary),而是代表平衡(Balance)。

注意：B+树索引并不能找到一个键值对应的具体行，只能找到被查找数据行所在的页，然后数据库通过把页读入内存，再在内存中查找，最后得到结果。

### 理解B+树算法
B+树是为磁盘及其他存储辅助设备而设计一种平衡查找树（不是二叉树）。B+树中，所有记录的节点按大小顺序存放在同一层的叶节点中，各页节点用指针进行连接。

下面演示一个B+数结构，高度为2，每页可放4条记录，扇出(fan out)为5。从下图可以看出，所有记录都在页节点中，并且为顺序存放，我们从最左边的叶节点开始遍历，可以得到所有键值的顺序排序：5、10、15、20、25、30、50、55、60、65、75、80、85、90.
![](http://www.niceru.com/wp-content/uploads/2013/09/1-600x161.png)
图1  高度为2的B+树

1. B+树的插入操作
B+树的插入必须保证插入后叶节点的记录依然排序，同时要考虑插入B+树的三种情况，每种情况都可能导致不同的插入算法。如下表所示：

| Leaf Page Full | Index Page Full | 操作 |
| --- | --- | --- |
| No | No | 直接将记录插入叶节点 |
| Yes | No | 1.拆分LeafPage  2.将中间的节点放入IndexPage  3.小于中间节点的记录放左边  4.大于等于中间节点的记录放右边 |
| Yes | Yes | 1.拆分LeafPage  2.小于中间节点的记录放左边  3.大于等于中间节点的记录放右边 4.拆分IndexPage  5.小于中间节点的记录放左边  6.大于中间节点的记录放右边  7.中间节点放入上一层IndexPage |

我们实例分析B+树的插入，在图1的B+树中，我们需要插入28这个值。因为Leaf Page和Index page都没有满，我们直接将记录插入叶节点就可以了。如下图所示：
![](http://www.niceru.com/wp-content/uploads/2013/09/2-600x161.png)
图2  插入键值28

下面我们再插入70这个值，这时Leaf Page已经满了，但是Index Page还没有满，符合上面的第二种情况。这时插入Leaf Page的情况为
50、55、60、65、70.我们根据中间的值60拆分叶节点，可得到下图3所示（双向链表指针依然存在，没有画出）：
![](http://www.niceru.com/wp-content/uploads/2013/09/3-600x236.png)
图3 插入键值70

最后我们再插入95，这个Leaf Page和Index Page都满了，符合上面第三种情况。需要做2次拆分，如下图4所示：
![](http://www.niceru.com/wp-content/uploads/2013/09/4-600x279.png)
图4 插入键值95

可以看到，不管怎么变化，B+树总会保持平衡。可是为了保持平衡，对于新插入的键值可能需要做大量的拆分页动作。B+树主要用于磁盘，拆分意味这磁盘的操作，应该在可能的情况下尽可能减少对页的拆分。因此，B+树提供了旋转功能。旋转发在LeafPage已经满了，但是左右兄弟节点没有满的情况下。这时B+树并不是急着做页的拆分，而是将记录移到所在页的兄弟节点上。通常情况下，左兄弟被首先检查用来做旋转操作，这时我们插入键值70，其实B+树并不会急于去拆分叶节点，而是做旋转，50，55，55旋转。
![](http://images2015.cnblogs.com/blog/990532/201701/990532-20170117135001442-1547516540.png)
图5 B+树的旋转操作

2. B+树的删除操作
B+树使用填充因子来控制树的删除变化，填充因子可以设置的最小值为50%，B+树的删除操作同样保证删除后叶节点的记录依然排序。
根据填充因子的变化，B+树删除依然需要考虑三种情况，如下表所示：

| Leaf Page Below Fill Factor | Index Page Below Fill Factor | 操作 |
| --- | --- | --- |
| No | No | 直接将记录从叶节点删除，如果改节点还是Index Page中的节点，则用该节点的右节点代替 |
| Yes | No | 合并叶节点及其兄弟节点，同时更新Index Page |
| Yes | Yes | 1.合并叶节点及其兄弟节点   2.更新Index Page   3.合并Index Page及其兄弟节点 |
根据图4的B+树，我们进行删除操作，首先删除键值为70的这条记录，该记录符合上表第一种情况，删除后如下图6所示：
![](http://www.niceru.com/wp-content/uploads/2013/09/6-600x278.png)
图6 删除键值70

接着我们删除键值为25的记录，这也是第一种情况，但是该值还是Index Page中的值，因此在删除Leaf Page中25的值后，还应将25的右兄弟节点的28更新到Page Index中，最后可得到图。
![](http://www.niceru.com/wp-content/uploads/2013/09/7-600x296.png)
图7 删除键值28

最后我们来看删除键值为60的情况，删除Leaf Page中键值为60的记录后，填充因子小于50%，这时需要做合并操作，同样，在删除Index Page中相关记录后需要做Index Page的合并操作，最后得到图。
![](./doc.img/B+tree.delete60.png)

***

### B+树索引介绍
B+树索引的本质是B+树在数据库中的实现。但是B+树有一个特点是高扇出性，因此在数据库中，B+树的高度一般在2到3层。也就是说查找某一键值的记录，最多只需要2到3次IO开销。按磁盘每秒100次IO来计算，查询时间只需0.0.2到0.03秒。

数据库中B+索引分为聚集索引（clustered index）和非聚集索引（secondary index）。这两种索引的共同点是内部都是B+树，高度都是平衡的，叶节点存放着所有数据。不同点是叶节点是否存放着一整行数据。

1. 聚集索引
Innodb存储引擎表是索引组织表，即表中数据按主键顺序存放。而聚集索引就是按每张表的主键构造一棵B+树，并且叶节点存放整张表的行记录数据。每张表只能有一个聚集索引（一个主键）。
聚集索引的另一个好处是它对于主键的排序查找和范围的速度非常快，叶节点的数据就是我们要找的数据。

主键排序查找：例如我们要找出最新的10位女生，由于B+树是双项链表，我们可以迅速找到最后一个页，并取出10条记录，我们用Explain进行分析：
```
explain select * from girl order by id desc limit 10
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: girl
         type: index
possible_keys: NULL
          key: PRIMARY
      key_len: 4
          ref: NULL
         rows: 10
        Extra:
1 row in set (0.00 sec)
```

主键范围查找：如果要通过主键查找某一范围内的数据，通过叶节点的上层中间节点就能得到页的范围，之后直接读取数据页即可：
```
explain select * from girl where id>10000000 and id<12000000
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: girl
         type: range
possible_keys: PRIMARY
          key: PRIMARY
      key_len: 8
          ref: NULL
         rows: 4301486
        Extra: Using where
1 row in set (0.00 sec)
```


2. 辅助索引
辅助索引（也称非聚集索引）。叶级别不包含行的全部数据，叶级别除了包含行的键值外，每个索引行还包含了一个书签（bookmark），该书签告诉InnoDB存储引擎，哪里可以找到与索引对应的数据，
辅助索引的存在并不影响数据在聚集索引中的组织，因此一个表可以有多个辅助索引。
当通过辅助索引查找数据时，innodb会遍历辅助索引并通过叶级别的指针获得指向主键索引的主键。然后再通过主键索引找到一行完整的数据。


3. B+树索引的管理
索引的创建和删除可以用两种方式。一种是alter table,另一种是create/drop index

alter table 创建和删除索引的语法为：
```
ALTER [ONLINE | OFFLINE] [IGNORE] TABLE tbl_name
  | ADD {INDEX|KEY} [index_name]
        [index_type] (index_col_name,…) [index_option] …

ALTER [ONLINE | OFFLINE] [IGNORE] TABLE tbl_name
  | DROP PRIMARY KEY
  | DROP {INDEX|KEY} index_name
```
create/drop index的语法为：
```
CREATE [ONLINE|OFFLINE] [UNIQUE|FULLTEXT|SPATIAL] INDEX index_name
    [index_type]
    ON tbl_name (index_col_name,…)

DROP [ONLINE|OFFLINE] INDEX index_name ON tbl_name
```
MySQL索引注意的问题：对于MySQL索引的添加与删除，mysql先是创建一张加好索引的临时表，然后把数据导入临时表，再删除原表，把临时表重命名为原表。

Innodb存储引擎从Innodb Plugin版本开始，支持一种快速创建索引的方法（只限于辅助索引，主键索引仍需要临时建表）。首先对表加S锁，在创建的过程中不需要重建表，但是由于加上了S锁，在创建索引的过程中只能进行查询操作，不能更新数据。

***

### B+树索引的使用
1. 什么时候使用B+索引
当查询表中很少一部分数据时，B+索引才有意义。对于性别，地区类型字段，他们的取值范围很小，即 *低选择性* 。这时B+索引是没有必要的。相反，某个字段取值范围很广，如姓名，几乎没有重复，即 *高选择性* 。则使用B+索引是比较合适的。因此。当访问高选择性字段并取出很少一部分数据时，该字段加B+索引是非常有效的。但是当取出的数据行占表中大部分数据时，数据库就不会使用B+索引了。

举例说明下，看下面这个团购订单表groupon_so的部分索引：
```
show index from groupon_so;
*************************** 1. row ***************************
        Table: groupon_so
   Non_unique: 0
     Key_name: PRIMARY
 Seq_in_index: 1
  Column_name: id
    Collation: A
  Cardinality: 10088342
     Sub_part: NULL
       Packed: NULL
         Null:
   Index_type: BTREE
      Comment:
Index_comment:
*************************** 2. row ***************************
        Table: groupon_so
   Non_unique: 1
     Key_name: idx_groupon_so_order_id
 Seq_in_index: 1
  Column_name: order_id
    Collation: A
  Cardinality: 10088342
     Sub_part: NULL
       Packed: NULL
         Null:
   Index_type: BTREE
      Comment:
Index_comment:
*************************** 3. row ***************************
        Table: groupon_so
   Non_unique: 1
     Key_name: idx_groupon_so_order_code
 Seq_in_index: 1
  Column_name: order_code
    Collation: A
  Cardinality: 10088342
     Sub_part: NULL
       Packed: NULL
         Null:
   Index_type: BTREE
      Comment:
Index_comment:
*************************** 4. row ***************************
        Table: groupon_so
   Non_unique: 1
     Key_name: idx_groupon_so_end_user_id
 Seq_in_index: 1
  Column_name: end_user_id
    Collation: A
  Cardinality: 10088342
     Sub_part: NULL
       Packed: NULL
         Null: YES
   Index_type: BTREE
      Comment:
Index_comment:
*************************** 5. row ***************************
        Table: groupon_so
   Non_unique: 1
     Key_name: idx_groupon_so_groupon_id
 Seq_in_index: 1
  Column_name: groupon_id
    Collation: A
  Cardinality: 148357
     Sub_part: NULL
       Packed: NULL
         Null:
   Index_type: BTREE
      Comment:
Index_comment:
```
其中有一个索引 idx_groupon_so_order_id ，这个索引里面字段订单号的值都是不重复的，是高选择性的字段。
我们查找order_id为 99165590 的这条记录，执行计划如下：
```
explain select * from groupon_so where order_id=99165590
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: groupon_so
         type: ref
possible_keys: idx_groupon_so_order_id
          key: idx_groupon_so_order_id
      key_len: 8
          ref: const
         rows: 1
        Extra:
1 row in set (0.00 sec)
```
可以看到使用了idx_groupon_so_order_id这个索引，符合高选择性，取少部分数据这个特性。
但是如果执行下面这条语句：
```
explain select * from groupon_so where order_id>99165590;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: groupon_so
         type: ALL
possible_keys: idx_groupon_so_order_id
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 10092839
        Extra: Using where
1 row in set (0.00 sec)
```
可以看到possible_keys依然是idx_groupon_so_order_code，但是索引优化使用的索引keys显示的是NULL，因为虽然这个字段是高选择性的，但是我们取出了表中的大部分数据，索引没有用到索引。
```
select @a:=count(id) from groupon_so where order_id>99165590;
+—————+
| @a:=count(id) |
+—————+
|       8684424 |
+—————+
1 row in set (2.48 sec)

select @a:=count(id) from groupon_so;
+—————+
| @a:=count(id) |
+—————+
|       9858135 |
+—————+
1 row in set (1.86 sec)

select 8684424/9858135;
+—————–+
| 8684424/9858135 |
+—————–+
|          0.8809 |
+—————–+
1 row in set (0.00 sec)
```
可以看到我们取出了表中88%的数据，索引没有用到索引。


2. 顺序读、随机读与预读取
顺序读是指顺序的读取磁盘上的块，随机读是指访问的块是不连续的，需要磁盘的磁头不断移动。随机读的性能是远远低于顺序读的。
在数据库中，顺序读根据索引的叶节点就能顺序的读取所需的行数据，这个顺序读只是逻辑的顺序读，在磁盘上还可能是随机读。随机读是指访问辅助索引叶节点不能完全得到结果，需要根据辅助索引叶节点中的主键去寻找实际数据行。对于一些取表里很大部分的数据，正是因为读取是随机读，而随机读的性能会远低于顺序读，所以优化器才会选择全部扫描顺序读，而不使用索引。

Innodb存储引擎有两种预读取方式，随机预读取和线性预读取。随机预读取是指当一个区（共64个连续页）中有13个页在缓冲区中并被频繁访问时，Innodb存储引擎会将这个区中剩余的页预读到缓冲区。线性预读取基于缓冲池中页的访问方式，而不是数量。如果一个区中有24个页被顺序访问了，则Innodb会读取下一个区中所有的页到缓冲区。但是Innodb预读取经过测试后性能比较差，经过TPCC测试发现禁用预读取比启用预读取提高了10%的性能。在新版本Innodb中，mysql禁用了随机预读取，仅保留了线性预读取，并且加入了innodb_read_ahead_threshold参数，当连续访问页超过该值时才启用预读取，默认值为56。
```
show variables like 'innodb_read_ahead_threshold%';
+—————————–+——-+
| Variable_name               | Value |
+—————————–+——-+
| innodb_read_ahead_threshold | 56    |
+—————————–+——-+
1 row in set (0.00 sec)
```

3. 辅助索引的优化
通过前面可知，辅助索引的叶节点包含主键，但是辅助索引的叶节点并不包含完整的行数据信息，因此，Innodb存储引擎总是会从辅助索引的叶节点判断是否能得到数据，让我们看个例子：
```
create table t (a int not null, b varchar(20), primary key(a),key(b));
Query OK, 0 rows affected (0.18 sec)
insert into t select 1, ' kangaroo';
Query OK, 1 row affected (0.00 sec)
Records: 1  Duplicates: 0  Warnings: 0
insert into t select  2,'dolphin';
Query OK, 1 row affected (0.01 sec)
Records: 1  Duplicates: 0  Warnings: 0
insert into t select  3,'dragon';
Query OK, 1 row affected (0.01 sec)
Records: 1  Duplicates: 0  Warnings: 0
insert into t select  4,'anteloge';
Query OK, 1 row affected (0.01 sec)
Records: 1  Duplicates: 0  Warnings: 0
```
如果执行select * from t很多人认为应该是如下结果：
```
mysql> select * from t order by a;
*************************** 1. row ***************************
a: 1
b: kangaroo
*************************** 2. row ***************************
a: 2
b: dolphin
*************************** 3. row ***************************
a: 3
b: dragon
*************************** 4. row ***************************
a: 4
b: anteloge
4 rows in set (0.00 sec)
```
但是实际执行结果确是：
```
mysql> select * from t;
*************************** 1. row ***************************
a: 4
b: anteloge
*************************** 2. row ***************************
a: 2
b: dolphin
*************************** 3. row ***************************
a: 3
b: dragon
*************************** 4. row ***************************
a: 1
b: kangaroo
4 rows in set (0.00 sec)
```
因为辅助索引包含了主键a的值，因此访问b列上的辅助索引就可以得到a的值，这样就可以得到表中所有的数据。我们看这条语句的执行计划：
```
mysql> explain select * from t;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: t
         type: index
possible_keys: NULL
          key: b
      key_len: 23
          ref: NULL
         rows: 4
        Extra: Using index
1 row in set (0.00 sec)
```
可以看到优化器最终走的索引b,如果想对a列进行排序，则需要进行order by操作：
```
mysql> explain select * from t order by a;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: t
         type: index
possible_keys: NULL
          key: PRIMARY
      key_len: 4
          ref: NULL
         rows: 4
        Extra: NULL
1 row in set (0.00 sec)

mysql> select * from t order by a;
*************************** 1. row ***************************
a: 1
b: kangaroo
*************************** 2. row ***************************
a: 2
b: dolphin
*************************** 3. row ***************************
a: 3
b: dragon
*************************** 4. row ***************************
a: 4
b: anteloge
```
或者使用主键强制得到结果：
```
mysql> select * from t force index(PRIMARY);
*************************** 1. row ***************************
a: 1
b: kangaroo
*************************** 2. row ***************************
a: 2
b: dolphin
*************************** 3. row ***************************
a: 3
b: dragon
*************************** 4. row ***************************
a: 4
b: anteloge
4 rows in set (0.00 sec)
```

4. 联合索引
联合索引是指对表中多个列做索引，联合索引的创建方法和之前的一样，如下：
```
mysql> alter table t add key idx_a_b(a,b);
Query OK, 0 rows affected (0.17 sec)
Records: 0  Duplicates: 0  Warnings: 0
```
联合索引还是一个B+树，不同的是联合索引键值的数量不是1，而是大于等于2.下面我们讨论一个两个整形列组成的联合索引，假定两个键值的名称分别为a和b，如下图8所示，每个节点上有两个键值，(1,1),(1,2),(2,1),(2,4),(3,1),(3,2), 数据按(a,b)顺序进行排列
![](http://www.niceru.com/wp-content/uploads/2013/09/8.png)
图8  多个键值的B+树

因此，对于查询select * from t where a=xxx and b=xxx,显然可以使用(a,b)这个联合索引。对于单个a列查询 select * from t where a=xxx也是可以使用(a,b)这个索引。但是对于b列的查询select * from t where b=xxx是用不到这颗B+树索引。可以看到叶节点上b的值为1、2、1、4、1、2.显然不是排序的，因此b列的查询使用不到(a,b)索引。

联合索引的第二个好处是，可以对第二键值进行排序。例如很多情况下我们需要查询某个用户的购物情况，并按照时间排序，取出最近3次的购买记录，这时使用联合索引可以避免多一次的排序操作。因为索引本身在叶节点中已经排序了。看下面示例：
```
mysql> create table buy_log(userid int unsigned not null, buy_date date);
Query OK, 0 rows affected (0.09 sec)

mysql> insert into buy_log values(1,'2013-01-01');
Query OK, 1 row affected (0.01 sec)

mysql> insert into buy_log values(2,'2013-01-01');
Query OK, 1 row affected (0.01 sec)

mysql> insert into buy_log values(3,'2013-01-01');
Query OK, 1 row affected (0.01 sec)

mysql> insert into buy_log values(1,'2013-02-01');
Query OK, 1 row affected (0.01 sec)

mysql> insert into buy_log values(3,'2013-02-01');
Query OK, 1 row affected (0.00 sec)

mysql> insert into buy_log values(1,'2013-03-01');
Query OK, 1 row affected (0.01 sec)

mysql> insert into buy_log values(1,'2013-04-01');
Query OK, 1 row affected (0.01 sec)

mysql> alter table buy_log add key(userid);
Query OK, 0 rows affected (0.07 sec)
Records: 0  Duplicates: 0  Warnings: 0

mysql> alter table buy_log add key(userid,buy_date);
Query OK, 0 rows affected (0.11 sec)
Records: 0  Duplicates: 0  Warnings: 0
```
上面我们建立了测试表和数据，建立了2个索引来比较。两个索引都包含了userid字段。如果只对于userid查询，优化器的选择是：
```
mysql> explain select * from buy_log where userid=2;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: buy_log
         type: ref
possible_keys: userid,userid_2
          key: userid
      key_len: 4
          ref: const
         rows: 1
        Extra: NULL
1 row in set (0.00 sec)
```
可以看到possible_keys里面两个索引都可以使用，分别是单个的userid索引和userid,buy_date的联合索引。但是优化器最终选择的是userid，因为该叶节点包含单个键值，因此一个页存放的记录应该更多。

接下来看以下的查询，假定要取出userid=1最近的3次购买记录，分别使用单个索引和联合索引的区别：
```
mysql> explain select * from buy_log where userid=1 order by buy_date desc limit 3;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: buy_log
         type: ref
possible_keys: userid,userid_2
          key: userid_2
      key_len: 4
          ref: const
         rows: 4
        Extra: Using where; Using index
1 row in set (0.00 sec)
```
同样对于上述SQL，两个索引都可使用，但是查询优化器使用了userid和buy_date组成的联合索引userid_2.因为这个联合索引中buy_date已经排序好了，可以减少一次排序操作。
```
mysql> explain select * from buy_log force index(userid) where userid=1 order by buy_date desc limit 3;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: buy_log
         type: ref
possible_keys: userid
          key: userid
      key_len: 4
          ref: const
         rows: 4
        Extra: Using where; Using filesort
1 row in set (0.00 sec)
```
在Extra这里可以看到Using filesort，Using filesort指排序，但不一定是在文件中完成。