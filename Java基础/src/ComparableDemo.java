import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Student implements Comparable {
    String name;
    int score;

    public Student(String name, int score) {
        this.name = name;
        this.score = score;
    }

    @Override
    public int compareTo(Object o) {
        // 因为我们需要按score来排序，所以直接将两个对象的score相减，就可以得到两者的大小关系了。
        // 那为什么是 o.score - this.score呢？因为要求是按score降序排序。
        // 大家可以这样理解，this和o在原始序列中的位置是o在前，this在后，
        // 然后这里如果返回正数或0，那么依旧是o在前，this在后，
        // 繁殖如果返回负数，那么就要调整位置this在前，o在后。
        if (((Student) o).score != this.score)
            return ((Student) o).score - this.score;
        return this.name.compareTo(((Student) o).name);
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", score=" + score +
                '}';
    }
}

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
