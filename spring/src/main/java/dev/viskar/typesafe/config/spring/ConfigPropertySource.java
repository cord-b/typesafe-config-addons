package dev.viskar.typesafe.config.spring;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dev.viskar.typesafe.config.spring.internal.SpringConfigUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Objects;

/**
 * A {@link PropertySource} implementation that can be added into Spring's {@link Environment}.
 */
public class ConfigPropertySource extends MapPropertySource {

    public static final String DEFAULT_PROPERTY_SOURCE_NAME = "ConfigPropertySource(application)";

    // Cached key set since MapPropertySource does not cache it
    private final String[] keys;

    // ************************************************************************
    // Constructors
    // - Use static factory methods instead
    // ************************************************************************

    protected ConfigPropertySource(String name, Config source) {
        super(name, SpringConfigUtils.flatten(source));
        this.keys = this.source.keySet().toArray(new String[0]);
    }

    // ************************************************************************
    // Factory Methods
    // ************************************************************************

    /**
     * Loads this property source using {@link ConfigFactory#load()}.
     */
    public static ConfigPropertySource load() {
        return load(DEFAULT_PROPERTY_SOURCE_NAME);
    }

    /**
     * Loads this property source using {@link ConfigFactory#load()}.
     *
     * @param propertySourceName The name of this property source.
     */
    public static ConfigPropertySource load(String propertySourceName) {
        Objects.requireNonNull(propertySourceName, "propertySourceName must not be null");
        return of(propertySourceName, ConfigFactory.load());
    }

    /**
     * Create this property source using a pre-loaded {@link Config}
     *
     * @param propertySourceName The name of this property source.
     * @param config             The config to wrap.
     */
    public static ConfigPropertySource of(String propertySourceName, Config config) {
        Objects.requireNonNull(propertySourceName, "propertySourceName must not be null");
        Objects.requireNonNull(config, "config must not be null");
        return new ConfigPropertySource(propertySourceName, config);
    }

    // ************************************************************************
    // MapPropertySource Overrides
    // ************************************************************************

    @Override
    public String[] getPropertyNames() {
        return keys;
    }
}