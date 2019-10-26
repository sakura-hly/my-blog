# 你需要知道的Java比较器

在Java里，数字大小可以直接使用 < 和 > 来比较，那么对于其它类型或者自定义类型呢？JDK为我们提供了两种比较器：Comparable和Comparator，下面来详细介绍这两位。

## Comparable

首先我们来看一下Comparable：

```java
public interface Comparable<T> {
    /**
     * @param   o the object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *          is less than, equal to, or greater than the specified object.
     *
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this object.
     */
    public int compareTo(T o);
}
```

显而易见，Comparable是一个支持泛型的接口，里面只有这么一个方法 compareTo()，意思是将当前对象（this引用的对象）与传入的对象o进行比较，大小关系由返回值决定：

1. 负数：this < o
2. 0: this == o
3. 正数: this > o

由于源码中注释太多，我这里就截取部分，其它的注释阐述了一些 compareTo() 的规范，总结一下就是：

* 对于所有x和y，sgn(x.compareTo(y)) == -sgn(y.compareTo(x))（注：sgn()是符号函数，其功能是取某个数的符号正，负或0），当然如果前者抛异常，后者也必须跟着抛异常。

* 传递性：如果 x.compareTo(y) > 0 并且 y.compareTo(z) > 0，那么 x.compareTo(z) > 0。

* 最后，如果 x.compareTo(y) == 0，那么对于任意 z，都有 sgn(x.compareTo(z)) == sgn(y.compareTo(z))。

另外还强烈建议 (x.compareTo(y)==0) == (x.equals(y))，但是不强制。只要我们按照上述规范实现 compareTo()，那么比较器就能够按照我们的期望运行。

好吧，Talk is cheap, show me the code.

我们定义一个Student类，有两个属性：name和score，

```java
class Student {
    String name;
    int score;

    public Student(String name, int score) {
        this.name = name;
        this.score = score;
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", score=" + score +
                '}';
    }
}
```

然后需求来了，班主任统计分数时，希望可以将学生从高到低排序，咋整呢？不要方，运用我们刚才学到的知识，先给Student加上一个 Comparable 接口

```java
class Student implements Comparable {...}
```

然后需要实现接口里的方法，

```java
@Override
public int compareTo(Object o) {
    // 因为我们需要按score来排序，所以直接将两个对象的score相减，就可以得到两者的大小关系了。
    // 那为什么是 o.score - this.score呢？因为要求是按score降序排序。
    // 大家可以这样理解，this和o在原始序列中的位置是o在前，this在后，
    // 然后这里如果返回正数或0，那么依旧是o在前，this在后，
    // 繁殖如果返回负数，那么就要调整位置this在前，o在后。
    return ((Student) o).score - this.score;
}
```

我们来测试一下，

```java
public class ComparableDemo {

    public static void main(String[] args) {
        List<Student> students = new ArrayList<>();
        students.add(new Student("Bob", 88));
        students.add(new Student("Marry", 85));
        students.add(new Student("Alan", 74));
        students.add(new Student("Harry", 90));
        students.add(new Student("Alice", 88));

        Collections.sort(students);
        System.out.println(students);
    }
}
```

结果输出：

```java
[Student{name='Harry', score=90}, Student{name='Bob', score=88}, Student{name='Alice', score=88}, Student{name='Marry', score=85}, Student{name='Alan', score=74}]
```

可以看到比较器如愿运行。

等等，还没完，班主任又说，分数相等时，按照名字的字典顺序排序。。。

哼哼，将刚才的 compareTo() 方法稍加改造即可。

```java
@Override
public int compareTo(Object o) {
    if (((Student) o).score != this.score)
        return ((Student) o).score - this.score;
    return this.name.compareTo(((Student) o).name);
}
```

跑一下刚才的测试：

```java
[Student{name='Harry', score=90}, Student{name='Alice', score=88}, Student{name='Bob', score=88}, Student{name='Marry', score=85}, Student{name='Alan', score=74}]
```

可以看到Alice确实排在Bob前面了。搞定，中场休息~~
