package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.SortedLists.KeyAbsentBehavior;
import com.google.common.collect.SortedLists.KeyPresentBehavior;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.DoNotMock;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link RangeMultiMap} whose contents will never change, with many other important properties
 * detailed at {@link ImmutableCollection}.
 *
 * @author Rajin Hossain
 * @since 14.0
 */
@Beta
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public class ImmutableRangeMultiMap<K extends Comparable<?>, V> implements RangeMultiMap<K, V>, Serializable {

    private static final ImmutableRangeMultiMap<Comparable<?>, Object> EMPTY =
            new ImmutableRangeMultiMap<>(ImmutableList.<Range<Comparable<?>>>of(), ImmutableList.of());

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new {@code
     * ImmutableRangeMultiMap}. As in {@link Builder}, overlapping ranges are not permitted.
     *
     * @since 23.1
     */
    public static <T extends @Nullable Object, K extends Comparable<? super K>, V>
    Collector<T, ?, ImmutableRangeMultiMap<K, V>> toImmutableRangeMultiMap(
            Function<? super T, Range<K>> keyFunction,
            Function<? super T, ? extends V> valueFunction) {
        return CollectCollectors.toImmutableRangeMultiMap(keyFunction, valueFunction);
    }

    /**
     * Returns an empty immutable range multimap.
     *
     * <p><b>Performance note:</b> the instance returned is a singleton.
     */
    @SuppressWarnings("unchecked")
    public static <K extends Comparable<?>, V> ImmutableRangeMultiMap<K, V> of() {
        return (ImmutableRangeMultiMap<K, V>) EMPTY;
    }

    /** Returns an immutable range multimap mapping a single range to a single value. */
    public static <K extends Comparable<?>, V> ImmutableRangeMultiMap<K, V> of(Range<K> range, V value) {
        return new ImmutableRangeMultiMap<>(ImmutableList.of(range), ImmutableList.of(value));
    }

    @SuppressWarnings("unchecked")
    public static <K extends Comparable<?>, V> ImmutableRangeMultiMap<K, V> copyOf(
            RangeMultiMap<K, ? extends V> rangeMultiMap) {
        if (rangeMultiMap instanceof ImmutableRangeMultiMap) {
            return (ImmutableRangeMultiMap<K, V>) rangeMultiMap;
        }
        Map<Range<K>, ? extends V> map = rangeMultiMap.asMapOfRanges();
        ImmutableList.Builder<Range<K>> rangesBuilder = new ImmutableList.Builder<>(map.size());
        ImmutableList.Builder<V> valuesBuilder = new ImmutableList.Builder<V>(map.size());
        for (Entry<Range<K>, ? extends V> entry : map.entrySet()) {
            rangesBuilder.add(entry.getKey());
            valuesBuilder.add(entry.getValue());
        }
        return new ImmutableRangeMultiMap<>(rangesBuilder.build(), valuesBuilder.build());
    }

    /** Returns a new builder for an immutable range multimap. */
    public static <K extends Comparable<?>, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * A builder for immutable range multimaps. Overlapping ranges are prohibited.
     *
     * @since 14.0
     */
    @DoNotMock
    public static final class Builder<K extends Comparable<?>, V> {
        private final List<Entry<Range<K>, V>> entries;

        public Builder() {
            this.entries = Lists.newArrayList();
        }

        /**
         * Associates the specified range with the specified value.
         *
         * @throws IllegalArgumentException if {@code range} is empty
         */
        @CanIgnoreReturnValue
        public Builder<K, V> put(Range<K> range, V value) {
            checkNotNull(range);
            checkNotNull(value);
            checkArgument(!range.isEmpty(), "Range must not be empty, but was %s", range);
            entries.add(Maps.immutableEntry(range, value));
            return this;
        }

        /** Copies all associations from the specified range multimap into this builder. */
        @CanIgnoreReturnValue
        public Builder<K, V> putAll(RangeMultiMap<K, ? extends V> rangeMultiMap) {
            for (Entry<Range<K>, ? extends V> entry : rangeMultiMap.asMapOfRanges().entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        @CanIgnoreReturnValue
        Builder<K, V> combine(Builder<K, V> builder) {
            entries.addAll(builder.entries);
            return this;
        }

        /**
         * Returns an {@code ImmutableRangeMultiMap} containing the associations previously added to this
         * builder.
         *
         * @throws IllegalArgumentException if any two ranges inserted into this builder overlap
         */
        public ImmutableRangeMultiMap<K, V> build() {
            Collections.sort(entries, Range.<K>rangeLexOrdering().onKeys());
            ImmutableList.Builder<Range<K>> rangesBuilder = new ImmutableList.Builder<>(entries.size());
            ImmutableList.Builder<V> valuesBuilder = new ImmutableList.Builder<V>(entries.size());
            for (int i = 0; i < entries.size(); i++) {
                Range<K> range = entries.get(i).getKey();
                if (i > 0) {
                    Range<K> prevRange = entries.get(i - 1).getKey();
                    if (range.isConnected(prevRange) && !range.intersection(prevRange).isEmpty()) {
                        throw new IllegalArgumentException(
                                "Overlapping ranges: range " + prevRange + " overlaps with entry " + range);
                    }
                }
                rangesBuilder.add(range);
                valuesBuilder.add(entries.get(i).getValue());
            }
            return new ImmutableRangeMultiMap<>(rangesBuilder.build(), valuesBuilder.build());
        }
    }

    private final transient ImmutableList<Range<K>> ranges;
    private final transient ImmutableList<V> values;

    ImmutableRangeMultiMap(ImmutableList<Range<K>> ranges, ImmutableList<V> values) {
        this.ranges = ranges;
        this.values = values;
    }

    @Override
    @CheckForNull
    public V get(K key) {
        int index =
                SortedLists.binarySearch(
                        ranges,
                        Range.<K>lowerBoundFn(),
                        Cut.belowValue(key),
                        KeyPresentBehavior.ANY_PRESENT,
                        KeyAbsentBehavior.NEXT_LOWER);
        if (index == -1) {
            return null;
        } else {
            Range<K> range = ranges.get(index);
            return range.contains(key) ? values.get(index) : null;
        }
    }

    @Override
    @CheckForNull
    public Entry<Range<K>, V> getEntry(K key) {
        int index =
                SortedLists.binarySearch(
                        ranges,
                        Range.<K>lowerBoundFn(),
                        Cut.belowValue(key),
                        KeyPresentBehavior.ANY_PRESENT,
                        KeyAbsentBehavior.NEXT_LOWER);
        if (index == -1) {
            return null;
        } else {
            Range<K> range = ranges.get(index);
            return range.contains(key) ? Maps.immutableEntry(range, values.get(index)) : null;
        }
    }

    @Override
    public Range<K> span() {
        if (ranges.isEmpty()) {
            throw new NoSuchElementException();
        }
        Range<K> firstRange = ranges.get(0);
        Range<K> lastRange = ranges.get(ranges.size() - 1);
        return Range.create(firstRange.lowerBound, lastRange.upperBound);
    }

    /**
     * Guaranteed to throw an exception and leave the {@code RangeMultiMap} unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    @DoNotCall("Always throws UnsupportedOperationException")
    public final void put(Range<K> range, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the {@code RangeMultiMap} unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    @DoNotCall("Always throws UnsupportedOperationException")
    public final void putCoalescing(Range<K> range, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the {@code RangeMultiMap} unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    @DoNotCall("Always throws UnsupportedOperationException")
    public final void putAll(RangeMultiMap<K, ? extends V> rangeMultiMap) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the {@code RangeMultiMap} unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    @DoNotCall("Always throws UnsupportedOperationException")
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the {@code RangeMultiMap} unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    @DoNotCall("Always throws UnsupportedOperationException")
    public final void remove(Range<K> range) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the {@code RangeMultiMap} unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    @DoNotCall("Always throws UnsupportedOperationException")
    public final void merge(
            Range<K> range,
            @CheckForNull V value,
            BiFunction<? super V, ? super @Nullable V, ? extends @Nullable V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableMap<Range<K>, V> asMapOfRanges() {
        if (ranges.isEmpty()) {
            return ImmutableMap.of();
        }
        RegularImmutableSortedSet<Range<K>> rangeSet =
                new RegularImmutableSortedSet<>(ranges, Range.<K>rangeLexOrdering());
        return new ImmutableSortedMap<>(rangeSet, values);
    }

    @Override
    public ImmutableMap<Range<K>, V> asDescendingMapOfRanges() {
        if (ranges.isEmpty()) {
            return ImmutableMap.of();
        }
        RegularImmutableSortedSet<Range<K>> rangeSet =
                new RegularImmutableSortedSet<>(ranges.reverse(), Range.<K>rangeLexOrdering().reverse());
        return new ImmutableSortedMap<>(rangeSet, values.reverse());
    }

    @Override
    public ImmutableRangeMultiMap<K, V> subRangeMultiMap(final Range<K> range) {
        if (checkNotNull(range).isEmpty()) {
            return ImmutableRangeMultiMap.of();
        } else if (ranges.isEmpty() || range.encloses(span())) {
            return this;
        }
        int lowerIndex =
                SortedLists.binarySearch(
                        ranges,
                        Range.<K>upperBoundFn(),
                        range.lowerBound,
                        KeyPresentBehavior.FIRST_AFTER,
                        KeyAbsentBehavior.NEXT_HIGHER);
        int upperIndex =
                SortedLists.binarySearch(
                        ranges,
                        Range.<K>lowerBoundFn(),
                        range.upperBound,
                        KeyPresentBehavior.ANY_PRESENT,
                        KeyAbsentBehavior.NEXT_HIGHER);
        if (lowerIndex >= upperIndex) {
            return ImmutableRangeMultiMap.of();
        }
        final int off = lowerIndex;
        final int len = upperIndex - lowerIndex;
        ImmutableList<Range<K>> subRanges =
                new ImmutableList<Range<K>>() {
                    @Override
                    public int size() {
                        return len;
                    }

                    @Override
                    public Range<K> get(int index) {
                        checkElementIndex(index, len);
                        if (index == 0 || index == len - 1) {
                            return ranges.get(index + off).intersection(range);
                        } else {
                            return ranges.get(index + off);
                        }
                    }

                    @Override
                    boolean isPartialView() {
                        return true;
                    }
                };
        final ImmutableRangeMultiMap<K, V> outer = this;
        return new ImmutableRangeMultiMap<K, V>(subRanges, values.subList(lowerIndex, upperIndex)) {
            @Override
            public ImmutableRangeMultiMap<K, V> subRangeMultiMap(Range<K> subRange) {
                if (range.isConnected(subRange)) {
                    return outer.subRangeMultiMap(subRange.intersection(range));
                } else {
                    return ImmutableRangeMultiMap.of();
                }
            }
        };
    }

    @Override
    public int hashCode() {
        return asMapOfRanges().hashCode();
    }

    @Override
    public boolean equals(@CheckForNull Object o) {
        if (o instanceof RangeMultiMap) {
            RangeMultiMap<?, ?> rangeMultiMap = (RangeMultiMap<?, ?>) o;
            return asMapOfRanges().equals(rangeMultiMap.asMapOfRanges());
        }
        return false;
    }

    @Override
    public String toString() {
        return asMapOfRanges().toString();
    }

    /**
     * This class is used to serialize ImmutableRangeMultiMap instances. Serializes the {@link
     * #asMapOfRanges()} form.
     */
    private static class SerializedForm<K extends Comparable<?>, V> implements Serializable {

        private final ImmutableMap<Range<K>, V> mapOfRanges;

        SerializedForm(ImmutableMap<Range<K>, V> mapOfRanges) {
            this.mapOfRanges = mapOfRanges;
        }

        Object readResolve() {
            if (mapOfRanges.isEmpty()) {
                return of();
            } else {
                return createRangeMultiMap();
            }
        }

        Object createRangeMultiMap() {
            Builder<K, V> builder = new Builder<>();
            for (Entry<Range<K>, V> entry : mapOfRanges.entrySet()) {
                builder.put(entry.getKey(), entry.getValue());
            }
            return builder.build();
        }

        private static final long serialVersionUID = 0;
    }

    Object writeReplace() {
        return new SerializedForm<>(asMapOfRanges());
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    private static final long serialVersionUID = 0;
}
