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

import org.agty.drive.dto.AppSettingDto;
import org.agty.drive.repository.AppSettingRepository;
import org.springframework.stereotype.Service;

@Service
public class AppSettingService {

    public static final String OPEN_REGISTRATION_ENABLED = "OPEN_REGISTRATION_ENABLED";

    private final AppSettingRepository appSettingRepository;

    public AppSettingService(AppSettingRepository appSettingRepository) {
        this.appSettingRepository = appSettingRepository;
    }

    public boolean isOpenRegistrationEnabled() {
        return getBoolean(OPEN_REGISTRATION_ENABLED, false);
    }

    public boolean updateOpenRegistrationEnabled(boolean enabled, Long actorUserId) {
        AppSettingDto dto = appSettingRepository.findByKey(OPEN_REGISTRATION_ENABLED);
        if (dto == null) {
            dto = new AppSettingDto();
            dto.setSettingKey(OPEN_REGISTRATION_ENABLED);
        }
        dto.setSettingValue(Boolean.toString(enabled));
        dto.setUpdatedBy(actorUserId);
        AppSettingDto saved = appSettingRepository.save(dto);
        return saved != null && Boolean.toString(enabled).equalsIgnoreCase(saved.getSettingValue());
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        AppSettingDto dto = appSettingRepository.findByKey(key);
        if (dto == null || dto.getSettingValue() == null || dto.getSettingValue().isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(dto.getSettingValue().trim());
    }
}
