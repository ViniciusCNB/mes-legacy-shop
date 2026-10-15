# Decisões

Data: 15/10/2026. Decisões rotineiras executadas pelo Codex dentro da autorização do autor para a Fase 1; revisão humana pendente. Não atribuir essas escolhas individualmente ao professor. O contexto relata a orientação acadêmica e distingue as propostas.

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

Estratégia de concorrência, DTOs, paginação, semântica completa dos filtros, configuração Security/CSRF/sessão e dados de demonstração serão decididos quando implementados. As regras da referência permanecem no contrato proposto, sem aprovação presumida de alterações. Nenhuma decisão experimental final, execução Ark, baseline/tag ou commit foi feita.
