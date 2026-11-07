# Instruções da tarefa

- Leia README.md, docs/ESPECIFICACAO.md e prompt.txt. Trabalhe na pasta fornecida para a tentativa.
- Execute somente a etapa indicada em prompt.txt. Mantenha Java 17, Maven Wrapper 3.9.11 e PostgreSQL 16.
- Preserve o contrato HTTP, regras de negócio, persistência, autenticação, CSRF e garantias de concorrência.
- Mantenha as verificações de negócio dos testes. Adapte o uso do framework quando necessário, sem remover, enfraquecer ou desativar assertions e sem substituir PostgreSQL por mocks ou H2.
- No Windows, valide com .\mvnw.cmd -B -ntp clean verify; em Linux, com sh ./mvnw -B -ntp clean verify. Relate falhas e bloqueios reais.
- Use apenas o material da tarefa e ferramentas locais de inspeção, edição e teste. Downloads de dependências e imagens necessários ao build/testes são permitidos.
- Use bancos descartáveis nos testes. Não modifique serviços, bancos ou repositórios alheios à tarefa.
- Não faça commits, tags, publicação ou etapas posteriores. No encerramento, informe mudanças, verificações e pendências.
