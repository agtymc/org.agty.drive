ALTER TABLE public.agdrv_folders
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

ALTER TABLE public.agdrv_files
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS agdrv_folders_expires_at_idx
    ON public.agdrv_folders (expires_at)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS agdrv_files_expires_at_idx
    ON public.agdrv_files (expires_at)
    WHERE deleted_at IS NULL;
