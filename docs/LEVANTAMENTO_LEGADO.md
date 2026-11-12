# Levantamento de características legadas — 11/11/2026

Objetivo: mostrar quais características antigas de Java e Spring a aplicação de origem tem, onde aparecem e o que a migração precisou mudar. Este documento é diagnóstico. Não altera a baseline, o protocolo ou as decisões, e a interpretação fica com o autor.

## Conclusão curta

A baseline compila para Boot 2.7.18, mas foi escrita no estilo recomendado para Boot 3: Spring Security com `SecurityFilterChain` e DSL de lambdas, `authorizeHttpRequests`, injeção por construtor, sem `WebSecurityConfigurerAdapter`, sem propriedades renomeadas e sem extensões do Hibernate.

Das quebras conhecidas da passagem 2.7 → 3.x, só quatro aparecem no código:
- o namespace `javax.*`;
- `antMatchers`;
- o comportamento do token CSRF;
- a barra final implícita nas rotas.

Isso explica por que as duas migrações levaram poucos minutos e mudaram quase só imports. A aplicação atual tem poucas características legadas: além de artificial, é superficial demais para representar o legado.

## 1. O que já existe

| Característica legada | Onde aparece | Mudança exigida no Boot 3.x | Nas tentativas claude-01 e codex-03 |
|---|---|---|---|
| Namespace `javax.persistence` e `javax.validation` (Java EE) | 17 arquivos de `src/main`: entidades `Product`, `AppUser`, `PurchaseOrder` e `OrderItem`; repositórios; `OrderService`; DTOs de requisição; controladores; `ApiExceptionHandler`. Também `CatalogIT` em teste | Troca para `jakarta.*` | Ambas trocaram de forma mecânica |
| `javax.servlet` | `ApiSecurityHandlers` | Troca para `jakarta.servlet` | Ambas trocaram |
| `antMatchers` | `SecurityConfiguration` (9 regras) | Removido no Security 6; passa a ser `requestMatchers` | Claude usou `requestMatchers(String)`; Codex usou `AntPathRequestMatcher` |
| Token CSRF sem máscara e carregado na hora | `SecurityConfiguration`, `CsrfController` | O Security 6 mascara o token (XOR) e adia o carregamento | Ambas preservaram o comportamento antigo de forma explícita |
| Barra final aceita sem configuração | Todos os `@RequestMapping` | O Spring MVC 6 deixa de casar `/rota/` | Claude não preservou; Codex preservou só as rotas administrativas (ver STATUS.md) |
| Testcontainers fixado em 1.19.8 por BOM próprio | `pom.xml` | Pode passar a ser gerenciado pelo Boot | Codex removeu o BOM; Claude manteve |

## 2. O que falta: as quebras clássicas da migração 2.7 → 3.x

Nenhum dos itens abaixo aparece hoje no código. Cada um é típico de aplicações Spring da era 2.x e quebra ou fica obsoleto no Boot 3. Ficam aqui como catálogo para o autor decidir, não como recomendação de inserir todos.

| Área | Característica legada | O que muda no 3.x |
|---|---|---|
| Spring Security | `extends WebSecurityConfigurerAdapter` com `configure(HttpSecurity)` | Classe removida; a configuração vira um bean `SecurityFilterChain` |
| Spring Security | `authorizeRequests()` e encadeamento com `.and()` | Substituídos por `authorizeHttpRequests` e pela DSL de lambdas |
| Spring Security | `@EnableGlobalMethodSecurity(prePostEnabled = true)` | Substituído por `@EnableMethodSecurity` |
| Spring Security | Classe só com `@EnableWebSecurity`, sem `@Configuration` | A anotação deixa de incluir `@Configuration` |
| Spring Security | `mvcMatchers` e `regexMatchers` | Removidos |
| Boot | Propriedades renomeadas, como `spring.redis.*`, `management.metrics.export.*`, `server.max-http-header-size` e `spring.jpa.hibernate.use-new-id-generator-mappings` | Mudam de nome ou deixam de existir |
| Boot | Autoconfiguração registrada em `META-INF/spring.factories` | Passa para `AutoConfiguration.imports` |
| Boot | `@ConstructorBinding` na classe de `@ConfigurationProperties` | Muda de pacote e de semântica |
| Hibernate 5 → 6 | `@Type(type = "...")`, `@TypeDef`, `UserType` próprio, biblioteca `hibernate-types` | API de tipos reescrita |
| Hibernate 5 → 6 | `GenerationType.AUTO` ou `SEQUENCE` com `hibernate_sequence` compartilhada | Gera sequências por entidade |
| Hibernate 5 → 6 | Dialeto explícito, como `PostgreSQL10Dialect`, e HQL com construções que o Hibernate 6 rejeita | Dialetos versionados são descontinuados; o HQL fica mais estrito |
| Spring MVC 6 | `extends ResponseEntityExceptionHandler` com `HttpStatus` na assinatura | A assinatura passa a usar `HttpStatusCode` |
| Spring MVC 6 | `switch` sobre `HttpMethod` | `HttpMethod` deixa de ser `enum` |
| Spring MVC 6 | `ResponseEntity.getStatusCodeValue()` | Obsoleto |
| Spring MVC 6 | Barra final sem teste que a cubra | Muda o comportamento em silêncio; hoje já existe, mas sem teste |
| Cliente HTTP | `RestTemplate` com Apache HttpClient 4 | O Boot 3 só integra o HttpClient 5 |
| Testes | `@MockBean` e `@SpyBean` | Obsoletos no 3.4; passam a ser `@MockitoBean` |
| Actuator | `httptrace` | Renomeado para `httpexchanges` |

## 3. Java 17 ou Java 8 na origem

Decidido em D1 (DECISOES.md): a nova origem usa Java 8 e Boot 2.1.18. A análise abaixo descreve a baseline atual.

Hoje a origem usa Java 17. O Boot 2.7 aceita Java 8 a 21, e o Boot 3 exige Java 17. Começar em Java 8 tornaria obrigatório atualizar a linguagem e a plataforma junto com o Spring. Isso faz parte da modernização típica e dá uma justificativa natural para a escolha de origem e destino.

Uso atual de recursos posteriores ao Java 8:

| Recurso | Onde |
|---|---|
| `List.of` (Java 9) | `ApiExceptionHandler`, `ApiSecurityHandlers` (8 usos) |
| `var` (Java 10) | `FoundationIT` |
| `java.net.http.HttpClient` (Java 11) | `OrdersIT`, `SecurityAdminIT` |
| `String.repeat` (Java 11) | `OrdersIT`, `CatalogIT` |
| `maven-enforcer` exige JDK `[17,18)` | `pom.xml` |

O código de produção mudaria pouco, só os `List.of`. Os testes de integração precisariam trocar o `HttpClient` do Java 11, que também é usado pelo avaliador externo. O avaliador pode continuar em Java 17, porque é um projeto Maven separado e só fala HTTP com o JAR.

Características legadas que só um código Java 8 teria:
- `java.util.Date`, `Calendar` e `SimpleDateFormat`;
- JAXB (`javax.xml.bind`), removido do JDK no Java 11;
- `sun.misc.*`;
- classes anônimas no lugar de lambdas.

## 4. Pontos para a justificativa de origem e destino

A justificativa adotada está em D1 (DECISOES.md). Os pontos abaixo valiam para a origem 2.7 e ficam como registro.

As datas e prazos de suporte abaixo precisam ser conferidos nas fontes oficiais antes de entrar no texto da dissertação.

- **Boot 2.7:** última linha 2.x. A 2.7.18 é a última versão de código aberto, e é a última linha que roda em Java 8.
- **Boot 3.5:** última linha 3.x. Exige Java 17 e Jakarta EE 9 ou superior. Permite isolar a passagem 2 → 3 sem os efeitos do Boot 4, como Jackson 3 e as novas versões base.
- **Uma origem e um destino:** simplifica o desenho experimental. A etapa Boot 4 do plano anterior deixa de ser necessária.

## 5. Decisões em aberto

As decisões abaixo precisam do autor e passam por uma nova baseline com revisão. As propostas para os itens 2 e 4 estão em [DECISOES.md](DECISOES.md), seções P1 e P2.

1. ~~Mudar a origem para Java 8.~~ Decidido em D1: Java 8 e Boot 2.1.18, com reconstrução da aplicação nessa stack.
2. Quais itens do catálogo da seção 2 entram na aplicação. O AGENTS.md proíbe "falhas deliberadas, pistas". Seria preciso registrar a diferença entre usar uma API legada correta para a época (aceitável) e plantar um defeito (não aceitável). Cada item precisa ter comportamento coberto por teste, para que uma migração errada falhe.
3. Se a barra final entra na especificação e nos testes.
4. Tratar as tentativas claude-01 e codex-03 como piloto do protocolo, já que foram feitas com a origem atual.
