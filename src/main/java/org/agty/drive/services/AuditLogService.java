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

import org.agty.drive.dto.AuditLogDto;
import org.agty.drive.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(Long actorUserId, String actionCode, String resourceType, Long resourceId, String details) {
        AuditLogDto dto = new AuditLogDto();
        dto.setActorUserId(actorUserId);
        dto.setActionCode(actionCode);
        dto.setResourceType(resourceType);
        dto.setResourceId(resourceId);
        dto.setDetails(details);
        auditLogRepository.save(dto);
    }

    public List<AuditLogDto> findRecent(int limit) {
        return auditLogRepository.findRecent(limit);
    }

    public long countAll() {
        return auditLogRepository.countAll();
    }

    public long countFiltered(String createdDate,
                              String actorLogin,
                              String actionCode,
                              String resourceQuery,
                              String details) {
        return auditLogRepository.countFiltered(createdDate, actorLogin, actionCode, resourceQuery, details);
    }

    public List<AuditLogDto> findPage(String sortMode, int offset, int limit) {
        return auditLogRepository.findPage(sortMode, offset, limit);
    }

    public List<AuditLogDto> findPage(String sortMode,
                                      int offset,
                                      int limit,
                                      String createdDate,
                                      String actorLogin,
                                      String actionCode,
                                      String resourceQuery,
                                      String details) {
        return auditLogRepository.findPage(sortMode, offset, limit, createdDate, actorLogin, actionCode, resourceQuery, details);
    }
}
