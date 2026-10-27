package com.example.orderingsystem.boundary.dto;

import com.example.orderingsystem.controller.dto.CheckoutResult;
import com.example.orderingsystem.entity.StatusPesanan;

import java.time.LocalDateTime;

public record CheckoutResponse(
        String idPesanan,
        String idPembayaran,
        double total,
        StatusPesanan statusPesanan,
        String statusPembayaran,
        String kodeReferensi,
        LocalDateTime tanggal) {

    public static CheckoutResponse from(CheckoutResult r) {
        return new CheckoutResponse(r.idPesanan(), r.idPembayaran(), r.total(), r.statusPesanan(),
                r.statusPembayaran(), r.kodeReferensi(), r.tanggal());
    }
}
