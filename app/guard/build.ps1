#requires -version 5
param([string]$Toolchain = "")
# 编译 com.dsh.peppa.guard —— 峰米投影的桌面守卫（Home 键接管 + 开机补刀）
# 需要 JDK17 / android-30.jar / aapt2 / r8 / guard.keystore（见 README.md）

$ErrorActionPreference = "Stop"
$proj = $PSScriptRoot
if (-not $Toolchain) { $Toolchain = Join-Path (Split-Path $proj -Parent) "toolchain" }
$tc = $Toolchain

$javac = Join-Path $tc "jdk-17.0.2\bin\javac.exe"
$java = Join-Path $tc "jdk-17.0.2\bin\java.exe"
$jarsigner = Join-Path $tc "jdk-17.0.2\bin\jarsigner.exe"
$aapt2 = Join-Path $tc "aapt2\aapt2.exe"
$androidJar = Join-Path $tc "android-30.jar"
$r8 = Join-Path $tc "r8.jar"
$ks = Join-Path $tc "guard.keystore"

foreach ($f in @($javac, $aapt2, $androidJar, $r8, $ks)) {
    if (-not (Test-Path $f)) { throw "缺工具链: $f" }
}

$work = Join-Path $proj "build"
Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $work, (Join-Path $work "classes"), (Join-Path $work "dex") | Out-Null

Write-Output "== 1/6 aapt2 compile =="
& $aapt2 compile --dir (Join-Path $proj "res") -o (Join-Path $work "res.zip")
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile 失败" }

Write-Output "== 2/6 aapt2 link =="
$unsigned = Join-Path $work "guard-unsigned.apk"
& $aapt2 link -o $unsigned -I $androidJar --manifest (Join-Path $proj "AndroidManifest.xml") `
    --min-sdk-version 21 --target-sdk-version 27 (Join-Path $work "res.zip")
if ($LASTEXITCODE -ne 0) { throw "aapt2 link 失败" }

Write-Output "== 3/6 javac =="
$srcs = Get-ChildItem (Join-Path $proj "src") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& $javac -nowarn -encoding UTF-8 -source 8 -target 8 -bootclasspath $androidJar -d (Join-Path $work "classes") $srcs 2>&1
if ($LASTEXITCODE -ne 0) { throw "javac 失败" }

Write-Output "== 4/6 d8 =="
$classes = Get-ChildItem (Join-Path $work "classes") -Recurse -Filter *.class | ForEach-Object { $_.FullName }
& $java -cp $r8 com.android.tools.r8.D8 --release --min-api 21 --lib $androidJar `
    --output (Join-Path $work "dex") $classes
if ($LASTEXITCODE -ne 0) { throw "d8 失败" }

Write-Output "== 5/6 pack classes.dex =="
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($unsigned, "Update")
$existing = $zip.GetEntry("classes.dex")
if ($existing) { $existing.Delete() }
[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, (Join-Path $work "dex\classes.dex"), "classes.dex", [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
$zip.Dispose()

Write-Output "== 6/6 sign (v1) =="
$out = Join-Path $proj "peppa-guard.apk"
Copy-Item $unsigned $out -Force
& $jarsigner -keystore $ks -storepass guardpass -keypass guardpass -sigalg SHA256withRSA -digestalg SHA-256 $out guard 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { throw "签名失败" }

Write-Output "built: $out ($((Get-Item $out).Length) bytes)"

