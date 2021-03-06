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

import java.util.function.Function;

import javax.jcr.RepositoryException;

/**
 * JCR {@link Function}.
 *
 * @param <T>
 *            input type
 * @param <R>
 *            output type
 */
@FunctionalInterface
public interface JcrFunction<T, R> extends Function<T, R> {

    /**
     * Apply the function to the argument.
     *
     * @param t
     * @return {@code R}
     * @throws RepositoryException
     */
    R jcrApply(T t) throws RepositoryException;

    /**
     * {@inheritDoc}
     */
    @Override
    default R apply(T t) {
        try {
            return jcrApply(t);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
