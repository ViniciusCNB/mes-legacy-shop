-- Exclusivo do PostgreSQL descartavel do teste; nao usar na base local.
TRUNCATE TABLE shop.order_items, shop.purchase_orders, shop.products RESTART IDENTITY;
INSERT INTO shop.products (id, sku, name, price, stock_quantity, active) VALUES
    (30, 'CATALOG-30', 'Terceiro produto', 39.90, 5, true),
    (20, 'HIDDEN-20', 'Produto inativo', 24.50, 9, false),
    (10, 'CATALOG-10', 'Primeiro produto', 19.90, 12, true),
    (40, 'CATALOG-40', 'Produto sem estoque', 5.00, 0, true);
ALTER TABLE shop.products ALTER COLUMN id RESTART WITH 100;
