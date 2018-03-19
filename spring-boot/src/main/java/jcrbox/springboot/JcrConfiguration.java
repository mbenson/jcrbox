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
package jcrbox.springboot;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.ServiceLoader;

import javax.annotation.PreDestroy;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.modeshape.jcr.ModeShape;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.schematic.DocumentFactory;
import org.modeshape.schematic.document.EditableDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jcrbox.Jcr;
import jcrbox.fp.JcrConsumer;
import jcrbox.query.QueryBuilder;

/**
 * Spring Boot auto-configuration for jcrbox.
 */
@Configuration
@EnableConfigurationProperties(JcrProperties.class)
public class JcrConfiguration {

    /**
     * {@link ModeShape} configuration.
     */
    @Configuration
    @ConditionalOnClass(ModeShapeEngine.class)
    @EnableConfigurationProperties(JcrProperties.class)
    public static class ModeShapeConfiguration {

        private static final String DEFAULT_REPO_NAME = "ModeShape";

        private @Autowired JcrProperties jcrProperties;

        private ModeShapeEngine modeShapeEngine;

        /**
         * Bean factory method for the {@link ModeShapeEngine}.
         *
         * @return {@link ModeShapeEngine}
         */
        @Bean
        public ModeShapeEngine modeShapeEngine() {
            modeShapeEngine = new ModeShapeEngine();
            modeShapeEngine.start();
            return modeShapeEngine;
        }

        /**
         * Bean factory method for the ModeShape {@link RepositoryConfiguration}.
         *
         * @return {@link RepositoryConfiguration}
         */
        @Bean
        public RepositoryConfiguration repositoryConfiguration() {
            final EditableDocument document = DocumentFactory.newDocument();

            jcrProperties.getRepositoryParameters().forEach((k, v) -> {
                final ListIterator<String> path = Arrays.asList(StringUtils.split(k, '.')).listIterator();
                findDocument(document, path).set(path.next(), v);
            });
            return new RepositoryConfiguration(document, DEFAULT_REPO_NAME);
        }

        private EditableDocument findDocument(EditableDocument parent, ListIterator<String> path) {
            final String key = path.next();
            if (path.hasNext()) {
                return findDocument(parent.getOrCreateDocument(key), path);
            }
            path.previous();
            return parent;
        }

        /**
         * Bean factory method for the {@link ModeShape} JCR {@link Repository}.
         *
         * @return {@link Repository}
         * @throws RepositoryException
         */
        @Bean
        public Repository repository() throws RepositoryException {
            return modeShapeEngine().deploy(repositoryConfiguration());
        }

        /**
         * Shutdown method for {@link ModeShape}.
         */
        @PreDestroy
        public void shutdown() {
            Optional.ofNullable(modeShapeEngine).ifPresent(ModeShapeEngine::shutdown);
        }
    }

    /**
     * Fallback bean factory method for a JCR {@link Repository} using a {@link RepositoryFactory}
     * {@link ServiceLoader}.
     *
     * @param jcrProperties
     * @return {@link Repository}
     * @throws RepositoryException
     */
    @Bean
    @ConditionalOnMissingBean(Repository.class)
    public Repository repository(JcrProperties jcrProperties) throws RepositoryException {
        for (RepositoryFactory repositoryFactory : ServiceLoader.load(RepositoryFactory.class)) {
            final Repository repository = repositoryFactory.getRepository(jcrProperties.getRepositoryParameters());
            if (repository != null) {
                return repository;
            }
        }
        return null;
    }

    /**
     * Bean factory method for a JCR {@link Session}.
     * 
     * @param repository
     * @return {@link Session}
     * @throws RepositoryException
     */
    @Bean(destroyMethod = "logout")
    public Session jcrSession(Repository repository) throws RepositoryException {
        return repository.login();
    }

    /**
     * Bean factory method for a {@link Jcr} instance over {@link #jcrSession(Repository)}.
     * 
     * @param session
     * @param jcrConfigurers
     * @param queryBuilders
     * @return {@link Jcr}
     */
    @Bean
    public Jcr jcr(Session session, Optional<List<JcrConsumer<Jcr>>> jcrConfigurers,
        Optional<List<QueryBuilder.Strong<?>>> queryBuilders) {
        final Jcr jcr = new Jcr(session);

        jcrConfigurers.ifPresent(l -> l.forEach(cfg -> cfg.accept(jcr)));
        queryBuilders.ifPresent(l -> l.forEach((JcrConsumer<QueryBuilder.Strong<?>>) jcr::getOrStoreQuery));

        return jcr;
    }
}
