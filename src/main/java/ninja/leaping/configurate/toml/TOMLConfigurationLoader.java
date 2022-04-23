/*
 * Configurate
 * Copyright (C) zml and Configurate contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.leaping.configurate.toml;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.loader.CommentHandler;
import org.spongepowered.configurate.loader.CommentHandlers;
import org.spongepowered.configurate.loader.ParsingException;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * A loader for the TOML configurations, using the toml4j library for parsing and generation.
 */
public class TOMLConfigurationLoader extends AbstractConfigurationLoader<BasicConfigurationNode> {

    private static final Set<Class<?>> NATIVE_TYPES = UnmodifiableCollections.toSet(
            List.class, Map.class, Double.class, Instant.class, Float.class, Integer.class,
            Boolean.class, String.class, Long.class, Date.class
    );

    static final ConfigurationOptions DEFAULT_OPTIONS = ConfigurationOptions.defaults()
            .nativeTypes(NATIVE_TYPES)
            .serializers(MoreTypes.SERIALIZERS);

    /**
     * Creates a new {@link TOMLConfigurationLoader} builder.
     *
     * @return A new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link TOMLConfigurationLoader}.
     */
    public static class Builder extends AbstractConfigurationLoader.Builder<Builder, TOMLConfigurationLoader> {
        private int keyIndent = 0;
        private int tableIndent = 0;
        private int arrayPadding = 0;
        private ZoneOffset zoneOffset = ZoneOffset.UTC;
        private boolean fractionalSeconds = false;

        Builder() {
            this.defaultOptions(DEFAULT_OPTIONS);
        }

        /**
         * Sets the level of indentation the resultant loader should use for keys.
         *
         * @param indent The indent level for keys
         * @return This builder (for chaining)
         */
        @NonNull
        public Builder setKeyIndent(int indent) {
            this.keyIndent = indent;
            return this;
        }

        /**
         * Gets the level of key indentation to be used by the resultant loader.
         *
         * @return The indent level
         */
        public int getKeyIndent() {
            return keyIndent;
        }

        /**
         * Sets the level of indentation the resultant loader should use for tables.
         *
         * @param indent The indent level for tables
         * @return This builder (for chaining)
         */
        @NonNull
        public Builder setTableIndent(int indent) {
            this.tableIndent = indent;
            return this;
        }

        /**
         * Gets the level of table indentation to be used by the resultant loader.
         *
         * @return The indent level
         */
        public int getTableIndent() {
            return tableIndent;
        }

        /**
         * Sets the {@link ZoneOffset} the resultant loader should use.
         *
         * @param offset The zone offset
         * @return This builder (for chaining)
         */
        @NonNull
        public Builder setZoneOffset(@NonNull ZoneOffset offset) {
            this.zoneOffset = offset;
            return this;
        }

        /**
         * Gets the {@link ZoneOffset} to be used by the resultant loader.
         *
         * @return The zone offset
         */
        @NonNull
        public ZoneOffset getZoneOffset() {
            return zoneOffset;
        }

        /**
         * Sets the amount of padding the resultant loader should use for array delimiters.
         *
         * @param padding The padding level for arrays
         * @return This builder (for chaining)
         */
        @NonNull
        public Builder setArrayPadding(int padding) {
            this.arrayPadding = padding;
            return this;
        }

        /**
         * Gets the amount of array padding to be used by the resultant loader.
         *
         * @return The padding level for arrays
         */
        public int getArrayPadding() {
            return arrayPadding;
        }

        /**
         * Sets if the resultant loader should use fractional seconds.
         *
         * @param fractionalSeconds If fractional seconds should be used
         * @return This builder (for chaining)
         */
        @NonNull
        public Builder setUseFractionalSeconds(boolean fractionalSeconds) {
            this.fractionalSeconds = fractionalSeconds;
            return this;
        }

        /**
         * Gets if fractional seconds should be used by the resultant loader.
         *
         * @return If fractional seconds should be used
         */
        public boolean getUseFractionalSeconds() {
            return fractionalSeconds;
        }

        @NonNull
        @Override
        public TOMLConfigurationLoader build() {
            this.defaultOptions(o -> o.nativeTypes(NATIVE_TYPES));
            return new TOMLConfigurationLoader(this);
        }
    }

    private final int keyIndent;
    private final int tableIndent;
    private final int arrayPadding;
    private final ZoneOffset zoneOffset;
    private final boolean fractionalSeconds;

    private TOMLConfigurationLoader(Builder builder) {
        super(builder, new CommentHandler[] {CommentHandlers.HASH});
        this.keyIndent = builder.getKeyIndent();
        this.tableIndent = builder.getTableIndent();
        this.arrayPadding = builder.getArrayPadding();
        this.zoneOffset = builder.getZoneOffset();
        this.fractionalSeconds = builder.getUseFractionalSeconds();
    }

    private TomlWriter createWriter() {
        TomlWriter.Builder builder = new TomlWriter.Builder()
                .indentTablesBy(tableIndent)
                .indentValuesBy(keyIndent)
                .padArrayDelimitersBy(arrayPadding)
                .timeZone(TimeZone.getTimeZone(zoneOffset.getId()));

        if (fractionalSeconds) {
            builder = builder.showFractionalSeconds();
        }

        return builder.build();
    }

    @Override
    protected void loadInternal(BasicConfigurationNode node, BufferedReader reader) throws ParsingException {
        Toml toml = new Toml().read(reader);
        Map<String, Object> map = toml.toMap();
        readObject(map, node);
    }

    @SuppressWarnings("unchecked")
    private static void readObject(Map<String, Object> from, ConfigurationNode to) {
        for (Map.Entry<String, Object> entry : from.entrySet()) {
            ConfigurationNode node = to.node(entry.getKey().replace("\"",""));
            Object value = entry.getValue();

            if (value instanceof Map) {
                readObject((Map<String, Object>) value, node);
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (Object element : list) {
                    ConfigurationNode listNode = node.appendListNode();
                    if (element instanceof Map) {
                        readObject((Map<String, Object>) element, listNode);
                    } else {
                        listNode.raw(element);
                    }
                }
            } else {
                node.raw(value);
            }
        }
    }

    @Override
    protected void saveInternal(ConfigurationNode node, Writer writer) throws ConfigurateException {
        try {
            Map<String, Object> map = writeNode(node);
            createWriter().write(map, writer);
        } catch (IOException e) {
            throw ConfigurateException.wrap(node, e);
        }
    }

    private static Map<String, Object> writeNode(ConfigurationNode from) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : from.childrenMap().entrySet()) {
            ConfigurationNode node = entry.getValue();
            if (node.isList()) {
                List<Object> list = new ArrayList<>();
                for (ConfigurationNode listNode : node.childrenList()) {
                    if (listNode.isMap()) {
                        list.add(writeNode(listNode));
                    } else {
                        list.add(listNode.raw());
                    }
                }
                map.put(entry.getKey().toString(), list);
            } else if (node.isMap()) {
                map.put(entry.getKey().toString(), writeNode(node));
            } else if (node.raw() instanceof Instant) {
                map.put(entry.getKey().toString(), MoreTypes.asDate(node.raw()));
            } else {
                map.put(entry.getKey().toString(), node.raw());
            }
        }
        return map;
    }

    @Override
    public BasicConfigurationNode createNode(final ConfigurationOptions options) {
        return BasicConfigurationNode.root(options.nativeTypes(NATIVE_TYPES));
    }
}