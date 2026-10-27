package com.example.orderingsystem.boundary.dto;

import jakarta.validation.constraints.NotBlank;

public record BatalTransaksiRequest(
        @NotBlank(message = "Alasan pembatalan harus disertakan") String alasan) { }
