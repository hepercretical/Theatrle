param(
    [ValidateSet('Run','Build','Install','Start','List','Devices')][string]$Action='Run',
    [string]$Avd='Medium_Phone',
    [string]$Device=''
)
$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
Push-Location $project
try {
    $sdk=if($env:ANDROID_HOME){$env:ANDROID_HOME}else{Join-Path $env:LOCALAPPDATA 'Android\Sdk'}
    $adb=Join-Path $sdk 'platform-tools\adb.exe'
    $emulator=Join-Path $sdk 'emulator\emulator.exe'
    $env:ANDROID_USER_HOME=Join-Path $env:USERPROFILE '.android'
    $javaSetting=Get-Content (Join-Path $project 'gradle.properties') | Where-Object {$_ -match '^org.gradle.java.home='} | Select-Object -First 1
    if($javaSetting){$env:JAVA_HOME=$javaSetting.Substring($javaSetting.IndexOf('=')+1).Replace('\:',':')}
    if($Action -eq 'Build' -or $Action -eq 'Install' -or $Action -eq 'Run') {
        & (Join-Path $project 'gradlew.bat') assembleDebug --console=plain
        if($LASTEXITCODE -ne 0){throw 'Build failed. The APK was not installed.'}
        $apk=Join-Path $project 'app\build\intermediates\apk\debug\app-debug.apk'
        if(-not(Test-Path -LiteralPath $apk)){throw "Build did not generate $apk"}
        Write-Host "Current APK: $apk"
        if($Action -eq 'Build'){exit 0}
    }
    if($Action -eq 'List'){& $emulator -list-avds; exit $LASTEXITCODE}
    if($Action -eq 'Devices'){& $adb devices; exit $LASTEXITCODE}
    $devices=@(& $adb devices | Where-Object {$_ -match '^emulator-\d+\s+device$'} | ForEach-Object {($_ -split '\s+')[0]})
    if(-not $Device -and $devices.Count -gt 1){throw 'Multiple emulators are running. Pass -Device emulator-5554 to select one.'}
    if(-not $Device -and $devices.Count -eq 1){$Device=$devices[0]}
    if(-not $Device) {
        $available=@(& $emulator -list-avds)
        if($Avd -notin $available){throw "Emulator '$Avd' is unavailable. Available emulators: $($available -join ', ')"}
        Write-Host "Starting Android emulator: $Avd"
        Start-Process -FilePath $emulator -ArgumentList @('-avd',$Avd) -WindowStyle Hidden
    }
    $deadline=(Get-Date).AddMinutes(5)
    do {
        if(-not $Device) {
            $devices=@(& $adb devices | Where-Object {$_ -match '^emulator-\d+\s+device$'} | ForEach-Object {($_ -split '\s+')[0]})
            if($devices.Count -eq 1){$Device=$devices[0]}
        }
        if($Device){$boot=@(& $adb -s $Device shell getprop sys.boot_completed 2>$null); if(($boot -join '').Trim() -eq '1'){break}}
        if((Get-Date) -gt $deadline){throw 'Emulator did not finish booting within five minutes.'}
        Write-Host 'Waiting for Android to finish booting...'
        Start-Sleep -Seconds 3
    } while($true)
    Write-Host "Android is ready: $Device"
    if($Action -eq 'Start'){exit 0}
    & $adb -s $Device install -r $apk
    if($LASTEXITCODE -ne 0){throw 'APK installation failed. Read the adb message above; the app was not launched.'}
    if($Action -eq 'Install'){exit 0}
    & $adb -s $Device shell am start -W -n com.theatrle.app/.MainActivity
    if($LASTEXITCODE -ne 0){throw 'Theatrle could not launch.'}
} finally { Pop-Location }
