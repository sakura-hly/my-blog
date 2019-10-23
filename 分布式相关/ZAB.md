# ZooKeeper's atomic broadcast protocol: Theory and practice

## Introduction

Zookeeper是一个具备容错功能的分布式协调服务，它封装了分布式协调算法以及维护了一个简单的数据库。

这个服务旨在高可用和高可靠性。客户端使用它来引导、存储配置数据、进程的运行状态、组成员信息、实现同步原语和管理故障恢复。服务通过副本机制实现可用性和可靠性，在处理读请求方面有很高的性能。

Zookeeper通常由3或5台主机组成，其中有一个leader，只要大多数(quorum/majority)可用，那么整个系统就是可用的。

Zookeeper中一个重要的组件是Zab，原子广播算法(Atomic Broadcast algorithm)，这是一个协议，用来管理副本的原子更新。这个算法复制leader选举、副本同步、管理要广播的更新事务以及崩溃恢复。
