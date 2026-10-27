package com.example.orderingsystem.repository;
import com.example.orderingsystem.entity.Pelanggan;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PelangganRepository extends JpaRepository<Pelanggan, String>
{}