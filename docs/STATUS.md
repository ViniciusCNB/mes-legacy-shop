# Status

## Tentativa codex-03 — 10/11/2026

Em 10/11/2026, a tentativa `codex-03-55119509` concluiu a migração de Spring Boot 2.7.18 para **3.5.16** sob o protocolo `mes-boot35-v1` com as emendas v2 e v3. A tarefa nova usou **GPT‑6.1 Sol / Médio** e Acesso total desde o primeiro turno. A execução levou **5min44s**, de 10:09:14 a 10:14:58 (UTC−03:00). A mensagem manteve o texto integral do `prompt.txt`, com um LF adicional no fim.

| Verificação | Resultado |
| --- | --- |
| Baseline em cópia separada | 123 testes aprovados com JDK 17.0.15 e PostgreSQL 16.10. |
| Docker Desktop 4.38.0 | Resource Saver desligado. O motor Linux respondeu após 6min03s sem início de contêineres e permaneceu acessível durante a tentativa e as verificações. |
| `clean verify` na saída do agente | 124 testes aprovados. Os 123 casos originais foram preservados. |
| `clean verify` em cópia independente | 124 testes aprovados, com zero falhas, erros ou skips. |
| Avaliador congelado 1.0.0 contra o JAR original | 26 testes HTTP aprovados, com zero falhas, erros ou skips. Os nove insumos do snapshot foram conferidos. |
| `clean verify` no worktree da branch | 124 testes aprovados, com zero falhas, erros ou skips. |

O JAR original avaliado tem SHA256 `f68120a7ef9e853985575d91842cce1531903ca323134445ed13c85a9d1883b6`. O avaliador foi executado com `evaluation/run.ps1` no PowerShell 7.6.9 disponibilizado pelo runtime do Codex. O checkout do avaliador era `3ebbcd8` e estava limpo durante a execução.

A branch `experimento/codex-boot-3.5.16` parte de `1e8ea40`, com um commit (0e40b50), e foi publicada no GitHub. O worktree em `C:/Users/vinic/mes-experimentos/mes-legacy-shop-codex-boot-3.5.16` contém os 22 arquivos modificados pelo agente, todos com base byte a byte igual à do pacote. O build emite aviso de depreciação para `AntPathRequestMatcher`.

O [registro local da tentativa](../.experiment-local/records/codex-03-55119509/RESULTADO.md) reúne transcrição, logs, diff, árvore, JAR, relatórios, intervenções e contagem de tokens. As tentativas Codex 01 e 02 continuam registradas como bloqueios de infraestrutura.

Conferência do operador Claude Code (10/11/2026): o SHA256 do JAR confere; avaliador congelado executado com evaluation/run.sh contra agent-original.jar, com 26 testes e 0 falhas/erros/skips, e aplicação em Spring Boot v3.5.16.

## Tentativa claude-01 — 10/11/2026

**Sucesso sob as regras.** Claude Code + Opus 5.5 (Médio) migrou o pacote congelado de Spring Boot 2.7.18 para **3.5.16** em cerca de 4min25s (00:05:22–00:09:47, −03:00), sem intervenções. A condição foi registrada antes do envio na [emenda v2 do protocolo](EXPERIMENTO.md).

| Verificação | Resultado |
| --- | --- |
| Ambiente: Resource Saver desligado; baseline em cópia separada; Docker ocioso por 7 min | 123 testes OK; o motor não pausou e executou PostgreSQL 16.10 depois do período ocioso |
| Agente: clean verify | 123 testes, 0 falhas/erros/skips |
| Operador: clean verify em cópia independente da saída | 123 testes, 0 falhas/erros/skips; Spring Boot v3.5.16 |
| Avaliador externo 1.0.0 contra o JAR do agente (SHA256 8e466963…30d6) | **26 testes, 0 falhas/erros/skips** |

- O diff tem 20 arquivos alterados, nenhum adicionado ou removido: POM, javax → jakarta e Security 6 (requestMatchers e handler CSRF sem máscara). No teste, mudou só um import.
- Causa das falhas do Codex: o Resource Saver do Docker Desktop 4.38 pausou a VM e ela não retomou. Ele foi desligado para as tentativas seguintes.
- O run.ps1 do avaliador não roda no Windows PowerShell 5.1 (NativeCommandError em java -version) e não há PowerShell 7 instalado. A avaliação usou o run.sh do mesmo snapshot, que não foi alterado.
- Evidências: .experiment-local/records/claude-01-20261109-2350/ (RESULTADO.md, transcrição, saída, diff, JAR, logs).
- Branch experimento/claude-boot-3.5.16, a partir de 1e8ea40, com um commit (a547494) contendo os 20 arquivos da saída, conferidos contra a base, e publicada no GitHub. Na branch, `.\mvnw.cmd -B -ntp clean verify` deu 123 testes, 0 falhas/erros/skips, Spring Boot v3.5.16. README, AGENTS.md e docs/ da branch continuam descrevendo a baseline 2.7.18.
- Pendência: fixar a configuração do Ark.

## Preparação experimental — 07/11/2026

**Pronta para a primeira tentativa em tarefa nova.** Alvo Spring Boot **3.5.16**, a partir de **2.7.18** no commit **1e8ea40939d92023df8dab81b64506c8f83a9ca6**. A primeira condição é Codex desktop + **GPT‑6.1 Sol (Alto)**. A preparação não executou uma migração, não iniciou o Ark, não criou tarefa nova, commit ou tag.

- [Protocolo mes-boot35-v1](EXPERIMENTO.md): uma tentativa exploratória por condição, até 45 minutos por tentativa, mesma entrada e avaliação, intervenções registradas. Revisão/configuração do Ark ficará para a preparação da segunda condição.
- ZIP: .experiment-local/input/mes-legacy-shop-boot-3.5.16-input.zip. Manifesto JSON ao lado, fora do ZIP. Gerador: experiments/New-MigrationInput.ps1.
- SHA256 do ZIP: 77a806f21c689fb493b0ae85d02f60c4c7af549c0a888fd7c93a07e8b0b1ebcf.
- SHA256 do prompt.txt: 278c5cb64daeb607d06136db420f58a597f69dd33608ee59c67df6304c70e807.
- Pacote com 71 arquivos. Código, SQL, testes, Wrapper e POM vêm do commit fixado; contrato público e instruções foram preparados para a tarefa. Avaliador externo, .git, .env real e resultados ficam fora.
- O repositório original continua em Boot 2.7.18; src/, POM, Compose, AGENTS.md original e os insumos do snapshot do avaliador não foram alterados.

### Verificações reais

| Verificação | Resultado |
| --- | --- |
| Duas gerações do ZIP e tentativa de sobrescrita | Mesmo SHA256 nas duas gerações; saída existente recusada. |
| Extração independente no diretório temporário | 71 arquivos, hashes conferidos, entradas restritas à lista explícita; arquivos originais conferidos com os blobs do commit considerando CRLF do Wrapper Windows. |
| Integridade do avaliador | SHA256 do snapshot e nove insumos conferidos; versão 1.0.0 inalterada. |
| .\mvnw.cmd -B -ntp clean verify na cópia isolada | **123 testes, 0 falhas, 0 erros, 0 skips**, BUILD SUCCESS, 2:06. |
| .\evaluation\run.ps1 -ApplicationJar com o JAR dessa cópia | **26 testes, 0 falhas, 0 erros, 0 skips**, BUILD SUCCESS, 24,181s; aplicação Boot 2.7.18 iniciada e testada em PostgreSQL descartável. |

JDK Microsoft 17.0.15, Docker Engine 27.5.1, Compose 2.32.4-desktop.1. O Docker Desktop foi iniciado para as verificações. O contêiner e volume da avaliação externa foram removidos pelo roteiro; o volume local não foi reinicializado. O JAR avaliado nesta preparação tem SHA256 93997ec33be7be99a14adaef22035c1e30a26b28a6ea39b25e7a219c129a84e6.

Evidências locais: .experiment-local/preflight/mes-boot35-preflight-8197e240/ (input-check.json, build.log, relatórios Failsafe, external.log e arquivos da avaliação externa). Elas comprovam a baseline do pacote; não contam como resultado de migração.

### Próximo passo

Revisar o protocolo, extrair o ZIP em uma pasta nova fora do repositório, abrir essa pasta em uma tarefa nova do Codex desktop, selecionar GPT‑6.1 Sol / Alto e enviar o prompt.txt. Usar nova extração, não a cópia de validação com target/ preenchido.

Commit sugerido para o autor: **chore: prepara experimento de migração para Spring Boot 3.5**.

~~~text
.gitignore
README.md
docs/BASELINE.md
docs/CONTEXTO_TP.md
docs/EXPERIMENTO.md
docs/PLANO.md
docs/STATUS.md
experiments/New-MigrationInput.ps1
experiments/input/AGENTS.md
experiments/input/README.md
experiments/input/prompt.txt
~~~

Não incluir .experiment-local/, relatórios ou temporários. A revisão formal da baseline e eventual tag permanecem com o autor.

## Histórico — Fase 5 (15/10/2026)

Atualização: 15/10/2026, America/Sao_Paulo. **Implementação e verificações técnicas da Fase 5 — Consolidação concluídas.** A Fase 4 foi commitada pelo autor em 4979132; a pasta estava limpa ao iniciar. Revisão humana, commit completo e eventual tag permanecem com o autor. Nenhum commit, staging, tag, pacote para o Ark ou experimento foi feito.

## Entregue

- evaluation/ independente, versão 1.0.0, com POM próprio: sem parent Boot, módulo, Spring/JPA/JDBC/Testcontainers ou classes da aplicação. JUnit 5.8.2 e Jackson 2.13.5 fixados; cliente HTTP nativo Java 17 e dinheiro lido como BigDecimal.
- 26 execuções de testes externos: saúde/catálogo/paginação, autenticação/perfis/CSRF, administração/SKU/validações, cálculo/estoque/rollback, isolamento, preço histórico, cancelamento, concorrência, filtros/resumo e limites exatos de período.
- Roteiros PowerShell/Bash para JAR já construído: PostgreSQL novo no digest original, projeto/volume exclusivos, porta dinâmica de banco, HTTP 18080, fixture própria, espera de saúde, logs/relatórios/hashes e remoção somente dos recursos criados. Sem reset do banco local.
- evaluation/SNAPSHOT.sha256 com nove insumos do avaliador e da especificação; conferido nas duas shells. Snapshot candidato a revisão/congelamento experimental; não equivale a aprovação acadêmica.
- tools/SourceMetrics.java: contagem reproduzível de linhas físicas totais/não vazias, separando produção, testes internos/externos, infraestrutura e documentação.
- docs/BASELINE.md e evaluation/README.md com identidade candidata, roteiro de revisão, critérios/limites da avaliação e separação do pacote futuro.
- README, instruções, contexto, plano, especificação e decisões atualizados. .gitignore exclui evaluation/target; atributos fixam LF para scripts/snapshot entre plataformas.
- src/, POM raiz e Compose local permanecem idênticos à Fase 4. Mantidos Boot 2.7.18, Java 17, Maven Wrapper 3.9.11, PostgreSQL 16.10, SQL V001–V004 e comportamento HTTP.

## Identidade e ambiente

Código da aplicação candidato: **4979132ee7fb6f0f589104abc999ed3f189dee82**. O commit completo da consolidação ainda será feito pelo autor; não inventar esse hash nem marcar baseline-boot-2.7 antes da revisão.

JAR avaliado: target/mes-legacy-shop-0.0.1-SNAPSHOT.jar.
SHA256: **bd5c34599643742384990d1dd562248986b6e4b8fa157c757a1f7a37a4fbfb42**.

Snapshot do avaliador/especificação (SHA256 do arquivo evaluation/SNAPSHOT.sha256):
**d878ffcab48bc0067438a8d8c423fff25496b5c5094621192e470a5db9c1589b**.

Windows 11 amd64, Microsoft OpenJDK 17.0.15 em C:\Users\vinic\.jdks\ms-17.0.15, Maven 3.9.11, Docker Engine 27.5.1/API 1.47, Compose 2.32.4 e PostgreSQL 16.10 em Linux. Imagem postgres:16.10-bookworm@sha256:38471f330eb885e04de130b768d6db4e10469e2311879c7e5c699f6d2d8a1c74.

Os comandos de build/execução usaram explicitamente JDK 17; o Java padrão de uma shell Windows sem essa seleção é 21. O Ubuntu/WSL tem JDKs 11/21 e Docker acessível, mas não JDK 17.

## Comandos e resultados reais

Comandos executados na raiz; receitas completas em README.md e evaluation/README.md.

| Comando/verificação | Resultado real |
| --- | --- |
| git status --short / git log -5 --oneline | Pasta inicialmente limpa, HEAD 4979132. |
| .\mvnw.cmd -B -ntp clean verify com JAVA_HOME no JDK 17 | BUILD SUCCESS, **123 testes, 0 falhas/erros/skips**, 1:35, encerrado 15/10/2026 19:37:57 -03. |
| .\evaluation\run.ps1 — primeira execução | 26 testes passaram em banco novo; projeto mes-evaluation-ca0c2f9ea260, PostgreSQL na porta 49778; ambiente removido. |
| bash evaluation/run.sh — primeira execução em Git Bash | 26 testes passaram em outro banco novo; projeto mes-evaluation-1294e189ac82; ambiente removido. |
| .\evaluation\run.ps1 — roteiro final com snapshot | **26 testes, 0 falhas/erros/skips**, BUILD SUCCESS em 29,610s; projeto mes-evaluation-f8207d3d071f, PostgreSQL 58378, HTTP 18080; encerrado 19:44:27 -03. Relatórios arquivados e recursos removidos. |
| bash evaluation/run.sh — roteiro final em Git Bash | **26 testes, 0 falhas/erros/skips**, BUILD SUCCESS em 29,911s; projeto mes-evaluation-ebb34a314277, PostgreSQL 51129, HTTP 18080; encerrado 19:46:03 -03. Relatórios arquivados e recursos removidos. |
| Conferência run.json/run.txt das execuções finais | Mesmo commit candidato, JAR SHA256 e snapshot acima; sourceDirty=true identifica corretamente a consolidação ainda não commitada. |
| Inspeção do classpath real do relatório XML externo | Somente JUnit/JUnit Platform/auxiliares e Jackson; sem Spring, Hibernate ou artefato da loja. JUnit 5.8.2/Jackson Core/Annotations/Databind 2.13.5 confirmados em execução. |
| Conferência PowerShell do SNAPSHOT.sha256 / sha256sum -c evaluation/SNAPSHOT.sha256 | Todos os nove insumos conferem. |
| java tools/SourceMetrics.java pelo JDK 17 | CSV produzido; números e método abaixo. |
| wsl -d Ubuntu, inspeção de Java/Docker | JDK padrão 21.0.12.1; instalados 11/21; Docker 27.5.1 acessível. Sem JDK 17. |
| JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 bash evaluation/run.sh no WSL | **Rejeição esperada, exit 1**, mensagem JAVA_HOME deve selecionar JDK 17, antes de criar recursos. Não foi execução nativa aprovada. |
| git diff --name-only -- src pom.xml compose.yaml | Vazio: código, testes internos, SQL, POM e Compose da aplicação preservados. |
| git diff --check / conferência de staging | Sem problemas; nenhum arquivo staged pelo Codex. |

As primeiras execuções precederam a finalização dos roteiros/snapshot; as duas execuções finais conferem o mesmo snapshot e artefato. São **26 casos externos distintos**, repetidos em bancos novos, não 104 casos diferentes.

Evidências aprovadas locais, ignoradas no Git:
- target/evaluation-runs/mes-evaluation-f8207d3d071f/ — PowerShell: run.json, result.json, logs e surefire-reports.
- target/evaluation-runs/mes-evaluation-ebb34a314277/ — Bash: run.txt, result.txt, logs e surefire-reports.
- target/failsafe-reports/ e target/site/jacoco/ — suíte interna/cobertura.
- C:\Users\vinic\AppData\Local\Temp\mes-consolidation-verify.log — build interno completo.
- C:\Users\vinic\AppData\Local\Temp\mes-evaluation-final.log e mes-evaluation-bash-final.log — roteiros finais completos.

Ambos os resultados finais indicam sucesso; Bash registra também exit=0. Arquivar evidências fora de target antes de executar outro clean.

## Cobertura e métricas físicas

Suíte interna: FoundationIT 1, CatalogIT 32, SecurityAdminIT 41 e OrdersIT 49 = **123**. Suíte externa: ShopContractTest = **26**. Todas passaram sem skips. A integração interna comprova constraints/esquema e contenção determinística; o avaliador externo verifica somente comportamento observável por HTTP, com três disputas independentes pela última unidade e cancelamento simultâneo, sem retries.

JaCoCo da aplicação: **435/439 linhas (99,09%)**, **1936/1954 instruções (99,08%)**, **48/50 ramos (96,00%)**. Sem meta arbitrária; números preservados da Fase 4 porque o código/testes internos não mudaram.

Contagem pelo programa versionado, incluindo comentários em linhas não vazias. Não é SLOC. CSV local em target/source-metrics.csv; caminhos/grupos explícitos em tools/SourceMetrics.java:

| Grupo | Arquivos | Linhas físicas | Não vazias |
| --- | ---: | ---: | ---: |
| Produção Java | 42 | 1565 | 1294 |
| Testes internos Java | 5 | 1126 | 1019 |
| Testes externos Java | 2 | 473 | 429 |
| Configuração/infraestrutura | 27 | 1164 | 1110 |
| Documentação | 9 | 955 | 695 |

Nenhuma linha ou teste foi acrescentado para atingir tamanho/cobertura alvo.

## Diagnósticos e ajustes desta fase

- Os testes internos e externos não apresentaram falhas de comportamento nesta fase; não houve alteração de aplicação, contrato anterior ou relaxamento de assertions.
- Primeira checagem Bash dentro do sandbox recebeu erro MSYS NtCreateDirectoryObject/0xC0000022. A mesma ferramenta fora dessa restrição funcionou; os roteiros completos foram executados com acesso autorizado a Docker/rede.
- Surefire 2.22.2 avisou que useModulePath era parâmetro desconhecido; removido do POM independente, sem desativar testes. O aviso de JAR vazio refere-se ao projeto apenas de testes, não ao artefato da loja.
- A primeira execução PowerShell não copiava os relatórios para o diretório da execução por uma flag de arquivamento não ativada. Corrigido; a versão final copia os relatórios atuais e registra snapshot/JDK, com nova execução aprovada.
- PpidChecker/NOOP do Surefire/Failsafe no Windows é diagnóstico não fatal visível nos dumps. As suítes completaram e produziram os resultados acima.
- O build nativo Linux está impedido pela ausência de JDK 17 no WSL. O roteiro rejeitou 21 como esperado; não foi instalado JDK, alterado serviço ou trocada a tecnologia para contornar a limitação.

## Estado local, revisão e próximo passo

- Ambiente local permanece saudável em 127.0.0.1:5432, com 6 produtos, 3 usuários e os mesmos 2 pedidos CANCELLED anteriores; MUG-001 conserva estoque 10. Nenhuma carga do avaliador entrou nesse volume.
- Os quatro projetos descartáveis foram removidos; os JARs de avaliação foram encerrados. Logs/relatórios permanecem nos diretórios de execução.
- Revisão humana desta entrega, commit completo e eventual tag estão pendentes com o autor. 4979132 identifica o código da aplicação; o futuro commit da Fase 5 identificará a consolidação completa.
- Próximo passo: revisar especificação/avaliador/roteiros e métricas, registrar commit candidato, conferir snapshot e preparar protocolo experimental. Fixar posteriormente versões alvo, Ark/modelo/prompt, orçamento/repetições/assistência e conteúdo exato do pacote. Nenhuma migração foi iniciada.

## Commit sugerido para o autor

Um commit atômico: **test: adiciona avaliação independente e consolida baseline candidata**.

Arquivos desta alteração (20):

~~~text
.gitattributes
.gitignore
AGENTS.md
docs/BASELINE.md
docs/CONTEXTO_TP.md
docs/DECISOES.md
docs/ESPECIFICACAO.md
docs/PLANO.md
docs/STATUS.md
evaluation/compose.yaml
evaluation/fixtures/seed.sql
evaluation/pom.xml
evaluation/README.md
evaluation/run.ps1
evaluation/run.sh
evaluation/SNAPSHOT.sha256
evaluation/src/test/java/br/ufmg/mes/evaluation/ApiClient.java
evaluation/src/test/java/br/ufmg/mes/evaluation/ShopContractTest.java
README.md
tools/SourceMetrics.java
~~~

Não incluir .env, target, evaluation/target, logs, caches ou temporários. Nenhum staging/commit foi feito pelo Codex.
