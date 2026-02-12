# Coupon Service

Promotional Campaign and Discount Management for Ski Shop E-commerce Platform

## Main Features Provided

### Feature List

- **Coupon Management**: Creation, validation, redemption, and lifecycle management
- **Campaign Management**: Promotional campaign creation, scheduling, and monitoring
- **Distribution Engine**: Automated and manual coupon distribution to target user segments
- **Rule Engine**: Complex business rule evaluation for coupon eligibility
- **Usage Tracking**: Real-time usage analytics and fraud detection
- **Analytics & Reporting**: Campaign performance analysis and optimization insights
- **Bulk Generation**: Efficient generation of multiple coupons with unique codes
- **Multi-format Support**: Percentage discounts, fixed amounts, free shipping, and BOGO offers

## Service Endpoints

| HTTP Method | Endpoint | Description | Authorization |
|-------------|----------|-------------|---------------|
| POST | `/api/v1/coupons` | Create coupon | Admin |
| GET | `/api/v1/coupons` | Get coupon list by campaign | Admin |
| GET | `/api/v1/coupons/{code}` | Get coupon by code | Public |
| GET | `/api/v1/coupons/by-id/{id}` | Get coupon by UUID | Public |
| POST | `/api/v1/coupons/validate` | Validate coupon | Authenticated |
| POST | `/api/v1/coupons/redeem` | Redeem coupon | Authenticated |
| GET | `/api/v1/coupons/usage/{couponId}` | Get coupon usage | Admin |
| POST | `/api/v1/coupons/bulk-generate` | Bulk coupon generation | Admin |
| GET | `/api/v1/coupons/user/available` | Get user available coupons | Authenticated |
| POST | `/api/v1/campaigns` | Create campaign | Admin |
| GET | `/api/v1/campaigns` | Get campaign list | Admin |
| GET | `/api/v1/campaigns/{campaignId}` | Get campaign details | Admin |
| PUT | `/api/v1/campaigns/{campaignId}` | Update campaign | Admin |
| POST | `/api/v1/campaigns/{campaignId}/activate` | Activate campaign | Admin |
| GET | `/api/v1/campaigns/{campaignId}/analytics` | Campaign analytics | Admin |
| GET | `/api/v1/campaigns/active` | Get active campaigns | Admin |
| GET | `/api/v1/distributions/rules/{campaignId}` | Get distribution rules | Admin |
| POST | `/api/v1/distributions/rules/{campaignId}` | Create distribution rule | Admin |
| PUT | `/api/v1/distributions/rules/{ruleId}` | Update distribution rule | Admin |
| DELETE | `/api/v1/distributions/rules/{ruleId}` | Delete distribution rule | Admin |
| POST | `/api/v1/distributions/execute/{campaignId}` | Execute coupon distribution | Admin |
| GET | `/api/v1/distributions/history/{campaignId}` | Get distribution history | Admin |

## Technology Stack

- **Java**: 21 LTS
- **Spring Boot**: 3.5.3
- **Spring Data JPA**: Database access
- **Spring Data Redis**: Caching and rate limiting
- **Spring Cloud Stream**: Event handling with Kafka
- **Quartz Scheduler**: Job scheduling
- **Database**: PostgreSQL 16+
- **Cache**: Redis 7.2+
- **Event Streaming**: Apache Kafka
- **Flyway**: Database migrations
- **MapStruct**: Object mapping
- **Containerization**: Docker
- **Cloud Platform**: Azure Container Apps

## Environment Variables

| Variable Name | Description | Default Value |
|---------------|-------------|---------------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `local` |
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/coupon_db` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `password` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS` | Kafka broker addresses | `localhost:9092` |
| `COUPON_CODE_LENGTH` | Length of generated coupon codes | `10` |
| `COUPON_CODE_PREFIX` | Prefix for generated coupon codes | `SKI` |
| `COUPON_GENERATION_BATCH_SIZE` | Batch size for bulk generation | `1000` |
| `RATE_LIMIT_VALIDATION` | Rate limit for validation requests | `60` |
| `RATE_LIMIT_REDEMPTION` | Rate limit for redemption requests | `10` |
| `AZURE_KEYVAULT_URI` | Azure Key Vault URI | - |
| `AZURE_CLIENT_ID` | Azure client ID | - |
| `AZURE_CLIENT_SECRET` | Azure client secret | - |
| `AZURE_TENANT_ID` | Azure tenant ID | - |
| `JWT_ISSUER_URI` | JWT issuer URI for authentication | `http://localhost:8080/realms/skishop` |

### Profiles

- **local**: Local development environment with H2 database
- **docker**: Docker development environment with containerized services
- **dev**: Development environment (shared development resources)
- **test**: Testing environment (test databases and mocked external services)
- **production**: Production environment (Azure services)

Profile settings are configured in `application.yml` and `application-{profile}.yml` files.

## Local Development Environment Setup and Verification

### Local Compilation and Execution

1. **Install Dependencies**

```bash
cd coupon-service
mvn clean install
```

1. **Configure Environment Variables**

Create a `.env` file or set environment variables:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/coupon_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="password"
export SPRING_DATA_REDIS_HOST="localhost"
export SPRING_DATA_REDIS_PORT="6379"
```

1. **Start Required Services**

Start PostgreSQL, Redis, and Kafka:

```bash
# PostgreSQL
docker run -d --name coupon-postgres -p 5432:5432 \
  -e POSTGRES_DB=coupon_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  postgres:15-alpine

# Redis
docker run -d --name coupon-redis -p 6379:6379 redis:7-alpine

# Kafka (with Zookeeper)
docker run -d --name coupon-zookeeper -p 2181:2181 confluentinc/cp-zookeeper:latest
docker run -d --name coupon-kafka -p 9092:9092 \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  --link coupon-zookeeper:zookeeper \
  confluentinc/cp-kafka:latest
```

1. **Start Application**

```bash
mvn spring-boot:run
```

1. **Access via Browser**

- **Swagger UI**: [http://localhost:8088/swagger-ui.html](http://localhost:8088/swagger-ui.html)
- **API Documentation**: [http://localhost:8088/v3/api-docs](http://localhost:8088/v3/api-docs)
- **Health Check**: [http://localhost:8088/actuator/health](http://localhost:8088/actuator/health)

### Run Microservices with Docker Compose

1. **Build and Start Containers**

```bash
docker-compose up --build
```

1. **Access via Browser**

- **Coupon Service**: [http://localhost:8088](http://localhost:8088)
- **Swagger UI**: [http://localhost:8088/swagger-ui.html](http://localhost:8088/swagger-ui.html)

## Production Environment Setup and Verification

Primary deployment target: Azure Container Apps
Related external resources: Azure Database for PostgreSQL, Azure Cache for Redis, Azure Service Bus, Azure Key Vault

### 1. Setup Related External Resources

**Azure Database for PostgreSQL**:

```bash
# Create Azure PostgreSQL
az postgres flexible-server create \
  --name "ski-shop-coupon-db" \
  --resource-group "rg-ski-shop" \
  --location "East US" \
  --admin-user "postgres" \
  --admin-password "YourSecurePassword123!" \
  --sku-name "Standard_D2s_v3" \
  --tier "GeneralPurpose" \
  --storage-size 128 \
  --version "16"

# Configure firewall
az postgres flexible-server firewall-rule create \
  --name "AllowAllAzureServices" \
  --resource-group "rg-ski-shop" \
  --server-name "ski-shop-coupon-db" \
  --start-ip-address "0.0.0.0" \
  --end-ip-address "0.0.0.0"
```

**Azure Cache for Redis**:

```bash
az redis create \
  --name "ski-shop-coupon-redis" \
  --resource-group "rg-ski-shop" \
  --location "East US" \
  --sku "Standard" \
  --vm-size "C1"
```

**Azure Service Bus**:

```bash
# Create Service Bus namespace
az servicebus namespace create \
  --name "ski-shop-servicebus" \
  --resource-group "rg-ski-shop" \
  --location "East US" \
  --sku "Standard"

# Create topics
az servicebus topic create \
  --name "coupon.lifecycle" \
  --namespace-name "ski-shop-servicebus" \
  --resource-group "rg-ski-shop"

az servicebus topic create \
  --name "coupon.redemption" \
  --namespace-name "ski-shop-servicebus" \
  --resource-group "rg-ski-shop"

az servicebus topic create \
  --name "campaign.lifecycle" \
  --namespace-name "ski-shop-servicebus" \
  --resource-group "rg-ski-shop"
```

### 2. Azure Container Apps Environment Setup and Deployment

```bash
# Set environment variables
export AZURE_SUBSCRIPTION_ID="your-subscription-id"
export AZURE_LOCATION="eastus"
export AZURE_ENV_NAME="coupon-prod"

# Initialize Azure Developer CLI
azd init --template coupon-service

# Set secrets
azd env set DB_PASSWORD "YourSecurePassword123!" --secret
azd env set AZURE_CLIENT_SECRET "your-azure-client-secret" --secret
azd env set REDIS_PASSWORD "your-redis-password" --secret

# Deploy infrastructure and application
azd up
```

### 3. Access Azure Container Apps Instance via Browser

After deployment, access the service at the provided Azure Container Apps URL:

- **Service URL**: [https://coupon-prod.internal.azurecontainerapps.io](https://coupon-prod.internal.azurecontainerapps.io)
- **Health Check**: [https://coupon-prod.internal.azurecontainerapps.io/actuator/health](https://coupon-prod.internal.azurecontainerapps.io/actuator/health)

### 4. Azure CLI Environment Setup Script

```bash
#!/bin/bash
# setup-azure-environment.sh

# Variables
RESOURCE_GROUP="rg-ski-shop-coupon"
LOCATION="eastus"
APP_NAME="coupon-service"
CONTAINER_REGISTRY="acrskishop"
POSTGRES_SERVER_NAME="ski-shop-coupon-db"
REDIS_CACHE_NAME="ski-shop-coupon-redis"
SERVICEBUS_NAMESPACE="ski-shop-servicebus"

# Create resource group
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create Container Registry
az acr create \
  --name $CONTAINER_REGISTRY \
  --resource-group $RESOURCE_GROUP \
  --sku "Standard" \
  --location $LOCATION

# Create Container Apps Environment
az containerapp env create \
  --name "env-ski-shop" \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION

# Deploy application
az containerapp create \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --environment "env-ski-shop" \
  --image "$CONTAINER_REGISTRY.azurecr.io/coupon-service:latest" \
  --target-port 8088 \
  --ingress 'external' \
  --env-vars \
    SPRING_PROFILES_ACTIVE="production" \
    SPRING_DATASOURCE_URL="jdbc:postgresql://$POSTGRES_SERVER_NAME.postgres.database.azure.com:5432/coupon_db" \
    DB_USERNAME="postgres" \
    SPRING_DATA_REDIS_HOST="$REDIS_CACHE_NAME.redis.cache.windows.net" \
    SPRING_DATA_REDIS_PORT="6380" \
    SPRING_DATA_REDIS_SSL="true" \
    SPRING_CLOUD_STREAM_SERVICEBUS_BINDER_NAMESPACE="$SERVICEBUS_NAMESPACE.servicebus.windows.net"

echo "Deployment completed. Check the Azure portal for the application URL."
```

## Service Feature Verification Methods

### curl Commands for Testing and Expected Results

### 1. Health Check

```bash
curl -X GET http://localhost:8088/actuator/health

# Expected Response:
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "kafka": {"status": "UP"}
  }
}
```

### 2. Create Campaign

```bash
curl -X POST http://localhost:8088/api/v1/campaigns \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token" \
  -d '{
    "name": "Winter Sale 2024",
    "description": "Winter seasonal promotion with percentage discounts",
    "campaignType": "PERCENTAGE",
    "startDate": "2025-01-01T00:00:00Z",
    "endDate": "2025-03-31T23:59:59Z",
    "maxCoupons": 1000,
    "rules": {
      "userSegment": "all",
      "minOrderAmount": 5000,
      "productCategories": ["ski", "snowboard"],
      "maxUsagePerUser": 1
    }
  }'

# Expected Response:
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Winter Sale 2024",
  "description": "Winter seasonal promotion with percentage discounts",
  "campaignType": "PERCENTAGE",
  "startDate": "2025-01-01T00:00:00Z",
  "endDate": "2025-03-31T23:59:59Z",
  "status": "DRAFT",
  "maxCoupons": 1000,
  "generatedCoupons": 0,
  "rules": {
    "userSegment": "all",
    "minOrderAmount": 5000,
    "productCategories": ["ski", "snowboard"],
    "maxUsagePerUser": 1
  },
  "createdAt": "2025-07-04T10:00:00Z",
  "updatedAt": "2025-07-04T10:00:00Z"
}
```

### 3. Create Coupon

```bash
curl -X POST http://localhost:8088/api/v1/coupons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token" \
  -d '{
    "campaignId": "550e8400-e29b-41d4-a716-446655440000",
    "code": "WINTER20",
    "couponType": "PERCENTAGE",
    "discountType": "PERCENTAGE",
    "discountValue": 20.00,
    "minimumAmount": 5000.00,
    "maximumDiscount": 10000.00,
    "usageLimit": 1,
    "expiresAt": "2025-03-31T23:59:59Z"
  }'

# Expected Response:
{
  "id": "660f9500-f38c-52e5-b827-557766550000",
  "campaignId": "550e8400-e29b-41d4-a716-446655440000",
  "code": "WINTER20",
  "couponType": "PERCENTAGE",
  "discountType": "PERCENTAGE",
  "discountValue": 20.00,
  "minimumAmount": 5000.00,
  "maximumDiscount": 10000.00,
  "usageLimit": 1,
  "usedCount": 0,
  "isActive": true,
  "expiresAt": "2025-03-31T23:59:59Z",
  "createdAt": "2025-07-04T10:10:00Z",
  "updatedAt": "2025-07-04T10:10:00Z"
}
```

### Example: Get coupon by ID

```bash
curl -X GET http://localhost:8088/api/v1/coupons/by-id/550e8400-e29b-41d4-a716-446655440000

# Expected Response:
#{
#  "success": true,
#  "data": { /* CouponDto.CouponResponse の JSON */ }
#}
```

### 4. Validate Coupon

```bash
curl -X POST http://localhost:8088/api/v1/coupons/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token" \
  -d '{
    "code": "WINTER20",
    "cartAmount": 15000.00,
    "userId": "user-123",
    "productIds": ["prod-1", "prod-2"],
    "metadata": {
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0..."
    }
  }'

# Expected Response:
{
  "isValid": true,
  "discountAmount": 3000.00,
  "finalAmount": 12000.00,
  "couponDetails": {
    "id": "660f9500-f38c-52e5-b827-557766550000",
    "code": "WINTER20",
    "discountType": "PERCENTAGE",
    "discountValue": 20.00,
    "expiresAt": "2025-03-31T23:59:59Z"
  },
  "validationResult": {
    "rulesMatched": ["userSegment", "productCategory", "minimumAmount"],
    "rulesFailed": [],
    "message": "Coupon is valid and applicable"
  }
}
```

## Integration with Other Microservices

This service integrates with the following microservices:

- **Authentication Service**: User authentication and authorization
- **User Management Service**: User profiles for targeting and distribution
- **Sales Management Service**: Order processing and redemption confirmation
- **Payment Cart Service**: Payment confirmation for finalizing redemption
- **Inventory Management Service**: Product information for category-based rules
- **Point Service**: Point balance integration for combination offers

## Testing

### Unit Test Execution

```bash
# Run unit tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Integration Test Execution

```bash
# Run integration tests
mvn test -Dtest="*IT"

# Run all tests including integration tests
mvn verify

# Run tests with Testcontainers
mvn test -Dspring.profiles.active=test
```

## Monitoring & Logging

### Health Check

```url
GET /actuator/health
```

Response includes PostgreSQL, Redis, and Kafka connectivity status.

### Metrics

```url
GET /actuator/metrics
GET /actuator/prometheus
```

Available metrics:

- Coupon validation count and success rate
- Redemption count and success rate
- Campaign performance metrics
- Rule engine evaluation time
- Cache hit ratio
- Database operation latency
- Event processing latency

### Log Levels

- **Development environment**: DEBUG
- **Production environment**: INFO

Key loggers:

- `com.skishop.coupon`: Coupon service operations
- `com.skishop.coupon.rule`: Rule engine operations
- `com.skishop.coupon.events`: Event processing operations
- `org.springframework.data`: Database and cache operations
- `org.springframework.kafka`: Kafka operations

## Troubleshooting

### Common Issues

#### 1. Database Connection Failure

```text
ERROR: Unable to connect to database
Solution: Check SPRING_DATASOURCE_URL, DB_USERNAME, and DB_PASSWORD environment variables
```

#### 2. Redis Connection Issues

```text
ERROR: Unable to connect to Redis
Solution: Verify Redis is running and SPRING_DATA_REDIS_HOST/SPRING_DATA_REDIS_PORT are correct
```

#### 3. Kafka Connection Failure

```text
ERROR: Unable to connect to Kafka
Solution: Check SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS environment variable
```

#### 4. Coupon Validation Failure

```text
ERROR: Coupon validation failed
Solution: Check coupon expiration date, usage limits, and rule criteria
```

#### 5. Rate Limit Exceeded

```text
ERROR: Too many requests
Solution: Implement exponential backoff in client applications
```

## Developer Information

### Directory Structure

```text
coupon-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/skishop/coupon/
│   │   │       ├── controller/          # REST API controllers
│   │   │       │   ├── CouponController.java
│   │   │       │   ├── CampaignController.java
│   │   │       │   └── DistributionController.java
│   │   │       ├── service/             # Business logic services
│   │   │       │   ├── CouponService.java
│   │   │       │   ├── CampaignService.java
│   │   │       │   └── DistributionService.java
│   │   │       ├── rule/                # Rule engine components
│   │   │       │   ├── RuleEngine.java
│   │   │       │   └── rules/
│   │   │       ├── dto/                 # Data transfer objects
│   │   │       ├── entity/              # JPA entities
│   │   │       ├── repository/          # Database repositories
│   │   │       ├── event/               # Event publishers and listeners
│   │   │       ├── config/              # Configuration classes
│   │   │       └── CouponServiceApplication.java
│   │   └── resources/
│   │       ├── application.yml          # Main configuration
│   │       ├── application-local.yml    # Local environment config
│   │       ├── application-docker.yml   # Docker environment config
│   │       └── db/migration/            # Flyway migration scripts
│   └── test/
│       ├── java/                        # Unit and integration tests
│       └── resources/
│           └── application-test.yml     # Test configuration
├── docker-compose.yml                   # Local development setup
├── Dockerfile                           # Container image definition
├── pom.xml                              # Maven configuration
└── README.md                            # This file
```

## Change History

- **v1.0.0** (2025-07-04): Initial release
  - Complete coupon and campaign management
  - Rule-based validation engine
  - Distribution system for targeted marketing
  - Integration with other microservices
  - Analytics and reporting capabilities
  - Docker containerization and Azure deployment support
