package org.spongepowered.configurate.toml;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.*;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.loader.CommentHandler;
import org.spongepowered.configurate.loader.CommentHandlers;
import org.spongepowered.configurate.loader.ParsingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

public class TOMLConfigurationLoader extends AbstractConfigurationLoader<BasicConfigurationNode> {

    public static @NonNull Builder builder() { return new Builder(); }

    public static class Builder extends AbstractConfigurationLoader.Builder<Builder, TOMLConfigurationLoader> {
        private int keyIndent = 0;
        private int tableIndent = 0;
        private int arrayPadding = 0;
        private ZoneOffset zoneOffset = ZoneOffset.UTC;
        private boolean fractionalSeconds = false;

        public Builder setKeyIndent(int keyIndent) {
            this.keyIndent = keyIndent;
            return this;
        }

        public int getKeyIndent() {
            return keyIndent;
        }

        public Builder setTableIndent(int tableIndent) {
            this.tableIndent = tableIndent;
            return this;
        }

        public int getTableIndent() {
            return tableIndent;
        }

        public Builder setArrayPadding(int arrayPadding) {
            this.arrayPadding = arrayPadding;
            return this;
        }

        public int getArrayPadding() {
            return arrayPadding;
        }

        public Builder setZoneOffset(ZoneOffset zoneOffset) {
            this.zoneOffset = zoneOffset;
            return this;
        }

        public ZoneOffset getZoneOffset() {
            return zoneOffset;
        }

        public Builder setUseFractionalSeconds(boolean fractionalSeconds) {
            this.fractionalSeconds = fractionalSeconds;
            return this;
        }

        public boolean getUseFractionalSeconds() {
            return fractionalSeconds;
        }

        public TOMLConfigurationLoader build() {
            this.defaultOptions = this.defaultOptions().serializers(MoreTypes.getTypeSerializers());
            return new TOMLConfigurationLoader(this);
        }
    }

    private final int keyIndent;
    private final int tableIndent;
    private final int arrayPadding;
    private final ZoneOffset zoneOffset;
    private final boolean fractionalSeconds;

    private TOMLConfigurationLoader(Builder builder) {
        super(builder, new CommentHandler[]{CommentHandlers.HASH});
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

        if (fractionalSeconds) builder = builder.showFractionalSeconds();

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
            ConfigurationNode node = to.node(entry.getKey().replace("\"", ""));
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
        Map<String, Object> map = writeNode(node);
        try {
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
            } else if (node.raw() instanceof Date) {
                map.put(entry.getKey().toString(), MoreTypes.asDate(node.raw()));
            } else if (node.raw() instanceof Instant) {
                map.put(entry.getKey().toString(), MoreTypes.asInstant(node.raw()));
            } else {
                map.put(entry.getKey().toString(), node.raw());
            }
        }
        return map;
    }

    @Override
    public BasicConfigurationNode createNode(@NonNull ConfigurationOptions options) {
        Set<Class<?>> types = new HashSet<>();
        types.add(List.class);
        types.add(Map.class);
        types.add(Double.class);
        types.add(Float.class);
        types.add(Integer.class);
        types.add(Boolean.class);
        types.add(String.class);
        types.add(Long.class);
        types.add(Instant.class);
        types.add(Date.class);
        options = options.nativeTypes(types);
        return BasicConfigurationNode.root(options);
    }
}
