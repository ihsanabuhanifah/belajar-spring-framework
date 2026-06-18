# 🏛️ Arsitektur & Alur Kerja Sistem HRIS Enterprise

Dokumen ini menjelaskan bagaimana ekosistem teknologi kita saling mengobrol dan menceritakan petualangan data dari ujung Frontend (Postman) hingga ke berbagai database.

---

## 🧭 Gambaran Umum Arsitektur (High-Level Architecture)

Aplikasi ini menggunakan pola modern bernama **CQRS (Command Query Responsibility Segregation)**, yaitu memisahkan jalur operasi pembuatan/perubahan data (Write) dengan jalur pencarian data (Read).

---

## 📑 Cerita Alur Kerja Fitur Utama (The Story of Data)

### 🔑 1. Fitur Autentikasi Kilat (Login & Session)

Ketika seorang HRD atau Karyawan menekan tombol **Login** di aplikasi:

1. Request masuk ke Spring Boot membawa `username` dan `password`.
2. Spring Boot mengecek ke **MySQL**, lalu mencocokkan password-nya menggunakan algoritma keamanan `BCrypt`.
3. Jika cocok, Spring Boot akan membuatkan token acak (`X-API-TOKEN`).
4. **Alur Rahasia:** Token ini tidak disimpan di MySQL, melainkan dititipkan ke **Redis** dengan waktu hangus (TTL) 30 menit.
5. Setiap kali user tersebut mengakses data lain, sistem cukup melirik **Redis** (hanya butuh waktu < 1 milidetik) untuk memastikan session masih aktif tanpa perlu membebani MySQL.

---

### 📝 2. Fitur Manajemen Karyawan & Sinkronisasi Otomatis

Ketika Admin menambahkan data karyawan baru (`POST /api/employees`):

1. Data lengkap profil karyawan langsung disimpan ke **MySQL** sebagai sumber data utama (_Source of Truth_).
2. Tepat setelah sukses di MySQL, Spring Boot tidak langsung mendaftarkannya ke mesin pencari, melainkan melemparkan pesan kecil berupa objek JSON `EmployeeEvent` ke **RabbitMQ**.
3. Spring Boot langsung membalas ke User: _"Data Sukses Disimpan!"_. User bisa langsung bekerja lagi tanpa _loading_ lama.
4. **Di balik layar (Asynchronous):** Ada kurir bernama `@RabbitListener` yang diam-diam terbangun ketika melihat ada pesan baru di RabbitMQ. Dia mengambil pesan tersebut lalu menyalin data karyawan baru itu ke dalam **Elasticsearch**.

---

### 🔍 3. Fitur Pencarian Pintar (High-Performance Search)

Ketika Manager HRD ingin mencari karyawan di kolom pencarian (`GET /api/employees/search?name=amato`):

1. Request **TIDAK DIKIRIM ke MySQL**, melainkan diarahkan 100% langsung menembak **Elasticsearch**.
2. Elasticsearch mencari data menggunakan indeks tekstual yang sangat canggih.
3. **Kelebihan:** Fitur ini mendukung _Fuzzy Search_. Jika HRD salah mengetik nama menjadi `Amatulah` (kurang huruf l), Elasticsearch akan tetap pintar dan berhasil menemukan data `Amatullah` dalam hitungan milidetik dari jutaan data.

---

### ⏱️ 4. Fitur Absensi Harian Anti-Tabrakan (Anti Race Condition)

Ketika ribuan karyawan melakukan absen masuk (`Clock In`) secara serentak pada jam 07.59 pagi:

1. Server akan kebanjiran request di milidetik yang sama. Rentan terjadi _Race Condition_ (data absen ganda atau server eror).
2. **Penanganan Enterprise:** Sebelum memproses absen di MySQL, Spring Boot akan memasang gembok pengunci digital di **Redis** (_Distributed Lock_) menggunakan ID karyawan tersebut.
3. Jika jari karyawan tidak sengaja menekan tombol absen dua kali, request kedua akan otomatis mentok di gembok Redis dan ditolak dengan aman. Request pertama pun diproses ke **MySQL** dengan status `PRESENT` atau `LATE`.

---

### 💰 5. Fitur Penggajian Massal Aman (Transaction & Audit Trail)

Ketika Direktur menekan tombol "Bayar Gaji Bulanan" untuk seluruh divisi:

1. Fungsi Service yang dibungkus `@Transactional` dinyalakan.
2. Sistem melakukan perulangan (_loop_) untuk menghitung gaji pokok, tunjangan, dan potongan absensi masing-masing orang di **MySQL**.
3. Jika di tengah jalan (misal pada karyawan ke-500) mendadak mati lampu atau server database putus koneksi, sistem akan otomatis melakukan **Rollback** (pembatalan total). Gaji karyawan 1 sampai 499 yang sempat tersimpan akan dihapus kembali demi mencegah kekacauan laporan keuangan.
4. Jika proses sukses total, sistem akan otomatis menulis buku harian besar di tabel `audit_logs` yang mencatat detail: _Siapa yang membayar, total uangnya berapa, dan jam berapa transaksi disetujui._

---

## 🛠️ Ringkasan Tugas Setiap Teknologi di Proyek Kita

| Teknologi            | Peran Utama                | Alasan Digunakan                                                                                                               |
| :------------------- | :------------------------- | :----------------------------------------------------------------------------------------------------------------------------- |
| 🐬 **MySQL**         | Lembaga Keuangan & Dokumen | Menyimpan data asli yang wajib valid dan tidak boleh hilang (Data User, Gaji, Absen).                                          |
| ⚡ **Redis**         | Laci Meja & Gembok Gerbang | Menyimpan Token Session agar web super cepat, melakukan caching data divisi, dan mengunci request tabrakan (_Race Condition_). |
| 🐇 **RabbitMQ**      | Kantor Pos / Kurir         | Mengantrekan tugas-tugas berat (kirim notifikasi/sinkronisasi data) agar aplikasi utama tidak pernah mengalami _lag/freezing_. |
| 🔍 **Elasticsearch** | Pustakawan Super Pintar    | Mengurusi pencarian data karyawan secara instan dengan fitur toleransi salah ketik (_Fuzzy Search_).                           |
