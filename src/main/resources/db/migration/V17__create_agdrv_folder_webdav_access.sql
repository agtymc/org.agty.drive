CREATE TABLE IF NOT EXISTS public.agdrv_folder_webdav_access (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    owner_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    access_token VARCHAR(128) NOT NULL,
    login_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    allow_write BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT agdrv_folder_webdav_access_owner_fk
        FOREIGN KEY (owner_id) REFERENCES public.agdrv_users (id),
    CONSTRAINT agdrv_folder_webdav_access_folder_fk
        FOREIGN KEY (folder_id) REFERENCES public.agdrv_folders (id),
    CONSTRAINT agdrv_folder_webdav_access_token_uq UNIQUE (access_token),
    CONSTRAINT agdrv_folder_webdav_access_login_ck CHECK (char_length(trim(login_name)) >= 3)
);

CREATE INDEX IF NOT EXISTS agdrv_folder_webdav_access_owner_idx
    ON public.agdrv_folder_webdav_access (owner_id, folder_id);

CREATE UNIQUE INDEX IF NOT EXISTS agdrv_folder_webdav_access_folder_active_uq
    ON public.agdrv_folder_webdav_access (folder_id)
    WHERE is_enabled = TRUE;
