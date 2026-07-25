CREATE TABLE IF NOT EXISTS public.agdrv_dic_users_roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    align INTEGER NOT NULL DEFAULT 0,
    disabled BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT agdrv_dic_users_roles_code_uq UNIQUE (code)
);

INSERT INTO public.agdrv_dic_users_roles (code, title, align, disabled)
SELECT 'ROLE_ADMIN', 'Администратор', 10, FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM public.agdrv_dic_users_roles
    WHERE code = 'ROLE_ADMIN'
);

INSERT INTO public.agdrv_dic_users_roles (code, title, align, disabled)
SELECT 'ROLE_USER', 'Пользователь', 20, FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM public.agdrv_dic_users_roles
    WHERE code = 'ROLE_USER'
);
