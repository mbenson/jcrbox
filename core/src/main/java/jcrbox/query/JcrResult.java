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
package jcrbox.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import jcrbox.Jcr;
import jcrbox.RangedIterator;
import jcrbox.WithNode;
import jcrbox.fp.JcrConsumer;
import jcrbox.fp.JcrFunction;
import jcrbox.fp.JcrSupplier;
import jcrbox.util.Lazy;

/**
 * Modernizes the JCR {@link QueryResult} API.
 */
public class JcrResult implements Iterable<JcrRow> {

    private static <T> Set<T> adapt(T[] array) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(array)));
    }

    private final Jcr jcr;
    private final QueryResult wrapped;
    private final Lazy<Set<String>> selectors;
    private final Lazy<Set<String>> columns;

    /**
     * List of rows only initialized with copied rows if needed for streaming and used thereafter.
     */
    private final Lazy<List<JcrRow>> rows;

    /**
     * Create a new {@link JcrResult}. This constructor is intended for jcrbox internal use and is {@code public} only
     * for the sake of inter-package visibility.
     *
     * @param jcr
     * @param wrapped
     */
    public JcrResult(Jcr jcr, QueryResult wrapped) {
        super();
        this.jcr = jcr;
        this.wrapped = Objects.requireNonNull(wrapped);
        selectors = new Lazy<>((JcrSupplier<Set<String>>) () -> adapt(this.wrapped.getSelectorNames()));
        columns = new Lazy<>((JcrSupplier<Set<String>>) () -> adapt(this.wrapped.getColumnNames()));
        rows = new Lazy<>(() -> {
            final List<JcrRow> result = new ArrayList<>();
            try {
                for (RowIterator rowIterator = this.wrapped.getRows(); rowIterator.hasNext();) {
                    result.add(copyRow(rowIterator.nextRow()));
                }
            } catch (RepositoryException e) {
                throw new IllegalStateException(e);
            }
            return result;
        });
    }

    /**
     * @return {@link Set}
     * @see {@link QueryResult#getColumnNames()}
     */
    public Set<String> columns() {
        return columns.get();
    }

    /**
     * @return {@link Set}
     * @see {@link QueryResult#getSelectorNames()}
     */
    public Set<String> selectors() {
        return selectors.get();
    }

    /**
     * @return {@link RangedIterator}
     * @see QueryResult#getNodes()
     */
    public RangedIterator<Node> nodes() {
        return RangedIterator.of((JcrSupplier<NodeIterator>) wrapped::getNodes);
    }

    /**
     * Get a {@link RangedIterator} view of the nodes returned by this query, adapted to the {@link WithNode} API.
     *
     * @return {@link RangedIterator}
     * @see QueryResult#getNodes()
     * @see Jcr#withNode(Node)
     */
    public RangedIterator<WithNode> withNodes() {
        return RangedIterator.map((JcrSupplier<NodeIterator>) wrapped::getNodes,
            (JcrFunction<Node, WithNode>) jcr::withNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RangedIterator<JcrRow> iterator() {
        return rows();
    }

    /**
     * @return {@link RangedIterator}
     * @see QueryResult#getRows()
     */
    public RangedIterator<JcrRow> rows() {
        /*
         * if the copied rows are available, iterate over them; otherwise adapt the original rowIterator
         */
        return rows.optional().map(RangedIterator::of).orElseGet((JcrSupplier<RangedIterator<JcrRow>>) () ->

        new RangedIterator.Adapter<Row, JcrRow>(wrapped.getRows(), row -> new JcrRow.Adapter(jcr, row)) {

            // can only stream copied rows
            @Override
            public Stream<JcrRow> stream() {
                return rows.get().stream();
            }
        });
    }

    /**
     * Learn whether this resultset is empty.
     *
     * @return {@code boolean}
     * @throws RepositoryException
     */
    public boolean isEmpty() throws RepositoryException {
        final Optional<List<JcrRow>> copiedRows = rows.optional();
        return copiedRows.isPresent() ? copiedRows.get().isEmpty() : wrapped.getRows().getSize() == 0;
    }

    /**
     * Certain JCR implementations (cough, ModeShape) reuse the same {@link Row} instance for each iteration, so we copy
     * the row when streaming.
     *
     * @param row
     * @return JcrRow
     * @throws RepositoryException
     */
    private JcrRow copyRow(Row row) throws RepositoryException {
        class SelectorInfo {

            Node node;
            double score;

            SelectorInfo(Node node, double score) {
                super();
                this.node = node;
                this.score = score;
            }
        }

        final Map<String, SelectorInfo> selectorInfo =
            selectors().stream().collect(Collectors.toMap(Function.identity(),
                (JcrFunction<String, SelectorInfo>) s -> new SelectorInfo(row.getNode(s), row.getScore(s))));

        final Map<String, Value> values = new LinkedHashMap<>();
        columns().stream().forEach((JcrConsumer<String>) c -> values.put(c, row.getValue(c)));

        return new JcrRow() {

            private void assertSingleSelector() throws RepositoryException {
                if (selectorInfo.size() > 1) {
                    throw new RepositoryException("query row has multiple selectors");
                }
            }

            @Override
            public Node getNode() throws RepositoryException {
                assertSingleSelector();
                return selectorInfo.values().iterator().next().node;
            }

            @Override
            public Node getNode(String selectorName) throws RepositoryException {
                return selectorInfo.get(selectorName).node;
            }

            @Override
            public double getScore() throws RepositoryException {
                assertSingleSelector();
                return selectorInfo.values().iterator().next().score;
            }

            @Override
            public double getScore(String selectorName) throws RepositoryException {
                return selectorInfo.get(selectorName).score;
            }

            @Override
            public Value[] getValues() throws RepositoryException {
                return values.values().toArray(new Value[values.size()]);
            }

            @Override
            public Value getValue(String columnName) throws RepositoryException {
                return values.get(columnName);
            }

            @Override
            public WithNode withNode() throws RepositoryException {
                return jcr.withNode(getNode());
            }

            @Override
            public WithNode withNode(String selectorName) throws RepositoryException {
                return jcr.withNode(getNode(selectorName));
            }
        };
    }
}
