package de.robertgering;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author robert gering
 */
public class ManagedCache<K extends Object, V extends Object> {

    private final Map<K, CacheEntry<K, V>> cache = new HashMap<K, CacheEntry<K, V>>();
    private long maxItems;
    private long defaultTTL;
    private final CleanupStrategy cleanupStrategy;

    public ManagedCache(long maxItems, long defaultTTL, CleanupStrategy cleanupStrategy) {
        this.maxItems = maxItems;
        this.defaultTTL = defaultTTL;
        this.cleanupStrategy = cleanupStrategy;
    }

    public long getDefaultTTL() {
        return defaultTTL;
    }

    public long getMaxItems() {
        return maxItems;
    }

    public int size() {
        cleanup();
        return cache.size();
    }

    public boolean isEmpty() {
        cleanup();
        return cache.isEmpty();
    }

    public boolean containsKey(K key) {
        cleanup();
        return cache.containsKey(key);
    }

    public boolean containsValue(V value) {
        for (CacheEntry<K, V> cacheEntry : cache.values()) {
            if (cacheEntry.getValue().equals(value) && cacheEntry.isValide()) {
                return true;
            }
        }
        return false;
    }

    public V get(K key) {
        CacheEntry<K, V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isValide()) {
            return entry.getValue();
        } else {
            cache.remove(key);
            return null;
        }
    }

    public void put(K key, V value, long ttl) {
        cache.put(key, new CacheEntry<K, V>(key, value, ttl));
        if (cache.size() > maxItems) {
            cleanup();
        }
    }

    public void put(K key, V value) {
        put(key, value, defaultTTL);
    }

    public V remove(K key) {
        return cache.remove(key).getValue();
    }

    public void remove(Collection<K> keys) {
        for (K key : keys) {
            cache.remove(key);
        }
    }

    public void clear() {
        cache.clear();
    }

    public Set<K> keySet() {
        return cache.keySet();
    }

    public void cleanup() {
        synchronized (cache) {
            Set<K> cleanKeys = new HashSet<K>();
            for (K key : cache.keySet()) {
                if (!cache.get(key).isValide()) {
                    cleanKeys.add(key);
                }
            }
            remove(cleanKeys);

            if (cache.size() > maxItems) {
                List<CacheEntry<K, V>> entries = new LinkedList(cache.values());
                Collections.sort(entries, CacheEntry.getComparator(cleanupStrategy));
                for (int i = 0; cache.size() > maxItems; i++) {
                    cache.remove(entries.get(i).getKey());
                }
            }
        }
    }
}
