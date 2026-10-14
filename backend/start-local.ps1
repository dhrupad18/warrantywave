$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
if (-not (Test-Path -LiteralPath $envFile)) {
    throw 'Missing project .env file. Copy .env.example to .env, then set the local database and admin credentials.'
}

Get-Content -LiteralPath $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#=\s]+)\s*=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim(), 'Process')
    }
}

$required = @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD', 'JWT_SECRET')
foreach ($name in $required) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, 'Process'))) {
        throw "$name must be set in .env."
    }
}
if ([Text.Encoding]::UTF8.GetByteCount($env:JWT_SECRET) -lt 32) {
    throw 'JWT_SECRET in .env must contain at least 32 UTF-8 bytes.'
}

Push-Location $projectRoot
try {
    & mvn -f backend/pom.xml spring-boot:run
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
