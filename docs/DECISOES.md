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

Estratégia de concorrência, DTOs/paginação de pedidos, semântica completa dos filtros e configuração Security/CSRF/sessão serão decididos nas próximas fases. DTOs, paginação e dados locais de produtos foram definidos na Fase 2. As regras da referência permanecem no contrato proposto, sem aprovação presumida de alterações. Não há decisão experimental final, execução Ark ou baseline/tag. O Codex não faz commits; o autor registrou a Fase 1 em 502c36c.

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

Resultados, incidentes do ambiente e limitações estão em STATUS.md. Segurança/administração ficam para a Fase 3; o protocolo experimental e a revisão acadêmica continuam separados da execução técnica.
