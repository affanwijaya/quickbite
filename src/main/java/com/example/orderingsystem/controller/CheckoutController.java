package com.example.orderingsystem.controller;

import com.example.orderingsystem.boundary.gateway.PaymentGatewayAPI;
import com.example.orderingsystem.controller.dto.CheckoutCommand;
import com.example.orderingsystem.controller.dto.CheckoutResult;
import com.example.orderingsystem.controller.exception.PaymentFailedException;
import com.example.orderingsystem.controller.exception.PesananTidakDapatDibatalkanException;
import com.example.orderingsystem.controller.exception.ResourceNotFoundException;
import com.example.orderingsystem.controller.exception.StokTidakCukupException;
import com.example.orderingsystem.entity.*;
import com.example.orderingsystem.repository.MenuRepository;
import com.example.orderingsystem.repository.PelangganRepository;
import com.example.orderingsystem.repository.PesananRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <<controller>> BCE: orkestrator use case checkout & pembatalan.
 * Bukan @RestController — HTTP ditangani oleh CheckoutBoundary.
 */
@Service
@RequiredArgsConstructor
public class CheckoutController {

    private final PelangganRepository pelangganRepository;
    private final MenuRepository menuRepository;
    private final PesananRepository pesananRepository;
    private final PaymentGatewayAPI paymentGateway; // Boundary eksternal (outbound)

    @Transactional
    public CheckoutResult prosesCheckout(CheckoutCommand cmd) {
        // 1. Verifikasi Pelanggan
        Pelanggan pelanggan = pelangganRepository.findById(cmd.idPelanggan())
                .orElseThrow(() -> new ResourceNotFoundException("Pelanggan dengan ID " + cmd.idPelanggan() + " tidak ditemukan"));

        // 2. Kunci & validasi stok SEMUA menu dulu, sebelum ada yang dipotong.
        //    Qty dijumlahkan per menu agar menu yang muncul dua kali di keranjang tetap tervalidasi benar.
        Map<String, Integer> totalQtyPerMenu = new HashMap<>();
        List<ResolvedItem> items = new ArrayList<>();
        for (CheckoutCommand.Item ci : cmd.items()) {
            Menu menu = menuRepository.findByIdForUpdate(ci.idMenu())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu dengan ID " + ci.idMenu() + " tidak ditemukan"));
            int qtyKumulatif = totalQtyPerMenu.merge(menu.getIdMenu(), ci.qty(), Integer::sum);
            if (!menu.validasiStok(qtyKumulatif)) {
                throw new StokTidakCukupException(menu.getNamaMenu(), menu.getStok());
            }
            items.add(new ResolvedItem(menu, ci.qty()));
        }

        // 3. Bentuk Pesanan + ItemPesanan (komposisi) yang merujuk Menu (agregasi)
        Pesanan pesanan = Pesanan.builder()
                .idPesanan(newId("ORD"))
                .tanggal(LocalDateTime.now())
                .pelanggan(pelanggan)
                .build();
        for (ResolvedItem ri : items) {
            pesanan.tambahItem(ItemPesanan.builder()
                    .idItem(newId("ITEM"))
                    .jumlah(ri.qty())
                    .menu(ri.menu())
                    .build());
        }
        double total = pesanan.hitungTotal();

        // 4. Boundary eksternal: Payment Gateway.
        //    Catatan produksi: call ini masih di dalam @Transactional sehingga lock stok ditahan selama HTTP;
        //    untuk produksi pisahkan menjadi reservasi (tx1) -> gateway -> finalisasi (tx2).
        String kodeRef = paymentGateway.mintaTransaksiPembayaran(total, cmd.tokenKartu());

        // 5. Pembayaran + verifikasi domain
        Pembayaran pembayaran = Pembayaran.builder()
                .idPembayaran(newId("PAY"))
                .jumlahBayar(total)
                .metode(cmd.metodePembayaran())
                .statusBayar(Pembayaran.STATUS_SUKSES)
                .kodeReferensi(kodeRef)
                .pesanan(pesanan)
                .build();
        if (!pembayaran.prosesVerifikasi()) {
            // Exception -> @Transactional rollback, stok tidak terpotong
            throw new PaymentFailedException("Verifikasi pembayaran gagal untuk pesanan " + pesanan.getIdPesanan());
        }
        pesanan.setPembayaran(pembayaran);

        // 6. Potong stok setelah pembayaran sukses
        for (ResolvedItem ri : items) {
            ri.menu().kurangiStok(ri.qty());
        }
        pesanan.updateStatus(StatusPesanan.DIPROSES);

        // 7. Simpan (cascade menyimpan ItemPesanan & Pembayaran)
        pesananRepository.save(pesanan);

        return new CheckoutResult(
                pesanan.getIdPesanan(),
                pembayaran.getIdPembayaran(),
                total,
                pesanan.getStatus(),
                pembayaran.getStatusBayar(),
                pembayaran.getKodeReferensi(),
                pesanan.getTanggal());
    }

    @Transactional
    public void batalkanTransaksi(String idPesanan, String alasan) {
        Pesanan pesanan = pesananRepository.findById(idPesanan)
                .orElseThrow(() -> new ResourceNotFoundException("Pesanan dengan ID " + idPesanan + " tidak ditemukan"));

        if (pesanan.getStatus() == StatusPesanan.SELESAI || pesanan.getStatus() == StatusPesanan.DIKIRIM) {
            throw new PesananTidakDapatDibatalkanException("Pesanan sudah dikirim/selesai dan tidak dapat dibatalkan.");
        }
        if (pesanan.getStatus() == StatusPesanan.BATAL) {
            throw new PesananTidakDapatDibatalkanException("Pesanan " + idPesanan + " sudah dibatalkan sebelumnya.");
        }

        // Kembalikan stok menu yang dipesan
        for (ItemPesanan item : pesanan.getDaftarItem()) {
            Menu menu = item.getMenu();
            menu.setStok(menu.getStok() + item.getJumlah());
        }

        pesanan.updateStatus(StatusPesanan.BATAL);
        pesananRepository.save(pesanan);

        System.out.println("== [LOG PEMBATALAN] Pesanan " + idPesanan + " dibatalkan. Alasan: " + alasan + " ==");
    }

    private static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private record ResolvedItem(Menu menu, int qty) { }
}
