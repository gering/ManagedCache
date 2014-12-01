package de.robertgering.managedcache;

import java.util.*;
import java.util.Map.Entry;

/**
 * ManagedCache implements the Map<String, V> Interface
 * this cache is thread safe
 * @author Robert Gering
 * @param <V> the value
 */
public class ManagedCache<V extends Object> {
	
	public static final long SECOND = 1000;
	public static final long MINUTE = 60 * SECOND;
	public static final long HOUR = 60 * MINUTE;
	public static final long DAY = 24 * HOUR;

	private final Map<String, CacheEntry<V>> cache = new HashMap<String, CacheEntry<V>>();
	private final long maxEntries;
	private final long defaultTTL;
	private final CleanupStrategy cleanupStrategy;
	
	// statistics
	private long hitCount;	// hitted cache
	private long missCount;	// missed cache
	
	public ManagedCache(long maxEntries, long defaultTTL, CleanupStrategy cleanupStrategy) {
		this.maxEntries = maxEntries;
		this.defaultTTL = defaultTTL;
		this.cleanupStrategy = cleanupStrategy;
	}

	public long getDefaultTTL() {
		return defaultTTL;
	}

	public long getMaxEntries() {
		return maxEntries;
	}

	public synchronized int size() {
		cleanup();
		return cache.size();
	}

	public synchronized boolean isEmpty() {
		cleanup();
		return cache.isEmpty();
	}

	public synchronized boolean containsKey(String key) {
		cleanup();
		boolean result = key != null && cache.containsKey(key);
		if (!result) {
			missCount++;
		}
		return result;
	}

	public synchronized boolean containsValue(V value) {
		for (CacheEntry<V> entry : entrySet()) {
			if (entry.isValid() && entry.getValue().equals(value)) {
				return true;
			}
		}
		return false;
	}

	public synchronized V get(Object key) {
		return get(key.toString());
	}
	
	public synchronized V get(String key) {
		if (key == null) {
			missCount++;
			return null;
		}
		CacheEntry<V> entry = cache.get(key);
		if (entry == null) {
			missCount++;
			return null;
		}
		if (entry.isValid()) {
			hitCount++;
			return entry.getValue();
		} else {
			missCount++;
			cache.remove((String)key);
			return null;
		}
	}

	private synchronized V put(String key, V value, long ttl, boolean cleanup) {
		if (key == null) {
			return null;
		}
		if (value == null) {
			return remove(key);
		}
		if (cache.size() >= maxEntries) {
			cleanup(1);
		}
		CacheEntry<V> oldValue = cache.put(key, new CacheEntry<V>(key, value, ttl));
		return oldValue != null ? oldValue.getValue() : null;
	}
	
	public V put(Object key, V value, long ttl) {
		return put(key.toString(), value, ttl);
	}

	public V put(Object key, V value) {
		return put(key.toString(), value);
	}
	
	public synchronized V put(String key, V value, long ttl) {
		return put(key, value, ttl, true);
	}

	public synchronized V put(String key, V value) {
		return put(key, value, defaultTTL, true);
	}

	public void putAll(Map<? extends String, ? extends V> m) {
		for (Entry<? extends String, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue(), defaultTTL, false);
		}
		cleanup();
	}
	
	public V remove(Object key) {
		return remove(key.toString());
	}
	
	public synchronized V remove(String key) {
		CacheEntry<V> entry = cache.remove(key);
		return entry != null ? entry.getValue() : null;
	}

	public synchronized void removeAll(Collection<String> keys) {
		for (String key : keys) {
			cache.remove(key);
		}
	}

	public synchronized void clear() {
		cache.clear();
	}

	public synchronized Set<String> keySet() {
		return cache.keySet();
	}
	
	public synchronized Set<CacheEntry<V>> entrySet() {
		cleanup();
		return new HashSet<CacheEntry<V>>(cache.values());
	}

	public Collection<V> values() {
		List<V> values = new ArrayList<V>(size());
		for (CacheEntry<V> entry : entrySet()) {
			values.add(entry.getValue());
		}
		return values;
	}
	
	public synchronized void cleanup() {
		cleanup((int) (cache.size() - maxEntries));
	}
	
	private synchronized void cleanup(int count) {
		
		if (count > cache.size()) count = cache.size();
		
		// cleanup invalid items
		{
			List<CacheEntry<V>> entries = new ArrayList(cache.values());
			for (CacheEntry<V> cacheEntry : entries) {
				if (!cacheEntry.isValid()) {
					cache.remove(cacheEntry.getKey());
					count--;
				}
			}
		}

		// must delete vaild items?
		if (count > 0) {
			List<CacheEntry<V>> entries = new ArrayList(cache.values());
			Collections.sort(entries, CacheEntry.getComparator(cleanupStrategy));

			for (int i = 0; i < count; i++) {
				CacheEntry<V> entry = entries.get(i);
				cache.remove(entry.getKey());
			}
		}
	}

	/**
	 * resets the hit and miss counter for this cache
	 */
	public synchronized void resetStatistics() {
		hitCount = 0;
		missCount = 0;
	}

	/**
	 * The hit count gets incremented everytime <code>get()</code> or
	 * <code>containsKey()</code> returns an entry or true.
	 *
	 * @return the number of successfull requests to the cache
	 */
	public synchronized long getHitCount() {
		return hitCount;
	}

	/**
	 * The miss count gets incremented everytime <code>get()</code> or
	 * <code>containsKey()</code> returns null or false.
	 *
	 * @return the number of failed requests to the cache
	 */
	public synchronized long getMissCount() {
		return missCount;
	}

	/**
	 * @return the total number of requests made to this cache
	 */
	public synchronized long getRequestCount() {
		return hitCount + missCount;
	}

	/**
	 * Looks over evry valid cached entry and averages the remaining ttl
	 *
	 * @return average remaining time to life in ms
	 */
	public synchronized long getAverageRemainingTTL() {
		cleanup();
		if (cache.isEmpty()) {
			return 0;
		}
		long total = 0;
		for (CacheEntry entry : cache.values()) {
			total += entry.getTimeToLife();
		}
		return total / cache.size();
	}
}
