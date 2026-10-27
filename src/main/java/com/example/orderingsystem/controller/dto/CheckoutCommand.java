package com.example.orderingsystem.controller.dto;

import java.util.List;

/**
 * Input use case checkout. Tipe internal Controller, tidak bergantung pada DTO HTTP.
 */
public record CheckoutCommand(
        String idPelanggan,
        String tokenKartu,
        String metodePembayaran,
        List<Item> items) {

    public record Item(String idMenu, int qty) { }
}
