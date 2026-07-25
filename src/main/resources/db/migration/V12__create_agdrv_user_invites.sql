CREATE TABLE IF NOT EXISTS public.agdrv_user_invites (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL,
    login VARCHAR(120) NOT NULL,
    email VARCHAR(255),
    display_name VARCHAR(255),
    role_code VARCHAR(32) NOT NULL DEFAULT 'ROLE_USER',
    status_code VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    storage_quota_bytes BIGINT NOT NULL DEFAULT 104857600,
    expires_at TIMESTAMP,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    used_at TIMESTAMP,
    invited_user_id BIGINT,
    CONSTRAINT agdrv_user_invites_created_by_fk
        FOREIGN KEY (created_by) REFERENCES public.agdrv_users (id),
    CONSTRAINT agdrv_user_invites_invited_user_fk
        FOREIGN KEY (invited_user_id) REFERENCES public.agdrv_users (id),
    CONSTRAINT agdrv_user_invites_token_uq UNIQUE (token),
    CONSTRAINT agdrv_user_invites_role_code_ck CHECK (role_code IN ('ROLE_ADMIN', 'ROLE_USER')),
    CONSTRAINT agdrv_user_invites_status_code_ck CHECK (status_code IN ('ACTIVE', 'BLOCKED', 'INVITED', 'DISABLED')),
    CONSTRAINT agdrv_user_invites_storage_quota_ck CHECK (storage_quota_bytes > 0)
);

CREATE INDEX IF NOT EXISTS agdrv_user_invites_created_by_idx
    ON public.agdrv_user_invites (created_by);

CREATE INDEX IF NOT EXISTS agdrv_user_invites_enabled_idx
    ON public.agdrv_user_invites (is_enabled, used_at);

CREATE INDEX IF NOT EXISTS agdrv_user_invites_login_idx
    ON public.agdrv_user_invites (login);
