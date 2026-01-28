![CI](https://github.com/janis-karklins88/inventory-order-management-system/actions/workflows/ci.yml/badge.svg)


# Inventory & Orders Management System (Backend)

Spring Boot backend application for managing products, inventory, and orders.  
Designed as a **backend-focused portfolio project** with clean architecture, tests, and reproducible local setup.

---

## Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA (Hibernate)
- MySQL 8 (Docker)
- Maven (Wrapper)
- JUnit / Mockito (tests)
- OpenAPI annotations (Swagger UI currently disabled)

---

## Project Goals

- Demonstrate production-style backend structure
- Clear separation of concerns (controller / service / repository)
- Transactional business logic
- Runnable locally with **minimal setup**

---

## Requirements

- Java 17+
- Docker Desktop
- Git

No local MySQL installation is required.

---

## How to Run 

1. Start database (Docker)

```bash
docker compose up -d
```
2. Run application
```bash
./mvnw spring-boot:run
```


**Default port:**  
http://localhost:8080


## Configuration
### Spring profiles

The application uses the dev profile by default.

`application.properties`

- shared configuration

- profile activation

`application-dev.yml`

- datasource configuration for local Docker MySQL

### Database

- MySQL 8.x

- Runs in Docker

- Data is persisted using a Docker volume

To connect manually:
```bash
docker exec -it iom-mysql mysql -u iom_user -p
```


Password:

```bash 
iom_pass
```

## Testing
The project includes
- repository tests
- service tests
- controller tests
- few e2e tests

### Notes
- Swagger UI is currently disabled due to Spring Boot / springdoc compatibility.

- OpenAPI annotations are present in controllers.

- Infrastructure (DB) is containerized; the application runs locally by design.
  
- This project focuses on backend architecture and business logic.
Authentication, authorization, and frontend UI are intentionally out of scope.

## API usage
All APIs are prefixed with `/api`.

### Health check

```bash
http://localhost:8080/api/health
```
### OpenAPI Specification
The OpenAPI specification is available at:
```bash
http://localhost:8080/v3/api-docs
```

This file can be:
- imported into Postman
- imported into Insomnia
- used for client generation

Swagger UI is currently disabled.



### Author
Janis Karklins

