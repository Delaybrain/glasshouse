# Downloads what build.ps1 needs into .\tools, nothing system-wide:
#   Java 17 (Eclipse Temurin JDK, about 190 MB)
#   Android command-line tools (about 150 MB), then platform 34 and build-tools 34 (about 120 MB)
# sdkmanager asks you to accept Google's Android SDK licences before it installs anything.
#   powershell -ExecutionPolicy Bypass -File setup-tools.ps1
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue" # Invoke-WebRequest is many times slower with the progress bar
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force tools | Out-Null

if (-not (Get-ChildItem tools -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue)) {
    Write-Host "Downloading Java 17 (Temurin)..."
    Invoke-WebRequest "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse" -OutFile tools\jdk.zip
    Expand-Archive tools\jdk.zip tools -Force
    Remove-Item tools\jdk.zip
}
$jdk = (Get-ChildItem tools -Directory -Filter "jdk-17*" | Select-Object -First 1).FullName
$env:JAVA_HOME = $jdk

$sdk = Join-Path $PSScriptRoot "tools\android-sdk"
$sdkmanager = Join-Path $sdk "cmdline-tools\latest\bin\sdkmanager.bat"
if (-not (Test-Path $sdkmanager)) {
    Write-Host "Downloading Android command-line tools..."
    Invoke-WebRequest "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" -OutFile tools\cmdline.zip
    Expand-Archive tools\cmdline.zip tools\cmdline-tmp -Force
    New-Item -ItemType Directory -Force "$sdk\cmdline-tools" | Out-Null
    Move-Item tools\cmdline-tmp\cmdline-tools "$sdk\cmdline-tools\latest"
    Remove-Item tools\cmdline.zip, tools\cmdline-tmp -Recurse -Force
}

# sdkmanager.bat breaks on a space anywhere in its path ("TV Debloat"), so it runs from Windows'
# short 8.3 names, through cmd.
function Short($p) { (cmd /c "for %I in (`"$p`") do @echo %~sI").Trim() }
$sm = Short $sdkmanager; $root = Short $sdk; $javaHome = Short $jdk

Write-Host "Android SDK licences:"
cmd /c "set JAVA_HOME=$javaHome&& $sm --sdk_root=$root --licenses"
Write-Host "Installing platform 34 and build-tools 34..."
cmd /c "set JAVA_HOME=$javaHome&& $sm --sdk_root=$root platforms;android-34 build-tools;34.0.0"
if (-not (Test-Path "$sdk\platforms\android-34\android.jar")) { throw "sdkmanager failed" }
Write-Host "Done. Now run build.ps1."
