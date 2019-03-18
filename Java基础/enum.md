# 枚举(enum)
## 理解枚举类型
枚举类型是Java 5中新增特性的一部分，它是一种特殊的数据类型，之所以特殊是因为它既是一种类(class)类型却又比类类型多了些特殊的约束，但是这些约束的存在也造就了枚举类型的简洁性、安全性以及便捷性。
### 枚举的定义
在定义枚举类型时我们使用的关键字是enum，与class关键字类似，只不过前者是定义枚举类型，后者是定义类类型。
同时我们应该知道的是枚举类型可以像类(class)类型一样，定义为一个单独的文件，当然也可以定义在其他类内部，更重要的是枚举常量在类型安全性和便捷性都很有保证，
如果出现类型问题编译器也会提示我们改进，但务必记住枚举表示的类型其取值是必须有限的，也就是说每个值都是可以枚举出来的。
### 枚举实现原理
在使用关键字enum创建枚举类型并编译后，编译器会为我们生成一个相关的类，这个类继承了Java API中的java.lang.Enum类，
也就是说通过关键字enum创建枚举类型在编译后事实上也是一个类类型而且该类继承自java.lang.Enum类。
如定义以下枚举类
```
public enum Day {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}
```
使用javac Day.java编译后生成Day.class文件
```
public final class Day extends Enum
{
    //编译器为我们添加的静态的values()方法
    public static Day[] values()
    {
        return (Day[])$VALUES.clone();
    }
    //编译器为我们添加的静态的valueOf()方法，注意间接调用了Enum也类的valueOf方法
    public static Day valueOf(String s)
    {
        return (Day)Enum.valueOf(com/zejian/enumdemo/Day, s);
    }
    //私有构造函数
    private Day(String s, int i)
    {
        super(s, i);
    }
     //前面定义的7种枚举实例
    public static final Day MONDAY;
    public static final Day TUESDAY;
    public static final Day WEDNESDAY;
    public static final Day THURSDAY;
    public static final Day FRIDAY;
    public static final Day SATURDAY;
    public static final Day SUNDAY;
    private static final Day $VALUES[];

    static 
    {    
        //实例化枚举实例
        MONDAY = new Day("MONDAY", 0);
        TUESDAY = new Day("TUESDAY", 1);
        WEDNESDAY = new Day("WEDNESDAY", 2);
        THURSDAY = new Day("THURSDAY", 3);
        FRIDAY = new Day("FRIDAY", 4);
        SATURDAY = new Day("SATURDAY", 5);
        SUNDAY = new Day("SUNDAY", 6);
        $VALUES = (new Day[] {
            MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
        });
    }
}
```
## 枚举的常见方法
Enum是所有 Java 语言枚举类型的公共基本类（注意Enum是抽象类），以下是它的常见方法：

|返回类型	|方法名称|	方法说明|
|:-----:|:-----:|:------:|
|int	|compareTo(E o)	|比较此枚举与指定对象的顺序|
|boolean	|equals(Object other)	|当指定对象等于此枚举常量时，返回 true。|
|Class<?>	|getDeclaringClass()	|返回与此枚举常量的枚举类型相对应的 Class 对象|
|String	|name()	|返回此枚举常量的名称，在其枚举声明中对其进行声明|
|int	|ordinal()	|返回枚举常量的序数（它在枚举声明中的位置，其中初始常量序数为零）|
|String	|toString()	|返回枚举常量的名称，它包含在声明中|
|static<T extends Enum<T>> T	|static valueOf(Class<T> enumType, String name)|	返回带指定名称的指定枚举类型的枚举常量。|

Enum类内部会有一个构造函数，该构造函数只能有编译器调用，我们是无法手动操作的，不妨看看Enum类的主要源码：
```
public abstract class Enum<E extends Enum<E>>
        implements Comparable<E>, Serializable {
    //枚举字符串名称
    private final String name;

    public final String name() {
        return name;
    }

    //枚举顺序值
    private final int ordinal;

    public final int ordinal() {
        return ordinal;
    }

    //枚举的构造方法，只能由编译器调用
    protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    public String toString() {
        return name;
    }

    public final boolean equals(Object other) {
        return this==other;
    }

    public final int hashCode() {
        return super.hashCode();
    }

    protected final Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    
    //比较的是ordinal值
    public final int compareTo(E o) {
        Enum<?> other = (Enum<?>)o;
        Enum<E> self = this;
        if (self.getClass() != other.getClass() && // optimization
            self.getDeclaringClass() != other.getDeclaringClass())
            throw new ClassCastException();
        return self.ordinal - other.ordinal;
    }

    @SuppressWarnings("unchecked")
    public final Class<E> getDeclaringClass() {
        Class<?> clazz = getClass();
        Class<?> zuper = clazz.getSuperclass();
        return (zuper == Enum.class) ? (Class<E>)clazz : (Class<E>)zuper;
    }

    public static <T extends Enum<T>> T valueOf(Class<T> enumType,
                                                String name) {
        T result = enumType.enumConstantDirectory().get(name);
        if (result != null)
            return result;
        if (name == null)
            throw new NullPointerException("Name is null");
        throw new IllegalArgumentException(
            "No enum constant " + enumType.getCanonicalName() + "." + name);
    }
    ...
}
```

## 枚举与单例模式
代码相当简洁，我们也可以像常规类一样编写enum类，为其添加变量和方法，访问方式也更简单，
使用SingletonEnum.INSTANCE进行访问，这样也就避免调用getInstance方法，更重要的是使用枚举单例的写法，我们完全不用考虑序列化和反射的问题。
```
public enum  SingletonEnum {
    INSTANCE;
}
```
枚举序列化是由jvm保证的，每一个枚举类型和定义的枚举变量在JVM中都是唯一的，在枚举类型的序列化和反序列化上，Java做了特殊的规定：
在序列化时Java仅仅是将枚举对象的name属性输出到结果中，反序列化的时候则是通过java.lang.Enum的valueOf方法来根据名字查找枚举对象。
同时，编译器是不允许任何对这种序列化机制的定制的并禁用了writeObject、readObject、readObjectNoData、writeReplace和readResolve等方法，从而保证了枚举实例的唯一性。

关于单例，我们总是应该记住：线程安全，延迟加载，序列化与反序列化安全，反射安全是很重重要的。