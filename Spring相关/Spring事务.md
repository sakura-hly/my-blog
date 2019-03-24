# Spring 事务
事务有ACID四大特性，这里就不具体阐述了。
## Spring事务管理接口
* PlatformTransactionManager: （平台）事务管理器
* TransactionDefinition: 事务定义信息（事务隔离级别、传播行为、超时、只读、回滚规则）
* TransactionStatus: 事务运行状态

所谓事务管理，其实就是 **按照给定的事务规则来执行提交或者回滚操作**。
### PlatformTransactionManager接口介绍
Spring并不直接管理事务，而是提供了多种事务管理器，他们将事务管理的职责委托给Hibernate或者JTA等持久化机制所提供的相关平台框架的事务来实现。
通过org.springframework.transaction.PlatformTransactionManager这个接口，Spring为各个平台如JDBC、Hibernate等都提供了对应的事务管理器，但是具体的实现就是各个平台自己的事情了。

PlatformTransactionManager接口中定义了三个方法：
```
public interface PlatformTransactionManager {
    //根据指定的传播行为，返回当前活动的事务或创建一个新事务
    TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException;
    //使用事务目前的状态提交事务
    void commit(TransactionStatus status) throws TransactionException;
    //对执行的事务进行回滚
    void rollback(TransactionStatus status) throws TransactionException;
}
```
Spring中PlatformTransactionManager根据不同持久层框架所对应的接口实现类

|事务|说明|
|---|----|
|org.springframework.jdbc.datasource.DataSourceTransactionManager|使用Spring JDBC或iBatis进行持久化数据时使用|
|org.springframework.orm.hibernate5.HibernateTransactionManager|使用hibernate5进行持久化数据时使用|
|org.springframework.orm.jpa.JpaTransactionManager|使用JPA进行持久化数据时使用|
|org.springframework.transaction.jta.JtaTransactionManager|使用JTA来管理事务，在一个事务跨越多个资源时使用|

比如我们在使用JDBC或者iBatis（就是Mybatis）进行数据持久化操作时,我们的xml配置通常如下：
```
<!-- 事务管理器 -->
<bean id="transactionManager"
    class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    <!-- 数据源 -->
    <property name="dataSource" ref="dataSource" />
</bean>
```

### TransactionDefinition接口介绍
事务属性可以理解成事务的一些基本配置，描述了事务策略如何应用到方法上。事务属性包含了5个方面。
* 隔离级别
* 传播行为
* 回滚规则
* 是否只读
* 事务超时

代码如下
```
public interface TransactionDefinition {
    /*传播行为*/
    int PROPAGATION_REQUIRED = 0;
    int PROPAGATION_SUPPORTS = 1;
    int PROPAGATION_MANDATORY = 2;
    int PROPAGATION_REQUIRES_NEW = 3;
    int PROPAGATION_NOT_SUPPORTED = 4;
    int PROPAGATION_NEVER = 5;
    int PROPAGATION_NESTED = 6;
    /*隔离级别*/
    int ISOLATION_DEFAULT = -1;
    int ISOLATION_READ_UNCOMMITTED = 1;
    int ISOLATION_READ_COMMITTED = 2;
    int ISOLATION_REPEATABLE_READ = 4;
    int ISOLATION_SERIALIZABLE = 8;
    int TIMEOUT_DEFAULT = -1;

    int getPropagationBehavior();

    int getIsolationLevel();

    //返回事务超时时间
    int getTimeout();
    //返回是否优化为只读
    boolean isReadOnly();

    @Nullable
    String getName();
}
```

1. 事务隔离级别
   
   关于事务隔离级别可以参考我这篇，[事务隔离级别](./../数据存储相关/Innodb锁和事务.md)
   
2. 事务传播行为

   当事务方法被另一个事务方法调用时，必须指定事务应该如何传播。例如：方法可能继续在现有事务中运行，也可能开启一个新事务，并在自己的事务中运行。
   在TransactionDefinition定义中包括了如下几个表示传播行为的常量：
   
   **支持当前事务的情况**：
   * TransactionDefinition.PROPAGATION_REQUIRED： 如果当前存在事务，则加入该事务，如果不存在，则创建一个新事务
   * TransactionDefinition.PROPAGATION_SUPPORTS： 如果当前存在事务，则加入该事务，如果不存在，则以非事务的方式继续运行
   * TransactionDefinition.PROPAGATION_MANDATORY： 如果当前存在事务，则加入该事务；如果当前没有事务，则抛出异常。（mandatory：强制性）
   
   **不支持当前事务的情况**：
   * TransactionDefinition.PROPAGATION_REQUIRES_NEW： 创建一个新事务，如果当前存在事务，则将当前事务挂起
   * TransactionDefinition.PROPAGATION_NOT_SUPPORTED： 以非事务方式运行，如果当前存在事务，则将当前事务挂起
   * TransactionDefinition.PROPAGATION_NEVER： 以非事务方式运行，如果当前存在事务，则抛出异常
   
   最后一种
   * TransactionDefinition.PROPAGATION_NESTED： 如果当前存在事务，则创建一个事务作为当前事务的嵌套事务来运行；如果当前没有事务，则该取值等价于TransactionDefinition.PROPAGATION_REQUIRED。
   
   前面的六种事务传播行为是 Spring 从 EJB 中引入的，他们共享相同的概念。
   
   而 PROPAGATION_NESTED 是 Spring 所特有的。以 PROPAGATION_NESTED 启动的事务内嵌于外部事务中（如果存在外部事务的话），此时，内嵌事务并不是一个独立的事务，
   它依赖于外部事务的存在，只有通过外部的事务提交，才能引起内部事务的提交，嵌套的子事务不能单独提交。外部事务的回滚也会导致嵌套子事务的回滚。
   
3. 事务超时属性(一个事务允许执行的最长时间)

   所谓事务超时，就是指一个事务所允许执行的最长时间，如果超过该时间限制但事务还没有完成，则自动回滚事务。在 TransactionDefinition 中以 int 的值来表示超时时间，其单位是秒。
   
4. 事务只读属性(对事物资源是否执行只读操作)
   
   事务的只读属性是指，对事务性资源进行只读操作或者是读写操作。
   
   所谓事务性资源就是指那些被事务管理的资源，比如数据源、 JMS 资源，以及自定义的事务性资源等等。
   如果确定只对事务性资源进行只读操作，那么我们可以将事务标志为只读的，以提高事务处理的性能。在 TransactionDefinition 中以 boolean 类型来表示该事务是否只读。

5. 回滚规则（定义事务回滚规则）

   回滚规则定义了哪些异常会导致事务回滚而哪些不会。
   
   默认情况下，事务只有遇到运行期异常时才会回滚，而在遇到检查型异常时不会回滚（这一行为与EJB的回滚行为是一致的）。
   但是你可以声明事务在遇到特定的检查型异常时像遇到运行期异常那样回滚。同样，你还可以声明事务遇到特定的异常不回滚，即使这些异常是运行期异常。
   
### TransactionStatus接口介绍
TransactionStatus接口用来记录事务的状态， 该接口定义了一组方法,用来获取或判断事务的相应状态信息。

PlatformTransactionManager.getTransaction() 方法返回一个 TransactionStatus 对象。返回的TransactionStatus 对象可能代表一个新的或已经存在的事务。

TransactionStatus接口：
```
public interface TransactionStatus extends SavepointManager, Flushable {
    //是否是新的事物
    boolean isNewTransaction();
    //是否有恢复点
    boolean hasSavepoint();
    //设置为只回滚
    void setRollbackOnly();
    //是否为只回滚
    boolean isRollbackOnly();
    //刷新事务
    void flush();
    //是否已完成
    boolean isCompleted();
}
```
