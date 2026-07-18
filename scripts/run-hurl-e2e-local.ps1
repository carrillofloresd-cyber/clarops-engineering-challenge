$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$logPath = Join-Path $projectRoot "hurl/e2e/local-e2e-run.log"
$appLogPath = Join-Path $projectRoot "hurl/e2e/local-e2e-app.log"
$appErrPath = Join-Path $projectRoot "hurl/e2e/local-e2e-app.err.log"
$hurlExe = "C:\Program Files\hurl\hurl.exe"
$mavenExe = "C:/swd/apache-maven-3.9.16/bin/mvn.cmd"
$jdkHome = "C:\Users\Moyos\.jdk\jdk-25.0.2"
$javaExe = Join-Path $jdkHome "bin/java.exe"
$appJar = Join-Path $projectRoot "target/clarops-sr-engineer-challenge-0.0.1-SNAPSHOT.jar"
$baseUrl = "http://localhost:8080/api"
$appProcess = $null

if (-not (Test-Path $hurlExe)) {
  throw "Hurl was not found at '$hurlExe'."
}

if (-not (Test-Path $mavenExe)) {
  throw "Maven was not found at '$mavenExe'."
}

if (-not (Test-Path $jdkHome)) {
  throw "JDK 25 was not found at '$jdkHome'."
}

if (-not (Test-Path $javaExe)) {
  throw "Java executable was not found at '$javaExe'."
}

$env:JAVA_HOME = $jdkHome
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

New-Item -ItemType Directory -Path (Split-Path $logPath -Parent) -Force | Out-Null
Start-Transcript -Path $logPath -Force | Out-Null

try {
  Write-Host "Packaging application..."
  & $mavenExe -q -DskipTests package
  if ($LASTEXITCODE -ne 0) {
    throw "Maven package failed with exit code $LASTEXITCODE."
  }

  if (-not (Test-Path $appJar)) {
    throw "Expected application jar not found at '$appJar'."
  }

  Write-Host "Starting packaged app with e2e profile..."
  if (Test-Path $appLogPath) {
    Remove-Item -Path $appLogPath -Force
  }
  if (Test-Path $appErrPath) {
    Remove-Item -Path $appErrPath -Force
  }
  $appProcess = Start-Process -FilePath $javaExe -ArgumentList "-jar", $appJar, "--spring.profiles.active=e2e" -WorkingDirectory $projectRoot -PassThru -RedirectStandardOutput $appLogPath -RedirectStandardError $appErrPath

  Write-Host "Waiting for API readiness..."
  $apiReady = $false
  for ($i = 0; $i -lt 90; $i++) {
    if ($appProcess.HasExited) {
      $outTail = if (Test-Path $appLogPath) { (Get-Content $appLogPath -Tail 20) -join "`n" } else { "No stdout log found." }
      $errTail = if (Test-Path $appErrPath) { (Get-Content $appErrPath -Tail 20) -join "`n" } else { "No stderr log found." }
      $tail = "STDOUT:`n$outTail`nSTDERR:`n$errTail"
      throw "Spring Boot process exited before readiness. ExitCode=$($appProcess.ExitCode)`n$tail"
    }
    try {
      $response = Invoke-WebRequest -Uri "$baseUrl/health" -UseBasicParsing -TimeoutSec 3
      if ($response.StatusCode -eq 200) {
        $apiReady = $true
        break
      }
    } catch {
      # Wait for boot.
    }
    Start-Sleep -Seconds 2
  }

  if (-not $apiReady) {
    $outTail = if (Test-Path $appLogPath) { (Get-Content $appLogPath -Tail 20) -join "`n" } else { "No stdout log found." }
    $errTail = if (Test-Path $appErrPath) { (Get-Content $appErrPath -Tail 20) -join "`n" } else { "No stderr log found." }
    $tail = "STDOUT:`n$outTail`nSTDERR:`n$errTail"
    throw "API did not become ready in time.`n$tail"
  }

  $hurlFiles = Get-ChildItem -Path (Join-Path $projectRoot "hurl/e2e") -Filter "*.hurl" | Sort-Object Name
  if ($hurlFiles.Count -eq 0) {
    throw "No Hurl files found under hurl/e2e."
  }

  $failed = @()
  foreach ($file in $hurlFiles) {
    Write-Host "Running $($file.Name)..."
    & $hurlExe --test --variable base_url=$baseUrl $file.FullName
    if ($LASTEXITCODE -ne 0) {
      $failed += $file.Name
    }
  }

  if ($failed.Count -gt 0) {
    throw "Hurl tests failed: $($failed -join ', ')"
  }

  Write-Host "All Hurl E2E tests passed."
} finally {
  if ($appProcess) {
    Write-Host "Stopping Spring Boot process..."
    Stop-Process -Id $appProcess.Id -Force -ErrorAction SilentlyContinue
  }
  Stop-Transcript | Out-Null
}
