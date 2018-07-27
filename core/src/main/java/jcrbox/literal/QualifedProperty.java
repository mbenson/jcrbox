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

import jcrbox.query.QueryBuilder;

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
     * Special {@link JcrProperty} instances.
     */
    public enum Special implements JcrProperty<Special> {
        //@formatter:off
		/**
		 * Special JCR property that will yield "all columns" when
		 * passed to a
		 * {@link QueryBuilder#column(QualifedProperty, String)}. The
		 * {@code columnName} parameter to that method should be
		 * specified with a {@code null} value.
		 */
		ALL_COLUMNS() {

			@Override
			public String fullname() {
				return null;
			}
		}
		//@formatter:om
	}

	/**
	 * Get a {@link JcrProperty} that indicates all columns.
	 *
	 * @return
	 */
	public static JcrProperty<?> allColumns() {
		return Special.ALL_COLUMNS;
	}

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
