# Tubes PBO Backend

AI-powered PDF summarization and quiz generation REST API built with Spring Boot 3.5.9 and Google Gemini.

## 📋 Overview

This Spring Boot application provides intelligent PDF document processing capabilities:
- **PDF Summarization**: Upload PDFs and get AI-generated summaries
- **Quiz Generation**: Generate customizable quizzes from summaries
- **User Management**: Secure authentication with email verification
- **Admin Dashboard**: User management and activity monitoring
- **History Tracking**: Manage and download processed documents

## 🛠️ Tech Stack

- **Java 25** with **Spring Boot 3.5.9**
- **Spring Data JPA** + **MariaDB** - Data persistence
- **Spring Security** + JWT/UUID tokens - Authentication & authorization
- **Spring AI** + **Google Gemini** - AI-powered content generation
- **iText PDF 9.4.0** - PDF processing
- **Spring Mail** - Email notifications
- **Springdoc OpenAPI 2.8.15** - API documentation
- **Lombok** - Boilerplate reduction
- **Maven** - Build automation

## 🏗️ Architecture

Clean architecture with domain-driven design:

```
src/main/java/tubes/pbo/be/
├── auth/              # Authentication & registration
├── summary/           # PDF summarization domain
├── quiz/              # Quiz generation domain
├── settings/          # User profile management
├── history/           # Document history & downloads
├── admin/             # Admin management
├── user/              # Shared user entity
└── shared/            # Cross-cutting concerns
    ├── config/        # Security, OpenAPI, file storage
    ├── security/      # JWT filters, security helpers
    ├── exception/     # Global exception handling
    └── dto/           # Shared response wrappers
```

Each domain is self-contained with controller/service/repository/dto/model layers.

## 🚀 Quick Start

### Prerequisites

- **Java 25**
- **Maven 3.6+**
- **MariaDB 10.6+**
- **Google Gemini API Key** (get from [Google AI Studio](https://makersuite.google.com/app/apikey))

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd tubes-pbo-be
   ```

2. **Configure environment**
   ```bash
   cp .env.example .env
   ```
   
   Edit `.env` with your credentials:
   ```properties
   # Database
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=tubes_pbo_backend
   DB_USERNAME=root
   DB_PASSWORD=yourpassword
   
   # Google Gemini AI
   GOOGLE_AI_API_KEY=your-gemini-api-key-here
   GOOGLE_AI_MODEL=gemini-2.0-flash
   
   # Email (Gmail SMTP)
   MAIL_HOST=smtp.gmail.com
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=your-app-password
   MAIL_FROM=noreply@yourdomain.com
   
   # Application URLs
   APP_BASE_URL=http://localhost:8080
   APP_FRONTEND_URL=http://localhost:3000
   ```

3. **Create database**
   ```sql
   CREATE DATABASE tubes_pbo_backend CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

The API will be available at `http://localhost:8080/api`

## 📚 API Documentation

### Interactive Documentation
- **Swagger UI**: [http://localhost:8080/api/swagger-ui.html](http://localhost:8080/api/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/api/api-docs](http://localhost:8080/api/api-docs)

### API Endpoints

**Base URL**: `/api`

#### Authentication (`/api/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Register new user |
| POST | `/login` | Login with email/password |
| POST | `/logout` | Logout (invalidate token) |
| GET | `/verify` | Verify email with token |
| POST | `/forgot-password` | Request password reset |
| POST | `/reset-password` | Reset password with token |

#### Summarization (`/api/summaries`) 🔒
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Upload PDF & generate summary |
| GET | `/` | List all summaries (paginated) |
| GET | `/{id}` | Get summary details |

#### Quiz (`/api/quizzes`) 🔒
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Generate quiz from summary |
| POST | `/{id}/submit` | Submit quiz answers |
| GET | `/` | List all quizzes (paginated) |
| GET | `/{id}` | Get quiz details |

#### History (`/api/history`) 🔒
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | List document history |
| DELETE | `/summaries/{id}` | Delete summary & file |
| GET | `/summaries/{id}/download` | Download summary as PDF |
| GET | `/summaries/{id}/original` | Download original PDF |

#### Settings (`/api/settings`) 🔒
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/profile` | Get user profile |
| PUT | `/profile` | Update profile |
| PUT | `/password` | Change password |

#### Admin (`/api/admin`) 🔒 👑
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/users` | List all users (paginated) |
| GET | `/users/{id}` | Get user details |
| POST | `/users` | Create new user |
| PUT | `/users/{id}` | Update user |
| DELETE | `/users/{id}` | Delete user |
| GET | `/dashboard/stats` | Dashboard statistics |
| GET | `/dashboard/activity` | Recent activity log |

🔒 = Requires authentication | 👑 = Admin only

### Authentication

Protected endpoints require Bearer token:
```
Authorization: Bearer {token}
```
- Token format: UUID
- Expiry: 24 hours
- Returns 401 if missing/invalid

### Response Formats

**Success (single resource)**:
```json
{
  "message": "Operation successful",
  "data": { /* resource */ }
}
```

**Success (list)**:
```json
{
  "content": [/* items */],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5
}
```

**Error**:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Specific error description"
}
```

## 🧪 Testing

Run all tests:
```bash
./mvnw test
```

Run specific test class:
```bash
./mvnw test -Dtest=AuthControllerTest
```

### Test Structure
```
src/test/java/tubes/pbo/be/
├── auth/controller/         # AuthController tests
├── auth/service/            # AuthService tests
├── summary/controller/      # SummaryController tests
├── quiz/controller/         # QuizController tests
└── ... (other domains)
```

Testing strategy:
- **Unit tests**: Mockito for services, mock external dependencies
- **Integration tests**: `@SpringBootTest` + `MockMvc`
- **Security tests**: `@WithMockUser` for auth
- Mock all external APIs (Google Gemini, email service)

## 🔧 Configuration

### Application Properties
Key configurations in [application.properties](src/main/resources/application.properties):

```properties
# Server
server.port=8080
spring.servlet.multipart.max-file-size=10MB

# Database
spring.datasource.url=jdbc:mariadb://localhost:3306/tubes_pbo_backend
spring.jpa.hibernate.ddl-auto=update

# AI
spring.ai.google.genai.api-key=${GOOGLE_AI_API_KEY}
spring.ai.google.genai.chat.options.model=gemini-2.0-flash
spring.ai.google.genai.chat.options.temperature=0.7

# CORS
app.cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

### File Storage
- Uploaded PDFs: `uploads/pdfs/{userId}/{uuid}.pdf`
- Original filenames stored in database
- Max size: 10MB (configurable via `FILE_MAX_SIZE_MB`)

## 🔐 Security Features

- **Password Hashing**: BCrypt with salt
- **Session Management**: UUID tokens with database persistence
- **Email Verification**: Required for new accounts (24h token expiry)
- **Password Reset**: Secure single-use tokens (1h expiry)
- **CORS Protection**: Configurable allowed origins
- **Role-based Access**: User/Admin roles
- **Resource Ownership**: Users can only access their own data

## 🤖 AI Integration

### Google Gemini
- **Model**: `gemini-2.0-flash` (configurable)
- **Temperature**: 0.7
- **Stored Metadata**: All AI-generated content includes `aiProvider` and `aiModel` fields
- **Error Handling**: AI failures return 500 with descriptive messages

## 📦 Build & Package

### Build JAR
```bash
./mvnw clean package
```
Output: `target/be-0.0.1-SNAPSHOT.jar`

### Run JAR
```bash
java -jar target/be-0.0.1-SNAPSHOT.jar
```

### Skip Tests
```bash
./mvnw clean package -DskipTests
```

## 🐛 Troubleshooting

### Database Connection Errors
```bash
# Check MariaDB service
sudo systemctl status mariadb

# Test connection
mysql -u root -p -e "SHOW DATABASES;"
```

### Google Gemini API Errors
- Verify API key is valid
- Check quota limits at [Google AI Studio](https://makersuite.google.com/)
- Ensure `GOOGLE_AI_API_KEY` is set in `.env`

### Email Not Sending
- For Gmail: Use [App Password](https://support.google.com/accounts/answer/185833)
- Enable "Less secure app access" if using regular password
- Check SMTP settings match your provider

### File Upload Errors
- Ensure `uploads/pdfs` directory exists and is writable
- Check `FILE_MAX_SIZE_MB` in `.env`
- Verify PDF files are valid

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Follow existing code patterns and architecture
4. Write tests for new features
5. Commit changes (`git commit -m 'Add amazing feature'`)
6. Push to branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### Code Style
- Use Lombok annotations (`@RequiredArgsConstructor`, `@Data`, etc.)
- Constructor injection for dependencies
- OpenAPI annotations on controllers (`@Tag`, `@Operation`)
- Follow domain-driven design principles

## 📝 License

This project is part of academic coursework (Tubes PBO).

## 👥 Authors

See [pom.xml](pom.xml) for developer information.

## 🔗 Related Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Google Gemini API](https://ai.google.dev/docs)
- [iText PDF](https://itextpdf.com/en/products/itext-core)
- [Springdoc OpenAPI](https://springdoc.org/)

---

**API Version**: 0.0.1-SNAPSHOT  
**Spring Boot**: 3.5.9  
**Java**: 25
