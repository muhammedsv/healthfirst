-- =====================================================
-- PIMS - HealthFirst Pharmacy
-- Run this whole script in MySQL Workbench (lightning bolt)
-- =====================================================
DROP DATABASE IF EXISTS healthfirst_pims;
CREATE DATABASE healthfirst_pims;
USE healthfirst_pims;

-- Application user used by the Java program (DBConnection.java)
CREATE USER IF NOT EXISTS 'pims_user'@'localhost' IDENTIFIED BY 'Pims@123';
GRANT ALL PRIVILEGES ON healthfirst_pims.* TO 'pims_user'@'localhost';
FLUSH PRIVILEGES;

-- ---------- TABLE 1: users ----------
CREATE TABLE users (
    user_id   INT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(50) NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,            -- stored as SHA-256 hash
    role      ENUM('Admin','Cashier') NOT NULL,
    full_name VARCHAR(100) NOT NULL
);

-- ---------- TABLE 2: suppliers ----------
CREATE TABLE suppliers (
    supplier_id    INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    phone          VARCHAR(20),
    email          VARCHAR(100),
    address        TEXT
);

-- ---------- TABLE 3: medicines ----------
CREATE TABLE medicines (
    medicine_id       INT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(150) NOT NULL,
    company           VARCHAR(100),
    medicine_type     VARCHAR(50),
    price             DECIMAL(10,2) NOT NULL,
    quantity_in_stock INT NOT NULL DEFAULT 0,
    reorder_level     INT NOT NULL DEFAULT 10,
    expiry_date       DATE NOT NULL,
    supplier_id       INT,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
);

-- ---------- TABLE 4: sales ----------
CREATE TABLE sales (
    sale_id      INT AUTO_INCREMENT PRIMARY KEY,
    sale_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL,
    user_id      INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ---------- TABLE 5: sale_items ----------
CREATE TABLE sale_items (
    sale_item_id  INT AUTO_INCREMENT PRIMARY KEY,
    sale_id       INT NOT NULL,
    medicine_id   INT NOT NULL,
    quantity_sold INT NOT NULL,
    price_at_sale DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (sale_id)     REFERENCES sales(sale_id),
    FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id)
);

-- =====================================================
-- SAMPLE DATA
-- =====================================================
INSERT INTO users (username, password, role, full_name) VALUES
('admin',   SHA2('admin123',256), 'Admin',   'Pharmacy Administrator'),
('cashier', SHA2('cashier123',256),  'Cashier', 'Default Cashier');

INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES
('MediSupply SA',   'Thabo Mokoena', '012 345 6789', 'thabo@medisupply.co.za', '12 Church Street, Pretoria'),
('PharmaDirect',    'Sarah Naidoo',  '011 222 3344', 'sarah@pharmadirect.co.za', '45 Rissik Street, Johannesburg'),
('Cape Wholesalers','Pieter van Wyk','021 555 8899', 'pieter@capewhole.co.za', '8 Long Street, Cape Town');

INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id) VALUES
('Panado 500mg',            'Adcock Ingram',   'Tablet',    25.50, 200, 50, DATE_ADD(CURDATE(), INTERVAL 18 MONTH), 1),
('Amoxil 500mg',            'Aspen',           'Capsule',   85.00, 120, 40, DATE_ADD(CURDATE(), INTERVAL 12 MONTH), 2),
('Nurofen 200mg',           'Reckitt',         'Tablet',    42.75,  15, 30, DATE_ADD(CURDATE(), INTERVAL 10 MONTH), 1),
('Benylin Cough Syrup',     'Johnson & Johnson','Syrup',    68.90,  60, 20, DATE_ADD(CURDATE(), INTERVAL 15 DAY),   3),
('Vitamin C 1000mg',        'Cipla',           'Tablet',    55.00,  90, 25, DATE_ADD(CURDATE(), INTERVAL 14 MONTH), 2),
('Hydrocortisone Cream',    'Cipla',           'Cream',     62.40,   8, 15, DATE_ADD(CURDATE(), INTERVAL 20 DAY),   3),
('Cetirizine 10mg',         'Aspen',           'Tablet',    38.20, 150, 40, DATE_ADD(CURDATE(), INTERVAL 16 MONTH), 2),
('Omeprazole 20mg',         'Adcock Ingram',   'Capsule',   72.00,  75, 30, DATE_ADD(CURDATE(), INTERVAL 9 MONTH),  1),
('Diclofenac Injection',    'Aspen',           'Injection',120.00,  25, 10, DATE_ADD(CURDATE(), INTERVAL 6 MONTH),  2),
('Antiseptic Cream',        'Johnson & Johnson','Cream',    29.99,   5, 20, DATE_ADD(CURDATE(), INTERVAL 25 DAY),   3);

-- Sample sales so the reports show data
INSERT INTO sales (sale_date, total_amount, user_id) VALUES
(DATE_SUB(NOW(), INTERVAL 2 DAY), 106.00, 2),
(DATE_SUB(NOW(), INTERVAL 1 DAY), 161.40, 2),
(NOW(),                           145.40, 2);

INSERT INTO sale_items (sale_id, medicine_id, quantity_sold, price_at_sale) VALUES
(1, 1, 2, 25.50), (1, 5, 1, 55.00),
(2, 2, 1, 85.00), (2, 7, 2, 38.20),
(3, 4, 1, 68.90), (3, 1, 3, 25.50);

-- Quick check
SELECT username, role FROM users;