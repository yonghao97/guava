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
     * Returns the number of key-value pairs in this multimap.
     *
     * <p><b>Note:</b> this method does not return the number of <i>distinct keys</i> in the multimap,
     * which is given by {@code keySet().size()} or {@code asMap().size()}. See the opening section of
     * the {@link Multimap} class documentation for clarification.
     */
    int size();
    boolean isEmpty();

    Range<K> span();
    @CheckForNull
    void put(Range<K> range, V value);

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
