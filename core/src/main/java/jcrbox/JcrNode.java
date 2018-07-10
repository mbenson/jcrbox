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
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.lang3.ArrayUtils;

import jcrbox.fp.JcrConsumer;

/**
 * Define JCR node types using a Java enum.
 *
 * @param <N>
 *            self type
 */
public interface JcrNode<N extends Enum<N> & JcrNode<N>> {

    /**
     * Annotation to define a top-level JCR node type.
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
     * Annotation to define a JCR child node type.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ChildNodeDefinition {
        /**
         * Whether a child node of this definition is automatically created.
         *
         * @return {@code boolean}, default {@code false}
         */
        boolean autoCreated() default false;

        /**
         * Whether a child node of this definition is mandatory.
         *
         * @return {@code boolean}, default {@code false}
         */
        boolean mandatory() default false;

        /**
         * The on parent version action of a child node of this definition.
         *
         * @return {@code int}, default ignored
         * @see OnParentVersionAction
         */
        int onParentVersion() default Integer.MIN_VALUE;

        /**
         * Whether the child node definition is protected.
         *
         * @return {@code boolean}, default {@code false}
         */
        boolean isProtected() default false;

        /**
         * Required primary types of a child node of this definition.
         * 
         * @return {@link String}[], default {@code {}}
         */
        String[] requiredPrimaryTypeNames() default {};

        /**
         * Default primary type of a child node of this definition.
         * 
         * @return {@link String}, default {@code ""}
         */
        String defaultPrimaryTypeName() default "";

        /**
         * Whether same name sibling nodes of this definition are allowed.
         * 
         * @return {@code boolean}, default {@code false}
         */
        boolean sameNameSiblings() default false;
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
        ntt.setName(nodeName());
        Optional.ofNullable(EnumHelper.getAnnotation(asEnum(), NodeDefinition.class))
            .ifPresent((JcrConsumer<NodeDefinition>) def -> {
                ntt.setAbstract(def.isAbstract());
                Optional.of(def.supertypes()).filter(ArrayUtils::isNotEmpty)
                    .ifPresent((JcrConsumer<String[]>) ntt::setDeclaredSuperTypeNames);
                ntt.setMixin(def.mixin());
                ntt.setOrderableChildNodes(def.orderableChildNodes());
                ntt.setPrimaryItemName(def.primaryItemName());
                ntt.setQueryable(def.queryable());
            });
        return ntt;
    }

    /**
     * Configure a {@link NodeDefinitionTemplate} from this enum constant, using, if available, the settings declared by
     * its {@link ChildNodeDefinition}.
     * 
     * @param ndt
     * @return @{code ndt}
     * @throws ConstraintViolationException
     */
    default NodeDefinitionTemplate configure(NodeDefinitionTemplate ndt) throws ConstraintViolationException {
        ndt.setName(nodeName());
        Optional.ofNullable(EnumHelper.getAnnotation(asEnum(), ChildNodeDefinition.class))
            .ifPresent((JcrConsumer<ChildNodeDefinition>) def -> {
                ndt.setAutoCreated(def.autoCreated());
                ndt.setMandatory(def.mandatory());
                if (def.onParentVersion() >= 0) {
                    ndt.setOnParentVersion(def.onParentVersion());
                }
                ndt.setProtected(def.isProtected());
                ndt.setRequiredPrimaryTypeNames(def.requiredPrimaryTypeNames());
                ndt.setDefaultPrimaryTypeName(def.defaultPrimaryTypeName());
                ndt.setSameNameSiblings(def.sameNameSiblings());
            });
        return ndt;
    }

    /**
     * Get the JCR namespace of the modeled node type.
     *
     * @return {@link String}
     * @see JcrNamespace.Helper#getNamespace(Class)
     */
    default String namespace() {
        return JcrNamespace.Helper.getNamespace(asEnum().getDeclaringClass());
    }

    /**
     * Get the basename of the modeled node type.
     * 
     * @return {@link String}
     * @see EnumHelper#basename(Enum)
     */
    default String basename() {
        return EnumHelper.basename(asEnum());
    }

    /**
     * Get the fully-qualified name of the modeled node type.
     *
     * @return {@link String}
     * @see #basename()
     * @see JcrNamespace.Helper#format(Class, String)
     */
    default String nodeName() {
        return JcrNamespace.Helper.format(getClass(), basename());
    }

    /**
     * Get a selector name to use for this node type in queries. Defaults to:
     * <ul>
     * <li>If unique, the first character of {@link Enum#name()} in lowercase (US).</li>
     * <li>Otherwise, {@link Enum#name()} in lowercase (US).</li>
     * </ul>
     *
     * @return {@link String}
     */
    default String selectorName() {
        final String full = asEnum().name().toLowerCase(Locale.US);
        final String mnemonic = full.substring(0, 1);

        @SuppressWarnings("unchecked")
        final boolean unique =
            Stream.of((N[]) getClass().getEnumConstants()).map(Enum::name).map(n -> n.toLowerCase(Locale.US))
                .map(s -> s.substring(0, 1)).filter(Predicate.isEqual(mnemonic)).count() == 1;

        return unique ? mnemonic : full;
    }

    /**
     * Get {@code this} as an {@link Enum} constant.
     *
     * @return {@code N}
     */
    @SuppressWarnings("unchecked")
    default N asEnum() {
        return (N) this;
    }

    /**
     * Learn whether this enum constant models the primary node type of the specified {@link Node}.
     *
     * @param node
     * @return {@code boolean}
     * @throws RepositoryException
     */
    default boolean isPrimaryNodeTypeOf(Node node) throws RepositoryException {
        return node.getPrimaryNodeType().isNodeType(nodeName());
    }

    /**
     * Learn whether the node type modeled by this enum constant is exhibited by the specified {@link Node}.
     *
     * @param node
     * @return {@code boolean}
     * @throws RepositoryException
     */
    default boolean isTypeOf(Node node) throws RepositoryException {
        return node.isNodeType(nodeName());
    }
}
