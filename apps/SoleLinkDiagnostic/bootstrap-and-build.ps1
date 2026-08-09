$ErrorActionPreference = "Stop"

$GradleVersion = "8.13"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$BootstrapRoot = Join-Path $ProjectRoot ".gradle-bootstrap"
$Archive = Join-Path $BootstrapRoot "gradle-$GradleVersion-bin.zip"
$GradleHome = Join-Path $BootstrapRoot "gradle-$GradleVersion"
$GradleBat = Join-Path $GradleHome "bin\gradle.bat"

if (-not (Get-Command java -ErrorAction SilentlyContinue) -and -not $env:JAVA_HOME) {
    throw "Java/JDK 17 wurde nicht gefunden. Installiere JDK 17 oder waehle das Android-Studio-JDK."
}

New-Item -ItemType Directory -Force -Path $BootstrapRoot | Out-Null

if (-not (Test-Path $GradleBat)) {
    Write-Host "Lade Gradle $GradleVersion von services.gradle.org ..."
    Invoke-WebRequest `
        -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" `
        -OutFile $Archive
    Expand-Archive -Path $Archive -DestinationPath $BootstrapRoot -Force
}

Push-Location $ProjectRoot
try {
    & $GradleBat wrapper --gradle-version $GradleVersion
    if ($LASTEXITCODE -ne 0) { throw "Gradle Wrapper konnte nicht erzeugt werden." }

    & ".\gradlew.bat" --no-daemon testDebugUnitTest assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "Android-Build ist fehlgeschlagen." }

    Write-Host ""
    Write-Host "APK erstellt:"
    Write-Host (Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk")
}
finally {
    Pop-Location
}
