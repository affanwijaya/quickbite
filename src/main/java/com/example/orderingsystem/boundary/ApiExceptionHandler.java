package com.example.orderingsystem.boundary;

import com.example.orderingsystem.boundary.dto.ApiResponse;
import com.example.orderingsystem.controller.exception.PaymentFailedException;
import com.example.orderingsystem.controller.exception.PesananTidakDapatDibatalkanException;
import com.example.orderingsystem.controller.exception.ResourceNotFoundException;
import com.example.orderingsystem.controller.exception.StokTidakCukupException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * <<boundary>> Menerjemahkan exception domain/controller menjadi respons HTTP.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(StokTidakCukupException.class)
    public ResponseEntity<ApiResponse<Void>> stokHabis(StokTidakCukupException e) {
        return respon(HttpStatus.CONFLICT, "STOK_HABIS", e.getMessage(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> tidakDitemukan(ResourceNotFoundException e) {
        return respon(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), null);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiResponse<Void>> gagalBayar(PaymentFailedException e) {
        return respon(HttpStatus.BAD_GATEWAY, "PAYMENT_FAILED", "Pembayaran gagal diproses: " + e.getMessage(), null);
    }

    @ExceptionHandler(PesananTidakDapatDibatalkanException.class)
    public ResponseEntity<ApiResponse<Void>> tidakDapatDibatalkan(PesananTidakDapatDibatalkanException e) {
        return respon(HttpStatus.CONFLICT, "TIDAK_DAPAT_DIBATALKAN", e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validasiGagal(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(err -> {
            String field = (err instanceof FieldError fieldError) ? fieldError.getField() : err.getObjectName();
            errors.put(field, err.getDefaultMessage());
        });
        return respon(HttpStatus.BAD_REQUEST, "VALIDASI_GAGAL", "Validasi input gagal", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> bodyTidakValid(HttpMessageNotReadableException e) {
        return respon(HttpStatus.BAD_REQUEST, "BODY_TIDAK_VALID", "Format body request tidak valid", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> mediaType(HttpMediaTypeNotSupportedException e) {
        return respon(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "MEDIA_TYPE", "Content-Type harus application/json", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> methodTidakDidukung(HttpRequestMethodNotSupportedException e) {
        return respon(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", e.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> endpointTidakAda(NoResourceFoundException e) {
        return respon(HttpStatus.NOT_FOUND, "NOT_FOUND", "Endpoint tidak ditemukan: /" + e.getResourcePath(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> umum(Exception e) {
        return respon(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal Server Error: " + e.getMessage(), null);
    }

    private static ResponseEntity<ApiResponse<Void>> respon(HttpStatus status, String code, String message, Object errors) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message, errors));
    }
}
