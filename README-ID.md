# TitikTolak Printer Android — prototipe 0.1

Aplikasi Android polling API website setiap 5 detik, lalu mengirim perintah TSPL ke printer Bluetooth Classic (SPP).

## Penting sebelum dipakai
- Ini prototipe awal, belum diuji di perangkat pengguna.
- Template cetak awal memakai TSPL dengan ukuran sementara `80 x 100 mm`.
- Nama field API dibuat fleksibel, tetapi perlu dicocokkan dengan respons asli `api.php`.
- Aplikasi menyimpan ID pesanan yang sudah dicetak di HP untuk mencegah cetak ulang. Karena itu, bila hasil cetak gagal setelah perintah dikirim, perlu reset data aplikasi atau ubah logika pencetakan.
- Versi ini tidak mengubah status pesanan di server.
- Untuk pemakaian toko, uji dengan pesanan dummy terlebih dahulu dan pastikan format serta ukuran kertas benar.

## Cara build APK hanya menggunakan HP Android
1. Buka GitHub di browser HP dan buat repository baru bernama `TitikTolakPrinter`.
2. Upload semua file dan folder dari ZIP ini ke repository. Pastikan folder `.github/workflows` ikut ter-upload.
3. Setelah file masuk ke branch `main`, buka tab **Actions** pada repository.
4. Pilih workflow **Build Android APK**, tekan **Run workflow** bila perlu.
5. Tunggu sampai job selesai hijau. Buka hasil run, bagian **Artifacts**, lalu unduh `TitikTolakPrinter-debug`.
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
