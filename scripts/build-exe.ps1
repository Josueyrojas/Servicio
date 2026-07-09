# Genera un .exe distribuible de la app (carpeta autocontenida con runtime de Java incluido).
# Uso: desde Servicio/ ->  powershell -ExecutionPolicy Bypass -File scripts/build-exe.ps1
# Resultado: Servicio/dist-out/NOBAITC/NOBAITC.exe (comparte toda la carpeta "NOBAITC", no solo el .exe).

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$m2 = "$env:USERPROFILE\.m2\repository"
$deps = @(
    "org\openjfx\javafx-base\20.0.2\javafx-base-20.0.2-win.jar",
    "org\openjfx\javafx-controls\20.0.2\javafx-controls-20.0.2-win.jar",
    "org\openjfx\javafx-fxml\20.0.2\javafx-fxml-20.0.2-win.jar",
    "org\openjfx\javafx-graphics\20.0.2\javafx-graphics-20.0.2-win.jar",
    "org\controlsfx\controlsfx\11.2.1\controlsfx-11.2.1.jar",
    "org\kordamp\ikonli\ikonli-core\12.4.0\ikonli-core-12.4.0.jar",
    "org\kordamp\ikonli\ikonli-javafx\12.4.0\ikonli-javafx-12.4.0.jar",
    "org\kordamp\ikonli\ikonli-fontawesome6-pack\12.4.0\ikonli-fontawesome6-pack-12.4.0.jar",
    "org\kordamp\ikonli\ikonli-typicons-pack\12.4.0\ikonli-typicons-pack-12.4.0.jar",
    "com\itextpdf\kernel\7.2.6\kernel-7.2.6.jar",
    "com\itextpdf\layout\7.2.6\layout-7.2.6.jar",
    "com\itextpdf\io\7.2.6\io-7.2.6.jar",
    "com\itextpdf\barcodes\7.2.6\barcodes-7.2.6.jar",
    "com\itextpdf\commons\7.2.6\commons-7.2.6.jar",
    "com\itextpdf\font-asian\7.2.6\font-asian-7.2.6.jar",
    "com\itextpdf\forms\7.2.6\forms-7.2.6.jar",
    "com\itextpdf\hyph\7.2.6\hyph-7.2.6.jar",
    "com\itextpdf\pdfa\7.2.6\pdfa-7.2.6.jar",
    "com\itextpdf\sign\7.2.6\sign-7.2.6.jar",
    "com\itextpdf\styled-xml-parser\7.2.6\styled-xml-parser-7.2.6.jar",
    "com\itextpdf\svg\7.2.6\svg-7.2.6.jar",
    "org\apache\poi\poi\5.2.3\poi-5.2.3.jar",
    "org\apache\poi\poi-ooxml\5.2.3\poi-ooxml-5.2.3.jar",
    "org\apache\poi\poi-ooxml-lite\5.2.3\poi-ooxml-lite-5.2.3.jar",
    "org\apache\xmlbeans\xmlbeans\5.1.1\xmlbeans-5.1.1.jar",
    "org\apache\commons\commons-collections4\4.4\commons-collections4-4.4.jar",
    "org\apache\commons\commons-compress\1.26.1\commons-compress-1.26.1.jar",
    "org\apache\commons\commons-math3\3.6.1\commons-math3-3.6.1.jar",
    "com\github\virtuald\curvesapi\1.07\curvesapi-1.07.jar",
    "org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar",
    "org\slf4j\slf4j-simple\1.7.36\slf4j-simple-1.7.36.jar",
    "org\apache\logging\log4j\log4j-api\2.18.0\log4j-api-2.18.0.jar"
)

Write-Host "== 1/4: limpiando builds anteriores =="
Remove-Item -Recurse -Force out-app, dist-input, dist-out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force out-app, dist-input | Out-Null

Write-Host "== 2/4: compilando (--release 17) =="
$cpFile = New-TemporaryFile
($deps | ForEach-Object { Join-Path $m2 $_ }) -join ";" | Set-Content -NoNewline -Path $cpFile
$sources = Get-ChildItem -Recurse -Path "src\main\java" -Filter *.java |
    Where-Object { $_.Name -ne "module-info.java" } | ForEach-Object { $_.FullName }
& javac -d out-app --release 17 -cp "@$cpFile" $sources
if ($LASTEXITCODE -ne 0) { throw "javac fallo" }
Remove-Item $cpFile

Write-Host "== 3/4: empaquetando app.jar + copiando dependencias =="
& jar --create --file dist-input\app.jar --main-class me.julionxn.nobaitc.app.Main -C out-app . -C src\main\resources .
if ($LASTEXITCODE -ne 0) { throw "jar fallo" }
foreach ($d in $deps) { Copy-Item (Join-Path $m2 $d) dist-input\ }

Write-Host "== 4/4: generando .exe con jpackage =="
& jpackage --type app-image --input dist-input --dest dist-out --name NOBAITC `
    --main-jar app.jar --main-class me.julionxn.nobaitc.app.Main --app-version 1.0
if ($LASTEXITCODE -ne 0) { throw "jpackage fallo" }

Write-Host "`nListo: dist-out\NOBAITC\NOBAITC.exe"
Write-Host "Comparte la carpeta 'dist-out\NOBAITC' completa (comprimida en .zip), no solo el .exe."
