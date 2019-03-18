import java.util.concurrent.Semaphore;
/*
 * output:
 * Thread 1: 1
 * Thread 2: 2
 * Thread 3: 3
 * Thread 1: 4
 * Thread 2: 5
 * Thread 3: 6
 * Thread 1: 7
 * Thread 2: 8
 * Thread 3: 9
 * Thread 1: 10
 */
public class SemaphoreTest {
    private static final int MAX_NUMBER = 100;
    private static int NUM = 1;
    private static final int K = 3;
    private static final Semaphore[] SEMAPHORES = new Semaphore[K];

    static {
        for (int i = 0; i < K; i++) {
            SEMAPHORES[i] = new Semaphore(1);
            if (i != 0) {
                try {
                    SEMAPHORES[i].acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class Work implements Runnable {
        private int id;

        public Work(int id) {
            this.id = id - 1;
        }

        @Override
        public void run() {
            int nextId = id == K - 1 ? 0 : id + 1;
            while (true) {
                try {
                    SEMAPHORES[id].acquire();
                    if (NUM > MAX_NUMBER) {
                        SEMAPHORES[nextId].release();
                        break;
                    }
                    System.out.println("Thread " + (id + 1) + ": " + NUM);
                    NUM++;
                    SEMAPHORES[nextId].release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        for (int i = 1; i <= K; i++) {
            Thread thread = new Thread(new Work(i));
            thread.start();
        }
    }
}
