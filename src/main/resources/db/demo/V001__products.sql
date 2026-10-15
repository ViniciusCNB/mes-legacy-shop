-- Carga local explicita. Preserva produtos existentes, inclusive suas alteracoes.
INSERT INTO shop.products (sku, name, price, stock_quantity, active) VALUES
    ('MUG-001', 'Caneca MES', 39.90, 10, true),
    ('NOTEBOOK-001', 'Caderno UFMG', 24.50, 20, true),
    ('PEN-001', 'Caneta', 5.00, 0, true),
    ('MUG-OLD', 'Caneca fora de linha', 29.90, 5, false)
ON CONFLICT (sku) DO NOTHING;
