public class GCTest {
    /*
     * -XX:+PrintGCDetails
     * java version "1.8.0_131"
     */
    public static void main(String[] args) {
        byte[] allocation1, allocation2, allocation3, allocation4, allocation5;
        allocation1 = new byte[29271 * 1024];
        allocation2 = new byte[1000 * 1024];
        allocation3 = new byte[1000 * 1024];
        allocation4 = new byte[1000 * 1024];
        allocation5 = new byte[1000 * 1024];
    }
}
