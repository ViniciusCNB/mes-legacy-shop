# Contexto do Trabalho Prático

Autor: Vinícius Borges. Disciplina MES, mestrado da UFMG. Professor: Marco Tulio Valente.
Referência integralmente lida: Contexto_e_Prompt_TP_Ark_SpringBoot.md, de 15/10/2026, fornecida pelo autor. Este documento sintetiza o contexto; o prompt reproduzido na referência não autoriza fases adicionais.

## Origem e questão orientadora

A proposta inicial de avaliar Ark em projetos FreshBrew deu lugar à construção de uma aplicação própria, para conhecer as regras e controlar a avaliação. As notas da reunião citam sistema pequeno/médio, banco, testes e aproximadamente 3 mil linhas; Java 7 e Struts foram considerados. Segundo o relato fornecido, em email posterior o professor sugeriu Spring Boot 2.7 → 3 → 4, inspirado no relato da Alura.

Pergunta orientadora: em que medida o Ark consegue migrar uma aplicação de Spring Boot 2.7 para 3 e depois para 4, preservando comportamento, e quais dificuldades encontra?

## Acordos, propostas e decisões de implementação

- Orientação acadêmica relatada: construir aplicação própria e estudar migração entre linhas do Boot. Não houve acesso independente ao email ou às notas da reunião.
- Propostas técnicas do autor: e-commerce local, Java 17, Boot 2.7.18, PostgreSQL 16, regras e fases descritas na especificação. Não são requisitos adicionais atribuídos ao professor.
- Pedido vigente do autor: implementar somente a Fundação com essa stack e os padrões da referência. As escolhas rotineiras executadas estão em DECISOES.md; não representam aprovação acadêmica individual.
- Continuam propostas: protocolo experimental final, versões alvo, comparação com OpenRewrite/outros agentes e investigação de diagnósticos no Ark. Não há data de entrega definida no material.

## Ferramentas e limites

Codex auxilia na construção inicial, configuração, documentação e testes; o autor deve revisar o resultado. Ark será usado posteriormente com versão/modelo e condições fixados. FreshBrew inspira avaliação comportamental; esta aplicação não integra o benchmark.

O objeto é controlado e recém-construído: não representa integralmente anos de manutenção. Três mil linhas são referência, nunca meta artificial. Na consolidação, contar separadamente produção, testes e infraestrutura. Não fabricar incompatibilidades ou complexidade.

O avaliador externo da Fase 5 ficará fora do pacote entregue ao agente de migração. O protocolo definirá arquivos excluídos, assistência humana, orçamento, repetições e evidências. Não há baseline candidata nem experimento iniciado.

Referências de contexto fornecidas pelo autor: [Alura](https://www.alura.com.br/alura-labs/como-migramos-do-spring-boot-2-para-o-4-com-ajuda-de-ia), [Ark](https://github.com/mtov/ark), [FreshBrew](https://github.com/mrcabbage972/freshbrew). Sua menção não indica reprodução ou validação desses trabalhos nesta fase.
