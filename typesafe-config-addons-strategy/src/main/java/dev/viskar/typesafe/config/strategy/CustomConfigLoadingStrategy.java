package dev.viskar.typesafe.config.strategy;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigLoadingStrategy;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.DefaultConfigLoadingStrategy;
import dev.viskar.typesafe.config.strategy.function.Func1;
import dev.viskar.typesafe.config.strategy.function.Func2;
import dev.viskar.typesafe.config.strategy.function.Func3;
import dev.viskar.typesafe.config.strategy.function.Func4;
import dev.viskar.typesafe.config.strategy.internal.Utils;
import dev.viskar.typesafe.config.strategy.internal.LoaderConfiguration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * A customizable {@link ConfigLoadingStrategy} that supports layering multiple configurations into a single config.
 * <p>
 * Once configured and installed, all calls to {@link ConfigFactory#load()} or
 * {@link ConfigFactory#defaultApplication()} will use this custom loading strategy.
 * <p>
 * Customize the factory using {@link #builder()}. See {@link Builder} for more details.
 * <p>
 * Install the built factory as the default using {@link #install()}.
 */
public class CustomConfigLoadingStrategy implements ConfigLoadingStrategy {

    private static final String STRATEGY_PROPERTY_NAME = Utils.detectPropertyName("config.strategy");

    private static ConfigLoadingStrategy installedImpl;

    private final ConfigLoadingStrategy impl;

    /**
     * Should not be invoked manually.
     * <p>
     * This default constructor exists so {@link ConfigFactory} can invoke this strategy when registered as the
     * default factory.
     */
    public CustomConfigLoadingStrategy() {
        ConfigLoadingStrategy installedImpl = CustomConfigLoadingStrategy.installedImpl;
        this.impl = installedImpl != null
                ? installedImpl
                : new DefaultConfigLoadingStrategy();
    }

    private CustomConfigLoadingStrategy(Callable<Config> loader) {
        this.impl = parseOptions -> {
            try {
                return loader.call();
            } catch (ConfigException e) {
                throw e;
            } catch (Exception e) {
                throw new ConfigException.Generic("Uncaught exception while loading config", e);
            }
        };
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
        System.setProperty(STRATEGY_PROPERTY_NAME, CustomConfigLoadingStrategy.class.getName());
        // Install
        softInstall();
    }

    public void softInstall() {
        // Assign the delegate to be used
        installedImpl = this.impl;
        // Clear caches
        ConfigFactory.invalidateCaches();
    }

    /**
     * Unregister this loading strategy as the default strategy.
     */
    public void uninstall() {
        if (installedImpl == this.impl) {
            installedImpl = null;
            ConfigFactory.invalidateCaches();
        }
    }

    // ************************************************************************
    // Builders
    // ************************************************************************

    /**
     * Fluent API to support a series of parse operation expressions that will get merged together when load() is invoked.
     * <p>
     * In the merging process, higher Configs win over lower Configs when reading from top to bottom.
     * <p>
     * Example using the provided methods:
     * <pre>
     *  .builder()
     *  .parseResourceAnySyntax("application-foo")
     *  .parseResourceAnySyntax("application")
     *  .parseResourceAnySyntax("defaults-foo")
     *  .parseResourceAnySyntax("defaults")
     *  .install();
     * </pre>
     * <p>
     * An alternative style is using the various {@link #with(Callable)} functions, which setup callbacks as specified.
     * This allows for calling arbitrary ConfigFactory functions, or even custom utility functions. Also a safe fallback
     * if a configuration you need isn't supported in the wrapper methods.
     * <pre>
     *  .builder()
     *  .with(ConfigFactory::parseResourceAnySyntax, "application-foo")
     *  .with(ConfigFactory::parseResourceAnySyntax, "application")
     *  .with(ConfigFactory::parseResourceAnySyntax, "defaults-foo")
     *  .with(ConfigFactory::parseResourceAnySyntax, "defaults")
     *  .install();
     * </pre>
     */
    public interface Builder extends CoreBuilder<Builder> {

        CustomConfigLoadingStrategy build();

        CustomConfigLoadingStrategy install();

    }

    /**
     * Fluent API to support a series of parse operation expressions that will get merged together when load() is invoked.
     * <p>
     * In the merging process, higher Configs win over lower Configs when reading from top to bottom.
     * <p>
     * Example using the provided methods:
     * <pre>
     *  .builder()
     *  .parseResourceAnySyntax("application-foo")
     *  .parseResourceAnySyntax("application")
     *  .parseResourceAnySyntax("defaults-foo")
     *  .parseResourceAnySyntax("defaults")
     *  .install();
     * </pre>
     * <p>
     * An alternative style is using the various {@link #with(Callable)} functions, which setup callbacks as specified.
     * This allows for calling arbitrary ConfigFactory functions, or even custom utility functions. Also a safe fallback
     * if a configuration you need isn't supported in the wrapper methods.
     * <pre>
     *  .builder()
     *  .with(ConfigFactory::parseResourceAnySyntax, "application-foo")
     *  .with(ConfigFactory::parseResourceAnySyntax, "application")
     *  .with(ConfigFactory::parseResourceAnySyntax, "defaults-foo")
     *  .with(ConfigFactory::parseResourceAnySyntax, "defaults")
     *  .install();
     * </pre>
     */
    public interface CoreBuilder<T extends CoreBuilder<T>> {

        // ********************************************************************
        // The 'with' API should be forward compatible with any ConfigFactory
        // functions added in the future since all invocations can be handled
        // with method references.
        // ********************************************************************

        T with(Callable<? extends Config> loader);

        /** Add a Config as a layer. Has less priority than earlier additions. */
        default T with(Config config) {
            return with(() -> config);
        }

        default <P1> T with(Func1<? super P1, ? extends Config> loader, P1 param1) {
            return with(() -> loader.apply(param1));
        }

        default <P1, P2> T with(Func2<? super P1, ? super P2, ? extends Config> loader, P1 param1, P2 param2) {
            return with(() -> loader.apply(param1, param2));
        }

        default <P1, P2, P3> T with(Func3<? super P1, ? super P2, ? super P3, ? extends Config> loader, P1 param1, P2 param2, P3 param3) {
            return with(() -> loader.apply(param1, param2, param3));
        }

        default <P1, P2, P3, P4> T with(Func4<? super P1, ? super P2, ? super P3, ? super P4, ? extends Config> loader, P1 param1, P2 param2, P3 param3, P4 param4) {
            return with(() -> loader.apply(param1, param2, param3, param4));
        }

        // ********************************************************************
        // Extra helpers that delegate to the with(..) APIs
        // If the function you want isn't here, just use the with() API
        // ********************************************************************

        /**
         * See {@link ConfigFactory#parseURL(URL)} (String)}
         *
         * @throws ConfigException.Generic if the URL is malformed.
         */
        default T parseURL(String url) throws ConfigException.Generic {
            try {
                return parseURL(new URL(url));
            } catch (MalformedURLException e) {
                throw new ConfigException.Generic("URL is not valid: " + url, e);
            }
        }

        /** See {@link ConfigFactory#parseURL(URL)} (String)} */
        default T parseURL(URL url) {
            return with(ConfigFactory::parseURL, url);
        }

        /** See {@link ConfigFactory#parseFile(File)} (String)} */
        default T parseFile(String file) {
            return parseFile(new File(file));
        }

        /** See {@link ConfigFactory#parseFile(File)} (String)} */
        default T parseFile(File file) {
            return with(ConfigFactory::parseFile, file);
        }

        /** See {@link ConfigFactory#parseResources(String)} */
        default T parseResources(String resources) {
            return with(ConfigFactory::parseResources, resources);
        }

        /** See {@link ConfigFactory#parseResourcesAnySyntax(String)} */
        default T parseResourcesAnySyntax(String resource) {
            return with(ConfigFactory::parseResourcesAnySyntax, resource);
        }

        /** See {@link DefaultConfigLoadingStrategy#parseApplicationConfig(ConfigParseOptions)} */
        default T defaultApplication() {
            return defaultApplication(ConfigParseOptions.defaults());
        }

        /** See {@link DefaultConfigLoadingStrategy#parseApplicationConfig(ConfigParseOptions)} */
        default T defaultApplication(ConfigParseOptions options) {
            // Make sure to call DefaultConfigLoadingStrategy for this
            // ConfigFactory::defaultApplication would be an infinite loop potentially
            return with(new DefaultConfigLoadingStrategy()::parseApplicationConfig, options);
        }

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
        default <P> T forEachProfile(Supplier<P[]> profiles, boolean preferFirst, BiConsumer<? super P, CoreBuilder<?>> builder) {
            return with(Utils.combineProfiles(profiles, preferFirst, builder));
        }

        /**
         * Configure this builder for each of the active profiles.
         *
         * @param <P>         A profile, typically a string.
         * @param profiles    An array of profiles to load.
         * @param preferFirst Whether the priority is given to the earlier or latter elements.
         * @param builder     A callback to configure this builder for each profile.
         */
        default <P> T forEachProfile(P[] profiles, boolean preferFirst, BiConsumer<? super P, CoreBuilder<?>> builder) {
            return with(Utils.combineProfiles(profiles, preferFirst, builder));
        }

    }

    static class BuilderImpl implements Builder {

        private final LoaderConfiguration loaderConfiguration = new LoaderConfiguration();

        @Override
        public CustomConfigLoadingStrategy build() {
            return new CustomConfigLoadingStrategy(loaderConfiguration);
        }

        @Override
        public CustomConfigLoadingStrategy install() {
            CustomConfigLoadingStrategy strategy = build();
            strategy.install();
            return strategy;
        }

        @Override
        public BuilderImpl with(Callable<? extends Config> config) {
            loaderConfiguration.with(config);
            return this;
        }

    }

}
