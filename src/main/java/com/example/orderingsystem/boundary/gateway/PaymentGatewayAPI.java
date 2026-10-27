package com.example.orderingsystem.boundary.gateway;

/**
 * <<boundary>> outbound: antarmuka ke payment gateway pihak ketiga.
 * Controller hanya bergantung pada interface ini, bukan pada SDK/HTTP client.
 */
public interface PaymentGatewayAPI {
    /**
     * @return kode referensi transaksi dari gateway
     * @throws com.example.orderingsystem.controller.exception.PaymentFailedException jika gateway menolak
     */
    String mintaTransaksiPembayaran(double total, String tokenKartu);
}
