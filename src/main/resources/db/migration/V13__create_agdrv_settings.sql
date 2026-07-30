CREATE TABLE IF NOT EXISTS public.agdrv_settings (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    setting_key VARCHAR(120) NOT NULL,
    setting_value TEXT,
    updated_by BIGINT,
    CONSTRAINT agdrv_settings_key_uq UNIQUE (setting_key),
    CONSTRAINT agdrv_settings_updated_by_fk
        FOREIGN KEY (updated_by) REFERENCES public.agdrv_users (id)
);

CREATE INDEX IF NOT EXISTS agdrv_settings_key_idx
    ON public.agdrv_settings (setting_key);
