package org.agty.drive.repository;

import org.agty.agtysql.data.Arguments;
import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.converters.UserConverter;
import org.agty.drive.dao.AgtySQLPool;
import org.agty.drive.dao.ConnectionPool;
import org.agty.drive.dto.UserDto;
import org.agty.drive.entity.AgdrvUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    public UserDto findByLogin(String login) {
        if (login == null || login.isBlank()) {
            return null;
        }

        String query = """
                SELECT
                    u.id,
                    u.created_at,
                    u.updated_at,
                    u.login,
                    u.email,
                    u.password_hash,
                    u.role_code,
                    role_dic.title AS role_title,
                    u.status_code,
                    status_dic.title AS status_title,
                    u.first_name,
                    u.last_name,
                    u.middle_name,
                    u.display_name,
                    u.created_by,
                    u.last_login_at,
                    u.storage_quota_bytes,
                    u.two_factor_email_enabled,
                    u.two_factor_totp_enabled,
                    u.two_factor_totp_secret,
                    u.two_factor_totp_created_at,
                    u.two_factor_email_code_hash,
                    u.two_factor_email_code_expires_at
                FROM public.agdrv_users u
                LEFT JOIN public.agdrv_dic_users_roles role_dic ON role_dic.code = u.role_code
                LEFT JOIN public.agdrv_dic_users_statuses status_dic ON status_dic.code = u.status_code
                WHERE u.login = '%s'
                """.formatted(login.replace("'", "''"));

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : UserConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UserDto findById(Long id) {
        if (id == null) {
            return null;
        }

        String query = """
                SELECT
                    u.id,
                    u.created_at,
                    u.updated_at,
                    u.login,
                    u.email,
                    u.password_hash,
                    u.role_code,
                    role_dic.title AS role_title,
                    u.status_code,
                    status_dic.title AS status_title,
                    u.first_name,
                    u.last_name,
                    u.middle_name,
                    u.display_name,
                    u.created_by,
                    u.last_login_at,
                    u.storage_quota_bytes,
                    u.two_factor_email_enabled,
                    u.two_factor_totp_enabled,
                    u.two_factor_totp_secret,
                    u.two_factor_totp_created_at,
                    u.two_factor_email_code_hash,
                    u.two_factor_email_code_expires_at
                FROM public.agdrv_users u
                LEFT JOIN public.agdrv_dic_users_roles role_dic ON role_dic.code = u.role_code
                LEFT JOIN public.agdrv_dic_users_statuses status_dic ON status_dic.code = u.status_code
                WHERE u.id = %d
                """.formatted(id);

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : UserConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long countAll() {
        String query = "SELECT COUNT(*) AS total FROM public.agdrv_users";

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            Long total = row == null ? null : row.getLong("total");
            return total == null ? 0L : total;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UserDto save(UserDto userDto) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            AgdrvUser saved = sql.sql().saveEntityWithCheck(UserConverter.dtoToEntity(userDto));
            if (saved == null || saved.getId() == null) {
                log.error("Failed to save agdrv user. login={}, errors={}", userDto.getLogin(), sql.sql().getErrors());
                return null;
            }
            return findById(saved.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<UserDto> findAll() {
        List<UserDto> result = new ArrayList<>();

        String query = """
                SELECT
                    u.id,
                    u.created_at,
                    u.updated_at,
                    u.login,
                    u.email,
                    u.password_hash,
                    u.role_code,
                    role_dic.title AS role_title,
                    u.status_code,
                    status_dic.title AS status_title,
                    u.first_name,
                    u.last_name,
                    u.middle_name,
                    u.display_name,
                    u.created_by,
                    u.last_login_at,
                    u.storage_quota_bytes,
                    u.two_factor_email_enabled,
                    u.two_factor_totp_enabled,
                    u.two_factor_totp_secret,
                    u.two_factor_totp_created_at,
                    u.two_factor_email_code_hash,
                    u.two_factor_email_code_expires_at
                FROM public.agdrv_users u
                LEFT JOIN public.agdrv_dic_users_roles role_dic ON role_dic.code = u.role_code
                LEFT JOIN public.agdrv_dic_users_statuses status_dic ON status_dic.code = u.status_code
                ORDER BY u.id ASC
                """;

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(Arguments.builder().setQuery(query))) != null) {
                result.add(UserConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public boolean existsByLogin(String login, Long excludeId) {
        if (login == null || login.isBlank()) {
            return false;
        }
        String excludeCondition = excludeId == null ? "" : " AND id <> %d".formatted(excludeId);
        String query = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_users
                WHERE lower(login) = lower('%s')
                %s
                """.formatted(login.trim().replace("'", "''"), excludeCondition);
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            Long total = row == null ? null : row.getLong("total");
            return total != null && total > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean existsByEmail(String email, Long excludeId) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String excludeCondition = excludeId == null ? "" : " AND id <> %d".formatted(excludeId);
        String query = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_users
                WHERE lower(email) = lower('%s')
                %s
                """.formatted(email.trim().replace("'", "''"), excludeCondition);
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            Long total = row == null ? null : row.getLong("total");
            return total != null && total > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
