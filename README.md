# mes-legacy-shop

Objeto de estudo do TP de MES. As Fases 1 e 2 fornecem a fundação local e o catálogo público de produtos. Ark e as migrações serão etapas posteriores. A construção inicial teve auxílio do Codex e requer revisão do autor.

## Versões e pré-requisitos

- JDK **17**, sem preview, com JAVA_HOME apontando para o JDK. O Enforcer rejeita outras linhas do Java.
- Maven **3.9.11** pelo Wrapper oficial **3.3.2**, tipo only-script, sem Maven global. O download tem SHA256 fixado. Primeira execução requer rede e PowerShell no Windows ou curl/wget e sha256sum/shasum no Linux.
- Spring Boot **2.7.18**. Hibernate **5.6.15.Final**, JDBC PostgreSQL **42.3.8** e JUnit Jupiter **5.8.2**, gerenciados pelo Boot.
- Docker com contêineres Linux e Docker Compose v2 com suporte a `up --wait` (verificado com 2.32.4). Testes usam Testcontainers **1.19.8** e Docker acessível pela JVM.
- PostgreSQL **16.10**, imagem `postgres:16.10-bookworm`, fixada também por digest `sha256:38471f330eb885e04de130b768d6db4e10469e2311879c7e5c699f6d2d8a1c74`. Compose e teste usam o mesmo conteúdo.
- JaCoCo **0.8.11**; relatório sem meta percentual arbitrária.

Execute os comandos a partir da raiz do repositório. Windows/PowerShell foi o ambiente de validação; a execução nativa em Linux ainda precisa ser confirmada. O script POSIX pode ser usado com `sh`, sem depender do bit executável de um checkout Windows.

## Preparar o banco

PowerShell, somente se ainda não houver .env:

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
$env:JAVA_HOME = 'C:\caminho\para\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
.\mvnw.cmd -v
docker compose config --quiet
docker compose up -d --wait
docker compose ps
```

Linux:

```sh
test -f .env || cp .env.example .env
export JAVA_HOME=/caminho/para/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"
java -version
sh ./mvnw -v
docker compose config --quiet
docker compose up -d --wait
docker compose ps
```

Edite .env antes de subir se necessário. O exemplo contém apenas credenciais locais de demonstração, ignoradas no Git quando copiadas para .env. A porta padrão do host é **5432**, por solicitação do autor após liberar o uso dessa porta. O banco continua em 5432 dentro do contêiner; a porta do host é publicada somente em 127.0.0.1.

O Compose lê .env para POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD e POSTGRES_PORT. A aplicação no perfil `local` (padrão) importa o mesmo arquivo como propriedades, sem biblioteca dotenv ou script que execute seu conteúdo. Use o formato simples CHAVE=valor, sem aspas, expansão de shell ou escapes; mantenha valores compatíveis com ambos os leitores. Variáveis de ambiente do processo têm precedência. POSTGRES_HOST (localhost) e SERVER_PORT (8080) são opcionais para a aplicação. Em outro perfil, forneça as variáveis explicitamente; não há perfil de produção.

Os scripts de `src/main/resources/db/schema` são montados em `/docker-entrypoint-initdb.d`, aplicados em ordem lexical **somente quando o volume está vazio**. V001__foundation.sql cria o esquema shop e V002__products.sql cria a tabela products. Mudanças em scripts, usuário ou senha não reinicializam volumes existentes.

## Build e teste essencial

PowerShell:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Linux:

```sh
sh ./mvnw -B -ntp clean verify
```

`verify` compila, empacota o JAR executável, roda FoundationIT e CatalogIT pelo Failsafe e gera `target/site/jacoco/index.html` e jacoco.csv. Cada classe de integração cria PostgreSQL real em contêiner descartável/porta aleatória, com banco e credenciais exclusivos, aplica os mesmos SQLs V001/V002 e inicia a aplicação. A suíte preserva as verificações de saúde e cobre catálogo, paginação, ativos/inativos, erros, persistência decimal, Bean Validation, restrições SQL e rejeição de esquema divergente. Não lê o banco de demonstração nem exige Compose previamente iniciado. A primeira execução também baixa imagens auxiliares fixadas pelo Testcontainers, como testcontainers/ryuk:0.7.0.

`test`/`package` isoladamente não executam a integração; use **verify** como verificação principal. Docker indisponível deve fazer o teste falhar, sem skip automático, H2 ou substituição por mocks. Resultados ficam em target/failsafe-reports. O nível de log do teste é INFO, mantendo erros e stack traces completos.

## Iniciar a aplicação

Com o Compose saudável, PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux:

```sh
sh ./mvnw spring-boot:run
```

Alternativamente, depois de `verify`, em ambos:

```sh
java -jar target/mes-legacy-shop-0.0.1-SNAPSHOT.jar
```

Pare a execução anterior com Ctrl+C antes de usar a mesma porta. Em outro terminal:

```powershell
curl.exe --fail http://127.0.0.1:8080/actuator/health
```

Linux:

```sh
curl --fail http://127.0.0.1:8080/actuator/health
```

Resposta esperada: HTTP 200 e `{"status":"UP"}`. Sem detalhes ou componentes sensíveis; a saúde inclui o DataSource. /actuator/env não é exposto. JPA estabelece a conexão durante a inicialização. O catálogo público está disponível; autenticação, administração e pedidos pertencem às próximas fases.

## Esquema, volume existente e dados de demonstração

A aplicação não aplica SQL: Hibernate usa `ddl-auto=validate` e falha quando o mapeamento diverge das tabelas. Não há Flyway/Liquibase ou histórico automático de scripts. Preserve V001; acrescente novos scripts versionados nas fases correspondentes.

**Banco novo:** `docker compose up -d --wait` com volume vazio aplica V001 e V002 em ordem, sem carga de demonstração automática.

**Volume existente da Fase 1:** ele já contém shop, mas ainda não products. Depois de subir o Compose, aplique **somente V002**, uma vez. PowerShell:

```powershell
Get-Content src/main/resources/db/schema/V002__products.sql -Raw | docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Linux:

```sh
docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < src/main/resources/db/schema/V002__products.sql
```

Não reaplique V001 nem V002 em uma base que já os recebeu. Antes de decidir, consulte `SELECT to_regclass('shop.products');` no psql: null indica tabela ausente. Os scripts falham em duplicação para evitar esconder o estado real do banco. Registre os scripts aplicados ao volume; inicie a aplicação somente após a atualização do esquema.

Para abrir psql usando as variáveis do contêiner (PowerShell e Linux):

```sh
docker compose exec postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Para carregar os produtos de demonstração explicitamente, PowerShell:

```powershell
Get-Content src/main/resources/db/demo/V001__products.sql -Raw | docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Linux:

```sh
docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < src/main/resources/db/demo/V001__products.sql
```

A carga tem três produtos ativos (um com estoque zero) e um inativo. Pode ser repetida por SKU, preservando produtos existentes e alterações locais; não há exclusão/reinicialização de dados por essa carga. Ainda não há usuários de demonstração. A fixture de teste é separada e só deve ser aplicada nos contêineres descartáveis, pois apaga produtos.

## Consultar o catálogo

PowerShell:

```powershell
curl.exe --fail 'http://127.0.0.1:8080/api/products?page=0&size=2'
curl.exe --fail 'http://127.0.0.1:8080/api/products/1'
```

Linux:

```sh
curl --fail 'http://127.0.0.1:8080/api/products?page=0&size=2'
curl --fail 'http://127.0.0.1:8080/api/products/1'
```

O ID 1 é um exemplo: use um ID retornado no seu catálogo. O JSON da listagem contém content, page, size, totalElements e totalPages. Produto contém somente id, sku, name, price (BRL) e stockQuantity. A ordem é fixa por id crescente; page padrão 0, máximo 1.000.000; size padrão 20, de 1 a 100. Inativos não aparecem nem entram nos totais. Sem estoque ainda é visível se ativo. Página além da última é vazia; catálogo vazio retorna totalPages=0.

Detalhe inexistente ou inativo: 404 PRODUCT_NOT_FOUND. Parâmetros inválidos: 400 INVALID_REQUEST com fieldErrors. Exemplos JSON e contrato completo em docs/ESPECIFICACAO.md. Não há endpoints de criação/edição de produto nesta fase.

Para parar preservando os dados: `docker compose stop`. Para remover o contêiner e a rede preservando o volume: `docker compose down`.

Para **apagar todos os dados experimentais deste projeto** e repetir a inicialização do banco novo:

```sh
docker compose down --volumes
docker compose up -d --wait
```

A redefinição acima é destrutiva e só se destina à base local descartável. Não foi necessário apagar volumes para mascarar falhas de testes. As verificações executadas, tentativas que falharam e limitações estão em [docs/STATUS.md](docs/STATUS.md).

## Documentação e próxima fase

[Contexto acadêmico](docs/CONTEXTO_TP.md), [especificação](docs/ESPECIFICACAO.md), [plano](docs/PLANO.md), [decisões](docs/DECISOES.md) e [estado real](docs/STATUS.md). As regras do e-commerce são propostas do autor. O próximo passo, mediante nova solicitação, é a Fase 3: segurança e administração. A Fase 2 não introduz rotas administrativas.
