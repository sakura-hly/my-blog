# ZooKeeper's atomic broadcast protocol: Theory and practice

## Introduction

Zookeeper是一个具备容错功能的分布式协调服务，它封装了分布式协调算法以及维护了一个简单的数据库。

这个服务旨在高可用和高可靠性。客户端使用它来引导、存储配置数据、进程的运行状态、组成员信息、实现同步原语和管理故障恢复。服务通过副本机制实现可用性和可靠性，在处理读请求方面有很高的性能。

Zookeeper通常由3或5台主机组成，其中有一个leader，只要大多数(quorum/majority)可用，那么整个系统就是可用的。

Zookeeper中一个重要的组件是Zab，原子广播算法(Atomic Broadcast algorithm)，这是一个协议，用来管理副本的原子更新。这个算法复制leader选举、副本同步、管理要广播的更新事务以及崩溃恢复。

## Background

广播算法将消息从主进程（primary process）传输给其他所有的进程。

原子广播协议是分布式算法，可以保证正确广播或无副作用终止，广泛应用在分布式计算的组间通信，同时也是一种可靠（reliable）的协议，因为它满足全局顺序（total order）。比如，有以下特点：

* Validaty：如果一个正确的进程（correct process）广播了一条消息，然后所有正确的进程最终都会传递它。
* Uniform Agreement：如果一个进程传递了一条消息，然后所有正确的进程最终都会传递它。
* Uniform Integrity：对于任意消息m，每个进程最多传递一次（at most once），仅当m的发送者先前广播了m。
* Uniform Total Order：如果进程p和q都传递消息m和m'，当且仅当q在传递m'之前传递m，p才在传递m'之前传递m。

### Paxos and design desicion for Zab

Zab协议的两个重要的需求是处理多个未付的客户端的操作以及从崩溃中有效地恢复。

Paxos不支持多个未付的事务，且没有FIFO的channel用来通信，所以能容忍消息的丢失和乱序。解决方法是，将多个事务操作批量放入一个提议（proposal）中，并且每次允许最多一个提议，然而性能会下降。

Zab为每个事务生成标识来获得全局顺序，每次执行最高标识的事务。

Zookeeper其他的性能需求有：1. 低延迟。2. 突发情况下良好的吞吐量，处理写请求直线上升的情况。3. 平滑故障处理。以致非主节点故障时，系统仍能对外提供服务。

### Crash-recovery system model

系统由多个进程组成，进程之间传递消息来通信，每个进程都有健壮的存储设备，并且可能故障并恢复无限多次。一个quorum是超过半数的进程集合。进程有两个状态：up和down。

每对进程之间有一条双向通道，满足以下属性。1. 完整性（integrity），断言当且仅当进程Pi已经发送消息m时，进程Pj从Pi接收消息m。2. 前置性（prefix），意思是如果进程Pj接收到消息m，并且在Pi发送给Pj的消息序列里消息m'在m之前，那么Pj在接收m之前接收m'。

为了实现这些属性，Zookeeper使用TCP协议以及FIFO channel来通信。

### Expected properties

为了保证进程的一致，Zab满足了两个安全属性。另外为了允许多个未付的操作，我们需要primary顺序的属性。为了阐述这些属性，我们首先需要一些定义。

在Zookeeper的错误恢复模型中，如果primary进程故障，一个新的primary进程需要被选举出来。由于广播消息是完全有序的，因此我们一次最多需要一个活动的primary。

事务是状态的改变，由primary广播一个消息对（v, z）给其他进程，其中v是新的状态，z是一个叫做zxid的标识。

为了一致性，下列属性是必须的。

1. 完整性（Integrity），如果一些进程传递（v, z），然后一些进程广播（v, z）。
2. 全局顺序（Total order），如果一些进程在传递（v', z'）之前传递（v, z），那么任何传递（v', z'）的进程必须在此之前传递了（v, z）。
3. Agreement。如果一些进程Pi传递了（v, z），一些进程Pj传递了（v', z'），那么Pi也会传递（v', z'），Pj也会传递（v, z）。

下面给出primary顺序的属性。

1. Local primary order: 如果一个primary在广播（v', z'）之前广播（v, z），那么传递（v', z'）的进程必须在此之前传递（v, z）。
2. Global primary order: 如果一个primary Pi广播了（v, z），并且Pi之后的primary Pj广播了（v', z'）。如果一个进程传递了（v, z）和（v', z'），那么它肯定在传递（v', z'）之前传递（v, z）。
3. Primary Integrity: 如果一个primary Pi广播了（v, z），并且一些进程传递了Pi之前的primary Pj广播的（v', z'），那么Pi肯定在传递（v, z）之前传递（v', z'）。
