package com.example.orderingsystem.boundary.gateway;

import com.example.orderingsystem.controller.exception.PaymentFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter tiruan (mock) PaymentGatewayAPI untuk pengembangan & praktikum.
 * Untuk gateway sungguhan (mis. Midtrans), buat adapter lain yang memakai RestClient
 * dan ganti bean ini — Controller tidak perlu diubah.
 */
@Component
class MockPaymentGateway implements PaymentGatewayAPI {

    private final String endpointUrl;
    private final String apiKey;

    MockPaymentGateway(@Value("${payment.gateway.endpoint-url}") String endpointUrl,
                       @Value("${payment.gateway.api-key}") String apiKey) {
        this.endpointUrl = endpointUrl;
        this.apiKey = apiKey;
    }

    @Override
    public String mintaTransaksiPembayaran(double total, String tokenKartu) {
        System.out.println("== [BOUNDARY LOG] Mengirim Transaksi ke Gateway ==");
        System.out.println("URL: " + endpointUrl);
        System.out.println("API Key: " + apiKey.substring(0, Math.min(7, apiKey.length())) + "********");
        System.out.println("Charge Total: Rp" + total);

        // Token tiruan "invalid_card" selalu ditolak
        if ("invalid_card".equalsIgnoreCase(tokenKartu)) {
            throw new PaymentFailedException("Penolakan dari Gateway Pembayaran: Saldo tidak cukup atau token kartu tidak sah");
        }
        return "PG-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
