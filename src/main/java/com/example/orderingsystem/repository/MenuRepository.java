package com.example.orderingsystem.repository;
import com.example.orderingsystem.entity.Menu;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface MenuRepository extends JpaRepository<Menu, String> {
    // Mengunci baris menu (SELECT ... FOR UPDATE) agar dua checkout bersamaan tidak memotong stok yang sama
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Menu m WHERE m.idMenu = :idMenu")
    Optional<Menu> findByIdForUpdate(@Param("idMenu") String idMenu);
}
