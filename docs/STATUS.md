# Status

Atualização: 15/10/2026, America/Sao_Paulo. **Fase 4 — Pedidos implementada e verificada.** A Fase 3 foi commitada pelo autor em 6778cd2; a pasta estava limpa ao iniciar. Nenhum commit, staging ou tag foi feito pelo Codex. A Fase 5 não foi iniciada.

## Entregue

- Stack preservada: Spring Boot 2.7.18, Java 17, Maven Wrapper 3.9.11, PostgreSQL 16.10 no mesmo digest, porta local 5432. Nenhuma dependência ou versão nova; POM inalterado.
- SQL transacional V004 cria purchase_orders/order_items, identidades, FKs, índices, produto único por pedido e checks de quantidade/valores/estado. V001–V003 preservados; Hibernate continua validate.
- POST /api/orders cria pedido do CUSTOMER autenticado, sem aceitar proprietário/preço/total do cliente. Entrada estrita, 1–100 produtos distintos, quantidade inteira positiva. Servidor calcula unitPrice/subtotal/total exatos e baixa estoque na mesma transação.
- Locks PESSIMISTIC_WRITE de produtos por ID crescente, compatíveis com a administração. Produto inexistente/inativo/sem estoque ou qualquer item inválido impede todos os efeitos; rollback multitem verificado.
- Preço contratado preservado mesmo após reajuste/inativação. BigDecimal, escala 2, preço numeric(19,2), totais numeric(38,2); quantidade/preço máximos testados sem arredondamento ou overflow monetário.
- GET /api/orders lista somente pedidos próprios, paginados. GET /api/orders/{id} permite proprietário ou ADMIN. Outro cliente recebe o mesmo 404 do ausente; respostas DTO sem dados pessoais, senha ou relações JPA.
- POST /api/orders/{id}/cancel permite proprietário ou ADMIN; bloqueia o pedido e depois produtos na mesma ordem. Cancelamento repetido/simultâneo restitui uma vez e preserva cancelledAt. Guarda de overflow de estoque causa 409/rollback integral, permitindo correção administrativa e nova tentativa.
- GET /api/admin/orders filtra customerId/status/start/end; GET /api/admin/reports/orders conta/soma somente não cancelados. Período em createdAt, início inclusivo/fim exclusivo, filtros combinados e agregação no banco. Paginação por id decrescente; datas UTC com precisão de microssegundos.
- HTTP Basic/BCrypt, perfis, CSRF e política sem sessão preservados. ADMIN não compra/lista como cliente. Testes usam usuários/token/cookie reais, sem mocks ou bypass.
- Documentação, contrato, decisões e receitas PowerShell/POSIX atualizados. Sem frontend, avaliador externo, baseline/tag ou experimento Ark.

## Comandos e resultados reais

JDK: Microsoft OpenJDK 17.0.15 em C:\Users\vinic\.jdks\ms-17.0.15; Maven 3.9.11. Windows 11 amd64, Docker Engine 27.5.1/API 1.47, Compose 2.32.4. PostgreSQL 16.10 em contêiner Linux, imagem postgres:16.10-bookworm@sha256:38471f330eb885e04de130b768d6db4e10469e2311879c7e5c699f6d2d8a1c74.

| Comando/verificação | Resultado |
| --- | --- |
| git status --short / git log -3 --oneline na inspeção inicial | Pasta limpa, HEAD 6778cd2; fases anteriores commitadas pelo autor. |
| docker compose config --quiet / docker compose ps | Configuração válida; serviço saudável em 127.0.0.1:5432. |
| SELECT to_regclass('shop.purchase_orders') | Tabela ausente antes de aplicar V004. |
| Get-Content src/main/resources/db/schema/V004__orders.sql -Raw \| docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' | BEGIN, duas tabelas/três índices, COMMIT; 6 produtos/3 usuários preservados, 0 pedidos inicialmente. |
| .\mvnw.cmd -B -ntp clean verify — primeira tentativa | BUILD FAILURE, 123 testes, 5 falhas, 0 erros/skips; diagnóstico e correções abaixo. |
| .\mvnw.cmd -B -ntp clean verify — após correções | **BUILD SUCCESS**, 123 testes, 0 falhas, 0 erros, 0 skips. Total 1:49, encerrado em 15/10/2026 19:23:50 -03. |
| java -jar target/mes-legacy-shop-0.0.1-SNAPSHOT.jar com o JDK 17 indicado | Inicialização local em 127.0.0.1:8080, conexão real e validate; GET /actuator/health 200 {"status":"UP"}. |
| Fluxo HTTP PowerShell com Basic/CSRF/cookie reais | Pedido 1: POST 201, 2 unidades de MUG-001 a 39.90, total 79.80; estoque 10→8. Releitura mantém instante; cliente 2 recebe 404. Resumo antes: 1/79.80; após cancelamento: 0/0.00. |
| Cancelamento repetido PowerShell | Ambas as chamadas 200, CANCELLED e mesmo cancelledAt; estoque 8→10, sem reposição adicional. |
| Receita POSIX do README executada por Git Bash | Pedido 2: 1 unidade/39.90, consulta e cancelamento com curl/awk/sed e cookie jar; estoque restituído. Sem dependência de jq ou bypass de CSRF. |
| Listas/filtros/resumo locais por HTTP | Cliente lista 2 pedidos; ADMIN filtra 2 CANCELLED no período e 0 CREATED. Resumo do período 0/0.00, excluindo cancelados. |
| Conferência SQL final | Pedidos 1 e 2 CANCELLED (79.80/39.90), produtos/usuários preservados. MUG-001 estoque 10; NOTEBOOK-001 estoque 20. |
| Encerramento do JAR | Somente PID 94596, iniciado nesta tarefa e conferido pelo comando do processo, encerrado; banco Compose mantido saudável. |
| git diff --check | Sem problemas de whitespace. |

As receitas completas estão no README. O Maven foi executado com JAVA_HOME definido para o JDK acima. Logs completos locais desta fase, fora do versionamento:
- C:\Users\vinic\AppData\Local\Temp\mes-orders-verify.log (primeira tentativa);
- C:\Users\vinic\AppData\Local\Temp\mes-orders-accepted-verify.log (build aprovado);
- C:\Users\vinic\AppData\Local\Temp\mes-orders-local-app.log (JAR local).

## Testes e cobertura

| Suíte | Execuções | Resultado final |
| --- | ---: | --- |
| FoundationIT | 1 | Passou: saúde, conexão/esquema PostgreSQL e ausência de exposição administrativa. |
| CatalogIT | 32 | Passou: catálogo/paginação, validações, dinheiro, constraints e rejeição de coluna ausente. |
| SecurityAdminIT | 41 | Passou: usuários/BCrypt/Basic, perfis, CSRF real, administração e estoque. |
| OrdersIT | 49 | Passou: valores/estoques, rollback, histórico, isolamento, perfis/CSRF, cancelamento/overflow, filtros/resumo, limites, constraints e concorrência. |
| Total | **123** | **0 falhas, 0 erros, 0 skips**. |

Cada classe cria banco PostgreSQL novo com V001–V004, sem Compose local, H2 ou mocks. Fixtures limpam explicitamente tabelas relacionadas apenas nos contêineres descartáveis; probe de esquema inclui as novas tabelas, mantendo a asserção específica de coluna ausente.

As duas chamadas HTTP de cada teste concorrente são iniciadas por barreira. Uma conexão independente mantém a linha bloqueada; o teste exige que pg_stat_activity comprove duas transações aguardando locks antes de liberá-la. Última unidade: exatamente um 201 e um 409 INSUFFICIENT_STOCK, um pedido/item persistido e estoque zero. Cancelamentos simultâneos por proprietário/ADMIN: dois 200 com a mesma resposta e estoque restituído uma vez. Não há retry, serialização artificial das chamadas ou relaxamento das assertions.

JaCoCo observado, sem meta arbitrária: **435/439 linhas (99,09%)**, **1936/1954 instruções (99,08%)**, **48/50 ramos (96,00%)**. Relatórios em target/site/jacoco/index.html e jacoco.csv; resultados detalhados em target/failsafe-reports. São relatórios locais, não arquivos para commit.

## Falhas encontradas e correções

A primeira execução teve cinco falhas reais, preservadas no log:
- Quatro comparações exatas de resposta detectaram nanosegundos no Instant recém-criado/cancelado e arredondamento a microssegundos na releitura do PostgreSQL. A aplicação agora trunca os instantes a microssegundos antes de persistir; as mesmas assertions de igualdade passaram, inclusive no cancelamento simultâneo.
- Uma comparação de dinheiro no limite revelou perda de precisão no leitor JsonNode padrão do teste, que interpretava o número como double. O leitor de resposta do teste passou a USE_BIG_DECIMAL_FOR_FLOATS, preservando o valor completo recebido; assertivas exatas de HTTP e banco passaram. Produção calcula/serializa BigDecimal.
- O Failsafe 2.22.2 gerenciado pelo parent registrou diagnóstico não fatal de inspeção do processo pai no Windows (PpidChecker/NOOP). A JVM continuou, as quatro suítes foram executadas e os resultados acima foram produzidos. Nenhum teste/filtro foi desativado.

Falhas de constraints e de validação de esquema provocadas pelos testes negativos aparecem nos logs intencionalmente. As falhas históricas de ambiente da Fase 3 permanecem no histórico do STATUS do commit 6778cd2; não se repetiram como falha nesta verificação.

## Estado local, limites e próximo passo

- JAR de verificação encerrado; PostgreSQL mantido em 5432 com volume preservado. Dois pedidos de demonstração CANCELLED permanecem como histórico; não foram apagados nem alterados diretamente para mascarar resultados. Nenhum estoque ficou consumido pelos roteiros.
- Build e aplicação em Linux nativo continuam não verificados. A receita POSIX passou em Git Bash no Windows; isso não comprova execução nativa do JDK/Boot em Linux. Os próprios bancos Testcontainers são PostgreSQL Linux reais.
- Revisão humana do código/contrato e commit permanecem com o autor. Não há baseline experimental completa ou execução Ark.
- **Próximo passo, somente mediante nova solicitação: Fase 5 — Consolidação.** Criar evaluation/ independente com JUnit 5/HTTP Java 17, consolidar roteiro, contratos/dados e métricas, reproduzir banco novo e preparar candidato a baseline para revisão. Essa fase não foi iniciada.

## Commit sugerido para o autor

Um commit atômico: **feat: implementa pedidos com estoque transacional e cancelamento**.

Arquivos desta alteração (30):

~~~text
AGENTS.md
docs/CONTEXTO_TP.md
docs/DECISOES.md
docs/ESPECIFICACAO.md
docs/PLANO.md
docs/STATUS.md
README.md
src/main/java/br/ufmg/mes/shop/api/ApiExceptionHandler.java
src/main/java/br/ufmg/mes/shop/order/CreateOrderRequest.java
src/main/java/br/ufmg/mes/shop/order/InvalidOrderRequestException.java
src/main/java/br/ufmg/mes/shop/order/OrderAdminController.java
src/main/java/br/ufmg/mes/shop/order/OrderConflictException.java
src/main/java/br/ufmg/mes/shop/order/OrderController.java
src/main/java/br/ufmg/mes/shop/order/OrderFilter.java
src/main/java/br/ufmg/mes/shop/order/OrderItem.java
src/main/java/br/ufmg/mes/shop/order/OrderNotFoundException.java
src/main/java/br/ufmg/mes/shop/order/OrderPageResponse.java
src/main/java/br/ufmg/mes/shop/order/OrderRepository.java
src/main/java/br/ufmg/mes/shop/order/OrderResponse.java
src/main/java/br/ufmg/mes/shop/order/OrderService.java
src/main/java/br/ufmg/mes/shop/order/OrderStatus.java
src/main/java/br/ufmg/mes/shop/order/OrderSummaryResponse.java
src/main/java/br/ufmg/mes/shop/order/PurchaseOrder.java
src/main/java/br/ufmg/mes/shop/security/SecurityConfiguration.java
src/main/resources/db/schema/V004__orders.sql
src/test/java/br/ufmg/mes/shop/order/OrdersIT.java
src/test/java/br/ufmg/mes/shop/product/CatalogIT.java
src/test/java/br/ufmg/mes/shop/support/PostgresTestDatabase.java
src/test/resources/db/catalog-fixture.sql
src/test/resources/db/security-fixture.sql
~~~

Não incluir .env, target, relatórios, logs, caches ou temporários. Nenhum staging/commit foi feito pelo Codex.
