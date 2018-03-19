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
package jcrbox.query;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;

import jcrbox.JcrNamespace;

/**
 * Marker interface for a class that represents a JCR query in a typesafe manner.
 */
public interface JcrQuery {

    /**
     * For e.g. a {@link JcrQuery} class that is a nested class, an outer class can be annotated @{@link PathRoot} to
     * signify that the query path implied should go no higher.
     * 
     * @see JcrQuery#path(Class)
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface PathRoot {
    }

    /**
     * Build a query path for the specified {@link JcrQuery} type. This will include a representation of each class from
     * {@code queryType} through its outer classes until one is reached that is annotated @{@link PathRoot}.
     * 
     * @param queryType
     * @return {@link String}
     * @see JcrNamespace.Helper#format(Class, String)
     */
    static String path(Class<? extends JcrQuery> queryType) {
        final Deque<String> path = new ArrayDeque<>();
        Class<?> c = queryType;
        while (c != null) {
            path.push(JcrNamespace.Helper.format(c, c.getSimpleName()));
            if (c.isAnnotationPresent(PathRoot.class)) {
                break;
            }
            c = c.getEnclosingClass();
        }
        return path.stream().collect(Collectors.joining("/", "/", ""));
    }
}
