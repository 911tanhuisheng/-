$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$projectRoot = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
$runtimeRoot = Join-Path $projectRoot '.local-ai'
$defaultComfyRoot = Join-Path $runtimeRoot 'ComfyUI'
$externalComfyRoot = 'D:\AI\ComfyUI'
$checkpointName = 'v1-5-pruned-emaonly.safetensors'
$logRoot = Join-Path $projectRoot '.runtime-logs'
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null
Start-Transcript -Path (Join-Path $logRoot 'image-ai-launch.log') -Append | Out-Null

function Write-Step([string]$Text) {
    Write-Host "`n==== $Text ====" -ForegroundColor Cyan
}

function Require-Command([string]$Name, [string]$Hint) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "缺少 $Name。$Hint"
    }
}

Write-Host '码跃 OJ - 图像 AI 一键安装与启动' -ForegroundColor Green
Write-Host '首次运行会下载 PyTorch、ComfyUI 和约 4.27GB 模型，请保持联网。'

Require-Command 'git' '请安装 Git for Windows 并勾选添加到 PATH。'
Require-Command 'python' '请安装 64 位 Python 3.12 或 3.13，并勾选 Add Python to PATH。'

$pythonVersion = & python -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')"
if ($LASTEXITCODE -ne 0 -or $pythonVersion -notin @('3.12', '3.13')) {
    throw "当前 Python 版本为 $pythonVersion，请安装 Python 3.12 或 3.13。"
}

$smi = Get-Command nvidia-smi -ErrorAction SilentlyContinue
if (-not $smi) {
    throw '未检测到 NVIDIA 驱动。请先安装显卡驱动；本地生图不建议使用 CPU。'
}
Write-Host ('[OK] ' + (& $smi.Source --query-gpu=name,memory.total --format=csv,noheader | Select-Object -First 1)) -ForegroundColor Green

New-Item -ItemType Directory -Force -Path $runtimeRoot | Out-Null
$comfyRoot = if (Test-Path (Join-Path $externalComfyRoot 'main.py')) { $externalComfyRoot } else { $defaultComfyRoot }
$env:COMFYUI_HOME = $comfyRoot

if (-not (Test-Path (Join-Path $comfyRoot '.git'))) {
    Write-Step '下载 ComfyUI'
    git clone --depth 1 https://github.com/Comfy-Org/ComfyUI.git $comfyRoot
    if ($LASTEXITCODE -ne 0) { throw 'ComfyUI 下载失败，请检查 GitHub 网络。' }
} else {
    Write-Host "[OK] 已找到 ComfyUI：$comfyRoot" -ForegroundColor Green
}

$comfyPython = Join-Path $comfyRoot '.venv\Scripts\python.exe'
if (-not (Test-Path $comfyPython)) {
    Write-Step '创建 ComfyUI Python 环境'
    & python -m venv (Join-Path $comfyRoot '.venv')
}

$readyMarker = Join-Path $comfyRoot '.venv\.mayue-ready'
if (-not (Test-Path $readyMarker)) {
    Write-Step '安装 NVIDIA PyTorch（首次下载较大）'
    & $comfyPython -m pip install --upgrade pip
    & $comfyPython -m pip install torch torchvision torchaudio --extra-index-url https://download.pytorch.org/whl/cu130
    if ($LASTEXITCODE -ne 0) { throw 'NVIDIA PyTorch 安装失败。' }
    Write-Step '安装 ComfyUI 依赖'
    & $comfyPython -m pip install -r (Join-Path $comfyRoot 'requirements.txt')
    if ($LASTEXITCODE -ne 0) { throw 'ComfyUI 依赖安装失败。' }
    New-Item -ItemType File -Force -Path $readyMarker | Out-Null
}

Write-Step '检查 CUDA'
$cudaResult = & $comfyPython -c "import torch; print('true' if torch.cuda.is_available() else 'false'); print(torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'NONE')"
if ($cudaResult[0] -ne 'true') { throw 'PyTorch 未能调用 NVIDIA 显卡，请更新 NVIDIA 驱动后重试。' }
Write-Host ("[OK] CUDA 显卡：" + $cudaResult[1]) -ForegroundColor Green

$checkpointDir = Join-Path $comfyRoot 'models\checkpoints'
$checkpointPath = Join-Path $checkpointDir $checkpointName
if (-not (Test-Path $checkpointPath) -or (Get-Item $checkpointPath).Length -lt 3GB) {
    Write-Step '下载 Stable Diffusion 1.5 模型（约 4.27GB）'
    New-Item -ItemType Directory -Force -Path $checkpointDir | Out-Null
    $hf = Join-Path $comfyRoot '.venv\Scripts\hf.exe'
    & $hf download Comfy-Org/stable-diffusion-v1-5-archive $checkpointName --local-dir $checkpointDir
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $checkpointPath)) {
        throw 'Stable Diffusion 模型下载失败，请检查 Hugging Face 网络后重试。'
    }
} else {
    Write-Host "[OK] 已找到生图模型：$checkpointName" -ForegroundColor Green
}

$visionRoot = Join-Path $projectRoot 'vision-ai'
$visionPython = Join-Path $visionRoot '.venv\Scripts\python.exe'
if (-not (Test-Path $visionPython)) {
    Write-Step '安装 YOLO/OCR 环境'
    & python -m venv (Join-Path $visionRoot '.venv')
    & $visionPython -m pip install --upgrade pip
    & $visionPython -m pip install -r (Join-Path $visionRoot 'requirements.txt')
    if ($LASTEXITCODE -ne 0) { throw 'YOLO/OCR 依赖安装失败。' }
}

Write-Step '启动图像 AI 服务'
$launcherPath = Join-Path $projectRoot 'start-vision-services.ps1'
$launcherCode = [IO.File]::ReadAllText($launcherPath, [Text.Encoding]::UTF8)
& ([ScriptBlock]::Create($launcherCode))
Stop-Transcript | Out-Null
