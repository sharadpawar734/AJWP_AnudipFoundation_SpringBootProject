-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS a;

-- Use the database
USE a;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    profile_photo VARCHAR(500)
);

-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    image_url VARCHAR(500)
);

-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create order_items table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Create carts table
CREATE TABLE IF NOT EXISTS carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT DEFAULT 1,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Create wishlists table
CREATE TABLE IF NOT EXISTS wishlists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Insert sample data (optional)
INSERT INTO users (username, email, password, phone, address, city, state, pincode, profile_photo) VALUES
('admin', 'admin@example.com', 'password123', '9876543210', 'Admin Office', 'Delhi', 'Delhi', '110001', 'https://ui-avatars.com/api/?name=Admin&background=667eea&color=fff&size=150'),
('user1', 'user1@example.com', 'password123', '9876543211', 'User Address', 'Mumbai', 'Maharashtra', '400001', 'https://ui-avatars.com/api/?name=User1&background=667eea&color=fff&size=150');

INSERT INTO products (name, description, price, stock, image_url) VALUES
('The Great Gatsby', 'A novel by F. Scott Fitzgerald', 19.99, 50, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400&h=300&fit=crop'),
('To Kill a Mockingbird', 'A novel by Harper Lee', 24.99, 30, 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=400&h=300&fit=crop'),
('1984', 'A dystopian novel by George Orwell', 15.99, 40, 'https://images.unsplash.com/photo-1535905557558-afc4877a26fc?w=400&h=300&fit=crop');

