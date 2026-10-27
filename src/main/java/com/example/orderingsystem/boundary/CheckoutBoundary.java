package com.example.orderingsystem.boundary;

import com.example.orderingsystem.boundary.dto.ApiResponse;
import com.example.orderingsystem.boundary.dto.BatalTransaksiRequest;
import com.example.orderingsystem.boundary.dto.CheckoutRequest;
import com.example.orderingsystem.boundary.dto.CheckoutResponse;
import com.example.orderingsystem.controller.CheckoutController;
import com.example.orderingsystem.controller.dto.CheckoutResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <<boundary>> CheckoutUI: hanya menangani HTTP, validasi input, dan konversi DTO <-> Command.
 * Tidak ada logika bisnis di sini.
 */
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutBoundary {

    private final CheckoutController checkoutController;

    /** klikTombolCheckout(keranjangData) */
    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(@Valid @RequestBody CheckoutRequest request) {
        CheckoutResult hasil = checkoutController.prosesCheckout(request.toCommand());
        return ResponseEntity.ok(ApiResponse.success("Checkout dan pembayaran berhasil diproses", CheckoutResponse.from(hasil)));
    }

    /** batalkanTransaksi(alasan) */
    @PostMapping("/{idPesanan}/batal")
    public ResponseEntity<ApiResponse<Void>> batalkanTransaksi(@PathVariable String idPesanan,
                                                               @Valid @RequestBody BatalTransaksiRequest request) {
        checkoutController.batalkanTransaksi(idPesanan, request.alasan());
        return ResponseEntity.ok(ApiResponse.success("Transaksi berhasil dibatalkan: " + request.alasan(), null));
    }
}
