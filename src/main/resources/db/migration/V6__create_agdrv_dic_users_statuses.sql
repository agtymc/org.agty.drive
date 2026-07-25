CREATE TABLE IF NOT EXISTS public.agdrv_dic_users_statuses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    align INTEGER NOT NULL DEFAULT 0,
    disabled BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT agdrv_dic_users_statuses_code_uq UNIQUE (code)
);

INSERT INTO public.agdrv_dic_users_statuses (code, title, align, disabled)
SELECT 'ACTIVE', 'Активен', 10, FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM public.agdrv_dic_users_statuses
    WHERE code = 'ACTIVE'
);

INSERT INTO public.agdrv_dic_users_statuses (code, title, align, disabled)
SELECT 'BLOCKED', 'Заблокирован', 20, FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM public.agdrv_dic_users_statuses
    WHERE code = 'BLOCKED'
);

INSERT INTO public.agdrv_dic_users_statuses (code, title, align, disabled)
SELECT 'INVITED', 'Приглашен', 30, FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM public.agdrv_dic_users_statuses
    WHERE code = 'INVITED'
);

INSERT INTO public.agdrv_dic_users_statuses (code, title, align, disabled)
SELECT 'DISABLED', 'Отключен', 40, FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM public.agdrv_dic_users_statuses
    WHERE code = 'DISABLED'
);

ALTER TABLE public.agdrv_users
    DROP CONSTRAINT IF EXISTS agdrv_users_status_code_ck;

ALTER TABLE public.agdrv_users
    ADD CONSTRAINT agdrv_users_status_code_fk
        FOREIGN KEY (status_code) REFERENCES public.agdrv_dic_users_statuses (code);
