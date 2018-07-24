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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.text.WordUtils;

import jcrbox.literal.JcrNode;
import jcrbox.literal.JcrProperty;

/**
 * Utility class for {@link JcrNode} and {@link JcrProperty} support.
 */
public class EnumHelper {

    /**
     * Transform an underscore-delimited {@link String} to camel case, tokenizing on the underscores.
     * 
     * @param s
     * @return camel case {@link String}, {@code null} if {@code s} is {@code null}
     */
    public static String underscoreDelimitedToCamelCase(String s) {
        return s == null ? null : WordUtils
            .uncapitalize(Stream.of(s.split("_")).map(WordUtils::capitalizeFully).collect(Collectors.joining()));
    }

    /**
     * Transform an enum name from the expected underscore-delimited caps to camel case.
     * 
     * @param e
     * @return {@link String}
     */
    public static <E extends Enum<E>> String basename(E e) {
        return underscoreDelimitedToCamelCase(e.name());
    }

    /**
     * Read the annotation of the specified type from the supplied enumeration constant.
     *
     * @param e
     * @param annotationType
     * @return {@code A}
     */
    public static <A extends Annotation> A getAnnotation(Enum<?> e, Class<A> annotationType) {
        return getField(e).getAnnotation(annotationType);
    }

    /**
     * Learn whether an annotation of the specified type is declared on the supplied enumeration constant.
     *
     * @param e
     * @param annotationType
     * @return {@code boolean}
     */
    public static boolean isAnnotationPresent(Enum<?> e, Class<? extends Annotation> annotationType) {
        return getField(e).isAnnotationPresent(annotationType);
    }

    /**
     * Get a {@link Predicate} to test an enumeration constant for the presence of an annotation of the specified type.
     *
     * @param annotationType
     * @return {@link Predicate}
     * @see #isAnnotationPresent(Enum, Class)
     */
    public static Predicate<Enum<?>> isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return e -> isAnnotationPresent(e, annotationType);
    }

    /**
     * Get a {@link Supplier} that returns the value obtained by applying the specified enumeration constant to the
     * specified {@link Function}.
     *
     * @param e
     * @param fn
     * @return {@link Supplier}
     */
    public static <E extends Enum<E>, R> Supplier<R> bindArgument(E e, Function<? super E, R> fn) {
        return () -> fn.apply(e);
    }

    private static Field getField(Enum<?> e) {
        try {
            return e.getDeclaringClass().getField(e.name());
        } catch (NoSuchFieldException | SecurityException ex) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    private EnumHelper() {
    }
}
