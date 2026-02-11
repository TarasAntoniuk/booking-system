# Booking System

A comprehensive booking system API built with Spring Boot, featuring real-time availability tracking, automated booking
expiration, payment processing, and Redis caching.

## 🏗️ Tech Stack

- **Backend:** Spring Boot 3.5.8, Java 21
- **Database:** PostgreSQL 17
- **Cache:** Redis 7
- **Build:** Gradle 9.0
- **Testing:** JUnit 5, Mockito, Spring Test, H2 Database, Testcontainers (PostgreSQL 17)

## ✨ Features

### Core Functionality

- User and accommodation unit management
- Booking system with 15-minute payment window
- Payment processing with booking confirmation
- Advanced search with real-time availability checking
- Cached statistics for performance

### Technical Features

- **Automated Expiration**: Scheduled job cancels unpaid bookings after 15 minutes
- **Double Booking Prevention**: Prevents overlapping reservations
- **Smart Caching**: Redis cache with automatic invalidation on data changes
- **Event Logging**: Audit trail for all state changes
- **Dynamic Search**: Specification pattern for flexible queries

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose

### Running the Application

1. **Start infrastructure (PostgreSQL + Redis)**

```bash
docker-compose up -d
```

2. **Run the application**

```bash
./gradlew bootRun
```

The API will be available at http://localhost:8080

### API Documentation

**Swagger UI:** http://localhost:8080/swagger-ui.html

## 🧪 Testing

### Hybrid Testing Strategy

The project uses a **two-tier testing approach** for optimal speed and reliability:

1. **Fast Tests (H2 in-memory)** - Unit and repository tests
2. **Production-like Tests (PostgreSQL Testcontainers)** - End-to-end functional tests

### Run All Tests

```bash
./gradlew test
```

### Generate Coverage Report

```bash
./gradlew jacocoTestReport
```

Report available at: `build/reports/jacoco/test/html/index.html`

### Test Statistics

- **Total Tests:** 132
- **Coverage:** 93% instructions, 95% lines, 76% branches
- **Test Types:** Unit, Integration, Functional E2E
- **Execution Time:** ~15 seconds
- **Database:** H2 (fast tests) + PostgreSQL Testcontainers (functional tests)

### Test Structure

```
Repository Tests (@DataJpaTest)
├─ H2 in-memory database
├─ Fast execution (<1s)
└─ BookingRepository, UnitRepository tests

Unit Tests (@SpringBootTest)
├─ H2 in-memory database  
├─ Mocked dependencies
└─ Service and controller tests

Functional E2E Tests (@SpringBootTest + @Testcontainers)
├─ Real PostgreSQL 17 container
├─ Production-like environment
├─ Complete user flows
└─ BookingSystemFunctionalTest, BookingExpirationSchedulerFunctionalTest
```

### Why Testcontainers?

- **Production Parity:** Tests run against actual PostgreSQL 17, not H2 simulation
- **PostgreSQL-specific Features:** Tests work with real database constraints, sequences, and SQL
- **Confidence:** Critical flows verified in production-like environment
- **Isolation:** Each test gets a clean database state

### Coverage by Component

- Controllers: 100%
- Core Services: 93-100%
- Overall: 93% instructions, 76% branches

## 📡 API Endpoints

### Units

```
POST   /api/units              Create accommodation unit
GET    /api/units/{id}         Get unit details
GET    /api/units              List all units (paginated, sortable)
GET    /api/units/search       Search with availability filter
```

### Bookings

```
POST   /api/bookings           Create booking (expires in 15 min)
GET    /api/bookings/{id}      Get booking details
GET    /api/bookings/user/{id} Get user's bookings
DELETE /api/bookings/{id}/cancel  Cancel booking
```

### Payments

```
POST   /api/payments/process   Process payment (confirms booking)
```

### Users

```
POST   /api/users              Create user
GET    /api/users/{id}         Get user details
```

### Statistics

```
GET    /api/statistics/available-units  Get cached available units count
```

## 🔧 Configuration

### Database

```yaml
spring.datasource.url=jdbc:postgresql://localhost:5432/booking_db
spring.datasource.username=booking_user
spring.datasource.password=booking_pass
```

### Redis Cache

```yaml
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### Booking Expiration

```yaml
booking.expiration.check-interval=60000  # Check every minute
booking.expiration.minutes=15            # Expire after 15 minutes
```

### Scheduling

```yaml
app.scheduling.enabled=true  # Enable in production
# Automatically disabled in tests
```

## 📊 Architecture

### Layers

```
Controllers  → Handle HTTP requests/responses
Services     → Business logic and orchestration  
Repositories → Data access with Spring Data JPA
Entities     → Domain models
DTOs         → API contracts
```

### Design Patterns

- **Layered Architecture** - Clear separation of concerns
- **Repository Pattern** - Data access abstraction
- **Specification Pattern** - Dynamic query building
- **Cache-Aside Pattern** - Performance optimization
- **Event Sourcing** - Audit trail

### Booking Lifecycle

```
1. Create Booking → Status: PENDING (15 min timer starts)
2. Process Payment → Status: CONFIRMED (timer cleared)
3. Or Timeout     → Status: CANCELLED (by scheduler)
```

### Cache Strategy

- **Invalidation Triggers:** Unit created, booking created/cancelled, payment processed
- **Recalculation:** Lazy (on next request after invalidation)
- **Performance:** 1-5ms (cache hit) vs 50-200ms (cache miss)

## 📈 Business Logic

### Cost Calculation

```
totalCost = baseCost × numberOfDays × 1.15
```

*15% markup automatically applied to all bookings*

### Availability Filter

Search excludes units with:

- PENDING bookings (reserved but not paid)
- CONFIRMED bookings (paid and active)

CANCELLED bookings do not block availability.

## 🗄️ Database Schema

### Core Tables

- `users` - User accounts
- `units` - Accommodation units
- `bookings` - Reservations with status and expiration
- `payments` - Payment transactions
- `events` - Audit log of all operations

## 💻 Development

### Build Project

```bash
./gradlew clean build
```

### Run Tests Only

```bash
./gradlew test --tests "*Test"
```

### Run Functional Tests

```bash
./gradlew test --tests "*FunctionalTest"
```

### Check Code Coverage

```bash
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### Database Access

```bash
# Connect to PostgreSQL
docker exec -it booking-db psql -U booking_user -d booking_db

# Useful queries
SELECT * FROM bookings WHERE status = 'PENDING';
SELECT COUNT(*) FROM units;
```

### Redis Access

```bash
# Connect to Redis
docker exec -it booking-redis redis-cli

# Check cache
GET stats:available_units_count
KEYS *
```

## 📝 Example Usage

### Create Complete Booking Flow

```bash
# 1. Create user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"john.doe","email":"john@example.com"}'

# 2. Search available units
curl "http://localhost:8080/api/units/search?numberOfRooms=2&startDate=2026-02-01&endDate=2026-02-03"

# 3. Create booking
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"unitId":1,"userId":1,"startDate":"2026-02-01","endDate":"2026-02-03"}'

# 4. Process payment
curl -X POST http://localhost:8080/api/payments/process \
  -H "Content-Type: application/json" \
  -d '{"bookingId":1}'
```

## 🔍 Quality Assurance

### Test Coverage Report

Run `./gradlew jacocoTestReport` to generate detailed coverage metrics:

- Instruction coverage: 93%
- Branch coverage: 76%
- Line coverage: 95%
- Method coverage: 89%
- Class coverage: 81%

### Code Quality

- Clean architecture with clear separation of concerns
- Comprehensive error handling
- Proper validation on all inputs
- Professional logging
- Self-documenting code with meaningful names

## 📦 Project Structure

```
src/
├── main/
│   ├── java/com/tarasantoniuk/
│   │   ├── booking/         # Booking management
│   │   ├── payment/         # Payment processing
│   │   ├── unit/            # Unit management
│   │   ├── user/            # User management
│   │   ├── statistic/       # Statistics & caching
│   │   ├── event/           # Event sourcing
│   │   ├── common/          # Shared utilities
│   │   └── initialization/  # Data seeding
│   └── resources/
│       ├── application.yml
│       └── db/changelog/    # Liquibase migrations
└── test/
    ├── java/                # 132 tests (93% coverage)
    └── resources/
        ├── application.yml    
        ├── application-test.yml
        └── application-integration.yml
```

## 🚀 Deployment

### Build for Production

```bash
./gradlew clean build -x test
```

### Docker Deployment

```bash
docker-compose -f docker-compose.yml up -d
```

## 📄 License

This project was created as a technical assessment.

---

**Built with Spring Boot 3.5.8 | 132 Tests | 93% Line Coverage | Testcontainers**