import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jc79482
 * Date: 12/25/2019
 * Time: 4:34 PM
 */
public class LRUCache {

    private Map<Integer, Integer> map;
    private LinkedList<Integer> list;
    private int capacity;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        map = new HashMap<>();
        list = new LinkedList<>();
    }

    public int get(int key) {
        if (!map.containsKey(key)) return -1;
        list.remove((Integer) key);
        list.addFirst(key);
        return map.get(key);
    }

    public void put(int key, int value) {
        if (capacity <= 0) return;
        if (!map.containsKey(key) && map.size() == capacity) {
            int evict = list.removeLast();
            map.remove(evict);
        }
        map.put(key, value);
        get(key);
    }

    public static void main(String[] args) {
        LRUCache cache = new LRUCache(2 /* capacity */);

        cache.put(1, 1);
        cache.put(2, 2);
        cache.get(1);       // returns 1
        cache.put(3, 3);    // evicts key 2
        cache.get(2);       // returns -1 (not found)
        cache.put(4, 4);    // evicts key 1
        cache.get(1);       // returns -1 (not found)
        cache.get(3);       // returns 3
        cache.get(4);       // returns 4
    }
}
