/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.common.collect;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A collection that maps a disjoint nonempty range to <i> multi</i> values. Each value can associate with multi ranges,
 * while each range is not allowed contain duplicate value.
 * (a.b) -> [1,2,3];
 * (c,d) -> [2,3,4,5]
 * (e,f) -> [3,6]
 *
 * @author Yonghao Lu
 * */
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

    /**
     * Stores a range-value pair in this RangeMultimap for each of {@code values}, all using the same range,
     * {@code key}. Equivalent to (but expected to be more efficient than):
     *
     * <pre>{@code
     * for (V value : values) {
     *   put(range, value);
     * }
     * }</pre>
     *
     * <p>In particular, this is a no-op if {@code values} is empty.
     *
     * @return {@code true} if the multimap changed
     */
    boolean putAll(Range<K> range, Iterable<? extends V> values);

    /**
     * Returns a view collection of the values associated with {@code range} in this RangeMultimap, if any.
     * Note that when {@code containsKey(key)} is false, this returns an empty collection, not {@code
     * null}.
     *
     * <p>Changes to the returned collection will update the underlying multimap, and vice versa.
     */
    Collection<V> get(@ParametricNullness Range<K> range);

    /**
     * Returns the range containing this key and its associated value, if such a range is present in
     * the RangeMultiMap, or {@code null} otherwise.
     */
    @CheckForNull
    Entry<Range<K>, Collection<V>> getEntry(K key);

    /**
     * Returns a view of the part of this range map that intersects with {@code range}.
     *
     * <p>For example, if {@code RangeMultiMap} had the entries {@code [1, 5] => "a","b", (6, 8) => "a","b","c",
     * (10, ∞) => "d"} then {@code RangeMultiMap.subRangeMap(Range.open(3, 12))} would return a range map
     * with the entries {@code (3, 5] => "a","b", (6, 8) => "a","b","c", (10, 12) => "d"}.
     *
     * <p>The returned range map will throw an {@link IllegalArgumentException} on an attempt to
     * insert a range not {@linkplain Range#encloses(Range) enclosed} by {@code range}.
     */
    Collection<V> subRangeMap(Range<K> range);

    /**
     * Removes a single range-value pair within the range {@code range} and the value {@code value} from this
     * multimap, if such exists. If multiple key-value pairs in the multimap fit this description,
     * which one is removed is unspecified.
     *
     * @return {@code true} if the RangeMultiMap changed
     */
    @CheckForNull
    boolean remove(Range<K> range, V value);


    /** Removes all associations from this range multi map (optional operation). */
    void clear();

    @Override
    boolean equals(@CheckForNull Object o);

    @Override
    int hashCode();

    /** Returns a readable string representation of this map. */
    @Override
    String toString();
}
