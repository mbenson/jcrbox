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

import java.util.function.Consumer;

import javax.jcr.RepositoryException;

/**
 * JCR {@link Consumer}.
 *
 * @param <T>
 *            accepted type
 */
@FunctionalInterface
public interface JcrConsumer<T> extends Consumer<T> {

    /**
     * Return a NOOP {@link JcrConsumer}.
     *
     * @return {@link JcrConsumer}
     */
    public static <X> JcrConsumer<X> noop() {
        return x -> {
        };
    }

    /**
     * Accept the argument.
     *
     * @param t
     * @throws RepositoryException
     */
    void jcrAccept(T t) throws RepositoryException;

    /**
     * {@inheritDoc}
     */
    @Override
    default void accept(T t) {
        try {
            jcrAccept(t);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
