package org.agty.drive.services;

import org.agty.drive.config.AppTime;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ExpirationPolicyService {

    public String normalizeExpirationInput(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        LocalDateTime parsed = AppTime.parseDateTimeInput(value);
        return parsed == null ? null : AppTime.formatForDatabase(parsed);
    }

    public String validateExpirationInput(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        LocalDateTime parsed = AppTime.parseDateTimeInput(value);
        if (parsed == null) {
            return "Укажите корректные дату и время срока жизни.";
        }
        if (!parsed.isAfter(AppTime.now())) {
            return "Срок жизни должен быть больше текущего времени.";
        }
        return null;
    }
}
