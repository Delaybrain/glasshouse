# Builds out\tvhome.apk on Windows. The same steps as build.sh, without needing zip or bash.
# Needs Java 17 and the Android SDK (platform 34, build-tools 34). setup-tools.ps1 puts both in .\tools.
#   powershell -ExecutionPolicy Bypass -File build.ps1
# Use a different app list with:  $env:APPS = "my-apps.txt"
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# Java: JAVA_HOME, else the one setup-tools.ps1 unpacked.
if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem "tools" -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}
if (-not $env:JAVA_HOME) { throw "Java 17 not found. Set JAVA_HOME, or run setup-tools.ps1." }
$bin = Join-Path $env:JAVA_HOME "bin"
$env:PATH = "$bin;$env:PATH"

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path $PSScriptRoot "tools\android-sdk" }
$bt = Join-Path $sdk "build-tools\34.0.0"
$jar = Join-Path $sdk "platforms\android-34\android.jar"
if (-not (Test-Path $jar) -or -not (Test-Path $bt)) {
    throw "Android SDK not found. Set ANDROID_HOME, or run setup-tools.ps1."
}

# Stops on the first tool that fails, like set -e in build.sh.
function Run($exe, [string[]]$argv) {
    & $exe @argv
    if ($LASTEXITCODE -ne 0) { throw "$exe failed with exit code $LASTEXITCODE" }
}

if (Test-Path out) { Remove-Item out -Recurse -Force }
New-Item -ItemType Directory -Force out\classes, out\dex, out\gen\com\ansub\tvhome | Out-Null

# Turn the app list into Apps.java.
$list = if ($env:APPS) { $env:APPS } else { "apps.txt" }
$apps = Get-Content $list | Where-Object { $_ -notmatch '^\s*#' } | ForEach-Object { $_.Trim() } | Where-Object { $_ }
$java = @("package com.ansub.tvhome;", "final class Apps { static final String[] LIST = {")
$java += $apps | ForEach-Object { "    `"$_`"," }
$java += "}; }"
[IO.File]::WriteAllLines("$PSScriptRoot\out\gen\com\ansub\tvhome\Apps.java", $java)

# The only resource is HomeGuard's accessibility config in res\xml.
Run "$bt\aapt2.exe" @("compile", "--dir", "res", "-o", "out\res.zip")
Run "$bt\aapt2.exe" @("link", "-o", "out\base.apk", "-I", $jar, "--manifest", "AndroidManifest.xml",
    "-R", "out\res.zip", "--auto-add-overlay", "--min-sdk-version", "21", "--target-sdk-version", "34")

$sources = Get-ChildItem src, out\gen -Recurse -Filter *.java | ForEach-Object { $_.FullName }
Run "javac" (@("-nowarn", "-Xlint:-options", "-encoding", "UTF-8", "-source", "1.8", "-target", "1.8",
    "-bootclasspath", "$jar;$bt\core-lambda-stubs.jar", "-d", "out\classes") + $sources)

$classes = Get-ChildItem out\classes -Recurse -Filter *.class | ForEach-Object { $_.FullName }
Run "$bt\d8.bat" (@("--min-api", "21", "--release", "--lib", $jar, "--output", "out\dex") + $classes)

# Put classes.dex into the APK aapt2 made.
Copy-Item out\base.apk out\unsigned.apk
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::Open("$PSScriptRoot\out\unsigned.apk", "Update")
try {
    [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, "$PSScriptRoot\out\dex\classes.dex", "classes.dex") | Out-Null
} finally { $zip.Dispose() }
Run "$bt\zipalign.exe" @("-f", "4", "out\unsigned.apk", "out\aligned.apk")

# A signing key is made on first build. Keep key.jks: updates must be signed with the same key.
$ks = if ($env:KEYSTORE) { $env:KEYSTORE } else { "key.jks" }
$pass = if ($env:KEYSTORE_PASS) { $env:KEYSTORE_PASS } else { "tvhome123" }
if (-not (Test-Path $ks)) {
    Run "keytool" @("-genkeypair", "-keystore", $ks, "-storepass", $pass, "-keypass", $pass, "-alias", "tvhome",
        "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000", "-dname", "CN=tvhome")
}
Run "$bt\apksigner.bat" @("sign", "--ks", $ks, "--ks-pass", "pass:$pass", "--out", "out\tvhome.apk", "out\aligned.apk")
Write-Host "Built out\tvhome.apk"
