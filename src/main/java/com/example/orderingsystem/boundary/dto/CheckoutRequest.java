package com.example.orderingsystem.boundary.dto;

import com.example.orderingsystem.controller.dto.CheckoutCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Kontrak HTTP request checkout. Dipetakan ke CheckoutCommand agar Controller tidak bergantung pada DTO web.
 */
public record CheckoutRequest(
        @NotBlank(message = "ID Pelanggan wajib diisi") String idPelanggan,
        @NotBlank(message = "Token kartu/kredensial pembayaran wajib diisi") String tokenKartu,
        String metodePembayaran, // opsional, default MIDTRANS
        @NotEmpty(message = "Keranjang belanja tidak boleh kosong") List<@Valid CartItem> keranjang) {

    public static final String METODE_DEFAULT = "MIDTRANS";

    public record CartItem(
            @NotBlank(message = "ID Menu tidak boleh kosong") String idMenu,
            @NotNull(message = "Jumlah tidak boleh null")
            @Min(value = 1, message = "Jumlah pembelian minimal 1") Integer qty) { }

    public CheckoutCommand toCommand() {
        String metode = (metodePembayaran == null || metodePembayaran.isBlank()) ? METODE_DEFAULT : metodePembayaran;
        return new CheckoutCommand(idPelanggan, tokenKartu, metode,
                keranjang.stream()
                        .map(i -> new CheckoutCommand.Item(i.idMenu(), i.qty()))
                        .toList());
    }
}
