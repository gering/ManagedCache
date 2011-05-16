
package de.robertgering;

/**
 *
 * @author robert gering
 */
public enum CleanupStrategy {
    CLEANUP_BY_LAST_ACCESS,
    CLEANUP_BY_LAST_ADDED,
    CLEANUP_BY_REMAINING_TTL,
    CLEANUP_BY_ACCESS_COUNT,
}
