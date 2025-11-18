# JSpec Spring Boot Demo

This is a complete Spring Boot application demonstrating how to integrate the jspec library for JSON/YAML document evaluation.

## Project Structure

```
spring/
├── pom.xml                                    # Maven configuration
├── src/main/java/com/example/jspecdemo/
│   ├── JSpecDemoApplication.java              # Main application
│   ├── config/
│   │   └── JSpecConfig.java                   # JSpec bean configuration
│   ├── service/
│   │   └── EligibilityService.java            # Business logic with caching
│   └── controller/
│       └── ValidationController.java          # REST API endpoints
└── src/main/resources/
    ├── application.yaml                       # Application configuration
    └── specifications/
        └── user-eligibility.yaml              # Sample specification file
```

## Prerequisites

- Java 21
- Maven 3.8+
- jspec library (see parent project)

## Running the Application

```bash
# From this directory
mvn spring-boot:run

# Or build and run
mvn clean package
java -jar target/jspec-spring-demo-1.0.0-SNAPSHOT.jar
```

## API Endpoints

### POST /api/loan/evaluate

Full evaluation results in JSON format.

```bash
curl -X POST http://localhost:8080/api/loan/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "age": 35,
    "annualIncome": 75000,
    "employmentStatus": "employed",
    "creditScore": 720,
    "hasDefaultHistory": false
  }'
```

### POST /api/loan/eligible

Simple eligibility check.

```bash
curl -X POST http://localhost:8080/api/loan/eligible \
  -H "Content-Type: application/json" \
  -d '{
    "age": 35,
    "annualIncome": 75000,
    "employmentStatus": "employed",
    "creditScore": 720,
    "hasDefaultHistory": false
  }'
```

Response:
```json
{
  "eligible": true
}
```

### POST /api/loan/summary

Evaluation summary statistics.

```bash
curl -X POST http://localhost:8080/api/loan/summary \
  -H "Content-Type: application/json" \
  -d '{
    "age": 17,
    "annualIncome": 25000,
    "employmentStatus": "student",
    "creditScore": 600,
    "hasDefaultHistory": true
  }'
```

Response:
```json
{
  "matched": 0,
  "notMatched": 6,
  "undetermined": 0,
  "total": 6
}
```

### POST /api/loan/failures

List of failed criteria.

```bash
curl -X POST http://localhost:8080/api/loan/failures \
  -H "Content-Type: application/json" \
  -d '{
    "age": 17,
    "annualIncome": 75000,
    "employmentStatus": "employed",
    "creditScore": 720,
    "hasDefaultHistory": false
  }'
```

Response:
```json
{
  "failedCriteria": ["age-check", "loan-eligibility"]
}
```

### GET /api/loan/health

Health check endpoint.

```bash
curl http://localhost:8080/api/loan/health
```

## Key Features Demonstrated

### 1. Configuration (`JSpecConfig.java`)

- YAML ObjectMapper for parsing specification files
- JSON result formatter for API responses
- Loading specifications from classpath resources
- Building specifications programmatically

### 2. Service Layer (`EligibilityService.java`)

- Evaluator caching with `ConcurrentHashMap` for performance
- Thread-safe for concurrent requests
- Convenient methods for common operations:
  - `isEligibleForLoan()` - Simple boolean check
  - `getFailedCriteria()` - List of failed criteria IDs
  - `getSummary()` - Evaluation statistics

### 3. REST Controller (`ValidationController.java`)

- Multiple endpoints for different use cases
- JSON response formatting
- Proper HTTP content types

## Customization

### Adding New Specifications

1. Create a YAML file in `src/main/resources/specifications/`
2. Add a bean in `JSpecConfig.java` to load it
3. Inject and use in your service

### Creating Criteria Programmatically

```java
QueryCriterion criterion = QueryCriterion.builder()
    .id("custom-check")
    .field("myField").gte(100)
    .build();
```

## License

This example is part of the jspec project and is available under the MIT License.
