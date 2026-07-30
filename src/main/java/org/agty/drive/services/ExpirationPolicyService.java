/*
 * Copyright 2026 Vladimir V
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
