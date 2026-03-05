$ErrorActionPreference = 'Stop'
$BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { 'https://doughnut.odd-e.com' }
$InstallPrefix = if ($env:INSTALL_PREFIX) { $env:INSTALL_PREFIX } else { "$env:USERPROFILE\.local\bin" }
$DownloadUrl = "$BaseUrl/doughnut-cli-latest/doughnut"

New-Item -ItemType Directory -Force -Path $InstallPrefix | Out-Null
Invoke-WebRequest -Uri $DownloadUrl -OutFile "$InstallPrefix\doughnut" -UseBasicParsing
Write-Host "Installed doughnut to $InstallPrefix\doughnut"
Write-Host "Ensure $InstallPrefix is in your PATH"
