$ErrorActionPreference = 'Stop'

$projectRoot = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
$visionPython = Join-Path $projectRoot 'vision-ai\.venv\Scripts\python.exe'
$visionRun = Join-Path $projectRoot 'vision-ai\run.py'
$localComfyRoot = Join-Path $projectRoot '.local-ai\ComfyUI'
$comfyRoot = if ($env:COMFYUI_HOME -and (Test-Path $env:COMFYUI_HOME)) {
    $env:COMFYUI_HOME
} elseif (Test-Path 'D:\AI\ComfyUI') {
    'D:\AI\ComfyUI'
} else {
    $localComfyRoot
}
$comfyPython = Join-Path $comfyRoot '.venv\Scripts\python.exe'
$comfyMain = Join-Path $comfyRoot 'main.py'
$checkpoint = Join-Path $comfyRoot 'models\checkpoints\v1-5-pruned-emaonly.safetensors'
$logRoot = Join-Path $projectRoot '.runtime-logs'

New-Item -ItemType Directory -Force -Path $logRoot | Out-Null

function Test-Port([int]$Port) {
    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $result = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        $ok = $result.AsyncWaitHandle.WaitOne(500) -and $client.Connected
        $client.Close()
        return $ok
    } catch { return $false }
}

if (-not (Test-Path $visionPython) -or -not (Test-Path $visionRun)) {
    throw "vision-ai 环境不完整，请先在 vision-ai 目录安装 requirements.txt"
}
if (-not (Test-Path $comfyPython) -or -not (Test-Path $comfyMain)) {
    throw "未找到 D:\AI\ComfyUI，请先安装本地图像生成服务"
}
if (-not (Test-Path $checkpoint)) {
    throw "缺少生图模型：$checkpoint"
}

if (Test-Port 8090) {
    Write-Host '[OK] YOLO/OCR 已在 8090 运行' -ForegroundColor Green
} else {
    Start-Process -FilePath $visionPython -ArgumentList @($visionRun) -WorkingDirectory (Split-Path $visionRun) `
        -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logRoot 'vision-ai.out.log') `
        -RedirectStandardError (Join-Path $logRoot 'vision-ai.err.log')
    Write-Host '[START] 正在启动 YOLO/OCR :8090 ...' -ForegroundColor Cyan
}

if (Test-Port 8188) {
    Write-Host '[OK] ComfyUI 已在 8188 运行' -ForegroundColor Green
} else {
    Start-Process -FilePath $comfyPython -ArgumentList @($comfyMain, '--listen', '127.0.0.1', '--port', '8188', '--lowvram') `
        -WorkingDirectory $comfyRoot -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logRoot 'comfyui.out.log') `
        -RedirectStandardError (Join-Path $logRoot 'comfyui.err.log')
    Write-Host '[START] 正在启动 ComfyUI :8188 ...' -ForegroundColor Cyan
}

foreach ($port in @(8090, 8188)) {
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        if (Test-Port $port) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw "端口 $port 启动超时，请查看 $logRoot 中的日志" }
    Write-Host "[READY] http://127.0.0.1:$port" -ForegroundColor Green
}

Write-Host '图像 AI 服务已全部就绪。现在可以启动 OJ 后端和前端。' -ForegroundColor Green
