package dev.viskar.typesafe.config.internal;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import lombok.SneakyThrows;

import java.io.File;
import java.net.URL;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

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
public interface ParseFunctions<T extends ParseFunctions<T>> {

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
    <P> T forEachProfile(Supplier<P[]> profiles, boolean preferFirst, BiConsumer<? super P, ParseFunctions<?>> builder);

    /**
     * Configure this builder for each of the active profiles.
     *
     * @param <P>         A profile, typically a string.
     * @param profiles    An array of profiles to load.
     * @param preferFirst Whether the priority is given to the earlier or latter elements.
     * @param builder     A callback to configure this builder for each profile.
     */
    <P> T forEachProfile(P[] profiles, boolean preferFirst, BiConsumer<? super P, ParseFunctions<?>> builder);

}