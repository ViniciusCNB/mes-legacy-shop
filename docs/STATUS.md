# Status

Atualização: 15/10/2026, America/Sao_Paulo. **Fase 1 — Fundação implementada e verificada no Windows.** Nenhuma Fase 2 iniciada. Revisão do autor e reprodução nativa em Linux pendentes. Nenhum commit, staging ou tag feito pelo Codex; HEAD permanece b093498 (Initial commit).

## Entregue

- Projeto Maven br.ufmg.mes:mes-legacy-shop:0.0.1-SNAPSHOT, Boot 2.7.18 e Java 17; ponto de entrada ShopApplication.
- Wrapper oficial 3.3.2 only-script para Windows/POSIX, Maven 3.9.11 e SHA256 fixos; Enforcer 3.4.1 para verificar JDK/Maven.
- Web, Actuator, JPA, validação, JDBC PostgreSQL e teste gerenciados pelo Boot. BOM Testcontainers 1.19.8 adicional fixado; JaCoCo 0.8.11; Failsafe/Surefire 2.22.2 do parent.
- Compose PostgreSQL 16.10-bookworm com digest fixo, volume, healthcheck e porta local 15432. .env.example e import nativo do .env no perfil local. .env real ignorado.
- GET /actuator/health sem componentes/detalhes; apenas saúde exposta. Conexão participa do resultado.
- SQL V001 cria somente esquema shop. Caminhos separados para esquema e futuras cargas locais. Hibernate validate, SQL automático da aplicação desligado, open-in-view=false e UTC JDBC.
- FoundationIT com PostgreSQL real descartável, aplicação em porta aleatória e assertions de versão, esquema e HTTP. Sem H2, mocks de conexão ou skip por ausência de Docker.
- AGENTS, README, contexto, especificação, plano, decisões e este registro. README inicial continha somente o título; foi expandido preservando-o. Não havia outra base ou instrução existente nos caminhos inspecionados.

## Ambiente e versões observadas

Windows 11 amd64; Microsoft OpenJDK 17.0.15+6-LTS selecionado explicitamente em JAVA_HOME (`C:\Users\vinic\.jdks\ms-17.0.15`). Não foi alterado o Java global da máquina. Maven 3.9.11; Docker Engine 27.5.1 (API 1.47, mínimo 1.24), Docker Desktop 4.38.0, Compose 2.32.4-desktop.1; daemon Linux amd64.

Imagem: `postgres:16.10-bookworm@sha256:38471f330eb885e04de130b768d6db4e10469e2311879c7e5c699f6d2d8a1c74`. Consulta real retornou PostgreSQL 16.10 (Debian 16.10-1.pgdg12+1), esquema shop. No Testcontainers, a referência equivalente usa postgres@sha256 com o mesmo digest.

Grafo resolvido por dependency:tree: Spring Boot 2.7.18; Hibernate 5.6.15.Final; JDBC PostgreSQL 42.3.8; JUnit Jupiter 5.8.2; módulos Testcontainers 1.19.8. A combinação compilou e executou o teste real. O arquivo completo filtrado está em target/dependency-tree.txt, gerado e não versionado.

## Comandos efetivamente executados e resultados

Os comandos Maven abaixo usaram JAVA_HOME com JDK 17. Os comandos completos de reprodução Windows/Linux estão no README.

| Comando/verificação | Resultado observado |
| --- | --- |
| Leitura integral do documento do autor; inspeção de arquivos, Git e AGENTS ancestrais | Documento acessível; repositório com somente README, Git limpo e commit inicial; sem instruções adicionais encontradas |
| java -version com JDK selecionado; .\mvnw.cmd -v | Java 17.0.15; Maven 3.9.11 |
| Plugin org.apache.maven.plugins:maven-wrapper-plugin:3.3.2:wrapper com '-Dmaven=3.9.11' '-Dtype=only-script' | Geração oficial concluída com BUILD SUCCESS |
| Download Maven 3.9.11 ZIP e comparação com SHA512 oficial | Conferência aprovada; SHA256 calculado 0d7125e8c91097b36edb990ea5934e6c68b4440eef4ea96510a0f6815e7eeadb, fixado no Wrapper |
| docker version; docker compose version | Cliente/daemon acessíveis fora da restrição de sandbox |
| docker pull postgres:16.10-bookworm | Download concluído, digest registrado; sem latest |
| docker compose config --quiet | Configuração final válida, saída zero |
| docker compose up -d --wait; docker compose ps | Saudável em 127.0.0.1:15432; V001 aplicado pelo entrypoint em volume inicialmente novo |
| SQL via pipe para docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' | SELECT de versão e information_schema confirmou PostgreSQL 16.10 e esquema shop; comando usa variáveis do contêiner |
| .\mvnw.cmd -B -ntp clean verify (configuração final) | **BUILD SUCCESS**, finalizado 17:10:01 -03:00, duração 1min25s; 1 teste, 0 falhas, 0 erros, 0 ignorados |
| .\mvnw.cmd -B -ntp dependency:tree '-Dincludes=org.springframework.boot:*,org.hibernate:hibernate-core,org.postgresql:postgresql,org.junit.jupiter:*,org.testcontainers:*' '-DoutputFile=target/dependency-tree.txt' | BUILD SUCCESS; versões resolvidas registradas acima |
| .\mvnw.cmd -B -ntp spring-boot:run; curl.exe --fail http://127.0.0.1:8080/actuator/health | Aplicação iniciou no perfil local usando .env; HTTP 200, {"status":"UP"} |
| GET /actuator/env na aplicação local | HTTP 404; não expõe configuração |
| java -jar target/mes-legacy-shop-0.0.1-SNAPSHOT.jar | JAR executável iniciou conectado ao banco Compose; HTTP 200/UP |
| docker compose stop postgres; GET /actuator/health no JAR | HTTP 503, {"status":"DOWN"}, sem detalhes |
| docker compose up -d --wait; GET /actuator/health no mesmo JAR | Banco saudável; HTTP 200, {"status":"UP"} novamente |
| Git Bash: bash -n mvnw; bash ./mvnw -v | Sintaxe aprovada e execução com saída zero; Maven 3.9.11 / JDK 17.0.15. Isso não é execução nativa Linux |
| git diff --check; git diff --cached --name-only; git check-ignore .env target/site/jacoco/index.html | Sem erro de whitespace no diff rastreado; index vazio; .env e target ignorados |

O teste integrou Hibernate e JDBC ao PostgreSQL 16 real e verificou o servidor HTTP com perfil test. A base Compose e o banco do teste são independentes. Contêineres do teste foram descartados pelo Testcontainers/Ryuk. Ao terminar a verificação, a aplicação foi encerrada e o serviço Compose foi parado, preservando o volume e o .env local. Para retomar: docker compose up -d --wait.

### Cobertura medida

JaCoCo em target/site/jacoco: ShopApplication, 1/3 linhas (**33,33%**), 3/8 instruções (**37,50%**), 1/2 métodos (**50%**), sem branches. Só existe a classe de bootstrap; o main não é chamado pelo teste SpringBootTest. Não foram criados testes de baixo valor para aumentar esses números. Esses valores não representam cobertura de regras do e-commerce, ainda inexistentes.

## Falhas encontradas e resolução

1. Dentro do sandbox, rede não resolvia repo.maven.apache.org e o named pipe dockerDesktopLinuxEngine retornava Access is denied. Execuções com acesso ampliado foram autorizadas e funcionaram. Não há impedimento remanescente de download/Docker nesta máquina; não houve substituição de tecnologia.
2. Primeira geração do Wrapper com argumento -Dmaven sem aspas no PowerShell terminou com Unknown lifecycle phase .9.11. Reexecutada com argumentos entre aspas, com sucesso. O projeto final fixa 3.9.11.
3. Primeiro Compose tentou 5432 e falhou por porta ocupada. Nenhum processo externo foi alterado. .env.example, fallback da conexão e Compose passaram a usar 15432; inicialização e SQL confirmados.
4. Uma consulta SQL inicialmente falhou por aspas aninhadas no shell; corrigida usando SQL via pipe. O comando real de psql com variáveis do contêiner foi validado e consta no README.
5. O primeiro clean verify com a tag fixa passou. Ao acrescentar o digest, houve dois builds com 1 erro/0 ignorados: o inicial e uma repetição diagnóstica com stack trace completo. Testcontainers 1.19.8 interpretava tag@digest como nome de repositório incompatível. A referência no teste foi corrigida para postgres@sha256, sem substituir a imagem ou declarar compatibilidade artificial. O clean verify final passou mantendo todas as assertions.
6. Failsafe 2.22.2 produziu dump não bloqueante de PpidChecker (Cannot use PPID process information, fallback interno NOOP) neste ambiente Windows. Não é erro de teste; o resumo final confirma 1 execução e zero falhas/erros/ignorados. A configuração de verificação não foi desativada. Reavaliar somente se afetar uma execução posterior.

Logs de desenvolvimento em `C:\Users\vinic\AppData\Local\Temp`: mes-foundation-verify.log (primeiro sucesso), mes-foundation-final-verify.log e mes-foundation-diagnostic.log (falhas ao acrescentar digest), mes-foundation-accepted-verify.log (sucesso final), mes-foundation-run.log e mes-foundation-jar.log (execuções). Não versionados. Relatórios finais em target/failsafe-reports e target/site/jacoco; clean os remove.

## Limitações e próximo passo

- Linux nativo não foi executado. Há Wrapper oficial POSIX, finais de linha LF, comandos com sh e sintaxe/execução verificadas no Git Bash; confirmar o fluxo completo em Linux antes do experimento.
- Hibernate validate está configurado, mas sem entidades não demonstra compatibilidade de tabelas, queries de domínio, transações ou concorrência. A Fase 2 deverá testar o esquema Product real e a rejeição de divergência de mapeamento.
- Ainda não há dados de demonstração, segurança ou regras do e-commerce. O teste de saúde não é avaliador comportamental do domínio. Protocolo e baseline final não definidos.
- A ausência de Docker não foi simulada nesta tarefa; @Testcontainers mantém o padrão de falhar quando não há daemon, sem disabledWithoutDocker ou perfil de opt-in.
- A revisão humana solicitada pela referência continua pendente; a verificação do Codex não substitui a revisão do autor.

**Próximo passo autorizado apenas em nova solicitação: Fase 2 — Catálogo.** Criar Product, próximo SQL versionado, dados locais e consultas públicas com paginação/validação, verificando ativos/inativos e persistência em PostgreSQL; sem administração ou pedidos.

## Commit sugerido para o autor

Um commit atômico para esta fase: **feat: implementa fundação com Boot 2.7.18 e PostgreSQL 16**.

Arquivos a incluir (todos os 22 arquivos modificados/novos desta tarefa):

```text
.env.example
.gitattributes
.gitignore
.mvn/wrapper/maven-wrapper.properties
AGENTS.md
README.md
compose.yaml
mvnw
mvnw.cmd
pom.xml
docs/CONTEXTO_TP.md
docs/DECISOES.md
docs/ESPECIFICACAO.md
docs/PLANO.md
docs/STATUS.md
src/main/java/br/ufmg/mes/shop/ShopApplication.java
src/main/resources/application.yml
src/main/resources/application-local.yml
src/main/resources/db/schema/V001__foundation.sql
src/main/resources/db/demo/README.md
src/test/java/br/ufmg/mes/shop/FoundationIT.java
src/test/resources/logback-test.xml
```

Não incluir .env, target, caches ou logs. Não há commit nem arquivo staged pelo Codex. O manifesto acima é sugestão para revisão e execução pelo autor, não uma instrução de commit automático.
