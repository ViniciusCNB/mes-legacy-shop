# mes-legacy-shop

Objeto de estudo do TP de MES. As Fases 1 a 4 fornecem a fundação local, catálogo, segurança/administração e pedidos com estoque transacional. Ark e as migrações serão etapas posteriores. A construção inicial teve auxílio do Codex e requer revisão do autor.

## Versões e pré-requisitos

- JDK **17**, sem preview, com JAVA_HOME apontando para o JDK. O Enforcer rejeita outras linhas do Java.
- Maven **3.9.11** pelo Wrapper oficial **3.3.2**, tipo only-script, sem Maven global. O download tem SHA256 fixado. Primeira execução requer rede e PowerShell no Windows ou curl/wget e sha256sum/shasum no Linux.
- Spring Boot **2.7.18** e Spring Security **5.7.11**, gerenciado pelo Boot. Hibernate **5.6.15.Final**, JDBC PostgreSQL **42.3.8** e JUnit Jupiter **5.8.2**, gerenciados pelo Boot.
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

Os scripts de `src/main/resources/db/schema` são montados em `/docker-entrypoint-initdb.d`, aplicados em ordem lexical **somente quando o volume está vazio**. V001__foundation.sql cria shop, V002__products.sql cria products, V003__users.sql cria app_users e V004__orders.sql cria purchase_orders/order_items. Mudanças em scripts, usuário ou senha não reinicializam volumes existentes.

## Build e teste essencial

PowerShell:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Linux:

```sh
sh ./mvnw -B -ntp clean verify
```

`verify` compila, empacota o JAR executável, roda FoundationIT, CatalogIT, SecurityAdminIT e OrdersIT pelo Failsafe e gera `target/site/jacoco/index.html` e jacoco.csv. Cada classe de integração cria PostgreSQL real em contêiner descartável/porta aleatória, com banco e credenciais exclusivos, aplica os mesmos SQLs V001/V002/V003/V004 e inicia a aplicação. A suíte preserva as verificações de saúde e cobre catálogo, paginação, ativos/inativos, erros, persistência decimal, Bean Validation, restrições SQL e rejeição de esquema divergente. A suíte de segurança usa Basic, BCrypt e CSRF reais e cliente HTTP do Java 17, incluindo PATCH, sem mocks de autenticação. OrdersIT verifica cálculos, rollback multitem, preço histórico, isolamento, cancelamento e concorrência HTTP, filtros/resumo e restrições reais. Não lê o banco de demonstração nem exige Compose previamente iniciado. A primeira execução também baixa imagens auxiliares fixadas pelo Testcontainers, como testcontainers/ryuk:0.7.0.

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

Resposta esperada: HTTP 200 e `{"status":"UP"}`. Sem detalhes ou componentes sensíveis; a saúde inclui o DataSource. /actuator/env não é exposto. JPA estabelece a conexão durante a inicialização. Catálogo e saúde são públicos. As rotas de usuário/CSRF exigem Basic; administração de produtos exige ADMIN. Pedidos exigem CUSTOMER ou proprietário/ADMIN conforme a seção da Fase 4.

## Esquema, volume existente e dados de demonstração

A aplicação não aplica SQL: Hibernate usa `ddl-auto=validate` e falha quando o mapeamento diverge das tabelas. Não há Flyway/Liquibase ou histórico automático de scripts. Preserve V001; acrescente novos scripts versionados nas fases correspondentes.

**Banco novo:** `docker compose up -d --wait` com volume vazio aplica V001, V002 e V003 em ordem, sem carga de demonstração automática.

**Volume existente da Fase 2:** ele já contém shop/products. Depois de subir o Compose, aplique **somente V003**, uma vez. Se estiver retomando da Fase 1, aplique V002 antes de V003. PowerShell:

```powershell
Get-Content src/main/resources/db/schema/V003__users.sql -Raw | docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Linux:

```sh
docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < src/main/resources/db/schema/V003__users.sql
```

Não reaplique V001, V002 ou V003 em uma base que já os recebeu. Antes de decidir, consulte `SELECT to_regclass('shop.products'), to_regclass('shop.app_users');` no psql: null indica tabela ausente. Os scripts falham em duplicação para evitar esconder o estado real do banco. Registre os scripts aplicados ao volume; inicie a aplicação somente após a atualização do esquema.

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

A carga tem três produtos ativos (um com estoque zero) e um inativo. Pode ser repetida por SKU, preservando produtos existentes e alterações locais; não há exclusão/reinicialização de dados por essa carga. A carga de usuários abaixo é separada. A fixture de teste é separada e só deve ser aplicada nos contêineres descartáveis, pois apaga produtos.

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

Detalhe inexistente ou inativo: 404 PRODUCT_NOT_FOUND. Parâmetros inválidos: 400 INVALID_REQUEST com fieldErrors. Exemplos JSON e contrato completo em docs/ESPECIFICACAO.md. As operações administrativas são descritas abaixo.

## Usuários locais, autenticação e CSRF

Depois de aplicar V003, carregue explicitamente os usuários. PowerShell:

```powershell
Get-Content src/main/resources/db/demo/V002__users.sql -Raw | docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Linux:

```sh
docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < src/main/resources/db/demo/V002__users.sql
```

Credenciais **exclusivamente locais de demonstração**:

| Email | Senha local | Perfil |
| --- | --- | --- |
| admin@local.test | Admin-local-123! | ADMIN |
| cliente1@local.test | Cliente1-local-123! | CUSTOMER |
| cliente2@local.test | Cliente2-local-123! | CUSTOMER |

A carga armazena hashes BCrypt custo 10. Pode ser repetida sem sobrescrever contas, senhas, perfis ou situação ativa existentes. Os testes têm usuários/senha próprios e banco descartável. Não há cadastro público nem API de administração de contas.

GET /api/me retorna somente id, name, email e role. PowerShell:

```powershell
curl.exe --fail --user 'admin@local.test:Admin-local-123!' http://127.0.0.1:8080/api/me
```

Linux:

```sh
curl --fail --user 'admin@local.test:Admin-local-123!' http://127.0.0.1:8080/api/me
```

Cada requisição protegida precisa de HTTP Basic; os usuários e a situação ativa são consultados no banco. Não há sessão de autenticação/JSESSIONID, formulário de login, JWT ou logout de sessão. Um cookie CSRF não autentica. O uso aqui é HTTP local em loopback.

CSRF está **habilitado**, inclusive para clientes autenticados por Basic. Antes de cada escrita, faça GET /api/csrf autenticado: ele retorna token/headerName e um cookie XSRF-TOKEN HttpOnly. Preserve o cookie e envie o token no cabeçalho X-XSRF-TOKEN. A autenticação pode renovar o cookie; o fluxo abaixo renova o token antes de cada escrita e mantém o cookie jar atualizado. A rota de token é parte funcional da API, sem bypass de segurança para testes.

Exemplo completo PowerShell, usando um SKU ainda livre:

```powershell
$apiBase = 'http://127.0.0.1:8080'
$demoPair = 'admin@local.test:Admin-local-123!'
$authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($demoPair))
$authHeaders = @{ Authorization = $authorization }
$token = Invoke-RestMethod "$apiBase/api/csrf" -Headers $authHeaders -SessionVariable apiSession
$writeHeaders = @{ Authorization = $authorization }
$writeHeaders[$token.headerName] = $token.token
$payload = @{ sku='ADMIN-DEMO-001'; name='Produto administrativo'; price=49.90; stockQuantity=3; active=$true } | ConvertTo-Json
$created = Invoke-RestMethod "$apiBase/api/admin/products" -Method Post -Headers $writeHeaders -WebSession $apiSession -ContentType 'application/json' -Body $payload
$created

$token = Invoke-RestMethod "$apiBase/api/csrf" -Headers $authHeaders -WebSession $apiSession
$writeHeaders[$token.headerName] = $token.token
$payload = @{ name='Produto administrativo revisado'; price=59.90; active=$false } | ConvertTo-Json
Invoke-RestMethod "$apiBase/api/admin/products/$($created.id)" -Method Put -Headers $writeHeaders -WebSession $apiSession -ContentType 'application/json' -Body $payload

$token = Invoke-RestMethod "$apiBase/api/csrf" -Headers $authHeaders -WebSession $apiSession
$writeHeaders[$token.headerName] = $token.token
$payload = @{ stockQuantity=7 } | ConvertTo-Json
Invoke-RestMethod "$apiBase/api/admin/products/$($created.id)/stock" -Method Patch -Headers $writeHeaders -WebSession $apiSession -ContentType 'application/json' -Body $payload
```

O exemplo deixa esse produto inativo e preservado no banco. Repetir o POST com o mesmo SKU retorna 409; escolha outro SKU para nova criação. PUT altera apenas nome/preço/ativo; PATCH define estoque **absoluto**, sem somar. Nenhuma rota exclui produtos.

Exemplo de criação Linux com curl e awk, sem ferramenta adicional para JSON:

```sh
api_base='http://127.0.0.1:8080'
demo_auth='admin@local.test:Admin-local-123!'
cookie_file=$(mktemp)
trap 'rm -f "$cookie_file"' EXIT
curl --fail --silent --show-error --user "$demo_auth" --cookie "$cookie_file" --cookie-jar "$cookie_file" "$api_base/api/csrf"
csrf_token=$(awk '$6 == "XSRF-TOKEN" {print $7}' "$cookie_file")
curl --fail --silent --show-error --user "$demo_auth" --cookie "$cookie_file" --cookie-jar "$cookie_file" \
  -H "X-XSRF-TOKEN: $csrf_token" -H 'Content-Type: application/json' \
  --data '{"sku":"ADMIN-DEMO-LINUX-001","name":"Produto administrativo","price":49.90,"stockQuantity":3,"active":true}' \
  "$api_base/api/admin/products"
```

Para PUT/PATCH, renove o token com o mesmo GET/cookie jar e releia o valor com awk antes da escrita. O prefixo #HttpOnly_ no arquivo curl é esperado. Esses comandos POSIX devem ser confirmados no ambiente Linux do autor; a validação de execução por plataforma está em STATUS.md.

Respostas de segurança: 401 AUTHENTICATION_REQUIRED (ausência/falha de Basic, incluindo usuário inativo), 403 FORBIDDEN (perfil sem permissão), 403 CSRF_INVALID (token ausente/inválido). O filtro CSRF precede autenticação; uma escrita sem token válido pode retornar 403 antes de avaliar credenciais. Com CSRF válido e sem Basic, retorna 401. GET protegido sem Basic retorna 401 com WWW-Authenticate. Credenciais inválidas enviadas até a uma rota pública são rejeitadas.

Corpos JSON são estritos: campos obrigatórios, propriedades desconhecidas e tipos inválidos são rejeitados com 400; estoque exige inteiro e active exige booleano. Dinheiro tem no máximo duas casas, completadas com zeros na resposta, sem arredondar valores inválidos. SKU duplicado: 409 SKU_ALREADY_EXISTS. Produto inexistente na administração: 404 PRODUCT_NOT_FOUND. As respostas usam code/message/fieldErrors quando aplicável; dados internos e hashes não são expostos. Acesso não previsto é negado por padrão, incluindo outros endpoints Actuator.

Para parar preservando os dados: `docker compose stop`. Para remover o contêiner e a rede preservando o volume: `docker compose down`.

Para **apagar todos os dados experimentais deste projeto** e repetir a inicialização do banco novo:

```sh
docker compose down --volumes
docker compose up -d --wait
```

A redefinição acima é destrutiva e só se destina à base local descartável. Não foi necessário apagar volumes para mascarar falhas de testes. As verificações executadas, tentativas que falharam e limitações estão em [docs/STATUS.md](docs/STATUS.md).

## Pedidos — Fase 4

Banco novo já aplica V001–V004. Para um volume das Fases 1–3, aplique somente os scripts que faltam, em ordem. V004 exige products/app_users e não é repetível: confira antes com SELECT to_regclass('shop.purchase_orders'). Se a tabela existir, inspecione o esquema em vez de repetir ou apagar dados. A aplicação deve estar parada durante a atualização.

PowerShell, depois de confirmar que V004 ainda não foi aplicado:

~~~powershell
Get-Content src/main/resources/db/schema/V004__orders.sql -Raw | docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
~~~

Linux:

~~~sh
docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < src/main/resources/db/schema/V004__orders.sql
~~~

V004 é transacional, preserva produtos/usuários e cria pedidos/itens sem exclusão em cascata. Depois inicie a aplicação normalmente. Não é necessário reinicializar o volume. Carregue os dados de demonstração anteriores se ainda não existirem.

Somente CUSTOMER cria/lista os próprios pedidos. Proprietário ou ADMIN consulta/cancela por ID; outro cliente recebe o mesmo 404 do pedido ausente. A compra recebe apenas items com productId/quantity, entre 1 e 100 produtos distintos, quantidades inteiras positivas. O servidor calcula valores e baixa estoque em uma transação. Preços contratados não mudam com reajustes; cancelar restitui uma única vez, inclusive com chamadas simultâneas. CSRF permanece obrigatório.

Exemplo PowerShell completo, com a aplicação iniciada e MUG-001 disponível:

~~~powershell
$apiBase = 'http://127.0.0.1:8080'
$product = (Invoke-RestMethod "$apiBase/api/products").content | Where-Object sku -eq 'MUG-001' | Select-Object -First 1
if (!$product -or $product.stockQuantity -lt 2) { throw 'MUG-001 precisa de duas unidades disponíveis' }
$authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('cliente1@local.test:Cliente1-local-123!'))
$authHeaders = @{ Authorization = $authorization }
$token = Invoke-RestMethod "$apiBase/api/csrf" -Headers $authHeaders -SessionVariable orderSession
$writeHeaders = @{ Authorization = $authorization }
$writeHeaders[$token.headerName] = $token.token
$payload = @{ items=@(@{ productId=$product.id; quantity=2 }) } | ConvertTo-Json -Depth 4
$response = Invoke-WebRequest "$apiBase/api/orders" -Method Post -Headers $writeHeaders -WebSession $orderSession -ContentType 'application/json' -Body $payload
$created = $response.Content | ConvertFrom-Json
$response.StatusCode
$created
Invoke-RestMethod "$apiBase/api/orders/$($created.id)" -Headers $authHeaders
Invoke-RestMethod "$apiBase/api/orders?page=0&size=20" -Headers $authHeaders

$adminHeaders = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('admin@local.test:Admin-local-123!')) }
Invoke-RestMethod "$apiBase/api/admin/orders?customerId=$($created.customerId)&status=CREATED" -Headers $adminHeaders
Invoke-RestMethod "$apiBase/api/admin/reports/orders?customerId=$($created.customerId)" -Headers $adminHeaders

$token = Invoke-RestMethod "$apiBase/api/csrf" -Headers $authHeaders -WebSession $orderSession
$writeHeaders[$token.headerName] = $token.token
$cancelled = Invoke-RestMethod "$apiBase/api/orders/$($created.id)/cancel" -Method Post -Headers $writeHeaders -WebSession $orderSession
$token = Invoke-RestMethod "$apiBase/api/csrf" -Headers $authHeaders -WebSession $orderSession
$writeHeaders[$token.headerName] = $token.token
$again = Invoke-RestMethod "$apiBase/api/orders/$($created.id)/cancel" -Method Post -Headers $writeHeaders -WebSession $orderSession
$cancelled
$again
~~~

O exemplo deixa um pedido CANCELLED preservado e restitui as duas unidades, sem excluir o histórico. Em dados novos, MUG-001 custa 39.90: quantidade 2 resulta em total 79.80. Se o autor alterou preço/estoque, o resultado acompanha os dados atuais.

Exemplo POSIX com curl/awk/sed (confira product_id no catálogo antes; 1 corresponde a MUG-001 na carga nova):

~~~sh
api_base='http://127.0.0.1:8080'
demo_auth='cliente1@local.test:Cliente1-local-123!'
product_id=1
curl --fail --silent --show-error "$api_base/api/products"
cookie_file=$(mktemp)
trap 'rm -f "$cookie_file"' EXIT
curl --fail --silent --show-error --user "$demo_auth" --cookie "$cookie_file" --cookie-jar "$cookie_file" "$api_base/api/csrf" > /dev/null
csrf_token=$(awk '$6 == "XSRF-TOKEN" {print $7}' "$cookie_file")
created=$(curl --fail --silent --show-error --user "$demo_auth" --cookie "$cookie_file" --cookie-jar "$cookie_file" \
  -H "X-XSRF-TOKEN: $csrf_token" -H 'Content-Type: application/json' \
  --data "{\"items\":[{\"productId\":$product_id,\"quantity\":1}]}" "$api_base/api/orders") || exit 1
printf '%s\n' "$created"
order_id=$(printf '%s\n' "$created" | sed -n 's/.*"id":\([0-9][0-9]*\).*/\1/p')
test -n "$order_id" || exit 1
curl --fail --silent --show-error --user "$demo_auth" "$api_base/api/orders/$order_id"
curl --fail --silent --show-error --user "$demo_auth" --cookie "$cookie_file" --cookie-jar "$cookie_file" "$api_base/api/csrf" > /dev/null
csrf_token=$(awk '$6 == "XSRF-TOKEN" {print $7}' "$cookie_file")
curl --fail --silent --show-error --user "$demo_auth" --cookie "$cookie_file" --cookie-jar "$cookie_file" \
  -H "X-XSRF-TOKEN: $csrf_token" -X POST "$api_base/api/orders/$order_id/cancel"
curl --fail --silent --show-error --user 'admin@local.test:Admin-local-123!' "$api_base/api/admin/reports/orders"
~~~

O sed acima extrai o campo id da resposta compacta desta aplicação; não é um parser JSON geral. O roteiro preserva um pedido CANCELLED e devolve o estoque.

GET /api/admin/orders aceita customerId, status (CREATED/CANCELLED), start/end e page/size. GET /api/admin/reports/orders aceita customerId/start/end, exclui cancelados e retorna orderCount/total. Datas são instantes UTC ISO-8601 com Z, por exemplo start=2026-10-15T00:00:00Z&end=2026-10-16T00:00:00Z, filtradas sobre createdAt: início inclusivo/fim exclusivo. Sem datas considera todo o histórico; intervalo vazio/invertido é inválido. Listas ordenam id decrescente, page padrão 0 e size 20 (até 100), envelope como catálogo.

Falhas de item desfazem a compra inteira: produto ausente 404 PRODUCT_NOT_FOUND, inativo 409 PRODUCT_INACTIVE, estoque insuficiente 409 INSUFFICIENT_STOCK; produto repetido/entrada inválida 400 INVALID_REQUEST. Pedido ausente/de outro cliente retorna 404 ORDER_NOT_FOUND. Reposição acima do limite inteiro retorna 409 STOCK_LIMIT_EXCEEDED e desfaz todo o cancelamento. Contrato completo em docs/ESPECIFICACAO.md.
## Documentação e próxima fase

[Contexto acadêmico](docs/CONTEXTO_TP.md), [especificação](docs/ESPECIFICACAO.md), [plano](docs/PLANO.md), [decisões](docs/DECISOES.md) e [estado real](docs/STATUS.md). As regras do e-commerce são propostas do autor. O próximo passo, mediante nova solicitação, é a Fase 5: consolidação, evaluation/ independente e preparação da baseline. Nenhum avaliador externo ou experimento Ark foi iniciado.
