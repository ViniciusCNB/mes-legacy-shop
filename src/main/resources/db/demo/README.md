# Dados locais de demonstração

V001__products.sql cria três produtos ativos (incluindo um com estoque zero) e um
inativo. A carga é explícita e usa ON CONFLICT (sku) DO NOTHING: repetir não
sobrescreve produtos existentes. IDs não são fixados e sequências podem ter lacunas.

Execute depois de aplicar os scripts de esquema V001 e V002. Os comandos Windows e
Linux estão no README da raiz. Esta pasta não é montada na inicialização do banco.

A fixture src/test/resources/db/catalog-fixture.sql é independente desta carga e
apaga dados antes de cada teste em PostgreSQL descartável. Nunca a aplique na base local.

V002__users.sql cria um administrador e dois clientes ativos com hashes BCrypt.
As senhas locais estão no README da raiz. A carga é explícita depois de V003 do
esquema; conflitos de email preservam contas existentes. Nunca substitua essa
carga pela fixture security-fixture.sql, que apaga usuários no banco de teste.
