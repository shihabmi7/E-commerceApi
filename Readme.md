# E‑Commerce API

A **robust**, **scalable** Spring Boot backend for an online store, designed with modular layers, JWT security, and clean architecture.

---

## 🚀 Current Status

✓ **Java 17 & Spring Boot 3.2.x** core setup

✓ **JPA/Hibernate** integration with MySQL

✓ **Lombok** for boilerplate reduction

✓ **Security**
- JWT‑based auth: `/auth/register`, `/auth/login`
- Stateless sessions, custom `JwtFilter`, protected `/api/**`

✓ **API Layers**
- **Entities**, **DTOs**, **Mappers**
- **Repositories**, **Services**, **Controllers**

✓ **Cart**
- `POST /api/carts/add`: idempotent add/update quantity
- `GET /api/cart?userId={id}`: returns `ProductInCartDto` (product details + quantity)

✓ **Orders**
- `POST /api/orders/place`: single endpoint builds `Order` + `OrderItem`s from cart & clears cart
- Supports linking user address

✓ **Address Management**
- `POST /api/addresses`: create addresses for users
- Orders now reference `address_id` for delivery

✓ **Swagger/OpenAPI**
- Live docs: `http://localhost:9090/swagger-ui/index.html`

---

## ⚙️ Prerequisites

- Java 17+
- Maven 3.6+
- MySQL (running & accessible)

---

## 🔧 Setup Guide

1. **Clone & configure**
   ```bash
   git clone https://github.com/shihabmi7/E-commerceAPI.git
   cd E-commerceAPI
   ```
2. **`application.properties`**
   ```properties
   server.port=9090
   spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db
   spring.datasource.username=YOUR_DB_USER
   spring.datasource.password=YOUR_DB_PASS

   security.jwt.secret=YOUR_BASE64_SECRET
   security.jwt.expiration-ms=3600000

   springdoc.api-docs.path=/v3/api-docs
   springdoc.swagger-ui.path=/swagger-ui/index.html
   ```
3. **Initialize schema**
   ```bash
   mysql -u root -p ecommerce_db < src/main/resources/schema.sql
   ```
4. **Build & run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
5. **Explore**
    - Swagger UI: `http://localhost:9090/swagger-ui/index.html`
    - Test via Postman or cURL

---

## 📖 API Overview

### Authentication (no token)
| Endpoint            | Method | Description                |
|---------------------|--------|----------------------------|
| `/auth/register`    | POST   | Create new user            |
| `/auth/login`       | POST   | Authenticate & return JWT  |

### Cart
| Endpoint                       | Method | Description                                |
|--------------------------------|--------|--------------------------------------------|
| `/api/carts/add`               | POST   | Add/update product in user’s cart          |
| `/api/cart?userId={userId}`    | GET    | List products in cart with aggregated qty  |

### Orders
| Endpoint                       | Method | Description                                              |
|--------------------------------|--------|----------------------------------------------------------|
| `/api/orders/place`            | POST   | Convert user’s cart into order + items, then clear cart  |

### Addresses
| Endpoint                 | Method | Description                 |
|--------------------------|--------|-----------------------------|
| `/api/addresses`         | POST   | Add a new address for user  |

_(And other CRUD endpoints under `/api/` for users, products, categories, etc.)_

---

## 🛠 Next Steps

- **RabbitMQ** integration for async order/event messaging
- **Firebase FCM** for real‑time push notifications (order status updates)
- Unit & integration tests (JUnit + Mockito)
- Dockerfile & Docker Compose
- CI/CD pipeline (GitHub Actions → AWS)

---

## 👍 Contributing

1. Fork repo
2. Create feature branch
3. Open Pull Request

---

**MIT License © 2025**

