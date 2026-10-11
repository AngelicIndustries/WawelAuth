package org.fentanylsolutions.wawelauth.client.skin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongFunction;

/**
 * Hands out one value per key and replaces it, under a new revision, whenever the key's source changes. Revisions
 * come from one counter, so they are unique across keys. Keeps the most recently used keys only. Not thread-safe.
 */
final class RevisionTracker<K, S, V> {

    private final AtomicLong revisions = new AtomicLong();
    private final Map<K, Slot<S, V>> entries;

    RevisionTracker(int maxEntries) {
        this.entries = new LinkedHashMap<K, Slot<S, V>>(16, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, Slot<S, V>> eldest) {
                return size() > maxEntries;
            }
        };
    }

    /** The value for this key and source, built by {@code factory} with a fresh revision if the source changed. */
    V get(K key, S source, LongFunction<V> factory) {
        Slot<S, V> slot = entries.get(key);
        if (slot != null && slot.source.equals(source)) {
            return slot.value;
        }
        V value = factory.apply(revisions.incrementAndGet());
        entries.put(key, new Slot<>(source, value));
        return value;
    }

    void clear() {
        entries.clear();
    }

    int size() {
        return entries.size();
    }

    private static final class Slot<S, V> {

        final S source;
        final V value;

        Slot(S source, V value) {
            this.source = source;
            this.value = value;
        }
    }
}
