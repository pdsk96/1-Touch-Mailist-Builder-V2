# ⚡ PDSK PROJECT - 1 Touch Mailist Builder

**PDSK PROJECT - 1 Touch Mailist Builder** adalah aplikasi Android tingkat lanjut (*Enterprise Grade*) berbasis **Kotlin & Jetpack Compose** dengan desain antarmuka **Cyberpunk HUD Dark Theme**. Aplikasi ini dirancang khusus untuk ekstraksi, otomatisasi, validasi, scoring, dan otomatisasi kampanye email marketing B2B/B2C secara real-time langsung dari perangkat seluler.

---

## 🚀 Fitur Unggulan (Core Features)

### 1. 🎬 Cyberpunk Animated Splash Screen & Audio Visual HUD
- **Boot Sequence FX**: Animasi booting bertema *Cyberpunk Matrix Scanline* saat aplikasi pertama kali dijalankan dengan efek suara sintetis (*Cyber Sound FX*).
- **Cyber Synth BGM Player**: Audio player latar belakang bawaan dengan kontrol musik synthwave/cyberpunk untuk suasana kerja imersif.
- **Dynamic Radar Scanner & Live Terminal**: Tampilan radar scan real-time dan terminal log HUD yang menampilkan aktivitas crawling milidetik demi milidetik.

### 2. 🤖 AI Cold Email Personalizer & Subject Line Generator (Gemini AI Integration)
- **Otomatisasi Konten Email**: Mengintegrasikan **Google Gemini AI** untuk merancang draf cold email dan judul pesan (*subject line*) secara personal berdasarkan domain target, kategori bisnis, dan profil prospek.
- **Konversi Tinggi**: Meningkatkan angka *open rate* dan *reply rate* dengan pembuatan pesan yang relevan secara kontekstual.

### 3. 📩 Built-in Direct SMTP Cold Email Campaign Sender
- **Pengiriman Langsung dari Aplikasi**: Mengirim email penawaran otomatis menggunakan server SMTP kustom (Gmail, SendGrid, Mailgun, Amazon SES).
- **Perlindungan Spam Filter**: Dilengkapi fitur *Smart Random Delay* (jeda acak 15–60 detik per email) untuk mencegah email masuk ke folder Spam atau terdeteksi bot.

### 4. 🛡️ Proxy Rotator & Custom User-Agent Defense
- **Perlindungan Anti-Bot**: Menggunakan rotasi IP Proxy (HTTP/SOCKS5) dan header *User-Agent* browser acak saat proses crawling.
- **Bypass Proteksi**: Mencegah pemblokiran IP, rate-limiting, atau Cloudflare CAPTCHA pada website berkeamanan tinggi.

### 5. 📊 Lead Quality Scoring & Business Industry Auto-Tagging
- **Penilaian Kualitas Prospek (0-100)**: Memberikan skor kualitas (*Hot Leads*) berdasarkan keaktifan DNS MX, ketersediaan kontak WhatsApp/Telepon, tautan Media Sosial, dan kategori domain (Business, Gov, Edu).
- **Penyaringan Cerdas**: Memudahkan tim sales untuk memprioritaskan prospek berpotensi paling tinggi.

### 6. 🔍 Google Maps & Business Directory Scraping Engine
- **Ekstraksi Bisnis Lokal**: Mengambil data kontak bisnis (Nama Bisnis, Email, Telepon/WA, Media Sosial) secara otomatis dari Google Maps dan Direktori Bisnis (YellowPages, Yelp).
- **Targeting Geografis**: Mendapatkan data prospek bisnis lokal yang akurat dan berdaya beli tinggi.

### 7. ☁️ Google Drive & Webhook Cloud Auto-Sync
- **Sinkronisasi Real-Time**: Otomatisasi pengiriman hasil scraping ke Google Sheets, Zapier/Make Webhook, atau database cloud CRM.
- **Integrasi Seamless**: Data prospek tersambung langsung dengan workflow tim tanpa pemindahan file CSV manual.

### 8. 🎯 SMTP Handshake Mailbox Ping (Port 25 Live VRFY Check)
- **Verifikasi Mailbox 99.9% Akurat**: Melakukan simulasi *SMTP Handshake* (`RCPT TO`) ke server email tujuan untuk memastikan alamat email benar-benar aktif tanpa perlu mengirim pesan fisik.

### 9. ⚡ Persistent Background Auto-Start & Battery Saver Exemption
- **Operasi 24/7 Tanpa Henti**: Dilengkapi dengan `BootReceiver` untuk otomatis memulai mesin crawler saat smartphone dihidupkan (*Boot Completed* / *App Update*).
- **Mode Bebas Hemat Baterai (Doze Mode Bypass)**: Pembebasan pembatasan baterai sistem Android & integrasi cepat ke Pengaturan Auto-Start OEM (Xiaomi, Oppo, Vivo, Samsung, Huawei) agar mesin crawler berjalan stabil di latar belakang.

### 10. 📷 ML Kit Optical Character Recognition (OCR) Scanner
- **Pemindaian Teks dari Gambar Web**: Menggunakan **Google ML Kit Text Recognition** untuk mendeteksi dan mengekstraksi alamat email yang tersembunyi di dalam banner gambar, flyer, kartu nama digital, serta payload gambar Base64 pada halaman web.
- **Kontrol Toggle OCR**: Dilengkapi sakelar (ON/OFF) pada menu konfigurasi untuk memilih antara mode *Deep OCR Scanning* atau mode ekstraksi cepat (*Code Stream Only*).

### 11. 📄 Deep Multi-Format & Document Stream Parsing Engine
- **Ekstraksi Serbaguna**: Mendukung ekstraksi alamat email dari berbagai format data kompleks termasuk payload JSON, atribut media HTML (`alt`, `title`, `data-email`), file PDF (`/URI` link & stream teks `Tj`/`TJ`), dokumen Office XML (`<w:t>`, `<cell>`), serta dump database SQL (`INSERT INTO`).

---

## 🛠️ Fitur Pendukung & Arsitektur Utama

- **🔘 1-Touch Engine & Speed Control**: Peluncuran crawler hanya dengan satu sentuhan dengan 3 opsi kecepatan (*Ultra Fast*, *Balanced*, *Stealth*).
- **🌐 Dork Search Engine**: Penemuan benih crawler (*seed URLs*) secara otomatis hanya dengan memasukkan kata kunci ceruk bisnis.
- **⏱️ Cron Auto-Scheduler**: Penjadwalan tugas crawling otomatis di latar belakang secara berkala (1 jam, 6 jam, 12 jam, 24 jam).
- **⚡ Foreground Service & Wake Lock**: Menjaga mesin crawler tetap aktif beroperasi meskipun layar smartphone mati atau aplikasi diminimalkan.
- **📑 Auto-Categorization**: Pengelompokan otomatis email ke dalam kategori: `GMAIL`, `YAHOO`, `BUSINESS`, `EDU`, `GOV`, `OUTLOOK`, dan `OTHER`.
- **📂 Export & Import Engine**: Fitur impor CSV/TXT dengan pembersih duplikat otomatis, serta ekspor multi-format (PDF Analytics Report, HTML, Excel, CSV, JSON, TXT, Clipboard).
- **💾 Room Local Database**: Penyimpanan data terenkripsi lokal yang aman dan persisten.

---

## 📦 Teknologi yang Digunakan

| Komponen | Teknologi / Library |
| :--- | :--- |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material Design 3 + Cyberpunk HUD Custom Styling) |
| **Architecture** | MVVM (Model-View-ViewModel) + Clean Architecture |
| **Concurrency** | Kotlin Coroutines & StateFlow |
| **Database** | Room Persistence Library (KSP) |
| **Networking & Parsing** | OkHttp, Jsoup, Java Net/Sockets |
| **Computer Vision** | Google ML Kit Text Recognition (OCR) |
| **AI Integration** | Google Gemini API (REST Service) |

---

## 📌 Lisensi & Pengembang

Dikembangkan dan Dikelola oleh **PDSK PROJECT**.  
*Enterprise Email Intelligence & Scraping Solutions.*
