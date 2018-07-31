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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeTemplate;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableObject;

import jcrbox.fp.JcrFp;
import jcrbox.util.EnumHelper;

/**
 * Define JCR node types using a Java enum.
 *
 * @param <N>
 *            self type
 */
public interface JcrNode<N extends Enum<N> & JcrNode<N>> extends JcrSource<N> {

    /**
     * Annotation to define a JCR node type.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface NodeDefinition {

        /**
         * Whether the node type is abstract.
         *
         * @return {@code boolean}, default {@code false}
         */
        boolean isAbstract() default false;

        /**
         * The node supertypes.
         *
         * @return {@link String}[]
         */
        String[] supertypes() default {};

        /**
         * Whether the node is a mixin type.
         *
         * @return {@code boolean}, default {@code false}
         */
        boolean mixin() default false;

        /**
         * Whether the node type has orderable child nodes.
         *
         * @return {@code boolean}, default {@code false}
         */
        boolean orderableChildNodes() default false;

        /**
         * The primary item name of the node type.
         *
         * @return {@link String}
         */
        String primaryItemName() default "";

        /**
         * Whether the node type is queryable.
         *
         * @return {@code boolean}
         */
        boolean queryable() default true;
    }

    /**
     * Configure a {@link NodeTypeTemplate} from this enum constant, using, if available, the settings declared by its
     * {@link NodeDefinition}.
     *
     * @param ntt
     * @return {@code ntt}
     * @throws ConstraintViolationException
     */
    default NodeTypeTemplate configure(NodeTypeTemplate ntt) throws ConstraintViolationException {
        ntt.setName(fullname());
        final MutableObject<String[]> annotatedSupertypes = new MutableObject<>(ArrayUtils.EMPTY_STRING_ARRAY);
        Optional.ofNullable(EnumHelper.getAnnotation(asEnum(), NodeDefinition.class)).ifPresent(JcrFp.adapt(def -> {
            ntt.setAbstract(def.isAbstract());
            annotatedSupertypes.setValue(def.supertypes());
            ntt.setMixin(def.mixin());
            ntt.setOrderableChildNodes(def.orderableChildNodes());
            ntt.setPrimaryItemName(def.primaryItemName());
            ntt.setQueryable(def.queryable());
        }));

        final String[] supertypes =
            Stream.concat(Stream.of(annotatedSupertypes.getValue()), getSupertypes().stream().map(JcrLiteral::fullname))
                .distinct().toArray(String[]::new);

        if (supertypes.length > 0) {
            ntt.setDeclaredSuperTypeNames(supertypes);
        }
        return ntt;
    }

    /**
     * Return the set of {@link JcrNode} types that make up supertypes of the modeled (top-level) node type. This method
     * can be overridden by individual {@link JcrNode} instances.
     *
     * @return empty {@link Set}
     */
    default Set<? extends JcrNode<?>> getSupertypes() {
        return Collections.emptySet();
    }

    /**
     * Learn whether this enum constant models the primary node type of the specified {@link Node}.
     *
     * @param node
     * @return {@code boolean}
     * @throws RepositoryException
     */
    default boolean isPrimaryNodeTypeOf(Node node) throws RepositoryException {
        return isAssignableFrom(node.getPrimaryNodeType());
    }

    /**
     * Learn whether the node type modeled by this enum constant is exhibited by the specified {@link Node}.
     *
     * @param node
     * @return {@code boolean}
     * @throws RepositoryException
     * @see {@link Node#isNodeType(String)}
     */
    default boolean isTypeOf(Node node) throws RepositoryException {
        return node.isNodeType(fullname());
    }

    /**
     * Learn whether the node type modeled by this enum is assignable from the specified {@link NodeType}.
     *
     * @param nodeType
     * @return {@code boolean}
     * @throws RepositoryException
     * @see {@link NodeType#isNodeType(String)}
     */
    default boolean isAssignableFrom(NodeType nodeType) throws RepositoryException {
        return nodeType.isNodeType(fullname());
    }
}
