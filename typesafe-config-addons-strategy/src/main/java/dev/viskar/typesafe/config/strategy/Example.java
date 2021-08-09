package dev.viskar.typesafe.config.strategy;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

public class Example {

    public static void main(String[] args) {
        CustomConfigLoadingStrategy
                .builder()
                .forEachProfile(new String[0], true, (profile, builder) -> builder
                        .with(ConfigFactory::parseResourcesAnySyntax, "application-" + profile, ConfigParseOptions.defaults().setAllowMissing(false))
                        .with(ConfigFactory::parseResourcesAnySyntax, "defaults-" + profile))
                .defaultApplication()
                .build();
    }

}
