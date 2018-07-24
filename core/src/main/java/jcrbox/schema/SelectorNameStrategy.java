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

import java.util.Map;
import java.util.Set;

import jcrbox.literal.JcrSource;

/**
 * Strategy interface to generate unique selector names for a {@link JcrSchema} detected at some level of a nested class
 * structure. Implementations should have a default constructor.
 */
@FunctionalInterface
public interface SelectorNameStrategy {

    /**
     * Generate the selector names for the specified {@link JcrSource}s.
     * 
     * @param sources
     * @return {@link Map}
     */
    Map<JcrSource<?>, String> generateSelectors(Set<JcrSource<?>> sources);
}
