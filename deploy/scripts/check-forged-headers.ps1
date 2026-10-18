<#
.SYNOPSIS
Regression check for forwarding-header spoofing of the login throttle (Windows).

.DESCRIPTION
Caddy is the only ingress and the API trusts the proxy, so it derives the client
IP from the X-Forwarded-For / Forwarded headers. If a client can forge one of
those headers it gets a fresh throttle key on every request and the per-IP limit
never fires.

This sends wrong logins for different usernames, each with a different forged
Forwarded, X-Forwarded-For and X-Real-IP value, and requires the per-address
limit to stop the flood. It mirrors scripts/check-forged-headers.sh and uses
curl.exe, which ships with Windows 10 1803 and later.

.EXAMPLE
cd deploy
.\scripts\check-forged-headers.ps1
.\scripts\check-forged-headers.ps1 -BaseUrl https://your-domain
#>
[CmdletBinding()]
param(
    [string]$BaseUrl = 'https://localhost',
    [int]$Limit = 20,
    [int]$Requests = 25
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# A unique suffix keeps the per-username limit out of the way, so this check
# targets the per-address limit only and can be re-run safely.
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$codes = @()
$first = $null

for ($n = 1; $n -le $Requests; $n++) {
    $body = '{"username":"forged-' + $runId + '-' + $n + '","password":"wrong"}'
    $bodyFile = [System.IO.Path]::GetTempFileName()
    try {
        [System.IO.File]::WriteAllText($bodyFile, $body)
        $code = & curl.exe -k -s -o NUL -w '%{http_code}' --max-time 10 `
            -H "Forwarded: for=10.9.9.$n" `
            -H "X-Forwarded-For: 10.9.9.$n" `
            -H "X-Real-IP: 10.9.9.$n" `
            -H 'Content-Type: application/json' `
            --data-binary "@$bodyFile" `
            "$BaseUrl/api/v1/auth/login"
    }
    finally {
        Remove-Item -LiteralPath $bodyFile -ErrorAction SilentlyContinue
    }
    $codes += [string]$code
    if ($n -eq 1) { $first = [string]$code }
}

$unauthorized = @($codes | Where-Object { $_ -eq '401' }).Count
$tooMany = @($codes | Where-Object { $_ -eq '429' }).Count
Write-Host "statuses: $($codes -join ' ')"
Write-Host "401: $unauthorized  429: $tooMany  (limit: $Limit of $Requests)"

if ($tooMany -lt 1) {
    [Console]::Error.WriteLine('FAIL: the per-address limit never fired, so a forged forwarding header changed the throttle key')
    exit 1
}
if ($first -eq '429') {
    [Console]::Error.WriteLine('FAIL: every request was throttled on the first run; is the address already locked out?')
    exit 1
}

Write-Host 'OK: forged forwarding headers did not change the throttle key'
