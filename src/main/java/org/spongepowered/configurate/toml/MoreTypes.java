package org.spongepowered.configurate.toml;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

public class MoreTypes {

    static TypeSerializerCollection getTypeSerializers() {
        return TypeSerializerCollection.defaults().childBuilder()
                .register(Date.class, new DateSerializer())
                .register(Instant.class, new InstantSerializer())
                .build();
    }

    public @Nullable static Date asDate(@Nullable Object value) {
        if (value == null) return null;

        if (value instanceof Date) return (Date) value;

        if (value instanceof TemporalAccessor) {
            TemporalAccessor ta = (TemporalAccessor) value;
            if (ta.isSupported(ChronoField.INSTANT_SECONDS)) {
                long millis = ta.get(ChronoField.INSTANT_SECONDS) * 1000L;
                if (ta.isSupported(ChronoField.MILLI_OF_SECOND)) {
                    millis += ta.get(ChronoField.MILLI_OF_SECOND);
                }
                return new Date(millis);
            }
        }

        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }

        String dateString = value.toString();
        try {
            return DateFormat.getInstance().parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    @Nullable public static Date strictAsDate(@Nullable Object value) {
        return value == null ? null : (value instanceof Date ? (Date) value : null);
    }

    public @Nullable static Instant asInstant(@Nullable Object value) {
        if (value == null) return null;

        if (value instanceof Instant) return (Instant) value;

        if (value instanceof Date) {
            return Instant.ofEpochMilli(((Date) value).getTime());
        }

        if (value instanceof TemporalAccessor) {
            try {
                return Instant.from((TemporalAccessor) value);
            } catch (DateTimeException e) {
                return null;
            }
        }

        String instantString = value.toString();
        try {
            return Instant.parse(instantString);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Nullable public static Instant strictAsInstant(@Nullable Object value) {
        return value == null ? null : (value instanceof Instant ? (Instant) value : null);
    }

    private static class InstantSerializer implements TypeSerializer<Instant> {

        @Override
        public Instant deserialize(Type type, ConfigurationNode node) throws SerializationException {
            @Nullable Instant instant = asInstant(node.raw());
            return instant != null ? instant : Instant.EPOCH;
        }

        @Override
        public void serialize(Type type, @Nullable Instant obj, ConfigurationNode node) throws SerializationException {
            node.set(type, obj);
        }
    }

    private static class DateSerializer implements TypeSerializer<Date> {

        @Override
        public Date deserialize(Type type, ConfigurationNode node) throws SerializationException {
            @Nullable Date date = asDate(node.raw());
            return date != null ? date : Date.from(Instant.EPOCH);
        }

        @Override
        public void serialize(Type type, @Nullable Date obj, ConfigurationNode node) throws SerializationException {
            node.set(type, obj);
        }
    }

}
