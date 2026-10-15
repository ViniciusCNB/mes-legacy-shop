# Candidato a baseline inicial

## Identidade e estado

Código da aplicação candidato: commit 4979132ee7fb6f0f589104abc999ed3f189dee82 (Fase 4). A consolidação não modifica src/, pom.xml, Compose local ou as regras HTTP. O build do artefato e sua avaliação estão em STATUS.md; o SHA256 identifica o JAR efetivamente executado, sem prometer JARs idênticos byte a byte em builds distintos.

O snapshot inicial do avaliador é 1.0.0, registrado em evaluation/SNAPSHOT.sha256 juntamente com a especificação. O autor ainda precisa revisar esta entrega e fazer seu commit completo da Fase 5. Esse novo hash deve identificar a baseline consolidada antes de qualquer tag baseline-boot-2.7. Nenhum commit/tag, pacote de entrega ou experimento foi criado nesta tarefa.

## Roteiro de revisão e reprodução

1. Conferir diff, especificação, decisões e matriz de cenários em evaluation/README.md. Confirmar o snapshot.
2. Selecionar JDK 17, conferir Wrapper/Maven 3.9.11 e Docker, executar clean verify na raiz.
3. Executar evaluation/run.ps1 ou evaluation/run.sh. Cada execução cria banco novo com SQL/fixtures conhecidos, inicia o JAR e roda o avaliador externo.
4. Conferir relatórios internos/externos, logs de inicialização, hash do JAR, estado final e limpeza do ambiente descartável. Preservar logs fora de target antes de novo clean.
5. Executar java tools/SourceMetrics.java e conferir cobertura JaCoCo da aplicação. Linhas físicas incluem comentários; não são meta de tamanho ou SLOC.
6. O autor revisa os fluxos principais e registra seu commit candidato completo; somente depois considerar a tag. Os resultados do gerador não substituem revisão humana.

Não é necessário apagar o banco local para reproduzir a avaliação: seus recursos são separados. A preparação do avaliador aplica SQL de esquema pelo entrypoint do PostgreSQL novo e fixtures explicitamente; a aplicação continua validate e não cria tabelas.

## Insumos e separação da avaliação

- Aplicação: Java 17, Boot 2.7.18, Maven 3.9.11, versões gerenciadas/fixadas no POM e PostgreSQL 16.10 no digest documentado.
- Contrato observável: docs/ESPECIFICACAO.md, JSON, erros, perfis/CSRF, paginação e período ali definidos.
- Dados: esquema src/main/resources/db/schema/V001–V004; demonstração separada em db/demo; fixtures externas em evaluation/fixtures/seed.sql.
- Testes internos: PostgreSQL real, constraints, validação do esquema e concorrência determinística, além dos comportamentos HTTP.
- Avaliador externo: Maven próprio, JUnit/HTTP Java 17/JSON; sem acesso às classes ou ao banco. O hash do snapshot é candidato ao congelamento após revisão.
- Evidências: logs/relatórios, identidade do JAR, fonte/estado de trabalho, versões, métricas e resultados reais registrados na entrega.

O avaliador externo, suas fixtures/roteiros, snapshots e resultados não devem fazer parte do pacote visível ao agente de migração. .git, caches, .env real, target, credenciais pessoais e patches de referência também não pertencem ao pacote. Testes internos e instruções necessárias de build/execução devem permanecer acessíveis.

A lista exata de arquivos e a redação do README/AGENTS do pacote serão definidas no protocolo experimental; não aplicar exclusões cegamente enquanto esses documentos ainda tiverem referências a arquivos de planejamento. Nenhum pacote foi montado ou entregue ao Ark.

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

Ainda são decisões de protocolo: versões alvo concretas, Ark/modelo/prompt, orçamento/tempo/iterações, repetições, assistência humana, insumos disponíveis ao agente, arquivamento de evidências e eventuais comparações. Não foram fixadas nem executadas nesta fase. Cada execução futura deverá começar da mesma baseline limpa e distinguir migração autônoma de assistência. A segunda etapa depende de intermediário validado.
