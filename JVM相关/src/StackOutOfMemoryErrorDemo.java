public class StackOutOfMemoryErrorDemo {
    public static void main(String[] args) {
        while (true) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true){
                        ;
                    }
                }
            });
            thread.start();
        }
    }
}
