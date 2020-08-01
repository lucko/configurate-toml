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

import com.google.common.reflect.TypeToken;

import org.checkerframework.checker.nullness.qual.Nullable;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

import java.text.DateFormat;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * Extension of {@link ninja.leaping.configurate.Types}.
 */
class MoreTypes {

    static {
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(Instant.class), new InstantSerializer());
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(Date.class), new DateSerializer());
    }

    static void registerSerializers() {
        // to ensure the static initializer above has been called.
    }

    /**
     * Attempts to convert <code>value</code> to a {@link Date}.
     *
     * <p>
     * <ul>
     *     <li>If <code>value</code> is a {@link Date}, casts and returns</li>
     *     <li>If <code>value</code> is a {@link TemporalAccessor}, returns a converted value</li>
     *     <li>If <code>value</code> is a {@link Number}, returns a converted as if the number is a
     *     unix timestamp in milliseconds</li>
     *     <li>Otherwise, <code>value</code> is converted {@link Object#toString() to a string}, and
     *     then {@link DateFormat#parse(String) parsed}.</li>
     * </ul>
     *
     * <p>Returns null if <code>value</code> is null.</p>
     *
     * @param value The value
     * @return <code>value</code> as a {@link Date}, or null
     */
    @Nullable
    public static Date asDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Date) {
            return (Date) value;
        }

        if (value instanceof TemporalAccessor) {
            TemporalAccessor temp = (TemporalAccessor) value;
            if (temp.isSupported(ChronoField.INSTANT_SECONDS)) {
                long millis = temp.get(ChronoField.INSTANT_SECONDS) * 1000;
                if (temp.isSupported(ChronoField.MILLI_OF_SECOND)) {
                    millis += temp.get(ChronoField.MILLI_OF_SECOND);
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
        } catch (ParseException ex) {
            return null;
        }
    }

    /**
     * Returns <code>value</code> if it is a {@link Date}.
     *
     * @param value The value
     * @return <code>value</code> as a {@link Date}, or null
     */
    @Nullable
    public static Date strictAsDate(Object value) {
        if (value == null) {
            return null;
        }

        return value instanceof Date ? (Date) value : null;
    }

    /**
     * Attempts to convert <code>value</code> to a {@link Instant}.
     *
     * <p>
     * <ul>
     *     <li>If <code>value</code> is a {@link Instant}, casts and returns</li>
     *     <li>If <code>value</code> is a {@link Date}, returns a converted value</li>
     *     <li>If <code>value</code> is a {@link TemporalAccessor}, returns a converted value</li>
     *     <li>If <code>value</code> is a {@link Number}, returns a converted as if the number is a
     *     unix timestamp in milliseconds</li>
     *     <li>Otherwise, <code>value</code> is converted {@link Object#toString() to a string}, and
     *     then {@link Instant#parse(CharSequence) parsed}.</li>
     * </ul>
     *
     * <p>Returns null if <code>value</code> is null.</p>
     *
     * @param value The value
     * @return <code>value</code> as a {@link Instant}, or null
     */
    @Nullable
    public static Instant asInstant(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Instant) {
            return (Instant) value;
        }

        if (value instanceof Date) {
            return Instant.ofEpochMilli(((Date) value).getTime());
        }

        if (value instanceof TemporalAccessor) {
            try {
                return Instant.from((TemporalAccessor) value);
            } catch (DateTimeException ex) {
                return null;
            }
        }

        String dateString = value.toString();
        try {
            return Instant.parse(dateString);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * Returns <code>value</code> if it is a {@link Instant}.
     *
     * @param value The value
     * @return <code>value</code> as a {@link Instant}, or null
     */
    @Nullable
    public static Instant strictAsInstant(Object value) {
        if (value == null) {
            return null;
        }

        return value instanceof Instant ? (Instant) value : null;
    }

    private static class InstantSerializer implements TypeSerializer<Instant> {

        @Override
        public Instant deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
            return asInstant(value.getValue());
        }

        @Override
        public void serialize(TypeToken<?> type, Instant obj, ConfigurationNode value) throws ObjectMappingException {
            value.setValue(obj);
        }
    }

    private static class DateSerializer implements TypeSerializer<Date> {

        @Override
        public Date deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
            return asDate(value.getValue());
        }

        @Override
        public void serialize(TypeToken<?> type, Date obj, ConfigurationNode value) throws ObjectMappingException {
            value.setValue(obj);
        }
    }
}
