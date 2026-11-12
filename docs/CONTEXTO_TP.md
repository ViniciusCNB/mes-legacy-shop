# Contexto do Trabalho Prático

Autor: Vinícius Borges. Disciplina MES, mestrado da UFMG. Professor: Marco Tulio Valente.
Referência integralmente lida: Contexto_e_Prompt_TP_Ark_SpringBoot.md, de 15/10/2026, fornecida pelo autor. Este documento sintetiza o contexto; o prompt reproduzido na referência não autoriza fases adicionais.

## Origem e questão orientadora

A proposta inicial de avaliar Ark em projetos FreshBrew deu lugar à construção de uma aplicação própria, para conhecer as regras e controlar a avaliação. Escopo de referência: sistema pequeno/médio, com banco, testes e aproximadamente 3 mil linhas. O foco é a modernização de aplicações Java/Spring.

Pergunta orientadora: em que medida agentes de código conseguem migrar uma aplicação de Java 8 / Spring Boot 2.1 para Java 17 / Spring Boot 3.5, preservando o comportamento e de fato modernizando código e testes, e quais dificuldades encontram?

## Acordos, propostas e decisões de implementação

- Direção do trabalho: aplicação própria e artificial; foco em Java/Spring; uma única versão de origem e uma de destino, ambas justificadas; Codex e Claude Code como agentes de migração; Ark em etapa posterior.
- Propostas técnicas do autor: e-commerce local, PostgreSQL 16, regras e fases descritas na especificação. A primeira baseline usou Java 17 e Boot 2.7.18; a origem foi redefinida para Java 8 e Boot 2.1 (DECISOES.md, D1).
- Pedido inicial do autor: implementar a Fundação com essa stack e os padrões da referência. Após o commit 502c36c, o autor autorizou a Fase 2, Catálogo, e solicitou a porta local 5432. Após o commit f452ab5 da Fase 2, autorizou a Fase 3, Segurança e administração. Após o commit 6778cd2, autorizou a Fase 4, Pedidos. Após o commit 4979132, autorizou a Fase 5, Consolidação. As escolhas rotineiras executadas estão em DECISOES.md; não representam aprovação acadêmica individual.
- Continuam propostas: protocolo experimental da nova origem, comparação com OpenRewrite e investigação de diagnósticos no Ark. Não há data de entrega definida no material.

## Ferramentas e limites

Codex e Claude Code auxiliam na construção, configuração, documentação e testes; o autor revisa o resultado. Ark poderá ser usado posteriormente, com versão, modelo e condições fixados. FreshBrew inspira avaliação comportamental; esta aplicação não integra o benchmark.

O objeto é controlado e recém-construído: não representa integralmente anos de manutenção. Três mil linhas são referência, nunca meta artificial. Na consolidação, contar separadamente produção, testes e infraestrutura. Não fabricar incompatibilidades ou complexidade.

O avaliador externo da Fase 5 fica fora do pacote entregue aos agentes. A baseline consolidada em 1e8ea40 (Java 17 / Boot 2.7.18) e o [protocolo experimental](EXPERIMENTO.md) `mes-boot35-v1` serviram ao estudo piloto, com as tentativas claude-01 e codex-03 (STATUS.md). O experimento definitivo usará a nova origem e um novo protocolo.

Referências de contexto fornecidas pelo autor: [Alura](https://www.alura.com.br/alura-labs/como-migramos-do-spring-boot-2-para-o-4-com-ajuda-de-ia), [Ark](https://github.com/mtov/ark), [FreshBrew](https://github.com/mrcabbage972/freshbrew). Sua menção não indica reprodução ou validação desses trabalhos nesta fase.
