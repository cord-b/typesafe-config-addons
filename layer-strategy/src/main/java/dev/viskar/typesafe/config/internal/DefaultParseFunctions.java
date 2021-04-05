package dev.viskar.typesafe.config.internal;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.SneakyThrows;

import java.io.File;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class DefaultParseFunctions extends AbstractParseFunctions<DefaultParseFunctions> implements ParseFunctions<DefaultParseFunctions>, Supplier<Config> {

    /**
     * Layers, sorted from lowest priority to highest priority.
     * Each layer that is added appended to the front at a lower priority than the configs before it.
     */
    private final Deque<Supplier<Config>> layers = new ArrayDeque<>();

    /**
     * Merges all the layers into a single Config.
     */
    @Override
    public Config get() {

        Config config = ConfigFactory.empty();

        for (Supplier<Config> layer : layers) {
            Config layerConfig = layer.get();
            if (layerConfig != null) {
                config = layerConfig.withFallback(config);
            }
        }

        return config;
    }

    @Override
    public DefaultParseFunctions addConfig(Supplier<Config> configCallback) {
        layers.addFirst(configCallback);
        return this;
    }

}
