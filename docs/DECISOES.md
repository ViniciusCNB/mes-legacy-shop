# Decisões

Data: 15/10/2026. Decisões rotineiras executadas pelo Codex dentro da autorização do autor por fase; revisão humana das novas alterações pendente. A tabela inicial preserva o histórico da Fase 1, commitada pelo autor em 502c36c. Não atribuir essas escolhas individualmente ao professor. O contexto relata a orientação acadêmica e distingue as propostas.

| Escolha | Estado e justificativa |
| --- | --- |
| Boot 2.7.18 / Java 17 / PostgreSQL 16 | Proposta da referência adotada pelo pedido desta fase. Não houve atualização de linha para resolver configuração. |
| Maven 3.9.11 / Wrapper 3.3.2 only-script | Escolha de implementação fixada, sem Maven global ou JAR Wrapper no repositório. SHA512 oficial da distribuição conferido; SHA256 calculado e fixado no Wrapper. |
| Enforcer 3.4.1 | Rejeita Java fora de [17,18) e Maven diferente de 3.9.11, evitando baseline compilada em JDK diverso. Compatibilidade verificada na execução. |
| Parent Boot e versões gerenciadas | Starters web, actuator, data-jpa, validation e test; driver PostgreSQL em runtime. Sem versões próprias de Hibernate/JUnit/driver, sem Lombok ou Security antecipado. Validation prepara a Fase 2. |
| Testcontainers BOM 1.19.8 | Boot 2.7.18 não gerencia esse BOM. Fixa módulos core/JDBC/PostgreSQL/JUnit e suas dependências; integração com JUnit Jupiter 5.8.2, JDK 17 e Docker 27.5.1 validada por execução. Não adotar automaticamente Testcontainers 2.x. |
| Failsafe 2.22.2 e Surefire 2.22.2 | Versões gerenciadas pelo parent Boot. *IT roda em verify, sem perfil opcional ou disabledWithoutDocker. Stack trace completo para diagnóstico. |
| JaCoCo 0.8.11 | Versão fixada com suporte a bytecode Java 17, relatório no verify; sem threshold artificial. A cobertura da fundação não mede ainda regras de negócio. |
| postgres:16.10-bookworm | Patch fixado para o objeto experimental, sem pretensão de selecionar a versão mais nova. Mesmo digest fixo em Compose e teste: sha256:38471f330eb885e04de130b768d6db4e10469e2311879c7e5c699f6d2d8a1c74. |
| Porta local 15432 / HTTP 8080 | 5432 já ocupada na máquina; nenhuma instância externa foi parada. Porta configurável no .env, banco e HTTP limitados a loopback. |
| Perfil local e .env como propriedades | Compose e aplicação compartilham nomes de variáveis; import nativo do Boot, sem dependência dotenv. Arquivo simples sem aspas/expansões e valores reais fora do Git. Perfil test recebe propriedades dinâmicas do contêiner. |
| Esquema shop / SQL V001 | Cria apenas namespace, não domínio fictício. Scripts montados no Compose para banco novo e aplicados pelo Testcontainers. Aplicação não executa SQL; Hibernate validate, open-in-view=false, UTC. |
| Actuator health | Endpoint existente no framework evita controller customizado sem necessidade. Apenas saúde exposta, sem detalhes; DataSource participa do estado. |
| Um teste de integração | Verifica conexão PostgreSQL 16, esquema, inicialização web e resposta pública mínima. Não há domínio para testar transações, tabelas ou regras; isso pertence às fases seguintes. |

## Referências técnicas verificadas

- [Requisitos do Boot 2.7.18](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/getting-started.html): JDK 17 está no intervalo suportado e Maven 3.9.11 atende ao requisito. A combinação foi também executada localmente.
- [POM oficial do Boot](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/2.7.18/spring-boot-dependencies-2.7.18.pom): Hibernate 5.6.15.Final, driver 42.3.8 e JUnit 5.8.2. O grafo resolvido fica registrado em STATUS.md.
- [Maven Wrapper](https://maven.apache.org/tools/wrapper/): geração oficial por plugin 3.3.2 e distribuição Maven imutável por checksum.
- [Histórico do JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/changes.html): suporte oficial ao Java 17 desde 0.8.8, preservado em 0.8.11.
- [DockerImageName do Testcontainers 1.19.8](https://github.com/testcontainers/testcontainers-java/blob/1.19.8/core/src/main/java/org/testcontainers/utility/DockerImageName.java): referências por digest usam nome@sha256; o Compose também admite tag@sha256. O conteúdo permanece igual.

## Reservado às próximas fases

Estratégia de concorrência, DTOs/paginação de pedidos e filtros foram definidos na Fase 4, registrada abaixo. A configuração Security/CSRF/sessão foi definida na Fase 3. DTOs, paginação e dados locais de produtos foram definidos na Fase 2. As regras da referência permanecem no contrato proposto, sem aprovação presumida de alterações. Não há decisão experimental final, execução Ark ou baseline/tag. O Codex não faz commits; o autor registrou a Fase 1 em 502c36c.

## Fase 2 — Catálogo

Autorizada pelo autor após o commit 502c36c da Fundação. Detalhes abaixo são escolhas de implementação, não novas orientações acadêmicas atribuídas ao professor.

| Escolha | Justificativa e estado |
| --- | --- |
| Porta 5432 | Solicitação explícita do autor; substitui a escolha local 15432 da Fase 1. Atualizados Compose, fallback JDBC, .env.example e somente a variável POSTGRES_PORT no .env local. |
| Product em shop.products | Long/IDENTITY, varchar(64) para SKU, varchar(160) para nome, numeric(19,2) para preço, inteiro de estoque e boolean ativo. Não cria usuários, pedidos ou rotas de escrita. |
| Validação Bean Validation e SQL | javax.validation gerenciado pelo Boot valida antes de persistir. Banco exige preço positivo, estoque não negativo, textos não brancos e SKU único. SKU é sensível a maiúsculas/minúsculas; sem normalização implícita. |
| V002 sem editar V001 | Banco novo aplica os dois scripts em ordem pelo entrypoint. Volume da Fundação recebe V002 explicitamente, preservando dados. Sem Flyway/Liquibase ou ddl-auto=update. |
| DTOs próprios e serviço transacional de leitura | ProductResponse e ProductPageResponse evitam serializar entidades ou o formato interno PageImpl do Spring. Organização simples por funcionalidade em product; erros HTTP comuns em api. |
| Paginação com ordem fixa | page=0/size=20 por padrão, size até 100, ordenação id crescente. page até 1.000.000 evita offset fora do intervalo do JPA mesmo com size=100. Produto sem estoque permanece no catálogo se ativo. |
| Erros mínimos | Inativo/ausente têm o mesmo 404 PRODUCT_NOT_FOUND. Parâmetros inválidos retornam 400 INVALID_REQUEST e fieldErrors. Não introduzir erros e contratos de escrita administrativa antes da Fase 3. |
| Carga local explícita por SKU | db/demo/V001__products.sql cria três ativos (um sem estoque) e um inativo; ON CONFLICT DO NOTHING permite repetição sem sobrescrever alterações. Não é executada automaticamente nem usada como fixture dos testes. |
| Testes reais sem dependências adicionais | FoundationIT preserva saúde e passa a inicializar V001/V002; CatalogIT cobre HTTP/JPA/constraints/validação de esquema em outro contêiner PostgreSQL. Schema inválido é criado somente em namespace descartável do teste; código e SQL reais permanecem corretos. |
| Stack preservada | POM e versões da Fundação permanecem inalterados; nenhuma biblioteca nova foi necessária. |

Resultados, incidentes do ambiente e limitações estão em STATUS.md. A seção acima registra o marco da Fase 2; segurança/administração foram autorizadas e implementadas na Fase 3; o protocolo experimental e a revisão acadêmica continuam separados da execução técnica.

## Fase 3 — Segurança e administração

Autorizada pelo autor após f452ab5. São decisões de implementação dentro desse escopo; não são novas exigências atribuídas ao professor. Resultados reais, falhas e revisão humana estão em STATUS.md.

| Escolha | Justificativa |
| --- | --- |
| Starter Security gerenciado pelo Boot | Spring Security 5.7.11 sem versão própria no POM; nenhuma dependência adicional de teste. Mantidos JDK 17, Boot 2.7.18, Maven e PostgreSQL. |
| AppUser / V003 | Entidade mínima com email canônico único, nome, hash BCrypt, ADMIN/CUSTOMER e active. Autenticação consulta o banco; não há API de contas ou cadastro. V001/V002 não foram editados. |
| BCrypt custo 10 | Hashes da demonstração e dos testes gerados pelo encoder real 5.7.11. Senhas de demonstração claramente locais no README; SQL guarda somente hashes. Carga ON CONFLICT preserva alterações de contas existentes. |
| HTTP Basic sem sessão de autenticação | STATELESS e request cache desativado; sem login de formulário/logout de sessão. Cada requisição protegida reavalia senha, perfil e situação no banco. Cookie CSRF não autentica. |
| CSRF mantido com cookie HttpOnly | Basic também pode ser enviado automaticamente por navegadores. CookieCsrfTokenRepository padrão, XSRF-TOKEN/X-XSRF-TOKEN, sem desativar proteção. GET /api/csrf autenticado e no-store fornece o token para o cliente. |
| Renovação de token antes de cada escrita | A cadeia padrão pode renovar o cookie durante autenticação Basic. O fluxo real preserva o cookie jar e consulta /api/csrf antes de escrever; foi acrescentado teste com CookieManager real, sem alterar estratégia de segurança para facilitar testes. |
| Política de acesso explícita | GET de catálogo/saúde público; /api/me e /api/csrf autenticados; /api/admin/** ADMIN; demais acessos negados. /error permite o tratamento padrão de erros. /actuator/env agora é negado (401 anônimo/403 autenticado); não é exposto. |
| Erros de segurança padronizados | 401 com challenge Basic para autenticação; 403 FORBIDDEN para autorização; 403 CSRF_INVALID antes da autenticação quando o token falha. Mensagens não distinguem conta inexistente, senha errada ou conta inativa. |
| DTOs de escrita distintos | Criação recebe SKU/nome/preço/estoque/ativo; PUT só nome/preço/ativo; PATCH só estoque absoluto. Resposta administrativa acrescenta active aos dados do produto. Sem exclusão física. |
| JSON estrito | Propriedades desconhecidas, frações em inteiros e coerções de tipos são rejeitadas. Desserializador de texto explícito evita conversão silenciosa de número/booleano em nome/SKU na versão gerenciada do Jackson. Validação de corpo/JSON retorna 400 sem detalhes internos. |
| Dinheiro com escala 2 | Preços válidos completam zeros antes da escrita; DTO valida no máximo duas casas e rejeita entradas que precisariam arredondamento. Não altera valores monetários nem usa float/double. |
| Escrita transacional com bloqueio de linha | PUT/PATCH leem produto com PESSIMISTIC_WRITE na transação, preservando campos de atualizações concorrentes administrativas. Compra concorrente e baixa transacional de pedidos pertencem à Fase 4. |
| Conflito real de SKU | A restrição products_sku_unique é traduzida em 409 SKU_ALREADY_EXISTS. Outras falhas de integridade não são mascaradas como SKU duplicado. |
| HTTP real nos testes | Java 17 HttpClient suporta PATCH e cookie jar sem bibliotecas extras. Usuários/BCrypt/PostgreSQL, cookies e cabeçalhos CSRF reais; sem WithMockUser, token fictício ou filtros desativados. |
| Inicialização de teste compartilhada | PostgresTestDatabase cria um contêiner independente por classe e instala V001/V002/V003. Evita divergência de scripts sem singleton de banco/contexto entre classes. |

Referência técnica consultada: [CookieCsrfTokenRepository 5.7.11](https://github.com/spring-projects/spring-security/blob/5.7.11/web/src/main/java/org/springframework/security/web/csrf/CookieCsrfTokenRepository.java). A política e o comportamento foram verificados também pela execução real.

O marco acima registra a Fase 3; pedidos foram autorizados e implementados na Fase 4, abaixo. Avaliador externo e protocolo Ark continuam reservados às fases posteriores.

## Fase 4 — Pedidos

Autorizada pelo autor após 6778cd2. Decisões de implementação, sem atribuição ao professor. POM, stack e versões preservados, sem novas dependências.

| Escolha | Justificativa |
| --- | --- |
| SQL V004 / PurchaseOrder / OrderItem | V001–V003 imutáveis; sem recriar base ou SQL automático na aplicação. FKs preservam histórico; checks de quantidade/estado/valores e produto único por pedido. |
| DTOs próprios | Entrada só produto/quantidade; proprietário vem de Basic. Resposta sem relações JPA/dados pessoais; itens por productId crescente. |
| Até 100 itens distintos | Limita trabalho por transação. Integer positivo para quantidade/estoque; numeric(19,2) para preço unitário e numeric(38,2) para resultados exatos de multiplicação/soma. |
| PESSIMISTIC_WRITE no PostgreSQL | Produtos por ID crescente na mesma transação da compra, compatível com administração. Funciona entre processos, sem lock apenas na JVM. |
| Cancelamento bloqueia primeiro o pedido | Segunda chamada aguarda e observa CANCELLED, sem nova reposição. Produtos bloqueados na ordem da compra. Reposição acima de Integer.MAX_VALUE causa 409 e rollback de todos os efeitos. |
| Propriedade e perfis | Outro cliente recebe o mesmo 404 do ausente; ADMIN consulta/cancela quaisquer, mas não compra. Basic/BCrypt/CSRF preservados. |
| Filtros por createdAt | customerId/status/start/end combinados por AND, UTC, início inclusivo/fim exclusivo. Datas opcionais, intervalo vazio/invertido inválido. |
| Paginação e resumo | Envelope/limites como catálogo; pedidos por id decrescente. Agregação SQL conta/soma apenas CREATED com preços contratados, sem carregar histórico inteiro. Vazio retorna 0/0.00. |
| HTTP e PostgreSQL reais nos testes | Duas threads sincronizadas por barreira disputam última unidade/cancelam pedido; um bloqueio independente só é liberado quando pg_stat_activity comprova as duas transações esperando no PostgreSQL. Assertions verificam respostas e estado persistido. Fixtures/probe de esquema incluem FKs/tabelas novas, sem reduzir verificações anteriores. |

Avaliador externo, métricas consolidadas e congelamento de baseline pertencem à Fase 5. Ark permanece posterior.

Instantes são truncados a microssegundos antes da gravação, correspondendo à precisão do PostgreSQL e mantendo respostas iniciais/consultas/repetições exatamente iguais. O leitor JSON dos testes usa BigDecimal para verificar dinheiro sem perda no próprio cliente.

## Fase 5 — Consolidação

Autorizada pelo autor após 4979132. O código/SQL/testes internos da aplicação e POM/Compose local permanecem inalterados. São escolhas de implementação, não novos acordos atribuídos ao professor.

| Escolha | Justificativa |
| --- | --- |
| Maven independente em evaluation/ | Sem parent Boot, módulo, Spring/JPA/JDBC ou imports da aplicação. JUnit 5.8.2, Jackson 2.13.5 e plugins com versões explícitas. As versões coincidem com componentes já verificados, mas o projeto não herda o grafo do Boot. |
| HttpClient Java 17 / BigDecimal | HTTP real e JSON exato, sem acrescentar cliente HTTP externo ou usar double para dinheiro. |
| Dados exclusivos | Fixture externa versionada, 1 ADMIN/2 clientes/1 inativo e 3 produtos conhecidos. BCrypt real, carga não repetível em banco vazio, sem truncar a base local nem reaproveitar fixtures internas. |
| Projeto Compose aleatório por execução | Mesmo PostgreSQL/digest, volume novo e porta dinâmica de loopback. Recusa identidades existentes. Não reinicia ou apaga o ambiente local de 5432. |
| Orquestração PowerShell/Bash | JAR fornecido pelo build raiz, perfil evaluation por variáveis, HTTP 18080 por padrão, espera de saúde com limite e limpeza só do processo/projeto criados. Uma execução por checkout; relatórios locais compartilhados. |
| Sem endpoints especiais | Preparação por SQL/infraestrutura, comportamento somente por API com Basic/CSRF reais. IDs e instantes dos cenários vêm das respostas, sem alterações diretas do banco durante os testes. |
| Concorrência externa observável | POSTs com tokens obtidos antes da barreira; três disputas independentes devem passar sem retry. Não tenta inferir mecanismo de lock via HTTP. Prova determinística e constraints permanecem nos testes internos. |
| Relatórios/identidade fora do Git | Artefato SHA256, fonte/estado da pasta, versão/snapshot do avaliador e logs/resultados por execução em target/evaluation-runs. Arquivar antes de clean; excluir evaluation/target do Git. |
| Snapshot candidato | evaluation/SNAPSHOT.sha256 registra especificação e insumos externos com LF. Revisão/congelamento definitivo depende do autor; mudanças futuras exigem versão/registro/revisão, sem ajustar assertions silenciosamente à migração. |
| Contador Java sem dependências | JDK já é requisito; tools/SourceMetrics.java mede linhas físicas totais/não vazias por grupo. Comentários contam; não chamar de SLOC ou adicionar linhas para atingir meta. |
| Identidade da baseline | Aplicação candidata em 4979132; commit final da consolidação será feito e identificado pelo autor. Não criar tag nem iniciar Ark. Conteúdo exato do pacote de migração depende de protocolo posterior; avaliador fica fora. |

Compatibilidade verificada com JDK 17/Maven 3.9.11 e execuções reais; resultados completos em STATUS.md. A configuração useModulePath inicialmente gerava aviso de parâmetro desconhecido no Surefire 2.22.2 e foi removida. Nenhuma verificação foi desativada. O JAR vazio de um projeto apenas de testes não é artefato da loja.

O Ubuntu/WSL disponível tem JDKs 11 e 21, sem JDK 17. Não foi instalado JDK no sistema nem executado build em outra linha para contornar a stack. Git Bash valida a receita POSIX usando o JDK Windows; não comprova execução nativa Linux.
