package dev.viskar.typesafe.config.strategy.internal;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dev.viskar.typesafe.config.strategy.CustomConfigLoadingStrategy.CoreBuilder;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class Utils {

    public static <P> Callable<Config> combineProfiles(Supplier<P[]> profiles, boolean preferFirst, BiConsumer<? super P, CoreBuilder<?>> builder) {
        return () -> {
            return combineProfiles(profiles.get(), preferFirst, builder).call();
        };
    }

    public static <P> Callable<Config> combineProfiles(P[] profiles, boolean preferFirst, BiConsumer<? super P, CoreBuilder<?>> builder) {
        LoaderConfiguration innerLoader = new LoaderConfiguration();
        if (preferFirst) {
            for (int i = 0; i < profiles.length; i++) {
                builder.accept(profiles[i], innerLoader);
            }
        } else {
            for (int i = profiles.length - 1; i >= 0; i--) {
                builder.accept(profiles[i], innerLoader);
            }
        }
        return innerLoader;
    }

    public static String detectPropertyName(String defaultValue) {
        String propName = defaultValue;
        try {
            Field f = ConfigFactory.class.getDeclaredField("STRATEGY_PROPERTY_NAME");
            f.setAccessible(true);
            propName = f.get(null).toString();
        } catch (Throwable ignore) {
            // ignore
        }
        return propName;
    }

}
