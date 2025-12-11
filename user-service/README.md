## User Service (Spring Boot 3, Java 21, JWT, Postgres)

### Run locally (no Docker)

1. Ensure Postgres is running locally with:
   - **DB**: `user_service_db`
   - **User**: `user_service`
   - **Password**: `user_service_password`
2. From `user-service` folder:
   ```bash
   mvn spring-boot:run
   ```

### Run with Docker / Docker Compose

From the `user-service` folder:

```bash
docker compose up --build
```

This will start:
- **Postgres** on `localhost:5432`
- **User Service** on `localhost:8081`

### Key Endpoints

- **Register**: `POST /api/auth/register`
  - Body:
    ```json
    {
      "username": "john",
      "email": "john@example.com",
      "password": "Password123!",
      "role": "CUSTOMER"
    }
    ```
- **Login**: `POST /api/auth/login`
  - Body:
    ```json
    {
      "username": "john",
      "password": "Password123!"
    }
    ```
- **Current user (for welcome page)**: `GET /api/auth/me`
  - Header: `Authorization: Bearer <jwt-token-from-login-or-register>`


