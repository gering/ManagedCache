package de.robertgering.managedcache;

import java.util.*;

/**
 *
 * @author robert gering
 * @param <K> the key
 * @param <V> the value
 */
public class ManagedCache<K extends Object, V extends Object> {

	public static final long SECOND = 1000;
	public static final long MINUTE = 60 * SECOND;
	public static final long HOUR = 60 * MINUTE;
	public static final long DAY = 24 * HOUR;

	private final Map<K, CacheEntry<K, V>> cache = Collections.synchronizedMap(new HashMap<K, CacheEntry<K, V>>());
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
		boolean result = cache.containsKey(key);
		if (result) {
			hitCount++;
		} else {
			missCount++;
		}
		return result;
	}

	public boolean containsValue(V value) {
		for (CacheEntry<K, V> cacheEntry : cache.values()) {
			if (cacheEntry.getValue().equals(value) && cacheEntry.isValid()) {
				return true;
			}
		}
		return false;
	}

	public V get(K key) {
		CacheEntry<K, V> entry = cache.get(key);
		if (entry == null) {
			missCount++;
			return null;
		}
		if (entry.isValid()) {
			hitCount++;
			return entry.getValue();
		} else {
			missCount++;
			cache.remove(key);
			return null;
		}
	}

	public V put(K key, V value, long ttl) {
		if (cache.size() >= maxEntries) {
			cleanup(1);
		}
		CacheEntry<K, V> oldValue = cache.put(key, new CacheEntry<K, V>(key, value, ttl));
		return oldValue != null ? oldValue.getValue() : null;
	}

	public V put(K key, V value) {
		return put(key, value, defaultTTL);
	}

	public V remove(K key) {
		CacheEntry<K ,V> entry = cache.remove(key);
		return entry != null ? entry.getValue() : null;
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
		cleanup((int) (cache.size() - maxEntries));
	}
	
	private void cleanup(int count) {
		synchronized (cache) {
			// cleanup invalid items
			Set<K> cleanKeys = new HashSet<K>();
			for (K key : keySet()) {
				CacheEntry<K, V> entry = cache.get(key);
				if (entry != null && !entry.isValid()) {
					cleanKeys.add(key);
				}
			}
			remove(cleanKeys);
			count -= cleanKeys.size();

			// must delete vaild items?
			if (count > 0) {
				List<CacheEntry<K, V>> entries = new LinkedList(cache.values());
				Collections.sort(entries, CacheEntry.getComparator(cleanupStrategy));
				for (int i = 0; i < count; i++) {
					CacheEntry<K, V> entry = entries.get(i);
					if (entry != null) {
						cache.remove(entry.getKey());
					}
				}
			}
		}
	}

	/**
	 * resets the hit and miss counter for this cache
	 */
	public void resetStatistics() {
		hitCount = 0;
		missCount = 0;
	}

	/**
	 * The hit count gets incremented everytime <code>get()</code> or
	 * <code>containsKey()</code> returns an entry or true.
	 *
	 * @return the number of successfull requests to the cache
	 */
	public long getHitCount() {
		return hitCount;
	}

	/**
	 * The miss count gets incremented everytime <code>get()</code> or
	 * <code>containsKey()</code> returns null or false.
	 *
	 * @return the number of failed requests to the cache
	 */
	public long getMissCount() {
		return missCount;
	}

	/**
	 * @return the total number of requests made to this cache
	 */
	public long getRequestCount() {
		return hitCount + missCount;
	}

	/**
	 * Looks over evry valid cached entry and averages the remaining ttl
	 *
	 * @return average remaining time to life in ms
	 */
	public long getAverageRemainingTTL() {
		cleanup();
		synchronized (cache) {
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
}
