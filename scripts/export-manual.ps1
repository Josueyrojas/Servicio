$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$htmlPath = Join-Path $projectRoot 'docs\manual-uso.html'
$pdfPath = Join-Path $projectRoot 'docs\manual-uso.pdf'
$edgePaths = @(
    (Join-Path ${env:ProgramFiles(x86)} 'Microsoft\Edge\Application\msedge.exe'),
    (Join-Path $env:ProgramFiles 'Microsoft\Edge\Application\msedge.exe')
)
$edgePath = $edgePaths | Where-Object { Test-Path $_ } | Select-Object -First 1

if (-not (Test-Path $htmlPath)) {
    throw "No se encontró el manual fuente: $htmlPath"
}
if (-not $edgePath) {
    throw 'No se encontró Microsoft Edge. Instálalo para exportar el manual a PDF.'
}

$htmlUri = ([System.Uri]::new($htmlPath)).AbsoluteUri
$process = Start-Process -FilePath $edgePath -ArgumentList @(
    '--headless',
    '--disable-gpu',
    '--no-first-run',
    '--no-default-browser-check',
    '--no-pdf-header-footer',
    "--print-to-pdf=$pdfPath",
    $htmlUri
) -Wait -PassThru

if ($process.ExitCode -ne 0 -or -not (Test-Path $pdfPath) -or (Get-Item $pdfPath).Length -eq 0) {
    throw "Falló la exportación del PDF (código $($process.ExitCode))."
}

Write-Output "PDF generado: $pdfPath"