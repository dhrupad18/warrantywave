# WarrantyWave

WarrantyWave is a vehicle ownership portal built with Java 17, Spring Boot, Maven, Angular, Spring Security, and MySQL. It supports customer accounts, vehicle records, warranty plans and eligibility-checked claims, explainable credit assessments, EMI schedules, and payment recording.

## Prerequisites

- Java 17 and Maven 3.9+
- Node.js 20 and npm
- MySQL 8.0+ for local development, or Docker Desktop for the container setup

## Local Development

Create the MySQL database and a user, then set these environment variables in the terminal used to run the API:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/warrantywave?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:SPRING_DATASOURCE_USERNAME = 'warrantywave'
$env:SPRING_DATASOURCE_PASSWORD = 'your-local-password'
$env:JWT_SECRET = 'replace-with-a-random-secret-at-least-32-characters'
$env:ADMIN_EMAIL = 'admin@example.com'
$env:ADMIN_PASSWORD = 'change-this-admin-password'
mvn -f backend/pom.xml spring-boot:run
```

The configured admin is created only when both admin environment variables are set and that email does not already exist. Public registration always creates a `CUSTOMER` account. The API listens on `http://localhost:8080`.

In another terminal:

```powershell
cd frontend
npm install
npm start
```

The Angular dev server listens on `http://localhost:4200` and proxies `/api` to the backend. The application uses MySQL in all environments; no in-memory database is included.

## Docker

Copy `.env.example` to `.env`, update the values, then load them into PowerShell and start the full stack from the project root:

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^([^#=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') }
}
./docker/start-docker.ps1
```

The script builds the API and frontend images, starts MySQL 8.4 with a persistent Docker volume, and runs the full stack. The frontend is available at `http://localhost:4200`, the API at `http://localhost:8080`, and health at `http://localhost:8080/actuator/health`.

To stop the containers while retaining database data:

```powershell
docker rm -f warrantywave-frontend warrantywave-api warrantywave-mysql
```

## Main API Routes

- `POST /api/auth/register` and `POST /api/auth/login`
- `GET/POST /api/vehicles`
- `GET /api/warranty/plans`, `GET/POST /api/warranties`
- `GET/POST /api/claims`, `PATCH /api/claims/{id}/review` for adjusters/admins
- `GET/POST /api/finance/applications`, `PATCH /api/finance/applications/{id}/decision` for credit officers/admins
- `GET /api/finance/installments`, `POST /api/finance/installments/{id}/pay`
- `GET /actuator/health`

JWT authentication is stateless. Passwords are stored with BCrypt, customer data is scoped to the authenticated account, and staff-only decisions are role-checked.