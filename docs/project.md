# 📘 Technical Specification & Roadmap: Toko Online Enterprise System

**Target Arsitektur:** High-Performance, Event-Driven, Secure, & Fault-Tolerant  
**Developer:** Ama (Amatullah)  
**Urutan Eksekusi:** Tahap 1 sampai Tahap 7  
**Catatan Penting:** Entity `User` dan `Employee` yang sudah ada tidak boleh diubah strukturnya, hanya diintegrasikan ke dalam domain Toko Online.

---

## 🏗️ Detail Arsitektur & Aliran Data Sistem

Aplikasi ini memisahkan jalur modifikasi data (**Write/Command**) dan jalur pencarian produk (**Read/Query**).

1. **Jalur Transaksi (Write):** Client → Controller → Service (`@Transactional`) → Simpan ke **MySQL** & **Redis** → Kirim pesan ke **RabbitMQ**.
2. **Jalur Sinkronisasi (Async):** **RabbitMQ** → Diambil oleh `@RabbitListener` (Consumer) → Disimpan ke **Elasticsearch**.
3. **Jalur Pencarian (Read):** Client → Cari Produk → **Elasticsearch** (Bisa fuzzy search sekelas Tokopedia/Shopee, tanpa menyentuh MySQL).

---

## 🚀 Detail Implementasi Per Tahap

### 📂 TAHAP 1: Inisialisasi Proyek & Entitas Toko Online (MySQL)

**Target:** Menyiapkan tabel baru untuk keperluan Toko Online tanpa merusak kelas `User` dan `Employee` yang sudah ada di proyek Ama.

- [ ] **1.1 Pembuatan Struktur Folder (Package):**
  - Gunakan package yang sudah ada untuk `User` dan `Employee`.
  - Buat folder baru untuk domain baru di bawah `programmerzamannow.HRIS` (atau disesuaikan menjadi `programmerzamannow.shop` jika Ama ingin memisahkan package):
    - `category/entity` dan `category/repository`
    - `product/entity` dan `product/repository`
    - `order/entity` dan `order/repository`
- [ ] **1.2 Pembuatan Java Entity Baru & Relasi:**
  - `Category.java`: Menampung `id` (String/Long PK), `name`, dan `description`.
  - `Product.java`: Menampung `id` (PK), `name`, `price` (BigDecimal/Long), `stock` (Integer), berelasi `@ManyToOne` dengan `Category`, dan memiliki kolom versi `@Version` untuk mencegah rebutan stok.
  - `Order.java`: Menampung `id` (PK), berelasi `@ManyToOne` dengan `User` atau `Employee` (sebagai pembeli), `order_date` (Long/Timestamp), `total_amount`, dan `status` (Enum/String: PENDING, PAID, SHIPPED).
  - `OrderDetail.java`: Menampung `id` (PK), berelasi `@ManyToOne` dengan `Order` dan `@ManyToOne` dengan `Product`, serta menampung `quantity` dan `price` saat dibeli.
- [ ] **1.3 Pembuatan JpaRepository:**
  - Buat interface repository untuk `CategoryRepository`, `ProductRepository`, `OrderRepository`, dan `OrderDetailRepository`.

### 🔐 TAHAP 2: Authentication & Authorization via Redis (RAM Speed)

**Target:** Mengamankan API belanja menggunakan token session yang ditaruh di Redis dengan performa pembacaan di bawah 1 milidetik.

- [ ] **2.1 Konfigurasi Koneksi Redis (`application.properties`)**
- [ ] **2.2 Logika Register & Login (`UserService.java`):**
  - Login sukses akan meng-generate `X-API-TOKEN` (UUID). Simpan ke Redis dengan TTL 30 menit.
- [ ] **2.3 Pembuatan WebMvcConfigurer & Interceptor (`AuthInterceptor`):**
  - Mencegat request, membaca token dari Redis. Jika tidak ada, langsung lempar `401 UNAUTHORIZED`.
- [ ] **2.4 Implementasi Role-Based Authorization di Controller:**
  - Validasi kasta `Role`. Fitur menambah produk atau mengedit stok barang hanya boleh diakses oleh akun dengan role `ADMIN` atau `MERCHANT`. Jika role `CUSTOMER` atau `EMPLOYEE` biasa mencoba menambah barang, lempar eror `403 FORBIDDEN`.

### 📜 TAHAP 3: Implementasi Transaksi Aman & Audit Trail (Catatan Sejarah)

**Target:** Menjamin konsistensi keuangan belanjaan dan stok barang serta mencatat log aktivitas sensitif.

- [ ] **3.1 Pembuatan Tabel & Entity `AuditLog`:**
  - Kolom: `id`, `username` (pelaku), `action` (CONTOH: CHECKOUT_ORDER, UPDATE_PRICE), `created_at`.
- [ ] **3.2 Pembelian Menggunakan `@Transactional`:**
  - Buat method `public void checkout(...)` di `OrderService`.
  - Pasang `@Transactional(rollbackFor = Exception.class)`.
  - Di dalam method, lakukan loop untuk memeriksa stok barang di tabel `products`, kurangi stoknya, simpan data ke `orders` dan `order_details`. Jika di tengah jalan ada satu barang yang stoknya mendadak habis, seluruh transaksi wajib dibatalkan otomatis (_rollback_).

### ⚔️ TAHAP 4: Mengatasi Race Condition (Pencegahan Rebutan Stok Flash Sale)

**Target:** Mencegah terjadinya _overselling_ (barang terjual melebihi stok asli) jika ada ribuan orang membeli barang sisa 1 secara bersamaan.

- [ ] **4.1 Solusi 1: Optimistic Locking via JPA:**
  - Gunakan properti `@Version` di entitas `Product`. Request kedua yang mencoba mengubah stok barang yang sama di milidetik yang sama akan otomatis gagal dengan `ObjectOptimisticLockingFailureException`.
- [ ] **4.2 Solusi 2: Distributed Lock via Redis (Redisson) untuk Checkout:**
  - Sebelum memotong stok barang di MySQL, lakukan _booking_ kunci di Redis menggunakan Key berbentuk: `lock:product:productId`. Pembeli lain harus mengantre dengan tertib.

### ✉️ TAHAP 5: Asynchronous Processing dengan RabbitMQ (Antrean Cetak Invoice)

**Target:** Memisahkan proses berat (pembuatan PDF nota belanja dan kirim email notifikasi) ke jalur antrean belakang agar API tetap kencang.

- [ ] **5.1 Konfigurasi RabbitMQ (`RabbitMessagingConfig.java`):**
  - Exchange: `shop.order.exchange`
  - Queue: `shop.invoice.queue`
  - Routing Key: `shop.invoice.routingkey`
- [ ] **5.2 Pembuatan Producer di `OrderService`:**
  - Setelah status pembayaran pesanan berubah menjadi `PAID` di MySQL, panggil `rabbitTemplate.convertAndSend()` untuk melempar data `OrderEvent` ke RabbitMQ.

### 🔍 TAHAP 6: Sinkronisasi Otomatis & Pencarian Kilat via Elasticsearch

**Target:** Membaca antrean dari RabbitMQ untuk dimasukkan ke Elasticsearch agar pembeli bisa mencari produk dengan sangat cepat dan pintar.

- [ ] **6.1 Pembuatan `ProductDocument.java` untuk Elasticsearch.**
- [ ] **6.2 Pembuatan Consumer (`OrderListener.java`):**
  - Membaca antrean dari RabbitMQ, lalu memanggil `productElasticsearchRepository.save(productDocument)` untuk menyinkronkan data barang ke Elasticsearch secara asinkron.
- [ ] **6.3 Fitur Fuzzy Search Produk:**
  - Gunakan `NativeQueryBuilder` Elasticsearch untuk mencari produk. Jika user salah mengetik _"komeja"_ atau _"sepatu larih"_, Elasticsearch akan tetap menampilkan produk _"Kemeja"_ dan _"Sepatu Lari"_.

### 🧪 TAHAP 7: Enterprise Integration Test (Green Build)

**Target:** Menguji seluruh siklus belanja dari login, checkout item flash sale, antrean pesan, hingga pencarian Elasticsearch sampai tes berwarna hijau (_Green Build_).
