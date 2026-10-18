<#
.SYNOPSIS
Generates strong random values for the Sentinel deploy secrets and prints them.

.DESCRIPTION
Native PowerShell, no WSL required. This script never writes a file: it prints
one NAME=value line per secret so you can paste them into deploy\.env. Keep the
printed values, because the enrolment key is needed again every time you enrol a
new agent.

.EXAMPLE
cd deploy
.\scripts\generate-secrets.ps1
#>
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

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
