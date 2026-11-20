#Requires -Version 5.1
# Run once on the Windows laptop that stays beside the cage.
[CmdletBinding()]
param(
    # optional: without it the camera is taken from the cage panel (the phone can be set up there too)
    [uri]$PhoneUrl,
    [Parameter(Mandatory)][ValidatePattern('^COM[0-9]+$')][string]$SerialPort,
    [uri]$ServerUrl = 'https://cuymonitor.duckdns.org'
)
$ErrorActionPreference = 'Stop'
if ($ServerUrl.Scheme -ne 'https' -or ($PhoneUrl -and $PhoneUrl.Scheme -notin 'http','https')) {
    throw 'Use HTTPS for the server and the IP Webcam address for the phone.'
}
$repos = Split-Path (Split-Path $PSScriptRoot)
$bridge = Join-Path $repos 'cuy-monitor-arduino\serial_bridge'
$ai = Join-Path $repos 'cuy-monitor-ai-service'
$server = $ServerUrl.AbsoluteUri.TrimEnd('/')
$phone = if ($PhoneUrl) { $PhoneUrl.AbsoluteUri.TrimEnd('/') } else { '' }
$health = Invoke-RestMethod "$server/ai/health" -TimeoutSec 15
if ($health.status -ne 'UP' -or $health.mockMode -ne $false) { throw 'Real AI service required.' }
if ((Invoke-RestMethod "$server/actuator/health" -TimeoutSec 15).status -ne 'UP') { throw 'Backend is offline.' }

function Install-PythonEnvironment($directory, $name, $requirements) {
    $envDir = Join-Path $directory $name
    $python = Join-Path $envDir 'Scripts\python.exe'
    if (!(Test-Path -LiteralPath $python)) {
        & py -3.14 -m venv $envDir
        if ($LASTEXITCODE) { throw 'Python 3.14 is required.' }
    }
    & $python -m pip install --disable-pip-version-check -r (Join-Path $directory $requirements) | Out-Host
    if ($LASTEXITCODE) { throw "Could not install $requirements." }
    return $python
}

$bridgePython = Install-PythonEnvironment $bridge '.venv' 'requirements.txt'
$edgePython = Install-PythonEnvironment $ai '.venv-edge' 'edge_agent\requirements.txt'
$user = [Security.Principal.WindowsIdentity]::GetCurrent().Name
$credential = Get-Credential -UserName $user -Message 'Windows account password to run the two tasks at boot'
if (!$credential -or $credential.UserName -ne $user) { throw 'Use the current Windows account.' }
$key = Read-Host 'Server ingestion API key' -AsSecureString
if (!$key.Length) { throw 'API key is required.' }
$plainKey = [Net.NetworkCredential]::new('', $key).Password
$quotedKey = "'" + $plainKey.Replace('\','\\').Replace("'","\'") + "'"
$utf8 = [Text.UTF8Encoding]::new($false)

function Set-MonitorConfig($file, $entries) {
    $content = if (Test-Path -LiteralPath $file) { @(Get-Content -LiteralPath $file) } else { @() }
    foreach ($name in $entries.Keys) {
        $content = @($content | Where-Object { $_ -notmatch "^\s*(export\s+)?${name}\s*=" })
        $content += "$name=$($entries[$name])"
    }
    [IO.File]::WriteAllLines($file, [string[]]$content, $utf8)
}
Set-MonitorConfig (Join-Path $bridge '.env') @{SERIAL_PORT=$SerialPort; BACKEND_URL=$server; API_KEY=$quotedKey; CAGE_ID='farm-station'}
Set-MonitorConfig (Join-Path $ai 'edge_agent\.env') @{A12_URL=$phone; AI_URL=$server; API_KEY=$quotedKey; CAGE_ID='farm-station'; FPS='1'; AUDIO_ENABLED='true'}

# the RTSP camera needs ffmpeg on the laptop; without it the agent reports FFMPEG_MISSING instead of failing silently
if (!(Get-Command ffmpeg -ErrorAction SilentlyContinue)) {
    try {
        & winget install --id Gyan.FFmpeg -e --accept-source-agreements --accept-package-agreements | Out-Host
        if ($LASTEXITCODE) { throw "winget exit code $LASTEXITCODE" }
    } catch {
        Write-Warning "Could not install ffmpeg ($($_.Exception.Message)). Install it by hand for the Tapo camera; the phone still works."
    }
}

$trigger = New-ScheduledTaskTrigger -AtStartup
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew
function Install-MonitorTask($name, $directory, $python, $arguments) {
    $action = New-ScheduledTaskAction -Execute $python -Argument $arguments -WorkingDirectory $directory
    $task = Get-ScheduledTask -TaskName $name -ErrorAction SilentlyContinue
    if ($task -and $task.State -eq 'Running') { Stop-ScheduledTask -TaskName $name }
    Register-ScheduledTask -TaskName $name -Action $action -Trigger $trigger -Settings $settings -User $user -Password $credential.GetNetworkCredential().Password -RunLevel Limited -Force | Out-Null
    Start-ScheduledTask -TaskName $name
}
Install-MonitorTask 'CuyMonitor-Scale' $bridge $bridgePython 'bridge.py'
Install-MonitorTask 'CuyMonitor-CameraAudio' $ai $edgePython '-m edge_agent'
Get-ScheduledTask -TaskName 'CuyMonitor-Scale','CuyMonitor-CameraAudio' | Select-Object TaskName,State
