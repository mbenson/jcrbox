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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.lang3.Validate;

import jcrbox.fp.JcrFunction;
import jcrbox.literal.JcrProperty.PropertyDefinition.DefaultValues;
import jcrbox.literal.JcrProperty.PropertyDefinition.DefaultValues.Other;
import jcrbox.util.EnumHelper;

/**
 * Define JCR properties with a Java enum.
 *
 * @param <P>
 *            self type
 */
public interface JcrProperty<P extends Enum<P> & JcrProperty<P>> extends JcrLiteral<P> {

    /**
     * For a given enum {@code PT}, if another enum {@code P} declares a property whose
     * {@link PropertyDefinition#constrainAsEnum()} is set to {@code PT}, this annotation can be used to mark one (or,
     * for multi-valued properties, more) of {@code PT}'s constants as default values for the property defined by the
     * member of {@code P}.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface DefaultValue {
    }

    /**
     * Specifies the property definition for a given enum constant.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface PropertyDefinition {

        /**
         * Specifies default values for a given property.
         */
        public @interface DefaultValues {

            /**
             * Permits the specification of a value of a type other than
             * {@code boolean}/{@code double}/{@code long}/{@link String}.
             *
             * @see ValueFactory#createValue(String, int)
             */
            public @interface Other {

                /**
                 * The desired value.
                 *
                 * @return {@link String}
                 */
                String value();

                /**
                 * {@link PropertyType} constant.
                 *
                 * @return {@code int}
                 */
                int type();
            }

            /**
             * Default values.
             *
             * @return {@code boolean[]}
             */
            boolean[] b() default {};

            /**
             * Default values.
             *
             * @return {@code double[]}
             */
            double[] d() default {};

            /**
             * Default values.
             *
             * @return {@code long[]}
             */
            long[] l() default {};

            /**
             * Default values.
             *
             * @return {@link String}[]
             */
            String[] s() default {};

            /**
             * Default values.
             *
             * @return {@link Other}[]
             */
            Other[] other() default {};
        }

        /**
         * Property type.
         *
         * @return {@code int}
         */
        int value() default Integer.MIN_VALUE;

        /**
         * Whether the property is automatically created.
         *
         * @return {@code boolean}
         */
        boolean autoCreated() default false;

        /**
         * Available query operators.
         *
         * @return {@link String}[]
         * @see javax.jcr.nodetype.PropertyDefinition#getAvailableQueryOperators()
         */
        String[] availableQueryOperators() default {};

        /**
         * Default values for the modeled property.
         *
         * @return {@link DefaultValues}
         */
        DefaultValues defaultValues() default @DefaultValues;

        /**
         * Whether the property is full text searchable.
         *
         * @return {@code boolean}
         */
        boolean fullTextSearchable() default true;

        /**
         * Whether the property is mandatory.
         *
         * @return {@code boolean}
         */
        boolean mandatory() default false;

        /**
         * Whether the property can be multi-valued.
         *
         * @return {@code boolean}
         */
        boolean multiple() default false;

        /**
         * The on parent version action of this property.
         *
         * @return {@code int}
         * @see OnParentVersionAction
         */
        int onParentVersion() default Integer.MIN_VALUE;

        /**
         * Whether the property is protected.
         *
         * @return {@code boolean}
         */
        boolean isProtected() default false;

        /**
         * Whether queries may be ordered by this property.
         *
         * @return {@code boolean}
         */
        boolean queryOrderable() default true;

        /**
         * Specify permitted values for the property. Mutually incompatible with {@link #constrainAsEnum()}.
         *
         * @return {@link String}[]
         */
        String[] valueConstraints() default {};

        /**
         * Permits optional specification of one {@link Enum} type to be represented by this property.
         * {@link PropertyType#STRING} is implied and may therefore be omitted; value constraints are implied to be the
         * enumeration constants of the specified type.
         *
         * @return {@link Class}[]
         */
        Class<? extends Enum<?>>[] constrainAsEnum() default {};
    }

    /**
     * Configure a {@link PropertyDefinitionTemplate} from this enum constant.
     *
     * @param pdt
     * @param valueFactory
     * @return {@code pdt}
     * @throws ConstraintViolationException
     */
    default PropertyDefinitionTemplate configure(PropertyDefinitionTemplate pdt, ValueFactory valueFactory)
        throws ConstraintViolationException {

        class ValidationHandler {

            final P p;

            ValidationHandler(P p) {
                this.p = p;
            }

            void alidate() {
                final PropertyDefinition def = EnumHelper.getAnnotation(p, PropertyDefinition.class);
                final Object[] desc = new Object[] { p.getDeclaringClass(), p.name() };
                Validate.validState(def.constrainAsEnum().length == 0 || def.valueConstraints().length == 0,
                    "Cannot specify both constrainAsEnum and valueConstraints: %s.%s", desc);
                Validate.validState(
                    def.constrainAsEnum().length == 0 || def.value() < 0 || def.value() == PropertyType.STRING,
                    "constrainAsEnum implies STRING property type: %s.%s", desc);
                Validate.validState(def.constrainAsEnum().length <= 1,
                    "constrainAsEnum is permitted a maximum of 1 value: %s.%s", desc);
            }
        }

        class DefinitionHandler {

            <T> void andle(T[] t, Consumer<? super T[]> next) {
                if (t.length > 0) {
                    next.accept(t);
                }
            }

            void andle(int i, IntConsumer next) {
                if (i >= 0) {
                    next.accept(i);
                }
            }

            void andleConstraints(PropertyDefinition def, PropertyDefinitionTemplate pdt) {
                final Class<? extends Enum<?>>[] enumTypes = def.constrainAsEnum();
                if (enumTypes.length == 1) {
                    pdt.setRequiredType(PropertyType.STRING);
                    andle(Stream.of(enumTypes[0].getEnumConstants()).map(Enum::name).toArray(String[]::new),
                        pdt::setValueConstraints);
                } else {
                    andle(def.valueConstraints(), pdt::setValueConstraints);
                }
            }

            void andleDefaultValues(PropertyDefinition def, PropertyDefinitionTemplate pdt, ValueFactory valueFactory) {
                final Class<? extends Enum<?>>[] enumTypes = def.constrainAsEnum();
                if (enumTypes.length == 1) {
                    pdt.setDefaultValues(Stream.of(enumTypes[0].getEnumConstants())
                        .filter(EnumHelper.isAnnotationPresent(DefaultValue.class)).map(Enum::name)
                        .map(valueFactory::createValue).toArray(Value[]::new));
                    return;
                }
                final DefaultValues defaultValues = def.defaultValues();
                final Set<Value> values = new HashSet<>();

                for (boolean b : defaultValues.b()) {
                    values.add(valueFactory.createValue(b));
                }
                for (double d : defaultValues.d()) {
                    values.add(valueFactory.createValue(d));
                }
                for (long l : defaultValues.l()) {
                    values.add(valueFactory.createValue(l));
                }
                Stream.of(defaultValues.s()).map(valueFactory::createValue).forEach(values::add);
                Stream.of(defaultValues.other())
                    .map((JcrFunction<Other, Value>) other -> valueFactory.createValue(other.value(), other.type()))
                    .forEach(values::add);

                pdt.setDefaultValues(values.toArray(new Value[values.size()]));
            }
        }

        final ValidationHandler v = new ValidationHandler(asEnum());
        v.alidate();

        final DefinitionHandler h = new DefinitionHandler();

        pdt.setName(fullname());
        Optional.ofNullable(EnumHelper.getAnnotation(asEnum(), PropertyDefinition.class)).ifPresent(def -> {
            pdt.setAutoCreated(def.autoCreated());
            pdt.setFullTextSearchable(def.fullTextSearchable());
            pdt.setMandatory(def.mandatory());
            pdt.setMultiple(def.multiple());
            pdt.setProtected(def.isProtected());
            pdt.setQueryOrderable(def.queryOrderable());
            h.andle(def.availableQueryOperators(), pdt::setAvailableQueryOperators);
            h.andle(def.onParentVersion(), pdt::setOnParentVersion);
            h.andle(def.value(), pdt::setRequiredType);
            h.andleConstraints(def, pdt);
            h.andleDefaultValues(def, pdt, valueFactory);
        });
        return pdt;
    }

    /**
     * Obtain a qualified form of this {@link JcrProperty} relative to {@code source}.
     *
     * @param source
     * @return {@link QualifedProperty}
     */
    default <S extends Enum<S> & JcrSource<S>> QualifedProperty<S, P> of(S source) {
        return new QualifedProperty<>(source, asEnum());
    }
}
