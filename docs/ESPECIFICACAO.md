# Especificação

## Estado do contrato

Somente GET /actuator/health está implementado na Fase 1. O restante é contrato proposto na referência do autor, a ser detalhado e validado nas fases correspondentes. Não constitui funcionalidade entregue nem acordo adicional com o professor.

## Fundação implementada

API local em 127.0.0.1:8080, configurável por SERVER_PORT. GET /actuator/health é público, retorna 200 com {"status":"UP"} quando os indicadores, inclusive conexão JDBC, estão saudáveis. O Actuator responde 503 quando o estado agregado é DOWN; detalhes e componentes não são expostos. Outros endpoints Actuator não são expostos. Sem Spring Security nesta fase.

PostgreSQL 16, esquema shop, instantes JDBC em UTC, Hibernate ddl-auto=validate e inicialização SQL automática da aplicação desativada. Ainda não existem entidades: validate está configurado, mas a validação de tabelas mapeadas só poderá ser demonstrada na Fase 2. O teste atual verifica conexão real, versão, existência do esquema e resposta HTTP.

## Modelo proposto

| Entidade | Dados |
| --- | --- |
| AppUser | Long id, nome, email único, hash BCrypt, perfil ADMIN/CUSTOMER, ativo |
| Product | Long id, SKU único, nome, preço, estoque inteiro, ativo |
| PurchaseOrder | Long id, cliente AppUser, CREATED/CANCELLED, criação, cancelamento, total |
| OrderItem | Long id, pedido, produto, quantidade, preço contratado, subtotal |

Valores em BRL com BigDecimal e duas casas; nunca float/double. Instantes em UTC. Representação HTTP proposta: ISO-8601 com Z, por exemplo 2026-10-15T12:00:00Z; confirmar os nomes dos campos antes dos endpoints. Não há cliente separado de AppUser CUSTOMER nem pagamento simulado.

## Regras futuras verificáveis

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
| GET /api/products | Público, ativos paginados | 2 |
| GET /api/products/{id} | Público, ativo | 2 |
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

Exemplo proposto de criação de pedido: {"items":[{"productId":1,"quantity":2}]}. Não enviar preço, total ou customerId. Exemplo proposto de erro: {"code":"INSUFFICIENT_STOCK","message":"Estoque insuficiente"}. Os códigos, DTOs e exemplos completos serão fixados antes da avaliação externa. Limites de paginação, nomes dos filtros e ordenação estável serão definidos na implementação de cada consulta; não há comportamento de paginação entregue agora.

## Testes planejados

Catálogo ativo/inativo; administração bloqueada para cliente; valores e baixa de estoque; rollback de pedido com item inválido; isolamento de dois clientes; preço histórico; cancelamento repetido; quantidade/SKU/estoque inválidos; concorrência pelo último item; filtros e resumo com cancelados. Usar assertions de comportamento e PostgreSQL real, sem testes de getters/setters ou mocks como prova do banco.

Na Fase 5, evaluation/ será projeto separado com JUnit 5 e cliente HTTP Java 17, sem dependência interna do Spring. Não haverá endpoints especiais para resetar dados ou ignorar segurança. Congelar contratos e avaliador antes do experimento; medir JaCoCo sem meta arbitrária.

## Fora do escopo

Frontend, pagamento real, frete, cupons, integração externa, mensageria, microsserviços, cadastro público, recuperação de senha, autenticação social, nuvem e IA no produto.
