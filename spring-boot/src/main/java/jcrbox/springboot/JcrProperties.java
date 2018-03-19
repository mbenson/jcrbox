package jcrbox.springboot;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jcr")
public class JcrProperties {

    private final Map<String, String> repositoryParameters = new LinkedHashMap<>();

    public Map<String, String> getRepositoryParameters() {
        return repositoryParameters;
    }
}
