/*
 *  Copyright the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package jcrbox;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.jcr.RangeIterator;

import jcrbox.fp.JcrFunction;
import jcrbox.fp.JcrOperator;
import jcrbox.fp.JcrSupplier;

/**
 * Generic form of {@link RangeIterator}.
 *
 * @param <T>
 */
public interface RangedIterator<T> extends Iterator<T> {

    /**
     * Adapter between {@link RangeIterator} and {@link RangedIterator}, supporting optional element type conversion.
     * Intended for internal use; {@code public} only for the sake of inter-package visibility.
     *
     * @param <T>
     * @param <R>
     */
    public static class Adapter<T, R> implements RangedIterator<R> {

        private final RangeIterator wrapped;
        private final JcrFunction<? super T, ? extends R> function;

        /**
         * Create a new {@link Adapter} instance.
         *
         * @param wrapped
         * @param function
         */
        protected Adapter(RangeIterator wrapped, JcrFunction<? super T, ? extends R> function) {
            super();
            this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
            this.function = Objects.requireNonNull(function, "function");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return wrapped.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public R next() {
            @SuppressWarnings("unchecked")
            final T next = (T) wrapped.next();
            return function.apply(next);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long position() {
            return wrapped.getPosition();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long size() {
            return wrapped.getSize();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void skip(long count) {
            wrapped.skip(count);
        }
    }

    /**
     * {@link RangedIterator} wrapping a {@link List}.
     *
     * @param <T>
     *            element type
     */
    static class OfList<T> implements RangedIterator<T> {

        private final List<T> wrapped;
        private long pos = 0;

        /**
         * Create a new {@link RangedIterator.OfList}.
         */
        OfList(List<T> wrapped) {
            this.wrapped = wrapped;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return pos < wrapped.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T next() {
            try {
                return wrapped.get((int) pos++);
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long position() {
            return pos;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long size() {
            return wrapped.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void skip(long count) {
            pos += count;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEmpty() {
            return wrapped.isEmpty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Stream<T> stream() {
            return wrapped.stream();
        }
    }

    /**
     * Get a {@link RangedIterator} wrapping a supplied {@link RangeIterator}.
     *
     * @param supplier
     * @return {@link RangedIterator}
     */
    public static <T> RangedIterator<T> of(JcrSupplier<? extends RangeIterator> supplier) {
        return new Adapter<>(supplier.get(), JcrOperator.identity());
    }

    /**
     * Map a supplied {@link RangeIterator} to a {@link RangedIterator} using the specified element mapping
     * {@link Function}.
     *
     * @param supplier
     * @param function
     * @return {@link RangedIterator}
     */
    public static <T, R> RangedIterator<R> map(JcrSupplier<? extends RangeIterator> supplier,
        JcrFunction<T, R> function) {
        return new Adapter<>(supplier.get(), function);
    }

    /**
     * Get a {@link RangedIterator} wrapping the specified {@link List}.
     * 
     * @param wrapped
     * @return {@link RangedIterator}
     */
    public static <T> RangedIterator<T> of(List<T> wrapped) {
        return new OfList<>(wrapped);
    }

    /**
     * @return {@code long}
     * @see RangeIterator#getPosition()
     */
    long position();

    /**
     * @return {@code long}
     * @see RangeIterator#getSize()
     */
    long size();

    /**
     * @param count
     * @see RangeIterator#skip(long)
     */
    void skip(long count);

    /**
     * Learn whether this {@link RangedIterator} is empty.
     * 
     * @return {@code boolean}
     */
    default boolean isEmpty() {
        return size() < 1;
    }

    /**
     * Adapt this {@link RangedIterator} to the {@link Stream} interface.
     * 
     * @return {@link Stream}
     */
    default Stream<T> stream() {
        final Stream.Builder<T> bld = Stream.builder();
        forEachRemaining(bld);
        return bld.build();
    }
}
