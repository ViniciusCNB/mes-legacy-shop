-- Exclusivo do banco novo criado pelos scripts evaluation/run.*.
-- Senha local de todos os usuarios: test-password; BCrypt custo 10.
-- Nao repetivel: falha se o banco nao estiver vazio, sem apagar dados.
BEGIN;
INSERT INTO shop.app_users (id, name, email, password_hash, role, active) VALUES
    (1, 'Evaluation admin', 'admin@evaluation.local', '$2a$10$EAUCToxT4i1ExFRQjER23.8yIlaePM/8Qt3D8zAKNT.o3jGcc5KY.', 'ADMIN', true),
    (2, 'Evaluation Alice', 'alice@evaluation.local', '$2a$10$EAUCToxT4i1ExFRQjER23.8yIlaePM/8Qt3D8zAKNT.o3jGcc5KY.', 'CUSTOMER', true),
    (3, 'Evaluation Bob', 'bob@evaluation.local', '$2a$10$EAUCToxT4i1ExFRQjER23.8yIlaePM/8Qt3D8zAKNT.o3jGcc5KY.', 'CUSTOMER', true),
    (4, 'Evaluation disabled', 'disabled@evaluation.local', '$2a$10$EAUCToxT4i1ExFRQjER23.8yIlaePM/8Qt3D8zAKNT.o3jGcc5KY.', 'ADMIN', false);
ALTER TABLE shop.app_users ALTER COLUMN id RESTART WITH 100;
INSERT INTO shop.products (id, sku, name, price, stock_quantity, active) VALUES
    (10, 'FIXTURE-ACTIVE', 'Produto inicial ativo', 19.90, 25, true),
    (20, 'FIXTURE-INACTIVE', 'Produto inicial inativo', 29.90, 8, false),
    (30, 'FIXTURE-ZERO', 'Produto inicial sem estoque', 5.00, 0, true);
ALTER TABLE shop.products ALTER COLUMN id RESTART WITH 100;
COMMIT;
