package de.robertgering.managedcache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author robse
 */
public class CleanupTest {

    @Test
    public void cleanupByLastAddedTest() throws InterruptedException {
        final ManagedCache<String, String> managedCache = new ManagedCache<String, String>(5, 100000, CleanupStrategy.CLEANUP_BY_LAST_ADDED);

        for (int i = 0; i < 6; i++) {
            managedCache.put("key" + i, "value" + i);
            Thread.sleep(1);
        }

        assertFalse(managedCache.containsKey("key0"));
        assertTrue(managedCache.containsKey("key1"));
        assertTrue(managedCache.containsKey("key2"));
        assertTrue(managedCache.containsKey("key3"));
        assertTrue(managedCache.containsKey("key4"));
        assertTrue(managedCache.containsKey("key5"));

        managedCache.put("key0", "value0");

        assertTrue(managedCache.containsKey("key0"));
        assertFalse(managedCache.containsKey("key1"));
    }

    @Test
    public void cleanupByLastAccessTest() throws InterruptedException {
        final ManagedCache<String, String> managedCache = new ManagedCache<String, String>(5, 100000, CleanupStrategy.CLEANUP_BY_LAST_ACCESS);

        for (int i = 0; i < 5; i++) {
            managedCache.put("key" + i, "value" + i);
            Thread.sleep(1);
        }

        managedCache.get("key0");
        Thread.sleep(1);
        managedCache.get("key1");
        Thread.sleep(1);
        managedCache.put("key5", "value5");

        assertTrue(managedCache.containsKey("key0"));
        assertTrue(managedCache.containsKey("key1"));
        assertFalse(managedCache.containsKey("key2"));
        assertTrue(managedCache.containsKey("key3"));
        assertTrue(managedCache.containsKey("key4"));
        assertTrue(managedCache.containsKey("key5"));
    }

    @Test
    public void cleanupByRemainingTTLTest() {
        final ManagedCache<String, String> managedCache = new ManagedCache<String, String>(5, 1000, CleanupStrategy.CLEANUP_BY_REMAINING_TTL);

        managedCache.put("key0", "value");
        managedCache.put("key1", "value");
        managedCache.put("key2", "value", 500);
        managedCache.put("key3", "value");
        managedCache.put("key4", "value");
        managedCache.put("key5", "value");

        assertTrue(managedCache.containsKey("key0"));
        assertTrue(managedCache.containsKey("key1"));
        assertFalse(managedCache.containsKey("key2"));
        assertTrue(managedCache.containsKey("key3"));
        assertTrue(managedCache.containsKey("key4"));
        assertTrue(managedCache.containsKey("key5"));
    }

    @Test
    public void cleanupByAccessCountTest() throws InterruptedException {
        final ManagedCache<String, String> managedCache = new ManagedCache<String, String>(5, 100000, CleanupStrategy.CLEANUP_BY_ACCESS_COUNT);

        for (int i = 0; i < 5; i++) {
            managedCache.put("key" + i, "value" + i);
        }

        managedCache.get("key0");
        managedCache.get("key1");
        managedCache.get("key2");
        managedCache.get("key4");
        //managedCache.get("key5");

        managedCache.put("key5", "value5");

        assertTrue(managedCache.containsKey("key0"));
        assertTrue(managedCache.containsKey("key1"));
        assertTrue(managedCache.containsKey("key2"));
        assertTrue(managedCache.containsKey("key4"));
        assertTrue(managedCache.containsKey("key5"));
        assertFalse(managedCache.containsKey("key3"));
    }
}
