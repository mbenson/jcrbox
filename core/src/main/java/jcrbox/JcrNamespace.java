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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Annotation to set a JCR namespace on a Java type containing modeled nodes, properties or queries.
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface JcrNamespace {

    /**
     * Helper class for the {@link JcrNamespace} annotation. Intended for internal use; {@code public} only for the sake
     * of inter-package visibility.
     */
    public static class Helper {

        private static final Map<Class<?>, String> MAP = new HashMap<>();

        /**
         * Get the namespace of a given type, either declared with {@link JcrNamespace} or {@code ""}.
         *
         * @param namespacedType
         * @return {@link String}
         */
        public static String getNamespace(Class<?> namespacedType) {
            return MAP.computeIfAbsent(namespacedType, e -> Optional.ofNullable(e.getAnnotation(JcrNamespace.class))
                .map(JcrNamespace::value).map(String::trim).orElse(""));
        }

        /**
         * Qualify the specified {@code basename} with any declared {@link JcrNamespace}.
         *
         * @param namespacedType
         * @param basename
         * @return qualified name or {@code basename} if no namespace was declared.
         */
        public static String format(Class<?> namespacedType, String basename) {
            final String ns = getNamespace(namespacedType);
            return StringUtils.isBlank(ns) ? basename : String.format("{%s}%s", ns, basename);
        }

        private Helper() {
        }
    }

    /**
     * The declared namespace value.
     *
     * @return {@link String}
     */
    String value();
}
