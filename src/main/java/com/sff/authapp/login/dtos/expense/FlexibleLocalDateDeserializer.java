package com.sff.authapp.login.dtos.expense;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.trim();
        try {
            // Case 1: Standard YYYY-MM-DD
            if (text.length() == 10 && text.charAt(4) == '-' && text.charAt(7) == '-') {
                return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            // Case 2: ISO datetime with 'T' (e.g. 2026-10-05T14:30:00 or with Z/+offset)
            if (text.contains("T")) {
                if (text.endsWith("Z") || text.contains("+")) {
                    return OffsetDateTime.parse(text).toLocalDate();
                }
                return LocalDateTime.parse(text).toLocalDate();
            }
            // Fallback general parse
            return LocalDate.parse(text);
        } catch (Exception e) {
            throw new IOException("Failed to parse date string: " + text, e);
        }
    }
}
