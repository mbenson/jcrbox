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
package jcrbox.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Lazy {@link Supplier}.
 *
 * @param <T>
 *            supplied type
 */
public class Lazy<T> implements Supplier<T> {
    private T value;
    private volatile Supplier<T> init;

    /**
     * Create a new {@link Lazy} instance whose value will come, when requested, from {@code init}.
     * 
     * @param init
     */
    public Lazy(Supplier<T> init) {
        reset(init);
    }

    /**
     * Reset the {@link Lazy} instance with a new backing {@link Supplier}.
     * 
     * @param init
     * @return {@code this}
     */
    public Lazy<T> reset(Supplier<T> init) {
        this.init = Objects.requireNonNull(init);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T get() {
        if (init != null) {
            synchronized (this) {
                if (init != null) {
                    value = init.get();
                    init = null;
                }
            }
        }
        return value;
    }

    /**
     * Get an {@link Optional} wrapping the <em>current</em> {@link Lazy} value. The returned object <em>does not</em>
     * "track" the state of the {@link Lazy} instance.
     * 
     * @return {@link Optional}
     */
    public Optional<T> optional() {
        return Optional.ofNullable(value);
    }

    /**
     * Wrap {@code delegate} to return an object that will allow our lazy value to consume an object of type {@code U}
     * when {@link Consumer#accept(Object)} is invoked.
     * 
     * @param delegate
     * @return {@link Consumer}
     */
    public <U> Consumer<U> consumer(BiConsumer<? super T, ? super U> delegate) {
        return u -> delegate.accept(get(), u);
    }
}
