# MyBatis配置
MyBatis配置文件并不复杂，
```
<?xml version="1.0" encoding="UTF-8" ?>
<configuration> <!--配置-->
    <properties/> <!--属性-->
    <typeAliases/> <!--别名-->
    <typeHandlers/> <!--类型处理器-->
    <objectFactory/> <!--对象工厂-->
    <plugins/> <!--插件-->
    <environments> <!--配置环境-->
        <environment> <!--环境变量-->
            <transactionManager/> <!--事务管理器-->
            <dataSource/> <!--数据源-->
        </environment>
    </environments>
    <databaseIdProvider/> <!--数据库厂商标识-->
    <mappers/> <!--映射器-->
</configuration>
```
但是需要注意的是，MyBatis 配置项的顺序不能颠倒。如果颠倒了它们的顺序，那么在 MyBatis 启动阶段就会发生异常，导致程序无法运行。
## properties属性
properties 属性可以给系统配置一些运行参数， 可以放在 XML 文件或者 properties 文件中，而不是放在 Java 编码中， 这样的好处在于方便参数修改，而不会引起代码的重新编译。
一般而言， MyBatis 提供了 3 种方式让我们使用 properties 
* property 子元素
* properties 文件 
* 程序代码传递

### property 子元素
```
<properties>
  <property name="driver" value="com.mysql.jdbc.Driver"/>
  <property name="url" value="jdbc:mysql://localhost:3306/ssm"/>
  <property name="user" value="root"/>
  <property name="password" value="123456"/>
</properties>
<typeAliases> <!--别名-->
    <typeAlias alias="role" type="com.learn.ssm.pojo.Role"/>
</typeAliases>
<!--数据库环境-->
<environments default="development">
    <environment id="development ">
        <transactionManager type="JDBC"/>
        <dataSource type="POOLED">
            <property name="driver" value="${driver}"/>
            <property name="url" value="${url}"/>
            <property name="user" value="${user}"/>
            <property name="password" value="${password}"/>
        </dataSource>
    </environment>
</environments>
```
这里使用了元素＜properties＞下 的子元素＜property＞定义。
### 使用 properties 文件
创建一个文件 jdbc.properties 放到 resource 的路径下
```
driver=com.mysql.jdbc.Driver
url=jdbc:mysql://localhost:3306/ssm
username=root
password=123456
```
在 MyBatis 中通过<properties>的属性 resource 来引入 properties 文件。
```
<properties resource="jdbc.properties"/>
```
### 使用程序传递万式传递参数
有时候生产环境中， 数据库的用户密码是对开发人员和其他人员保密的。运维人员为了保密， 一般都需要把用户和密码经过加密成为密文后，配置到 properties 文件中。
那么这时候就需要解密了，现在假设系统己经为提供了这样的一个 CodeUtils.decode(str)进行解密，那么我们在创建 SqlSessionFactory前，
就需要把用户名和密码解密， 然后把解密后的字符串重置到 properties 属性中
```
String resource ＝ "mybatis-config.xml";
InputStream inputStream;
InputStream in ＝ Resources.getResourceAsStream("jdbc.properties");
Properties props ＝new Properties();
props.load(in);
String username = props.getProperty("database.username");
String password = props.getProperty("database.password");
//解密用户和密码，并在属性中重置
props.put ("database.username", CodeUtils.decode(username));
props.put("database .password", CodeUtils.decode(password));
inputStream = Resources.getResourceAsStream(resource);
//使用程序传递的方式覆盖原有的 properties 属性参数
SqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, props);
```
这 3 种方式是有优先级的， 最优先的是使用程序传递的方式，其次是使用 properties 文件的方式， 最后是
使用 property 子元素的方式， MyBatis 会根据优先级来覆盖原先配置的属性值。

## settings设置
这是 MyBatis 中极为重要的调整设置，它们会改变 MyBatis 的运行时行为。

一个配置完整的 settings 元素的示例如下：
```
<settings>
  <setting name="cacheEnabled" value="true"/>
  <setting name="lazyLoadingEnabled" value="true"/>
  <setting name="multipleResultSetsEnabled" value="true"/>
  <setting name="useColumnLabel" value="true"/>
  <setting name="useGeneratedKeys" value="false"/>
  <setting name="autoMappingBehavior" value="PARTIAL"/>
  <setting name="autoMappingUnknownColumnBehavior" value="WARNING"/>
  <setting name="defaultExecutorType" value="SIMPLE"/>
  <setting name="defaultStatementTimeout" value="25"/>
  <setting name="defaultFetchSize" value="100"/>
  <setting name="safeRowBoundsEnabled" value="false"/>
  <setting name="mapUnderscoreToCamelCase" value="false"/>
  <setting name="localCacheScope" value="SESSION"/>
  <setting name="jdbcTypeForNull" value="OTHER"/>
  <setting name="lazyLoadTriggerMethods" value="equals,clone,hashCode,toString"/>
</settings>
```
## typeAliases类型别名
类型别名是为 Java 类型设置一个短的名字。 它只和 XML 配置有关，存在的意义仅在于用来减少类完全限定名的冗余。
```
<typeAliases>
  <typeAlias alias="Author" type="domain.blog.Author"/>
  <typeAlias alias="Blog" type="domain.blog.Blog"/>
  <typeAlias alias="Comment" type="domain.blog.Comment"/>
  <typeAlias alias="Post" type="domain.blog.Post"/>
  <typeAlias alias="Section" type="domain.blog.Section"/>
  <typeAlias alias="Tag" type="domain.blog.Tag"/>
</typeAliases>
```
也可以指定一个包名，MyBatis 会在包名下面搜索需要的 Java Bean
```
<typeAliases>
  <package name="domain.blog"/>
</typeAliases>
```
每一个在包 domain.blog 中的 Java Bean，在没有注解的情况下，会使用 Bean 的首字母小写的非限定类名来作为它的别名。 
比如 domain.blog.Author 的别名为 author；若有注解，则别名为其注解值。
```
@Alias("author")
public class Author {
    ...
}
```
在 MyBatis 的初始化过程中，系统自动初始化了 一些别名。
## typeHandlers类型处理器
在 JDBC 中， 需要在 PreparedStatement 对象中设置那些已经预编译过的 SQL 语旬的参数。
执行 SQL 后，会通过 ResultSet 对象获取得到数据库的数据，而这些 MyBatis 是根据数据的类型通过 typeHandler 来实现的。

无论是 MyBatis 在预处理语句（PreparedStatement）中设置一个参数时，还是从结果集中取出一个值时， 都会用类型处理器将获取的值以合适的方式转换成 Java 类型。
也可以自定义typeHandlers。

### 系统 typeHandler
在 MyBatis 中 typeHandler 都要实现接口 org.apache.ibatis.type.TypeHandler， 首先让我们先看看这个接口的定义
```
public interface TypeHandler<T> {
    // 使用 typeHandler 通过 PreparedStatement 对象进行设置 SQL 参数
    void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

    T getResult(ResultSet rs, String columnName) throws SQLException;

    T getResult(ResultSet rs, int columnIndex) throws SQLException;

    T getResult(CallableStatement cs, int columnIndex) throws SQLException;

}
```
MyBatis 系统的 typeHandler 都继承了 org.apache.ibatis.type.BaseTypeHandler。MyBatis 使用最多的 typeHandler 之一 StringTypeHandler。它用于字符串转换，
```
public class StringTypeHandler extends BaseTypeHandler<String> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, parameter);
  }

  @Override
  public String getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return rs.getString(columnName);
  }

  @Override
  public String getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return rs.getString(columnIndex);
  }

  @Override
  public String getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return cs.getString(columnIndex);
  }
}
```
在 MyBatis 中采用 org.apache.ibatis.type.TypeHandlerRegistry 类对象的 register 方法进行注册，
```
public TypeHandlerRegistry() {
    register(Boolean.class, new BooleanTypeHandler());
    register(boolean.class, new BooleanTypeHandler());
    register(JdbcType.BOOLEAN, new BooleanTypeHandler());
    register(JdbcType.BIT, new BooleanTypeHandler());
    ...
```
这样就实现了用代码的形式注册 typeHandler， 注意， 自定义的 typeHandler 一般不会使用代码注册， 而是通过配置或扫描。

### 自定义 typeHandler
在大部分的场景下， MyBatis 的 typeHandler 就能应付一般的场景，但是有时候不够用。比如使用枚举的时候，枚举有特殊的转化规则，这个时候需要自定义 typeHandler 进行处理它。

自定义 typeHandler 就需要去实现接口 typeHandler，或者继承 BaseTypeHandler。 
```
public class MyTypeHandler extends BaseTypeHandler<String> {
    Logger logger = Logger.getLogger(MyTypeHandler.class.getName());

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        logger.info("设置 string 参数 [" + parameter + "]");
        ps.setString(i, parameter);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String result = rs.getString(columnName);
        logger.info("读取 string 参数 [" + result + "]");
        return result;
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String result = rs.getString(columnIndex);
        logger.info("读取 string 参数 [" + result + "]");
        return result;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String result = cs.getString(columnIndex);
        logger.info("读取 string 参数 [" + result + "]");
        return result;
    }
}
```
还需要配置一下
```
<typeHandlers>
    <typeHandler handler="com.learn.ssm.MyTypeHandler"/>
</typeHandlers>
```
配置完成后系统才会读取它， 这样注册后， 当 jdbcType 和 javaType 能与 MyTypeHandler 对应的时候， 它就会启动 MyTypeHandler。有时候还可以显式启用 typeHandler。

有时候由于枚举类型很多，系统需要的 typeHandler 也会很多，如果采用配置也会很麻烦，这个时候可以考虑使用包扫描的形式。
```
<typeHandlers>
  <package name="com.learn.ssm"/>
</typeHandlers>
```
注意在使用自动发现功能的时候，只能通过注解方式来指定 JDBC 的类型。
```
@MappedTypes(String.class)
@MappedjdbcTypes(jdbcType.VARCHAR)
public class MyTypeHandler implements TypeHandler<String> {
    ...
}
```
### 枚举 TypeHandler
在绝大多数情况下， typeHandler 因为枚举而使用， MyBatis 已经定义了两个类作为枚举类型的支持，这两个类分别是：
* EnumOrdinalTypeHandler
* EnumTypeHandler

EnumOrdinalTypeHandler 是按枚举数据下标索引进行匹配的。也是枚举类型的默认转换类，它要求数据库返回一个整数作为下标，它会根据下标找到对应的枚举类型。

EnumTypeHandler 会把使用的名称转化为对应的枚举，比如它会根据数据库返回的字符串"MALE"，进行 Enum.valueOf(SexEnum.class，"MALE")。

很多时候我们希望使用自定义的 typeHandler 。

## ObjectFactory 对象工厂
MyBatis 每次创建结果对象的新实例时，它都会使用一个对象工厂（ObjectFactory）实例来完成。
在默认的情况下， MyBatis 会使用其定义的对象工厂——DefaultObjectFactory (org.apache.ibatis.reflection.factory.DefaultObjectFactory) 来完成对应的工作。

如果想覆盖对象工厂的默认行为，则可以通过创建自己的对象工厂来实现
```
public class MyObjectFactory extends DefaultObjectFactory {

    private static final long serialVersionUID = -3215697574350565442L;

    Logger log = Logger.getLogger(MyObjectFactory.class.getName());

    private Object temp = null;

    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);
        log.info("初始化参数: [" + properties.toString() + "]");
    }

    @Override
    public <T> T create(Class<T> type) {
        T result = super.create(type);
        log.info("创建对象 " + result.toString());
        return result;
    }

    @Override
    public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        T result = super.create(type, constructorArgTypes, constructorArgs);
        log.info("创建对象 " + result.toString());
        temp = result;
        return result;
    }

    @Override
    public <T> boolean isCollection(Class<T> type) {
        return super.isCollection(type);
    }
}
```
然后对它进行配置
```
<objectFactory type="com.learn.ssm.MyObjectFactory">
    <property name="prop1" value="value1"/>
</objectFactory>
```
这样 MyBatis 就会采用配置的 MyObjectFactory 来生成结果集对象。

## environments 运行环境
在 MyBatis 中，运行环境主要的作用是配置数据库信息，它可以配置多个数据库，一般而言只需要配置其中的一个就可以了。它下面又分为事务管理器(transactionManager)和数据源(dataSource)。
###  transactionManager 事务管理器
在 MyBatis 中， transactionManager 提供了两个实现类，它需要实现接口 Transaction(org.apache.ibatis.transaction.Transaction)
```
public interface Transaction {

  /**
   * Retrieve inner database connection
   * @return DataBase connection
   * @throws SQLException
   */
  Connection getConnection() throws SQLException;

  /**
   * Commit inner database connection.
   * @throws SQLException
   */
  void commit() throws SQLException;

  /**
   * Rollback inner database connection.
   * @throws SQLException
   */
  void rollback() throws SQLException;

  /**
   * Close inner database connection.
   * @throws SQLException
   */
  void close() throws SQLException;

  /**
   * Get transaction timeout if set
   * @throws SQLException
   */
  Integer getTimeout() throws SQLException;
  
}
```
MyBatis 为 Transaction 提供了两个实现类： JdbcTransaction 和 ManagedTransaction，对应着两种工厂： JdbcTransactionFactory 和 ManagedTransactionFactory ， 
这个工厂需要实现 TransactionFactory 接口，通过它们会生成对应的 Transaction 对象。于是可以把事务管理器配置成为以下两种方式：
```
<transactionManager type="JDBC"/>
<transactionManager type="MANAGED"/>
```
* JDBC – 这个配置就是直接使用了 JDBC 的提交和回滚设置，它依赖于从数据源得到的连接来管理事务作用域。
* MANAGED – 这个配置几乎没做什么。它从来不提交或回滚一个连接，而是让容器来管理事务的整个生命周期（比如 JEE 应用服务器的上下文）。 默认情况下它会关闭连接，然而一些容器并不希望这样，因此需要将 closeConnection 属性设置为 false 来阻止它默认的关闭行为。

**提示：** 如果你正在使用 Spring + MyBatis，则没有必要配置事务管理器， 因为 Spring 模块会使用自带的管理器来覆盖前面的配置。
###  dataSource 数据源
dataSource 元素使用标准的 JDBC 数据源接口来配置 JDBC 连接对象的资源。有三种内建的数据源类型（也就是 type=”[UNPOOLED|POOLED|JNDI]”）：
**UNPOOLED** – 这个数据源的实现只是每次被请求时打开和关闭连接。虽然有点慢，但对于在数据库连接可用性方面没有太高要求的简单应用程序来说，是一个很好的选择。
* driver – 这是 JDBC 驱动的 Java 类的完全限定名（并不是 JDBC 驱动中可能包含的数据源类）。
* url – 这是数据库的 JDBC URL 地址。
* username – 登录数据库的用户名。
* password – 登录数据库的密码。
* defaultTransactionIsolationLevel – 默认的连接事务隔离级别。

作为可选项，你也可以传递属性给数据库驱动。只需在属性名加上“driver.”前缀即可
```driver.encoding=UTF8```

**POOLED** – 这种数据源的实现利用“池”的概念将 JDBC 连接对象组织起来，避免了创建新的连接实例时所必需的初始化和认证时间。
除了上述提到 UNPOOLED 下的属性外，还有更多属性用来配置 POOLED 的数据源：
* poolMaximumActiveConnections – 在任意时间可以存在的活动（也就是正在使用）连接数量，默认值：10
* poolMaximumIdleConnections – 任意时间可能存在的空闲连接数。
* poolMaximumCheckoutTime – 在被强制返回之前，池中连接被检出（checked out）时间，默认值：20000 毫秒（即 20 秒）
* poolTimeToWait – 这是一个底层设置，如果获取连接花费了相当长的时间，连接池会打印状态日志并重新尝试获取一个连接（避免在误配置的情况下一直安静的失败），默认值：20000 毫秒（即 20 秒）。
* poolMaximumLocalBadConnectionTolerance – 这是一个关于坏连接容忍度的底层设置， 作用于每一个尝试从缓存池获取连接的线程。 如果这个线程获取到的是一个坏的连接，那么这个数据源允许这个线程尝试重新获取一个新的连接，但是这个重新尝试的次数不应该超过 poolMaximumIdleConnections 与 poolMaximumLocalBadConnectionTolerance 之和。 默认值：3 （新增于 3.4.5）
* poolPingQuery – 发送到数据库的侦测查询，用来检验连接是否正常工作并准备接受请求。默认是“NO PING QUERY SET”，这会导致多数数据库驱动失败时带有一个恰当的错误消息。
* poolPingEnabled – 是否启用侦测查询。若开启，需要设置 poolPingQuery 属性为一个可执行的 SQL 语句（最好是一个速度非常快的 SQL 语句），默认值：false。
* poolPingConnectionsNotUsedFor – 配置 poolPingQuery 的频率。可以被设置为和数据库连接超时时间一样，来避免不必要的侦测，默认值：0（即所有连接每一时刻都被侦测 — 当然仅当 poolPingEnabled 为 true 时适用）。

**JNDI** – 这个数据源的实现是为了能在如 EJB 或应用服务器这类容器中使用，容器可以集中或在外部配置数据源，然后放置一个 JNDI 上下文的引用。这种数据源配置只需要两个属性：
* initial_context – 这个属性用来在 InitialContext 中寻找上下文（即，initialContext.lookup(initial_context)）。这是个可选属性，如果忽略，那么将会直接从 InitialContext 中寻找 data_source 属性。
* data_source – 这是引用数据源实例位置的上下文的路径。提供了 initial_context 配置时会在其返回的上下文中进行查找，没有提供时则直接在 InitialContext 中查找。

和其他数据源配置类似，可以通过添加前缀“env.”直接把属性传递给初始上下文。比如：
```env.encoding=UTF8```

你可以通过实现接口 org.apache.ibatis.datasource.DataSourceFactory 来使用第三方数据源：
```
public interface DataSourceFactory {
  void setProperties(Properties props);
  DataSource getDataSource();
}
```
## databaseIdProvider 数据库厂商标识
...

## mappers 映射器
引入映射器有以下几种方法
```
<!-- 使用相对于类路径的资源引用 -->
<mappers>
  <mapper resource="org/mybatis/builder/AuthorMapper.xml"/>
  <mapper resource="org/mybatis/builder/BlogMapper.xml"/>
  <mapper resource="org/mybatis/builder/PostMapper.xml"/>
</mappers>
<!-- 使用完全限定资源定位符（URL） -->
<mappers>
  <mapper url="file:///var/mappers/AuthorMapper.xml"/>
  <mapper url="file:///var/mappers/BlogMapper.xml"/>
  <mapper url="file:///var/mappers/PostMapper.xml"/>
</mappers>
<!-- 使用映射器接口实现类的完全限定类名 -->
<mappers>
  <mapper class="org.mybatis.builder.AuthorMapper"/>
  <mapper class="org.mybatis.builder.BlogMapper"/>
  <mapper class="org.mybatis.builder.PostMapper"/>
</mappers>
<!-- 将包内的映射器接口实现全部注册为映射器 -->
<mappers>
  <package name="org.mybatis.builder"/>
</mappers>
```
这些配置会告诉了 MyBatis 去哪里找映射文件。

参考[http://www.mybatis.org/mybatis-3/zh/configuration.html](http://www.mybatis.org/mybatis-3/zh/configuration.html)