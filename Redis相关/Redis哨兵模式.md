# Redis 哨兵模式
## 哨兵
sentinel，译名是哨兵，主要有以下作用
* 集群监控：复制监控主节点和从节点是否正常工作
* 消息通知：如果某个 Redis 节点有故障，那么哨兵会发送消息给管理员
* 故障转移：如果主节点挂了，会自动转移到从节点上
* 配置中心：如果发生故障转移，将新的主节点地址发送给客户端

哨兵用于实现 Redis 高可用，本身也是分布式的，作为一个哨兵集群运行。
* 故障转移时，判断一个主节点是否挂了，需要大多数哨兵同意才行，涉及到分布式选举
* 即使部分哨兵节点挂掉了，哨兵集群还是能正常工作的
## 哨兵的核心知识
* 哨兵至少需要3个实例，来保证集群高可用
* 哨兵集群 + Redis 主从架构只能保证 Redis 集群高可用，不保证数据零丢失

哨兵集群必须部署 2 个以上节点
```
2 个哨兵，majority=2
3 个哨兵，majority=2
4 个哨兵，majority=2
5 个哨兵，majority=3
...
```
一般会开启三个哨兵节点搭建哨兵集群。

## Redis 哨兵主备切换的数据丢失问题
主备切换的过程，可能会导致数据丢失
1. 异步复制导致数据丢失
   
   因为主节点复制数据到从节点是异步的，所以可能还没有复制完，主节点就宕机了，此时就丢失了部分数据。
   ![](./doc.img/async-replication-data-lose-case.png)
2. 脑裂导致数据丢失

   脑裂，就是某个正常的主节点突然脱离了网络，跟其他从节点断开连接，但是实际上主节点还运行着。此时哨兵可能会认为主节点宕机了，然后开启选举，将其他从节点
   切换成主节点，这时集群中就有了两个主节点。
   
   此时虽然某个从节点被切换成了主节点，但是可能客户端还没有切换到新的主节点，还继续往旧的主节点写数据。因此等旧的主节点恢复后，会被作为一个从节点挂到新的主节点上，
   自己的数据会被清空，这样就丢失了部分数据。
   ![](./doc.img/redis-cluster-split-brain.png)
   
## 如何解决数据丢失
进行如下配置：
```
min-slaves-to-write 1
min-slaves-max-lag 10
```
表示，要求至少有 1 个 slave，数据复制和同步的延迟不能超过 10 秒。

如果所有的从节点数据复制和同步的延迟都超过10秒，主节点就不会接收任何请求。
1. 减少异步复制数据的丢失
   
   有了 min-slaves-max-lag 这个配置，就可以确保说，一旦从节点复制数据和 ack 延时太长，就认为可能主节点宕机后损失的数据太多了，
   那么就拒绝写请求，这样可以把主节点宕机时由于部分数据未同步到从节点导致的数据丢失降低的可控范围内。
2. 减少脑裂的数据丢失

   如果一个主节点出现了脑裂，跟其他从节点丢了连接，那么上面两个配置可以确保说，如果不能继续给指定数量的从节点发送数据，
   而且从节点超过 10 秒没有给自己 ack 消息，那么就直接拒绝客户端的写请求。因此在脑裂场景下，最多就丢失 10 秒的数据。
   
## sdown 和 odown
* sdown是主观宕机，如果只有一个哨兵觉得一个主节点宕机了，那么就是主观宕机
* odown是客观宕机，如果majority数量的哨兵都认为一个主节点宕机了，那么就是客观宕机

sdown 达成的条件很简单，如果一个哨兵 ping 一个主节点，超过了 is-master-down-after-milliseconds 指定的毫秒数之后，就主观认为主节点宕机了；
如果一个哨兵在指定时间内，收到了majority数量的其它哨兵也认为那个 master 是主节点的，那么就认为是 odown 了。
## 哨兵集群的自动发现机制
哨兵之间互相发现，是通过 Redis 的 pub/sub 功能实现的，每个哨兵都会往 \_\_sentinel\_\_:hello 这个 channel 里发送一个消息，这时所有其他哨兵都可以消费这个消息，
可以由此感知到其他哨兵的存在。

每隔两秒钟，每个哨兵都会往自己监控的某个 master+slaves 对应的 \_\_sentinel\_\_:hello channel 里发送一个消息，内容是自己的 host、ip 和 runid 还有对这个 master 的监控配置。

每个哨兵也会去监听自己监控的每个 master+slaves 对应的\_\_sentinel\_\_:hello channel，然后去感知到同样在监听这个 master+slaves 的其他哨兵的存在。

每个哨兵还会跟其他哨兵交换对 master 的监控配置，互相进行监控配置的同步。
## slave 配置的自动纠正
哨兵还负责自动纠正 slave 的一些配置。比如 slave 如果要成为潜在的 master 候选人，哨兵会确保 slave 复制现有 master 的数据；
如果 slave 连接到了一个错误的 master 上，比如故障转移之后，那么哨兵会确保它们连接到正确的 master 上。

## slave->master 选举算法
如果一个主节点被odown了，而且majority数量的哨兵都允许主备切换，那么某个哨兵就会执行主备切换操作，此时首先要选举一个从节点来，会考虑从节点的一些信息：
* 跟主节点断开连接的时长
* 从节点的优先级
* replica offset
* run id

如果一个从节点跟主节点断开连接的时间已经超过了 down-after-milliseconds 的 10 倍，外加 节点宕机的时长，那么 slave 就被认为不适合选举为 master。
```
(down-after-milliseconds * 10) + milliseconds_since_master_is_in_SDOWN_state
```
接下来会对 slave 进行排序：
* 按照 slave 优先级进行排序，slave priority 越低，优先级就越高
* 如果 slave priority 相同，那么看 replica offset，哪个 slave 复制了越多的数据，offset 越靠后，优先级就越高
* 如果上面两个条件都相同，那么选择一个 run id 比较小的那个 slave

## quorum 和 majority
* quorum：确认odown的最少的哨兵数量
* majority：授权进行主从切换的最少的哨兵数量

每次一个哨兵要做主备切换，首先需要 quorum 数量的哨兵认为 odown，然后选举出一个哨兵来做切换，这个哨兵还需要得到 majority 数量哨兵的授权，才能正式执行切换。

如果 quorum < majority，比如 5 个哨兵，majority 就是 3，quorum 设置为 2，那么就 3 个哨兵授权就可以执行切换。

但是如果 quorum >= majority，那么必须 quorum 数量的哨兵都授权，比如 5 个哨兵，quorum 是 5，那么必须 5 个哨兵都同意授权，才能执行切换。
## configuration epoch
哨兵会对一套 redis master+slaves 进行监控，有相应的监控的配置。

执行切换的那个哨兵，会从要切换到的新 master（salve->master）那里得到一个 configuration epoch，这就是一个 version 号，每次切换的 version 号都必须是唯一的。

如果第一个选举出的哨兵切换失败了，那么其他哨兵，会等待 failover-timeout 时间，然后接替继续执行切换，此时会重新获取一个新的 configuration epoch，作为新的 version 号。

## configuration 传播
哨兵完成切换之后，会在本地生成最新的master配置，然后同步给其他的哨兵，就是通过 pub/sub 消息机制。

这里之前的 version 号就很重要了，因为各种消息都是通过一个 channel 去发布和监听的，所以一个哨兵完成一次新的切换之后，新的 master 配置是跟着新的 version 号的。其他的哨兵都是根据版本号的大小来更新自己的 master 配置的。

转载[https://github.com/doocs/advanced-java/blob/master/docs/high-concurrency/redis-sentinel.md](https://github.com/doocs/advanced-java/blob/master/docs/high-concurrency/redis-sentinel.md)