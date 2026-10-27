// =====================================================
// QuickBite - Frontend sederhana (HTML, CSS, JavaScript)
// Berkomunikasi dengan backend Spring Boot:
//   POST /api/checkout                -> checkout pesanan
//   POST /api/checkout/{id}/batal     -> batalkan pesanan
// =====================================================

// Kalau halaman dibuka dari backend (http://localhost:8080 atau domain online) pakai alamat relatif.
// Kalau dibuka langsung dari file / Live Server / preview IntelliJ, arahkan ke backend di port 8080.
const DIBUKA_DARI_BACKEND = window.location.protocol.startsWith("http") && window.location.port !== "5500" && window.location.port !== "63342";
const BASE_URL = DIBUKA_DARI_BACKEND ? "" : "http://localhost:8080";
const API_URL = BASE_URL + "/api/checkout";

// Data menu disamakan dengan data seeder di database (tabel menu).
// Backend belum punya endpoint untuk mengambil daftar menu, jadi ditulis di sini.
const daftarMenu = [
    {
        idMenu: "MENU-01",
        nama: "Kopi Susu Gula Aren",
        harga: 18000,
        ikon: "☕",
        deskripsi: "Espresso, susu segar, dan gula aren asli. Manisnya pas untuk menemani harimu."
    },
    {
        idMenu: "MENU-02",
        nama: "Croissant Butter",
        harga: 25000,
        ikon: "🥐",
        deskripsi: "Renyah di luar, lembut berlapis di dalam, dipanggang dengan butter premium."
    }
];

// Isi keranjang: { "MENU-01": 2, "MENU-02": 1 }
let keranjang = {};

// Metode pembayaran yang dipilih
let metodePembayaran = "MIDTRANS";

// Riwayat pesanan yang dibuat selama halaman dibuka
let riwayat = [];

// ID pesanan yang sedang akan dibatalkan (dipakai oleh popup)
let idYangDibatalkan = null;

// ---------- Fungsi bantu ----------

function formatRupiah(angka) {
    return "Rp" + angka.toLocaleString("id-ID");
}

function formatTanggal(teks) {
    return new Date(teks).toLocaleString("id-ID", { dateStyle: "medium", timeStyle: "short" });
}

function cariMenu(idMenu) {
    return daftarMenu.find(function (menu) {
        return menu.idMenu === idMenu;
    });
}

function jumlahItemKeranjang() {
    let jumlah = 0;
    for (const idMenu in keranjang) {
        jumlah = jumlah + keranjang[idMenu];
    }
    return jumlah;
}

// ---------- Tampilkan daftar menu ----------

function tampilkanMenu() {
    const wadah = document.getElementById("daftar-menu");
    wadah.innerHTML = "";

    daftarMenu.forEach(function (menu, index) {
        const qty = keranjang[menu.idMenu] || 0;
        let infoKeranjang = "";
        if (qty > 0) {
            infoKeranjang = "<span class='di-keranjang'>" + qty + " di keranjang</span>";
        }

        const kartu = document.createElement("div");
        // Kartu kedua dibuat gelap sebagai aksen
        kartu.className = index % 2 === 1 ? "menu-card gelap" : "menu-card";
        kartu.innerHTML =
            "<div class='menu-icon'>" + menu.ikon + "</div>" +
            "<h3>" + menu.nama + "</h3>" +
            "<p class='deskripsi'>" + menu.deskripsi + "</p>" +
            "<div class='menu-bawah'>" +
            "  <div>" +
            "    <div class='harga'>" + formatRupiah(menu.harga) + "</div>" +
            "    " + infoKeranjang +
            "  </div>" +
            "  <button class='btn-tambah' onclick=\"tambahKeKeranjang('" + menu.idMenu + "')\">+ Tambah</button>" +
            "</div>";
        wadah.appendChild(kartu);
    });
}

// ---------- Keranjang ----------

function tambahKeKeranjang(idMenu) {
    if (keranjang[idMenu]) {
        keranjang[idMenu] = keranjang[idMenu] + 1;
    } else {
        keranjang[idMenu] = 1;
    }
    perbaruiTampilan();
}

function kurangiDariKeranjang(idMenu) {
    keranjang[idMenu] = keranjang[idMenu] - 1;
    if (keranjang[idMenu] <= 0) {
        delete keranjang[idMenu];
    }
    perbaruiTampilan();
}

function kosongkanKeranjang() {
    keranjang = {};
    document.getElementById("pesan").innerHTML = "";
    perbaruiTampilan();
}

function tampilkanKeranjang() {
    const wadah = document.getElementById("keranjang");
    const daftarId = Object.keys(keranjang);
    let total = 0;

    if (daftarId.length === 0) {
        wadah.innerHTML = '<p class="kosong">Keranjang masih kosong. Tambahkan menu dulu, ya.</p>';
        document.getElementById("total").textContent = formatRupiah(0);
        return;
    }

    wadah.innerHTML = "";
    daftarId.forEach(function (idMenu) {
        const menu = cariMenu(idMenu);
        const qty = keranjang[idMenu];
        const subtotal = menu.harga * qty;
        total = total + subtotal;

        const baris = document.createElement("div");
        baris.className = "item-keranjang";
        baris.innerHTML =
            "<div>" +
            "  <b>" + menu.ikon + " " + menu.nama + "</b><br>" +
            "  <small>" + formatRupiah(menu.harga) + " × " + qty + " = " + formatRupiah(subtotal) + "</small>" +
            "</div>" +
            "<div class='qty'>" +
            "  <button type='button' onclick=\"kurangiDariKeranjang('" + idMenu + "')\">−</button>" +
            "  <span>" + qty + "</span>" +
            "  <button type='button' onclick=\"tambahKeKeranjang('" + idMenu + "')\">+</button>" +
            "</div>";
        wadah.appendChild(baris);
    });

    document.getElementById("total").textContent = formatRupiah(total);
}

// Progress bar: langkah 1 = pilih menu, langkah 2 = isi pembayaran
function tampilkanProgress() {
    const adaIsi = jumlahItemKeranjang() > 0;
    document.getElementById("progress-bar").style.width = adaIsi ? "100%" : "50%";
    document.getElementById("step-text").textContent = adaIsi ? "Langkah 2 dari 2 · Pembayaran" : "Langkah 1 dari 2 · Pilih menu";
    document.getElementById("badge-keranjang").textContent = jumlahItemKeranjang();
}

function perbaruiTampilan() {
    tampilkanMenu();
    tampilkanKeranjang();
    tampilkanProgress();
}

// ---------- Pilihan metode pembayaran ----------

function pilihMetode(event) {
    const tombol = event.target.closest(".pill");
    if (!tombol) {
        return;
    }
    document.querySelectorAll("#pilihan-metode .pill").forEach(function (pill) {
        pill.classList.remove("aktif");
    });
    tombol.classList.add("aktif");
    metodePembayaran = tombol.dataset.metode;
}

// ---------- Pesan sukses / error ----------

function tampilkanSukses(html) {
    document.getElementById("pesan").innerHTML = '<div class="alert alert-sukses">' + html + "</div>";
}

function tampilkanError(hasil) {
    let html = "<p><b>Gagal:</b> " + hasil.message + "</p>";

    // Error validasi berisi daftar field yang salah
    if (hasil.errors) {
        html += "<ul>";
        for (const field in hasil.errors) {
            html += "<li>" + hasil.errors[field] + "</li>";
        }
        html += "</ul>";
    }

    document.getElementById("pesan").innerHTML = '<div class="alert alert-error">' + html + "</div>";
}

// ---------- Checkout ----------

async function prosesCheckout(event) {
    event.preventDefault(); // supaya halaman tidak reload

    const daftarId = Object.keys(keranjang);
    if (daftarId.length === 0) {
        tampilkanError({ message: "Keranjang masih kosong. Tambahkan menu terlebih dahulu." });
        return;
    }

    // Susun data sesuai CheckoutRequest di backend
    const dataRequest = {
        idPelanggan: document.getElementById("idPelanggan").value,
        tokenKartu: document.getElementById("tokenKartu").value,
        metodePembayaran: metodePembayaran,
        keranjang: daftarId.map(function (idMenu) {
            return { idMenu: idMenu, qty: keranjang[idMenu] };
        })
    };

    const tombol = document.getElementById("btn-checkout");
    tombol.disabled = true;
    tombol.textContent = "Memproses...";

    try {
        const response = await fetch(API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(dataRequest)
        });
        const hasil = await response.json();

        if (response.ok) {
            const data = hasil.data;
            tampilkanSukses(
                "<p><b>🎉 " + hasil.message + "</b></p>" +
                "<p>ID Pesanan: " + data.idPesanan + "</p>" +
                "<p>Total Bayar: " + formatRupiah(data.total) + "</p>" +
                "<p>Kode Referensi: " + data.kodeReferensi + "</p>"
            );
            riwayat.unshift(data);
            tampilkanRiwayat();

            // Kosongkan keranjang setelah berhasil
            keranjang = {};
            perbaruiTampilan();
        } else {
            tampilkanError(hasil);
        }
    } catch (error) {
        tampilkanError({ message: "Tidak dapat terhubung ke server. Pastikan backend sudah berjalan." });
    }

    tombol.disabled = false;
    tombol.textContent = "Checkout";
}

// ---------- Riwayat & pembatalan ----------

function tampilkanRiwayat() {
    const wadah = document.getElementById("isi-riwayat");
    document.getElementById("riwayat-kosong").hidden = riwayat.length > 0;
    wadah.innerHTML = "";

    riwayat.forEach(function (pesanan) {
        let tombolBatal = "";
        if (pesanan.statusPesanan === "DIPROSES") {
            tombolBatal = "<button class='btn btn-dark btn-small' onclick=\"bukaDialogBatal('" + pesanan.idPesanan + "')\">Batalkan</button>";
        }

        const kartu = document.createElement("div");
        kartu.className = "order-card";
        kartu.innerHTML =
            "<div class='order-atas'>" +
            "  <span class='order-id'>" + pesanan.idPesanan + "</span>" +
            "  <span class='status status-" + pesanan.statusPesanan + "'>" + pesanan.statusPesanan + "</span>" +
            "</div>" +
            "<div class='order-detail'>" +
            "  <p>" + formatTanggal(pesanan.tanggal) + "</p>" +
            "  <p>Ref: " + pesanan.kodeReferensi + "</p>" +
            "</div>" +
            "<div class='order-bawah'>" +
            "  <span class='order-total'>" + formatRupiah(pesanan.total) + "</span>" +
            "  " + tombolBatal +
            "</div>";
        wadah.appendChild(kartu);
    });
}

function bukaDialogBatal(idPesanan) {
    idYangDibatalkan = idPesanan;
    document.getElementById("dialog-id").textContent = idPesanan;
    const inputAlasan = document.getElementById("alasan");
    inputAlasan.value = "";
    inputAlasan.placeholder = "contoh: ingin mengganti menu";
    inputAlasan.classList.remove("input-error");
    document.getElementById("dialog-batal").showModal();
}

async function batalkanPesanan(idPesanan, alasan) {
    try {
        const response = await fetch(API_URL + "/" + idPesanan + "/batal", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ alasan: alasan })
        });
        const hasil = await response.json();

        if (response.ok) {
            tampilkanSukses("<p><b>" + hasil.message + "</b></p><p>Stok menu telah dikembalikan.</p>");
            riwayat.forEach(function (pesanan) {
                if (pesanan.idPesanan === idPesanan) {
                    pesanan.statusPesanan = "BATAL";
                }
            });
            tampilkanRiwayat();
        } else {
            tampilkanError(hasil);
        }
    } catch (error) {
        tampilkanError({ message: "Tidak dapat terhubung ke server. Pastikan backend sudah berjalan." });
    }
    document.getElementById("checkout").scrollIntoView();
}

function tutupDialogBatal() {
    document.getElementById("dialog-batal").close();
    idYangDibatalkan = null;
}

// Dipanggil saat tombol "Ya, Batalkan" di popup diklik
function konfirmasiBatal() {
    const idPesanan = idYangDibatalkan;
    const inputAlasan = document.getElementById("alasan");
    const alasan = inputAlasan.value.trim();

    // Alasan wajib diisi (sesuai validasi di backend)
    if (alasan === "") {
        inputAlasan.placeholder = "Alasan wajib diisi";
        inputAlasan.classList.add("input-error");
        inputAlasan.focus();
        return;
    }
    inputAlasan.classList.remove("input-error");
    tutupDialogBatal();
    if (idPesanan) {
        batalkanPesanan(idPesanan, alasan);
    }
}

// ---------- Jalankan saat halaman dibuka ----------

document.getElementById("form-checkout").addEventListener("submit", prosesCheckout);
document.getElementById("pilihan-metode").addEventListener("click", pilihMetode);
document.getElementById("btn-kosongkan").addEventListener("click", kosongkanKeranjang);
document.getElementById("btn-konfirmasi-batal").addEventListener("click", konfirmasiBatal);
document.getElementById("btn-tutup-dialog").addEventListener("click", tutupDialogBatal);
perbaruiTampilan();
tampilkanRiwayat();
