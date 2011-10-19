package cache;

import java.util.Comparator;
import java.util.Map.Entry;

/**
 *
 * @author robert gering
 */
public class CacheEntry<K extends Object, V extends Object> implements Entry<K, V> {

    private final K key;
    private V value;
    private final long timeToLife;     // in ms
    private final long added;
    private long lastAccess;
    private long accessCount;


    /**
     * Creates a new CacheEntry with ttl
     * @param value
     * @param timeToLife time to live in ms
     */
    CacheEntry(K key, V value, long timeToLife) {
        this.key = key;
        this.value = value;
        this.timeToLife = timeToLife;
        this.added = System.currentTimeMillis();
        this.lastAccess = this.added;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        lastAccess = System.currentTimeMillis();
        accessCount++;
        return value;
    }

    public V setValue(V value) {
        this.value = value;
        return this.value;
    }

    public long getAdded() {
        return added;
    }

    public long getTimeToLife() {
        return timeToLife;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public boolean isValide() {
        return rest() > 0;
    }

    public long passed() {
        return System.currentTimeMillis() - added;
    }

    public long rest() {
        return timeToLife - passed();
    }

    public static Comparator<CacheEntry> getComparator(CleanupStrategy strategy) {
        switch (strategy) {
            case CLEANUP_BY_LAST_ADDED:
                return new Comparator<CacheEntry>() {
                    public int compare(CacheEntry o1, CacheEntry o2) {
                        return o1.getAdded() > o2.getAdded() ? 1 : -1;
                    }
                };
            case CLEANUP_BY_LAST_ACCESS:
                return new Comparator<CacheEntry>() {
                    public int compare(CacheEntry o1, CacheEntry o2) {
                        return o1.getLastAccess() > o2.getLastAccess() ? 1 : -1;
                    }
                };
            case CLEANUP_BY_REMAINING_TTL:
                return new Comparator<CacheEntry>() {
                    public int compare(CacheEntry o1, CacheEntry o2) {
                        return o1.rest() > o2.rest() ? 1 : -1;
                    }
                };
            case CLEANUP_BY_ACCESS_COUNT:
                return new Comparator<CacheEntry>() {
                    public int compare(CacheEntry o1, CacheEntry o2) {
                        return o1.getAccessCount() > o2.getAccessCount() ? 1 : -1;
                    }
                };
            default:
                return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CacheEntry) {
            return getValue().equals(((CacheEntry)obj).getValue());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.value != null ? this.value.hashCode() : 0);
        return hash;
    }
}
