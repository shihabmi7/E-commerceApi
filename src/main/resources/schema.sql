-- Step 1: Users Table (parent)
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role ENUM('admin', 'customer') DEFAULT 'customer',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Step 2: Addresses Table (depends on users)
CREATE TABLE addresses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    street VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Step 3: Categories Table
CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Step 4: Products Table (depends on categories)
CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock INT DEFAULT 0,
    category_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Step 5: Product Images Table (depends on products)
CREATE TABLE product_images (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Step 6: Orders Table (depends on users)
CREATE TABLE orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    status ENUM('pending', 'shipped', 'delivered', 'cancelled') DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Step 7: Order Items Table (depends on orders, products)
CREATE TABLE order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Step 8: Payments Table (depends on orders)
CREATE TABLE payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    payment_method VARCHAR(50),
    payment_status ENUM('pending', 'completed', 'failed') DEFAULT 'pending',
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- Step 9: Shipping Table (depends on orders, addresses)
CREATE TABLE shipping (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    address_id INT NOT NULL,
    tracking_number VARCHAR(100),
    shipped_date TIMESTAMP,
    delivery_date TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (address_id) REFERENCES addresses(id)
);

-- Step 10: Cart Table (depends on users, products)
CREATE TABLE cart (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Step 11: Reviews Table (depends on users, products)
CREATE TABLE reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Step 12: Wishlists Table (depends on users, products)
CREATE TABLE wishlists (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Step 13: Discounts Table (depends on products)
CREATE TABLE discounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    discount_percentage DECIMAL(5,2) NOT NULL,
    start_date DATE,
    end_date DATE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);


-- 1) Categories
INSERT INTO categories (name, description) VALUES
  ('Electronics', 'Gadgets, devices and accessories'),
  ('Books',       'Printed and e-books across genres'),
  ('Clothing',    'Apparel for men and women');

-- 2) Products
INSERT INTO products (name, description, price, stock, category_id) VALUES
  ('Wireless Mouse',    'Ergonomic 2.4GHz wireless mouse',  19.99, 150, 1),
  ('USB-C Charger',     'PD fast charger 30W',              25.00, 300, 1),
  ('Spring Boot in Action', 'Comprehensive guide to Spring Boot', 39.50,  75, 2),
  ('Men''s T-Shirt',    '100% cotton, crew neck',           15.00, 200, 3);

-- 3) Product Images
INSERT INTO product_images (product_id, image_url) VALUES
  (1, 'https://example.com/images/mouse1.jpg'),
  (1, 'https://example.com/images/mouse2.jpg'),
  (2, 'https://example.com/images/charger.jpg'),
  (3, 'https://example.com/images/springboot.jpg'),
  (4, 'https://example.com/images/tshirt.jpg');

-- 4) (Optional) Discounts
INSERT INTO discounts (product_id, discount_percentage, start_date, end_date) VALUES
  (1,    10.00, '2025-04-01', '2025-04-30'),
  (3, 5.00,  '2025-04-15', '2025-05-15');

-- 5) Verify with sample queries
SELECT p.id, p.name, p.price, c.name AS category
  FROM products p
  JOIN categories c ON p.category_id = c.id;

SELECT p.name, pi.image_url
  FROM products p
  JOIN product_images pi ON p.id = pi.product_id
  WHERE p.id = 1;
