$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$PidFile = Join-Path $Root '.run\pids.txt'

function Stop-ProcessTree([int]$ProcessId) {
    & taskkill.exe /PID $ProcessId /T /F 2>$null | Out-Null
}

if (Test-Path $PidFile) {
    Get-Content $PidFile | ForEach-Object {
        if ($_ -match '^\d+$') { Stop-ProcessTree ([int]$_) }
    }
    Remove-Item $PidFile -Force
}

# Recupera procesos secundarios de una ejecucion anterior cuyo PID padre ya termino.
foreach ($port in 5173, 8001, 8080) {
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object {
            $process = Get-CimInstance Win32_Process -Filter "ProcessId=$_" -ErrorAction SilentlyContinue
            if (-not $process) { return }
            $commandLine = [string]$process.CommandLine
            $belongsToTutor =
                ($port -eq 5173 -and $process.Name -eq 'node.exe' -and $commandLine.Contains($Root)) -or
                ($port -eq 8080 -and $process.Name -eq 'java.exe' -and $commandLine.Contains($Root)) -or
                ($port -eq 8001 -and $process.Name -like 'python*.exe' -and $commandLine -match 'uvicorn.+8001')
            if ($belongsToTutor) { Stop-ProcessTree ([int]$process.ProcessId) }
        }
}

Write-Host 'Procesos locales del Tutor Inteligente detenidos.' -ForegroundColor Green
