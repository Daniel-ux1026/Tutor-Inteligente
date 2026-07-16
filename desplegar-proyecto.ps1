# Automatiza la verificacion y el despliegue seguro de los cuatro servicios.
[CmdletBinding()]
param(
    [string]$NombreProyecto = "Tutor-Inteligente",
    [switch]$SoloVerificar,
    [switch]$OmitirPruebas,
    [switch]$NoAbrirNavegador
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$script:Raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
$script:Npx = $null
$script:UltimoServicio = $null

Set-Location -LiteralPath $script:Raiz

function Write-Section {
    param([string]$Text)
    Write-Host ""
    Write-Host ("=" * 72) -ForegroundColor DarkCyan
    Write-Host $Text -ForegroundColor Cyan
    Write-Host ("=" * 72) -ForegroundColor DarkCyan
}

function Write-Ok {
    param([string]$Text)
    Write-Host "[OK] $Text" -ForegroundColor Green
}

function Write-InfoMessage {
    param([string]$Text)
    Write-Host "[INFO] $Text" -ForegroundColor Cyan
}

function Write-WarnMessage {
    param([string]$Text)
    Write-Host "[AVISO] $Text" -ForegroundColor Yellow
}

function Get-Executable {
    param([string[]]$Names)

    foreach ($name in $Names) {
        $command = Get-Command $name -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($command) {
            return $command.Source
        }
    }

    return $null
}

function Invoke-Native {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [string[]]$Arguments = @(),
        [string]$WorkingDirectory = $script:Raiz
    )

    Push-Location -LiteralPath $WorkingDirectory
    try {
        & $FilePath @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "El comando '$([IO.Path]::GetFileName($FilePath)) $($Arguments -join ' ')' termino con codigo $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }
}

function ConvertFrom-RailwayJson {
    param([Parameter(Mandatory = $true)][string]$Text)

    $trimmed = $Text.Trim()
    if (-not $trimmed) {
        return $null
    }

    try {
        return $trimmed | ConvertFrom-Json
    }
    catch {
        $objectStart = $trimmed.IndexOf("{")
        $objectEnd = $trimmed.LastIndexOf("}")
        if ($objectStart -ge 0 -and $objectEnd -gt $objectStart) {
            try {
                return $trimmed.Substring($objectStart, $objectEnd - $objectStart + 1) | ConvertFrom-Json
            }
            catch {
                # Se intenta como arreglo debajo.
            }
        }

        $arrayStart = $trimmed.IndexOf("[")
        $arrayEnd = $trimmed.LastIndexOf("]")
        if ($arrayStart -ge 0 -and $arrayEnd -gt $arrayStart) {
            return $trimmed.Substring($arrayStart, $arrayEnd - $arrayStart + 1) | ConvertFrom-Json
        }

        throw "Railway devolvio una respuesta que no se pudo interpretar."
    }
}

function Invoke-RailwayRaw {
    param(
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [switch]$AllowFailure,
        [switch]$Sensitive
    )

    $previousErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $script:Npx --yes "@railway/cli@latest" @Arguments 2>&1
        $exitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorAction
    }
    $text = (($output | ForEach-Object { $_.ToString() }) -join "`n").Trim()

    if ($exitCode -ne 0) {
        if ($AllowFailure) {
            return $null
        }

        if ($Sensitive) {
            throw "Railway rechazo una operacion con variables protegidas. No se mostraron sus valores."
        }

        if ($text) {
            throw "Railway: $text"
        }

        throw "Railway termino con codigo $exitCode."
    }

    return $text
}

function Invoke-RailwayJson {
    param(
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [switch]$AllowFailure,
        [switch]$Sensitive
    )

    $raw = Invoke-RailwayRaw -Arguments $Arguments -AllowFailure:$AllowFailure -Sensitive:$Sensitive
    if ($null -eq $raw) {
        return $null
    }

    return ConvertFrom-RailwayJson -Text $raw
}

function Set-RailwayVariable {
    param(
        [Parameter(Mandatory = $true)][string]$Service,
        [Parameter(Mandatory = $true)][string]$Key,
        [Parameter(Mandatory = $true)][string]$Value
    )

    $previousErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = $Value | & $script:Npx --yes "@railway/cli@latest" variable set $Key --stdin --service $Service --skip-deploys --json 2>&1
        $exitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorAction
    }

    if ($exitCode -ne 0) {
        $null = $output
        throw "No se pudo configurar '$Key' en el servicio '$Service'. El valor no se mostro."
    }
}

function Get-RailwayVariables {
    param([Parameter(Mandatory = $true)][string]$Service)

    $variables = Invoke-RailwayJson -Arguments @("variable", "list", "--service", $Service, "--json") -Sensitive
    if ($null -eq $variables) {
        return [pscustomobject]@{}
    }

    return $variables
}

function Get-PropertyValue {
    param(
        [Parameter(Mandatory = $true)]$Object,
        [Parameter(Mandatory = $true)][string]$Name
    )

    $property = $Object.PSObject.Properties[$Name]
    if ($property) {
        return [string]$property.Value
    }

    return $null
}

function New-RandomToken {
    param([int]$Bytes = 48)

    $buffer = New-Object byte[] $Bytes
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($buffer)
    }
    finally {
        $generator.Dispose()
    }

    return [Convert]::ToBase64String($buffer).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function Ensure-SecretVariable {
    param(
        [Parameter(Mandatory = $true)][string]$Service,
        [Parameter(Mandatory = $true)][string]$Key,
        [Parameter(Mandatory = $true)][scriptblock]$Generator
    )

    $variables = Get-RailwayVariables -Service $Service
    $current = Get-PropertyValue -Object $variables -Name $Key
    if ($current) {
        return $current
    }

    $value = & $Generator
    Set-RailwayVariable -Service $Service -Key $Key -Value $value
    return $value
}

function Get-ServiceItems {
    $data = Invoke-RailwayJson -Arguments @("service", "list", "--json")
    if ($null -eq $data) {
        return @()
    }

    if ($data.PSObject.Properties["services"]) {
        return @($data.services)
    }

    return @($data)
}

function Get-ServiceByName {
    param([Parameter(Mandatory = $true)][string]$Name)

    foreach ($service in (Get-ServiceItems)) {
        if ([string]$service.name -eq $Name) {
            return $service
        }
    }

    return $null
}

function Ensure-Service {
    param([Parameter(Mandatory = $true)][string]$Name)

    $service = Get-ServiceByName -Name $Name
    if ($service) {
        Write-Ok "Servicio '$Name' ya existe."
        return $service
    }

    $null = Invoke-RailwayJson -Arguments @("add", "--service", $Name, "--json")
    $service = Get-ServiceByName -Name $Name
    if (-not $service) {
        throw "Railway creo '$Name', pero no fue posible volver a localizarlo."
    }

    Write-Ok "Servicio '$Name' creado."
    return $service
}

function Find-RailwayDomain {
    param([string]$Text)

    if (-not $Text) {
        return $null
    }

    $match = [regex]::Match($Text, "(?i)([a-z0-9][a-z0-9-]*(?:\.[a-z0-9-]+)*\.up\.railway\.app)")
    if ($match.Success) {
        return $match.Groups[1].Value.ToLowerInvariant()
    }

    return $null
}

function Ensure-PublicDomain {
    param(
        [Parameter(Mandatory = $true)][string]$Service,
        [Parameter(Mandatory = $true)][int]$Port
    )

    $raw = Invoke-RailwayRaw -Arguments @("domain", "list", "--service", $Service, "--json") -AllowFailure
    $domain = Find-RailwayDomain -Text $raw
    if ($domain) {
        Write-Ok "Dominio de '$Service': https://$domain"
        return $domain
    }

    $raw = Invoke-RailwayRaw -Arguments @("domain", "--service", $Service, "--port", [string]$Port, "--json")
    $domain = Find-RailwayDomain -Text $raw
    if (-not $domain) {
        $raw = Invoke-RailwayRaw -Arguments @("domain", "list", "--service", $Service, "--json")
        $domain = Find-RailwayDomain -Text $raw
    }

    if (-not $domain) {
        throw "No se pudo determinar el dominio publico de '$Service'."
    }

    Write-Ok "Dominio de '$Service' creado: https://$domain"
    return $domain
}

function Warn-IfPublicDomainExists {
    param([Parameter(Mandatory = $true)][string]$Service)

    $raw = Invoke-RailwayRaw -Arguments @("domain", "list", "--service", $Service, "--json") -AllowFailure
    $domain = Find-RailwayDomain -Text $raw
    if ($domain) {
        Write-WarnMessage "El servicio privado '$Service' ya tiene el dominio https://$domain. Eliminalo desde Railway para mantenerlo privado."
    }
}

function Configure-ServiceSettings {
    $settings = @(
        @("sqlserver", "build.builder", "DOCKERFILE"),
        @("sqlserver", "build.dockerfilePath", "database/Dockerfile"),
        @("sqlserver", "deploy.restartPolicyType", "ALWAYS"),

        @("ia", "build.builder", "DOCKERFILE"),
        @("ia", "build.dockerfilePath", "ia/Dockerfile"),
        @("ia", "deploy.healthcheckPath", "/health"),
        @("ia", "deploy.healthcheckTimeout", "180"),
        @("ia", "deploy.restartPolicyType", "ON_FAILURE"),
        @("ia", "deploy.restartPolicyMaxRetries", "5"),

        @("backend", "build.builder", "DOCKERFILE"),
        @("backend", "build.dockerfilePath", "backend/Dockerfile"),
        @("backend", "deploy.healthcheckPath", "/actuator/health"),
        @("backend", "deploy.healthcheckTimeout", "300"),
        @("backend", "deploy.restartPolicyType", "ON_FAILURE"),
        @("backend", "deploy.restartPolicyMaxRetries", "10"),
        @("backend", "deploy.drainingSeconds", "20"),

        @("frontend", "build.builder", "DOCKERFILE"),
        @("frontend", "build.dockerfilePath", "frontend/Dockerfile"),
        @("frontend", "deploy.healthcheckPath", "/health"),
        @("frontend", "deploy.healthcheckTimeout", "120"),
        @("frontend", "deploy.restartPolicyType", "ON_FAILURE"),
        @("frontend", "deploy.restartPolicyMaxRetries", "5")
    )

    $arguments = @("environment", "edit", "--message", "Configura despliegue seguro de Tutor Inteligente")
    foreach ($setting in $settings) {
        $arguments += @("--service-config", $setting[0], $setting[1], $setting[2])
    }
    $arguments += "--json"

    $null = Invoke-RailwayRaw -Arguments $arguments
    Write-Ok "Dockerfiles, health checks y reinicios configurados."
}

function Ensure-SqlVolume {
    param([Parameter(Mandatory = $true)]$SqlService)

    $raw = Invoke-RailwayRaw -Arguments @("volume", "list", "--json") -AllowFailure
    $serviceId = [string]$SqlService.id
    $hasVolume = $false

    if ($raw) {
        if ($raw -match [regex]::Escape("/var/opt/mssql")) {
            $hasVolume = $true
        }
        elseif ($serviceId -and $raw -match [regex]::Escape($serviceId)) {
            $hasVolume = $true
        }
    }

    if ($hasVolume) {
        Write-Ok "El volumen persistente de SQL Server ya existe."
        return
    }

    if (-not $serviceId) {
        throw "No se pudo obtener el identificador del servicio sqlserver."
    }

    $null = Invoke-RailwayRaw -Arguments @("volume", "add", "--service", $serviceId, "--mount-path", "/var/opt/mssql", "--json")
    Write-Ok "Volumen persistente creado en /var/opt/mssql."
}

function Get-LatestDeploymentStatus {
    param([Parameter(Mandatory = $true)][string]$Service)

    $data = Invoke-RailwayJson -Arguments @("deployment", "list", "--service", $Service, "--limit", "1", "--json") -AllowFailure
    if ($null -eq $data) {
        return $null
    }

    $items = @()
    if ($data.PSObject.Properties["deployments"]) {
        $items = @($data.deployments)
    }
    else {
        $items = @($data)
    }

    if ($items.Count -eq 0) {
        return $null
    }

    $statusProperty = $items[0].PSObject.Properties["status"]
    if ($statusProperty) {
        return ([string]$statusProperty.Value).ToUpperInvariant()
    }

    return $null
}

function Wait-RailwayDeployment {
    param(
        [Parameter(Mandatory = $true)][string]$Service,
        [int]$TimeoutSeconds = 900
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastStatus = ""

    while ((Get-Date) -lt $deadline) {
        $status = Get-LatestDeploymentStatus -Service $Service
        if (-not $status) {
            $status = "PENDIENTE"
        }

        if ($status -ne $lastStatus) {
            Write-InfoMessage "${Service}: $status"
            $lastStatus = $status
        }

        if ($status -in @("SUCCESS", "SLEEPING")) {
            Write-Ok "Despliegue de '$Service' disponible."
            return
        }

        if ($status -in @("FAILED", "CRASHED", "REMOVED", "CANCELED", "CANCELLED")) {
            $script:UltimoServicio = $Service
            throw "El despliegue de '$Service' termino con estado $status."
        }

        Start-Sleep -Seconds 10
    }

    $script:UltimoServicio = $Service
    throw "Tiempo agotado esperando el despliegue de '$Service'."
}

function Start-RailwayDeployment {
    param([Parameter(Mandatory = $true)][string]$Service)

    $script:UltimoServicio = $Service
    Write-InfoMessage "Enviando '$Service' a Railway..."
    $null = Invoke-RailwayRaw -Arguments @(
        "up",
        "--service", $Service,
        "--detach",
        "--yes",
        "--message", "Despliegue automatico Tutor Inteligente",
        "."
    )
}

function Wait-HttpHealth {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [int]$TimeoutSeconds = 300
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 15
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                Write-Ok "Salud verificada: $Url"
                return
            }
        }
        catch {
            # El dominio puede tardar unos segundos en propagarse.
        }

        Start-Sleep -Seconds 8
    }

    throw "El servicio no respondio correctamente en $Url."
}

function Test-RequiredFiles {
    $required = @(
        "backend/pom.xml",
        "backend/Dockerfile",
        "frontend/package.json",
        "frontend/pnpm-lock.yaml",
        "frontend/Dockerfile",
        "frontend/nginx.conf",
        "ia/app/main.py",
        "ia/Dockerfile",
        "ia/requirements-runtime.txt",
        "database/Dockerfile",
        "database/railway-entrypoint.sh"
    )

    foreach ($relativePath in $required) {
        $fullPath = Join-Path $script:Raiz $relativePath
        if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
            throw "Falta el archivo requerido: $relativePath"
        }
    }

    Write-Ok "Estructura y archivos de despliegue completos."
}

function Test-GitSafety {
    $git = Get-Executable -Names @("git.exe", "git")
    if (-not $git) {
        Write-WarnMessage "Git no esta instalado; no impide el envio local a Railway."
        return
    }

    $inside = (& $git -C $script:Raiz rev-parse --is-inside-work-tree 2>$null)
    if ($LASTEXITCODE -ne 0 -or $inside -ne "true") {
        Write-WarnMessage "Esta carpeta no es un clon Git; Railway desplegara su contenido local."
        return
    }

    $trackedEnv = (& $git -C $script:Raiz ls-files .env)
    if ($trackedEnv) {
        throw "Seguridad: .env esta registrado en Git. Retiralo del repositorio antes de desplegar."
    }

    $changes = (& $git -C $script:Raiz status --short)
    if ($changes) {
        Write-WarnMessage "Hay cambios locales sin commit. Se desplegaran exactamente como estan en esta carpeta."
    }
    else {
        Write-Ok "Repositorio limpio y .env fuera de Git."
    }
}

function Invoke-ProjectTests {
    Write-Section "2/5 - Pruebas locales"

    $maven = Get-Executable -Names @("mvn.cmd", "mvn")
    if (-not $maven) {
        throw "No se encontro Maven. Instala Maven 3.9 y Java 21, o usa -OmitirPruebas."
    }
    Invoke-Native -FilePath $maven -Arguments @("--batch-mode", "--file", "backend/pom.xml", "test")
    Write-Ok "Backend: pruebas Maven superadas."

    $python = Join-Path $script:Raiz "ia/.venv/Scripts/python.exe"
    if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
        $venvCreated = $false
        $py = Get-Executable -Names @("py.exe", "py")
        if ($py) {
            foreach ($version in @("-3.12", "-3.11", "-3")) {
                try {
                    Invoke-Native -FilePath $py -Arguments @($version, "-m", "venv", "ia/.venv")
                    $venvCreated = $true
                    break
                }
                catch {
                    # Se prueba la siguiente version compatible registrada.
                }
            }
        }

        if (-not $venvCreated) {
            $systemPython = Get-Executable -Names @("python.exe", "python")
            if ($systemPython) {
                try {
                    Invoke-Native -FilePath $systemPython -Arguments @("-m", "venv", "ia/.venv")
                    $venvCreated = $true
                }
                catch {
                    $venvCreated = $false
                }
            }
        }

        if (-not $venvCreated) {
            $python = $null
            Write-WarnMessage "No hay un Python compatible instalado. Se omite solo la prueba local de IA; Railway la construira con Python 3.12."
        }
    }

    if ($python) {
        Invoke-Native -FilePath $python -Arguments @("-m", "pip", "install", "--disable-pip-version-check", "--requirement", "requirements.txt") -WorkingDirectory (Join-Path $script:Raiz "ia")
        Invoke-Native -FilePath $python -Arguments @("-m", "unittest", "discover", "-s", "tests", "-v") -WorkingDirectory (Join-Path $script:Raiz "ia")
        Write-Ok "IA: dependencias y pruebas superadas."
    }

    if (-not $script:Npx) {
        throw "No se encontro npx. Instala Node.js 24, o usa -OmitirPruebas."
    }

    $frontendDirectory = Join-Path $script:Raiz "frontend"
    Invoke-Native -FilePath $script:Npx -Arguments @("--yes", "pnpm@11.9.0", "install", "--frozen-lockfile") -WorkingDirectory $frontendDirectory
    Invoke-Native -FilePath $script:Npx -Arguments @("--yes", "pnpm@11.9.0", "test") -WorkingDirectory $frontendDirectory

    $previousApiUrl = $env:VITE_API_URL
    $previousDemoMode = $env:VITE_DEMO_MODE
    try {
        $env:VITE_API_URL = "https://api.example.invalid/api"
        $env:VITE_DEMO_MODE = "false"
        Invoke-Native -FilePath $script:Npx -Arguments @("--yes", "pnpm@11.9.0", "build") -WorkingDirectory $frontendDirectory
    }
    finally {
        if ($null -eq $previousApiUrl) {
            Remove-Item Env:VITE_API_URL -ErrorAction SilentlyContinue
        }
        else {
            $env:VITE_API_URL = $previousApiUrl
        }

        if ($null -eq $previousDemoMode) {
            Remove-Item Env:VITE_DEMO_MODE -ErrorAction SilentlyContinue
        }
        else {
            $env:VITE_DEMO_MODE = $previousDemoMode
        }
    }

    Invoke-Native -FilePath $script:Npx -Arguments @("--yes", "pnpm@11.9.0", "audit", "--prod", "--audit-level=high") -WorkingDirectory $frontendDirectory
    Write-Ok "Frontend: instalacion, pruebas, build y auditoria superados."
}

try {
    Write-Host ""
    Write-Host "TUTOR INTELIGENTE - VERIFICACION Y DESPLIEGUE AUTOMATICO" -ForegroundColor White -BackgroundColor DarkCyan

    Write-Section "1/5 - Verificacion del proyecto"
    Test-RequiredFiles
    Test-GitSafety

    $script:Npx = Get-Executable -Names @("npx.cmd", "npx")
    if (-not $script:Npx -and -not $SoloVerificar) {
        throw "No se encontro npx. Instala Node.js 24 LTS antes de desplegar."
    }

    if ($OmitirPruebas) {
        Write-WarnMessage "Se omitieron las pruebas locales por parametro."
    }
    else {
        Invoke-ProjectTests
    }

    if ($SoloVerificar) {
        Write-Section "RESULTADO"
        Write-Ok "El proyecto esta listo para iniciar el despliegue."
        exit 0
    }

    Write-Section "3/5 - Cuenta y recursos de Railway"
    $whoAmI = Invoke-RailwayRaw -Arguments @("whoami") -AllowFailure
    if (-not $whoAmI) {
        Write-InfoMessage "Se abrira el inicio de sesion oficial de Railway."
        & $script:Npx --yes "@railway/cli@latest" login
        if ($LASTEXITCODE -ne 0) {
            throw "No se completo el inicio de sesion en Railway."
        }
        $whoAmI = Invoke-RailwayRaw -Arguments @("whoami")
    }
    Write-Ok $whoAmI

    $linkedProject = Invoke-RailwayRaw -Arguments @("status", "--json") -AllowFailure
    if (-not $linkedProject) {
        $initArguments = @("init", "--name", $NombreProyecto)
        if ($env:RAILWAY_WORKSPACE) {
            $initArguments += @("--workspace", $env:RAILWAY_WORKSPACE)
        }
        $initArguments += "--json"

        Write-InfoMessage "Creando y vinculando el proyecto '$NombreProyecto'..."
        $null = Invoke-RailwayRaw -Arguments $initArguments
        Write-Ok "Proyecto Railway creado y vinculado."
    }
    else {
        Write-Ok "La carpeta ya esta vinculada a un proyecto Railway."
    }

    $sqlService = Ensure-Service -Name "sqlserver"
    $null = Ensure-Service -Name "ia"
    $null = Ensure-Service -Name "backend"
    $null = Ensure-Service -Name "frontend"

    Ensure-SqlVolume -SqlService $sqlService
    Configure-ServiceSettings

    $backendDomain = Ensure-PublicDomain -Service "backend" -Port 8080
    $frontendDomain = Ensure-PublicDomain -Service "frontend" -Port 8080
    Warn-IfPublicDomainExists -Service "ia"
    Warn-IfPublicDomainExists -Service "sqlserver"

    Write-Section "4/5 - Variables y secretos"
    $sqlPassword = Ensure-SecretVariable -Service "sqlserver" -Key "MSSQL_SA_PASSWORD" -Generator {
        return "Ti9!Aa" + (New-RandomToken -Bytes 30)
    }
    $null = $sqlPassword

    $aiKey = Ensure-SecretVariable -Service "ia" -Key "AI_SERVICE_API_KEY" -Generator {
        return New-RandomToken -Bytes 48
    }
    $null = $aiKey

    $jwtSecret = Ensure-SecretVariable -Service "backend" -Key "JWT_SECRET" -Generator {
        return New-RandomToken -Bytes 64
    }
    $null = $jwtSecret

    $teacherInvitation = Ensure-SecretVariable -Service "backend" -Key "TEACHER_INVITATION_CODE" -Generator {
        return "DOC-" + (New-RandomToken -Bytes 24)
    }

    $sqlVariables = [ordered]@{
        ACCEPT_EULA = "Y"
        MSSQL_PID = "Developer"
        DB_NAME = "TutorInteligente"
    }
    foreach ($entry in $sqlVariables.GetEnumerator()) {
        Set-RailwayVariable -Service "sqlserver" -Key $entry.Key -Value ([string]$entry.Value)
    }

    $iaVariables = [ordered]@{
        PORT = "8001"
        AI_DOCS_ENABLED = "false"
    }
    foreach ($entry in $iaVariables.GetEnumerator()) {
        Set-RailwayVariable -Service "ia" -Key $entry.Key -Value ([string]$entry.Value)
    }

    $backendVariables = [ordered]@{
        PORT = "8080"
        DB_HOST = "sqlserver.railway.internal"
        DB_PORT = "1433"
        DB_NAME = "TutorInteligente"
        DB_USER = "sa"
        DB_PASSWORD = '${{sqlserver.MSSQL_SA_PASSWORD}}'
        DB_ENCRYPT = "true"
        DB_TRUST_SERVER_CERTIFICATE = "true"
        AI_SERVICE_URL = "http://ia.railway.internal:8001"
        AI_SERVICE_API_KEY = '${{ia.AI_SERVICE_API_KEY}}'
        CORS_ORIGINS = ("https://" + $frontendDomain)
        DEMO_DATA = "false"
        RECOVERY_ENABLED = "false"
        RECOVERY_EXPOSE_LOCAL_CODE = "false"
        SWAGGER_ENABLED = "false"
    }
    foreach ($entry in $backendVariables.GetEnumerator()) {
        Set-RailwayVariable -Service "backend" -Key $entry.Key -Value ([string]$entry.Value)
    }

    $frontendVariables = [ordered]@{
        PORT = "8080"
        VITE_API_URL = ("https://" + $backendDomain + "/api")
        VITE_DEMO_MODE = "false"
    }
    foreach ($entry in $frontendVariables.GetEnumerator()) {
        Set-RailwayVariable -Service "frontend" -Key $entry.Key -Value ([string]$entry.Value)
    }

    Write-Ok "Variables configuradas. Los secretos no se guardaron en el repositorio."

    Write-Section "5/5 - Despliegue y comprobacion"
    Start-RailwayDeployment -Service "sqlserver"
    Start-RailwayDeployment -Service "ia"
    Wait-RailwayDeployment -Service "sqlserver"
    Wait-RailwayDeployment -Service "ia"

    Start-RailwayDeployment -Service "backend"
    Wait-RailwayDeployment -Service "backend"

    Start-RailwayDeployment -Service "frontend"
    Wait-RailwayDeployment -Service "frontend"

    $backendHealth = "https://$backendDomain/actuator/health"
    $frontendHealth = "https://$frontendDomain/health"
    Wait-HttpHealth -Url $backendHealth
    Wait-HttpHealth -Url $frontendHealth

    Write-Section "DESPLIEGUE COMPLETADO"
    Write-Host "Aplicacion:              https://$frontendDomain" -ForegroundColor Green
    Write-Host "API:                     https://$backendDomain/api" -ForegroundColor Green
    Write-Host "Salud del backend:       $backendHealth" -ForegroundColor Green
    Write-Host "Codigo invitacion docente: $teacherInvitation" -ForegroundColor Yellow
    Write-Host ""
    Write-WarnMessage "Guarda el codigo de invitacion docente en un gestor de contrasenas. No se guardo en archivos del proyecto."
    Write-WarnMessage "SQL Server usa la edicion Developer, adecuada para demostracion y desarrollo, no para uso comercial."

    if (-not $NoAbrirNavegador) {
        Start-Process ("https://" + $frontendDomain)
    }

    exit 0
}
catch {
    Write-Host ""
    Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
    if ($script:UltimoServicio) {
        Write-Host ""
        Write-Host "Diagnostico sugerido:" -ForegroundColor Yellow
        Write-Host "  npx --yes @railway/cli@latest logs --service $script:UltimoServicio --latest --lines 100"
    }
    Write-Host ""
    Write-Host "Consulta docs\DESPLIEGUE_SEGURO.md para requisitos y recuperacion." -ForegroundColor Yellow
    exit 1
}
