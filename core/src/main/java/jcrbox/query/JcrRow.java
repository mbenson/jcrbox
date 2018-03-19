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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.query.Row;

import jcrbox.Jcr;
import jcrbox.JcrNode;
import jcrbox.WithNode;

/**
 * Enhanced JCR {@link Row}.
 */
public interface JcrRow extends Row {

    /**
     * Adapter from {@link Row} to {@link JcrRow}.
     */
    static class Adapter implements JcrRow {

        private final Jcr jcr;
        private final Row wrapped;

        /**
         * Create a new {@link Adapter}.
         * 
         * @param jcr
         * @param wrapped
         */
        Adapter(Jcr jcr, Row wrapped) {
            super();
            this.jcr = jcr;
            this.wrapped = wrapped;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Value[] getValues() throws RepositoryException {
            return wrapped.getValues();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Value getValue(String columnName) throws RepositoryException {
            return wrapped.getValue(columnName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Node getNode() throws RepositoryException {
            return wrapped.getNode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Node getNode(String selectorName) throws RepositoryException {
            return wrapped.getNode(selectorName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getPath() throws RepositoryException {
            return wrapped.getPath();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getPath(String selectorName) throws RepositoryException {
            return wrapped.getPath(selectorName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getScore() throws RepositoryException {
            return wrapped.getScore();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getScore(String selectorName) throws RepositoryException {
            return wrapped.getScore(selectorName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public WithNode withNode() throws RepositoryException {
            return jcr.withNode(getNode());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public WithNode withNode(String selectorName) throws RepositoryException {
            return jcr.withNode(getNode(selectorName));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default String getPath() throws RepositoryException {
        return getNode().getPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default String getPath(String selectorName) throws RepositoryException {
        return getNode(selectorName).getPath();
    }

    /**
     * Adapt a single-node row to the {@link WithNode} API.
     * 
     * @return {@link WithNode}
     * @throws RepositoryException
     * @see {@link #getNode()}
     */
    WithNode withNode() throws RepositoryException;

    /**
     * Adapt the specified node to the {@link WithNode} API.
     * 
     * @param selectorName
     * @return {@link WithNode}
     * @throws RepositoryException
     * @see {@link #getNode(String)}
     */
    WithNode withNode(String selectorName) throws RepositoryException;

    /**
     * Get the node specified by {@code node}.
     * 
     * @param node
     * @return {@link NodeTypeExistsException}
     * @throws RepositoryException
     * @see {@link JcrNode#selectorName()}
     */
    default Node getNode(JcrNode<?> node) throws RepositoryException {
        return getNode(node.selectorName());
    }

    /**
     * Adapt the specified node to the {@link WithNode} API.
     * 
     * @param node
     * @return {@link WithNode}
     * @throws RepositoryException
     * @see {@link #withNode(String)}
     * @see {@link JcrNode#selectorName()}
     */
    default WithNode withNode(JcrNode<?> node) throws RepositoryException {
        return withNode(node.selectorName());
    }

    /**
     * Get the path of the specified result node.
     * 
     * @param node
     * @return {@link StringIndexOutOfBoundsException}
     * @throws RepositoryException
     * @see {@link #getPath(String)}
     * @see JcrNode#selectorName()
     */
    default String getPath(JcrNode<?> node) throws RepositoryException {
        return getPath(node.selectorName());
    }

    /**
     * Get the score of the specified result node.
     * 
     * @param node
     * @return {@code double}
     * @throws RepositoryException
     * @see {@link #getScore(String)}
     * @see JcrNode#selectorName()
     */
    default double getScore(JcrNode<?> node) throws RepositoryException {
        return getScore(node.selectorName());
    }
}
