# Outzdir E-Commerce API

Outzdir is a Spring Boot e-commerce backend application providing robust APIs for user authentication, product catalog management, cart handling with dynamic discount coupon logic, and order placement with pessimistic lock inventory control.

---

## Technology Stack

- **Java 21**
- **Spring Boot 3.x**
- **Spring Security & JWT** (Access Token + Refresh Token flow)
- **Spring Data JPA & Hibernate**
- **MySQL Database**
- **Lombok & ModelMapper**

---

## Getting Started

### Prerequisites

- **Java SDK 21** installed and configured on your path.
- **MySQL Server** running locally or remotely.

### Database Setup

1. Open your MySQL client and create a database named `outzdir`:
   ```sql
   CREATE DATABASE outzdir;
   ```
2. Open `src/main/resources/application.properties` and verify your datasource credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/outzdir
   spring.datasource.username=your_username
   spring.datasource.password=password
   ```
3. The JPA DDL setting is configured to `update`, which will automatically generate and alter database tables when the application starts:
   ```properties
   spring.jpa.hibernate.ddl-auto=update
   ```
4. Seed the coupons into the database:
   ```sql
   INSERT INTO discount (name, code, type, value, min_cart_value, max_discount, start_date, end_date, active)
   VALUES (
       '20% Sitewide Discount',
       'SAVE20',
       'PERCENTAGE',
       20.0,       -- 20% discount
       500.0,      -- Minimum subtotal required
       1000.0,     -- Maximum discount cap
       '2026-08-01 00:00:00',
       '2026-12-31 23:59:59',
       true        -- Active status
   );

   INSERT INTO discount (name, code, type, value, min_cart_value, max_discount, start_date, end_date, active)
   VALUES (
       'Flat 200 Off',
       'WELCOME300',
       'FLAT',
       300.0,      -- Flat 300 discount
       2000.0,     -- Minimum subtotal required
       NULL,
       '2026-08-01 00:00:00',
       '2026-12-31 23:59:59',
       true         -- Active status
   );
   ```

### Running the Application
To start the application, navigate to the project root directory and run the Spring Boot maven wrapper command:

- **Windows (PowerShell):**
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- **macOS / Linux:**
  ```bash
  ./mvnw spring-boot:run
  ```

---

## Authentication Flow

- This API uses **JWT Token-based security**.
- After logging in, you will receive an `accessToken` and `refreshToken`.
- Include the access token in the `Authorization` header for all protected endpoints:
  ```http
  Authorization: Bearer <your_access_token>
  ```

---

## API Documentation

All API endpoints are prefixed with `/api/v1`.

### 1. Authentication Endpoints (`/api/v1/auth`)

####  User Registration

- **Endpoint:** `POST /api/v1/auth/signup`
- **Access:** Public
- **Request Body:**
  ```json
  {
    "name": "Prakash Pradhan",
    "email": "prakash@example.com",
    "phoneNumber": "9876543210",
    "password": "securepassword123"
  }
  ```
- **Response (201 Created):**
  ```json
  {
    "id": 1,
    "name": "Prakash Pradhan",
    "email": "prakash@example.com"
  }
  ```
- **Response (400 Bad Request):** `"User already exist with email: prakash@example.com"`

#### User Login

- **Endpoint:** `POST /api/v1/auth/login`
- **Access:** Public
- **Request Body:**
  ```json
  {
    "email": "prakash@example.com",
    "password": "securepassword123"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "id": 1,
    "email": "prakash@example.com",
    "name": "Prakash Pradhan"
  }
  ```

#### Token Refresh

- **Endpoint:** `POST /api/v1/auth/refresh`
- **Access:** Public
- **Request Body:**
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiIsIn..."
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsIn..."
  }
  ```

#### User Logout

- **Endpoint:** `POST /api/v1/auth/logout`
- **Access:** Public
- **Request Body:**
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiIsIn..."
  }
  ```
- **Response (200 OK):** `"Logged out successfully"`

---

### 2. Product Catalog Endpoints (`/api/v1/products`)

_All endpoints require a valid JWT Access Token._

#### Get All Products

- **Endpoint:** `GET /api/v1/products`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Response (200 OK):**
  ```json
  [
    {
      "category": "Women fashion",
      "id": 1,
      "price": 999.0,
      "productStatus": "INACTIVE",
      "product_description": "Black Top with printed leafs",
      "product_name": "White Pant",
      "quantity": 10,
      "sku": "Top"
    },
    {
      "category": "Women fashion",
      "id": 2,
      "price": 999.0,
      "productStatus": "ACTIVE",
      "product_description": "Black Top with printed leafs",
      "product_name": "Black Pant",
      "quantity": 6,
      "sku": "Top"
    },
    {
      "category": "Women fashion",
      "id": 3,
      "price": 999.0,
      "productStatus": "ACTIVE",
      "product_description": "Black Top with printed leafs",
      "product_name": "Black Top",
      "quantity": 9,
      "sku": "Top"
    },
    {
      "category": "Women fashion",
      "id": 4,
      "price": 899.0,
      "productStatus": "ACTIVE",
      "product_description": "White Top with printed leafs",
      "product_name": "White Top",
      "quantity": 10,
      "sku": "Top"
    },
    {
      "category": "Women fashion",
      "id": 5,
      "price": 899.0,
      "productStatus": "ACTIVE",
      "product_description": "White Top with printed leafs",
      "product_name": "Black saree",
      "quantity": 20,
      "sku": "Saree"
    }
  ]
  ```

#### Get Product by ID

- **Endpoint:** `GET /api/v1/products/{id}`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Response (200 OK):**
  ```json
  {
    "category": "Women fashion",
    "id": 2,
    "price": 999.0,
    "productStatus": "ACTIVE",
    "product_description": "Black Top with printed leafs",
    "product_name": "Black Pant",
    "quantity": 6,
    "sku": "Top"
  }
  ```

---

### 3. Cart Endpoints (`/api/v1/cart`)

_All endpoints require a valid JWT Access Token._

#### View My Cart

- **Endpoint:** `GET /api/v1/cart`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Response (200 OK):**
```json
{
  "appliedDiscount": {
    "active": "ACTIVE",
    "code": "SAVE20",
    "discountItems": [],
    "endDate": "2026-12-31T23:59:59",
    "id": 2,
    "maxDiscount": 1000.0,
    "minCartValue": 500.0,
    "name": "20% Sitewide Discount",
    "startDate": "2026-08-01T00:00:00",
    "type": "PERCENTAGE",
    "value": 20.0
  },
  "cartItems": [
    {
      "createdAt": "2026-08-22T11:01:37.436471",
      "id": 13,
      "product": {
        "category": "Women fashion",
        "id": 3,
        "price": 999.0,
        "productStatus": "ACTIVE",
        "product_description": "Black Top with printed leafs",
        "product_name": "Black Top",
        "quantity": 9,
        "sku": "Top"
      },
      "quantity": 5,
      "unitPrice": 999.0,
      "updatedAt": "2026-08-22T11:01:37.436471"
    }
  ],
  "cartStatus": "ACTIVE",
  "createdAt": "2026-08-21T20:55:31.065033",
  "discountAmount": 999.0,
  "id": 1,
  "subtotal": 4995.0,
  "total": 3996.0,
  "updatedAt": "2026-08-22T11:03:04.638108"
}
```
#### Add Product to Cart

- **Endpoint:** `POST /api/v1/cart/items`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Request Body:**
  ```json
  {
    "productId": 1,
    "quantity": 2
  }
  ```
- **Response (200 OK):** `"Product added to cart successfully"`
- **Response (400 Bad Request):** `"Requested quantity exceeds available stock (30)"`

#### Update Cart Item Quantity

- **Endpoint:** `PUT /api/v1/cart/items/{productId}`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Request Body:**
  ```json
  {
    "quantity": 3
  }
  ```
- **Response (200 OK):** `"Cart item quantity updated successfully"`

#### Remove Product from Cart

- **Endpoint:** `DELETE /api/v1/cart/items`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Request Body:**
  ```json
  {
    "product_id": 1
  }
  ```
- **Response (200 OK):** `"Item removed from cart successfully"`

#### Apply Coupon Discount

- **Endpoint:** `POST /api/v1/cart/coupon`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Request Body:**
  ```json
  {
    "code": "SAVE10"
  }
  ```
- **Response (200 OK):** `"Coupon applied successfully"`
- **Response (400 Bad Request):** `"Coupon has expired"` or `"Minimum cart value of 100.0 required to apply this coupon"`

#### Remove Coupon

- **Endpoint:** `DELETE /api/v1/cart/coupon`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Response (200 OK):** `"Coupon removed successfully"`

---

### 4. Order Endpoints (`/api/v1/orders`)

_All endpoints require a valid JWT Access Token. Operations run under database pessimistic locking to avoid inventory race conditions._

#### Place Order

- **Endpoint:** `POST /api/v1/orders`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Request Body:**
  ```json
  {
    "paymentMethod": "COD",
    "shippingAddress": "whitefield, Bengaluru, 560066"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "id": 1,
    "subtotal": 299.98,
    "discountAmount": 30.00,
    "total": 269.98,
    "orderStatus": "PENDING",
    "paymentStatus": "UNPAID",
    "paymentMethod": "COD",
    "shippingAddress": "Whitefield, Bengaluru, 560066",
    "orderItems": [
      {
        "id": 10,
        "product": {
          "id": 1,
          "product_name": "Premium Wireless Headphones"
        },
        "quantity": 2,
        "unitPrice": 149.99
      }
    ],
    "createdAt": "2026-08-22T10:15:30",
    "updatedAt": "2026-08-22T10:15:30"
  }
  ```

#### Get Order History

- **Endpoint:** `GET /api/v1/orders`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Response (200 OK):**
  ```json
  [
    {
      "id": 1,
      "subtotal": 269.98,
      "discountAmount": 0.0,
      "total": 269.98,
      "orderStatus": "CREATED",
      "paymentStatus": "UNPAID",
      "paymentMethod": "COD",
      "shippingAddress": "Whitefield, Bengaluru, 560066",
      "createdAt": "2026-08-22T10:15:30"
    }
  ]
  ```
