# BeamerParts - Fully Cloud-Based Setup (No Docker Required)

## Overview
This branch uses 100% cloud services - perfect for work environments without Docker admin permissions:
- **Databases**: Neon PostgreSQL (serverless)
- **Redis**: Redis Cloud or Upstash Redis
- **RabbitMQ**: CloudAMQP or AWS SQS

## 🚀 Quick Setup

### Step 1: Create Neon PostgreSQL Databases
1. Go to [Neon Console](https://console.neon.tech/)
2. Create 3 separate databases:
   - `user_db` (for User Service)
   - `vehicle_db` (for Vehicle Service) 
   - `product_db` (for Product Service)
3. Copy the connection strings for each database

### Step 2: Set Up Redis (Choose One)

#### Option A: Redis Cloud (Recommended)
1. Go to [Redis Cloud](https://redis.com/redis-enterprise-cloud/)
2. Create a free account (30MB free tier)
3. Create a new database
4. Copy the endpoint, port, and password

#### Option B: Upstash Redis
1. Go to [Upstash](https://upstash.com/)
2. Create a free account (10K commands/day)
3. Create a new Redis database
4. Copy the connection details

### Step 3: Set Up RabbitMQ (Choose One)

#### Option A: CloudAMQP (Recommended)
1. Go to [CloudAMQP](https://www.cloudamqp.com/)
2. Create a free account (1M messages/month)
3. Create a new RabbitMQ instance
4. Copy the AMQP URL and extract host/credentials

#### Option B: AWS SQS (If you have AWS account)
1. Set up AWS SQS queues
2. Configure AWS credentials
3. Update application to use SQS instead of RabbitMQ

### Step 4: Configure Environment Variables
1. Copy `.env.template` to `.env`
2. Fill in all your cloud service URLs and credentials:

```bash
# Neon PostgreSQL
NEON_USER_DB_URL=postgresql://username:password@ep-cool-lab-123456.us-east-1.aws.neon.tech/user_db?sslmode=require
NEON_VEHICLE_DB_URL=postgresql://username:password@ep-cool-lab-123457.us-east-1.aws.neon.tech/vehicle_db?sslmode=require
NEON_PRODUCT_DB_URL=postgresql://username:password@ep-cool-lab-123458.us-east-1.aws.neon.tech/product_db?sslmode=require

# Redis Cloud
REDIS_HOST=redis-12345.c1.us-east-1-2.ec2.cloud.redislabs.com
REDIS_PORT=12345
REDIS_PASSWORD=your_redis_password

# CloudAMQP
RABBITMQ_HOST=duck.rmq.cloudamqp.com
RABBITMQ_USERNAME=your_username
RABBITMQ_PASSWORD=your_password
```

## 🚀 Quick Start

### Automated Setup (Recommended):
```bash
# 1. Copy environment template and configure
copy .env.template .env
notepad .env  # Configure your cloud service connections

# 2. Run all services
start-neon-cloud.cmd
```

### Manual Setup:
Follow the detailed steps below for more control over the startup process.

### Step 5: Run Services with Cloud Profile
```bash
# Set environment variables from .env file (PowerShell)
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}

# User Service
cd user_service
mvn spring-boot:run -Dspring.profiles.active=neon

# Vehicle Service  
cd vehicle_service
mvn spring-boot:run -Dspring.profiles.active=neon

# Product Service
cd product_service
mvn spring-boot:run -Dspring.profiles.active=neon

# API Gateway
cd api_gateway
mvn spring-boot:run
```

## 🏗️ Architecture Changes

### What's Different:
- ✅ **Databases**: Neon PostgreSQL (serverless cloud)
- ✅ **Redis**: Redis Cloud or Upstash (cloud)
- ✅ **RabbitMQ**: CloudAMQP (cloud)
- ✅ **Services**: Same Spring Boot applications
- ❌ **Docker**: Not required at all!

### Benefits:
- 🚀 **Zero Docker dependency** 
- 🌐 **100% cloud-hosted** - works from any machine
- 📊 **Production-like setup** - same as real deployment
- 💰 **Free tiers available** - all services have generous free plans
- 🔄 **Team collaboration** - everyone uses same cloud services
- 🏢 **Work-friendly** - no admin permissions needed

## 💰 Free Tier Limits

### Neon PostgreSQL:
- ✅ 3 databases per project
- ✅ 512 MB storage per database
- ✅ 1 concurrent connection per database
- ✅ Perfect for development

### Redis Cloud:
- ✅ 30 MB memory
- ✅ 30 connections
- ✅ Perfect for caching

### CloudAMQP:
- ✅ 1M messages per month
- ✅ 20 concurrent connections
- ✅ Perfect for microservices messaging

## 🛠️ Alternative Cloud Services

### More Redis Options:
- **Upstash Redis** (10K commands/day): https://upstash.com/
- **AWS ElastiCache** (if you have AWS): https://aws.amazon.com/elasticache/
- **Azure Redis** (if you have Azure): https://azure.microsoft.com/en-us/services/cache/

### More Message Queue Options:
- **AWS SQS** (1M requests/month): https://aws.amazon.com/sqs/
- **Azure Service Bus** (if you have Azure): https://azure.microsoft.com/en-us/services/service-bus/
- **Google Cloud Pub/Sub** (if you have GCP): https://cloud.google.com/pubsub

## 🔧 Development Workflow

### Daily Development:
1. **Start infrastructure** (once per day):
   ```bash
   docker-compose -f docker-compose-neon.yml up -d
   ```

2. **Run services** (with hot reload):
   ```bash
   # Terminal 1
   cd user_service && mvn spring-boot:run -Dspring.profiles.active=neon
   
   # Terminal 2  
   cd vehicle_service && mvn spring-boot:run -Dspring.profiles.active=neon
   
   # Terminal 3
   cd product_service && mvn spring-boot:run -Dspring.profiles.active=neon
   
   # Terminal 4
   cd api_gateway && mvn spring-boot:run
   ```

3. **Make code changes** - services auto-reload with DevTools

### End of Day:
```bash
docker-compose -f docker-compose-neon.yml down
```

## 🎯 Access Points
- **API Gateway**: http://localhost:8080
- **User Service**: http://localhost:8081  
- **Vehicle Service**: http://localhost:8082
- **Product Service**: http://localhost:8083
- **RabbitMQ Management**: http://localhost:15672
- **Neon Console**: https://console.neon.tech/ (database management)

## 🔄 Switching Between Branches

### Switch to Docker databases:
```bash
git checkout main
docker-compose up -d
# Run services without profile
```

### Switch to Neon databases:
```bash
git checkout neon-serverless-db
docker-compose -f docker-compose-neon.yml up -d
# Run services with -Dspring.profiles.active=neon
```

This setup gives you full development capabilities without needing Docker admin permissions! 🎉
