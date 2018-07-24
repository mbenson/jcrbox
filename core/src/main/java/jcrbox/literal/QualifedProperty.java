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

import java.util.Objects;

/**
 * Represents a property of a node.
 *
 * @param <S>
 *            {@link JcrSource} type
 * @param <P>
 *            {@link JcrProperty} type
 */
public class QualifedProperty<S extends Enum<S> & JcrSource<S>, P extends Enum<P> & JcrProperty<P>> {

    /**
     * The {@code S} source of this {@link QualifedProperty}.
     */
    public final S source;

    /**
     * The {@code P} property of this {@link QualifedProperty}.
     */
    public final P property;

    /**
     * Create a new {@link QualifedProperty} instance.
     *
     * @param source
     * @param property
     */
    public QualifedProperty(S source, P property) {
        super();
        this.source = Objects.requireNonNull(source, "source");
        this.property = Objects.requireNonNull(property, "property");
    }
}
