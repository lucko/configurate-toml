# configurate-toml
A [Configurate](https://github.com/SpongePowered/Configurate/) loader for the [TOML](https://github.com/toml-lang/toml) file format.

### Usage
configurate-toml is published to Maven Central with [group id: `me.lucko.configurate`, artifact id: `configurate-toml`](https://search.maven.org/artifact/me.lucko.configurate/configurate-toml).

```java
TOMLConfigurationLoader loader = TOMLConfigurationLoader.builder()
        .path(Paths.get("config.toml"))
        .build();

ConfigurationNode node = loader.load();
```
