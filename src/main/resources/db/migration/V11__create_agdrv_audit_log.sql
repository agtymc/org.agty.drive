CREATE TABLE IF NOT EXISTS public.agdrv_audit_log (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_user_id BIGINT,
    action_code VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32),
    resource_id BIGINT,
    details TEXT,
    CONSTRAINT agdrv_audit_log_actor_fk
        FOREIGN KEY (actor_user_id) REFERENCES public.agdrv_users (id)
);

CREATE INDEX IF NOT EXISTS agdrv_audit_log_created_at_idx
    ON public.agdrv_audit_log (created_at DESC);

CREATE INDEX IF NOT EXISTS agdrv_audit_log_actor_idx
    ON public.agdrv_audit_log (actor_user_id);

CREATE INDEX IF NOT EXISTS agdrv_audit_log_resource_idx
    ON public.agdrv_audit_log (resource_type, resource_id);
