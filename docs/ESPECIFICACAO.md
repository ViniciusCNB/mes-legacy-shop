# Especificação

## Estado do contrato

GET /actuator/health e o catálogo público GET /api/products e GET /api/products/{id} têm implementação nas Fases 1 e 2. O resultado real das verificações está em STATUS.md. Segurança, escrita administrativa e pedidos permanecem como contrato proposto para as próximas fases, sem acordo adicional atribuído ao professor.

## Fundação implementada

API local em 127.0.0.1:8080, configurável por SERVER_PORT. GET /actuator/health é público, retorna 200 com {"status":"UP"} quando os indicadores, inclusive conexão JDBC, estão saudáveis. O Actuator responde 503 quando o estado agregado é DOWN; detalhes e componentes não são expostos. Outros endpoints Actuator não são expostos. Sem Spring Security nesta fase.

PostgreSQL 16, esquema shop, instantes JDBC em UTC, Hibernate ddl-auto=validate e inicialização SQL automática da aplicação desativada. Product está mapeado em shop.products pelo SQL V002. O esquema é validado na inicialização, sem criação/atualização automática de tabelas. Há testes de conexão, catálogo HTTP, persistência e rejeição de esquema divergente; os resultados efetivos constam de STATUS.md. A porta local do banco é 5432 por solicitação do autor.

## Modelo (Product implementado; demais entidades planejadas)

| Entidade | Dados |
| --- | --- |
| AppUser | Long id, nome, email único, hash BCrypt, perfil ADMIN/CUSTOMER, ativo |
| Product | Long id, SKU único, nome, preço, estoque inteiro, ativo |
| PurchaseOrder | Long id, cliente AppUser, CREATED/CANCELLED, criação, cancelamento, total |
| OrderItem | Long id, pedido, produto, quantidade, preço contratado, subtotal |

Valores em BRL com BigDecimal e duas casas; nunca float/double. Instantes em UTC. Representação HTTP proposta: ISO-8601 com Z, por exemplo 2026-10-15T12:00:00Z; confirmar os nomes dos campos antes dos endpoints. Não há cliente separado de AppUser CUSTOMER nem pagamento simulado.

## Regras de negócio a preservar

- Produtos: preço positivo, estoque não negativo, SKU único; catálogo somente de ativos. Só ADMIN cria/edita/define estoque. Inativação preserva histórico; sem exclusão física.
- Pedido pertence ao usuário autenticado; corpo não define proprietário. Pelo menos um item, quantidades inteiras positivas e nenhum produto repetido.
- Servidor calcula preços, subtotais e total. Produtos devem existir, estar ativos e ter estoque. Um item inválido desfaz todos os efeitos da operação.
- Pedido e baixa de estoque são transacionais; duas compras do último item devem produzir somente um sucesso, sem estoque negativo. Definir estratégia de concorrência na Fase 4.
- Preço contratado é preservado após reajustes. CUSTOMER consulta/cancela somente os próprios pedidos; ADMIN pode consultar/cancelar quaisquer pedidos.
- Cancelamento repõe estoque uma vez; repetição retorna o pedido cancelado sem reposição adicional.
- Resumo conta/soma pedidos não cancelados, com início inclusivo e fim exclusivo. Filtros por cliente, situação e período.
- HTTP Basic e BCrypt com usuários do banco. Dois clientes e um administrador locais de demonstração. Sem cadastro público, JWT/OAuth ou provedor externo. Definir e testar CSRF e sessão na Fase 3.

## Rotas propostas por fase

| Método e rota | Acesso | Fase |
| --- | --- | --- |
| GET /actuator/health | Público | 1, entregue |
| GET /api/products | Público, ativos paginados | 2, implementado |
| GET /api/products/{id} | Público, ativo | 2, implementado |
| GET /api/me | Autenticado, sem hash/senha | 3 |
| POST /api/admin/products | ADMIN | 3 |
| PUT /api/admin/products/{id} | ADMIN, nome/preço/ativo | 3 |
| PATCH /api/admin/products/{id}/stock | ADMIN, estoque absoluto | 3 |
| POST /api/orders | CUSTOMER | 4 |
| GET /api/orders | CUSTOMER, próprios paginados | 4 |
| GET /api/orders/{id} | Proprietário ou ADMIN | 4 |
| POST /api/orders/{id}/cancel | Proprietário ou ADMIN | 4 |
| GET /api/admin/orders | ADMIN, filtros/paginação | 4 |
| GET /api/admin/reports/orders | ADMIN, resumo no período | 4 |

Criações: 201. Consultas, atualizações e cancelamentos: 200. Entrada inválida: 400. Recurso ausente ou pedido de outro cliente: 404. Conflitos de negócio (SKU/estoque): 409. Ausência/falha de autenticação: 401; acesso administrativo por cliente: 403. Erros de domínio/validação usarão code, message e fieldErrors quando cabível. Respostas de segurança serão documentadas na Fase 3.

Exemplo proposto de criação de pedido: {"items":[{"productId":1,"quantity":2}]}. Não enviar preço, total ou customerId. Exemplo proposto de erro: {"code":"INSUFFICIENT_STOCK","message":"Estoque insuficiente"}. Os códigos, DTOs e exemplos completos serão fixados antes da avaliação externa. O contrato de paginação do catálogo está definido abaixo. Filtros e paginação de pedidos serão definidos na Fase 4.

## Testes planejados

Catálogo ativo/inativo; administração bloqueada para cliente; valores e baixa de estoque; rollback de pedido com item inválido; isolamento de dois clientes; preço histórico; cancelamento repetido; quantidade/SKU/estoque inválidos; concorrência pelo último item; filtros e resumo com cancelados. Usar assertions de comportamento e PostgreSQL real, sem testes de getters/setters ou mocks como prova do banco.

Na Fase 5, evaluation/ será projeto separado com JUnit 5 e cliente HTTP Java 17, sem dependência interna do Spring. Não haverá endpoints especiais para resetar dados ou ignorar segurança. Congelar contratos e avaliador antes do experimento; medir JaCoCo sem meta arbitrária.

## Fora do escopo

Frontend, pagamento real, frete, cupons, integração externa, mensageria, microsserviços, cadastro público, recuperação de senha, autenticação social, nuvem e IA no produto.

## Contrato do catálogo — Fase 2

Autorização do autor em 15/10/2026: prosseguir para o Catálogo e retornar a porta local do PostgreSQL a 5432. Os detalhes abaixo são decisões de implementação dentro desse escopo, sem atribuição ao professor. A execução dos testes e a revisão do autor são registradas separadamente em STATUS.md.

- GET /api/products: público, somente ativos, ordem fixa id crescente. page começa em 0, padrão 0, máximo 1.000.000; size padrão 20, entre 1 e 100. Parâmetros omitidos ou vazios usam os padrões. O teto de page mantém o offset dentro do intervalo aceito pelo JPA. Ordenação configurável não faz parte deste contrato.
- Resposta 200: {"content":[{"id":1,"sku":"MUG-001","name":"Caneca MES","price":39.90,"stockQuantity":10}],"page":0,"size":20,"totalElements":1,"totalPages":1}. Página além do resultado retorna content vazio com totais preservados; catálogo vazio tem totalPages=0.
- GET /api/products/{id}: ID Long positivo; retorna os mesmos cinco campos de produto. Ativo sem estoque continua visível. Produto ausente ou inativo retorna 404 com {"code":"PRODUCT_NOT_FOUND","message":"Produto não encontrado"}.
- Parâmetro fora dos limites, não inteiro ou fora do intervalo do tipo retorna 400 com code INVALID_REQUEST, message "Parâmetros inválidos" e fieldErrors, por exemplo [{"field":"size","message":"deve estar entre 1 e 100"}]. Mensagens específicas constam das respostas; não há stack trace público.
- Product: id Long gerado por identidade, sku não branco com até 64 caracteres e unicidade sensível a maiúsculas/minúsculas; name não branco até 160; price BigDecimal positivo com no máximo 17 dígitos inteiros e 2 fracionários; stockQuantity inteiro não negativo; active boolean obrigatório. Banco: shop.products, numeric(19,2), checks de preço/estoque/texto e unique de SKU. Não há exclusão física ou rota de escrita nesta fase.
- Dados locais são uma carga explícita e repetível por SKU, sem sobrescrever alterações de produtos existentes. Fixtures do teste são independentes dessa carga. V001 de esquema permanece intacto; V002 cria products.
- Aceitação: ativos/inativos, totais/paginação, detalhe/404, parâmetros inválidos, persistência decimal exata, validações/restrições reais e falha de inicialização por divergência de esquema. Sem segurança, administração ou pedidos.
