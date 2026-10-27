// =====================================================
// QuickBite - Frontend sederhana (HTML, CSS, JavaScript)
// Berkomunikasi dengan backend Spring Boot:
//   POST /api/checkout                -> checkout pesanan
//   POST /api/checkout/{id}/batal     -> batalkan pesanan
// =====================================================

// Kalau halaman dibuka dari backend (http://localhost:8080) pakai alamat relatif.
// Kalau dibuka langsung dari file / Live Server / preview IntelliJ, arahkan ke backend di port 8080.
const BASE_URL = window.location.port === "8080" ? "" : "http://localhost:8080";
const API_URL = BASE_URL + "/api/checkout";

// Data menu disamakan dengan data seeder di database (tabel menu).
// Backend belum punya endpoint untuk mengambil daftar menu, jadi ditulis di sini.
const daftarMenu = [
    { idMenu: "MENU-01", nama: "Kopi Susu Gula Aren", harga: 18000 },
    { idMenu: "MENU-02", nama: "Croissant Butter", harga: 25000 }
];

// Isi keranjang: { "MENU-01": 2, "MENU-02": 1 }
let keranjang = {};

// Riwayat pesanan yang dibuat selama halaman dibuka
let riwayat = [];

// ---------- Fungsi bantu ----------

function formatRupiah(angka) {
    return "Rp" + angka.toLocaleString("id-ID");
}

function formatTanggal(teks) {
    return new Date(teks).toLocaleString("id-ID");
}

function cariMenu(idMenu) {
    return daftarMenu.find(function (menu) {
        return menu.idMenu === idMenu;
    });
}

// ---------- Tampilkan daftar menu ----------

function tampilkanMenu() {
    const wadah = document.getElementById("daftar-menu");
    wadah.innerHTML = "";

    daftarMenu.forEach(function (menu) {
        const div = document.createElement("div");
        div.className = "menu-item";
        div.innerHTML =
            "<div>" +
            "  <h3>" + menu.nama + "</h3>" +
            "  <p>" + formatRupiah(menu.harga) + "</p>" +
            "  <small>" + menu.idMenu + "</small>" +
            "</div>" +
            "<button onclick=\"tambahKeKeranjang('" + menu.idMenu + "')\">+ Tambah</button>";
        wadah.appendChild(div);
    });
}

// ---------- Keranjang ----------

function tambahKeKeranjang(idMenu) {
    if (keranjang[idMenu]) {
        keranjang[idMenu] = keranjang[idMenu] + 1;
    } else {
        keranjang[idMenu] = 1;
    }
    tampilkanKeranjang();
}

function kurangiDariKeranjang(idMenu) {
    keranjang[idMenu] = keranjang[idMenu] - 1;
    if (keranjang[idMenu] <= 0) {
        delete keranjang[idMenu];
    }
    tampilkanKeranjang();
}

function tampilkanKeranjang() {
    const wadah = document.getElementById("keranjang");
    const daftarId = Object.keys(keranjang);
    let total = 0;

    if (daftarId.length === 0) {
        wadah.innerHTML = '<p class="kosong">Keranjang masih kosong.</p>';
        document.getElementById("total").textContent = formatRupiah(0);
        return;
    }

    wadah.innerHTML = "";
    daftarId.forEach(function (idMenu) {
        const menu = cariMenu(idMenu);
        const qty = keranjang[idMenu];
        const subtotal = menu.harga * qty;
        total = total + subtotal;

        const div = document.createElement("div");
        div.className = "item-keranjang";
        div.innerHTML =
            "<div>" +
            "  <b>" + menu.nama + "</b><br>" +
            "  <small>" + formatRupiah(menu.harga) + " x " + qty + " = " + formatRupiah(subtotal) + "</small>" +
            "</div>" +
            "<div class='qty'>" +
            "  <button onclick=\"kurangiDariKeranjang('" + idMenu + "')\">-</button>" +
            "  <span>" + qty + "</span>" +
            "  <button onclick=\"tambahKeKeranjang('" + idMenu + "')\">+</button>" +
            "</div>";
        wadah.appendChild(div);
    });

    document.getElementById("total").textContent = formatRupiah(total);
}

// ---------- Pesan sukses / error ----------

function tampilkanSukses(html) {
    document.getElementById("pesan").innerHTML = '<div class="sukses">' + html + "</div>";
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

    document.getElementById("pesan").innerHTML = '<div class="error">' + html + "</div>";
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
        metodePembayaran: document.getElementById("metodePembayaran").value,
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
                "<p><b>" + hasil.message + "</b></p>" +
                "<p>ID Pesanan: " + data.idPesanan + "</p>" +
                "<p>Total Bayar: " + formatRupiah(data.total) + "</p>" +
                "<p>Status: " + data.statusPesanan + " (pembayaran " + data.statusPembayaran + ")</p>" +
                "<p>Kode Referensi: " + data.kodeReferensi + "</p>"
            );
            riwayat.unshift(data);
            tampilkanRiwayat();

            // Kosongkan keranjang setelah berhasil
            keranjang = {};
            tampilkanKeranjang();
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
    const tabel = document.getElementById("tabel-riwayat");
    const isi = document.getElementById("isi-riwayat");

    document.getElementById("riwayat-kosong").hidden = riwayat.length > 0;
    tabel.hidden = riwayat.length === 0;
    isi.innerHTML = "";

    riwayat.forEach(function (pesanan) {
        let tombolBatal = "-";
        if (pesanan.statusPesanan === "DIPROSES") {
            tombolBatal = "<button class='btn-batal' onclick=\"batalkanPesanan('" + pesanan.idPesanan + "')\">Batalkan</button>";
        }

        const baris = document.createElement("tr");
        baris.innerHTML =
            "<td>" + pesanan.idPesanan + "</td>" +
            "<td>" + formatTanggal(pesanan.tanggal) + "</td>" +
            "<td>" + formatRupiah(pesanan.total) + "</td>" +
            "<td><span class='status status-" + pesanan.statusPesanan + "'>" + pesanan.statusPesanan + "</span></td>" +
            "<td>" + pesanan.kodeReferensi + "</td>" +
            "<td>" + tombolBatal + "</td>";
        isi.appendChild(baris);
    });
}

async function batalkanPesanan(idPesanan) {
    const alasan = prompt("Masukkan alasan pembatalan pesanan " + idPesanan + ":");
    if (alasan === null) {
        return; // user menekan Cancel
    }

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
}

// ---------- Jalankan saat halaman dibuka ----------

document.getElementById("form-checkout").addEventListener("submit", prosesCheckout);
tampilkanMenu();
tampilkanKeranjang();
