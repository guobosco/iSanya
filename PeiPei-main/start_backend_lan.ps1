$ErrorActionPreference = "Stop"

try {
    Set-Location -Path $PSScriptRoot
    Set-Location -Path ".\backend"

    $pythonCmd = Get-Command python -ErrorAction SilentlyContinue
    if (-not $pythonCmd) {
        throw "Python not found. Please install Python and add it to PATH."
    }

    if (-not (Test-Path ".venv")) {
        Write-Host "[1/4] Creating virtual environment .venv ..."
        python -m venv .venv
    }

    $pythonExe = Join-Path (Get-Location) ".venv\Scripts\python.exe"
    if (-not (Test-Path $pythonExe)) {
        throw "Virtual environment creation failed. Missing: $pythonExe"
    }

    Write-Host "[2/4] Upgrading pip ..."
    & $pythonExe -m pip install --upgrade pip

    Write-Host "[3/4] Installing backend dependencies ..."
    & $pythonExe -m pip install -r "requirements.txt"

    $localIp = (
        Get-NetIPAddress -AddressFamily IPv4 |
        Where-Object {
            $_.IPAddress -notlike "127.*" -and
            $_.IPAddress -notlike "169.254.*" -and
            ($_.IPAddress -like "192.168.*" -or $_.IPAddress -like "10.*" -or $_.IPAddress -like "172.1[6-9].*" -or $_.IPAddress -like "172.2?.*" -or $_.IPAddress -like "172.3[0-1].*")
        } |
        Select-Object -First 1 -ExpandProperty IPAddress
    )

    if (-not $localIp) {
        $localIp = "127.0.0.1"
    }

    $env:LULU_HOST = "0.0.0.0"
    $env:LULU_PORT = "8000"

    $occupied = Get-NetTCPConnection -LocalPort 8000 -State Listen -ErrorAction SilentlyContinue
    if ($occupied) {
        Write-Host ""
        Write-Host "Port 8000 is already in use." -ForegroundColor Yellow
        Write-Host "Please stop the existing process first, then retry." -ForegroundColor Yellow
        Write-Host "Tip: run in terminal -> netstat -ano | findstr :8000" -ForegroundColor Yellow
        throw "Cannot start backend because port 8000 is occupied."
    }

    Write-Host "[4/4] Starting backend service ..."
    Write-Host ""
    Write-Host "Local URL: http://127.0.0.1:8000"
    Write-Host ("LAN URL: http://{0}:8000" -f $localIp)
    Write-Host ("Health URL: http://{0}:8000/healthz" -f $localIp)
    Write-Host ""
    Write-Host "Press Ctrl + C to stop."
    Write-Host ""

    & $pythonExe "main.py"
    if ($LASTEXITCODE -ne 0) {
        throw "Backend exited with code $LASTEXITCODE."
    }
}
catch {
    Write-Host ""
    Write-Host "Startup failed:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    throw
}
