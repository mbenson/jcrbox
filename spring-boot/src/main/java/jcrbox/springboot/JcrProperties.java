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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot configuration properties object.
 */
@ConfigurationProperties("jcr")
public class JcrProperties {

    private final Map<String, String> repositoryParameters = new LinkedHashMap<>();
    private boolean allowMetaUpdates;

    /**
     * Get the repository parameters map.
     * 
     * @return {@link Map}
     */
    public Map<String, String> getRepositoryParameters() {
        return repositoryParameters;
    }

    /**
     * Get the {@code allowMetaUpdates}.
     * @return boolean
     */
    public boolean isAllowMetaUpdates() {
        return allowMetaUpdates;
    }

    /**
     * Set the {@code allowMetaUpdates.
     * @param allowMetaUpdates boolean
     */
    public void setAllowMetaUpdates(boolean allowMetaUpdates) {
        this.allowMetaUpdates = allowMetaUpdates;
    }
}
