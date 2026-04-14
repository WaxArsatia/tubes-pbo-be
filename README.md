# Tubes PBO Backend

Spring Boot backend for PDF summarization and quiz generation with user authentication and admin monitoring.

## Overview

This service provides:

- User registration, login, email verification, and password reset
- PDF upload and AI-generated summary creation
- Quiz generation from summaries and answer submission
- User profile and password management
- Summary history, deletion, and PDF downloads
- Admin endpoints for user management and activity/dashboard views

Base API path: `/api`

## Features

- Session-based bearer authentication (UUID token)
- Role support: `USER` and `ADMIN`
- PDF-only upload validation
- Persistent storage with MariaDB + JPA
- OpenAPI/Swagger documentation

## Tech Stack

- Java 25
- Spring Boot 3.5.9
- Spring Web, Validation, Security, Data JPA, Mail
- Spring AI (`spring-ai-starter-model-google-genai`)
- MariaDB JDBC driver
- iText Core 9.4.0 (PDF handling)
- springdoc-openapi 2.8.15
- Lombok
- Maven Wrapper (`mvnw`)

## Setup and Run

### 1. Prerequisites

- Java 25
- MariaDB

### 2. Configure environment

Copy env template:

```bash
cp .env.example .env
```

Set required values in `.env`:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `GOOGLE_AI_API_KEY` (required for AI summary/quiz generation)
- `MAIL_USERNAME`, `MAIL_PASSWORD` (for verification/reset emails)

Optional app settings are also available in `.env.example` (CORS, file upload limits, URLs, token expiry).

### 3. Run

```bash
./mvnw spring-boot:run
```

Default server URL: `http://localhost:8080`

### 4. Test

```bash
./mvnw test
```

## API Documentation

- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/docs`

## Implemented Endpoint Groups

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/verify`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `POST /api/auth/logout`

- `POST /api/summaries`
- `GET /api/summaries`
- `GET /api/summaries/{id}`

- `POST /api/quizzes`
- `POST /api/quizzes/{id}/submit`
- `GET /api/quizzes`
- `GET /api/quizzes/{id}`

- `GET /api/history`
- `DELETE /api/history/{id}`
- `GET /api/history/{id}/download`
- `GET /api/history/{id}/download-original`

- `GET /api/settings`
- `PUT /api/settings`
- `PUT /api/settings/password`

- `GET /api/admin/users`
- `GET /api/admin/users/{id}`
- `POST /api/admin/users`
- `PUT /api/admin/users/{id}`
- `DELETE /api/admin/users/{id}`
- `GET /api/admin/dashboard`
- `GET /api/admin/activity`

## Project Structure

```text
src/main/java/tubes/pbo/be/
  auth/       # Authentication, session, email tokens
  summary/    # PDF upload and summary generation
  quiz/       # Quiz generation and submission
  history/    # Summary history and download/delete
  settings/   # Profile and password updates
  admin/      # Admin management and monitoring
  user/       # User entity/repository
  shared/     # Config, security, exceptions, common DTOs
```

Main configuration files:

- `application.properties`
- `.env.example`
- `pom.xml`
