$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$dockerComposeFile = Join-Path $projectRoot "docker/docker-compose.yml"
$dockerEnvPath = Join-Path $projectRoot "docker/.env"
$dockerEnvExamplePath = Join-Path $projectRoot "docker/example.env"
$appLogPath = Join-Path $projectRoot "hurl/e2e/full-e2e-app.log"
$hurlExe = "C:\Program Files\hurl\hurl.exe"
$mavenExe = "C:/swd/apache-maven-3.9.16/bin/mvn.cmd"
$jdkHome = "C:\Users\Moyos\.jdk\jdk-25.0.2"
$baseUrl = "http://localhost:8080/api"
$appProcess = $null
$useDockerMode = $true

if (-not (Test-Path $hurlExe)) {
  throw "Hurl was not found at '$hurlExe'. Install it first."
}

if (-not (Test-Path $mavenExe)) {
  throw "Maven was not found at '$mavenExe'. Update the path in scripts/run-hurl-e2e.ps1."
}

if (-not (Test-Path $jdkHome)) {
  throw "JDK 25 was not found at '$jdkHome'."
}

$env:JAVA_HOME = $jdkHome
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

if (-not (Test-Path $dockerEnvPath) -and (Test-Path $dockerEnvExamplePath)) {
  Copy-Item -Path $dockerEnvExamplePath -Destination $dockerEnvPath
}

$previousErrorPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
& docker info *> $null
$dockerInfoExitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorPreference

if ($dockerInfoExitCode -ne 0) {
  $useDockerMode = $false
}

if ($useDockerMode) {
  Write-Host "Starting PostgreSQL with Docker Compose..."
  & docker compose -f $dockerComposeFile up -d challenge-postgresql | Out-Null

  Write-Host "Waiting for PostgreSQL to be ready..."
  $ready = $false
  for ($i = 0; $i -lt 60; $i++) {
    $containerIdRaw = & docker compose -f $dockerComposeFile ps -q challenge-postgresql
    if ($containerIdRaw) {
      $containerId = $containerIdRaw.Trim()
      if ($containerId) {
        $status = (& docker inspect -f "{{.State.Health.Status}}" $containerId).Trim()
        if ($status -eq "healthy") {
          $ready = $true
          break
        }
      }
    }
    Start-Sleep -Seconds 2
  }

  if (-not $ready) {
    throw "PostgreSQL did not become healthy in time."
  }

  Write-Host "Resetting trace data..."
  & docker compose -f $dockerComposeFile exec -T challenge-postgresql psql -U clarops -d clarops_challenge -c "TRUNCATE TABLE clarops_challenge_schema.trace_events, clarops_challenge_schema.traces RESTART IDENTITY CASCADE;" | Out-Null
} else {
  Write-Host "Docker daemon is unavailable, running with Spring test profile (in-memory DB)."
}

Write-Host "Starting Spring Boot application..."
if ($useDockerMode) {
  if (Test-Path $appLogPath) {
    Remove-Item -Path $appLogPath -Force
  }
  $appProcess = Start-Process -FilePath $mavenExe -ArgumentList "spring-boot:run" -WorkingDirectory $projectRoot -PassThru -RedirectStandardOutput $appLogPath -RedirectStandardError $appLogPath
} else {
  if (Test-Path $appLogPath) {
    Remove-Item -Path $appLogPath -Force
  }
  $appProcess = Start-Process -FilePath $mavenExe -ArgumentList "spring-boot:run", "-Dspring-boot.run.profiles=e2e" -WorkingDirectory $projectRoot -PassThru -RedirectStandardOutput $appLogPath -RedirectStandardError $appLogPath
}

Write-Host "Waiting for API health endpoint..."
$apiReady = $false
for ($i = 0; $i -lt 90; $i++) {
  if ($appProcess.HasExited) {
    $tail = if (Test-Path $appLogPath) { (Get-Content $appLogPath -Tail 40) -join "`n" } else { "No app log found." }
    throw "Spring Boot process exited before readiness. ExitCode=$($appProcess.ExitCode)`n$tail"
  }
  try {
    $response = Invoke-WebRequest -Uri "$baseUrl/health" -UseBasicParsing -TimeoutSec 3
    if ($response.StatusCode -eq 200) {
      $apiReady = $true
      break
    }
  } catch {
    # Keep waiting while the app starts.
  }
  Start-Sleep -Seconds 2
}

if (-not $apiReady) {
  if ($appProcess) {
    Stop-Process -Id $appProcess.Id -Force -ErrorAction SilentlyContinue
  }
  $tail = if (Test-Path $appLogPath) { (Get-Content $appLogPath -Tail 40) -join "`n" } else { "No app log found." }
  throw "API did not become ready in time.`n$tail"
}

Write-Host "Running Hurl E2E suite..."
$hurlResult = & $hurlExe --test --variable base_url=$baseUrl (Join-Path $projectRoot "hurl/e2e/*.hurl")
$exitCode = $LASTEXITCODE
$hurlResult

if ($appProcess) {
  Write-Host "Stopping Spring Boot application..."
  Stop-Process -Id $appProcess.Id -Force -ErrorAction SilentlyContinue
}

if ($exitCode -ne 0) {
  throw "Hurl E2E suite failed with exit code $exitCode."
}

Write-Host "All Hurl E2E tests passed."
