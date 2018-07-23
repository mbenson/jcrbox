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
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;

import jcrbox.fp.JcrConsumer;

/**
 * {@link JcrLiteral} to define child nodes.
 *
 * @param <N>
 *            self type
 */
public interface JcrChild<N extends Enum<N> & JcrChild<N>> extends JcrLiteral<N> {

    /**
     * Annotation to define a JCR child node.
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
         * @return {@link String}
         * @see JcrChild#getDefaultPrimaryType()
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
     * Configure a {@link NodeDefinitionTemplate} from this enum constant, using, if available, the settings declared by
     * its {@link ChildNodeDefinition}.
     * 
     * @param ndt
     * @return @{code ndt}
     * @throws ConstraintViolationException
     */
    default NodeDefinitionTemplate configure(NodeDefinitionTemplate ndt) throws ConstraintViolationException {
        ndt.setName(fullname());

        final MutableObject<String[]> annotatedRequiredPrimaryTypeNames =
            new MutableObject<>(ArrayUtils.EMPTY_STRING_ARRAY);
        final MutableObject<String> annotatedDefaultPrimaryTypeName = new MutableObject<>();

        Optional.ofNullable(EnumHelper.getAnnotation(asEnum(), ChildNodeDefinition.class))
            .ifPresent((JcrConsumer<ChildNodeDefinition>) def -> {
                ndt.setAutoCreated(def.autoCreated());
                ndt.setMandatory(def.mandatory());
                if (def.onParentVersion() >= 0) {
                    ndt.setOnParentVersion(def.onParentVersion());
                }
                ndt.setProtected(def.isProtected());
                annotatedRequiredPrimaryTypeNames.setValue(def.requiredPrimaryTypeNames());
                annotatedDefaultPrimaryTypeName.setValue(def.defaultPrimaryTypeName());
                ndt.setSameNameSiblings(def.sameNameSiblings());
            });

        final String[] requiredPrimaryTypeNames = Stream.concat(Stream.of(annotatedRequiredPrimaryTypeNames.getValue()),
            getRequiredPrimaryTypes().stream().map(JcrLiteral::fullname)).distinct().toArray(String[]::new);

        ndt.setRequiredPrimaryTypeNames(requiredPrimaryTypeNames);

        final Set<String> defaultPrimaryTypeNames = new HashSet<>();
        Optional.ofNullable(getDefaultPrimaryType()).map(JcrLiteral::fullname).ifPresent(defaultPrimaryTypeNames::add);
        Optional.of(annotatedDefaultPrimaryTypeName).map(MutableObject::getValue).filter(StringUtils::isNotBlank)
            .ifPresent(defaultPrimaryTypeNames::add);

        final String defaultPrimaryTypeName;

        if (defaultPrimaryTypeNames.isEmpty()) {
            if (requiredPrimaryTypeNames.length == 1) {
                defaultPrimaryTypeName = requiredPrimaryTypeNames[0];
            } else {
                defaultPrimaryTypeName = null;
            }
        } else if (defaultPrimaryTypeNames.size() == 1) {
            defaultPrimaryTypeName = defaultPrimaryTypeNames.iterator().next();
        } else {
            throw new IllegalStateException(
                String.format("Illegal multiple default primary types: %s", defaultPrimaryTypeNames));
        }
        ndt.setDefaultPrimaryTypeName(defaultPrimaryTypeName);

        return ndt;
    }

    /**
     * Get the default primary node type as a {@link JcrNode}. Returning a value is mutually incompatible with a
     * non-blank value of {@link ChildNodeDefinition#defaultPrimaryTypeName()}.
     * 
     * @return {@link JcrNode}
     */
    default JcrNode<?> getDefaultPrimaryType() {
        return null;
    }

    /**
     * Get required primary types as a {@link Set} of {@link JcrNode}s. Values will be merged with those specified via
     * {@link ChildNodeDefinition#requiredPrimaryTypeNames()}.
     * 
     * @return empty {@link Set}
     */
    default Set<? extends JcrNode<?>> getRequiredPrimaryTypes() {
        return Collections.emptySet();
    }
}
