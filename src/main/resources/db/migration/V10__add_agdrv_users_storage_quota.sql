ALTER TABLE public.agdrv_users
    ADD COLUMN IF NOT EXISTS storage_quota_bytes BIGINT NOT NULL DEFAULT 104857600;

UPDATE public.agdrv_users
SET storage_quota_bytes = 104857600
WHERE storage_quota_bytes IS NULL OR storage_quota_bytes <= 0;
