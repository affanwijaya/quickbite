package com.example.orderingsystem;

import com.example.orderingsystem.entity.Menu;
import com.example.orderingsystem.entity.Pelanggan;
import com.example.orderingsystem.repository.MenuRepository;
import com.example.orderingsystem.repository.PelangganRepository;
import com.example.orderingsystem.repository.PesananRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uji end-to-end (Boundary -> Controller -> Entity -> DB H2 via Flyway).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutFlowTests {

    @Autowired MockMvc mockMvc;
    @Autowired PelangganRepository pelangganRepository;
    @Autowired MenuRepository menuRepository;
    @Autowired PesananRepository pesananRepository;

    @BeforeEach
    void seed() {
        pesananRepository.deleteAll();
        pelangganRepository.deleteAll();
        menuRepository.deleteAll();
        pelangganRepository.save(Pelanggan.builder().idPelanggan("CUST-001").nama("Budi Santoso")
                .email("budi@example.com").nomorTelepon("08123456789").alamat("Jl. Sudirman No. 45, Jakarta").build());
        menuRepository.save(Menu.builder().idMenu("MENU-01").namaMenu("Kopi Susu Gula Aren").harga(18000.0).stok(20).build());
        menuRepository.save(Menu.builder().idMenu("MENU-02").namaMenu("Croissant Butter").harga(25000.0).stok(10).build());
    }

    private String checkoutBody(String token, String... items) {
        return """
                {"idPelanggan":"CUST-001","tokenKartu":"%s","keranjang":[%s]}
                """.formatted(token, String.join(",", items));
    }

    private String checkout(String body) throws Exception {
        return mockMvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
    }

    private int stok(String idMenu) {
        return menuRepository.findById(idMenu).orElseThrow().getStok();
    }

    @Test
    void checkoutSukses_laluDibatalkan_stokKembali() throws Exception {
        String json = mockMvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody("tok_visa_valid_999",
                                "{\"idMenu\":\"MENU-01\",\"qty\":2}", "{\"idMenu\":\"MENU-02\",\"qty\":1}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(61000.0))
                .andExpect(jsonPath("$.data.statusPesanan").value("DIPROSES"))
                .andExpect(jsonPath("$.data.statusPembayaran").value("SUKSES"))
                .andReturn().getResponse().getContentAsString();
        assertThat(stok("MENU-01")).isEqualTo(18);
        assertThat(stok("MENU-02")).isEqualTo(9);

        String idPesanan = JsonPath.read(json, "$.data.idPesanan");
        mockMvc.perform(post("/api/checkout/" + idPesanan + "/batal").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alasan\":\"Pelanggan ingin mengganti menu pesanan\"}"))
                .andExpect(status().isOk());
        assertThat(stok("MENU-01")).isEqualTo(20);
        assertThat(stok("MENU-02")).isEqualTo(10);

        // Pembatalan kedua ditolak (409) dan stok tidak bertambah lagi
        mockMvc.perform(post("/api/checkout/" + idPesanan + "/batal").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alasan\":\"ulang\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TIDAK_DAPAT_DIBATALKAN"));
        assertThat(stok("MENU-01")).isEqualTo(20);
    }

    @Test
    void stokTidakCukup_409() throws Exception {
        mockMvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody("tok", "{\"idMenu\":\"MENU-02\",\"qty\":100}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STOK_HABIS"))
                .andExpect(jsonPath("$.message").value("Stok tidak mencukupi untuk menu: Croissant Butter (Sisa: 10)"));
    }

    @Test
    void menuSamaDuaKali_totalMelebihiStok_ditolak() throws Exception {
        checkout(checkoutBody("tok", "{\"idMenu\":\"MENU-02\",\"qty\":6}", "{\"idMenu\":\"MENU-02\",\"qty\":6}"));
        assertThat(stok("MENU-02")).isEqualTo(10);
        assertThat(pesananRepository.count()).isZero();
    }

    @Test
    void gatewayMenolak_502_danTidakAdaPerubahan() throws Exception {
        mockMvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody("invalid_card", "{\"idMenu\":\"MENU-01\",\"qty\":1}")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PAYMENT_FAILED"));
        assertThat(stok("MENU-01")).isEqualTo(20);
        assertThat(pesananRepository.count()).isZero();
    }

    @Test
    void pelangganTidakAda_404() throws Exception {
        mockMvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody("tok", "{\"idMenu\":\"MENU-01\",\"qty\":1}").replace("CUST-001", "CUST-999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void validasiInput_dan_jsonRusak_400() throws Exception {
        mockMvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.idPelanggan").exists())
                .andExpect(jsonPath("$.errors.keranjang").exists());
        mockMvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content("{rusak"))
                .andExpect(status().isBadRequest());
    }
}
