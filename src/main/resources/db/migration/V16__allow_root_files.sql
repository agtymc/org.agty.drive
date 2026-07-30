ALTER TABLE public.agdrv_files
    ALTER COLUMN folder_id DROP NOT NULL;

DROP INDEX IF EXISTS public.agdrv_files_folder_name_uq;

CREATE UNIQUE INDEX IF NOT EXISTS agdrv_files_root_name_uq
    ON public.agdrv_files (owner_id, original_filename)
    WHERE folder_id IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS agdrv_files_folder_name_uq
    ON public.agdrv_files (folder_id, original_filename)
    WHERE folder_id IS NOT NULL AND deleted_at IS NULL;
