[CmdletBinding()]
param(
    [switch]$Build,
    [string]$JavaHome,
    [string]$EnvFile,
    [ValidateRange(1, 65535)][int]$Port = 8087
)
$ErrorActionPreference = 'Stop'
$moduleRoot = Split-Path $PSScriptRoot -Parent
$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $moduleRoot '../..'))
$originalDirectory = (Get-Location).Path
$allowed = @('GOOGLE_API_KEY', 'GEMINI_API_KEY', 'GEMINI_CROP_MODEL', 'GOOGLE_CLOUD_PROJECT_ID', 'GOOGLE_CLOUD_PROJECT', 'GOOGLE_CLOUD_LOCATION', 'GCP_REGION', 'GCLOUD_ADC', 'GOOGLE_APPLICATION_CREDENTIALS', 'VISUALS_API_KEY', 'VISUALS_AI_ENABLED')
$values = @{}
$sources = @{}
$commonDirectory = & git -C $repositoryRoot rev-parse --git-common-dir 2>$null
$envFiles = @()
if ($LASTEXITCODE -eq 0 -and $commonDirectory) {
    if (-not [IO.Path]::IsPathRooted($commonDirectory)) { $commonDirectory = Join-Path $repositoryRoot $commonDirectory }
    $envFiles += Join-Path (Split-Path ([IO.Path]::GetFullPath($commonDirectory)) -Parent) '.env'
}
$envFiles += Join-Path $repositoryRoot '.env'
$envFiles += Join-Path $moduleRoot '.env'
if ($EnvFile) {
    $envFiles += [IO.Path]::GetFullPath($EnvFile)
    if (-not (Test-Path -LiteralPath $envFiles[-1] -PathType Leaf)) { throw 'Le fichier -EnvFile est introuvable.' }
}
foreach ($file in $envFiles) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { continue }
    foreach ($line in Get-Content -LiteralPath $file) {
        if ($line -notmatch '^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') { continue }
        $key = $Matches[1]; $value = $Matches[2]
        if ($key -notin $allowed) { continue }
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        } else { $value = ($value -replace '\s+#.*$', '').Trim() }
        $values[$key] = $value
        $sources[$key] = Split-Path $file -Parent
    }
}
foreach ($key in $allowed) {
    $existing = [Environment]::GetEnvironmentVariable($key, 'Process')
    if (-not [string]::IsNullOrEmpty($existing)) { $values[$key] = $existing; $sources[$key] = $originalDirectory }
}
foreach ($key in @('GOOGLE_APPLICATION_CREDENTIALS', 'GCLOUD_ADC')) {
    if ($values[$key]) {
        $path = $values[$key]
        if ($path.StartsWith('~/') -or $path.StartsWith('~\')) { $path = Join-Path $HOME $path.Substring(2) }
        if (-not [IO.Path]::IsPathRooted($path)) { $path = Join-Path $sources[$key] $path }
        $values[$key] = [IO.Path]::GetFullPath($path)
    }
}
if (-not $values['GOOGLE_APPLICATION_CREDENTIALS'] -and $values['GCLOUD_ADC']) { $values['GOOGLE_APPLICATION_CREDENTIALS'] = $values['GCLOUD_ADC'] }
if (-not $JavaHome) { $JavaHome = $env:JAVA_HOME }
if (-not $JavaHome -and (Test-Path -LiteralPath (Join-Path $HOME '.jdks'))) {
    $candidate = Get-ChildItem -LiteralPath (Join-Path $HOME '.jdks') -Directory | Where-Object Name -Match '(?:jdk|temurin|corretto|zulu)[-_]?21(?:\D|$)' | Sort-Object Name -Descending | Select-Object -First 1
    if ($candidate) { $JavaHome = $candidate.FullName }
}
$java = if ($JavaHome) { Join-Path $JavaHome 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
$version = & $java --version 2>&1
if ($LASTEXITCODE -ne 0 -or ($version | Select-Object -First 1) -notmatch '\b21(?:\.|\s|$)') { throw 'Java 21 est requis. Utilisez -JavaHome pour sélectionner son JDK.' }
$previous = @{}
try {
    if ($JavaHome) { $values['JAVA_HOME'] = $JavaHome; $values['PATH'] = (Join-Path $JavaHome 'bin') + [IO.Path]::PathSeparator + $env:PATH }
    foreach ($key in $values.Keys) { $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process'); [Environment]::SetEnvironmentVariable($key, $values[$key], 'Process') }
    Push-Location $moduleRoot
    try {
        # Windows Unix-domain sockets require a shorter path than the usual user Temp.
        $socketDirectory = Join-Path $HOME '.vz'
        New-Item -ItemType Directory -Force -Path $socketDirectory | Out-Null
        if ($Build) {
            & (Join-Path $moduleRoot 'mvnw.cmd') "-Djdk.net.unixdomain.tmpdir=$socketDirectory" package
            if ($LASTEXITCODE -ne 0) { throw 'La compilation a échoué.' }
        }
        $jar = Join-Path $moduleRoot 'target/quarkus-app/quarkus-run.jar'
        if (-not (Test-Path -LiteralPath $jar)) { throw 'Application non compilée. Relancez avec -Build.' }
        Write-Host "Visuals Agent : http://127.0.0.1:$Port (Ctrl+C pour arrêter)"
        & $java '-Djava.awt.headless=true' "-Djdk.net.unixdomain.tmpdir=$socketDirectory" '-Dquarkus.http.host=127.0.0.1' "-Dquarkus.http.port=$Port" -jar $jar
        if ($LASTEXITCODE -ne 0) { throw "Le service s'est arrêté avec le code $LASTEXITCODE." }
    } finally { Pop-Location }
} finally {
    foreach ($key in $previous.Keys) { [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process') }
}
