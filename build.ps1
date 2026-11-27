param([switch]$Testar, [switch]$Executar)
$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try {
function Ferramenta([string]$Nome) {
    if ($env:JAVA_HOME) {
        $candidato = Join-Path $env:JAVA_HOME "bin/$Nome.exe"
        if (Test-Path -LiteralPath $candidato) { return $candidato }
    }
    $comando = Get-Command $Nome -ErrorAction SilentlyContinue
    if (-not $comando) { throw "Instale um JDK 17 ou superior e configure JAVA_HOME ou PATH. Ferramenta ausente: $Nome" }
    return $comando.Source
}
$javac = Ferramenta 'javac'
$java = Ferramenta 'java'
$jar = Ferramenta 'jar'
New-Item -ItemType Directory -Force -Path build/classes | Out-Null
$fontes = @(Get-ChildItem -LiteralPath src -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& $javac --release 17 -encoding UTF-8 -d build/classes @fontes
if ($LASTEXITCODE -ne 0) { throw 'A compilação falhou.' }
& $jar --create --file build/clinica-viva.jar --main-class br.edu.ceub.clinica.Main -C build/classes .
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível criar o JAR.' }
Write-Output 'Projeto compilado: build/clinica-viva.jar'
if ($Testar) {
    New-Item -ItemType Directory -Force -Path build/test | Out-Null
    $testes = @(Get-ChildItem -LiteralPath test -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    & $javac --release 17 -encoding UTF-8 -cp build/classes -d build/test @testes
    if ($LASTEXITCODE -ne 0) { throw 'A compilação dos testes falhou.' }
    & $java '-Djava.awt.headless=true' -cp 'build/classes;build/test' br.edu.ceub.clinica.ClinicaTest
    if ($LASTEXITCODE -ne 0) { throw 'Os testes falharam.' }
}
if ($Executar) {
    & $java -jar build/clinica-viva.jar
    if ($LASTEXITCODE -ne 0) { throw 'A execução falhou.' }
}
} finally {
    Pop-Location
}
