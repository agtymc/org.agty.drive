CREATE TABLE IF NOT EXISTS public.agdrv_folder_collaborative_access (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    owner_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    password_hash VARCHAR(255),
    allow_write BOOLEAN NOT NULL DEFAULT FALSE,
    allow_delete BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT agdrv_folder_collaborative_access_owner_fk
        FOREIGN KEY (owner_id) REFERENCES public.agdrv_users (id),
    CONSTRAINT agdrv_folder_collaborative_access_folder_fk
        FOREIGN KEY (folder_id) REFERENCES public.agdrv_folders (id),
    CONSTRAINT agdrv_folder_collaborative_access_target_user_fk
        FOREIGN KEY (target_user_id) REFERENCES public.agdrv_users (id),
    CONSTRAINT agdrv_folder_collaborative_access_owner_target_ck
        CHECK (owner_id <> target_user_id)
);

CREATE INDEX IF NOT EXISTS agdrv_folder_collaborative_access_owner_idx
    ON public.agdrv_folder_collaborative_access (owner_id, is_enabled, folder_id);

CREATE INDEX IF NOT EXISTS agdrv_folder_collaborative_access_target_idx
    ON public.agdrv_folder_collaborative_access (target_user_id, is_enabled, folder_id);

CREATE UNIQUE INDEX IF NOT EXISTS agdrv_folder_collaborative_access_active_uq
    ON public.agdrv_folder_collaborative_access (owner_id, folder_id, target_user_id)
    WHERE is_enabled = TRUE;
