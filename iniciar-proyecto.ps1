[CmdletBinding()]
param(
    [switch]$SinDocker,
    [switch]$SinEntrenarIA,
    [switch]$AbrirNavegador
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Run = Join-Path $Root '.run'
New-Item -ItemType Directory -Force -Path $Run | Out-Null

# Algunos terminales heredan simultaneamente las variantes Path y PATH.
# Windows no las distingue, pero Start-Process de PowerShell 5 puede rechazarlas.
$ProcessPath = $env:Path
[Environment]::SetEnvironmentVariable('Path', $null, 'Process')
[Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
[Environment]::SetEnvironmentVariable('Path', $ProcessPath, 'Process')

function Test-TutorSystemReady {
    try {
        $frontend = Invoke-WebRequest 'http://127.0.0.1:5173/' -UseBasicParsing -TimeoutSec 1
        $backend = Invoke-RestMethod 'http://127.0.0.1:8080/actuator/health' -TimeoutSec 1
        $ia = Invoke-RestMethod 'http://127.0.0.1:8001/health' -TimeoutSec 1
        return $frontend.StatusCode -eq 200 -and $backend.status -eq 'UP' -and $ia.status -eq 'UP'
    } catch {
        return $false
    }
}

if (Test-TutorSystemReady) {
    Write-Host 'Tutor Inteligente ya esta ejecutandose.' -ForegroundColor Green
    if ($AbrirNavegador) { Start-Process 'http://127.0.0.1:5173/' }
    exit 0
}

$PidFile = Join-Path $Run 'pids.txt'
if (Test-Path $PidFile) {
    & (Join-Path $Root 'detener-proyecto.ps1')
    Start-Sleep -Seconds 1
}

function Require-Command([string]$Name, [string]$Hint) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Falta '$Name'. $Hint"
    }
}

Require-Command java 'Instala JDK 21.'
Require-Command node 'Instala Node.js 20 o superior.'
Require-Command npm.cmd 'Instala npm junto con Node.js.'

function Test-PythonRuntime([string]$Executable, [string[]]$PrefixArgs) {
    try {
        & $Executable @PrefixArgs -c 'import fastapi, uvicorn, pandas, numpy, sklearn, joblib' 2>$null
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

$PythonExecutable = $null
$PythonPrefixArgs = @()
$PythonCandidates = @()
$LocalPython = Join-Path $Root 'ia\.venv\Scripts\python.exe'
$CodexPython = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'

if (Test-Path $LocalPython) {
    $PythonCandidates += [pscustomobject]@{ Executable = $LocalPython; PrefixArgs = @(); Name = 'entorno local' }
}
$PythonCommand = Get-Command python.exe -ErrorAction SilentlyContinue
if ($PythonCommand) {
    $PythonCandidates += [pscustomobject]@{ Executable = $PythonCommand.Source; PrefixArgs = @(); Name = 'Python del sistema' }
}
$PyCommand = Get-Command py.exe -ErrorAction SilentlyContinue
if ($PyCommand) {
    $PythonCandidates += [pscustomobject]@{ Executable = $PyCommand.Source; PrefixArgs = @('-3'); Name = 'Python Launcher' }
}
if (Test-Path $CodexPython) {
    $PythonCandidates += [pscustomobject]@{ Executable = $CodexPython; PrefixArgs = @(); Name = 'runtime Python disponible' }
}

foreach ($Candidate in $PythonCandidates) {
    if (Test-PythonRuntime $Candidate.Executable $Candidate.PrefixArgs) {
        $PythonExecutable = $Candidate.Executable
        $PythonPrefixArgs = $Candidate.PrefixArgs
        Write-Host "IA: se usara $($Candidate.Name)." -ForegroundColor DarkGray
        break
    }
}

if (-not $PythonExecutable) {
    $BootstrapPython = $PythonCandidates | Select-Object -First 1
    if (-not $BootstrapPython) {
        throw 'No se encontro Python. Instala Python 3.11 o superior y vuelve a ejecutar el lanzador.'
    }

    Write-Host 'Preparando por primera vez el entorno de IA...' -ForegroundColor Cyan
    $BootstrapExecutable = $BootstrapPython.Executable
    $BootstrapPrefixArgs = @($BootstrapPython.PrefixArgs)
    & $BootstrapExecutable @BootstrapPrefixArgs -m venv (Join-Path $Root 'ia\.venv')
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $LocalPython)) {
        throw 'No se pudo crear el entorno de Python para la IA.'
    }
    & $LocalPython -m pip install --disable-pip-version-check -r (Join-Path $Root 'ia\requirements.txt')
    if ($LASTEXITCODE -ne 0 -or -not (Test-PythonRuntime $LocalPython @())) {
        throw 'No se pudieron instalar las dependencias de IA. Comprueba tu conexion a Internet y vuelve a intentarlo.'
    }
    $PythonExecutable = $LocalPython
    $PythonPrefixArgs = @()
    Write-Host 'Entorno de IA preparado.' -ForegroundColor Green
}

$NpmExecutable = (Get-Command npm.cmd).Source
$FrontendModules = Join-Path $Root 'frontend\node_modules\vite\bin\vite.js'
if (-not (Test-Path $FrontendModules)) {
    Write-Host 'Instalando por primera vez las dependencias del frontend...' -ForegroundColor Cyan
    Push-Location (Join-Path $Root 'frontend')
    try {
        & $NpmExecutable install --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) {
            throw 'npm no pudo instalar las dependencias.'
        }
    } finally {
        Pop-Location
    }
    Write-Host 'Frontend preparado.' -ForegroundColor Green
}

$BackendJar = Join-Path $Root 'backend\target\tutor-inteligente-backend-1.0.0.jar'
$MavenAvailable = [bool](Get-Command mvn -ErrorAction SilentlyContinue)
if (-not $MavenAvailable -and -not (Test-Path $BackendJar)) {
    throw 'Falta Maven y tampoco existe el JAR precompilado del backend.'
}

if (-not (Test-Path (Join-Path $Root '.env'))) {
    function New-RandomBase64([int]$ByteCount) {
        $bytes = New-Object byte[] $ByteCount
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        [Convert]::ToBase64String($bytes)
    }

    # En modo local se solicita la clave durante el primer inicio y se conserva
    # únicamente en .env. Docker usa una contraseña aleatoria.
    if ($SinDocker) {
        $secureDbPassword = Read-Host 'Contraseña del usuario sa de SQL Server' -AsSecureString
        $dbPassword = [System.Net.NetworkCredential]::new('', $secureDbPassword).Password
        if ([string]::IsNullOrWhiteSpace($dbPassword)) {
            throw 'La contraseña de SQL Server es obligatoria para crear .env.'
        }
    } else {
        $dbPassword = 'Aa1!' + ((New-RandomBase64 18) -replace '[+/=]', 'X')
    }
    $jwtSecret = New-RandomBase64 48
    @"
# Generado automaticamente. No confirmes este archivo en Git.
DB_HOST=localhost
DB_PORT=1433
DB_NAME=TutorInteligente
DB_USER=sa
DB_PASSWORD=$dbPassword
JWT_SECRET=$jwtSecret
AI_SERVICE_URL=http://localhost:8001
VITE_API_URL=http://localhost:8080/api
"@ | Set-Content (Join-Path $Root '.env') -Encoding utf8
    Write-Host 'Se creo .env con la configuracion local de SQL Server. No confirmes este archivo en Git.' -ForegroundColor Yellow
}

Get-Content (Join-Path $Root '.env') | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}

if ($SinDocker) {
    Write-Host 'Comprobando SQL Server local y la base TutorInteligente...' -ForegroundColor Cyan
    $connectionString = "Server=$($env:DB_HOST),$($env:DB_PORT);Database=master;User ID=$($env:DB_USER);Password=$($env:DB_PASSWORD);Encrypt=True;TrustServerCertificate=True;Connection Timeout=5"
    $databaseReady = $false
    $lastDatabaseError = $null

    for ($i = 0; $i -lt 6 -and -not $databaseReady; $i++) {
        $connection = New-Object System.Data.SqlClient.SqlConnection $connectionString
        try {
            $connection.Open()
            $command = $connection.CreateCommand()
            $command.CommandText = @"
IF DB_ID(@databaseName) IS NULL
BEGIN
    DECLARE @sql nvarchar(max) = N'CREATE DATABASE ' + QUOTENAME(@databaseName);
    EXEC sp_executesql @sql;
END
"@
            [void]$command.Parameters.Add('@databaseName', [System.Data.SqlDbType]::NVarChar, 128)
            $command.Parameters['@databaseName'].Value = $env:DB_NAME
            [void]$command.ExecuteNonQuery()
            $databaseReady = $true
        } catch {
            $lastDatabaseError = $_.Exception.Message
            if ($i -lt 5) { Start-Sleep -Seconds 2 }
        } finally {
            $connection.Dispose()
        }
    }

    if (-not $databaseReady) {
        throw "No se pudo conectar a SQL Server con $($env:DB_USER) en $($env:DB_HOST):$($env:DB_PORT). Verifica que el servicio este iniciado, que la autenticacion SQL este habilitada y que la clave sea correcta. Detalle: $lastDatabaseError"
    }
    Write-Host "Base $($env:DB_NAME) lista; Flyway aplicara las migraciones automaticamente." -ForegroundColor Green
} else {
    Require-Command docker 'Instala Docker Desktop o ejecuta el script con -SinDocker y usa tu SQL Server local.'
    & docker compose -f (Join-Path $Root 'docker-compose.yml') up -d sqlserver
    Write-Host 'Esperando a SQL Server 2019...' -ForegroundColor Cyan
    $ready = $false
    for ($i = 0; $i -lt 30 -and -not $ready; $i++) {
        Start-Sleep -Seconds 2
        & docker exec tutor-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P $env:DB_PASSWORD -C -Q "SELECT 1" 2>$null | Out-Null
        $ready = $LASTEXITCODE -eq 0
    }
    if (-not $ready) { throw 'SQL Server no respondio dentro del tiempo esperado.' }
    & docker exec tutor-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P $env:DB_PASSWORD -C -Q "IF DB_ID(N'TutorInteligente') IS NULL CREATE DATABASE TutorInteligente"
}

if (-not $SinEntrenarIA -or -not (Test-Path (Join-Path $Root 'ia\models\decision_tree_tutor.joblib'))) {
    Push-Location (Join-Path $Root 'ia')
    & $PythonExecutable @PythonPrefixArgs train_model.py
    Pop-Location
}

$processes = @()
$IaArguments = @($PythonPrefixArgs) + @('-m','uvicorn','app.main:app','--host','127.0.0.1','--port','8001')
$processes += Start-Process $PythonExecutable -ArgumentList $IaArguments -WorkingDirectory (Join-Path $Root 'ia') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $Run 'ia.log') -RedirectStandardError (Join-Path $Run 'ia.err.log') -PassThru
if ($MavenAvailable) {
    $processes += Start-Process mvn -ArgumentList 'spring-boot:run' -WorkingDirectory (Join-Path $Root 'backend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $Run 'backend.log') -RedirectStandardError (Join-Path $Run 'backend.err.log') -PassThru
} else {
    Write-Host 'Maven no esta en PATH; se usara el JAR verificado incluido.' -ForegroundColor Yellow
    $processes += Start-Process java -ArgumentList '-jar',$BackendJar -WorkingDirectory (Join-Path $Root 'backend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $Run 'backend.log') -RedirectStandardError (Join-Path $Run 'backend.err.log') -PassThru
}
$processes += Start-Process $NpmExecutable -ArgumentList 'run','dev' -WorkingDirectory (Join-Path $Root 'frontend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $Run 'frontend.log') -RedirectStandardError (Join-Path $Run 'frontend.err.log') -PassThru

$processes.Id | Set-Content (Join-Path $Run 'pids.txt')
Write-Host ''
Write-Host 'Tutor Inteligente iniciado:' -ForegroundColor Green
Write-Host '  Frontend: http://127.0.0.1:5173'
Write-Host '  Backend:  http://127.0.0.1:8080'
Write-Host '  Swagger:  http://127.0.0.1:8080/swagger-ui.html'
Write-Host '  IA:       http://127.0.0.1:8001/docs'
Write-Host "Registros: $Run"

if ($AbrirNavegador) {
    Write-Host 'Esperando a que el sistema este listo...' -ForegroundColor Cyan
    $ready = $false
    for ($i = 0; $i -lt 60 -and -not $ready; $i++) {
        $ready = Test-TutorSystemReady
        if (-not $ready) { Start-Sleep -Seconds 1 }
    }

    if ($ready) {
        Write-Host 'Sistema listo. Abriendo la aplicacion...' -ForegroundColor Green
        Start-Process 'http://127.0.0.1:5173/'
    } else {
        Write-Warning "Los procesos se iniciaron, pero alguno no respondio a tiempo. Revisa los registros en $Run"
    }
}
