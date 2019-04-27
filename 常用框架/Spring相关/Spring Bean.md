# Spring Bean
## 概述
在 Spring 中，那些组成应用程序的主体及由 Spring IOC 容器所管理的对象，被称之为 bean。

简单地讲，bean 就是由 IOC 容器初始化、装配及管理的对象，除此之外，bean 就与应用程序中的其他对象没有什么区别了。
而 bean 的定义以及 bean 相互间的依赖关系将通过配置元数据来描述。

Spring 中的 Bean 默认都是单例的，Spring的单例是基于BeanFactory也就是Spring容器的，单例Bean在此容器内只有一个，Java的单例是基于 JVM，每个 JVM 内只有一个实例。

## bean的作用域
Spring Framework支持五种作用域，分别阐述如下表。

|类别|说明|
|---|---|
|singleton|在Spring IOC容器中仅存在一个Bean实例，Bean以单例模式存在，默认值|
|prototype|每次从容器中获取Bean时，都会返回一个新的实例|
|request|每次HTTP请求都会创建一个新的Bean|
|session|同一个HTTP session都会创建一个新的Bean|
|globalSession|在一个全局的HTTP Session中，一个bean定义对应一个实例，仅在使用portlet context的时候有效|

五种作用域中，request、session 和 global session 三种作用域仅在基于web的应用中使用（不必关心你所采用的是什么web应用框架），
只能用在基于 web 的 Spring ApplicationContext 环境。

## bean的生命周期
Spring Bean是Spring应用中最最重要的部分了。所以来看看Spring容器在初始化一个bean的时候会做那些事情，顺序是怎样的，在容器关闭的时候，又会做哪些事情。

> spring版本：4.2.3.RELEASE 鉴于Spring源码是用gradle构建的，
运行beanLifeCycle模块下的junit test即可在控制台看到如下输出，可以清楚了解Spring容器在创建，初始化和销毁Bean的时候依次做了那些事情。
```
Spring容器初始化
=====================================
调用GiraffeService无参构造函数
GiraffeService中利用set方法设置属性值
调用setBeanName:: Bean Name defined in context=giraffeService
调用setBeanClassLoader,ClassLoader Name = sun.misc.Launcher$AppClassLoader
调用setBeanFactory,setBeanFactory:: giraffe bean singleton=true
调用setEnvironment
调用setResourceLoader:: Resource File Name=spring-beans.xml
调用setApplicationEventPublisher
调用setApplicationContext:: Bean Definition Names=[giraffeService, org.springframework.context.annotation.CommonAnnotationBeanPostProcessor#0, com.giraffe.spring.service.GiraffeServicePostProcessor#0]
执行BeanPostProcessor的postProcessBeforeInitialization方法,beanName=giraffeService
调用PostConstruct注解标注的方法
执行InitializingBean接口的afterPropertiesSet方法
执行配置的init-method
执行BeanPostProcessor的postProcessAfterInitialization方法,beanName=giraffeService
Spring容器初始化完毕
=====================================
从容器中获取Bean
giraffe Name=李光洙
=====================================
调用preDestroy注解标注的方法
执行DisposableBean接口的destroy方法
执行配置的destroy-method
Spring容器关闭
```
### initialization 和 destroy
有时我们需要在Bean属性值set好之后和Bean销毁之前做一些事情，比如检查Bean中某个属性是否被正常的设置好值了。
Spring框架提供了多种方法让我们可以在Spring Bean的生命周期中执行initialization和pre-destroy方法。
1. 实现InitializingBean和DisposableBean接口
   
   这两个接口都只包含一个方法。通过实现InitializingBean接口的afterPropertiesSet()方法可以在Bean属性值设置好之后做一些操作，实现DisposableBean接口的destroy()方法可以在销毁Bean之前做一些操作。
   ```
   public class GiraffeService implements InitializingBean,DisposableBean {
       @Override
       public void afterPropertiesSet() throws Exception {
           System.out.println("执行InitializingBean接口的afterPropertiesSet方法");
       }
       @Override
       public void destroy() throws Exception {
           System.out.println("执行DisposableBean接口的destroy方法");
       }
   }
   ```
   这种方法比较简单，但是不建议使用。因为这样会将Bean的实现和Spring框架耦合在一起。
2. 在bean的配置文件中指定init-method和destroy-method方法
   
   Spring允许我们创建自己的 init 方法和 destroy 方法，只要在 Bean 的配置文件中指定 init-method 和 destroy-method 的值就可以在 Bean 初始化时和销毁之前执行一些操作。
   ```
   public class GiraffeService {
       //通过<bean>的destroy-method属性指定的销毁方法
       public void destroyMethod() throws Exception {
           System.out.println("执行配置的destroy-method");
       }
       //通过<bean>的init-method属性指定的初始化方法
       public void initMethod() throws Exception {
           System.out.println("执行配置的init-method");
       }
   }
   
   <bean name="giraffeService" class="com.giraffe.spring.service.GiraffeService" 
       init-method="initMethod" destroy-method="destroyMethod">
   </bean>
   ```
   需要注意的是自定义的init-method和post-method方法可以抛异常但是不能有参数。
3. 使用@PostConstruct和@PreDestroy注解

   除了xml配置的方式，Spring 也支持用 @PostConstruct和 @PreDestroy注解来指定 init 和 destroy 方法。这两个注解均在javax.annotation 包中。
   为了注解可以生效，需要在配置文件中定义org.springframework.context.annotation.CommonAnnotationBeanPostProcessor或context:annotation-config
   ```
   public class GiraffeService {
       @PostConstruct
       public void initPostConstruct(){
           System.out.println("执行PostConstruct注解标注的方法");
       }
       @PreDestroy
       public void preDestroy(){
           System.out.println("执行preDestroy注解标注的方法");
       }
   }
   
   <bean class="org.springframework.context.annotation.CommonAnnotationBeanPostProcessor" />
   ```
   
### 实现*Aware接口在Bean中使用Spring框架的一些对象
有些时候我们需要在 Bean 的初始化中使用 Spring 框架自身的一些对象来执行一些操作，比如获取 ServletContext 的一些参数，获取 ApplicaitionContext 中的 BeanDefinition 的名字，获取 Bean 在容器中的名字等等。
为了让 Bean 可以获取到框架自身的一些对象，Spring 提供了一组名为*Aware的接口。

这些接口均继承于org.springframework.beans.factory.Aware标记接口，并提供一个将由 Bean 实现的set*方法，Spring通过基于setter的依赖注入方式使相应的对象可以被Bean使用。
* ApplicationContextAware: 获得ApplicationContext对象,可以用来获取所有Bean definition的名字
* BeanFactoryAware:获得BeanFactory对象，可以用来检测Bean的作用域
* BeanNameAware:获得Bean在配置文件中定义的名字
* ResourceLoaderAware:获得ResourceLoader对象，可以获得classpath中某个文件
* ServletContextAware:在一个MVC应用中可以获取ServletContext对象，可以读取context中的参数
* ServletConfigAware:在一个MVC应用中可以获取ServletConfig对象，可以读取config中的参数
```
public class GiraffeService implements   ApplicationContextAware,
        ApplicationEventPublisherAware, BeanClassLoaderAware, BeanFactoryAware,
        BeanNameAware, EnvironmentAware, ImportAware, ResourceLoaderAware{
         @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        System.out.println("执行setBeanClassLoader,ClassLoader Name = " + classLoader.getClass().getName());
    }
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        System.out.println("执行setBeanFactory,setBeanFactory:: giraffe bean singleton=" +  beanFactory.isSingleton("giraffeService"));
    }
    @Override
    public void setBeanName(String s) {
        System.out.println("执行setBeanName:: Bean Name defined in context=" + s);
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        System.out.println("执行setApplicationContext:: Bean Definition Names="
                + Arrays.toString(applicationContext.getBeanDefinitionNames()));
    }
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        System.out.println("执行setApplicationEventPublisher");
    }
    @Override
    public void setEnvironment(Environment environment) {
        System.out.println("执行setEnvironment");
    }
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource("classpath:spring-beans.xml");
        System.out.println("执行setResourceLoader:: Resource File Name=" + resource.getFilename());
    }
    @Override
    public void setImportMetadata(AnnotationMetadata annotationMetadata) {
        System.out.println("执行setImportMetadata");
    }
}
```
### BeanPostProcessor
上面的*Aware接口是针对某个实现这些接口的Bean定制初始化的过程， Spring同样可以针对容器中的所有Bean，或者某些Bean定制初始化过程，只需提供一个实现BeanPostProcessor接口的类即可。
该接口中包含两个方法，postProcessBeforeInitialization和postProcessAfterInitialization。 
postProcessBeforeInitialization方法会在容器中的Bean初始化之前执行， postProcessAfterInitialization方法在容器中的Bean初始化之后执行。
```
public class CustomerBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("执行BeanPostProcessor的postProcessBeforeInitialization方法,beanName=" + beanName);
        return bean;
    }
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("执行BeanPostProcessor的postProcessAfterInitialization方法,beanName=" + beanName);
        return bean;
    }
}

<!-- 要将BeanPostProcessor的Bean像其他Bean一样定义在配置文件中 -->
<bean class="com.giraffe.spring.service.CustomerBeanPostProcessor"/>
```
### 总结
所以。结合第一节控制台输出的内容，Spring Bean的生命周期是这样的：
* Bean容器找到配置文件中 Spring Bean 的定义。
* Bean容器利用Java Reflection API创建一个Bean的实例。
* 如果涉及到一些属性值 利用set方法设置一些属性值。
* 如果Bean实现了BeanNameAware接口，调用setBeanName()方法，传入Bean的名字。
* 如果Bean实现了BeanClassLoaderAware接口，调用setBeanClassLoader()方法，传入ClassLoader对象的实例。
* 如果Bean实现了BeanFactoryAware接口，调用setBeanClassLoader()方法，传入ClassLoader对象的实例。
* 与上面的类似，如果实现了其他*Aware接口，就调用相应的方法。
* 如果有和加载这个Bean的Spring容器相关的BeanPostProcessor对象，执行postProcessBeforeInitialization()方法
* 如果Bean实现了InitializingBean接口，执行afterPropertiesSet()方法。
* 如果Bean在配置文件中的定义包含init-method属性，执行指定的方法。
* 如果有和加载这个Bean的Spring容器相关的BeanPostProcessor对象，执行postProcessAfterInitialization()方法
* 当要销毁Bean的时候，如果Bean实现了DisposableBean接口，执行destroy()方法。
* 当要销毁Bean的时候，如果Bean在配置文件中的定义包含destroy-method属性，执行指定的方法。

![Spring Bean的生命周期](./doc.img/spring.bean.lifecycle.png)

### 单例对象的管理
当scope=”singleton”，即默认情况下，会在启动容器时（即实例化容器时）时实例化。但我们可以指定Bean节点的lazy-init=”true”来延迟初始化bean，
这时候，只有在第一次获取bean时才会初始化bean，即第一次请求该bean时才初始化。如下配置：
```
<bean id="ServiceImpl" class="cn.csdn.service.ServiceImpl" lazy-init="true"/> 
```
如果想对所有的默认单例bean都应用延迟初始化，可以在根节点beans设置default-lazy-init属性为true
```
<beans default-lazy-init="true" …>
```
默认情况下，Spring 在读取 xml 文件的时候，就会创建对象。在创建对象的时候先调用构造器，然后调用 init-method 属性值中所指定的方法。
对象在被销毁的时候，会调用 destroy-method 属性值中所指定的方法（例如调用Container.destroy()方法的时候）。写一个测试类
```
public class LifeBean {
    private String name;  

    public LifeBean(){  
        System.out.println("LifeBean()构造函数");  
    }  
    public String getName() {  
        return name;  
    }  

    public void setName(String name) {  
        System.out.println("setName()");  
        this.name = name;  
    }  

    public void init(){  
        System.out.println("this is init of lifeBean");  
    }  

    public void destory(){  
        System.out.println("this is destory of lifeBean " + this);  
    }  
}
```
配置如下
```
<bean id="life_singleton" class="com.bean.LifeBean" scope="singleton" 
        init-method="init" destroy-method="destory" lazy-init="true"/>
```
测试代码
```
public class App {

    public static void main(String[] args) {
        AbstractApplicationContext context = new ClassPathXmlApplicationContext("classpath:application.xml");
        LifeBean life = (LifeBean) context.getBean("life_singleton");
        System.out.println(life);
        context.close();
    }
}
```
运行结果
```
LifeBean()构造函数
this is init of lifeBean
com.bean.LifeBean@573f2bb1
...
this is destory of lifeBean com.bean.LifeBean@573f2bb1
```

### 单非例对象的管理
当scope=”prototype”时，容器也会延迟初始化 bean，Spring 读取xml文件的时候，并不会立刻创建对象，而是在第一次请求该 bean 时才初始化（如调用getBean方法时）。
在第一次请求每一个 prototype 的bean 时，Spring容器都会调用其构造器创建这个对象，然后调用init-method属性值中所指定的方法。

对象销毁的时候，Spring 容器不会帮我们调用任何方法，因为是非单例，这个类型的对象有很多个，Spring容器一旦把这个对象交给你之后，就不再管理这个对象了。

```
<bean id="life_prototype" class="com.bean.LifeBean" scope="prototype" 
        init-method="init" destroy-method="destory" lazy-init="true"/>
```
测试代码
```
public class App {

    public static void main(String[] args) {
        AbstractApplicationContext context = new ClassPathXmlApplicationContext("classpath:application.xml");
        LifeBean life = (LifeBean) context.getBean("life_prototype");
        System.out.println(life);
        context.close();
    }
}
```
运行结果
```
LifeBean()构造函数
this is init of lifeBean
com.bean.LifeBean@68c4039c
```
可以发现，对于作用域为 prototype 的 bean ，其destroy方法并没有被调用。如果 bean 的 scope 设为prototype时，当容器关闭时，destroy 方法不会被调用。

对于 prototype 作用域的 bean，有一点非常重要，那就是 Spring不能对一个 prototype bean 的整个生命周期负责：容器在初始化、配置、装饰或者是装配完一个prototype实例后，将它交给客户端，随后就对该prototype实例不闻不问了。

不管何种作用域，容器都会调用所有对象的初始化生命周期回调方法。但对prototype而言，任何配置好的析构生命周期回调方法都将不会被调用。
清除prototype作用域的对象并释放任何prototype bean所持有的昂贵资源，都是客户端代码的职责
（让Spring容器释放被prototype作用域bean占用资源的一种可行方式是，通过使用bean的后置处理器，该处理器持有要被清除的bean的引用）。