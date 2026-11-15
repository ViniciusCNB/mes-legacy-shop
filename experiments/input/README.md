# mes-legacy-shop — entrada da tarefa

API REST de catálogo, administração e pedidos. O contrato está em [docs/ESPECIFICACAO.md](docs/ESPECIFICACAO.md); a tarefa a executar está em [prompt.txt](prompt.txt).

## Estado atual

O projeto usa Java 8 e Spring Boot 2.1.18.RELEASE, com Maven Wrapper 3.9.11. A suíte interna é formada por testes de integração executados pelo Failsafe na fase `verify`, com PostgreSQL 16 real em contêineres descartáveis.

## Ambiente

Use JAVA_HOME apontando para o JDK da versão alvo da tarefa, Maven Wrapper 3.9.11 e Docker Desktop em modo de contêineres Linux no Windows. Docker precisa estar acessível à JVM. Não é necessário iniciar o Compose local para executar os testes internos: cada suíte de integração usa um PostgreSQL descartável e aplica o esquema versionado.

PowerShell:

~~~powershell
.\mvnw.cmd -B -ntp clean verify
~~~

Linux:

~~~sh
sh ./mvnw -B -ntp clean verify
~~~

O comando gera o JAR em target/ e relatórios em target/failsafe-reports/. Para execução manual, use um projeto Compose e banco próprios da tentativa. O exemplo .env.example pode ser copiado para .env; defina POSTGRES_PORT como porta livre, diferente de qualquer banco de uso normal. O Compose instala src/main/resources/db/schema no volume novo. Inicie o aplicativo com .\mvnw.cmd spring-boot:run ou sh ./mvnw spring-boot:run. A porta HTTP padrão é 8080, ajustável com SERVER_PORT. A saúde fica em /actuator/health.

A carga opcional em src/main/resources/db/demo/ contém exclusivamente dados locais. Veja o README da pasta. As credenciais locais são admin@local.test / Admin-local-123!, cliente1@local.test / Cliente1-local-123! e cliente2@local.test / Cliente2-local-123!.
