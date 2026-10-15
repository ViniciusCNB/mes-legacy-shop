-- Exclusivo dos conteineres descartaveis. Senha de teste: test-password.
TRUNCATE TABLE shop.app_users RESTART IDENTITY;
INSERT INTO shop.app_users (id, name, email, password_hash, role, active) VALUES
    (1, 'Admin de teste', 'admin@test.local', '$2a$10$EAUCToxT4i1ExFRQjER23.8yIlaePM/8Qt3D8zAKNT.o3jGcc5KY.', 'ADMIN', true),
    (2, 'Alice', 'alice@test.local', '$2a$10$EAUCToxT4i1ExFRQjER23.8yIlaePM/8Qt3D8zAKNT.o3jGcc5KY.', 'CUSTOMER', true),
    (3, 'Bob', 'bob@test.local', '$2a$10$EAUCToxT4i1ExFRQjER23.8yIlaePM/8Qt3D8zAKNT.o3jGcc5KY.', 'CUSTOMER', true),
    (4, 'Inativo', 'disabled@test.local', '$2a$10$EAUCToxT4i1ExFRQjER23.8yIlaePM/8Qt3D8zAKNT.o3jGcc5KY.', 'ADMIN', false);
ALTER TABLE shop.app_users ALTER COLUMN id RESTART WITH 100;
