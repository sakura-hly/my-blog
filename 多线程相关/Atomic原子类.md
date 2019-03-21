# Atomic原子类
## Atomic 原子类介绍
Atomic 是指一个操作是不可中断的。即使是在多个线程一起执行的时候，一个操作一旦开始，就不会被其他线程干扰。
所谓原子类说简单点就是具有原子/原子操作特征的类。

java的原子类都存放在java.util.concurrent.atomic下。根据操作的数据类型，可以将JUC包中的原子类分为4类

1. 基本类型
   
   使用原子的方式更新基本类型
   * AtomicInteger：整型原子类
   * AtomicLong：长整型原子类
   * AtomicBoolean ：布尔型原子类
   
2. 数组类型

   使用原子的方式更新数组里的某个元素
   * AtomicIntegerArray：整型数组原子类
   * AtomicLongArray：长整型数组原子类
   * AtomicReferenceArray ：引用类型数组原子类
   
3. 引用类型

   * AtomicReference：引用类型原子类
   * AtomicStampedReference：原子更新引用类型里的字段原子类
   * AtomicMarkableReference ：原子更新带有标记位的引用类型

4. 对象的属性修改类型

   * AtomicIntegerFieldUpdater：原子更新整型字段的更新器
   * AtomicLongFieldUpdater：原子更新长整型字段的更新器
   * AtomicStampedReference ：原子更新带有版本号的引用类型。
   该类将整数值与引用关联起来，可用于解决原子的更新数据和数据的版本号，可以解决使用 CAS 进行原子更新时可能出现的 ABA 问题。
   
## 基本类型原子类
### 基本类型原子类介绍
* AtomicInteger：整型原子类
* AtomicLong：长整型原子类
* AtomicBoolean ：布尔型原子类

上面三个类提供的方法几乎相同，所以我们这里以 AtomicInteger 为例子来介绍。

常用方法:
```
    public final int get()//Gets the current value.
    public final void lazySet(int newValue)//Eventually sets to the given value.
    public final int getAndSet(int newValue)//Atomically sets to the given value and returns the old value.
    public final boolean compareAndSet(int expect, int update)//Atomically sets the value to the given updated value if the current value == the expected value.
    public final int getAndIncrement()//Atomically increments by one the current value.
    public final int getAndDecrement()//Atomically decrements by one the current value.
    public final int getAndAdd(int delta)//Atomically adds the given value to the current value.
```
###  AtomicInteger 使用demo
```
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerDemo {
    private static volatile AtomicInteger n = new AtomicInteger(0);

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int i1 = 0; i1 < 10; i1++) {
                    n.incrementAndGet();
                }
            }).start();
        }

        while (Thread.activeCount() > 2) {
            //System.out.println(Thread.activeCount());
            /**
             * IDEA:
             * java.lang.ThreadGroup[name=main,maxpri=10]
             * Thread[main,5,main]
             * Thread[Monitor Ctrl-Break,5,main]
             */
            Thread.yield();
        }
        Thread.currentThread().getThreadGroup().list();
        System.out.println(n.get());
    }
}
```
多线程环境使用原子类，不需要加锁，也可以实现线程安全。
### AtomicInteger 线程安全原理简单分析
关键源码：
```
// setup to use Unsafe.compareAndSwapInt for updates
private static final Unsafe unsafe = Unsafe.getUnsafe();
private static final long valueOffset;

static {
    try {
        valueOffset = unsafe.objectFieldOffset
            (AtomicInteger.class.getDeclaredField("value"));
    } catch (Exception ex) { throw new Error(ex); }
}

private volatile int value;
```
AtomicInteger 类主要利用 CAS (compare and swap) + volatile 和 native 方法来保证原子操作，从而避免 synchronized 的高开销，执行效率大为提升。

CAS的原理是拿期望的值和原本的一个值作比较，如果相同则更新成新的值。UnSafe 类的 objectFieldOffset() 方法是一个本地方法，这个方法是用来拿到“原来的值”的内存地址，返回值是 valueOffset。
另外 value 是一个volatile变量，在内存中可见，因此 JVM 可以保证任何时刻任何线程总能拿到该变量的最新值。

## 数组类型原子类
### 数组类型原子类介绍
* AtomicIntegerArray：整型数组原子类
* AtomicLongArray：长整型数组原子类
* AtomicReferenceArray ：引用类型数组原子类

上面三个类提供的方法几乎相同，所以我们这里以 AtomicIntegerArray 为例子来介绍。

常用方法:
```
public final int get(int i)//Gets the current value at position i
public final void lazySet(int i, int newValue)//Eventually sets the element at position i to the given value
public final int getAndSet(int i, int newValue)//Atomically sets the element at position i to the given value and returns the old value
public final boolean compareAndSet(int i, int expect, int update)//Atomically sets the element at position  i to the given updated value if the current value == the expected value
public final int getAndIncrement(int i)//Atomically increments by one the element at index i
public final int getAndDecrement(int i)//Atomically decrements by one the element at index i
public final int getAndAdd(int i, int delta)//Atomically adds the given value to the element at index i
```
### AtomicIntegerArray 常见方法使用
```
import java.util.concurrent.atomic.AtomicIntegerArray;

public class AtomicIntegerArrayDemo {

    public static void main(String[] args) {
        int[] nums = {1, 2, 3, 4, 5, 6};
        AtomicIntegerArray array = new AtomicIntegerArray(nums);
        for (int i = 0; i < nums.length; i++) {
            System.out.println(array.get(i));
        }

        int temp = array.getAndSet(0, 2);
        System.out.println("temp: " + temp + "; array: " + array);

        temp = array.getAndIncrement(0);
        System.out.println("temp: " + temp + "; array: " + array);

        temp = array.getAndAdd(0, 5);
        System.out.println("temp: " + temp + "; array: " + array);
    }
}
```
## 引用类型原子类
### 引用类型原子类介绍
* AtomicReference：引用类型原子类
* AtomicStampedReference：原子更新引用类型里的字段原子类
* AtomicMarkableReference ：原子更新带有标记位的引用类型

上面三个类提供的方法几乎相同，所以我们这里以 AtomicReference 为例子来介绍。

使用示例:
```
import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceDemo {

    public static void main(String[] args) {
        AtomicReference<Person> reference = new AtomicReference<>();
        Person person = new Person("aaa", 20);
        reference.set(person);

        Person updatePerson = new Person("bbb", 33);
        reference.compareAndSet(person, updatePerson);

        System.out.println(reference.get()); // Person{name='bbb', age=33}
    }
}

class Person {
    private String name;
    private int age;

    public Person(String name, int age) {
        super();
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
```

## 对象的属性修改类型原子类
### 对象的属性修改类型原子类介绍
* AtomicIntegerFieldUpdater：原子更新整型字段的更新器
* AtomicLongFieldUpdater：原子更新长整型字段的更新器
* AtomicStampedReference ：原子更新带有版本号的引用类型。
该类将整数值与引用关联起来，可用于解决原子的更新数据和数据的版本号，可以解决使用 CAS 进行原子更新时可能出现的 ABA 问题。

要想原子地更新对象的属性需要两步。第一步，因为对象的属性修改类型原子类都是抽象类，所以每次使用都必须使用静态方法 newUpdater()创建一个更新器，并且需要设置想要更新的类和属性。
第二步，更新的对象属性必须使用 public volatile 修饰符。
```
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AtomicIntegerFieldUpdaterDemo {
    public static void main(String[] args) {
        AtomicIntegerFieldUpdater<User> updater = AtomicIntegerFieldUpdater.newUpdater(User.class, "age");

        User user = new User("aaa", 11);
        System.out.println(updater.getAndIncrement(user)); //11
        System.out.println(updater.get(user));//12
    }
}

class User {
    private String name;
    public volatile int age;

    public User(String name, int age) {
        super();
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
```
