# SWEN3 Paperless Application

A Spring Boot application for document management with PostgreSQL database and pgAdmin interface.

## Architecture

![System Architecture](artifacts/architecture.svg)

The system follows a microservices architecture with the following components:

- **Next.js Web Frontend** (Port 3000): User interface for document management
- **Spring Boot REST API** (Port 4000): Backend API handling business logic
- **Kafka Message Queue** (Port 9092): Event streaming for OCR processing
- **OCR Worker**: Scalable workers for document text extraction
- **PostgreSQL Database** (Port 5455): Data persistence
- **pgAdmin** (Port 5050): Database administration interface
- **File Storage**: Document and file storage system

## Prerequisites

- Docker and Docker Compose installed on your system
- Git for cloning the repository

## Quick Start

### Build and Start the Application

```bash
# Build and start all services (database, app, pgAdmin)
docker compose up --build -d

# Or run in foreground to see logs
docker compose up --build --scale worker=3
```

### Access the Services

- **Spring Boot API**: http://localhost:8080
- **pgAdmin (Database Management)**: http://localhost:5050
  - Email: `admin@admin.com`
  - Password: `admin`
- **PostgreSQL Database**: localhost:5455 (from host machine)

## Database Configuration

The application uses PostgreSQL with the following configuration:

- **Database Name**: `paperlessdb`
- **Username**: `paperless_user`
- **Password**: `paperless_pw`
- **Port**: 5455 (host) → 5432 (container)
