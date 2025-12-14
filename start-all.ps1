# Start Analyzer Service (Port 8082)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'c:\Users\김명호\IdeaProjects\Intelligent_Test_Data_Generator'; ./gradlew :itdg-analyzer:bootRun"

# Start Generator Service (Port 8083)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'c:\Users\김명호\IdeaProjects\Intelligent_Test_Data_Generator'; ./gradlew :itdg-generator:bootRun"

# Start Orchestrator Service (Port 8081)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'c:\Users\김명호\IdeaProjects\Intelligent_Test_Data_Generator'; ./gradlew :itdg-orchestrator:bootRun"

# Start Frontend (Port 3000) - Using Python SimpleHTTP for stability with Korean paths
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'c:\Users\김명호\IdeaProjects\Intelligent_Test_Data_Generator\itdg-frontend\build'; python -m http.server 3000"

Write-Host "All services are starting in new windows..."
Write-Host "Analyzer: http://localhost:8082"
Write-Host "Generator: http://localhost:8083"
Write-Host "Orchestrator: http://localhost:8081"
Write-Host "Frontend: http://localhost:3000"
