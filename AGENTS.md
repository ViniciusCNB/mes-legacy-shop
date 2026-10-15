# Instruções de trabalho

- Leia README.md e docs/{CONTEXTO_TP,ESPECIFICACAO,PLANO,STATUS,DECISOES}.md antes de alterar a aplicação.
- Implemente somente a fase solicitada. A Fase 2 inclui somente Product, catálogo público, SQL/dados locais e testes correspondentes. Segurança, administração e pedidos pertencem às fases seguintes.
- Mantenha Java 17 sem preview, Spring Boot 2.7.18, Maven Wrapper 3.9.11 e PostgreSQL 16. Use o gerenciamento de dependências do Boot; justifique e fixe componentes adicionais.
- Código em inglês, documentação em português; POJOs sem Lombok, organização por funcionalidade conforme surgirem funcionalidades. Não exponha entidades JPA na API.
- SQL versionado em src/main/resources/db/schema; dados locais em db/demo. Hibernate usa validate; sem update, H2, Flyway ou Liquibase.
- Testes de integração precisam de PostgreSQL real e devem falhar quando Docker estiver indisponível. Não ignore falhas ou reduza assertions para obter sucesso.
- Verificação completa: PowerShell `.\mvnw.cmd -B -ntp clean verify`; Linux `sh ./mvnw -B -ntp clean verify`. Exige JAVA_HOME com JDK 17 e Docker acessível. Resultados e limitações ficam em docs/STATUS.md.
- Banco local: copie .env.example para .env sem sobrescrever configuração existente; `docker compose up -d --wait`. Execução: `.\mvnw.cmd spring-boot:run` ou `sh ./mvnw spring-boot:run`.
- Não adicione falhas deliberadas, pistas ou soluções de migração. Não inicie Ark nem implemente frontend, integrações externas ou produção.
- Atualize documentação ao concluir cada fase. Não faça commits nesta tarefa; o autor fará os commits.
