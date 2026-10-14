param(
    [string]$Jdk = '',
    [ValidateSet('build','clean','packageRelease','packageProject')][string[]]$Task = @('build'),
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $Jdk) {
    $bundled = Join-Path $projectRoot '.toolchains/jdk17'
    if (Test-Path -LiteralPath $bundled) {
        $candidate = Get-ChildItem -LiteralPath $bundled -Directory | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin/javac.exe') } | Select-Object -First 1
        if ($candidate) { $Jdk = $candidate.FullName }
    }
    if (-not $Jdk) { $Jdk = $env:JAVA_HOME }
}
if (-not $Jdk -or -not (Test-Path -LiteralPath (Join-Path $Jdk 'bin/javac.exe'))) { throw '请通过 -Jdk 指定完整 JDK 17。' }
if (-not (Select-String -LiteralPath (Join-Path $Jdk 'release') -Pattern '^JAVA_VERSION="17([.\-" ]|$)' -Quiet)) { throw '此工程使用完整 JDK 17。' }
$previousJava = $env:JAVA_HOME
Push-Location $projectRoot
try {
    $env:JAVA_HOME = (Resolve-Path -LiteralPath $Jdk).Path
    $gradleArguments = @($Task) + @('--console=plain')
    if ($Offline) { $gradleArguments += '--offline' }
    & (Join-Path $projectRoot 'gradlew.bat') @gradleArguments
    if ($LASTEXITCODE -ne 0) { throw "Gradle 构建失败：$LASTEXITCODE" }
} finally {
    $env:JAVA_HOME = $previousJava
    Pop-Location
}
