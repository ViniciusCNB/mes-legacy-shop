# Status

Atualização: 15/10/2026, America/Sao_Paulo. **Fase 3 — Segurança e administração implementada e verificada.** A Fase 2 foi commitada pelo autor em `f452ab5`. Nenhum commit, staging ou tag foi feito pelo Codex nesta continuação. A Fase 4 não foi iniciada.

## Entregue

- Mantidos Boot 2.7.18, Java 17, Maven Wrapper 3.9.11, PostgreSQL 16.10/digest e porta local 5432. Adicionado somente spring-boot-starter-security, com Spring Security 5.7.11 gerenciado pelo Boot; sem novas bibliotecas de teste.
- AppUser com id, nome, email canônico único, hash BCrypt, ADMIN/CUSTOMER e situação ativa. V003__users.sql acrescenta app_users; V001/V002 de esquema foram preservados.
- HTTP Basic consulta usuários no banco em cada requisição; BCrypt custo 10. Sem sessão de autenticação, formulário de login, JWT ou cadastro público.
- GET /api/me em DTO com somente id/name/email/role. Usuário inativo, inexistente ou senha inválida retorna 401 genérico com challenge Basic.
- GET /api/csrf autenticado/no-store, cookie HttpOnly e cabeçalho reais. CSRF permanece habilitado nas escritas; cookie CSRF não autentica. Fluxo de renovação com cookie jar documentado e testado.
- POST /api/admin/products, PUT /api/admin/products/{id} e PATCH /api/admin/products/{id}/stock restritos a ADMIN. Criação 201, atualização 200, estoque absoluto, inativação sem excluir linha e 409 para SKU duplicado.
- DTOs de escrita com validação; JSON estrito para nomes, valores, campos desconhecidos e tipos. Preços válidos completam duas casas decimais sem arredondar entradas inválidas. PUT/PATCH são transacionais e usam bloqueio de linha.
- Erros 400/401/403/404/409 padronizados sem expor hashes, credenciais, classes ou stack traces. Acesso não previsto é negado por padrão; saúde/catálogo continuam públicos. /actuator/env passou de 404 para 401 anônimo/403 autenticado pela nova política explícita.
- Carga explícita de demonstração com um administrador e dois clientes ativos, hashes BCrypt e credenciais locais no README. Repetição por email preserva contas existentes; fixtures de teste independentes.
- Documentação, decisões, plano e instruções atualizados. Sem pedidos, frontend, serviços externos ou migração Ark.

## Ambiente e versões verificadas

Windows 11 amd64; Microsoft OpenJDK 17.0.15 selecionado em JAVA_HOME; Maven 3.9.11; Docker Engine 27.5.1/API 1.47, daemon Linux amd64; Compose 2.32.4. Imagem PostgreSQL preservada: postgres:16.10-bookworm, digest sha256:38471f330eb885e04de130b768d6db4e10469e2311879c7e5c699f6d2d8a1c74.

`dependency:tree` confirmou spring-security-config/core/crypto/web **5.7.11**. O grafo filtrado está em target/security-dependency-tree.txt, não versionado. JUnit, Testcontainers, Hibernate, driver JDBC e JaCoCo mantêm as versões anteriores. Os testes HTTP novos usam o cliente nativo Java 17, incluindo PATCH, sem spring-security-test/WithMockUser ou mocks de autenticação.

## Comandos e resultados reais

Os comandos Maven usaram JDK 17. As receitas completas de preparação, execução e uso Windows/POSIX estão no README.

| Comando/verificação | Resultado observado |
| --- | --- |
| Inspeção integral de AGENTS/README/docs/código e Git | Árvore inicialmente limpa; Fase 2 no commit f452ab5; nenhuma base recriada |
| docker version; docker compose ps | Daemon acessível com acesso ampliado; banco deste projeto saudável em 127.0.0.1:5432 |
| .\mvnw.cmd -B -ntp compile / test-compile | BUILD SUCCESS com o starter de segurança e implementação/estrutura de teste |
| Primeiro clean verify | 71 execuções, 1 falha e 1 erro, 0 ignorados. Falha real de coerção JSON e erro de conexão inicial registrados abaixo |
| clean verify após correção e teste de cookie jar | BUILD SUCCESS, 72 execuções sem falhas/erros/ignorados |
| clean verify com casos monetários adicionais | BUILD SUCCESS, 74 execuções; foram depois tornados literais/exatos os valores esperados desses testes |
| **.\mvnw.cmd -B -ntp clean verify final** | **BUILD SUCCESS, 74 execuções, 0 falhas, 0 erros, 0 ignorados**. FoundationIT: 1; CatalogIT: 32; SecurityAdminIT: 41 (incluindo parametrizações). Finalizado 18:51:46 -03:00, duração 1min09s |
| .\mvnw.cmd -B -ntp dependency:tree '-Dincludes=org.springframework.security:*' '-DoutputFile=target/security-dependency-tree.txt' | BUILD SUCCESS; todos os módulos de segurança resolvidos em 5.7.11 |
| SELECT to_regclass('shop.app_users') no volume local | Tabela ausente, permitindo aplicar somente V003 sem reexecutar scripts anteriores |
| Get-Content src/main/resources/db/schema/V003__users.sql -Raw via pipe para psql do Compose | CREATE TABLE, saída zero, volume existente preservado |
| Carga db/demo/V002__users.sql via psql, duas vezes | INSERT 0 3 e INSERT 0 0; três usuários ativos, perfis corretos, hashes com 60 caracteres |
| java -jar target/mes-legacy-shop-0.0.1-SNAPSHOT.jar com JDK 17 | Iniciou com validação de esquema e PostgreSQL em 5432; HTTP em 127.0.0.1:8080 |
| curl.exe --fail --user para /api/me; consultas das três contas locais | HTTP 200 com id/name/email/role de cada conta; sem senha/hash |
| Exemplo PowerShell completo do README com WebSession | POST criou ADMIN-DEMO-001 (ID 9); PUT alterou nome/preço/ativo; PATCH definiu estoque 7. Produto permaneceu inativo e preservado |
| Exemplo curl/awk do README executado no Git Bash | Saída zero; cookie HttpOnly/token aceitos; POST criou ADMIN-DEMO-LINUX-001 (ID 10). Depois foi inativado pela API |
| GET /api/me sem Basic | 401 AUTHENTICATION_REQUIRED |
| POST administrativo com Basic e sem token CSRF | 403 CSRF_INVALID |
| Cliente local com Basic e CSRF válidos em POST administrativo | 403 FORBIDDEN, sem criação de produto |
| Repetição do SKU ADMIN-DEMO-001 com ADMIN e CSRF válidos | 409 SKU_ALREADY_EXISTS; produto existente preservado |
| Produto criado e depois inativado consultado publicamente | 404 PRODUCT_NOT_FOUND |
| curl.exe --fail para saúde e catálogo sem autenticação | 200/UP; catálogo público com os três ativos originais e totais corretos |
| git diff --check; git diff --cached --name-only; git log -1 | Sem erro de whitespace, index vazio, HEAD permanece f452ab5 |

O psql recebeu os SQLs por pipe em `docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'`. Os scripts antigos não foram reaplicados nem editados; não houve reset de volume.

## Comportamentos comprovados pela suíte

- Regressão de saúde/conexão/catálogo/paginação/DTOs e validação do esquema, preservando as assertions de negócio da Fase 2. A atualização de status do Actuator não exposto foi definida no contrato antes da implementação da segurança.
- Administrador e dois clientes distintos autenticados a partir de PostgreSQL real; hashes BCrypt válidos; conta inativa/ausente/senha errada rejeitadas com mensagem genérica e challenge Basic.
- /api/me sem senha/hash ou entidade JPA. Cookies CSRF não dão acesso sem Basic; nenhum JSESSIONID foi criado. Alterar perfil/active no banco afeta a requisição seguinte.
- Cada uma das três operações administrativas bloqueia cliente (403), anônimo/senha errada com CSRF válido (401), e administrador sem CSRF ou com token divergente (403). Nessas falhas, os dados permanecem iguais à fixture.
- Fluxo administrativo completo: criação, mudança de detalhes, ocultação de inativo sem exclusão, estoque absoluto/repetição sem incremento, estoque zero e reativação no catálogo.
- Conflito de SKU pelo banco traduzido em 409; produto ausente em PUT/PATCH retorna 404; corpos/IDs inválidos e JSON/coerções/campos indevidos retornam 400 sem efeitos parciais.
- Preços 0.1 e 12 representados e persistidos como 0.10 e 12.00. Mais de duas casas continuam inválidas; não há arredondamento silencioso.
- Renovação de token antes de cada escrita com CookieManager real e autenticação em cada chamada. Cookie HttpOnly, Path=/, no-store e ausência de sessão de autenticação verificados.

JaCoCo final: **240/244 linhas (98,36%)**, **947/960 instruções (98,65%)**, **10/12 branches (83,33%)**. Sem meta arbitrária ou testes de getters para inflar cobertura. A métrica não substitui assertions de autorização, CSRF ou regras futuras de pedidos. Relatórios em target/site/jacoco e target/failsafe-reports, não versionados.

## Falhas encontradas e resolução

1. Primeiro build completo: FoundationIT recebeu `java.net.SocketException: Connection reset` ao abrir conexão JDBC após o início do contêiner. Hibernate em seguida não obteve metadados para resolver dialect. Não foi fixado dialect para esconder a falha, nem alterado timeout/retry/skip. A causa além do reset de conexão não foi determinada; a falha não se repetiu nas três execuções completas posteriores, todas aprovadas.
2. O primeiro teste HTTP detectou nome numérico aceito como texto (esperava 400, recebeu 201). A configuração de coerção de texto não impedia essa conversão na versão gerenciada do Jackson. Foi substituída por desserialização estrita de String; a assertion foi mantida e passou. Nenhuma versão foi atualizada para contornar o problema.
3. A observação com cookie jar mostrou renovação do token/cookie na cadeia padrão após autenticação. O fluxo documentado foi verificado renovando o token antes de cada escrita, sem desativar CSRF ou alterar filtros para facilitar testes.
4. Logs ERROR de chave duplicada e de coluna ausente são esperados nos testes negativos, assertados como 409 e recusa de inicialização, respectivamente. Não são erros ocultados do resultado final.

Logs em C:\Users\vinic\AppData\Local\Temp, não versionados: mes-security-dependencies.log, mes-security-compile.log, mes-security-first-verify.log (falhas), mes-security-second-verify.log (72 aprovados), mes-security-final-verify.log e mes-security-accepted-verify.log (74 aprovados), mes-security-jar.log (diagnóstico) e mes-security-accepted-jar.log (JAR final). O histórico das fases anteriores permanece nos commits do autor.

## Estado local ao terminar

- Aplicação JAR de verificação encerrada; banco Compose permanece saudável em 5432.
- Esquema V001/V002/V003 aplicado ao volume preservado. Há seis produtos: quatro da carga da Fase 2 e dois criados pelos exemplos desta fase. Três permanecem ativos no catálogo; os produtos ADMIN-DEMO-001 e ADMIN-DEMO-LINUX-001 estão inativos, sem exclusão física.
- Três usuários locais ativos da carga V002 de demonstração. Credenciais e comandos em README.md; repetir a carga não sobrescreve contas.
- Contêineres dos testes são descartáveis e foram removidos pelo Testcontainers/Ryuk. .env, target, logs e arquivos temporários não são versionados.

## Limitações e próximo passo

- Build/aplicação em Linux nativo continuam não verificados. O exemplo POSIX HTTP foi executado no Git Bash do Windows; isso não comprova execução nativa do Java/Boot em Linux.
- O reset de conexão isolado deve ser considerado se reaparecer em outra execução; os resultados posteriores não apagam a falha registrada.
- Pedidos, concorrência de compras, cancelamento, filtros/resumo e avaliação externa não foram implementados. Revisão humana das mudanças e do contrato desta fase permanece pendente; não há baseline experimental completa ou execução Ark.

**Próximo passo: Fase 4 — Pedidos**, somente mediante nova solicitação. Implementar propriedade pelo cliente autenticado, valores e preço contratado, baixa transacional de estoque, atomicidade, isolamento entre clientes, cancelamento com reposição única, consultas/filtros/resumo e disputa pelo último item, com PostgreSQL real. Não iniciar o avaliador externo antes da Fase 5.

## Commit sugerido para o autor

Um commit atômico: **feat: adiciona segurança e administração de produtos**.

Arquivos desta alteração (37):

```text
AGENTS.md
README.md
docs/CONTEXTO_TP.md
docs/DECISOES.md
docs/ESPECIFICACAO.md
docs/PLANO.md
docs/STATUS.md
pom.xml
src/main/java/br/ufmg/mes/shop/api/ApiExceptionHandler.java
src/main/java/br/ufmg/mes/shop/api/JsonConfiguration.java
src/main/java/br/ufmg/mes/shop/product/Product.java
src/main/java/br/ufmg/mes/shop/product/ProductRepository.java
src/main/java/br/ufmg/mes/shop/product/AdminProductResponse.java
src/main/java/br/ufmg/mes/shop/product/CreateProductRequest.java
src/main/java/br/ufmg/mes/shop/product/UpdateProductRequest.java
src/main/java/br/ufmg/mes/shop/product/StockUpdateRequest.java
src/main/java/br/ufmg/mes/shop/product/ProductAdminController.java
src/main/java/br/ufmg/mes/shop/product/ProductAdminService.java
src/main/java/br/ufmg/mes/shop/product/SkuAlreadyExistsException.java
src/main/java/br/ufmg/mes/shop/security/ApiSecurityHandlers.java
src/main/java/br/ufmg/mes/shop/security/CsrfController.java
src/main/java/br/ufmg/mes/shop/security/SecurityConfiguration.java
src/main/java/br/ufmg/mes/shop/user/AppUser.java
src/main/java/br/ufmg/mes/shop/user/AppUserRepository.java
src/main/java/br/ufmg/mes/shop/user/AppUserService.java
src/main/java/br/ufmg/mes/shop/user/UserController.java
src/main/java/br/ufmg/mes/shop/user/UserResponse.java
src/main/java/br/ufmg/mes/shop/user/UserRole.java
src/main/resources/application.yml
src/main/resources/db/schema/V003__users.sql
src/main/resources/db/demo/README.md
src/main/resources/db/demo/V002__users.sql
src/test/java/br/ufmg/mes/shop/FoundationIT.java
src/test/java/br/ufmg/mes/shop/product/CatalogIT.java
src/test/java/br/ufmg/mes/shop/security/SecurityAdminIT.java
src/test/java/br/ufmg/mes/shop/support/PostgresTestDatabase.java
src/test/resources/db/security-fixture.sql
```

Não incluir .env, target, logs, caches ou temporários. Sem commit/staging feito pelo Codex; revisão e commits ficam com o autor.
