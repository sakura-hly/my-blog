import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: jc79482
 * Date: 1/15/2020
 * Time: 4:51 PM
 */
public class RateLimiter {
    public int REQUEST_LIMIT = 3;
    public Long TIME_LIMIT = 1000L;

    public class HitCounter {
        public Queue<Long> queue;

        public HitCounter() {
            queue = new ArrayBlockingQueue<>(REQUEST_LIMIT);
        }

        public boolean hit(long timestamp) {
            /* when a timestamp hit, we should poll all the timestamp before TIME_LIMIT*/
            while (!queue.isEmpty() && timestamp - queue.peek() >= TIME_LIMIT) queue.poll();
            // if (queue.size() < REQUEST_LIMIT) {
            //	queue.add(timestamp);
            //	return true;
            // }
            // return false;
            return queue.offer(timestamp);
        }
    }

    public Map<String, HitCounter> clientTimeStampMap = new ConcurrentHashMap<>();

    public boolean isAllow(String clientId) {
        long currTime = System.currentTimeMillis();
        if (!clientTimeStampMap.containsKey(clientId)) {
            HitCounter h = new HitCounter();
            clientTimeStampMap.put(clientId, h);
            h.hit(currTime);
            return true;
        } else {
            HitCounter h = clientTimeStampMap.get(clientId);
            return h.hit(currTime);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        RateLimiter rateLimiter = new RateLimiter();
        for (int i = 0; i < 5; i++) {
            System.out.println(rateLimiter.isAllow("1"));
        }
        for (int i = 0; i < 5; i++) {
            System.out.println(rateLimiter.isAllow("2"));
        }
        Thread.sleep(1 * 1000);
        System.out.println(rateLimiter.isAllow("1"));
        System.out.println(rateLimiter.isAllow("2"));
    }
}
