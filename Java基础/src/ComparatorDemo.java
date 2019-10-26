import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

class StudentComparator implements Comparator<Student> {

    @Override
    public int compare(Student o1, Student o2) {
        if (o2.score != o1.score)
            return o2.score - o1.score;
        return o1.name.compareTo(o2.name);
    }
}

public class ComparatorDemo {

    public static void main(String[] args) {
        List<Student> students = new ArrayList<>();
        students.add(new Student("Bob", 88));
        students.add(new Student("Marry", 85));
        students.add(new Student("Alan", 74));
        students.add(new Student("Harry", 90));
        students.add(new Student("Alice", 88));

        // 由于Student不再实现Comparable，所以这里需要传入第二个参数，即我们自定义的比较器
        Collections.sort(students, new StudentComparator());

        // Collections.sort(students, (o1, o2) -> {
        //     if (o2.score != o1.score)
        //         return o2.score - o1.score;
        //     return o1.name.compareTo(o2.name);
        // });
        System.out.println(students);
    }
}
