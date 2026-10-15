# Status

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
