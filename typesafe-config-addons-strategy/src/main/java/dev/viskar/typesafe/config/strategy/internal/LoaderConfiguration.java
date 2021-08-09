package dev.viskar.typesafe.config.strategy.internal;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dev.viskar.typesafe.config.strategy.CustomConfigLoadingStrategy.CoreBuilder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

public class LoaderConfiguration implements CoreBuilder<LoaderConfiguration>, Callable<Config> {

    /**
     * Layers, sorted from lowest priority to highest priority.
     * Each layer that is added appended to the front at a lower priority than the configs before it.
     */
    private final Deque<Callable<? extends Config>> layers = new ArrayDeque<>();

    /**
     * Merges all the layers into a single Config.
     */
    @Override
    public Config call() throws Exception {

        Config config = ConfigFactory.empty();

        for (Callable<? extends Config> layer : layers) {
            Config layerConfig = layer.call();
            if (layerConfig != null) {
                config = layerConfig.withFallback(config);
            }
        }

        return config;
    }

    @Override
    public LoaderConfiguration with(Callable<? extends Config> configCallback) {
        layers.addFirst(configCallback);
        return this;
    }

}
