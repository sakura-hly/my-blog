# equals()和hashcode()概述
equals()和hashcode()是Object类的两个很重要的方法。
## equals()方法
equals()方法是用来判断两个对象是否相等，源码如下：
```
public boolean equals(Object obj) {
    return (this == obj);
}
```
通过判断两个对象的地址是否相等，即是否是同一个对象。

默认所有类会继承Object的equals()，也就是说同一个对象时才返回true。但是有时候我们需要根据自己的逻辑重写equals()，典型的比如String类：
```
public boolean equals(Object anObject) {
    if (this == anObject) {
        return true;
    }
    if (anObject instanceof String) {
        String anotherString = (String)anObject;
        int n = value.length;
        if (n == anotherString.value.length) {
            char v1[] = value;
            char v2[] = anotherString.value;
            int i = 0;
            while (n-- != 0) {
                if (v1[i] != v2[i])
                    return false;
                i++;
            }
            return true;
        }
    }
    return false;
}
```
可以看到String类的equals()通过判断每一个字符来返回结果。

注意，根据Object规范，重写equals需要满足以下约定：
1. 自反性
2. 对称性
3. 传递性
4. 一致性
5. 非空性

另外，在《Effective Java》一书中提到，**覆盖equals时总要覆盖hashCode**。

## hashCode()方法
hashCode()的作用是返回对象的哈希码。它实际上返回的是一个int整数，用来确定对象在哈希表中的位置。

在Object类的规范里，hashCode()只对哈希表有用，对应Java的集合，就是HashMap，HashTable，HashSet等。

## 覆盖equals时总要覆盖hashCode
我们可以研究以下覆盖equals时没有覆盖hashCode会发生什么。
```
import java.util.*;
import java.lang.Comparable;

public class App{

    public static void main(String[] args) {
        // 新建Person对象，
        Person p1 = new Person("eee", 100);
        Person p2 = new Person("eee", 100);
        Person p3 = new Person("aaa", 200);

        // 新建HashSet对象 
        HashSet set = new HashSet();
        set.add(p1);
        set.add(p2);
        set.add(p3);

        // 比较p1 和 p2， 并打印它们的hashCode()
        System.out.printf("p1.equals(p2) : %s; p1(%d) p2(%d)\n", p1.equals(p2), p1.hashCode(), p2.hashCode());
        // 打印set
        System.out.printf("set:%s\n", set);
    }

    /**
     * @desc Person类。
     */
    private static class Person {
        int age;
        String name;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String toString() {
            return "("+name + ", " +age+")";
        }

        /** 
         * @desc 覆盖equals方法 
         */  
        @Override
        public boolean equals(Object obj){  
            if(obj == null){  
                return false;  
            }  
              
            //如果是同一个对象返回true，反之返回false  
            if(this == obj){  
                return true;  
            }  
              
            //判断是否类型相同  
            if(this.getClass() != obj.getClass()){  
                return false;  
            }  
              
            Person person = (Person)obj;  
            return name.equals(person.name) && age==person.age;  
        } 
    }
}
```
虽然我们重写了equals()，但是打印结果显示 set 里面还是有重复的元素：p1和p2。

这是因为，虽然p1和p2的equals()相等，但是hashCode()不相等，所以HashSet在添加元素的时候，认为它们不相等。