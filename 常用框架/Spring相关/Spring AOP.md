# Spring AOP实现原理
先看看代理模式的几种实现。
## 代理模式
代理模式的ＵＭＬ类图如下：

![代理模式的ＵＭＬ类图](./doc.img/spring.proxy.uml.png)

代理类实现了被代理类的接口，同时与被代理类是组合关系。

### 静态代理
接口类：
```
public interface Person {
    void speak();
}
```
真实实体类：
```
public class Actor implements Person {
    
    private String content;

    public Actor(String content) {
        this.content = content;
    }

    @Override
    public void speak() {
        System.out.println(content);
    }
}
```
代理类：
```
public class Agent implements Person {
    private Actor actor;
    private String before;
    private String after;

    public Agent(Actor actor, String before, String after) {
        this.actor = actor;
        this.before = before;
        this.after = after;
    }

    @Override
    public void speak() {
        //before speak
        System.out.println("Before actor speak, Agent say: " + before);
        //real speak
        this.actor.speak();
        //after speak
        System.out.println("After actor speak, Agent say: " + after);
    }
}
```
测试方法:
```
public class StaticProxy {

    public static void main(String[] args) {
        Actor actor = new Actor("I am a famous actor!");
        Agent agent = new Agent(actor, "Hello I am an agent.", "That's all!");
        agent.speak();
    }
}
```
结果：
```
Before actor speak, Agent say: Hello I am an agent.
I am a famous actor!
After actor speak, Agent say: That's all!
```

### JDK动态代理
首先介绍一下最核心的一个接口和一个方法：
首先是java.lang.reflect包里的InvocationHandler接口：
```

public interface InvocationHandler {
    /**
     * @param   proxy 被代理的类的实例
     * @param   method 调用被代理的类的方法
     * @param   args 该方法需要的参数
     */
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable;
}
```
使用方法首先是需要实现该接口，并且我们可以在invoke方法中调用被代理类的方法并获得返回值，自然也可以在调用该方法的前后去做一些额外的事情，从而实现动态代理，下面的例子会详细写到。

另外一个很重要的静态方法是java.lang.reflect包中的Proxy类的newProxyInstance方法：
```
/**
 * @param   loader 被代理的类的类加载器
 * @param   interfaces 被代理类的接口数组
 * @param   h 就是刚刚介绍的调用处理器类的对象实例
 */
public static Object newProxyInstance(ClassLoader loader, Class<?>[] interfaces, InvocationHandler h) throws IllegalArgumentException
```
该方法会返回一个被修改过的类的实例，从而可以自由的调用该实例的方法。下面是一个实际例子。
Fruit接口：
```
public interface Fruit {

    void show();
}
```
Apple实现Fruit接口：
```
public class Apple implements Fruit {
    @Override
    public void show() {
        System.out.println("<<<<show method is invoked");
    }
}
```
代理类Agent.java：
```
public class AppleAgent {

    static class MyHandler implements InvocationHandler {
        private Object proxy;

        public MyHandler(Object proxy) {
            this.proxy = proxy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println(">>>>before invoking");
            Object res = method.invoke(proxy, args);
            System.out.println(">>>>after invoking");
            return res;
        }
    }

    //返回一个被修改过的对象
    public static Object agent(Class interfaceClazz, Object proxy) {
        return Proxy.newProxyInstance(interfaceClazz.getClassLoader(), new Class[]{interfaceClazz}, new MyHandler(proxy));
    }
}
```
测试类：
```
public class ReflectTest {

    public static void main(String[] args) {
        //注意一定要返回接口，不能返回实现类否则会报错
        Fruit fruit = (Fruit) AppleAgent.agent(Fruit.class, new Apple());
        fruit.show();
    }
}
```
结果：
```
>>>>before invoking
<<<<show method is invoked
>>>>after invoking
```
可以看到对于不同的实现类来说，可以用同一个动态代理类来进行代理，实现了“一次编写到处代理”的效果。但是这种方法有个缺点，就是被代理的类一定要是实现了某个接口的，这很大程度限制了本方法的使用场景。下面还有另外一个使用了CGlib增强库的方法。


### CGLIB库的方法
CGlib是一个字节码增强库，为AOP等提供了底层支持，他的原理是对指定的目标类生成一个子类，并覆盖其中方法实现增强，但因为采用的是继承，所以不能对final修饰的类进行代理。 下面看看它是怎么实现动态代理的。
```
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;

public class CGlibAgent implements MethodInterceptor {

    private Object proxy;

    public Object getInstance(Object proxy) {
        this.proxy = proxy;
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(this.proxy.getClass());
        // 回调方法
        enhancer.setCallback(this);
        // 创建代理对象
        return enhancer.create();
    }
    //回调方法
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        System.out.println(">>>>before invoking");
        //真正调用
        Object ret = methodProxy.invokeSuper(o, objects);
        System.out.println(">>>>after invoking");
        return ret;
    }

    public static void main(String[] args) {
        CGlibAgent cGlibAgent = new CGlibAgent();
        Apple apple = (Apple) cGlibAgent.getInstance(new Apple());
        apple.show();
    }
}
```
结果：
```
>>>>before invoking
<<<<show method is invoked
>>>>after invoking
```
可以看到结果和JDK动态代理是一样的，但是可以直接对实现类进行操作而非接口，这样会有很大的便利。但是这里要求被代理类是可继承的，也就是不能被final修饰。

## AOP 简介
### 概念
* 切面(Aspect): 官方的抽象定义为“一个关注点的模块化，这个关注点可能会横切多个对象”
* 连接点(Joinpoint): 程序执行过程中的某一行为
* 通知(Advice): “切面”对于某个“连接点”所产生的动作
* 切入点(Pointcut): 匹配连接点的断言，在AOP中通知和一个切入点表达式关联
* 目标对象(Target Object): 被一个或者多个切面所通知的对象
* AOP代理(AOP Proxy): 在Spring AOP中有两种代理方式，JDK动态代理和CGLIB代理
### 通知(Advice)类型
* 前置通知(Before advice): 在某连接点（JoinPoint）之前执行的通知，但这个通知不能阻止连接点前的执行。ApplicationContext中在\<aop:aspect\>里面使用\<aop:before\>元素进行声明
* 后置通知(After advice): 当某连接点退出的时候执行的通知（不论是正常返回还是异常退出）。ApplicationContext中在\<aop:aspect\>里面使用\<aop:after\>元素进行声明
* 返回后通知(After return advice): 在某连接点正常完成后执行的通知，不包括抛出异常的情况。ApplicationContext中在\<aop:aspect\>里面使用\<after-returning\>元素进行声明
* 环绕通知(Around advice): 包围一个连接点的通知，类似Web中Servlet规范中的Filter的doFilter方法。可以在方法的调用前后完成自定义的行为，也可以选择不执行。
ApplicationContext中在\<aop:aspect\>里面使用\<aop:around\>元素进行声明
* 抛出异常后通知(After throwing advice): 在方法抛出异常退出时执行的通知。 ApplicationContext中在\<aop:aspect\>里面使用\<aop:after-throwing\>元素进行声明

切入点表达式 ：如execution(* com.spring.service.\*.\*(..))
### 特点
1. 降低模块之间的耦合度
2. 使系统容易扩展
3. 更好的代码复用

