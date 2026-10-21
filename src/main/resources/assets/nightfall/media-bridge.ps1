param([string]$Action = "status")
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding -ArgumentList $false
$OutputEncoding = [Console]::OutputEncoding

function Wait-WinRT($Operation) {
    $task = [System.WindowsRuntimeSystemExtensions]::AsTask($Operation)
    if (-not $task.Wait(6000)) { throw "Windows media operation timed out" }
    return $task.GetAwaiter().GetResult()
}

try {
    Add-Type -AssemblyName System.Runtime.WindowsRuntime
    $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
    $manager = Wait-WinRT ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync())
    $sessions = @($manager.GetSessions())
    $session = $null
    foreach ($candidate in $sessions) {
        $info = $candidate.GetPlaybackInfo()
        if ($null -ne $info -and [string]$info.PlaybackStatus -eq "Playing") {
            $session = $candidate
            break
        }
    }
    if ($null -eq $session) { $session = $manager.GetCurrentSession() }
    if ($null -eq $session -and $sessions.Count -gt 0) { $session = $sessions[0] }

    if ($null -eq $session) {
        [ordered]@{ ok = $false; title = ""; artist = ""; app = ""; status = "No session"; positionMs = 0; durationMs = 0 } | ConvertTo-Json -Compress
        exit 0
    }

    if ($Action -eq "toggle") {
        $null = Wait-WinRT ($session.TryTogglePlayPauseAsync())
        @{ ok = $true } | ConvertTo-Json -Compress
        exit 0
    }
    if ($Action -eq "next") {
        $null = Wait-WinRT ($session.TrySkipNextAsync())
        @{ ok = $true } | ConvertTo-Json -Compress
        exit 0
    }
    if ($Action -eq "previous") {
        $null = Wait-WinRT ($session.TrySkipPreviousAsync())
        @{ ok = $true } | ConvertTo-Json -Compress
        exit 0
    }

    $props = Wait-WinRT ($session.TryGetMediaPropertiesAsync())
    $timeline = $session.GetTimelineProperties()
    $playback = $session.GetPlaybackInfo()
    $title = ""
    $artist = ""
    if ($null -ne $props) {
        $title = [string]$props.Title
        $artist = [string]$props.Artist
    }
    $position = 0L
    $duration = 0L
    if ($null -ne $timeline) {
        $position = [long]$timeline.Position.TotalMilliseconds
        $duration = [long]$timeline.EndTime.TotalMilliseconds
    }
    $status = "Unknown"
    if ($null -ne $playback) { $status = [string]$playback.PlaybackStatus }
    [ordered]@{
        ok = $true; title = $title; artist = $artist
        app = [string]$session.SourceAppUserModelId; status = $status
        positionMs = $position; durationMs = $duration
    } | ConvertTo-Json -Compress
} catch {
    [ordered]@{
        ok = $false; title = ""; artist = ""; app = ""; status = "Unavailable"
        positionMs = 0; durationMs = 0; error = $_.Exception.Message
    } | ConvertTo-Json -Compress
}