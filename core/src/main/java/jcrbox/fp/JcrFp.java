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
package jcrbox.fp;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Utility class for JCR functional programming.
 */
public class JcrFp {

    /**
     * Adapt a {@link JcrBiConsumer}.
     *
     * @param arg
     * @return {@link BiConsumer}
     */
    public static <T, U> BiConsumer<T, U> adapt(JcrBiConsumer<T, U> arg) {
        return arg;
    }

    /**
     * Adapt a {@link JcrBiFunction}.
     *
     * @param arg
     * @return {@link BiFunction}
     */
    public static <T, U, R> BiFunction<T, U, R> adapt(JcrBiFunction<T, U, R> arg) {
        return arg;
    }

    /**
     * Adapt a {@link JcrConsumer}.
     *
     * @param arg
     * @return {@link Consumer}
     */
    public static <T> Consumer<T> adapt(JcrConsumer<T> arg) {
        return arg;
    }

    /**
     * Adapt a {@link JcrFunction}.
     *
     * @param arg
     * @return {@link Function}
     */
    public static <T, R> Function<T, R> adapt(JcrFunction<T, R> arg) {
        return arg;
    }

    /**
     * Adapt a {@link JcrIterable}.
     *
     * @param arg
     * @return {@link Iterable}
     */
    public static <T> Iterable<T> adapt(JcrIterable<T> arg) {
        return arg;
    }

    /**
     * Adapt a {@link JcrOperator}.
     *
     * @param arg
     * @return {@link UnaryOperator}
     */
    public static <T> UnaryOperator<T> adapt(JcrOperator<T> arg) {
        return arg;
    }

    /**
     * Adapt a {@link JcrPredicate}.
     *
     * @param arg
     * @return {@link Predicate}
     */
    public static <T> Predicate<T> adapt(JcrPredicate<T> arg) {
        return arg;
    }

    /**
     * Adapt a {@link JcrSupplier}.
     *
     * @param arg
     * @return {@link Supplier}
     */
    public static <T> Supplier<T> adapt(JcrSupplier<T> arg) {
        return arg;
    }

    private JcrFp() {
    }
}
