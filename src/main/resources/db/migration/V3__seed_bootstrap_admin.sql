CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO public.agdrv_users (
    login,
    password_hash,
    role_code,
    status_code,
    display_name
)
SELECT
    'admin',
    crypt('admin', gen_salt('bf')),
    'ROLE_ADMIN',
    'ACTIVE',
    'Administrator'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.agdrv_users
    WHERE login = 'admin'
);
