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
class Student implements Comparable<Student> {...}
```

然后需要实现接口里的方法，

```java
@Override
public int compareTo(Student o) {
    // 因为我们需要按score来排序，所以直接将两个对象的score相减，就可以得到两者的大小关系了。
    // 那为什么是 o.score - this.score呢？因为要求是按score降序排序。
    // 大家可以这样理解，this和o在原始序列中的位置是o在前，this在后，
    // 然后这里如果返回正数或0，那么依旧是o在前，this在后，
    // 繁殖如果返回负数，那么就要调整位置this在前，o在后。
    return o.score - this.score;
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
public int compareTo(Student o) {
    if (o.score != this.score)
        return o.score - this.score;
    return this.name.compareTo(o.name);
}
```

跑一下刚才的测试：

```java
[Student{name='Harry', score=90}, Student{name='Alice', score=88}, Student{name='Bob', score=88}, Student{name='Marry', score=85}, Student{name='Alan', score=74}]
```

可以看到Alice确实排在Bob前面了。搞定，中场休息~~

## Comparator

说完了 Comparable，我们再来看 Comparator 就很简单了，首先还是来看一下源码，

```java
@FunctionalInterface
public interface Comparator<T> {
    int compare(T o1, T o2);
}
```

Comparator 也是一个接口，并且是一个函数式接口，意味着可以转换成lambda了。compare() 方法和前面提到的 compareTo() 作用一样一样的，只不过从比较this和o，变成了比较o1和o2。

那为什么JDK多此一举要提供两个功能差不多的接口呢？我个人理解是，有时候对于我们自定义的类，在不同的场景下可能需要不同的排序规则，用 Comparable 的话我们只能制定一种排序规则，所以这时就该 Comparator 出场了，而且还可以写出lambda。

好了，现在用 Comparator 来重新写前面的需求。现在我们的 Student 不再实现 Comparable 接口，而是重新定义一个新的比较器类：

```java
class StudentComparator implements Comparator<Student> {

    @Override
    public int compare(Student o1, Student o2) {
        if (o2.score != o1.score)
            return o2.score - o1.score;
        return o1.name.compareTo(o2.name);
    }
}
```

compare()方法的实现跟前面的compareTo()大体一致，跑一下测试用例：

```java
public class ComparatorDemo {

    public static void main(String[] args) {
        List<Student2> students = new ArrayList<>();
        students.add(new Student2("Bob", 88));
        students.add(new Student2("Marry", 85));
        students.add(new Student2("Alan", 74));
        students.add(new Student2("Harry", 90));
        students.add(new Student2("Alice", 88));

        // 由于Student不再实现Comparable，所以这里需要传入第二个参数，即我们自定义的比较器
        Collections.sort(students, new StudentComparator());
        System.out.println(students);
    }
}
```

结果也是运行正确：

```java
[Student{name='Harry', score=90}, Student{name='Alice', score=88}, Student{name='Bob', score=88}, Student{name='Marry', score=85}, Student{name='Alan', score=74}]
```

说好的lambda呢？

```java
Collections.sort(students, (o1, o2) -> {
    if (o2.score != o1.score)
        return o2.score - o1.score;
    return o1.name.compareTo(o2.name);
});
```

## Practice

哈哈，理论部分到此结束。为了验证大家的掌握情况，这里有一道编程题，[1122. Relative Sort Array](https://leetcode.com/problems/relative-sort-array/)。

```java
Input: arr1 = [2,3,1,3,2,4,6,7,9,2,19], arr2 = [2,1,4,3,9,6]
Output: [2,2,2,1,4,3,3,9,6,7,19]
```

题意：给出两个数组，将arr1按照arr2的相关性排序。如果arr1的任意两个数字都在arr2中出现，那么就按照在arr2中出现的位置排序，否则将没出现的数字排在出现的数字之后，并且升序。

相信各位聪明的读者根据前面学习的内容，可以很快解答出来。在此，贴出本人的代码：

```java
class Solution {
    public int[] relativeSortArray(int[] arr1, int[] arr2) {
        Map<Integer, Integer> map = new HashMap<>(arr2.length);
        for (int i = 0; i < arr2.length; i++) map.put(arr2[i], i);

        return Arrays.stream(arr1).boxed().sorted((a, b) -> {
            if (map.containsKey(a) && map.containsKey(b)) {
                return map.get(a) - map.get(b);
            }
            if (map.containsKey(a)) return -1;
            if (map.containsKey(b)) return 1;
            return a - b;
        }).mapToInt(i -> i).toArray();
    }
}
```

最后，看到这里不容易了，谢谢各位的观看:)
