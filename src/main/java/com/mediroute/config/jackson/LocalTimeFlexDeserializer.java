package com.mediroute.config.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Deserializes a LocalTime from either a plain time string (HH:mm[:ss])
 * or a full ISO-8601 date/time string (e.g., 2025-08-14T14:05:00.000Z).
 * If a full date-time is provided, the time component is extracted.
 */
public class LocalTimeFlexDeserializer extends JsonDeserializer<LocalTime> {

    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        String text = p.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }

        // Try simple LocalTime first (HH:mm or HH:mm:ss)
        try {
            return LocalTime.parse(text, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (DateTimeParseException ignored) { }

        // Try OffsetDateTime (e.g., Z or offset present)
        try {
            OffsetDateTime odt = OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return odt.toLocalTime();
        } catch (DateTimeParseException ignored) { }

        // Try LocalDateTime (no zone info)
        try {
            LocalDateTime ldt = LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.toLocalTime();
        } catch (DateTimeParseException ignored) { }

        // Try epoch millis/seconds
        if (text.chars().allMatch(Character::isDigit)) {
            try {
                long epoch = Long.parseLong(text);
                Instant instant = epoch > 10_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
                return instant.atOffset(ZoneOffset.UTC).toLocalTime();
            } catch (Exception ignored) { }
        }

        // As a last resort, let Jackson report a standard error
        throw ctxt.weirdStringException(text, LocalTime.class, "Unsupported time format");
    }
}


