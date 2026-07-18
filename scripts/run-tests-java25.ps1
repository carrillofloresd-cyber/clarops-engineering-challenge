$ErrorActionPreference = 'Stop'

$javaHome = 'C:\Users\Moyos\.jdk\jdk-25.0.2'
if (-not (Test-Path $javaHome)) {
  throw "JDK 25 not found at $javaHome"
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"

Write-Host 'Running mvnw test with Java 25...'
& .\mvnw.cmd test
if ($LASTEXITCODE -ne 0) {
  throw "mvnw test failed with exit code $LASTEXITCODE"
}

Write-Host 'TESTS_OK'
