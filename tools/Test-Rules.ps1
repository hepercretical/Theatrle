$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
Push-Location $project
try {
    $javaDir = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot' }
    $classes = Join-Path $project 'app\build\rule-checks'
    New-Item -ItemType Directory -Path $classes -Force | Out-Null
    $sources = @('MovieTitle','GameEngine','TitleLayout','FilmCsv','SharePattern') | ForEach-Object { "app/src/main/java/com/theatrle/app/$_.java" }
    & (Join-Path $javaDir 'bin/javac.exe') --release 17 -d $classes $sources tools/RuleChecks.java
    if ($LASTEXITCODE -ne 0) { throw 'Rule checks did not compile' }
    & (Join-Path $javaDir 'bin/java.exe') -cp $classes RuleChecks
    if ($LASTEXITCODE -ne 0) { throw 'Rule checks failed' }
} finally { Pop-Location }
