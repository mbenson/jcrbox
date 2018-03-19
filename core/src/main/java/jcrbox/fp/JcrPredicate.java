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

import java.util.function.Predicate;

import javax.jcr.RepositoryException;

/**
 * JCR {@link Predicate}.
 *
 * @param <T>
 *            tested type
 */
@FunctionalInterface
public interface JcrPredicate<T> extends Predicate<T> {

    /**
     * Test the value.
     * 
     * @param t
     * @return {@code boolean}
     * @throws RepositoryException
     */
    boolean jcrTest(T t) throws RepositoryException;

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean test(T t) {
        try {
            return jcrTest(t);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
