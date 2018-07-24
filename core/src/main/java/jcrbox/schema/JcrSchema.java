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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jcrbox.literal.JcrSource;

/**
 * Defines a lexicon of sources among which generated selector names will be unique.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JcrSchema {

    /**
     * Member {@link JcrSource} types.
     * 
     * @return {@link Class}[]
     */
    Class<? extends JcrSource<?>>[] value();

    /**
     * {@link SelectorNameStrategy} type for this schema.
     * 
     * @return {@link Class}
     */
    Class<? extends SelectorNameStrategy> selectorNameStrategy() default DefaultSelectorNameStrategy.class;
}
