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

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.lang3.Validate;

import jcrbox.fp.JcrConsumer;
import jcrbox.fp.JcrFunction;
import jcrbox.fp.JcrOperator;
import jcrbox.fp.JcrSupplier;

/**
 * Defines a set of convenience methods around a {@link Node}.
 */
public class WithNode {

    private final Jcr jcr;
    private final Node target;

    /**
     * Work with the specified {@link Node}.
     *
     * @param jcr
     * @param node
     */
    WithNode(Jcr jcr, Node node) {
        this.jcr = Validate.notNull(jcr, "jcr");
        target = Validate.notNull(node, "node");
    }

    /**
     * Get the target {@link Node}.
     *
     * @return {@link Node}
     */
    public Node getTarget() {
        return target;
    }

    /**
     * Get the parent node.
     *
     * @return {@link WithNode}
     */
    public WithNode parent() {
        return map(Node::getParent);
    }

    /**
     * Find or create the child node {@code name}.
     *
     * @param name
     * @return {@link WithNode}
     */
    public WithNode next(String name) {
        return map(node -> jcr.findOrCreateNode(node, name, (String) null, null));
    }

    /**
     * Find or create the child node {@code name}.
     *
     * @param name
     * @param type
     * @return {@link WithNode}
     */
    public WithNode next(String name, String type) {
        return map(node -> jcr.findOrCreateNode(node, name, type, type));
    }

    /**
     * Find or create the child node {@code name}.
     *
     * @param name
     * @param type
     * @return {@link WithNode}
     */
    public WithNode next(String name, JcrNode<?> type) {
        return next(name, type.nodeName());
    }

    /**
     * Find or create the child node {@code path}.
     *
     * @param path
     * @param step
     *            intermediate node type
     * @param type
     *            final node type
     * @return {@link WithNode}
     */
    public WithNode path(String path, String step, String type) {
        return map(node -> jcr.findOrCreateNode(node, path, step, type));
    }

    /**
     * Find or create the child node {@code path}.
     *
     * @param path
     * @param step
     *            intermediate node type
     * @param type
     *            final node type
     * @return {@link WithNode}
     */
    public WithNode path(Path path, String step, String type) {
        return path(path.relative().toString(), step, type);
    }

    /**
     * Find or create the child node {@code path}.
     *
     * @param path
     * @param step
     *            intermediate node type
     * @param type
     *            final node type
     * @return {@link WithNode}
     */
    public WithNode path(String path, JcrNode<?> step, JcrNode<?> type) {
        return path(path, step.nodeName(), type.nodeName());
    }

    /**
     * Find or create the child node {@code path}.
     *
     * @param path
     * @param step
     *            intermediate node type
     * @param type
     *            final node type
     * @return {@link WithNode}
     * @throws RepositoryException
     */
    public WithNode path(Path path, JcrNode<?> step, JcrNode<?> type) {
        return path(path.relative().toString(), step, type);
    }

    /**
     * Work with the {@link Node} result of invoking {@code operator} with {@link #getTarget()}.
     *
     * @param operator
     * @return {@link WithNode}
     */
    public WithNode map(JcrOperator<Node> operator) {
        final Node next = Validate.notNull(operator).apply(getTarget());
        if (next == getTarget()) {
            return this;
        }
        return new WithNode(jcr, next);
    }

    /**
     * Search for the named child node.
     *
     * @param path
     * @return {@link Optional} {@link WithNode}
     * @throws RepositoryException
     */
    public Optional<WithNode> find(String path) throws RepositoryException {
        return getTarget().hasNode(path) ? Optional.of(map(n -> n.getNode(path))) : Optional.empty();
    }

    /**
     * Search for the named child node.
     *
     * @param path
     * @return {@link Optional} {@link WithNode}
     * @throws RepositoryException
     */
    public Optional<WithNode> find(Path path) throws RepositoryException {
        return find(path.relative().toString());
    }

    /**
     * Set {@link #getTarget()}'s {@code jcr:data} property to a {@link Binary} representing the content return from
     * {@code data}.
     *
     * @param data
     * @return {@link WithNode}
     * @throws RepositoryException
     */
    public WithNode data(Supplier<InputStream> data) throws RepositoryException {
        return invoke(node -> node.setProperty(Property.JCR_DATA,
            jcr.session.getValueFactory().createBinary(Validate.notNull(data).get())));
    }

    /**
     * Set {@link #getTarget()}'s {@code property} property to the single {@link Value} returned by {@code value}.
     *
     * @param property
     * @param value
     * @return {@code this}
     * @throws RepositoryException
     */
    public WithNode set(String property, JcrFunction<ValueFactory, Value> value) throws RepositoryException {
        return invoke(
            node -> node.setProperty(property, Validate.notNull(value).jcrApply(jcr.session.getValueFactory())));
    }

    /**
     * Set {@link #getTarget()}'s {@code property} property to the single {@link Value} returned by {@code value}.
     *
     * @param property
     * @param value
     * @return {@code this}
     * @throws RepositoryException
     */
    public WithNode set(JcrProperty<?> property, JcrFunction<ValueFactory, Value> value) throws RepositoryException {
        return set(property.propertyName(), value);
    }

    /**
     * Set {@link #getTarget()}'s {@code property} property to the {@link Value} array returned by {@code value}.
     *
     * @param property
     * @param values
     * @return {@code this}
     * @throws RepositoryException
     */
    public WithNode setMulti(String property, JcrFunction<ValueFactory, Value[]> values) throws RepositoryException {
        return invoke(
            node -> node.setProperty(property, Validate.notNull(values).jcrApply(jcr.session.getValueFactory())));
    }

    /**
     * Set {@link #getTarget()}'s {@code property} property to the {@link Value} array returned by {@code value}.
     *
     * @param property
     * @param values
     * @return {@code this}
     * @throws RepositoryException
     */
    public WithNode setMulti(JcrProperty<?> property, JcrFunction<ValueFactory, Value[]> values)
        throws RepositoryException {
        return setMulti(property.propertyName(), values);
    }

    /**
     * Get the property value of {@link #getTarget()} that has {@link JcrProperty#propertyName()} of {@code property}.
     *
     * @param property
     * @return {@link Property}
     * @throws RepositoryException
     */
    public Property get(JcrProperty<?> property) throws RepositoryException {
        return getTarget().getProperty(property.propertyName());
    }

    /**
     * Learn whether {@link #getTarget()} defines a property with {@link JcrProperty#propertyName()} of
     * {@code property}.
     *
     * @param property
     * @return boolean
     * @throws RepositoryException
     */
    public boolean has(JcrProperty<?> property) throws RepositoryException {
        return getTarget().hasProperty(property.propertyName());
    }

    /**
     * Invoke {@code consumer} against {@link #getTarget()}.
     *
     * @param consumer
     * @return {@code this}
     * @throws RepositoryException
     */
    public WithNode invoke(JcrConsumer<Node> consumer) throws RepositoryException {
        Validate.notNull(consumer).jcrAccept(getTarget());
        return this;
    }

    /**
     * Iterate child nodes.
     *
     * @return {@link RangedIterator} of {@link WithNode}.
     * @see Node#getNodes()
     */
    public RangedIterator<WithNode> nodes() {
        return withNodes(getTarget()::getNodes);
    }

    /**
     * Iterate child nodes.
     *
     * @param namePattern
     * @return {@link RangedIterator} of {@link WithNode}.
     * @see Node#getNodes(String)
     */
    public RangedIterator<WithNode> nodes(String namePattern) {
        return withNodes(() -> getTarget().getNodes(namePattern));
    }

    /**
     * Iterate child nodes.
     *
     * @param nameGlobs
     * @return {@link RangedIterator} of {@link WithNode}.
     * @see Node#getNodes(String[])
     */
    public RangedIterator<WithNode> nodes(String... nameGlobs) {
        return withNodes(() -> getTarget().getNodes(nameGlobs));
    }

    /**
     * Iterate nodes in {@code target}'s shared set.
     *
     * @return {@link RangedIterator} of {@link WithNode}.
     * @see Node#getSharedSet()
     */
    public RangedIterator<WithNode> sharedSet() {
        return withNodes(getTarget()::getSharedSet);
    }

    private RangedIterator<WithNode> withNodes(JcrSupplier<NodeIterator> nodes) {
        return RangedIterator.map(nodes, (JcrFunction<Node, WithNode>) jcr::withNode);
    }

    /**
     * Iterate child properties.
     *
     * @return {@link RangedIterator} of {@link Property}.
     * @see Node#getProperties()
     */
    public RangedIterator<Property> properties() {
        return props(getTarget()::getProperties);
    }

    /**
     * Iterate child properties.
     *
     * @param namePattern
     * @return {@link RangedIterator} of {@link Property}.
     * @see Node#getProperties(String)
     */
    public RangedIterator<Property> properties(String namePattern) {
        return props(() -> getTarget().getProperties(namePattern));
    }

    /**
     * Iterate child properties.
     *
     * @param nameGlobs
     * @return {@link RangedIterator} of {@link Property}.
     * @see Node#getProperties(String[])
     */
    public RangedIterator<Property> properties(String... nameGlobs) {
        return props(() -> getTarget().getProperties(nameGlobs));
    }

    /**
     * Iterate references.
     *
     * @return {@link RangedIterator} of {@link Property}.
     * @see Node#getReferences()
     */
    public RangedIterator<Property> references() {
        return props(getTarget()::getReferences);
    }

    /**
     * Iterate references.
     *
     * @param name
     * @return {@link RangedIterator} of {@link Property}.
     * @see Node#getReferences(String)
     */
    public RangedIterator<Property> references(String name) {
        return props(() -> getTarget().getReferences(name));
    }

    /**
     * Iterate weak references.
     *
     * @return {@link RangedIterator} of {@link Property}.
     * @see Node#getWeakReferences()
     */
    public RangedIterator<Property> weakReferences() {
        return props(getTarget()::getWeakReferences);
    }

    /**
     * Iterate weak references.
     *
     * @param name
     * @return {@link RangedIterator} of {@link Property}.
     * @see Node#getWeakReferences(String)
     */
    public RangedIterator<Property> weakReferences(String name) {
        return props(() -> getTarget().getWeakReferences(name));
    }

    /**
     * Learn whether the target of this {@link WithNode} represents the root of the repository.
     *
     * @return {@code boolean}
     * @throws RepositoryException
     */
    public boolean isRoot() throws RepositoryException {
        return getTarget().getDepth() == 0;
    }

    private RangedIterator<Property> props(JcrSupplier<PropertyIterator> supplier) {
        return RangedIterator.of(supplier);
    }
}
