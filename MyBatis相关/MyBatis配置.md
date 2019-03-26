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

