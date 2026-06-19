-- =========================================================================
-- DATABASE SCRIPT: INTEGRASI IDENTITAS UTUH & TOKO ONLINE ENTERPRISE
-- DEVELOPER: AMA (AMATULLAH)
-- =========================================================================

-- DROP TABLE IF EXISTS BERURUTAN (BIAR AMAN & BERSIH SAAT RE-RUN)
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS order_details;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS users;


-- =========================================================================
-- SISI 1: TABEL OTENTIKASI & IDENTITAS PROFIL USER (MANDATORI)
-- =========================================================================

-- 1. TABEL UTAMA UNTUK LOGIN (USERS)
CREATE TABLE users (
    username VARCHAR(100) NOT NULL,
    password VARCHAR(200) NOT NULL, -- Menampung Bcrypt encrypted password
    role VARCHAR(50) NOT NULL,      -- Contoh: 'ADMIN', 'HRD', 'EMPLOYEE', 'CUSTOMER'
    token VARCHAR(100),
    token_expired_at BIGINT,
    PRIMARY KEY (username),
    UNIQUE KEY unique_token (token)
) ENGINE=InnoDB;

-- 2. TABEL IDENTITAS / PROFIL DETAIL USER (EMPLOYEES)
CREATE TABLE employees (
    id VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL, -- Kunci rahasia terhubung ke akun loginnya di atas
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    job_title VARCHAR(100) NOT NULL, -- Contoh: 'Java Developer', 'Customer Premium'
    joined_at BIGINT NOT NULL,       -- Tanggal daftar/masuk (Epoch Time)
    PRIMARY KEY (id),
    UNIQUE KEY unique_email (email),
    CONSTRAINT fk_users_employees FOREIGN KEY (username) REFERENCES users (username) ON DELETE CASCADE
) ENGINE=InnoDB;


-- =========================================================================
-- SISI 2: EXTENSION TAMBAHAN UNTUK DOMAIN TOKO ONLINE ENTERPRISE (BARU)
-- =========================================================================

-- 3. TABEL KATEGORI PRODUK
CREATE TABLE categories (
    id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,     -- Contoh: 'Elektronik', 'Pakaian'
    description TEXT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 4. TABEL PRODUK / BARANG JUALAN
CREATE TABLE products (
    id VARCHAR(100) NOT NULL,
    category_id VARCHAR(100),       
    name VARCHAR(200) NOT NULL,     
    price BIGINT NOT NULL,          
    stock INT NOT NULL DEFAULT 0,   
    version BIGINT NOT NULL DEFAULT 0, -- ⚔️ Optimistic Locking untuk mengunci stok Flash Sale
    PRIMARY KEY (id),
    CONSTRAINT fk_categories_products FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 5. TABEL TRANSAKSI UTAMA (ORDERS)
CREATE TABLE orders (
    id VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL, -- 🔗 JEMBATAN KUNCI: Menembak langsung ke tabel users
    order_date BIGINT NOT NULL,     -- Waktu checkout (Epoch Time)
    total_amount BIGINT NOT NULL,   
    status VARCHAR(50) NOT NULL,    -- 'PENDING', 'PAID', 'SHIPPED'
    PRIMARY KEY (id),
    CONSTRAINT fk_users_orders FOREIGN KEY (username) REFERENCES users (username) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. TABEL DETAIL BELANJAAN (ORDER DETAILS)
CREATE TABLE order_details (
    id VARCHAR(100) NOT NULL,
    order_id VARCHAR(100) NOT NULL, 
    product_id VARCHAR(100) NOT NULL, 
    quantity INT NOT NULL,          
    price BIGINT NOT NULL,          -- Harga barang mencatat harga saat detik pembelian
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_details FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_products_details FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 7. TABEL JEJAK SEJARAH DIGITAL (AUDIT LOGS)
CREATE TABLE audit_logs (
    id VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL, 
    action VARCHAR(100) NOT NULL,   -- Contoh: 'CHECKOUT_ORDER', 'UPDATE_PRODUCT'
    entity_name VARCHAR(100) NOT NULL, 
    entity_id VARCHAR(100) NOT NULL, 
    old_value TEXT,                 
    new_value TEXT,                 
    created_at BIGINT NOT NULL,     
    PRIMARY KEY (id)
) ENGINE=InnoDB;