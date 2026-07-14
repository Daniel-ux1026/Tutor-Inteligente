$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

Push-Location (Join-Path $Root 'ia')
python -m unittest discover -s tests -v
Pop-Location

Push-Location (Join-Path $Root 'frontend')
npm test
npm run build
Pop-Location

Push-Location (Join-Path $Root 'backend')
mvn test
Pop-Location

Write-Host 'Todas las verificaciones automatizadas finalizaron.' -ForegroundColor Green
