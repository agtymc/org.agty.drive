CREATE TABLE IF NOT EXISTS public.agdrv_users (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    login VARCHAR(120) NOT NULL,
    email VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    role_code VARCHAR(32) NOT NULL DEFAULT 'ROLE_USER',
    status_code VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    first_name VARCHAR(120),
    last_name VARCHAR(120),
    middle_name VARCHAR(120),
    display_name VARCHAR(255),
    created_by BIGINT,
    last_login_at TIMESTAMP,
    CONSTRAINT agdrv_users_login_uq UNIQUE (login),
    CONSTRAINT agdrv_users_email_uq UNIQUE (email),
    CONSTRAINT agdrv_users_role_code_ck CHECK (role_code IN ('ROLE_ADMIN', 'ROLE_USER')),
    CONSTRAINT agdrv_users_status_code_ck CHECK (status_code IN ('ACTIVE', 'BLOCKED', 'INVITED', 'DISABLED')),
    CONSTRAINT agdrv_users_created_by_fk
        FOREIGN KEY (created_by) REFERENCES public.agdrv_users (id)
);

CREATE TABLE IF NOT EXISTS public.agdrv_folders (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    owner_id BIGINT NOT NULL,
    parent_id BIGINT,
    name VARCHAR(255) NOT NULL,
    path_key VARCHAR(2000) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT agdrv_folders_owner_fk
        FOREIGN KEY (owner_id) REFERENCES public.agdrv_users (id),
    CONSTRAINT agdrv_folders_parent_fk
        FOREIGN KEY (parent_id) REFERENCES public.agdrv_folders (id),
    CONSTRAINT agdrv_folders_name_ck CHECK (char_length(trim(name)) > 0),
    CONSTRAINT agdrv_folders_path_key_ck CHECK (char_length(trim(path_key)) > 0)
);

CREATE TABLE IF NOT EXISTS public.agdrv_files (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    owner_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_filename VARCHAR(255),
    extension VARCHAR(32),
    mime_type VARCHAR(255),
    checksum VARCHAR(128),
    file_size BIGINT NOT NULL DEFAULT 0,
    is_image BOOLEAN NOT NULL DEFAULT FALSE,
    is_video BOOLEAN NOT NULL DEFAULT FALSE,
    preview_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
    description TEXT,
    CONSTRAINT agdrv_files_owner_fk
        FOREIGN KEY (owner_id) REFERENCES public.agdrv_users (id),
    CONSTRAINT agdrv_files_folder_fk
        FOREIGN KEY (folder_id) REFERENCES public.agdrv_folders (id),
    CONSTRAINT agdrv_files_file_size_ck CHECK (file_size >= 0),
    CONSTRAINT agdrv_files_preview_status_ck CHECK (preview_status IN ('NONE', 'READY', 'FAILED', 'PENDING'))
);

CREATE TABLE IF NOT EXISTS public.agdrv_file_content (
    file_id BIGINT PRIMARY KEY,
    content_oid OID,
    content_bytea BYTEA,
    content_size BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT agdrv_file_content_file_fk
        FOREIGN KEY (file_id) REFERENCES public.agdrv_files (id) ON DELETE CASCADE,
    CONSTRAINT agdrv_file_content_size_ck CHECK (content_size >= 0),
    CONSTRAINT agdrv_file_content_payload_ck CHECK (
            content_oid IS NOT NULL
            OR content_bytea IS NOT NULL
        )
);

CREATE TABLE IF NOT EXISTS public.agdrv_share_links (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL,
    resource_type VARCHAR(16) NOT NULL,
    resource_id BIGINT NOT NULL,
    title VARCHAR(255),
    password_hash VARCHAR(255),
    expires_at TIMESTAMP,
    allow_download BOOLEAN NOT NULL DEFAULT TRUE,
    allow_preview BOOLEAN NOT NULL DEFAULT TRUE,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_downloads BIGINT,
    download_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT agdrv_share_links_created_by_fk
        FOREIGN KEY (created_by) REFERENCES public.agdrv_users (id),
    CONSTRAINT agdrv_share_links_token_uq UNIQUE (token),
    CONSTRAINT agdrv_share_links_resource_type_ck CHECK (resource_type IN ('FILE', 'FOLDER')),
    CONSTRAINT agdrv_share_links_max_downloads_ck CHECK (max_downloads IS NULL OR max_downloads >= 0),
    CONSTRAINT agdrv_share_links_download_count_ck CHECK (download_count >= 0)
);

CREATE INDEX IF NOT EXISTS agdrv_users_status_idx
    ON public.agdrv_users (status_code);

CREATE INDEX IF NOT EXISTS agdrv_users_role_idx
    ON public.agdrv_users (role_code);

CREATE INDEX IF NOT EXISTS agdrv_folders_owner_idx
    ON public.agdrv_folders (owner_id);

CREATE INDEX IF NOT EXISTS agdrv_folders_parent_idx
    ON public.agdrv_folders (parent_id);

CREATE INDEX IF NOT EXISTS agdrv_folders_path_key_idx
    ON public.agdrv_folders (owner_id, path_key);

CREATE UNIQUE INDEX IF NOT EXISTS agdrv_folders_root_name_uq
    ON public.agdrv_folders (owner_id, name)
    WHERE parent_id IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS agdrv_folders_child_name_uq
    ON public.agdrv_folders (owner_id, parent_id, name)
    WHERE parent_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS agdrv_files_owner_idx
    ON public.agdrv_files (owner_id);

CREATE INDEX IF NOT EXISTS agdrv_files_folder_idx
    ON public.agdrv_files (folder_id);

CREATE INDEX IF NOT EXISTS agdrv_files_mime_type_idx
    ON public.agdrv_files (mime_type);

CREATE UNIQUE INDEX IF NOT EXISTS agdrv_files_folder_name_uq
    ON public.agdrv_files (folder_id, original_filename)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS agdrv_share_links_created_by_idx
    ON public.agdrv_share_links (created_by);

CREATE INDEX IF NOT EXISTS agdrv_share_links_lookup_idx
    ON public.agdrv_share_links (resource_type, resource_id);

CREATE INDEX IF NOT EXISTS agdrv_share_links_expires_at_idx
    ON public.agdrv_share_links (expires_at);
