<#
.SYNOPSIS
Generates strong random values for the Sentinel deploy secrets.

.DESCRIPTION
Native PowerShell, no WSL required.

Without -Write the script only prints: one NAME=value line per secret, which you
can paste into deploy\.env yourself. It never writes a file.

With -Write the script creates deploy\.env from .env.example if it does not
exist, replaces the SENTINEL_DB_PASSWORD, SENTINEL_JWT_SECRET and
SENTINEL_ENROLLMENT_KEY lines in place with fresh random values without showing
them, and asks for the bootstrap admin password on a hidden prompt (at least 12
characters). SENTINEL_DOMAIN is left alone and SENTINEL_ADMIN_USERNAME is set to
admin unless it already has a value. Values that are already real are not
overwritten unless -Force is given. The file is written with LF line endings.

.PARAMETER Write
Create or update deploy\.env instead of printing the values.

.PARAMETER Force
Overwrite values in deploy\.env that are not placeholders.

.EXAMPLE
cd deploy
.\scripts\generate-secrets.ps1 -Write

.EXAMPLE
cd deploy
.\scripts\generate-secrets.ps1
#>
[CmdletBinding()]
param(
    [switch]$Write,
    [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$Placeholder = 'REPLACE_WITH_GENERATED_VALUE'
$LegacyPlaceholder = 'change-me'
$SecretKeys = @(
    'SENTINEL_DB_PASSWORD',
    'SENTINEL_JWT_SECRET',
    'SENTINEL_ENROLLMENT_KEY'
)
$TargetKeys = $SecretKeys + @('SENTINEL_ADMIN_PASSWORD')
$MinPasswordLength = 12

function New-Secret {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    }
    finally {
        $rng.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

function Get-EnvEntries {
    param([string]$Path)
    $entries = [ordered]@{}
    foreach ($line in [System.IO.File]::ReadAllLines($Path)) {
        $separator = $line.IndexOf('=')
        if ($separator -lt 1) { continue }
        $key = $line.Substring(0, $separator)
        if (-not $entries.Contains($key)) {
            $entries[$key] = $line.Substring($separator + 1)
        }
    }
    return $entries
}

function Test-RealValue {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return $false }
    if ($Value -eq $Placeholder -or $Value -eq $LegacyPlaceholder) { return $false }
    return $true
}

function Set-EnvEntry {
    param(
        [System.Collections.Generic.List[string]]$Lines,
        [string]$Key,
        [string]$Value
    )
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        if ($Lines[$i].StartsWith("$Key=")) {
            $Lines[$i] = "$Key=$Value"
            return
        }
    }
    $Lines.Add("$Key=$Value")
}

function Read-AdminPassword {
    if ([Console]::IsInputRedirected) {
        $plain = [Console]::In.ReadLine()
        if ($null -eq $plain) {
            [Console]::Error.WriteLine('No admin password was read from the redirected input.')
            exit 1
        }
        if ($plain.Length -lt $MinPasswordLength) {
            [Console]::Error.WriteLine("SENTINEL_ADMIN_PASSWORD must be at least $MinPasswordLength characters.")
            exit 1
        }
        return $plain
    }
    while ($true) {
        $secure = Read-Host -AsSecureString -Prompt "New SENTINEL_ADMIN_PASSWORD (hidden, at least $MinPasswordLength characters)"
        if ($null -eq $secure) {
            [Console]::Error.WriteLine('No admin password was read from the input.')
            exit 1
        }
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try {
            $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        }
        finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
        if ($null -ne $plain -and $plain.Length -ge $MinPasswordLength) {
            return $plain
        }
        Write-Host "SENTINEL_ADMIN_PASSWORD must be at least $MinPasswordLength characters, try again."
    }
}

function Write-EnvFile {
    param(
        [string]$Path,
        [System.Collections.Generic.List[string]]$Lines
    )
    $text = [string]::Join("`n", $Lines) + "`n"
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $text, $utf8NoBom)
}

if (-not $Write) {
    Write-Host ''
    Write-Host 'Sentinel deploy secrets'
    Write-Host '======================='
    Write-Host 'Paste these lines into deploy\.env (copy .env.example to .env first).'
    Write-Host 'Each value is a fresh 32-byte random secret.'
    Write-Host ''

    $secrets = [ordered]@{
        'SENTINEL_DB_PASSWORD'       = New-Secret
        'SENTINEL_JWT_SECRET'        = New-Secret
        'SENTINEL_ENROLLMENT_KEY'    = New-Secret
        'SENTINEL_ADMIN_PASSWORD'    = New-Secret
    }

    foreach ($name in $secrets.Keys) {
        Write-Output ("{0}={1}" -f $name, $secrets[$name])
    }

    Write-Host ''
    Write-Host 'Also set in deploy\.env:'
    Write-Host '  SENTINEL_DOMAIN          the DNS name this server answers on (use localhost to try it locally)'
    Write-Host '  SENTINEL_ADMIN_USERNAME  the bootstrap admin username'
    Write-Host ''
    Write-Host 'Save every printed value in a password manager now. They are not stored anywhere by this script.'
    Write-Host 'The enrolment key is needed again for every new agent you enrol, not just once.'
    return
}

$deployDir = Split-Path -Parent $PSScriptRoot
$examplePath = Join-Path $deployDir '.env.example'
$envPath = Join-Path $deployDir '.env'

if (-not (Test-Path $examplePath)) {
    [Console]::Error.WriteLine("Missing template: $examplePath")
    exit 1
}

if (-not (Test-Path $envPath)) {
    Copy-Item -LiteralPath $examplePath -Destination $envPath
    Write-Host "Created $envPath from .env.example"
}

$entries = Get-EnvEntries -Path $envPath

$alreadySet = @()
foreach ($key in $TargetKeys) {
    $current = ''
    if ($entries.Contains($key)) {
        $current = [string]$entries[$key]
    }
    if (Test-RealValue -Value $current) {
        $alreadySet += $key
    }
}

if ($alreadySet.Count -gt 0 -and -not $Force) {
    Write-Host ''
    Write-Host "Refusing to overwrite real values already in $envPath :"
    foreach ($key in $alreadySet) {
        Write-Host "  $key"
    }
    Write-Host 'Re-run with -Force to replace them, or edit the file by hand.'
    exit 1
}

$adminPassword = Read-AdminPassword

$fileLines = New-Object 'System.Collections.Generic.List[string]'
$fileLines.AddRange([string[]][System.IO.File]::ReadAllLines($envPath))

foreach ($key in $SecretKeys) {
    Set-EnvEntry -Lines $fileLines -Key $key -Value (New-Secret)
}
Set-EnvEntry -Lines $fileLines -Key 'SENTINEL_ADMIN_PASSWORD' -Value $adminPassword

$usernameSet = $false
if ($entries.Contains('SENTINEL_ADMIN_USERNAME')) {
    $usernameSet = Test-RealValue -Value ([string]$entries['SENTINEL_ADMIN_USERNAME'])
}
if (-not $usernameSet) {
    Set-EnvEntry -Lines $fileLines -Key 'SENTINEL_ADMIN_USERNAME' -Value 'admin'
}

Write-EnvFile -Path $envPath -Lines $fileLines

Write-Host ''
Write-Host "Wrote $envPath (values are not shown):"
Write-Host '  SENTINEL_DB_PASSWORD, SENTINEL_JWT_SECRET, SENTINEL_ENROLLMENT_KEY  fresh random values'
Write-Host '  SENTINEL_ADMIN_PASSWORD                                             the password you typed'
if ($usernameSet) {
    Write-Host '  SENTINEL_ADMIN_USERNAME                                             kept as it is'
}
else {
    Write-Host '  SENTINEL_ADMIN_USERNAME                                             set to admin'
}
Write-Host '  SENTINEL_DOMAIN                                                     left unchanged'
Write-Host ''
Write-Host 'Next: set SENTINEL_DOMAIN in the file if needed, then run:'
Write-Host '  docker compose up -d --build'
Write-Host ''
Write-Host 'WARNING: PostgreSQL only reads SENTINEL_DB_PASSWORD when the database volume is'
Write-Host 'first created. Changing it later leaves the old password inside the volume and'
Write-Host 'breaks the connection. For a trial, run: docker compose down -v'
