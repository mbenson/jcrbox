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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.text.WordUtils;

import jcrbox.literal.JcrSource;

/**
 * Default {@link SelectorNameStrategy}. Uses (US English) lowercase initials for every {@link Enum} constant. Repeated
 * initials will begin with the suffix {@code 2}, incrementing by 1 each time.
 */
public class DefaultSelectorNameStrategy implements SelectorNameStrategy {

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<JcrSource<?>, String> generateSelectors(Set<JcrSource<?>> sources) {
        final Map<String, AtomicInteger> counts = new HashMap<>();
        final Map<JcrSource<?>, String> result = new TreeMap<>();

        for (JcrSource<?> s : sources) {
            final String initials = WordUtils.initials(s.asEnum().name(), '_').toLowerCase(Locale.US);
            final int count = counts.computeIfAbsent(initials, k -> new AtomicInteger()).incrementAndGet();
            result.put(s, count == 1 ? initials : initials + count);
        }
        return result;
    }
}
