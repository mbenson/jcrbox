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

import static javax.jcr.nodetype.NodeType.NT_QUERY;

import java.util.Iterator;
import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.apache.commons.lang3.Validate;

import jcrbox.fp.JcrConsumer;
import jcrbox.fp.JcrFunction;
import jcrbox.literal.JcrChild;
import jcrbox.literal.JcrNode;
import jcrbox.literal.JcrProperty;
import jcrbox.query.JcrQuery;
import jcrbox.query.JcrResult;
import jcrbox.query.QueryBuilder;
import jcrbox.query.QueryParameter;

/**
 * JCR {@link Session} wrapper/jcrbox API entry point.
 */
public class Jcr {

    /**
     * Obtain a function to generate an enumValue.
     *
     * @param e
     * @return {@link JcrFunction}
     */
    public static JcrFunction<ValueFactory, Value> enumValue(Enum<?> e) {
        return vf -> vf.createValue(e.name());
    }

    /**
     * The wrapped {@link Session}.
     */
    public final Session session;

    private boolean allowMetaUpdates;

    /**
     * Create a new {@link Jcr} for working with the specified {@link Session}.
     *
     * @param session
     */
    public Jcr(Session session) {
        this.session = Validate.notNull(session);
    }

    /**
     * Set whether updates to JCR workspace metadata (i.e. {@link NodeType} definitions) are permitted.
     * 
     * @param allowMetaUpdates
     * @return {@code this}, fluently
     */
    public Jcr allowMetaUpdates(boolean allowMetaUpdates) {
        this.allowMetaUpdates = allowMetaUpdates;
        return this;
    }
    
    /**
     * Get a strongly typed {@link Session}.
     * @param <S> inferred type
     * @return {@code S}
     */
    @SuppressWarnings("unchecked")
    public <S extends Session> S getSession() {
        return (S) session;
    }

    /**
     * Get a {@link WithNode} wrapping the {@link Node} at {@code path}.
     *
     * @param path
     *            must be absolute
     * @return {@link WithNode}
     * @throws RepositoryException
     */
    public WithNode withNode(Path path) throws RepositoryException {
        return withNode(session.getNode(path.absolute().toString()));
    }

    /**
     * Get a {@link WithNode} wrapping the {@link Node} at {@code path}.
     *
     * @param path
     *            must be absolute
     * @param defaultNodeType
     * @param finalNodeType
     * @return {@link WithNode}
     * @throws RepositoryException
     */
    public WithNode withNode(Path path, String defaultNodeType, String finalNodeType) throws RepositoryException {
        return withNode(
            findOrCreateNode(session.getRootNode(), path.absolute().toString(), defaultNodeType, finalNodeType));
    }

    /**
     * Get a {@link WithNode} wrapping the {@link Node} at {@code path}.
     *
     * @param path
     *            must be absolute
     * @param defaultNodeType
     * @param finalNodeType
     * @return {@link WithNode}
     * @throws RepositoryException
     */
    public WithNode withNode(Path path, JcrNode<?> defaultNodeType, JcrNode<?> finalNodeType)
        throws RepositoryException {
        return withNode(path.absolute(), defaultNodeType.fullname(), finalNodeType.fullname());
    }

    /**
     * Get a {@link WithNode} wrapping the specified {@link Node}.
     *
     * @param node
     * @return {@link WithNode}
     */
    public WithNode withNode(Node node) {
        return new WithNode(this, node);
    }

    /**
     * Get a {@link WithNode} wrapping the {@link Session} root {@link Node}.
     *
     * @return {@link WithNode}
     * @throws RepositoryException
     */
    public WithNode withRoot() throws RepositoryException {
        return new WithNode(this, session.getRootNode());
    }

    /**
     * Get or register the specified {@link JcrNode} type, leaving all configuration except for the node name up to
     * {@code definer}.
     *
     * @param node
     * @param definer
     * @return {@link NodeType}
     * @throws RepositoryException
     */
    public NodeType getOrRegisterNodeType(JcrNode<?> node, JcrFunction<NodeTypeManager, NodeTypeTemplate> definer)
        throws RepositoryException {
        final NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();
        final String name = node.fullname();
        if (mgr.hasNodeType(name) && !allowMetaUpdates) {
            return mgr.getNodeType(name);
        }
        final NodeTypeTemplate nodeTypeTemplate = definer.apply(mgr);
        nodeTypeTemplate.setName(name);
        return mgr.registerNodeType(nodeTypeTemplate, allowMetaUpdates);
    }

    /**
     * Get or register the specified {@link JcrNode} type, passing the {@link NodeTypeTemplate} to {@code definer} for
     * configuration. Any information available from the {@link JcrNode} is automatically configured.
     *
     * @param node
     * @param definer
     * @return {@link NodeType}
     * @throws RepositoryException
     */
    public NodeType getOrRegisterNodeType(JcrNode<?> node, JcrConsumer<NodeTypeTemplate> definer)
        throws RepositoryException {
        final NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();
        final String name = node.fullname();
        if (mgr.hasNodeType(name) && !allowMetaUpdates) {
            return mgr.getNodeType(name);
        }
        final NodeTypeTemplate nodeTypeTemplate = mgr.createNodeTypeTemplate();
        node.configure(nodeTypeTemplate);
        definer.accept(nodeTypeTemplate);
        return mgr.registerNodeType(nodeTypeTemplate, allowMetaUpdates);
    }

    /**
     * Add a property definition to {@code node}.
     *
     * @param node
     * @param property
     * @return success
     * @throws RepositoryException
     */
    @SuppressWarnings("unchecked")
    public boolean addTo(NodeTypeTemplate node, JcrProperty<?> property) throws RepositoryException {
        return node.getPropertyDefinitionTemplates().add(property.configure(
            session.getWorkspace().getNodeTypeManager().createPropertyDefinitionTemplate(), session.getValueFactory()));
    }

    /**
     * Add {@code childNode} to {@code node}.
     *
     * @param node
     * @param child
     * @return success
     * @throws RepositoryException
     */
    public boolean addTo(NodeTypeTemplate node, JcrChild<?> child) throws RepositoryException {
        return addTo(node, child, JcrConsumer.noop());
    }

    /**
     * Add {@code childNode} to {@code node} with additional configuration.
     *
     * @param node
     * @param child
     * @param config
     * @return success
     * @throws RepositoryException
     */
    @SuppressWarnings("unchecked")
    public boolean addTo(NodeTypeTemplate node, JcrChild<?> child, JcrConsumer<NodeDefinitionTemplate> config)
        throws RepositoryException {
        final NodeDefinitionTemplate nodeDefinitionTemplate =
            session.getWorkspace().getNodeTypeManager().createNodeDefinitionTemplate();
        child.configure(nodeDefinitionTemplate);
        config.accept(nodeDefinitionTemplate);
        return node.getNodeDefinitionTemplates().add(nodeDefinitionTemplate);
    }

    /**
     * Get or store a query at {@code path}, using {@code queryBuilder} to build it if necessary.
     *
     * @param path
     * @param queryBuilder
     * @return {@link Node} created
     * @throws RepositoryException
     */
    public Node getOrStoreQuery(String path, QueryBuilder queryBuilder) throws RepositoryException {
        final Path p = Path.parse(session.getWorkspace().getNamespaceRegistry(), path);
        Validate.isTrue(p.isAbsolute(), "%s is not an absolute path", path);

        if (hasNode(path, NT_QUERY)) {
            return session.getNode(path);
        }
        final Path parent = p.parent();
        if (!parent.isEmpty()) {
            findOrCreateNode(session.getRootNode(), parent, (String) null, null);
        }
        return query(queryBuilder).storeAsNode(path);
    }

    /**
     * Get or store a query at {@code path}, using {@code queryBuilder} to build it if necessary.
     *
     * @param path
     * @param queryBuilder
     * @return {@link Node} created
     * @throws RepositoryException
     */
    public Node getOrStoreQuery(Path path, QueryBuilder queryBuilder) throws RepositoryException {
        return getOrStoreQuery(path.absolute().toString(), queryBuilder);
    }

    /**
     * Get or store a query at the implied path for a {@link JcrQuery} class, using {@code queryBuilder} to build it if
     * necessary.
     *
     * @param queryLiteral
     * @param queryBuilder
     * @return {@link Node} created
     * @throws RepositoryException
     * @see JcrQuery#path(Class)
     */
    public Node getOrStoreQuery(Class<? extends JcrQuery> queryLiteral, QueryBuilder queryBuilder)
        throws RepositoryException {
        return getOrStoreQuery(JcrQuery.path(queryLiteral), queryBuilder);
    }

    /**
     * Call {@link #getOrStoreQuery(Class, QueryBuilder)} with {@link QueryBuilder.Strong#queryType},
     * {@code queryBuilder}.
     *
     * @param queryBuilder
     *            required
     * @return {@link Node} created
     * @throws RepositoryException
     * @see JcrQuery#path(Class)
     */
    public Node getOrStoreQuery(QueryBuilder.Strong<?> queryBuilder) throws RepositoryException {
        return getOrStoreQuery(Objects.requireNonNull(queryBuilder).queryType, queryBuilder);
    }

    /**
     * Learn whether the repository contains a {@link Node} at {@code path}.
     *
     * @param path
     * @return boolean
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public boolean hasNode(String path) throws RepositoryException {
        return hasNode(path, (String) null);
    }

    /**
     * Learn whether the repository contains a {@link Node} at {@code path}.
     *
     * @param path
     * @return boolean
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public boolean hasNode(Path path) throws RepositoryException {
        return hasNode(path.absolute().toString(), (String) null);
    }

    /**
     * Learn whether the repository contains a {@link Node} at {@code path}, and of a certain type.
     *
     * @param path
     * @param type
     * @return boolean
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public boolean hasNode(Path path, String type) throws RepositoryException {
        return hasNode(path.absolute().toString(), type);
    }

    /**
     * Learn whether the repository contains a {@link Node} at {@code path}, and of a certain type.
     *
     * @param path
     * @param type
     * @return boolean
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public boolean hasNode(String path, String type) throws RepositoryException {
        if (!session.nodeExists(path)) {
            return false;
        }
        final Node node = session.getNode(path);
        return type == null || node.isNodeType(type);
    }

    /**
     * Learn whether the repository contains a {@link Node} at {@code path}, and of a certain type.
     *
     * @param path
     * @param type
     * @return boolean
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public boolean hasNode(Path path, JcrNode<?> type) throws RepositoryException {
        return hasNode(path.absolute().toString(), type);
    }

    /**
     * Learn whether the repository contains a {@link Node} at {@code path}, and of a certain type.
     *
     * @param path
     * @param type
     * @return boolean
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public boolean hasNode(String path, JcrNode<?> type) throws RepositoryException {
        if (!session.nodeExists(path)) {
            return false;
        }
        final Node node = session.getNode(path);
        return type == null || type.isTypeOf(node);
    }

    /**
     * Use a {@link QueryBuilder} to build a {@link Query}.
     *
     * @param queryBuilder
     * @return {@link Query}
     * @throws RepositoryException
     */
    public Query query(QueryBuilder queryBuilder) throws RepositoryException {
        return Objects.requireNonNull(queryBuilder).buildQuery(session.getWorkspace().getQueryManager().getQOMFactory(),
            this);
    }

    /**
     * Execute a stored query specifying a page of results.
     *
     * @param path
     * @param page
     * @param pageSize
     * @param parameters
     * @return {@link JcrResult}
     * @throws InvalidQueryException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public JcrResult executeStoredQuery(Path path, long page, long pageSize, QueryParameter... parameters)
        throws RepositoryException {
        return executeStoredQuery(path.absolute().toString(), page, pageSize, parameters);
    }

    /**
     * Execute a stored query specifying a page of results.
     *
     * @param path
     * @param page
     * @param pageSize
     * @param parameters
     * @return {@link JcrResult}
     * @throws InvalidQueryException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public JcrResult executeStoredQuery(String path, long page, long pageSize, QueryParameter... parameters)
        throws RepositoryException {
        return execute(session.getWorkspace().getQueryManager().getQuery(session.getNode(path)), page, pageSize,
            parameters);
    }

    /**
     * Execute a stored query specifying a page of results.
     *
     * @param queryLiteral
     * @param page
     * @param pageSize
     * @param parameters
     * @return {@link JcrResult}
     * @throws RepositoryException
     */
    public JcrResult executeStoredQuery(Class<? extends JcrQuery> queryLiteral, long page, long pageSize,
        QueryParameter... parameters) throws RepositoryException {
        return executeStoredQuery(JcrQuery.path(queryLiteral), page, pageSize, parameters);
    }

    /**
     * Execute a stored query.
     *
     * @param path
     * @param parameters
     * @return {@link JcrResult}
     * @throws InvalidQueryException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public JcrResult executeStoredQuery(String path, QueryParameter... parameters) throws RepositoryException {
        return execute(session.getWorkspace().getQueryManager().getQuery(session.getNode(path)), parameters);
    }

    /**
     * Execute a stored query.
     *
     * @param path
     * @param parameters
     * @return {@link JcrResult}
     * @throws InvalidQueryException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public JcrResult executeStoredQuery(Path path, QueryParameter... parameters) throws RepositoryException {
        return executeStoredQuery(path.absolute().toString(), parameters);
    }

    /**
     * Execute a stored query.
     *
     * @param queryLiteral
     * @param parameters
     * @return {@link JcrResult}
     * @throws RepositoryException
     */
    public JcrResult executeStoredQuery(Class<? extends JcrQuery> queryLiteral, QueryParameter... parameters)
        throws RepositoryException {
        return executeStoredQuery(JcrQuery.path(queryLiteral), parameters);
    }

    /**
     * Execute a query.
     *
     * @param q
     * @param parameters
     * @return {@link JcrResult}
     * @throws RepositoryException
     */
    public JcrResult execute(Query q, QueryParameter... parameters) throws RepositoryException {
        for (QueryParameter p : parameters) {
            q.bindValue(p.name, p.value);
        }
        return new JcrResult(this, q.execute());
    }

    /**
     * Execute a query specifying a page of results.
     *
     * @param q
     * @param page, 1-based
     * @param pageSize
     * @param parameters
     * @return {@link JcrResult}
     * @throws RepositoryException
     */
    public JcrResult execute(Query q, long page, long pageSize, QueryParameter... parameters)
        throws RepositoryException {
        q.setLimit(pageSize);
        q.setOffset(pageSize * (page - 1));
        return execute(q, parameters);
    }

    /**
     * Create a {@link QueryParameter}.
     *
     * @param name
     * @param value
     *            function
     * @return {@link QueryParameter}
     * @throws RepositoryException
     */
    public QueryParameter queryParameter(String name, JcrFunction<ValueFactory, Value> value)
        throws RepositoryException {
        return QueryParameter.of(name, value.apply(session.getValueFactory()));
    }

    /**
     * Create a {@link String} {@link Value} for the specified {@link Enum}.
     *
     * @param e
     * @return {@link Value}
     * @throws RepositoryException
     */
    public Value createValue(Enum<?> e) throws RepositoryException {
        return enumValue(e).apply(session.getValueFactory());
    }

    /**
     * Get or create a node at the specified path.
     *
     * @param parentNode
     *            the parent node. may not be null
     * @param path
     *            the path of the desired child node. may not be blank
     * @param defaultNodeType
     *            the default node type. may be null
     * @param finalNodeType
     *            the optional final node type. may be null
     * @return the existing or newly created node
     * @throws RepositoryException
     * @throws IllegalArgumentException
     *             if either the parentNode or path argument is null
     */
    public Node findOrCreateNode(Node parentNode, String path, String defaultNodeType, String finalNodeType)
        throws RepositoryException {
        Objects.requireNonNull(parentNode, "parentNode");
        Validate.notBlank(path, "invalid path specified: %s", path);
        if (parentNode.hasNode(path)) {
            return parentNode.getNode(path);
        }
        return findOrCreateNode(parentNode, Path.parse(session.getWorkspace().getNamespaceRegistry(), path),
            defaultNodeType, finalNodeType);
    }

    /**
     * Get or create a node at the specified path.
     *
     * @param parentNode
     *            the parent node. may not be null
     * @param path
     *            the path of the desired child node. may not be blank
     * @param defaultNodeType
     *            the default node type. may be null
     * @param finalNodeType
     *            the optional final node type. may be null
     * @return the existing or newly created node
     * @throws RepositoryException
     * @throws IllegalArgumentException
     *             if either the parentNode or path argument is null
     */
    public Node findOrCreateNode(Node parentNode, String path, JcrNode<?> defaultNodeType, JcrNode<?> finalNodeType)
        throws RepositoryException {
        return findOrCreateNode(parentNode, path, nameOf(defaultNodeType), nameOf(finalNodeType));
    }

    /**
     * Get or create a node at the specified path.
     *
     * @param parentNode
     *            the parent node. may not be null
     * @param path
     *            the path of the desired child node. may not be null
     * @param defaultNodeType
     *            the default node type. may be null
     * @param finalNodeType
     *            the optional final node type. may be null
     * @return the existing or newly created node
     * @throws RepositoryException
     * @throws IllegalArgumentException
     *             if either the parentNode or path argument is null
     */
    public Node findOrCreateNode(Node parentNode, Path path, String defaultNodeType, String finalNodeType)
        throws RepositoryException {
        Objects.requireNonNull(parentNode, "parentNode");
        Objects.requireNonNull(path, "path");

        Validate.isTrue(parentNode.getDepth() == 0 || path.isRelative(),
            "Absolute path %s is not compatible with node at %s", path, parentNode.getPath());

        path = path.relative();
        final String p = path.toString();
        if (parentNode.hasNode(p)) {
            return parentNode.getNode(p);
        }
        Node n = parentNode;
        for (Iterator<Path.Element> elements = path.iterator(); elements.hasNext();) {
            final String step = elements.next().toString();
            if (n.hasNode(step)) {
                n = n.getNode(step);
            } else {
                n = n.addNode(step, elements.hasNext() ? defaultNodeType : finalNodeType);
            }
        }
        return n;
    }

    /**
     * Get or create a node at the specified path.
     *
     * @param parentNode
     *            the parent node. may not be null
     * @param path
     *            the path of the desired child node. may not be null
     * @param defaultNodeType
     *            the default node type. may be null
     * @param finalNodeType
     *            the optional final node type. may be null
     * @return the existing or newly created node
     * @throws RepositoryException
     * @throws IllegalArgumentException
     *             if either the parentNode or path argument is null
     */
    public Node findOrCreateNode(Node parentNode, Path path, JcrNode<?> defaultNodeType, JcrNode<?> finalNodeType)
        throws RepositoryException {
        return findOrCreateNode(parentNode, path, nameOf(defaultNodeType), nameOf(finalNodeType));
    }

    /**
     * Remove from {@link Session} by {@link Path}.
     *
     * @param path
     * @throws RepositoryException
     */
    public void remove(Path path) throws RepositoryException {
        session.removeItem(path.absolute().toString());
    }

    /**
     * Parse a {@link Path} in this workspace.
     *
     * @param path
     * @return {@link Path}
     * @throws RepositoryException
     */
    public Path path(String path) throws RepositoryException {
        return Path.parse(session.getWorkspace().getNamespaceRegistry(), path);
    }
    
    private String nameOf(JcrNode<?> nodeType) {
        return nodeType == null ? null : nodeType.fullname();
    }
}
