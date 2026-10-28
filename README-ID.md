# Printer Thermal Bridge Android — prototipe 0.1

Aplikasi Android memeriksa status printer lalu polling API setiap 5 detik, kemudian mengirim perintah TSPL ke printer Bluetooth Classic (SPP).

## Penting sebelum dipakai
- Versi ini belum diuji ulang pada printer fisik setelah penambahan pemeriksaan status kertas.
- Template cetak awal memakai TSPL dengan ukuran sementara `80 x 100 mm`.
- Nama field API dibuat fleksibel, tetapi perlu dicocokkan dengan respons asli `api.php`.
- Aplikasi menyimpan ID pesanan yang sudah dicetak di HP untuk mencegah cetak ulang. Karena itu, bila hasil cetak gagal setelah perintah dikirim, perlu reset data aplikasi atau ubah logika pencetakan.
- Jika printer melaporkan kertas habis atau status printer tidak bisa dibaca, aplikasi menghentikan polling API. Pemeriksaan ini memakai perintah status TSPL `ESC!?`; jika firmware EP807 tidak mendukung respons tersebut, aplikasi akan tetap menjeda API sampai status dapat dibaca. Uji saat printer tersedia.
- Polling setiap 5 detik berarti hingga 3.600 permintaan per jam saat layanan terus aktif. Pastikan server API cukup ringan; bila server terbebani, naikkan interval.
- Versi ini tidak mengubah status pesanan di server.
- Untuk pemakaian toko, uji dengan pesanan dummy terlebih dahulu dan pastikan format serta ukuran kertas benar.

## Cara build APK hanya menggunakan HP Android
1. Buka repository GitHub `printerbrigde` yang sudah digunakan, atau repository proyek yang Anda pilih.
2. Upload semua file dan folder dari ZIP ini ke repository. Pastikan folder `.github/workflows` ikut ter-upload.
3. Setelah file masuk ke branch `main`, buka tab **Actions** pada repository.
4. Pilih workflow **Build Android APK**, tekan **Run workflow** bila perlu.
5. Tunggu sampai job selesai hijau. Buka hasil run, bagian **Artifacts**, lalu unduh artifact APK dari workflow.
6. Ekstrak ZIP artifact dan instal `app-debug.apk`. Android mungkin meminta izin instal aplikasi dari browser/File Manager.

## Penggunaan
1. Pasangkan printer `MH1.12.03.2022` di Setelan Bluetooth Android.
2. Buka aplikasi, masukkan URL API (default `https://titiktolak.com/printer-test/api.php`).
3. Tekan **Lihat printer yang sudah dipasangkan**, lalu pilih printer.
4. Tekan **Mulai cetak otomatis**. Pertahankan aplikasi/layanan berjalan dan nonaktifkan optimasi baterai untuk aplikasi ini bila Android menghentikan layanan.

## Format respons API yang didukung
Bisa berupa array JSON:
```json
[
  {
    "id": 1001,
    "status": "pending",
    "customer": "Pelanggan Tes",
    "items": [
      {"name": "Nasi Goreng", "qty": 2, "price": 15000},
      {"name": "Es Teh", "qty": 1, "price": 5000}
    ],
    "total": 35000
  }
]
```
Atau object dengan properti `orders` atau `data` yang berisi array. Field item mendukung `name/product/item/nama`, `qty/quantity/jumlah`, dan `price/subtotal/harga`.

## Jika gagal build
GitHub Actions akan menampilkan log kesalahan. Kirim screenshot atau teks error tersebut untuk diperbaiki. Pastikan repository berisi file dan folder pada tingkat teratas, bukan hanya folder ZIP pembungkus.
