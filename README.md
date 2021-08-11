# Addons for Typesafe Config

This project contains addons to supplement Typesafe Config:
* **CustomConfigLoadingStrategy**
  * Implements `ConfigLoadingStrategy` that can be fully customized with a builder API, and then installed as the global `ConfigLoadingStrategy`.
 
 
* **ConfigPropertySource (for Spring Framework)**
  * A `PropertySoruce` wrapper for `Config` objects so that they can be loaded into Spring Framework. Handles converting property paths into Spring conventions (maps, array notation, etc).

## Artifacts
```xml
<dependency>
    <groupId>dev.viskar</groupId>
    <artifactId>typesafe-config-addons-strategy</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>dev.viskar</groupId>
    <artifactId>typesafe-config-addons-spring</artifactId>
    <version>${version}</version>
</dependency>
```

## CustomConfigLoadingStrategy

This addon provides a custom **ConfigLoadingStrategy** that replaces the default `ConfigFactory.load()` and `ConfigFactory.defaultApplication()` behavior.

### Usage
* Create a **CustomConfigLoadingStrategy** and customize it using the builder.
* Call `install()` to register this strategy as the global strategy, replacing the `ConfigFactory.load()` default behavior.
* Future calls to `ConfigFactory.load()` or `ConfigFactory.defaultApplication()` will now use this strategy.

### Example
In this example, let's assume the setup is happening in a Spring App with some profiles enabled
```java 
// In this example, let's assume the setup is happening in a Spring App with some profiles enabled
Supplier<String[]> activeProfiles = environment::getActiveProfiles;

// This controls whether the earlier(true) or latter(false) profiles 'win' in the array.
boolean preferFirst = false;

 CustomConfigLoadingStrategy
        .builder()
        // Adds overrides based on Spring's Environment.getActiveProfiles()
        .forEachProfile(activeProfiles, preferFirst, (profile, builder) -> {
            builder.parseResourceAnySyntax("application-" + profile);
        })
        .defaultApplication()
        // Perhaps some dependencies have embedded resources named "defaults-{profile}" that we want to load
        .forEachProfile(activeProfiles, preferFirst, (profile, builder) -> {
            builder.parseResourceAnySyntax("default-" + profile);
        })
        // Install this ConfigLoadingStrategy to be the default used by ConfigFactory
        .install();

// Setup complete
// Any future calls to load() will now delegate to this strategy:
Config config = ConfigFactory.load();
```

#### Notes on ordering
* All layering is performed from top to bottom, where the top layers win over the bottom layers.
* In the `forEachProfile(...)` blocks, the **true/false** boolean controls whether **earlier/latter** profiles win, respectively.

#### Additional Options

The builder also supports an alternative configuration style using the `with()` methods. This is flexible for any scenario,
and is useful to know when needing to invoke a method that the builder does not expose. 

```java 
CustomConfigLoadingStrategy
        .builder()
        .with(ConfigFactory::parseResources, "required.conf", ConfigParseOptions.defaults().setAllowMissing(false))
        .with(ConfigFactory::parseResourceAnySyntax, "application")
        .install();
```



## ConfigPropertySource (for Spring)

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

### Suggestions

When working in a Spring application, it is useful to pair this with the `CustomConfigLoadingStrategy` to handle Spring profiles. 

## Full Example (Strategy + Spring)
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
                        builder.parseResourceAnySyntax("application-"+profile);
                    })
                    // Then adds the application's base layer
                    .parseURL("http://config-server/example-application/application.conf")
                    .defaultApplication()
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

# Overrides from a config server would apply immediately on-top-of their respective application- or default- files.
- //config-server/example-application/application-prod.conf
- //config-server/example-application/application-dev.conf
- //config-server/example-application/application.conf
- //config-server/example-application/defaults-prod.conf
- //config-server/example-application/defaults-dev.conf
```
