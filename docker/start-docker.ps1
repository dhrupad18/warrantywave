$ErrorActionPreference = 'Stop'

$network = 'warrantywave-net'
$volume = 'warrantywave-mysql'
$mysqlPassword = if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { 'warrantywave-local' }
$jwtSecret = if ($env:JWT_SECRET) { $env:JWT_SECRET } else { 'local-development-secret-change-before-deployment-7h2Wq9Vt4Kp8Dx3M' }

if (-not (docker network ls --format '{{.Name}}' | Select-String -Quiet "^$network$")) {
    docker network create $network | Out-Null
}
docker volume create $volume | Out-Null

if (-not (docker ps -a --format '{{.Names}}' | Select-String -Quiet '^warrantywave-mysql$')) {
    docker run -d --name warrantywave-mysql --network $network --restart unless-stopped `
        -e MYSQL_DATABASE=warrantywave -e MYSQL_USER=warrantywave `
        -e "MYSQL_PASSWORD=$mysqlPassword" -e "MYSQL_ROOT_PASSWORD=$mysqlPassword" `
        -v "${volume}:/var/lib/mysql" -p 3306:3306 mysql:8.4 | Out-Null
} else {
    docker start warrantywave-mysql | Out-Null
}

docker build -f ./docker/backend.Dockerfile -t warrantywave-api ./backend
if (docker ps -a --format '{{.Names}}' | Select-String -Quiet '^warrantywave-api$') {
    docker rm -f warrantywave-api | Out-Null
}
docker run -d --name warrantywave-api --network $network --restart unless-stopped `
    -e SPRING_DATASOURCE_URL='jdbc:mysql://warrantywave-mysql:3306/warrantywave?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' `
    -e SPRING_DATASOURCE_USERNAME=warrantywave -e "SPRING_DATASOURCE_PASSWORD=$mysqlPassword" `
    -e "JWT_SECRET=$jwtSecret" -e FRONTEND_ORIGIN=http://localhost:4200 `
    -e "ADMIN_EMAIL=$env:ADMIN_EMAIL" -e "ADMIN_PASSWORD=$env:ADMIN_PASSWORD" `
    -p 8080:8080 warrantywave-api | Out-Null

docker build -f ./docker/frontend.Dockerfile -t warrantywave-frontend ./frontend
if (docker ps -a --format '{{.Names}}' | Select-String -Quiet '^warrantywave-frontend$') {
    docker rm -f warrantywave-frontend | Out-Null
}
docker run -d --name warrantywave-frontend --network $network --restart unless-stopped `
    -p 4200:80 warrantywave-frontend | Out-Null

Write-Output 'WarrantyWave is starting: http://localhost:4200'
Write-Output 'API health: http://localhost:8080/actuator/health'