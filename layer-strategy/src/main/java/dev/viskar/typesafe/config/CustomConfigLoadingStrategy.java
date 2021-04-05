package dev.viskar.typesafe.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigLoadingStrategy;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.DefaultConfigLoadingStrategy;
import dev.viskar.typesafe.config.internal.AbstractParseFunctions;
import dev.viskar.typesafe.config.internal.DefaultParseFunctions;

import java.util.function.Supplier;

/**
 * A customizable {@link ConfigLoadingStrategy} that supports layering multiple configurations into a single config.
 * <p>
 * Once configured and installed, all calls to {@link ConfigFactory#load()} or
 * {@link ConfigFactory#defaultApplication()} will use this custom loading strategy.
 * <p>
 * Customize the factory using {@link #builder()}.
 * <p>
 * Install the built factory as the default using {@link #install()}.
 * <p>
 * If the runtime cannot use {@link System#setProperty(String, String)}, then instead:<ul>
 * <li>Set -Dconfig.strategy=dev.viskar.typesafe.config.layersCustomConfigLoadingStrategy</li>
 * <li>Invoke {@link #softInstall()} to when configuration is complete to activate.</li>
 * </ul>
 */
public class CustomConfigLoadingStrategy implements ConfigLoadingStrategy {

    private static CustomConfigLoadingStrategy installedInstance;

    private final ConfigLoadingStrategy impl;

    /**
     * Should not be invoked manually.
     * <p>
     * This default constructor exists so {@link ConfigFactory} can invoke this strategy when registered as the
     * default factory.
     */
    public CustomConfigLoadingStrategy() {
        CustomConfigLoadingStrategy instance = CustomConfigLoadingStrategy.installedInstance;
        this.impl = instance != null
                ? instance.impl
                : new DefaultConfigLoadingStrategy();
    }

    private CustomConfigLoadingStrategy(Supplier<Config> loader) {
        this.impl = parseOptions -> loader.get();
    }

    // ************************************************************************
    // ConfigLoadingStrategy
    // ************************************************************************

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Like {@link ConfigFactory#load()}, except applying this provider's customizations.
     */
    public Config load() {
        return ConfigFactory.load(parseApplicationConfig());
    }

    /**
     * Like {@link ConfigFactory#defaultApplication()}, except using this provider's customizations.
     */
    public Config parseApplicationConfig() {
        return parseApplicationConfig(ConfigParseOptions.defaults());
    }

    @Override
    public Config parseApplicationConfig(ConfigParseOptions parseOptions) {
        return impl.parseApplicationConfig(parseOptions);
    }

    // ************************************************************************
    // ConfigLoadingStrategy Hooks
    // ************************************************************************

    /**
     * Sets this instance as the default {@link ConfigLoadingStrategy}.
     * Once set, the standard {@link ConfigFactory} will delegate to this instance on load() and defaultApplication()
     */
    public void install() {
        // Register this class as the ConfigLoadingStrategy for ConfigFactory
        System.setProperty("config.strategy", getClass().getName());
        // Install
        softInstall();
    }

    public void softInstall() {
        // Assign the delegate to be used
        installedInstance = this;
        // Clear caches
        ConfigFactory.invalidateCaches();
    }

    /**
     * Unregister this loading strategy as the default strategy.
     */
    public void uninstall() {
        if (installedInstance == this) {
            installedInstance = null;
            ConfigFactory.invalidateCaches();
        }
    }

    // ************************************************************************
    // Builders
    // ************************************************************************

    public static class Builder extends AbstractParseFunctions<Builder> {

        private final DefaultParseFunctions layers = new DefaultParseFunctions();

        public CustomConfigLoadingStrategy build() {
            return new CustomConfigLoadingStrategy(layers);
        }

        public CustomConfigLoadingStrategy install() {
            CustomConfigLoadingStrategy strategy = build();
            strategy.install();
            return strategy;
        }

        @Override
        public Builder addConfig(Supplier<Config> config) {
            layers.addConfig(config);
            return this;
        }
    }

}
