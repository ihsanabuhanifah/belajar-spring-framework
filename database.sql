-- 1. TABEL UTAMA UNTUK LOGIN (USERS)
CREATE TABLE users (
    username VARCHAR(100) NOT NULL,
    password VARCHAR(200) NOT NULL, -- Diperpanjang menjadi 200 untuk menampung Bcrypt Password
    role VARCHAR(50) NOT NULL,      -- Contoh: 'ADMIN', 'HRD', 'EMPLOYEE'
    token VARCHAR(100),
    token_expired_at BIGINT,
    PRIMARY KEY (username),
    UNIQUE KEY unique_token (token)
) ENGINE=InnoDB;

-- 2. TABEL DEPARTEMEN / DIVISI
CREATE TABLE departments (
    id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,     -- Contoh: 'Teknologi Informasi', 'Keuangan'
    description TEXT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 3. TABEL PROFIL KARYAWAN (EMPLOYEES)
CREATE TABLE employees (
    id VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL, -- Terhubung ke akun loginnya
    department_id VARCHAR(100),     -- Terhubung ke divisinya
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    job_title VARCHAR(100) NOT NULL, -- Contoh: 'Java Developer', 'HR Manager'
    joined_at BIGINT NOT NULL,       -- Tanggal masuk kerja (Epoch Time)
    PRIMARY KEY (id),
    UNIQUE KEY unique_email (email),
    CONSTRAINT fk_users_employees FOREIGN KEY (username) REFERENCES users (username) ON DELETE CASCADE,
    CONSTRAINT fk_departments_employees FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 4. TABEL ABSENSI HARIAN (ATTENDANCES)
CREATE TABLE attendances (
    id VARCHAR(100) NOT NULL,
    employee_id VARCHAR(100) NOT NULL,
    date_bytes BIGINT NOT NULL,     -- Tanggal absen (Format yyyymmdd dalam angka)
    clock_in BIGINT,                -- Jam masuk kerja (Timestamp)
    clock_out BIGINT,               -- Jam pulang kerja (Timestamp)
    status VARCHAR(50) NOT NULL,    -- 'PRESENT' (Hadir), 'LATE' (Terlambat), 'SICK' (Sakit), 'LEAVE' (Cuti)
    PRIMARY KEY (id),
    CONSTRAINT fk_employees_attendances FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. TABEL PENGGAJIAN BULANAN (PAYROLLS)
CREATE TABLE payrolls (
    id VARCHAR(100) NOT NULL,
    employee_id VARCHAR(100) NOT NULL,
    period VARCHAR(20) NOT NULL,    -- Contoh: '2026-06' (Bulan Juni 2026)
    basic_salary BIGINT NOT NULL,   -- Gaji Pokok
    allowance BIGINT DEFAULT 0,     -- Tunjangan
    deduction BIGINT DEFAULT 0,     -- Potongan (Gara-gara sering terlambat/absen)
    net_salary BIGINT NOT NULL,     -- Gaji Bersih yang diterima
    paid_at BIGINT,                 -- Tanggal ditransfer gajinya
    status VARCHAR(50) NOT NULL,    -- 'PENDING', 'PAID'
    PRIMARY KEY (id),
    CONSTRAINT fk_employees_payrolls FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE
) ENGINE=InnoDB;