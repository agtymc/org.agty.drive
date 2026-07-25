CREATE TABLE IF NOT EXISTS public.agdrv_sessions (
    primary_id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    creation_time BIGINT NOT NULL,
    last_access_time BIGINT NOT NULL,
    max_inactive_interval INT NOT NULL,
    expiry_time BIGINT NOT NULL,
    principal_name VARCHAR(100),
    CONSTRAINT agdrv_sessions_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS agdrv_sessions_ix1
    ON public.agdrv_sessions (session_id);

CREATE INDEX IF NOT EXISTS agdrv_sessions_ix2
    ON public.agdrv_sessions (expiry_time);

CREATE INDEX IF NOT EXISTS agdrv_sessions_ix3
    ON public.agdrv_sessions (principal_name);

CREATE TABLE IF NOT EXISTS public.agdrv_sessions_attributes (
    session_primary_id CHAR(36) NOT NULL,
    attribute_name VARCHAR(200) NOT NULL,
    attribute_bytes BYTEA NOT NULL,
    CONSTRAINT agdrv_sessions_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT agdrv_sessions_attributes_fk
        FOREIGN KEY (session_primary_id) REFERENCES public.agdrv_sessions (primary_id) ON DELETE CASCADE
);
