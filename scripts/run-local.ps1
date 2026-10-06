param(
    [Parameter(Position = 0)]
    [ValidateSet("8080", "8081")]
    [string]$Port = "8080"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env.$Port"

if (-not (Test-Path $envFile)) {
    Write-Error "缺少 $envFile"
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $eq = $line.IndexOf("=")
    if ($eq -lt 1) { return }
    $name = $line.Substring(0, $eq).Trim()
    $value = $line.Substring($eq + 1)
    Set-Item -Path "Env:$name" -Value $value
}

Write-Host "Using $envFile  SERVER_PORT=$Port"
Set-Location $root
mvn -DskipTests spring-boot:run
