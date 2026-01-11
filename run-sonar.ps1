<# 
  run-sonar.ps1
  - Lance tests + coverage back/front, puis sonar-scanner à la racine du mono-repo
  - Charge SONAR_TOKEN depuis:
      1) variable d'env SONAR_TOKEN
      2) sinon front/.env (gitignored)
  - Vérifie que jacoco.xml et lcov.info existent et ne sont pas vides
#>

param(
  # URL SonarQube (si vide: SONAR_HOST_URL env, sinon http://localhost:9000)
  [string]$SonarHostUrl,

  # Chemin relatif du fichier .env contenant SONAR_TOKEN=...
  [string]$EnvFilePath = "front\.env"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step([string]$Message) {
  Write-Host "`n== $Message ==" -ForegroundColor Cyan
}

function Test-CommandExists([string]$CommandName) {
  $cmd = Get-Command $CommandName -ErrorAction SilentlyContinue
  if (-not $cmd) {
    throw "Commande introuvable: '$CommandName'. Vérifie l'installation et le PATH."
  }
}

function Test-FileNonEmpty([string]$Path) {
  if (-not (Test-Path -LiteralPath $Path)) {
    throw "Fichier introuvable: $Path"
  }
  $len = (Get-Item -LiteralPath $Path).Length
  if ($len -le 0) {
    throw "Fichier vide: $Path"
  }
}

function Get-SonarTokenFromEnvFile([string]$AbsoluteEnvFilePath) {
  if (-not (Test-Path -LiteralPath $AbsoluteEnvFilePath)) {
    return $null
  }

  # Cherche la première ligne SONAR_TOKEN=...
  $line = Get-Content -LiteralPath $AbsoluteEnvFilePath |
    Where-Object { $_ -match '^\s*SONAR_TOKEN\s*=' } |
    Select-Object -First 1

  if (-not $line) { return $null }

  $value = ($line -replace '^\s*SONAR_TOKEN\s*=\s*', '').Trim()

  # Support simple des commentaires inline: SONAR_TOKEN=xxx # comment
  if ($value -match '^(.*?)\s+#') {
    $value = $Matches[1].Trim()
  }

  # Enlève guillemets éventuels
  if ($value.StartsWith('"') -and $value.EndsWith('"')) { $value = $value.Trim('"') }
  if ($value.StartsWith("'") -and $value.EndsWith("'")) { $value = $value.Trim("'") }

  if ($value.Length -eq 0) { return $null }
  return $value
}

function Get-ProjectKeyFromProperties([string]$PropertiesPath) {
  if (-not (Test-Path -LiteralPath $PropertiesPath)) { return $null }
  $line = Get-Content -LiteralPath $PropertiesPath |
    Where-Object { $_ -match '^\s*sonar\.projectKey\s*=' } |
    Select-Object -First 1
  if (-not $line) { return $null }
  return (($line -split '=', 2)[1]).Trim()
}

# Toujours se placer à la racine du repo (dossier du script)
Push-Location $PSScriptRoot
try {
  # Defaults (hors param pour éviter erreurs de parsing)
  if (-not $SonarHostUrl -or $SonarHostUrl.Trim().Length -eq 0) {
    if ($env:SONAR_HOST_URL -and $env:SONAR_HOST_URL.Trim().Length -gt 0) {
      $SonarHostUrl = $env:SONAR_HOST_URL
    } else {
      $SonarHostUrl = "http://localhost:9000"
    }
  }

  Write-Step "Pré-checks"
  Test-CommandExists "sonar-scanner"
  Test-CommandExists "mvn"
  Test-CommandExists "npx"

  # Charger le token
  if (-not $env:SONAR_TOKEN -or $env:SONAR_TOKEN.Trim().Length -eq 0) {
    $absEnv = Join-Path $PSScriptRoot $EnvFilePath
    $tokenFromFile = Get-SonarTokenFromEnvFile $absEnv
    if ($tokenFromFile) {
      $env:SONAR_TOKEN = $tokenFromFile
    }
  }

  if (-not $env:SONAR_TOKEN -or $env:SONAR_TOKEN.Trim().Length -eq 0) {
    throw "SONAR_TOKEN manquant. Mets-le dans front/.env (SONAR_TOKEN=...) ou via `$env:SONAR_TOKEN='...'"
  }

  Write-Step "Back: mvn clean test jacoco:report"
  Push-Location ".\back"
  try {
    mvn clean test jacoco:report
  }
  finally {
    Pop-Location
  }

  Write-Step "Front: ng test (ChromeHeadless) + coverage"
  Push-Location ".\front"
  try {
    npx ng test --watch=false --browsers=ChromeHeadless --code-coverage
  }
  finally {
    Pop-Location
  }

  Write-Step "Vérification des rapports coverage"
  $jacocoXml = Join-Path $PSScriptRoot "back\target\site\jacoco\jacoco.xml"
  $lcovInfo  = Join-Path $PSScriptRoot "front\coverage\lcov.info"
  Test-FileNonEmpty $jacocoXml
  Test-FileNonEmpty $lcovInfo

  Write-Step "Sonar: sonar-scanner (mono-repo)"
  sonar-scanner `
    -D "sonar.host.url=$SonarHostUrl" `
    -D "sonar.login=$env:SONAR_TOKEN"

  $projectKey = Get-ProjectKeyFromProperties (Join-Path $PSScriptRoot "sonar-project.properties")
  if ($projectKey) {
    Write-Host "`nOK - Dashboard: $SonarHostUrl/dashboard?id=$projectKey"
  } else {
    Write-Host "`nOK - Analyse envoyée. Ouvre SonarQube: $SonarHostUrl"
  }
}
finally {
  Pop-Location
}
