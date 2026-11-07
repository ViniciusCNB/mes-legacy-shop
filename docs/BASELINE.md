# Candidato a baseline inicial

## Identidade e estado

Código da aplicação candidato: commit 4979132ee7fb6f0f589104abc999ed3f189dee82 (Fase 4). A consolidação não modifica src/, pom.xml, Compose local ou as regras HTTP. O build do artefato e sua avaliação estão em STATUS.md; o SHA256 identifica o JAR efetivamente executado, sem prometer JARs idênticos byte a byte em builds distintos.

O snapshot inicial do avaliador é 1.0.0, registrado em evaluation/SNAPSHOT.sha256 juntamente com a especificação. O autor registrou a consolidação da Fase 5 em 1e8ea40939d92023df8dab81b64506c8f83a9ca6. Esse é o ponto de partida do pacote experimental; a revisão formal e eventual tag baseline-boot-2.7 continuam com o autor. Nenhuma migração foi iniciada.

## Roteiro de revisão e reprodução

1. Conferir diff, especificação, decisões e matriz de cenários em evaluation/README.md. Confirmar o snapshot.
2. Selecionar JDK 17, conferir Wrapper/Maven 3.9.11 e Docker, executar clean verify na raiz.
3. Executar evaluation/run.ps1 ou evaluation/run.sh. Cada execução cria banco novo com SQL/fixtures conhecidos, inicia o JAR e roda o avaliador externo.
4. Conferir relatórios internos/externos, logs de inicialização, hash do JAR, estado final e limpeza do ambiente descartável. Preservar logs fora de target antes de novo clean.
5. Executar java tools/SourceMetrics.java e conferir cobertura JaCoCo da aplicação. Linhas físicas incluem comentários; não são meta de tamanho ou SLOC.
6. O autor revisa os fluxos principais no commit candidato completo; somente depois considerar a tag. Os resultados do gerador não substituem revisão humana.

Não é necessário apagar o banco local para reproduzir a avaliação: seus recursos são separados. A preparação do avaliador aplica SQL de esquema pelo entrypoint do PostgreSQL novo e fixtures explicitamente; a aplicação continua validate e não cria tabelas.

## Insumos e separação da avaliação

- Aplicação: Java 17, Boot 2.7.18, Maven 3.9.11, versões gerenciadas/fixadas no POM e PostgreSQL 16.10 no digest documentado.
- Contrato observável: docs/ESPECIFICACAO.md, JSON, erros, perfis/CSRF, paginação e período ali definidos.
- Dados: esquema src/main/resources/db/schema/V001–V004; demonstração separada em db/demo; fixtures externas em evaluation/fixtures/seed.sql.
- Testes internos: PostgreSQL real, constraints, validação do esquema e concorrência determinística, além dos comportamentos HTTP.
- Avaliador externo: Maven próprio, JUnit/HTTP Java 17/JSON; sem acesso às classes ou ao banco. O hash do snapshot é candidato ao congelamento após revisão.
- Evidências: logs/relatórios, identidade do JAR, fonte/estado de trabalho, versões, métricas e resultados reais registrados na entrega.

O avaliador externo, suas fixtures/roteiros, snapshots e resultados não devem fazer parte do pacote visível ao agente de migração. .git, caches, .env real, target, credenciais pessoais e patches de referência também não pertencem ao pacote. Testes internos e instruções necessárias de build/execução devem permanecer acessíveis.

O [protocolo da primeira etapa](EXPERIMENTO.md) define o pacote com lista explícita, README/AGENTS próprios e exclusão do avaliador. O pacote foi preparado para a tentativa com Codex; ainda não foi entregue ao Ark.

## Métricas físicas

Ferramenta versionada: tools/SourceMetrics.java, executada pelo JDK 17 na raiz. Conta linhas físicas totais/não vazias, com comentários, sem ler .env, .git, target, caches ou logs. O conjunto de caminhos/grupos é explícito no programa. Java de produção, testes internos e avaliador são separados de configuração/infraestrutura e documentação. O próprio contador fica em infraestrutura.

| Grupo | Arquivos | Linhas físicas | Não vazias |
| --- | ---: | ---: | ---: |
| Produção Java | 42 | 1565 | 1294 |
| Testes internos Java | 5 | 1126 | 1019 |
| Testes externos Java | 2 | 473 | 429 |
| Configuração/infraestrutura | 27 | 1164 | 1110 |
| Documentação | 9 | 955 | 695 |

Cobertura de instruções/linhas/ramos é outra medida, produzida pelo JaCoCo e registrada em STATUS.md. Não acrescentar código ou testes apenas para atingir aproximadamente 3 mil linhas.

## Antes de autorizar o experimento

O [protocolo mes-boot35-v1](EXPERIMENTO.md) fixa o alvo 3.5.16, a primeira condição Codex/GPT‑6.1 Sol Alto, o pacote e a política de execução. A revisão/configuração do Ark deve ser definida antes da sua tentativa. A etapa de Boot 4 depende de um intermediário validado.
