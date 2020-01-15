import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jc79482
 * Date: 1/15/2020
 * Time: 2:03 PM
 * link: https://medium.com/algorithm-and-datastructure/lfu-cache-in-o-1-in-java-4bac0892bdb3 && https://leetcode.com/problems/lfu-cache/
 */
public class LFUCache {

    Map<Integer, Integer> values; // k -> v
    Map<Integer, Integer> counts; // k -> count
    Map<Integer, LinkedHashSet<Integer>> lists; // count -> item list

    private int capacity;
    private int min = -1;

    public LFUCache(int capacity) {
        this.capacity = capacity;
        values = new HashMap<>();
        counts = new HashMap<>();
        lists = new HashMap<>();
    }

    public int get(int key) {
        if (!values.containsKey(key)) return -1;
        int count = counts.get(key);
        count++;
        counts.put(key, count);
        lists.get(count - 1).remove(key);
        if (min == count - 1 && lists.get(min).size() == 0) min++;
        if (!lists.containsKey(count)) lists.put(count, new LinkedHashSet<>());
        lists.get(count).add(key);
        return values.get(key);
    }

    public void put(int key, int value) {
        if (this.capacity <= 0) return;
        if (values.containsKey(key)) {
            values.put(key, value);
            get(key);
            return;
        }
        if (values.size() == capacity) {
            int evict = lists.get(min).iterator().next();
            lists.get(min).remove(evict);
            values.remove(evict);
            counts.remove(evict);
        }
        values.put(key, value);
        counts.put(key, 1);
        if (!lists.containsKey(1)) lists.put(1, new LinkedHashSet<>());
        lists.get(1).add(key);
        min = 1;
    }

    public static void main(String[] args) {
        LFUCache cache = new LFUCache(2 /* capacity */);

        cache.put(1, 1);
        cache.put(2, 2);
        cache.get(1);       // returns 1
        cache.put(3, 3);    // evicts key 2
        cache.get(2);       // returns -1 (not found)
        cache.get(3);       // returns 3.
        cache.put(4, 4);    // evicts key 1.
        cache.get(1);       // returns -1 (not found)
        cache.get(3);       // returns 3
        cache.get(4);       // returns 4
    }
}
