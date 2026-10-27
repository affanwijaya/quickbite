package com.example.orderingsystem.controller.exception;

public class StokTidakCukupException extends RuntimeException {
    public StokTidakCukupException(String namaMenu, int sisa) {
        super("Stok tidak mencukupi untuk menu: " + namaMenu + " (Sisa: " + sisa + ")");
    }
}
