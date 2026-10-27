package com.example.orderingsystem.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "pembayaran")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pembayaran {
    public static final String STATUS_SUKSES = "SUKSES";
    @Id
    @Column(name = "id_pembayaran", length = 36)
    private String idPembayaran;
    @Column(name = "jumlah_bayar", nullable = false)
    private double jumlahBayar;
    @Column(nullable = false, length = 50)
    private String metode;
    @Column(name = "status_bayar", nullable = false, length = 50)
    private String statusBayar;
    @Column(name = "kode_referensi", length = 100)
    private String kodeReferensi;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pesanan", nullable = false, unique = true)
    private Pesanan pesanan;
    // Domain Method sesuai Class Diagram
    public boolean prosesVerifikasi() {
        return STATUS_SUKSES.equalsIgnoreCase(this.statusBayar)
                && this.kodeReferensi != null
                && !this.kodeReferensi.isEmpty();
    }
}