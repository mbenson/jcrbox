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
package jcrbox.literal;

import jcrbox.util.EnumHelper;

/**
 * Broad interface to expose JCR entities as literal using Java {@code enum}s.
 * 
 * @param <E>
 *            self type
 */
public interface JcrLiteral<E extends Enum<E> & JcrLiteral<E>> {

    /**
     * Get this {@link JcrLiteral} as an {@link Enum} constant.
     *
     * @return {@code E}
     */
    @SuppressWarnings("unchecked")
    default E asEnum() {
        return (E) this;
    }

    /**
     * Get the JCR namespace of this {@link JcrLiteral}.
     * 
     * @return {@link String}
     */
    default String namespace() {
        return JcrNamespace.Helper.getNamespace(asEnum().getDeclaringClass());
    }

    /**
     * Get the basename of this {@link JcrLiteral}.
     * 
     * @return {@link String}
     * @see EnumHelper#basename(Enum)
     */
    default String basename() {
        return EnumHelper.basename(asEnum());
    }

    /**
     * Get the fully-qualified name of this {@link JcrLiteral}.
     *
     * @return {@link String}
     * @see #basename()
     * @see JcrNamespace.Helper#format(Class, String)
     */
    default String fullname() {
        return JcrNamespace.Helper.format(getClass(), basename());
    }
}
