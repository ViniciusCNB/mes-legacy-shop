# Status

Atualização: 15/10/2026, America/Sao_Paulo. **Fase 2 — Catálogo implementada e verificada.** A Fase 1 foi commitada pelo autor em `502c36c`. Nenhum commit, staging ou tag foi feito pelo Codex nesta continuação. A Fase 3 não foi iniciada.

## Entrega atual

- Mantidos Boot 2.7.18, Java 17, Maven Wrapper 3.9.11, PostgreSQL 16.10 e todas as versões/dependências da Fundação. POM e SQL V001 de esquema não foram alterados.
- Product em shop.products, com Long/IDENTITY, SKU único, nome, BigDecimal/numeric(19,2), estoque não negativo e situação ativa. Bean Validation javax e restrições PostgreSQL para as regras aplicáveis.
- V002__products.sql aplicado explicitamente ao volume preservado da Fundação. Banco novo aplica V001 e V002 pelo entrypoint; os testes verificam essa inicialização em contêineres novos. Não houve exclusão de volume ou troca de banco.
- GET /api/products público, somente ativos, paginação e totais, ordem fixa id crescente; GET /api/products/{id} público, com 404 idêntico para inativo e ausente. Produto ativo sem estoque permanece visível.
- DTOs de produto/página, serviço de leitura transacional e erros code/message/fieldErrors. Parâmetros inválidos retornam 400. Não há rotas administrativas, de escrita ou de pedidos.
- Carga local explícita: três produtos ativos, incluindo um sem estoque, e um inativo. Repetição por SKU não duplica nem sobrescreve produtos existentes. Fixture do teste é separada.
- Porta local **5432** em Compose, .env.example, fallback JDBC e .env local, conforme solicitação do autor. Final de linha LF explícito para .env.example e .gitattributes.
- Documentação e instruções atualizadas; sem frontend, segurança antecipada, integrações externas ou migração Ark.

## Verificações efetivamente executadas

Ambiente: Windows 11 amd64; Microsoft OpenJDK 17.0.15 selecionado em JAVA_HOME; Maven 3.9.11; Docker Engine 27.5.1/API 1.47, daemon Linux amd64; Compose 2.32.4. Imagem PostgreSQL preservada por digest: sha256:38471f330eb885e04de130b768d6db4e10469e2311879c7e5c699f6d2d8a1c74.

| Comando/verificação | Resultado real |
| --- | --- |
| Inspeção de AGENTS, README, docs, código e Git | Árvore inicialmente limpa; commit 502c36c contém a Fundação; escopo limitado à Fase 2 |
| .\mvnw.cmd -B -ntp test-compile com JDK 17 | BUILD SUCCESS; produção e testes compilaram |
| Primeiro .\mvnw.cmd -B -ntp clean verify | BUILD FAILURE: daemon Docker indisponível, 2 erros de inicialização das classes, 0 ignorados; nenhum teste foi desabilitado |
| docker version / _ping | HTTP 500 antes do reinício do Docker Desktop pelo autor; depois daemon respondeu normalmente |
| clean verify após reinício | BUILD SUCCESS, 30 execuções sem falhas/erros/ignorados; versão anterior ao reforço dos checks de texto em branco |
| **.\mvnw.cmd -B -ntp clean verify final** | **BUILD SUCCESS**, 33 execuções (FoundationIT: 1; CatalogIT: 32, incluindo parametrizações), **0 falhas, 0 erros, 0 ignorados**. Finalizado 18:16:22 -03:00, duração 1min56s |
| docker compose config --quiet | Configuração válida |
| Primeiro Compose na porta 5432 após recuperar o daemon | Falhou: porta ainda ocupada pelo PostgreSQL 14/main do Ubuntu/WSL, não pelo projeto |
| wsl -d Ubuntu -u root -- pg_ctlcluster 14 main stop | Executado somente após autorização explícita do autor; cluster confirmado down. Nenhum WSL inteiro, banco ou volume foi apagado; serviço não foi desabilitado |
| docker compose up -d --wait; docker compose ps | PostgreSQL deste projeto saudável em 127.0.0.1:5432, preservando o volume existente |
| SELECT to_regclass('shop.products') antes de atualizar | Tabela ausente no volume da Fase 1; V001 já presente |
| Get-Content src/main/resources/db/schema/V002__products.sql -Raw, via pipe para psql do Compose | CREATE TABLE, saída zero; aplicado somente V002 |
| Carga db/demo/V001__products.sql via mesmo psql, duas vezes | Primeira: INSERT 0 4; segunda: INSERT 0 0; quatro linhas preservadas, três ativas e uma inativa |
| java -jar target/mes-legacy-shop-0.0.1-SNAPSHOT.jar com JDK 17 | Iniciou com JPA/validate e banco Compose em 5432; HTTP em 127.0.0.1:8080 |
| GET /actuator/health | HTTP 200, {"status":"UP"} |
| GET /api/products?page=0&size=2 | HTTP 200; IDs 1 e 2, totalElements=3, totalPages=2 |
| GET /api/products?page=1&size=2 | HTTP 200; ID 3 com estoque zero; inativo ID 4 não aparece nem entra nos totais |
| GET /api/products/1 | HTTP 200; somente id/sku/name/price/stockQuantity, preço 39.90 |
| GET /api/products/4 e /api/products/999 | HTTP 404 e o mesmo JSON PRODUCT_NOT_FOUND/Produto não encontrado |
| GET /api/products?size=101 | HTTP 400, INVALID_REQUEST, fieldErrors com size |
| GET /api/admin/products; POST /api/products | Respectivamente 404 e 405; administração e escrita não foram expostas |
| curl.exe --fail para saúde, página size=2 e detalhe /1, conforme README | Saída zero e JSONs esperados |
| git diff --check; git diff --cached --name-only | Sem erro de whitespace; nenhum arquivo staged |

O psql foi executado com `docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'`, recebendo o SQL por pipe. Os comandos completos Windows/Linux para banco novo, volume existente e demonstração estão no README.

## O que os testes comprovaram

- Conexão com PostgreSQL 16 real, esquema inicial, saúde HTTP sem detalhes e não exposição de /actuator/env.
- Filtro ativo/inativo no resultado e nos totais, ordenação por ID independente da ordem de inserção, paginação, catálogo vazio e página além do resultado.
- Detalhe em DTO e mesma resposta 404 para ausente/inativo; parâmetros negativos, limites e erros de tipo/overflow retornam o contrato 400.
- Persistência com identidade e valor decimal exato até a precisão contratada, sem uso de double/float para dinheiro.
- Bean Validation rejeita dados inválidos antes de persistir, sem deixar linha adicional; SQL também rejeita SKU duplicado, preço zero, estoque negativo e textos compostos somente por espaços/tabulação quando JPA é ignorado.
- A configuração real da aplicação recusa iniciar contra um esquema de teste com coluna mapeada ausente. A mensagem ERROR/Schema-validation no log é **esperada e assertada nesse teste negativo**; não é falha escondida do build. O namespace validation_probe existe somente no PostgreSQL descartável do teste e é removido ao final.

JaCoCo final: **80/83 linhas (96,39%)** e **270/278 instruções (97,12%)**; sem branches instrumentados. As três linhas não cobertas pertencem ao main e ao getter isActive; não foram criados testes de getters para elevar cobertura. Esses números não medem as regras futuras de pedidos/segurança. Relatórios em target/site/jacoco e target/failsafe-reports, não versionados.

## Incidentes e estado deixado no ambiente

O impedimento inicial era do daemon Docker, não da stack. A falha foi registrada em vez de usar H2, skip ou alterar versões. O autor reiniciou o Docker Desktop. A porta 5432 ainda estava encaminhada por wslrelay.exe para postgres PID 368 no Ubuntu; pg_lsclusters identificou 14/main online. O autor autorizou explicitamente parar esse cluster externo, que agora permanece parado.

A aplicação JAR de verificação foi encerrada. **O PostgreSQL do projeto permanece saudável em 5432**, com V001/V002 aplicados e os quatro produtos locais. Para executar a API, selecionar JAVA_HOME com JDK 17 e usar `.\mvnw.cmd spring-boot:run` (ou o JAR), conforme README. Contêineres dos testes são descartáveis e foram removidos pelo Testcontainers/Ryuk.

Logs não versionados em C:\Users\vinic\AppData\Local\Temp: mes-catalog-compile.log, mes-catalog-verify.log (falha de Docker), mes-catalog-post-restart-verify.log (sucesso intermediário), mes-catalog-final-verify.log (sucesso final) e mes-catalog-jar.log. clean remove os relatórios de target. O histórico detalhado da Fundação está no STATUS.md do commit 502c36c.

## Limitações e próximo passo

- Execução da aplicação/build em Linux nativo continua não verificada. Os comandos POSIX e Wrapper oficial foram preservados; a execução POSIX no Git Bash foi verificada na Fase 1. O uso do daemon Linux não equivale a validar o build nativo Linux.
- Não há autenticação, usuários locais, escrita administrativa, pedidos, concorrência de compras ou avaliação externa. Esses itens continuam nas fases correspondentes.
- Revisão do autor sobre o código, testes e decisões desta fase permanece pendente. Não há baseline experimental completa nem experimento Ark iniciado.

**Próximo passo: Fase 3 — Segurança e administração**, somente mediante nova solicitação. Implementar AppUser, usuários locais com BCrypt/ADMIN/CUSTOMER, HTTP Basic, política documentada de sessão/CSRF, /api/me e operações administrativas de produtos/estoque; verificar bloqueios e ausência de exposição de credenciais. Não iniciar pedidos nessa fase.

## Commit sugerido para o autor

Um commit atômico: **feat: implementa catálogo público e usa PostgreSQL local em 5432**.

Arquivos desta alteração (26):

```text
.env.example
.gitattributes
AGENTS.md
README.md
compose.yaml
docs/CONTEXTO_TP.md
docs/DECISOES.md
docs/ESPECIFICACAO.md
docs/PLANO.md
docs/STATUS.md
src/main/resources/application.yml
src/main/resources/db/schema/V002__products.sql
src/main/resources/db/demo/README.md
src/main/resources/db/demo/V001__products.sql
src/main/java/br/ufmg/mes/shop/api/ApiError.java
src/main/java/br/ufmg/mes/shop/api/ApiExceptionHandler.java
src/main/java/br/ufmg/mes/shop/product/Product.java
src/main/java/br/ufmg/mes/shop/product/ProductRepository.java
src/main/java/br/ufmg/mes/shop/product/ProductService.java
src/main/java/br/ufmg/mes/shop/product/ProductController.java
src/main/java/br/ufmg/mes/shop/product/ProductResponse.java
src/main/java/br/ufmg/mes/shop/product/ProductPageResponse.java
src/main/java/br/ufmg/mes/shop/product/ProductNotFoundException.java
src/test/java/br/ufmg/mes/shop/FoundationIT.java
src/test/java/br/ufmg/mes/shop/product/CatalogIT.java
src/test/resources/db/catalog-fixture.sql
```

Não incluir .env, target, logs ou caches. .env local foi alterado somente para a porta solicitada e permanece ignorado. POM, Wrapper e V001 de esquema permanecem no commit anterior, sem alteração. O Codex não realizou commit ou staging.
