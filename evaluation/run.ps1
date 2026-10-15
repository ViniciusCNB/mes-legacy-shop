param(
    [ValidateRange(1024, 65535)][int]$HttpPort = 18080,
    [string]$ApplicationJar
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
if (!$env:JAVA_HOME) { throw 'Defina JAVA_HOME para um JDK 17.' }
$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
if (!(Test-Path -LiteralPath $java)) { throw "Java não encontrado: $java" }
$javaVersion = (& $java -version 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch 'version "17\.') {
    throw 'JAVA_HOME deve selecionar JDK 17; não alterar a stack para executar a avaliação.'
}
if (!$ApplicationJar) { $ApplicationJar = Join-Path $repo 'target/mes-legacy-shop-0.0.1-SNAPSHOT.jar' }
$ApplicationJar = (Resolve-Path -LiteralPath $ApplicationJar).Path
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $HttpPort)
try { $listener.Start() } finally { $listener.Stop() }

$project = 'mes-evaluation-' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
$compose = @('compose', '-f', (Join-Path $PSScriptRoot 'compose.yaml'), '-p', $project)
$artifacts = Join-Path $repo "target/evaluation-runs/$project"
[IO.Directory]::CreateDirectory($artifacts) | Out-Null
$application = $null
$ownsProject = $false
$passed = $false
$evaluationStarted = $false
Push-Location $repo
try {
    $existing = & docker ps -aq --filter "label=com.docker.compose.project=$project"
    if ($LASTEXITCODE -ne 0) { throw 'Docker indisponível; nenhuma avaliação executada.' }
    if ($existing) { throw "Projeto já existe: $project" }
    $existingVolumes = & docker volume ls -q --filter "label=com.docker.compose.project=$project"
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível inspecionar volumes.' }
    if ($existingVolumes) { throw "Volume já existe para $project" }
    $ownsProject = $true
    & docker @compose up -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao iniciar PostgreSQL isolado.' }
    $binding = & docker @compose port postgres 5432
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao descobrir a porta PostgreSQL.' }
    $databasePort = [int](($binding.Trim() -split ':')[-1])
    Get-Content (Join-Path $PSScriptRoot 'fixtures/seed.sql') -Raw |
        & docker @compose exec -T postgres psql -v ON_ERROR_STOP=1 -U shop_evaluation -d shop_evaluation
    if ($LASTEXITCODE -ne 0) { throw 'Falha na fixture; não apagar nem ignorar o erro.' }

    $variables = @{
        POSTGRES_HOST='127.0.0.1'; POSTGRES_PORT=[string]$databasePort
        POSTGRES_DB='shop_evaluation'; POSTGRES_USER='shop_evaluation'
        POSTGRES_PASSWORD='evaluation_local_only'; SERVER_PORT=[string]$HttpPort
        SPRING_PROFILES_ACTIVE='evaluation'
    }
    $saved = @{}
    foreach ($name in $variables.Keys) {
        $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
        [Environment]::SetEnvironmentVariable($name, $variables[$name], 'Process')
    }
    try {
        $start = @{
            FilePath=$java; ArgumentList=@('-jar', ('"' + $ApplicationJar + '"'))
            WorkingDirectory=$repo; WindowStyle='Hidden'; PassThru=$true
            RedirectStandardOutput=(Join-Path $artifacts 'application.log')
            RedirectStandardError=(Join-Path $artifacts 'application-error.log')
        }
        $application = Start-Process @start
    } finally {
        foreach ($name in $saved.Keys) {
            [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process')
        }
    }
    $baseUrl = "http://127.0.0.1:$HttpPort"
    $ready = $false
    $deadline = [DateTime]::UtcNow.AddSeconds(90)
    while ([DateTime]::UtcNow -lt $deadline) {
        $application.Refresh()
        if ($application.HasExited) { throw 'A aplicação encerrou; consulte application*.log.' }
        try {
            $health = Invoke-RestMethod "$baseUrl/actuator/health" -TimeoutSec 2
            if ($health.status -eq 'UP') { $ready = $true; break }
        } catch { }
        Start-Sleep -Milliseconds 500
    }
    if (!$ready) { throw 'Saúde não ficou UP em 90s; consulte os logs.' }
    @{
        project=$project; baseUrl=$baseUrl; databasePort=$databasePort; javaVersion=$javaVersion
        applicationJar=$ApplicationJar
        applicationSha256=(Get-FileHash -LiteralPath $ApplicationJar -Algorithm SHA256).Hash.ToLowerInvariant()
        sourceCommit=((& git rev-parse HEAD) -join '').Trim()
        sourceDirty=([bool](& git status --porcelain))
        evaluatorVersion='1.0.0'
        evaluatorSnapshotSha256=$(if (Test-Path 'evaluation/SNAPSHOT.sha256') { (Get-FileHash 'evaluation/SNAPSHOT.sha256' -Algorithm SHA256).Hash.ToLowerInvariant() } else { $null })
        startedAt=[DateTime]::UtcNow.ToString('o')
    } | ConvertTo-Json | Set-Content (Join-Path $artifacts 'run.json') -Encoding UTF8

    $evaluationStarted = $true
    & (Join-Path $repo 'mvnw.cmd') -B -ntp -f evaluation/pom.xml clean verify "-Devaluation.baseUrl=$baseUrl" *> (Join-Path $artifacts 'evaluation.log')
    $mavenResult = $LASTEXITCODE
    Get-Content (Join-Path $artifacts 'evaluation.log') -Tail 35
    if ($mavenResult -ne 0) { throw "Avaliador falhou (exit $mavenResult); consulte $artifacts" }
    $passed = $true
} finally {
    if ($evaluationStarted -and (Test-Path (Join-Path $PSScriptRoot 'target/surefire-reports'))) {
        try {
            Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'target/surefire-reports') -Destination (Join-Path $artifacts 'surefire-reports') -Recurse
        } catch { Write-Warning "Falha ao copiar relatórios: $_"; $passed = $false }
    }
    if ($application) {
        $application.Refresh()
        if (!$application.HasExited) { Stop-Process -InputObject $application }
    }
    if ($ownsProject) {
        if ($project -notmatch '^mes-evaluation-[0-9a-f]{12}$') { throw 'Identidade de limpeza inválida.' }
        & docker @compose logs --no-color *> (Join-Path $artifacts 'postgres.log')
        & docker @compose down --volumes
        if ($LASTEXITCODE -ne 0) { throw "Falha ao limpar somente $project; preserve os logs." }
    }
    @{ passed=$passed; finishedAt=[DateTime]::UtcNow.ToString('o') } |
        ConvertTo-Json | Set-Content (Join-Path $artifacts 'result.json') -Encoding UTF8
    Pop-Location
    Write-Output "Evidências: $artifacts"
}

if (!$passed) { throw "Avaliação não concluída; consulte $artifacts" }
