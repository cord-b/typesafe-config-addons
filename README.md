# Addons for Typesafe Config

This project contains addons to enhance Typesafe Config usage:
* **Custom Loading Strategy Addon** - Helps configure application environments that have multiple layers/profiles of Configs to apply. A common situation in application deployments.
* **Spring PropertySource Addon** - Provides a `PropertySoruce` wrapper for `Config` objects. Back your Spring Environment with your Typesafe Config properties. Take advantage of extra features like **import**s, and URL loading.

## Getting these Dependencies
Artifacts are available in the [jitpack.io](https://jitpack.io/) repository. See examples below:
* Note: `{version}` can either be a git tag (`x.y.z`) or a branch (`branch-SNAPSHOT`).

### Maven
```xml
<!-- Add jitpack repository -->
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
```xml
<!-- All Modules -->
<dependency>
  <groupId>dev.viskar</groupId>
  <artifactId>typesafe-config-addons</artifactId>
  <version>${version}</version>
</dependency>
```
```xml
<!-- Or Individual Dependencies -->
<dependency>
  <groupId>dev.viskar.typesafe-config-addons</groupId>
  <artifactId>layer-strategy</artifactId>
  <version>${version}</version>
</dependency>
<dependency>
    <groupId>dev.viskar.typesafe-config-addons</groupId>
    <artifactId>spring</artifactId>
    <version>${version}</version>
</dependency>
```

### Gradle
```groovy
// Add Repository
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

```groovy
// All modules
dependencies {
  implementation 'dev.viskar:typesafe-config-addons:{VERSION}'
}
```
```groovy
// Individual modules
dependencies {
  implementation 'dev.viskar.typesafe-config-addons:layer-strategy:{VERSION}'
  implementation 'dev.viskar.typesafe-config-addons:spring:{VERSION}'
}
```

## Custom Loading Strategy Addon

Customize a `ConfigLoadingStrategy` to assemble a `Config` object merged from several sources. This is an alternative to using  `-Dconfig.(resource|file|url)={resource}` which requires a tailored resource that specifies a whole series of `import`s for a similar result.

The custom strategy can be activated as the default ConfigLoadingStrategy, making all calls to `ConfigFactory.load()` follow this behavior.

### Usage
Chain a series of `parse*()` and `forEachProfile()` statements to express the Config loading strategy.
```java
 // Setup layers using environment.getActiveProfiles() 
 CustomConfigLoadingStrategy
        .builder()
        // Adds overrides based on Spring's Environment.getActiveProfiles()
        .forEachProfile(env::getActiveProfiles, false, (profile, builder) -> {
            builder.parseResource("application-" + profile);
        })
        // Default Application layer
        .parseResource("application")
        // Perhaps some dependencies have embedded resources named "defaults-{profile}" that we want to load
        .forEachProfile(env::getActiveProfiles, false, (profile, builder) -> {
            builder.parseResource("default-" + profile);
        })
        // Install this ConfigLoadingStrategy to be the default used by ConfigFactory
        .install();

// Any library calling load() will now get the customized configuration:
Config config = ConfigFactory.load();
```

All layering is performed from top to bottom, where the top layers win over the bottom layers. In the `forEachProfile(...)` blocks, the `true/false` boolean controls whether earlier or latter profiles are given higher priority.
  
Once the strategy is set, call either `build()` or `install()`.
* `build()` - Returns the CustomConfigLoadingStrategy for programmatic usage.
* `install()` - Sets this CustomConfigLoadingStrategy as the global default used by `ConfigFactory.load()` and `ConfigFactory.defaultApplication()`.

## Spring PropertySource Addon

This Spring addon can wrap a `Config` object as a `PropertySource`. The Config property paths are flattened and prepared in a way that is compatible with how Spring performs property loading.

### Usage

```java
@Override
protected void configureEnvironment(ConfigurableEnvironment env, String[] args) {
    // Load this property source
    ConfigPropertySource propertySource = ConfigPropertySource.load();
    // Add it to Spring
    env.getPropertySources().addLast(propertySource);
}
```

* `ConfigPropertySource.load()` - Loads the property source using `ConfigFactory.load()`.
* `ConfigPropertySource.load(propertySourceName)` - Same as `load()`, but gives the property source a custom name.
* `ConfigPropertySource.of(propertySourceName, config)` - Wrap your own `Config` with this property source.

### Suggestions

When working in a Spring application, it is recommended to also use the Custom Loading Strategy Addon to handle Spring profile layering. 

## Full Example (Config Strategy + Spring)
This example shows how you can combine the custom strategy and Spring PropertySource to fully configure your Spring application using Typesafe Config, instead of Spring property files.

```java
@SpringBootApplication
public class ExampleApplication {
    
    public static void main(String[] args) {
        new SpringApplication(ExampleApplication.class) {

            @Override
            protected void configureEnvironment(ConfigurableEnvironment env, String[] args) {
                super.configureEnvironment(environment, args);

                // (1) Example to configure Spring profiles as layers.
                CustomConfigLoadingStrategy.builder()
                    // Adds overrides based on Spring's Environment.getActiveProfiles()
                    // Let's load local resources and remote configurations from a config server
                    .forEach(env::getActiveProfiles, false, (profile, builder) -> {
                        builder.parseURL("http://config-server/example-application/application-"+profile+".conf");
                        builder.parseResource("application-"+profile);
                    })
                    // Then adds the application's base layer
                    .parseURL("http://config-server/example-application/application.conf");
                    .parseResource("application");
                    // Allow for defaults too (maybe dependencies include these)
                    .forEach(env::getActiveProfiles, false, (profile, builder) -> {
                        builder.parseURL("http://config-server/example-application/defaults-"+profile+".conf");
                        builder.parseResource("defaults-"+profile);
                    })
                    // Install this ConfigLoadingStrategy to be the default used by ConfigFactory
                    .install();

                // Inject the ConfigPropertySource into Spring Environment:
                // We can simply use 'ConfigPropertySource.load()' because 'install()' has been performed.
                env.getPropertySources().addLast(ConfigPropertySource.load());
            }

        }.run(args);
    }
}
```
The above code assembles an application environment that can load from various property sources:
```
# My application's static config would be detected:
# src/main/resources/
- application-prod.conf
- application-dev.conf
- application.conf
- reference.conf
  
# My dependencies with embedded config would be picked up
- defaults-prod.conf
- defaults-dev.conf
- reference.conf

# Overrides from a config server would apply on-top-of their respective application- or default- files.
- /example-application/application-prod.conf
- /example-application/application-dev.conf
- /example-application/application.conf
- /example-application/defaults-prod.conf
- /example-application/defaults-dev.conf
```
