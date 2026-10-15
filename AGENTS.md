# Instruções de trabalho

- Leia README.md e docs/{CONTEXTO_TP,ESPECIFICACAO,PLANO,STATUS,DECISOES}.md antes de alterar a aplicação.
- Implemente somente a fase solicitada. A Fase 5 consolida documentação/métricas e evaluation/ independente. Não iniciar migração Ark, criar commits/tags ou considerar a baseline revisada sem revisão do autor.
- Mantenha Java 17 sem preview, Spring Boot 2.7.18, Maven Wrapper 3.9.11 e PostgreSQL 16. Use o gerenciamento de dependências do Boot; justifique e fixe componentes adicionais.
- Código em inglês, documentação em português; POJOs sem Lombok, organização por funcionalidade conforme surgirem funcionalidades. Não exponha entidades JPA na API.
- SQL versionado em src/main/resources/db/schema; dados locais em db/demo. Hibernate usa validate; sem update, H2, Flyway ou Liquibase.
- Testes de integração precisam de PostgreSQL real e devem falhar quando Docker estiver indisponível. Não ignore falhas ou reduza assertions para obter sucesso.
- Verificação completa: PowerShell `.\mvnw.cmd -B -ntp clean verify`; Linux `sh ./mvnw -B -ntp clean verify`. Exige JAVA_HOME com JDK 17 e Docker acessível. Resultados e limitações ficam em docs/STATUS.md.
- Banco local: copie .env.example para .env sem sobrescrever configuração existente; `docker compose up -d --wait`. Execução: `.\mvnw.cmd spring-boot:run` ou `sh ./mvnw spring-boot:run`.
- Não adicione falhas deliberadas, pistas ou soluções de migração. Não inicie Ark nem implemente frontend, integrações externas ou produção.
- Compras bloqueiam produtos por ID crescente; cancelamentos bloqueiam primeiro o pedido e depois os produtos nessa ordem. Preserve atomicidade e reposição única, inclusive concorrente. Mantenha CSRF habilitado nas escritas; os testes devem obter cookie/token reais. Não substituir usuários do banco por mocks de segurança. Basic é exigido em cada requisição protegida; cookie CSRF não autentica.
- Avaliação externa: primeiro construa/verifique o JAR na raiz; depois .\evaluation\run.ps1 ou bash evaluation/run.sh. Banco novo isolado, sem reset do volume local. Não executar roteiros simultâneos no mesmo checkout. Avaliador/especificação têm snapshot; registrar/revisar mudanças antes do experimento. Métricas: java tools/SourceMetrics.java. Não entregar evaluation/ ao agente de migração.
- Atualize documentação ao concluir cada fase. Não faça commits nesta tarefa; o autor fará os commits.
