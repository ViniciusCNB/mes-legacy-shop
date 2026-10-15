# Avaliador HTTP independente — 1.0.0

Projeto Maven separado, sem parent Boot e sem imports/dependências da aplicação, Spring, JPA, JDBC ou Testcontainers. Os testes enxergam somente HTTP. Não é módulo do POM raiz e não é parte do JAR da loja.

JUnit Jupiter 5.8.2 e Jackson Databind/Core/Annotations 2.13.5, fixados no POM; HttpClient do JDK 17. Jackson lê dinheiro como BigDecimal. Plugins Clean 3.2.0, Resources 3.2.0, Compiler 3.10.1, Surefire 2.22.2, Jar 3.2.2 e Enforcer 3.4.1 têm versões explícitas. A compatibilidade foi verificada executando o avaliador; não há gerenciamento de versões herdado do Spring.

## Execução completa a partir de banco novo

Execute da raiz do repositório, um roteiro por checkout de cada vez. Pré-requisitos da aplicação, JDK 17 selecionado em JAVA_HOME, Docker/Compose acessível e porta HTTP 18080 livre. PowerShell 7 no Windows; Bash, curl, sed, grep, coreutils e ferramentas do Wrapper no Linux. Os scripts rejeitam JDK fora de 17 antes de criar recursos.

PowerShell:

~~~powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-17'
.\mvnw.cmd -B -ntp clean verify
.\evaluation\run.ps1
~~~

Bash/Linux:

~~~sh
export JAVA_HOME=/caminho/para/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"
sh ./mvnw -B -ntp clean verify
bash evaluation/run.sh
~~~

O build raiz verifica a aplicação e produz seu JAR. Os scripts exigem esse artefato; não silenciam nem pulam testes para criá-lo. Um build raiz com clean remove também target/evaluation-runs: arquive as evidências antes de limpar.

O roteiro:

1. Verifica JDK/JAR/porta e cria identidade aleatória mes-evaluation-<12 hexadecimais>, recusando containers/volumes já existentes com essa identidade.
2. Cria PostgreSQL 16.10 no digest da aplicação, em porta dinâmica de loopback e volume novo, com os scripts V001–V004 da aplicação.
3. Carrega fixtures/seed.sql, que falha em banco já populado; não trunca tabelas. Dados de demonstração da loja não são usados.
4. Inicia o JAR no perfil evaluation, com variáveis explícitas e sem importar o .env local. Aguarda saúde UP com limite de tempo.
5. Executa Maven Wrapper com -f evaluation/pom.xml clean verify e a URL explícita, preservando falhas.
6. Arquiva logs, relatórios JUnit e identificação do JAR; encerra o seu processo e remove somente o projeto/volume descartável criado nesta execução, inclusive quando o avaliador falha.

Nenhum reset do volume local em 5432. Não execute avaliações simultâneas no mesmo checkout: evaluation/target é compartilhado. A execução nativa Linux continua sujeita aos pré-requisitos e ao estado registrado em docs/STATUS.md; Git Bash usa o JDK Windows.

Para outra porta: .\evaluation\run.ps1 -HttpPort 18081 ou EVALUATION_HTTP_PORT=18081 bash evaluation/run.sh. Para outro artefato já construído: parâmetro -ApplicationJar no PowerShell ou primeiro argumento do Bash. O avaliador permanece o mesmo; compatibilidade do artefato será determinada pelos resultados.

## Execução direta contra ambiente já preparado

Somente se a aplicação com fixtures de avaliação já estiver em execução e isolada:

~~~powershell
.\mvnw.cmd -B -ntp -f evaluation/pom.xml clean verify '-Devaluation.baseUrl=http://127.0.0.1:18080'
~~~

No Linux, substitua .\mvnw.cmd por sh ./mvnw. Sem evaluation.baseUrl válida, o avaliador falha com diagnóstico; não usa implicitamente localhost:8080. @BeforeAll exige o administrador e o SKU das fixtures antes de executar escritas. Use exclusivamente base experimental: os testes criam produtos/pedidos e não excluem esse histórico pela API.

## Dados e segurança

Banco shop_evaluation; credencial PostgreSQL evaluation_local_only exclusiva do ambiente descartável. Usuários locais admin@evaluation.local, alice@evaluation.local, bob@evaluation.local e disabled@evaluation.local; senha test-password, BCrypt custo 10. ADMIN id 1, clientes ids 2/3; conta 4 inativa. Produtos fixos ids 10 (ativo/25 unidades/19.90), 20 (inativo), 30 (ativo/sem estoque).

Produtos de cada cenário são criados por ADMIN via API, com SKU exclusivo. Nenhum teste altera diretamente SQL/estoque/timestamps ou importa fixtures internas da aplicação. Basic, cookie HttpOnly e cabeçalho CSRF são reais; token é renovado antes de cada escrita. Cookie não autentica.

## Cenários e limites

| Cenário externo | Verificação observável |
| --- | --- |
| Saúde/catálogo | Estado mínimo, ativos/inativos, produto sem estoque, campos, paginação/ordem/totais e erros. |
| Segurança | Perfis e usuários reais; credencial errada/inativa; 401/403; CSRF ausente rejeitado; cookie não cria sessão autenticada; nenhum hash exposto. |
| Administração | Criação, duplicidade de SKU, preço inválido, estoque absoluto/inválido, atualização/inativação e cliente proibido. |
| Compra | Valores exatos, campos/Location, proprietário, baixa e releitura; entrada estrita e ausência de efeitos inválidos. |
| Atomicidade | Falha no segundo produto por inatividade, falta ou estoque; estoque inicial e quantidade de pedidos preservados. |
| Isolamento | Listas próprias; outro cliente não consulta/cancela; mesmo 404 do ausente; ADMIN consulta. |
| Histórico/cancelamento | Preço preservado após reajuste/inativação; reposição uma vez; mesmo cancelledAt na repetição. |
| Concorrência | Três disputas independentes pela última unidade, sem retry; um 201/um 409, um pedido e estoque zero. Cancelamento simultâneo por proprietário/ADMIN restitui uma vez. |
| Filtros/resumo | Intervalo construído com createdAt retornados pela API, início exato incluído/fim exato excluído, cliente/status, paginação e cancelados excluídos da soma/contagem. |

26 execuções JUnit no conjunto inicial, incluindo casos parametrizados. Não há dependência de ordem entre métodos; JUnit executa métodos sequencialmente por padrão. Cada cenário cria seus produtos; filtros usam os instantes retornados pelo servidor, sem forjar relógio ou timestamps.

A prova determinística de contenção no banco pertence aos testes internos. A concorrência externa sincroniza os POSTs por barreira, com tokens já obtidos, e valida somente os efeitos pela API. Não demonstra todas as intercalações possíveis ou desempenho/carga. Checks SQL/validação de esquema também permanecem na suíte interna.

## Evidências e snapshot

target/evaluation-runs/<projeto>/ guarda application*.log, postgres.log, evaluation.log, surefire-reports e run.json/result.json no PowerShell ou run.txt/result.txt no Bash. Identificação inclui versão do avaliador, commit/estado da pasta, SHA256 do JAR e, quando presente, SHA256 de SNAPSHOT.sha256. Arquivos são locais/ignorados; falhas de build/testes não são tratadas como sucesso.

SNAPSHOT.sha256 registra os insumos versionáveis do avaliador e docs/ESPECIFICACAO.md, com caminhos relativos à raiz e LF. Não inclui a si mesmo ou relatórios. Conferência PowerShell a partir da raiz:

~~~powershell
Get-Content evaluation/SNAPSHOT.sha256 | ForEach-Object {
    $expected, $relative = $_ -split '  ', 2
    $actual = (Get-FileHash -LiteralPath $relative -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) { throw "Snapshot divergente: $relative" }
}
~~~

Bash/Linux:

~~~sh
sha256sum -c evaluation/SNAPSHOT.sha256
~~~

É um snapshot candidato à revisão, sem commit/tag feito pelo Codex. Mudança posterior em contrato/fixture/assertions exige registro em docs/DECISOES.md, revisão e nova versão/snapshot antes do experimento. Não ajustar expectativas silenciosamente ao resultado de uma migração. O avaliador e suas evidências ficam fora do pacote entregue ao agente de migração.
