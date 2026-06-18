# 📘 Technical Specification & Roadmap: HRIS Enterprise System

**Target Arsitektur:** High-Performance, Event-Driven, Secure, & Fault-Tolerant  
**Developer:** Ama (Amatullah)  
**Urutan Eksekusi:** Tahap 1 sampai Tahap 7

---

## 🏗️ Detail Arsitektur & Aliran Data Sistem

Aplikasi ini memisahkan jalur modifikasi data (**Write/Command**) dan jalur pencarian data (**Read/Query**).

1. **Jalur Transaksi (Write):** Client → Controller → Service (`@Transactional`) → Simpan ke **MySQL** & **Redis** → Kirim pesan ke **RabbitMQ**.
2. **Jalur Sinkronisasi (Async):** **RabbitMQ** → Diambil oleh `@RabbitListener` (Consumer) → Disimpan ke **Elasticsearch**.
3. **Jalur Pencarian (Read):** Client → Controller → **Elasticsearch** (Tanpa menyentuh MySQL).

---

## 🚀 Detail Implementasi Per Tahap

### 📂 TAHAP 1: Inisialisasi Proyek & Entitas Basis (MySQL)

**Target:** Menyiapkan representasi tabel database ke dalam class Java (JPA Entity) beserta repositorinya.

- [ ] **1.1 Pembuatan Struktur Folder (Package):**
  - Buat folder baru di bawah `programmerzamannow.restfull.entity.hris`
  - Buat folder repository di bawah `programmerzamannow.restfull.repository.hris`
- [ ] **1.2 Pembuatan Java Entity:**
  - `User.java`: Menampung `username` (PK), `password` (terenkripsi), `role` (Enum/String: ADMIN, HRD, EMPLOYEE).
  - `Department.java`: Menampung `id` (PK), `name`, `description`.
  - `Employee.java`: Menampung data profil, berelasi `@OneToOne` dengan `User` dan `@ManyToOne` dengan `Department`.
  - `Attendance.java`: Menampung `id`, `employee_id` (FK), `date_bytes` (YYYYMMDD), `clock_in`, `clock_out`, `status`.
  - `Payroll.java`: Menampung `id`, `employee_id` (FK), `period` (YYYY-MM), `basic_salary`, `allowance`, `deduction`, `net_salary`, `status`, dan kolom versi `@Version` untuk locking.
- [ ] **1.3 Pembuatan JpaRepository:**
  - Buat interface repository untuk kelima entitas di atas, pastikan meng-extends `JpaRepository<Entity, String>`.

---

### 🔐 TAHAP 2: Authentication & Authorization via Redis (RAM Speed)

**Target:** Mengamankan API menggunakan token session yang ditaruh di Redis dengan performa pembacaan di bawah 1 milidetik.

- [ ] **2.1 Konfigurasi Koneksi Redis (`application.properties`):**
  - Mengatur `spring.data.redis.host=localhost` dan `spring.data.redis.port=6379`.
- [ ] **2.2 Logika Register & Login (`UserService.java`):**
  - Registrasi wajib melakukan _hashing_ password menggunakan `BCrypt.hashpw()`.
  - Saat login sukses, generate UUID acak sebagai `X-API-TOKEN`.
  - Simpan token tersebut ke **Redis** sebagai _Key_, dan data JSON objek `User` sebagai _Value_. Set waktu kedaluwarsa (TTL) selama 30 menit (`Duration.ofMinutes(30)`).
- [ ] **2.3 Pembuatan WebMvcConfigurer & Interceptor:**
  - Buat `AuthInterceptor` yang bertugas mencegat semua request masuk, membaca header `X-API-TOKEN`, dan mencocokkannya ke Redis menggunakan `StringRedisTemplate`.
  - Jika token tidak ada di Redis, langsung lempar error `401 UNAUTHORIZED`.
- [ ] **2.4 Implementasi Role-Based Authorization:**
  - Buat mekanisme pengecekan di Controller. Jika method hanya boleh diakses oleh `HRD` atau `ADMIN`, lakukan validasi `user.getRole()` sebelum logika bisnis dijalankan. Jika melanggar, lempar error `403 FORBIDDEN`.

---

### 📜 TAHAP 3: Implementasi Transaksi Aman & Audit Trail (Catatan Sejarah)

**Target:** Menjamin konsistensi data keuangan dan mencatat setiap jejak digital perubahan data sensitif.

- [ ] **3.1 Pembuatan Tabel & Entity `AuditLog`:**
  - Kolom: `id`, `username` (pelaku), `action` (CONTOH: CREATE_PAYROLL), `entity_name`, `entity_id`, `old_value` (JSON text), `new_value` (JSON text), `created_at`.
- [ ] **3.2 Implementasi `@Transactional` pada Pembuatan Gaji Bulanan:**
  - Buat method `public void processPayrollMassal(...)` di `PayrollService`.
  - Pasang anotasi `@Transactional(rollbackFor = Exception.class)`.
  - Di dalam method ini, lakukan loop hitung gaji karyawan, simpan ke tabel `payrolls`, dan simpan log aktivitasnya ke tabel `audit_logs`. Jika di tengah-tengah loop terjadi error (misal koneksi mati), seluruh data gaji yang sempat tersimpan wajib dibatalkan otomatis (_rollback_).

---

### ⚔️ TAHAP 4: Mengatasi Race Condition (Pencegahan Kebocoran Uang)

**Target:** Mencegah terjadinya data ganda atau salah hitung jika ada dua request masuk bersamaan di milidetik yang sama (contoh: user klik tombol "Absen" atau "Bayar Gaji" dua kali dengan cepat).

- [ ] **4.1 Solusi 1: Optimistic Locking via JPA:**
  - Tambahkan properti `private Long version;` yang dipasangi anotasi `jakarta.persistence.Version` di dalam entitas `Payroll` dan `Employee`.
  - Jika ada dua request mencoba mengubah baris gaji yang sama, request kedua akan otomatis gagal dan melemparkan `ObjectOptimisticLockingFailureException`.
- [ ] **4.2 Solusi 2: Distributed Lock via Redis (Redisson) untuk Absensi:**
  - Sebelum proses `clockIn` dijalankan di `AttendanceService`, lakukan _booking_ kunci di Redis menggunakan Key berbentuk: `lock:attendance:employeeId:yyyymmdd`.
  - Jika kunci gagal didapatkan (artinya sedang ada proses absen lain yang berjalan untuk karyawan tersebut), langsung tolak request kedua dengan pesan "Proses sedang berjalan, mohon tunggu".

---

### ✉️ TAHAP 5: Asynchronous Processing dengan RabbitMQ (Antrean Kerja)

**Target:** Memisahkan proses berat (cetak PDF slip gaji dan kirim email) ke jalur antrean belakang agar performa API tetap kencang.

- [ ] **5.1 Konfigurasi RabbitMQ (`RabbitMessagingConfig.java`):**
  - Deklarasikan nama Exchange: `hris.analytics.exchange`
  - Deklarasikan nama Queue: `hris.payroll.queue`
  - Deklarasikan Routing Key: `hris.payroll.routingkey`
  - Pasang `Jackson2JsonMessageConverter` agar Spring Boot bisa mengirim objek Java dalam bentuk JSON otomatis ke RabbitMQ.
- [ ] **5.2 Pembuatan Producer di `PayrollService`:**
  - Setelah status transaksi gaji berubah menjadi `PAID` di MySQL, panggil `rabbitTemplate.convertAndSend()` untuk melempar data `PayrollEvent` ke RabbitMQ.
- [ ] **5.3 Pemantauan via Dashboard UI:**
  - Buka `localhost:15672` untuk memastikan status antrean bertambah saat proses pembayaran gaji dipicu.

---

### 🔍 TAHAP 6: Sinkronisasi Otomatis & Pencarian Kilat via Elasticsearch

**Target:** Membaca antrean dari RabbitMQ untuk dimasukkan ke Elasticsearch, lalu membangun fitur pencarian karyawan yang super cepat.

- [ ] **6.1 Pembuatan `EmployeeDocument.java`:**
  - Buat entity khusus Elasticsearch menggunakan anotasi `@Document(indexName = "employees")`.
  - Pasang anotasi `@Id`, `@Field(type = FieldType.Text, analyzer = "standard")` pada kolom nama dan jabatan agar bisa di-indeks secara tekstual.
- [ ] **6.2 Pembuatan Consumer (`PayrollListener.java` / `EmployeeListener.java`):**
  - Buat class baru dengan anotasi `@Component`.
  - Buat method yang dipasangi `@RabbitListener(queues = "hris.payroll.queue")`.
  - Di dalam method ini, tangkap data event, lalu panggil `employeeElasticsearchRepository.save(employeeDocument)` untuk menyinkronkan data ke Elasticsearch.
- [ ] **6.3 Refaktor Endpoint Search Karyawan:**
  - Di dalam `EmployeeService`, ubah fungsi pencarian lamanya. Gunakan `NativeQueryBuilder` milik Elasticsearch untuk mencari data menggunakan metode _Fuzzy Query_ (toleransi salah ketik).

---

### 🧪 TAHAP 7: Enterprise Integration Test (Green Build)

**Target:** Menguji seluruh ekosistem teknologi tanpa mengotori database asli.

- [ ] **7.1 Pembuatan Base Integration Test:**
  - Buat file test yang dipasangi `@SpringBootTest` dan `@AutoConfigureMockMvc`.
- [ ] **7.2 Pengujian Alur Penuh (End-to-End):**
  - Tulis test case untuk login admin → simpan token ke Redis → tambah karyawan baru → jalankan transaksi penggajian massal → cek apakah pesan masuk ke RabbitMQ → cek apakah data sukses tersinkron ke Elasticsearch → lakukan pencarian data via endpoint search Elasticsearch.
  - Pastikan semua asersi (`Assertions.assertEquals`, `Assertions.assertNotNull`) bernilai benar dan test berakhir warna hijau.

---

## 🛠️ Cara Menggunakan Dokumen Ini Saat Bertanya:

Sebutkan tahapan detailnya agar kita bisa langsung fokus ke blok kode yang tepat. Contoh:

> _"Hai Gemini, saya ready. Mari kita mulai dari **TAHAP 1 (Langkah 1.2)**. Buatkan saya kode Java Entity lengkap untuk `User.java`, `Department.java`, dan `Employee.java` beserta anotasi relasi JPA-nya."_
