package dev.viskar.typesafe.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigLoadingStrategy;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.DefaultConfigLoadingStrategy;
import dev.viskar.typesafe.config.internal.AbstractBuilder;
import dev.viskar.typesafe.config.internal.DefaultParseFunctions;

import java.io.File;
import java.net.URL;
import java.util.function.BiConsumer;
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
 * <li>Set -Dconfig.strategy=dev.viskar.typesafe.config.CustomConfigLoadingStrategy as a startup property.</li>
 * <li>Then invoke {@link #softInstall()} later when the strategy is configured at runtime.</li>
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
        return new BuilderImpl();
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

    /**
     * Fluent API to support a series of parse operation expressions that will get merged together.
     * <p>
     * In the merging process, higher Configs win over lower Configs when reading from top to bottom:
     * <pre>
     *  .builder()
     *  .parseResource("application-prod-eu")
     *  .parseResource("application-prod")
     *  .parseResource("application")
     * </pre>
     */
    public interface CoreBuilder<T extends CoreBuilder<T>> {

        /** Add a Config supplier as a layer. Has less priority than earlier additions. */
        T addConfig(Supplier<Config> config);

        /** Add a Config as a layer. Has less priority than earlier additions. */
        T addConfig(Config config);

        /**
         * Add a Config parsed from a URL as a layer. Has less priority than earlier additions.
         *
         * @throws ConfigException.Generic if the URL is malformed.
         */
        T parseURL(String url) throws ConfigException.Generic;

        /** Add a Config parsed from a URL as a layer. Has less priority than earlier additions. */
        T parseURL(URL url);

        /** Add a Config parsed from a File as a layer. Has less priority than earlier additions. */
        T parseFile(String file);

        /** Add a Config parsed from a File as a layer. Has less priority than earlier additions. */
        T parseFile(File file);

        /** Add a Config parsed from a Resource as a layer. Has less priority than earlier additions. */
        T parseResource(String resource);

        /** Add a Config parsed from the "application" Resource as a layer. Has less priority than earlier additions. */
        T defaultApplication();

        /**
         * Configure this builder for each of the active profiles.
         * <p>
         * The supplier will be called on each time the Config chain is loaded,
         * meaning future changes to the profiles will be detected on future reloads.
         *
         * @param <P>         A profile, typically a string.
         * @param profiles    Supplies an array of enabled profiles to load.
         * @param preferFirst Whether the priority is given to the earlier or latter elements.
         * @param builder     A callback to configure this builder for each profile.
         */
        <P> T forEachProfile(Supplier<P[]> profiles, boolean preferFirst, BiConsumer<? super P, CoreBuilder<?>> builder);

        /**
         * Configure this builder for each of the active profiles.
         *
         * @param <P>         A profile, typically a string.
         * @param profiles    An array of profiles to load.
         * @param preferFirst Whether the priority is given to the earlier or latter elements.
         * @param builder     A callback to configure this builder for each profile.
         */
        <P> T forEachProfile(P[] profiles, boolean preferFirst, BiConsumer<? super P, CoreBuilder<?>> builder);

    }

    public interface Builder extends CoreBuilder<Builder> {

        CustomConfigLoadingStrategy build();

        CustomConfigLoadingStrategy install() ;

    }

    static class BuilderImpl extends AbstractBuilder<Builder> implements Builder {

        private final DefaultParseFunctions layers = new DefaultParseFunctions();

        @Override
        public CustomConfigLoadingStrategy build() {
            return new CustomConfigLoadingStrategy(layers);
        }

        @Override
        public CustomConfigLoadingStrategy install() {
            CustomConfigLoadingStrategy strategy = build();
            strategy.install();
            return strategy;
        }

        @Override
        public BuilderImpl addConfig(Supplier<Config> config) {
            layers.addConfig(config);
            return this;
        }

    }

}
