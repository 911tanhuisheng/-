param(
    [string]$OutputRoot = "",
    [switch]$SkipZip
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$parent = Split-Path $repoRoot -Parent
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $parent "mayue-oj-delivery-$stamp"
}
$OutputRoot = [System.IO.Path]::GetFullPath($OutputRoot)

if ($OutputRoot.StartsWith($repoRoot + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Output directory must be outside the repository."
}
if (Test-Path -LiteralPath $OutputRoot) {
    throw "Output directory already exists: $OutputRoot"
}

New-Item -ItemType Directory -Path $OutputRoot | Out-Null

$excludedDirs = @(
    ".git", ".idea", ".vscode", ".venv", "node_modules", "target",
    "dist", "dist-ssr", "__pycache__", ".runtime-logs", ".local-ai",
    "tmpCode", "logs", "coverage"
)
$excludedDirs += @(
    (Join-Path $repoRoot "weights"),
    (Join-Path $repoRoot "vision-ai\.venv-partial-20260917-161046"),
    (Join-Path $repoRoot "vision-ai\runs"),
    (Join-Path $repoRoot "git-master"),
    (Join-Path $repoRoot "runs")
)
$excludedFiles = @(
    ".env", "ap.env", "application-local.yml", "*.log", "*.class", "*.jar", "*.war",
    "*.pyc", "*.pyo", "best.pt", "last.pt", "npm-debug.log*", "pnpm-debug.log*", "hs_err_pid*.log"
)

$arguments = @(
    $repoRoot, $OutputRoot, "/E", "/COPY:DAT", "/DCOPY:DAT", "/R:1", "/W:1",
    "/NFL", "/NDL", "/NJH", "/NJS", "/NP", "/XD"
) + $excludedDirs + @("/XF") + $excludedFiles

& robocopy @arguments | Out-Null
if ($LASTEXITCODE -ge 8) {
    throw "Robocopy failed with exit code $LASTEXITCODE"
}

$required = @(
    "README.md",
    ".env.example",
    "spingboot-init\pom.xml",
    "spingboot-init\src\main\resources\db\schema.sql",
    "Sandbox\pom.xml",
    "Sandbox\docker\python-vision\Dockerfile",
    "Sandbox\docker\python-vision\models\hard-hat-best.pt",
    "vision-ai\requirements.txt",
    "vision-ai\run.py",
    "vision-ai\models\hard-hat-best.pt",
    "deploy\ubuntu\start-sandbox.sh",
    "docs\GRADUATION_THESIS_GUIDE.md"
)
foreach ($relativePath in $required) {
    $fullPath = Join-Path $OutputRoot $relativePath
    if (!(Test-Path -LiteralPath $fullPath)) {
        throw "Required delivery file is missing: $relativePath"
    }
}
$frontendPackage = Get-ChildItem -LiteralPath $OutputRoot -Filter package.json -File -Recurse |
    Where-Object { $_.Directory.Name -eq "myoj" } | Select-Object -First 1
if ($null -eq $frontendPackage) {
    throw "Frontend package.json is missing."
}

$manifest = Get-ChildItem -LiteralPath $OutputRoot -File -Recurse -Force |
    ForEach-Object {
        [PSCustomObject]@{
            Path = $_.FullName.Substring($OutputRoot.Length + 1).Replace("\", "/")
            Size = $_.Length
            SHA256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
        }
    }
$manifest | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $OutputRoot "DELIVERY_MANIFEST.json") -Encoding UTF8

$size = (Get-ChildItem -LiteralPath $OutputRoot -File -Recurse | Measure-Object Length -Sum).Sum
Write-Host ("Delivery directory: {0} ({1:N2} MB)" -f $OutputRoot, ($size / 1MB))

if (!$SkipZip) {
    $zipPath = "$OutputRoot.zip"
    Compress-Archive -LiteralPath $OutputRoot -DestinationPath $zipPath -CompressionLevel Optimal
    Write-Host ("Delivery archive: {0} ({1:N2} MB)" -f $zipPath, ((Get-Item -LiteralPath $zipPath).Length / 1MB))
}
