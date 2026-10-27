package com.example.orderingsystem.controller.dto;

import com.example.orderingsystem.entity.StatusPesanan;

import java.time.LocalDateTime;

/**
 * Output use case checkout. Controller mengembalikan ini, bukan Entity JPA.
 */
public record CheckoutResult(
        String idPesanan,
        String idPembayaran,
        double total,
        StatusPesanan statusPesanan,
        String statusPembayaran,
        String kodeReferensi,
        LocalDateTime tanggal) { }
