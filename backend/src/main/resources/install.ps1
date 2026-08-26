$ErrorActionPreference = 'Stop'
$BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { 'https://storage.googleapis.com/dough-frontend-01' }
$InstallPrefix = if ($env:INSTALL_PREFIX) { $env:INSTALL_PREFIX } else { "$env:USERPROFILE\.local\bin" }
$DownloadUrl = "$BaseUrl/doughnut-cli-latest/doughnut"

New-Item -ItemType Directory -Force -Path $InstallPrefix | Out-Null
Invoke-WebRequest -Uri $DownloadUrl -OutFile "$InstallPrefix\donut" -UseBasicParsing
Write-Host "Installed donut to $InstallPrefix\donut"
Write-Host "Ensure $InstallPrefix is in your PATH"
