package org.agty.drive.repository;

import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.converters.AppSettingConverter;
import org.agty.drive.dao.AgtySQLPool;
import org.agty.drive.dao.ConnectionPool;
import org.agty.drive.dto.AppSettingDto;
import org.agty.drive.entity.AgdrvSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class AppSettingRepository {

    private static final Logger log = LoggerFactory.getLogger(AppSettingRepository.class);

    public AppSettingDto save(AppSettingDto dto) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            AgdrvSetting saved = sql.sql().saveEntityWithCheck(AppSettingConverter.dtoToEntity(dto));
            if (saved == null || saved.getId() == null) {
                log.error("Failed to save app setting. key={}, errors={}", dto.getSettingKey(), sql.sql().getErrors());
                return null;
            }
            return findById(saved.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public AppSettingDto findById(Long id) {
        if (id == null) {
            return null;
        }
        return fetchOne(baseSelect() + " WHERE s.id = " + id);
    }

    public AppSettingDto findByKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String query = baseSelect() + " WHERE s.setting_key = '%s'".formatted(key.trim().replace("'", "''"));
        return fetchOne(query);
    }

    private AppSettingDto fetchOne(String query) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            if (row == null || row.getLong("id") == null) {
                return null;
            }
            return AppSettingConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String baseSelect() {
        return """
                SELECT
                    s.id,
                    s.created_at,
                    s.updated_at,
                    s.setting_key,
                    s.setting_value,
                    s.updated_by
                FROM public.agdrv_settings s
                """;
    }
}
