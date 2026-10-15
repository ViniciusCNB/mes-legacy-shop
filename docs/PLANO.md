# Plano por fases

| Fase | Escopo | Critério de conclusão |
| --- | --- | --- |
| 1 Fundação | Boot 2.7.18/Java 17, Wrapper, Compose, configuração, saúde, SQL inicial e documentação | Build e inicialização verificáveis, teste com PostgreSQL e limitações registradas |
| 2 Catálogo | Product, SQL, dados locais, catálogo público e validações | Persistência e consultas verificadas, inativos ocultos; sem administração |
| 3 Segurança e administração | AppUser, BCrypt, HTTP Basic, perfis, produtos/estoque administrativos, /api/me | Autenticação, autorização e ausência de hashes/senhas nas respostas verificadas; política CSRF/sessão documentada |
| 4 Pedidos | Criação, isolamento, preço contratado, cancelamento, filtros e resumo | Atomicidade, concorrência e fluxos críticos verificados com PostgreSQL |
| 5 Consolidação | evaluation/ independente, roteiro, métricas e documentação completa | Banco novo reproduzível, avaliador passando, revisão do autor e candidato a baseline identificado |

O estado efetivamente observado fica em STATUS.md. A conclusão de uma fase não autoriza iniciar a seguinte.

## Aceitação futura da aplicação completa

Stack inicial preservada; ambiente reconstruível; comportamentos da ESPECIFICACAO passando em banco novo; contratos/dados/versões conhecidos; revisão do autor; commit candidato identificado. Somente depois da revisão considerar a tag baseline-boot-2.7. Nenhum commit ou tag é feito nesta tarefa.

## Experimento posterior (planejamento)

Após validar a aplicação, fixar versões concretas de Boot 3.5.x e 4.0.x, Ark/modelo/prompt, ferramentas, limites e assistência permitida. Começar cada repetição da mesma baseline limpa. Registrar build, testes, inicialização, HTTP, persistência, alterações, tokens e custo. Identificar intervenções humanas; a segunda etapa depende de uma versão intermediária válida e não deve ser atribuída integralmente ao Ark quando houver correção assistida.

## Andamento em 15/10/2026

- Fase 1 concluída e commitada pelo autor em 502c36c.
- Fase 2 autorizada pelo autor: catálogo público e retorno da porta local PostgreSQL a 5432. Implementação e testes relacionados estão nesta alteração; resultados finais em STATUS.md.
- A Fase 3 será o próximo passo, somente mediante nova solicitação: usuários, HTTP Basic/BCrypt, política de sessão/CSRF e administração de produtos/estoque.
- Nenhum experimento Ark ou migração de framework é autorizado pelas duas primeiras fases.
