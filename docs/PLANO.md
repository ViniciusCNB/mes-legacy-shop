# Plano por fases

| Fase | Escopo | Critério de conclusão |
| --- | --- | --- |
| 1 Fundação | Boot 2.7.18/Java 17, Wrapper, Compose, configuração, saúde, SQL inicial e documentação | Build e inicialização verificáveis, teste com PostgreSQL e limitações registradas |
| 2 Catálogo | Product, SQL, dados locais, catálogo público e validações | Persistência e consultas verificadas, inativos ocultos; sem administração |
| 3 Segurança e administração | AppUser, BCrypt, HTTP Basic, perfis, produtos/estoque administrativos, /api/me | Autenticação, autorização e ausência de hashes/senhas nas respostas verificadas; política CSRF/sessão documentada |
| 4 Pedidos | Criação, isolamento, preço contratado, cancelamento, filtros e resumo | Atomicidade, concorrência e fluxos críticos verificados com PostgreSQL |
| 5 Consolidação | evaluation/ independente, roteiro, métricas e documentação completa | Banco novo reproduzível, avaliador passando, revisão do autor e candidato a baseline identificado |

O estado efetivamente observado fica em STATUS.md. A conclusão de uma fase não autoriza iniciar a seguinte.

## Aceitação e revisão da aplicação completa

Stack inicial preservada; ambiente reconstruível; comportamentos da ESPECIFICACAO passando em banco novo; contratos/dados/versões conhecidos; revisão do autor; commit candidato identificado. Somente depois da revisão considerar a tag baseline-boot-2.7. Nenhum commit ou tag é feito nesta tarefa.

## Experimento posterior (planejamento)

Após validar a aplicação, fixar versões concretas de Boot 3.5.x e 4.0.x, Ark/modelo/prompt, ferramentas, limites e assistência permitida. Começar cada repetição da mesma baseline limpa. Registrar build, testes, inicialização, HTTP, persistência, alterações, tokens e custo. Identificar intervenções humanas; a segunda etapa depende de uma versão intermediária válida e não deve ser atribuída integralmente ao Ark quando houver correção assistida.

## Andamento em 15/10/2026

- Fase 1 concluída e commitada pelo autor em 502c36c.
- Fase 2 concluída e commitada pelo autor em f452ab5; PostgreSQL local permanece em 5432.
- Fase 3 concluída e commitada pelo autor em 6778cd2.
- Fase 4 concluída e commitada pelo autor em 4979132.
- Fase 5: consolidação concluída e commitada pelo autor em 1e8ea40. Resultados reais em STATUS.md.
- Nenhum experimento Ark ou migração de framework é autorizado pelas cinco fases de construção.

## Preparação experimental em 07/11/2026

Primeira comparação: Boot 2.7.18 → 3.5.16, começando com Codex e GPT‑6.1 Sol Alto. O [protocolo mes-boot35-v1](EXPERIMENTO.md) define o pacote, a avaliação independente e as regras de execução. A tentativa acontecerá em tarefa separada; não foi iniciada nesta preparação.

## Reconstrução da origem em Java 8 / Boot 2.1 (plano, 11/11/2026)

Plano para revisão do autor; nenhuma etapa foi iniciada. Base: decisão D1 e critério P1 (DECISOES.md). O contrato da especificação e o avaliador externo não mudam de comportamento. A migração avaliada inclui a suíte de testes interna, não só o código de produção.

### Testes como parte da migração

A suíte atual tem 43 métodos de teste, 16 deles parametrizados, que geram 123 casos em JUnit 5. Na nova origem, a suíte é reescrita no idioma de 2018, caso a caso, sem reduzir cenários ou assertions. Assim, migrar os testes passa a ser uma parte real do trabalho do agente:

| Idioma da origem (JUnit 4 / Boot 2.1) | O que a migração para Boot 3.5 exige |
| --- | --- |
| `@RunWith(SpringRunner.class)`, `@Before`, `@BeforeClass`, `org.junit.Test` | JUnit 5 (`@BeforeEach`, `@BeforeAll`, `org.junit.jupiter`); o starter de testes do Boot 3 não traz o motor do JUnit 4 |
| Casos parametrizados com `@RunWith(Parameterized.class)` mais `SpringClassRule` e `SpringMethodRule` | `@ParameterizedTest` com `@MethodSource` ou `@CsvSource` |
| `@Test(expected = ...)` e a regra `ExpectedException` | `assertThrows` |
| Contêiner do Testcontainers como `@ClassRule` | `@Testcontainers` e `@Container` |
| Propriedades do banco via `ApplicationContextInitializer` e `TestPropertyValues` | `@DynamicPropertySource` (ou `@ServiceConnection`) |
| `org.springframework.boot.web.server.LocalServerPort` | Pacote movido para `org.springframework.boot.test.web.server` |
| `TestRestTemplate` e `RestTemplate` com Apache HttpClient 4 (cookies CSRF via `BasicCookieStore`) | O Boot 3 só integra o HttpClient 5 (`org.apache.hc.client5`) |

### Evidência de que a migração modernizou código e testes

1. **Avaliador externo inalterado.** Os 26 testes HTTP rodam contra o JAR de origem e o de destino. Ele detecta regressão de comportamento mesmo que a suíte interna tenha sido enfraquecida.
2. **Inventário da suíte.** Uma ferramenta em `tools/` lista classe, método, casos executados, assertions por método e testes ignorados, antes e depois. A correspondência 1:1 mostra que nenhum cenário foi removido, ignorado ou teve assertions reduzidas.
3. **Varredura de marcadores legados.** Uma ferramenta em `tools/` conta, por arquivo, os marcadores da tabela de caracterização (`javax.persistence`, `WebSecurityConfigurerAdapter`, `org.junit.Test`, `SpringRunner`, `org.apache.http`, entre outros). Na origem, a contagem prova a presença do legado; no destino, a contagem zero prova a modernização, e eventuais resíduos ficam visíveis.
4. **Opcional: teste de mutação (PIT)** em classes centrais, como `OrderService`, na origem e no destino. Uma pontuação de mutação equivalente indica que os testes migrados mantiveram a capacidade de detectar defeitos. O custo é alto com Testcontainers, e a compatibilidade precisa ser validada antes de ser adotado.

### Etapas

| Etapa | Conteúdo | Critério de saída |
| --- | --- | --- |
| R1 | Preservar a origem do piloto (tag do autor no commit atual) e trabalhar numa branch própria | Piloto recuperável |
| R2 | POM em Boot 2.1.18 e Java 8; enforcer `[1.8,1.9)`; JaCoCo; Testcontainers 1.21.4 por BOM; HttpClient 4; atualizar AGENTS.md e README.md | Build vazio passa em JDK 8 |
| R3 | Código de produção por funcionalidade (fundação, catálogo, segurança, pedidos) no idioma de 2018, conforme P1 | Compila; esquema SQL inalterado |
| R4 | Suíte interna reescrita em JUnit 4, caso a caso, a partir da atual | `clean verify` com o mesmo conjunto de cenários; inventário 1:1 |
| R5 | Avaliador externo e roteiro de sondagem contra o JAR 2.1 | 26/26; diferenças de sondagem documentadas |
| R6 | Tabela de caracterização (P1), inventário da suíte, varredura de marcadores e métricas | Tabela com fonte e teste para cada item |
| R7 | Nova versão da especificação e do snapshot do avaliador; protocolo v2, pacote e prompt. Decidir neste momento: se o prompt exige JUnit 5 explicitamente, se o teste de mutação será adotado e se a barra final entra na especificação | Revisados pelo autor antes de qualquer tentativa |

Depois do R7: tentativas com Codex e Claude Code, com esforço Médio, nas mesmas condições.
