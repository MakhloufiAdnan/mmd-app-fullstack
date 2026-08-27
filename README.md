# Java Angular Social App

Full-stack social application built with **Java 21, Spring Boot, Angular and MySQL**.

The project implements a small topic-based social network where users can create an account, subscribe to topics, publish posts, comment and browse a chronological feed.

## ✨ Features

* User registration and authentication
* Persistent authentication using HttpOnly refresh-token cookies
* Topic subscription and unsubscription
* Chronological feed with ascending / descending sorting
* Post creation and detailed post view
* Comments
* User profile management
* CSRF protection for state-changing requests

## 🛠️ Tech Stack

### Backend

* Java 21
* Spring Boot 3
* REST APIs
* MySQL
* JUnit 5
* MockMvc
* Testcontainers
* JaCoCo

### Frontend

* Angular
* TypeScript
* Angular Material
* Karma / Jasmine
* Istanbul coverage

### Quality & Tooling

* Docker
* SonarQube
* Postman
* Git

## 🏗️ Architecture

The project is organized as a monorepo:

```text
.
├── front/      # Angular SPA
├── back/       # Spring Boot REST API
├── docs/       # Technical documentation and API contract
└── Postman/    # API testing resources
```

The Angular application communicates with the backend through `/api`.

During local development, the Angular development server proxies API requests to the Spring Boot application.

## 🔐 Authentication & Security

Authentication includes:

* Access and refresh-token flows
* Refresh token stored in an HttpOnly cookie
* Logout support
* CSRF protection
* Authentication-protected API endpoints

Additional security and implementation decisions are documented in the project documentation.

## 🧪 Testing & Quality

The project includes:

* Backend unit and integration tests
* Spring MVC tests with MockMvc
* Database integration tests with Testcontainers
* Frontend tests
* Backend coverage with JaCoCo
* Frontend coverage with Istanbul
* SonarQube static analysis

## 🚀 Running locally

### Requirements

* Java 21
* Node.js / npm
* Docker
* Git

### Database

```bash
cd back
docker compose up -d
```

### Backend

```bash
cd back
./mvnw spring-boot:run
```

Backend:

```text
http://localhost:8080
```

### Frontend

```bash
cd front
npm install
npm start
```

Frontend:

```text
http://localhost:4200
```

## 📚 Documentation

Additional documentation is available in `docs/`, including:

* Technical decisions
* API contract
* Privacy and cookie considerations
* Testing and quality information

## 📌 Project Scope

This repository represents an MVP focused on Full-Stack application development, REST API design, authentication, automated testing and software quality practices.
