# Caracterização da origem — Java 8 / Spring Boot 2.1.18

Data: 11/11/2026. Etapa R6 do [plano](PLANO.md). Descreve as características legadas presentes na origem reconstruída, onde estão, por que a migração para Java 17 / Spring Boot 3.5 precisa tratá-las e qual teste falha se forem tratadas errado. O critério de admissão é o P1 de [DECISOES.md](DECISOES.md). O [levantamento anterior](LEVANTAMENTO_LEGADO.md) descreve a baseline do piloto.

## Resumo

| | Piloto (Java 17 / Boot 2.7.18) | Origem (Java 8 / Boot 2.1.18) |
| --- | --- | --- |
| Tipos de marcador legado presentes | 3 | 19 (15 de quebra, 4 de obsolescência) |
| Ocorrências | 96 | 189 |
| Destinos do piloto (claude-01, codex-03) | 0 | — |
| Código de produção (arquivos / linhas físicas) | 42 / 1.565 | 43 / 1.591 |
| Testes internos (arquivos / linhas físicas) | 5 / 1.126 | 6 / 1.303 |
| Testes internos (métodos / casos / pontos de asserção) | 39 / 123 / 230 | 39 / 123 / 230 |

Contagens geradas por `java tools/LegacyScan.java <raiz>` e `java tools/SourceMetrics.java` (JDK 17); inventário da suíte por `tools/TestInventory.java`. Na origem, o legado aparece no código de produção, na configuração, no build e na suíte de testes.

## Características legadas

**Quebra:** com o marcador presente, a aplicação não compila, não inicia ou não passa nos testes no Boot 3.5. **Obsoleto:** ainda funciona no Boot 3.5, mas está descontinuado ou foi substituído pelo idioma da stack de destino; a modernização é medida pela varredura.

A coluna "Verificado" indica como a quebra foi confirmada nos artefatos do Boot 3.5.16 (Hibernate 6.6.x, Spring Security 6.5.x, Spring Framework 6.2.x) baixados no repositório Maven local.

| ID | Característica | Onde | Tipo | Efeito no Boot 3.5 | Verificado | Teste que detecta migração errada | Fonte oficial |
| --- | --- | --- | --- | --- | --- | --- | --- |
| M01 | `javax.persistence`, `javax.validation`, `javax.servlet` | 17 arquivos de produção (entidades, repositórios, DTOs, controladores, `OrderService`, tratadores de erro) e `CatalogIT` | Quebra | Jakarta EE 9+: os pacotes passam a ser `jakarta.*` | Compilação no piloto | Toda a suíte (não compila) | Boot 3.0 Migration Guide |
| M02 | `extends WebSecurityConfigurerAdapter` | `SecurityConfiguration` | Quebra | Classe removida no Security 6 | Ausente do `spring-security-config` 6.5 | Toda a suíte (não compila) | Blog Spring Security, fev/2022; Security 6.0 migration |
| M03 | `antMatchers(...)` | `SecurityConfiguration` (8 regras) | Quebra | Removido; passa a ser `requestMatchers` | `AbstractRequestMatcherRegistry` 6.5 só tem `requestMatchers` | Toda a suíte (não compila) | Security 6.0 migration |
| M04 | `authorizeRequests()` | `SecurityConfiguration` | Obsoleto | Descontinuado em favor de `authorizeHttpRequests` | Ainda presente no 6.5 | Varredura | Security 6.0 migration |
| M05 | Encadeamento `.and()` da DSL | `SecurityConfiguration` | Obsoleto | Descontinuado em favor da DSL de lambdas | Ainda presente no 6.5 | Varredura | Security 6.1 release notes |
| M06 | `@EnableWebSecurity` sem `@Configuration` | `SecurityConfiguration` | Quebra | A anotação deixa de incluir `@Configuration`; a configuração é ignorada e vale a segurança padrão do Boot | Meta-anotação presente no 5.1.13 e ausente no 6.5 | `SecurityAdminIT`, `OrdersIT` e avaliador (401/403, CSRF e erros JSON) | Security 6.0 migration |
| M07 | `CookieCsrfTokenRepository` sem tratador de requisição | `SecurityConfiguration`, `CsrfController` | Quebra | Token mascarado (XOR) e carregamento adiado; o token publicado deixa de ser igual ao cookie | Piloto: as duas tentativas precisaram ajustar | `csrf(...)` em `SecurityAdminIT` e `OrdersIT`; avaliador | Security 5.8/6.0 migration (exploits) |
| M08 | Dialeto `PostgreSQL95Dialect` declarado | `application.yml` | Quebra | Dialetos versionados saem do `hibernate-core` 6; o contexto não inicia | Classe ausente do `hibernate-core` 6.6.53 | Toda a suíte e avaliador (contexto não inicia) | Hibernate 6.0 migration guide |
| M09 | `spring.datasource.initialization-mode` | `application.yml` | Obsoleto | Propriedade com descontinuação de nível `error` desde 2.5; substituída por `spring.sql.init.mode` e ignorada no 3.5 | Metadados do `spring-boot-autoconfigure` 3.5.16 | Varredura (o valor `never` coincide com o efeito padrão para PostgreSQL) | Boot 2.5 release notes |
| M10 | Java 8 (`java.version` 1.8, enforcer `[1.8,1.9)`) | `pom.xml` | Quebra | Boot 3 exige Java 17 | Requisito do Boot 3 | Build (enforcer e compilação) | Boot 3.0 Migration Guide |
| M11 | Parent `2.1.18.RELEASE` | `pom.xml` | Quebra | É o objeto da migração | — | — | — |
| M12 | JUnit 4 (`org.junit.Test`, `@Rule`, `@ClassRule`, `@After`, `@RunWith`) | 4 classes de teste | Quebra | O starter de testes traz só a JUnit Platform; testes JUnit 4 não executam sem o motor vintage. Com `failIfNoTests=true`, o Failsafe falha | Comportamento do Boot desde 2.4 | Build (`failIfNoTests`) e inventário | Boot 2.4 release notes; guia de migração do JUnit 5 |
| M13 | `SpringRunner`, `SpringClassRule`, `SpringMethodRule` | 4 classes de teste | Quebra | Integração JUnit 4 do Spring TestContext | Idem M12 | Idem M12 | Idem M12 |
| M14 | JUnitParams (`JUnitParamsRunner`, `@Parameters`) | `pom.xml` e 3 classes de teste | Quebra | Sem suporte à JUnit Platform; passa a ser `@ParameterizedTest` | Idem M12 | Idem M12; inventário confere os 123 casos | Guia de migração do JUnit 5 |
| M15 | Contêiner Testcontainers como `@ClassRule` | 4 classes de teste | Obsoleto | Substituído por `@Testcontainers`/`@Container` (JUnit 5) ou `@ServiceConnection` | Testcontainers 1.21.4 ainda tem regras JUnit 4 | Varredura | Documentação do Testcontainers |
| M16 | `ConfigFileApplicationContextInitializer` | `CatalogIT` | Quebra | Removido; passa a ser `ConfigDataApplicationContextInitializer` | Classe ausente do `spring-boot-test` 3.5.16 | `CatalogIT` (não compila) | Boot 2.4 release notes; Boot 3.0 Migration Guide |
| M17 | `org.springframework.boot.web.server.LocalServerPort` | `SecurityAdminIT`, `OrdersIT` | Quebra | Removido do pacote antigo; existe só em `org.springframework.boot.test.web.server` | Classe só no novo pacote no 3.5.16 | Não compila | Boot 2.7 release notes; Boot 3.0 Migration Guide |
| M18 | Apache HttpClient 4 (`org.apache.http`) | `pom.xml`, `HttpResult`, `SecurityAdminIT`, `OrdersIT` | Quebra | O Boot 3.5 não gerencia mais a versão do HttpClient 4; só o HttpClient 5 | `spring-boot-dependencies` 3.5.16 sem `httpclient.version` | Build (dependência sem versão) | Boot 3.0 Migration Guide |
| M19 | `ApplicationContextInitializer` com `TestPropertyValues` para o banco | 4 classes de teste | Obsoleto | Substituído por `@DynamicPropertySource` (Boot 2.2.6+) ou `@ServiceConnection` (3.1+) | Ainda funciona no 3.5 | Varredura | Documentação do Spring Boot |

As fontes oficiais citadas devem ser conferidas antes de entrar no texto da dissertação. A coluna "Verificado" foi conferida nos JARs; as referências bibliográficas ainda não foram revisadas.

## Mudanças de comportamento sem marcador textual

| Comportamento | Situação na origem | Cobertura |
| --- | --- | --- |
| Barra final nas rotas (Spring MVC 6 deixa de casar `/rota/`) | 7 rotas aceitam barra final, igual à baseline do piloto | Só o roteiro de sondagem. Decisão no R7 |
| Mapeamento de `Instant` no Hibernate 6 (`TIMESTAMP_UTC`) | `created_at`/`cancelled_at` em `timestamptz`, `hibernate.jdbc.time_zone=UTC` | `OrdersIT` (filtros nos limites exatos de tempo) e avaliador |
| Formato do token CSRF e renovação do cookie | Token igual ao cookie; renovado após escrita autenticada | Testes de CSRF e roteiro de sondagem |

## Como a evidência é usada

1. **Antes da migração:** esta tabela e a varredura mostram o legado na origem.
2. **Depois da migração:** a varredura no destino mostra o que foi removido e o que restou; o inventário mostra que os 39 métodos e 123 casos foram preservados, sem skips e sem perda de asserções; o avaliador externo e o roteiro de sondagem mostram o comportamento.
3. **Leitura conjunta:** uma migração que só adiciona o motor vintage do JUnit 4 passa nos testes, mas mantém M12 a M14 na varredura. Uma que remove testes ou asserções passa na varredura, mas aparece no inventário.
