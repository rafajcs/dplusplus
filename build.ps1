# build.ps1 - script de build do compilador D++
#
# Uso:
#   .\build.ps1                       compila e copia os recursos .dat para bin/
#   .\build.ps1 run teste/fatorial.dpp   compila e executa o Main no arquivo dado
#   .\build.ps1 gen                   regenera lexer/parser/nos com o SableCC
#   .\build.ps1 clean                 remove o diretorio bin/
#   .\build.ps1 all teste/fatorial.dpp   gen + compila + executa
#
# O SableCC e localizado pela variavel de ambiente SABLECC_JAR ou por um
# sablecc.jar na raiz do projeto. A geracao so roda se o jar for encontrado.

param(
    [string]$Comando = "build",
    [string]$Arquivo = "teste/LinkedList.dpp"
)

$ErrorActionPreference = "Stop"
$raiz = $PSScriptRoot
$src  = Join-Path $raiz "src"
$bin  = Join-Path $raiz "bin"
$grammar = Join-Path $src "grupo_18.sable"

function Find-Sablecc {
    if ($env:SABLECC_JAR -and (Test-Path $env:SABLECC_JAR)) { return $env:SABLECC_JAR }
    $local = Join-Path $raiz "sablecc.jar"
    if (Test-Path $local) { return $local }
    return $null
}

function Invoke-Gen {
    $jar = Find-Sablecc
    if (-not $jar) {
        Write-Host "sablecc.jar nao encontrado. Defina SABLECC_JAR ou coloque sablecc.jar na raiz." -ForegroundColor Yellow
        Write-Host "Pulando geracao; usando o parser ja gerado em src/." -ForegroundColor Yellow
        return
    }
    Write-Host "Gerando lexer/parser com SableCC ($jar)..." -ForegroundColor Cyan
    & java -jar $jar -d $src $grammar
    if ($LASTEXITCODE -ne 0) { throw "Falha na geracao do SableCC." }
    Write-Host "Geracao concluida." -ForegroundColor Green
}

function Invoke-Build {
    if (-not (Test-Path $bin)) { New-Item -ItemType Directory -Path $bin | Out-Null }

    Write-Host "Compilando fontes Java..." -ForegroundColor Cyan
    $fontes = Get-ChildItem -Path $src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
    & javac -d $bin -cp $src $fontes
    if ($LASTEXITCODE -ne 0) { throw "Falha na compilacao Java." }

    # Copia os recursos .dat preservando a estrutura de pacotes (necessarios em runtime).
    Write-Host "Copiando recursos .dat..." -ForegroundColor Cyan
    Get-ChildItem -Path $src -Recurse -Filter *.dat | ForEach-Object {
        $relativo = $_.FullName.Substring($src.Length).TrimStart('\','/')
        $destino  = Join-Path $bin $relativo
        $pastaDst = Split-Path $destino -Parent
        if (-not (Test-Path $pastaDst)) { New-Item -ItemType Directory -Path $pastaDst -Force | Out-Null }
        Copy-Item $_.FullName $destino -Force
    }
    Write-Host "Build concluido em $bin" -ForegroundColor Green
}

function Invoke-Run {
    param([string]$Alvo)
    Write-Host "Executando: $Alvo" -ForegroundColor Cyan
    & java -cp $bin dplusplus.Main $Alvo
}

function Invoke-Clean {
    if (Test-Path $bin) {
        Remove-Item -Recurse -Force $bin
        Write-Host "Removido $bin" -ForegroundColor Green
    } else {
        Write-Host "Nada para limpar." -ForegroundColor Yellow
    }
}

switch ($Comando.ToLower()) {
    "build" { Invoke-Build }
    "gen"   { Invoke-Gen }
    "run"   { Invoke-Build; Invoke-Run -Alvo $Arquivo }
    "all"   { Invoke-Gen; Invoke-Build; Invoke-Run -Alvo $Arquivo }
    "clean" { Invoke-Clean }
    default {
        Write-Host "Comando desconhecido: $Comando" -ForegroundColor Red
        Write-Host "Use: build | run <arquivo.dpp> | gen | all <arquivo.dpp> | clean"
    }
}
