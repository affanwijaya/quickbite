package com.example.orderingsystem.repository;
import com.example.orderingsystem.entity.Pesanan;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PesananRepository extends JpaRepository<Pesanan, String> {}