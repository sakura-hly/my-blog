public class Demo {
    public static void main(String[] args) {
        Integer i1 = 40;
        Integer i2 = 40;
        Integer i3 = 0;
        Integer i4 = new Integer(40);
        Integer i5 = new Integer(40);
        Integer i6 = new Integer(0);

        System.out.println("i1 = i2   " + (i1 == i2)); // true
        System.out.println("i1 = i2 + i3   " + (i1 == i2 + i3)); // true
        System.out.println("i1 = i4   " + (i1 == i4)); // false
        System.out.println("i4 = i5   " + (i4 == i5)); // false
        System.out.println("i4 = i5 + i6   " + (i4 == i5 + i6)); // true
        System.out.println("40 = i5 + i6   " + (40 == i5 + i6)); // true
    }
}
