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

/**
 * Represents a property of a node.
 *
 * @param <N>
 *            {@link JcrNode} type
 * @param <P>
 *            {@link JcrProperty} type
 */
public class QualifedProperty<N extends Enum<N> & JcrNode<N>, P extends Enum<P> & JcrProperty<P>> {

    /**
     * The {@code N} node of this {@link QualifedProperty}.
     */
    public final N node;

    /**
     * The {@code P} property of this {@link QualifedProperty}.
     */
    public final P property;

    /**
     * Create a new {@link QualifedProperty} instance.
     *
     * @param node
     * @param property
     */
    public QualifedProperty(N node, P property) {
        super();
        this.node = node;
        this.property = property;
    }
}
