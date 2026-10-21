param([string]$Action = "status")
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding -ArgumentList $false
$OutputEncoding = [Console]::OutputEncoding
try {
    Add-Type -AssemblyName System.Runtime.WindowsRuntime
    $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
    $manager = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync().GetAwaiter().GetResult()
    $sessions = $manager.GetSessions()
    $session = $manager.GetCurrentSession()

    if ($null -eq $session -and $sessions.Count -gt 0) {
        $session = $sessions[0]
    }
    if ($null -eq $session) {
        @{ ok = $false; title = ""; artist = ""; app = ""; status = "No session"; positionMs = 0; durationMs = 0 } | ConvertTo-Json -Compress
        exit 0
    }

    if ($Action -eq "toggle") {
        $null = $session.TryTogglePlayPauseAsync().GetAwaiter().GetResult()
        @{ ok = $true } | ConvertTo-Json -Compress
        exit 0
    }
    if ($Action -eq "next") {
        $null = $session.TrySkipNextAsync().GetAwaiter().GetResult()
        @{ ok = $true } | ConvertTo-Json -Compress
        exit 0
    }
    if ($Action -eq "previous") {
        $null = $session.TrySkipPreviousAsync().GetAwaiter().GetResult()
        @{ ok = $true } | ConvertTo-Json -Compress
        exit 0
    }

    $props = $session.TryGetMediaPropertiesAsync().GetAwaiter().GetResult()
    $timeline = $session.GetTimelineProperties()
    $playback = $session.GetPlaybackInfo()
    $title = ""
    $artist = ""
    if ($null -ne $props) {
        $title = [string]$props.Title
        $artist = [string]$props.Artist
    }
    $appId = [string]$session.SourceAppUserModelId
    $position = 0L
    $duration = 0L
    if ($null -ne $timeline) {
        $position = [long]$timeline.Position.TotalMilliseconds
        $duration = [long]$timeline.EndTime.TotalMilliseconds
    }
    $status = "Unknown"
    if ($null -ne $playback) { $status = [string]$playback.PlaybackStatus }
    [ordered]@{
        ok = $true
        title = $title
        artist = $artist
        app = $appId
        status = $status
        positionMs = $position
        durationMs = $duration
    } | ConvertTo-Json -Compress
} catch {
    [ordered]@{ ok = $false; title = ""; artist = ""; app = ""; status = "Unavailable"; positionMs = 0; durationMs = 0; error = $_.Exception.Message } | ConvertTo-Json -Compress
}
