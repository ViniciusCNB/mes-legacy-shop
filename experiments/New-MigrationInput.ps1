#Requires -Version 7.0
[CmdletBinding()]
param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '../.experiment-local/input')
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$baseline = '1e8ea40939d92023df8dab81b64506c8f83a9ca6'
$target = '3.5.16'
$baseName = "mes-legacy-shop-boot-$target-input"
$output = [IO.Path]::GetFullPath($OutputDirectory)
$archive = Join-Path $output "$baseName.zip"
$manifest = Join-Path $output "$baseName.manifest.json"
$partial = Join-Path $output ".$baseName-$([Guid]::NewGuid().ToString('N')).zip"
$partialManifest = "$partial.json"
$utf8 = [Text.UTF8Encoding]::new($false)

[IO.Directory]::CreateDirectory($output) | Out-Null
if ((Test-Path -LiteralPath $archive) -or (Test-Path -LiteralPath $manifest)) {
    throw "Saída já existe em $output. Use outro diretório para preservar a evidência."
}

$committedAt = & git -C $repo show -s --format=%cI $baseline
if ($LASTEXITCODE -ne 0) { throw "Baseline indisponível: $baseline" }
$entryDate = [DateTimeOffset]::Parse($committedAt)
$tracked = @('.env.example', '.gitattributes', '.gitignore',
    '.mvn/wrapper/maven-wrapper.properties', 'compose.yaml', 'mvnw', 'mvnw.cmd', 'pom.xml',
    'src', 'docs/ESPECIFICACAO.md')

function Write-ZipText($zip, [string]$path, [string]$content) {
    $existing = $zip.GetEntry($path)
    if ($existing) { $existing.Delete() }
    $entry = $zip.CreateEntry($path, [IO.Compression.CompressionLevel]::Optimal)
    $entry.LastWriteTime = $entryDate
    $writer = [IO.StreamWriter]::new($entry.Open(), $utf8)
    try { $writer.Write($content.Replace([string][char]13,'')) } finally { $writer.Dispose() }
}

try {
    & git -C $repo archive --format=zip "--output=$partial" $baseline -- @tracked
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao exportar os arquivos da baseline.' }

    $zip = [IO.Compression.ZipFile]::Open($partial, [IO.Compression.ZipArchiveMode]::Update)
    try {
        $entry = $zip.GetEntry('docs/ESPECIFICACAO.md')
        $reader = [IO.StreamReader]::new($entry.Open(), $utf8)
        try { $contract = $reader.ReadToEnd() } finally { $reader.Dispose() }
        $sections = $contract -split '(?m)^## Consolidação — Fase 5\r?$', 2
        if ($sections.Count -ne 2) { throw 'Seção privada da especificação não localizada.' }
        # Preserve the public rules; omit project history and the evaluation setup.
        $contract = [regex]::Replace($sections[0],
            '(?m)^(?:As Fases 1 a 4 implementam|Na Fase 5, evaluation/|Autorização do autor em|Autorizado pelo autor após)[^\r\n]*\r?\n', '')
        $contract = [regex]::Replace($contract, '(?m)^## Estado do contrato\r?\n(?:\r?\n)*', '')
        $contract = $contract.Replace('## Verificações e avaliação futura', '## Verificações de comportamento')
        $contract = $contract.Replace(' Há testes de conexão, catálogo HTTP, persistência e rejeição de esquema divergente; os resultados efetivos constam de STATUS.md.', '')
        $contract = $contract.Replace('; sua consolidação para avaliação externa pertence à Fase 5.', '.')
        Write-ZipText $zip 'docs/ESPECIFICACAO.md' $contract
        foreach ($name in @('AGENTS.md', 'README.md', 'prompt.txt')) {
            Write-ZipText $zip $name ([IO.File]::ReadAllText((Join-Path $PSScriptRoot "input/$name"), $utf8))
        }

        $files = @(foreach ($entry in ($zip.Entries | Where-Object Name | Sort-Object FullName)) {
            $stream = $entry.Open()
            try {
                [ordered]@{ path = $entry.FullName; sha256 = (Get-FileHash -InputStream $stream).Hash.ToLowerInvariant() }
            } finally { $stream.Dispose() }
        })
    } finally { $zip.Dispose() }

    $record = [ordered]@{
        protocol = 'mes-boot35-v1'
        baseline_commit = $baseline
        spring_boot_from = '2.7.18'
        spring_boot_to = $target
        archive = [IO.Path]::GetFileName($archive)
        archive_sha256 = (Get-FileHash -LiteralPath $partial).Hash.ToLowerInvariant()
        files = $files
    }
    [IO.File]::WriteAllText($partialManifest, ($record | ConvertTo-Json -Depth 4) + [char]10, $utf8)
    [IO.File]::Move($partial, $archive)
    [IO.File]::Move($partialManifest, $manifest)
    [pscustomobject]@{ Archive = $archive; Manifest = $manifest; Files = $files.Count; Sha256 = $record.archive_sha256 }
} finally {
    foreach ($file in @($partial, $partialManifest)) {
        if (Test-Path -LiteralPath $file) { Remove-Item -LiteralPath $file }
    }
}
