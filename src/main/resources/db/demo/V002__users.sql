-- Credenciais locais de demonstracao no README; nunca usar estas contas em producao.
-- Repetir a carga preserva contas, hashes, perfis e situacao ja existentes.
INSERT INTO shop.app_users (name, email, password_hash, role, active) VALUES
    ('Administrador local', 'admin@local.test', '$2a$10$vWsCNtapaFRhtS4TfFDv1OxjN5UJRaYJOi.m.A2q3Gx6dC4gOJ1L.', 'ADMIN', true),
    ('Cliente um', 'cliente1@local.test', '$2a$10$dsDAFxinvNgTGOU9YVOokuHS46BEvW9y5eXY5akfoPzwa8yXPODEi', 'CUSTOMER', true),
    ('Cliente dois', 'cliente2@local.test', '$2a$10$wtUEGu1bQPYJRMarhSvHg.daMnAdLBaddO./HHKQUYvOK1ggGxcRu', 'CUSTOMER', true)
ON CONFLICT (email) DO NOTHING;
