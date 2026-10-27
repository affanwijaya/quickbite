package com.example.orderingsystem.boundary;

import com.example.orderingsystem.controller.CheckoutController;
import com.example.orderingsystem.controller.dto.CheckoutResult;
import com.example.orderingsystem.controller.exception.PaymentFailedException;
import com.example.orderingsystem.controller.exception.StokTidakCukupException;
import com.example.orderingsystem.entity.StatusPesanan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uji kontrak HTTP Boundary tanpa database: Controller di-mock.
 */
@WebMvcTest(CheckoutBoundary.class)
class CheckoutBoundaryTest {

    @Autowired MockMvc mvc;
    @MockitoBean CheckoutController checkoutController;

    private static final String BODY = """
            {"idPelanggan":"P-01","tokenKartu":"tok_123","keranjang":[{"idMenu":"M-01","qty":2}]}
            """;

    @Test
    void checkout_berhasil_mengembalikan_id_dan_total() throws Exception {
        given(checkoutController.prosesCheckout(any())).willReturn(new CheckoutResult(
                "P-01", "PAY-01", 50000.0, StatusPesanan.DIPROSES, "SUKSES", "PG-REF-1", LocalDateTime.now()));

        mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idPesanan").value("P-01"))
                .andExpect(jsonPath("$.data.total").value(50000.0));
    }

    @Test
    void stok_habis_409() throws Exception {
        given(checkoutController.prosesCheckout(any())).willThrow(new StokTidakCukupException("Kopi", 0));
        mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STOK_HABIS"));
    }

    @Test
    void gateway_gagal_502() throws Exception {
        given(checkoutController.prosesCheckout(any())).willThrow(new PaymentFailedException("ditolak"));
        mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PAYMENT_FAILED"));
    }
}
