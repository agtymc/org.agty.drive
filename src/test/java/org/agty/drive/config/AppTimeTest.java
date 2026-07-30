package org.agty.drive.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTimeTest {

    @Test
    void shouldFormatDatabaseDateTimeWithFiveFractionDigitsForTitle() {
        assertEquals("24.07.2026 18:04",
                AppTime.formatForTitle("2026-07-24 18:04:06.35237", "dd.MM.yyyy HH:mm"));
    }

    @Test
    void shouldFormatDatabaseDateTimeWithoutFractionForTitle() {
        assertEquals("24.07.2026 18:04",
                AppTime.formatForTitle("2026-07-24 18:04:06", "dd.MM.yyyy HH:mm"));
    }
}
