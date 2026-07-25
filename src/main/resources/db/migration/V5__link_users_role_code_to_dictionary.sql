ALTER TABLE public.agdrv_users
    DROP CONSTRAINT IF EXISTS agdrv_users_role_code_ck;

ALTER TABLE public.agdrv_users
    ADD CONSTRAINT agdrv_users_role_code_fk
        FOREIGN KEY (role_code) REFERENCES public.agdrv_dic_users_roles (code);
