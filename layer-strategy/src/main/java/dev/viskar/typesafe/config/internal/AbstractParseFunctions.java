package dev.viskar.typesafe.config.internal;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class AbstractParseFunctions<T extends AbstractParseFunctions<T> & ParseFunctions<T>> implements ParseFunctions<T> {

    @Override
    public T addConfig(Config config) {
        return addConfig(() -> config);
    }

    @Override
    public T parseURL(String url) throws ConfigException.Generic {
        try {
            return parseURL(new URL(url));
        } catch (MalformedURLException e) {
            throw new ConfigException.Generic("URL is not valid: " + url, e);
        }
    }

    @Override
    public T parseURL(URL url) {
        return addConfig(() -> ConfigFactory.parseURL(url));
    }

    @Override
    public T parseFile(String file) {
        return parseFile(new File(file));
    }

    @Override
    public T parseFile(File file) {
        return addConfig(() -> ConfigFactory.parseFile(file));
    }

    @Override
    public T parseResource(String resource) {
        return addConfig(() -> ConfigFactory.parseResourcesAnySyntax(resource));
    }

    public T defaultApplication() {
        return parseResource("application");
    }

    @Override
    public <P> T forEachProfile(Supplier<P[]> profiles, boolean preferFirst, BiConsumer<? super P, ParseFunctions<?>> builder) {
        return addConfig(() -> combineProfiles(profiles.get(), preferFirst, builder).get());
    }

    @Override
    public <P> T forEachProfile(P[] profiles, boolean preferFirst, BiConsumer<? super P, ParseFunctions<?>> builder) {
        return addConfig(combineProfiles(profiles, preferFirst, builder));
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private <P> Supplier<Config> combineProfiles(P[] profiles, boolean preferFirst, BiConsumer<? super P, ParseFunctions<?>> builder) {
        DefaultParseFunctions collector = new DefaultParseFunctions();
        if (preferFirst) {
            for (int i = 0; i < profiles.length; i++) {
                builder.accept(profiles[i], collector);
            }
        } else {
            for (int i = profiles.length - 1; i >= 0; i--) {
                builder.accept(profiles[i], collector);
            }
        }
        return collector;
    }
}
