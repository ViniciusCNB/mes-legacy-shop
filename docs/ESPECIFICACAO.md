# Especificação

## Estado do contrato

Saúde, catálogo público, usuários/segurança e administração de produtos têm implementação nas Fases 1 a 3. O resultado real das verificações está em STATUS.md. Pedidos permanecem como contrato proposto para a Fase 4, sem acordo adicional atribuído ao professor.

## Fundação implementada

API local em 127.0.0.1:8080, configurável por SERVER_PORT. GET /actuator/health é público, retorna 200 com {"status":"UP"} quando os indicadores, inclusive conexão JDBC, estão saudáveis. O Actuator responde 503 quando o estado agregado é DOWN; detalhes e componentes não são expostos. Outros endpoints Actuator não são expostos. Spring Security controla autenticação, autorização e CSRF conforme o contrato da Fase 3 abaixo.

PostgreSQL 16, esquema shop, instantes JDBC em UTC, Hibernate ddl-auto=validate e inicialização SQL automática da aplicação desativada. Product está mapeado em shop.products pelo SQL V002. O esquema é validado na inicialização, sem criação/atualização automática de tabelas. Há testes de conexão, catálogo HTTP, persistência e rejeição de esquema divergente; os resultados efetivos constam de STATUS.md. A porta local do banco é 5432 por solicitação do autor.

## Modelo (Product e AppUser implementados; pedidos planejados)

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
- HTTP Basic e BCrypt com usuários do banco. Dois clientes e um administrador locais de demonstração. Sem cadastro público, JWT/OAuth ou provedor externo. Política de CSRF e sessão definida na Fase 3 abaixo.

## Rotas propostas por fase

| Método e rota | Acesso | Fase |
| --- | --- | --- |
| GET /actuator/health | Público | 1, entregue |
| GET /api/products | Público, ativos paginados | 2, implementado |
| GET /api/products/{id} | Público, ativo | 2, implementado |
| GET /api/me | Autenticado, sem hash/senha | 3, implementado |
| GET /api/csrf | Autenticado, cookie e token para escritas | 3, implementado |
| POST /api/admin/products | ADMIN | 3, implementado |
| PUT /api/admin/products/{id} | ADMIN, nome/preço/ativo | 3, implementado |
| PATCH /api/admin/products/{id}/stock | ADMIN, estoque absoluto | 3, implementado |
| POST /api/orders | CUSTOMER | 4 |
| GET /api/orders | CUSTOMER, próprios paginados | 4 |
| GET /api/orders/{id} | Proprietário ou ADMIN | 4 |
| POST /api/orders/{id}/cancel | Proprietário ou ADMIN | 4 |
| GET /api/admin/orders | ADMIN, filtros/paginação | 4 |
| GET /api/admin/reports/orders | ADMIN, resumo no período | 4 |

Criações: 201. Consultas, atualizações e cancelamentos: 200. Entrada inválida: 400. Recurso ausente ou pedido de outro cliente: 404. Conflitos de negócio (SKU/estoque): 409. Ausência/falha de autenticação: 401; acesso administrativo por cliente: 403. Erros de domínio/validação usarão code, message e fieldErrors quando cabível. Respostas de segurança e precedência de CSRF estão definidas no contrato da Fase 3 abaixo.

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
- Product: id Long gerado por identidade, sku não branco com até 64 caracteres e unicidade sensível a maiúsculas/minúsculas; name não branco até 160; price BigDecimal positivo com no máximo 17 dígitos inteiros e 2 fracionários; stockQuantity inteiro não negativo; active boolean obrigatório. Banco: shop.products, numeric(19,2), checks de preço/estoque/texto e unique de SKU. Não há exclusão física; as rotas de escrita foram acrescentadas na Fase 3.
- Dados locais são uma carga explícita e repetível por SKU, sem sobrescrever alterações de produtos existentes. Fixtures do teste são independentes dessa carga. V001 de esquema permanece intacto; V002 cria products.
- Aceitação: ativos/inativos, totais/paginação, detalhe/404, parâmetros inválidos, persistência decimal exata, validações/restrições reais e falha de inicialização por divergência de esquema. Esse foi o escopo da Fase 2; segurança/administração foram acrescentadas na Fase 3 e pedidos permanecem planejados.

## Contrato da Fase 3 — Segurança e administração

Autorizado pelo autor após o commit f452ab5. As escolhas rotineiras abaixo são de implementação, sem atribuição de novas decisões ao professor. Pedidos continuam fora desta fase.

- AppUser: id Long/IDENTITY, name até 160, email canônico minúsculo até 254 e único, password_hash BCrypt de 60 caracteres, role ADMIN/CUSTOMER e active obrigatório. SQL V003; sem cadastro público ou API de usuários.
- HTTP Basic consulta usuários e situação ativa no banco em cada requisição; BCrypt custo 10. Não há sessão de autenticação, login por formulário, JWT ou logout de sessão. O cookie CSRF não autentica e não há JSESSIONID.
- GET /api/me exige autenticação e retorna somente {"id":1,"name":"Administrador local","email":"admin@local.test","role":"ADMIN"}. Não expor senha, hash ou entidade JPA.
- GET /api/csrf é uma rota funcional autenticada para obter {"headerName":"X-XSRF-TOKEN","token":"..."}, com Cache-Control no-store. Antes de cada escrita, obter/renovar o token: a autenticação Basic pode rotacionar o cookie CSRF. Preservar as atualizações do cookie XSRF-TOKEN (HttpOnly, Path=/) e enviar seu token no cabeçalho informado, junto com Basic, em POST/PUT/PATCH/DELETE. CSRF permanece habilitado; não é endpoint especial de teste.
- Falha/ausência de autenticação: 401 AUTHENTICATION_REQUIRED com WWW-Authenticate Basic. Cliente em operação administrativa: 403 FORBIDDEN. CSRF ausente/inválido: 403 CSRF_INVALID; esse filtro precede autenticação e pode rejeitar escrita antes da verificação de credenciais. Uma escrita com CSRF válido e sem autenticação retorna 401. Credenciais inválidas também são rejeitadas em rotas públicas se enviadas.
- Só GET de saúde/catálogo é público. Rotas de usuário/CSRF exigem autenticação; /api/admin/** exige ADMIN; demais acessos são negados por padrão (401 anônimo/403 autenticado), inclusive /actuator/env. A asserção anterior de 404 desse endpoint passa a verificar 401 sem exposição, por essa política explícita. /error permite apenas o tratamento de erros padrão.
- POST /api/admin/products: corpo obrigatório {"sku":"BOOK-001","name":"Livro","price":59.90,"stockQuantity":10,"active":true}; retorna 201 com os cinco campos públicos e active. SKU duplicado retorna 409 SKU_ALREADY_EXISTS, inclusive pela restrição do banco.
- PUT /api/admin/products/{id}: corpo completo {"name":"Livro revisado","price":69.90,"active":false}; retorna 200 com resposta administrativa. SKU e estoque não podem ser alterados por essa rota; inativar preserva a linha e oculta no catálogo.
- PATCH /api/admin/products/{id}/stock: {"stockQuantity":5}; define quantidade absoluta não negativa, retorna 200 com resposta administrativa. Não exclui produto. IDs positivos; recurso ausente retorna 404 PRODUCT_NOT_FOUND, inclusive na administração.
- Nome/SKU não brancos, limites da Fase 2, dinheiro com até 17 dígitos inteiros/2 fracionários, estoque inteiro e flags booleanas. Campos obrigatórios ausentes/nulos, JSON inválido, propriedades desconhecidas ou coerções de tipo retornam 400 INVALID_REQUEST. Valores de validação têm fieldErrors; erros de JSON não incluem detalhes internos.
- Escritas administrativas são transacionais. Atualizações bloqueiam a linha do produto para não sobrescrever estoque/detalhes lidos por outra atualização administrativa. O controle de concorrência de pedidos será definido e testado na Fase 4.
- Carga local V002__users.sql: um ADMIN e dois CUSTOMER, senhas exclusivamente de demonstração documentadas no README, somente hashes no SQL. Carga explícita e repetível por email, sem sobrescrever contas existentes. Testes usam dados e senha próprios, sem mocks de autenticação ou bypass de CSRF.
