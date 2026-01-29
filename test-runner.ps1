#!/usr/bin/env pwsh
# Simple test runner script
$ErrorActionPreference = "Continue"

Write-Host "Running SimulationRunControllerTest..." -ForegroundColor Cyan

& mvn clean test "-Dtest=SimulationRunControllerTest" 2>&1 | 
    Select-String -Pattern "Tests run:|Failures:|Errors:|testDownloadSimulationArtefact" |
    ForEach-Object { Write-Host $_ }

Write-Host "`nTest execution completed." -ForegroundColor Green
