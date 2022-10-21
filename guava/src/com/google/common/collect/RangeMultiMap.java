package com.google.common.collect;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@ElementTypesAreNonnullByDefault
public interface RangeMultiMap<K extends Comparable, V> {

    /**
     * Returns the number of key-value pairs in this RangeMultimap.
     */
    int size();

    /**
     * Returns {@code true} if this RangeMultimap contains no key-value pairs. Equivalent to {@code size()
     * == 0}, but can in some cases be more efficient.
     */
    boolean isEmpty();

    /**
     * Returns the minimal range {@linkplain Range#encloses(Range) enclosing} the ranges in this
     * {@code RangeMultiMap}.
     *
     * @throws NoSuchElementException if this RangeMulti map is empty
     */
    Range<K> span();

    /**
     * Stores a key-value pair in this range multi map.
     *
     * <p>Some multimap implementations allow duplicate key-value pairs, in which case {@code put}
     * always adds a new key-value pair and increases the multimap size by 1. Other implementations
     * prohibit duplicates, and storing a key-value pair that's already in the multimap has no effect.
     *
     * @return {@code true} if the method increased the size of the multimap, or {@code false} if the
     *     RangeMultimap already contained the key-value pair and doesn't allow duplicates
     */
    @CheckForNull
    boolean put(Range<K> range, V value);

    void putAll(Range<K> range, Iterable<? extends V> values);

    @CheckForNull
    Collection<V> get(K key);

    @CheckForNull
    Collection<V> get(Range<K> range);

    @CheckForNull
    Entry<Range<K>, Collection<V>> getEntry(K key);

    Collection<V> subRangeMap(Range<K> range);

    @CheckForNull
    boolean remove(K key, V value);

    @CheckForNull
    boolean remove(Range<K> range, V value);


    void clear();

    @Override
    boolean equals(@CheckForNull Object o);

    @Override
    int hashCode();

    @Override
    String toString();
}
