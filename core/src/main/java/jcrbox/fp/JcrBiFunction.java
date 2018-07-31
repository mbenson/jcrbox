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

import java.util.function.BiFunction;

import javax.jcr.RepositoryException;

/**
 * JCR {@link BiFunction}.
 *
 * @param <T>
 *            LHS input type
 * @param <U>
 *            RHS input type
 * @param <R>
 *            output type
 */
@FunctionalInterface
public interface JcrBiFunction<T, U, R> extends BiFunction<T, U, R> {

    /**
     * Apply the function to the arguments.
     *
     * @param t
     * @param u
     * @return {@code R}
     * @throws RepositoryException
     */
    R jcrApply(T t, U u) throws RepositoryException;

    /**
     * {@inheritDoc}
     */
    @Override
    default R apply(T t, U u) {
        try {
            return jcrApply(t, u);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
