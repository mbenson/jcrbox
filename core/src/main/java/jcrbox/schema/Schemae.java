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
package jcrbox.schema;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.AnnotationUtils;
import org.apache.commons.lang3.EnumUtils;

import jcrbox.literal.JcrSource;

/**
 * Utility class for managing schemae.
 *
 * @see JcrSchema
 */
public class Schemae {
    private static final ConcurrentMap<Class<? extends JcrSource<?>>, JcrSchema> SCHEMAE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<JcrSchema, Map<JcrSource<?>, String>> SELECTOR_NAMES = new ConcurrentHashMap<>();

    /**
     * Get the map of selector names for {@code sourceType}, taking into account {@link JcrSchema} annotations.
     *
     * @param sourceType
     * @return {@link Map}
     */
    public static Map<JcrSource<?>, String> getSelectorNames(Class<? extends JcrSource<?>> sourceType) {
        final JcrSchema schema = SCHEMAE.computeIfAbsent(Objects.requireNonNull(sourceType), Schemae::schema);
        return SELECTOR_NAMES.computeIfAbsent(schema, Schemae::selectorNames);
    }

    private static JcrSchema schema(Class<? extends JcrSource<?>> sourceType) {
        Class<?> t = sourceType;
        while (t != null) {
            if (t.isAnnotationPresent(JcrSchema.class)) {
                return t.getAnnotation(JcrSchema.class);
            }
            t = t.getEnclosingClass();
        }
        return implicitSchema(sourceType);
    }

    private static Map<JcrSource<?>, String> selectorNames(JcrSchema schema) {
        final SelectorNameStrategy selectorNameStrategy;
        try {
            selectorNameStrategy = schema.selectorNameStrategy().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Cannot instantiate %s", schema.selectorNameStrategy()),
                e);
        }
        return selectorNameStrategy.generateSelectors(sources(schema));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Set<JcrSource<?>> sources(JcrSchema schema) {
        final Set<JcrSource<?>> result = new HashSet<>();

        for (Class<? extends JcrSource<?>> t : schema.value()) {
            EnumUtils.getEnumList((Class<? extends Enum>) t).forEach(e -> result.add((JcrSource<?>) e));
        }
        return result;
    }

    private static JcrSchema implicitSchema(Class<? extends JcrSource<?>> sourceType) {
        return (JcrSchema) Proxy.newProxyInstance(JcrSchema.class.getClassLoader(), new Class[] { JcrSchema.class },
            new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (matches(method, "equals", Object.class)) {
                        return args[0] instanceof Annotation
                            && AnnotationUtils.equals((Annotation) args[0], (Annotation) proxy);
                    }
                    if (matches(method, "hashCode")) {
                        return AnnotationUtils.hashCode((Annotation) proxy);
                    }
                    if (matches(method, "toString")) {
                        return AnnotationUtils.toString((Annotation) proxy);
                    }
                    if (matches(method, "annotationType")) {
                        return JcrSchema.class;
                    }
                    if (matches(method, "value")) {
                        return new Class[] { sourceType };
                    }
                    if (method.getDeclaringClass().equals(Object.class)) {
                        return method.invoke(this, args);
                    }
                    return Optional.ofNullable(method.getDefaultValue()).orElse(null);
                }
            });
    }

    private static boolean matches(Method m, String name, Class<?>... parameterTypes) {
        return m != null && m.getName().equals(name) && Arrays.equals(m.getParameterTypes(), parameterTypes);
    }

    private Schemae() {
    }
}
