package com.example.bankapp.repository;

import com.example.bankapp.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUsernameContainingIgnoreCaseOrRoleContainingIgnoreCaseOrModulContainingIgnoreCaseOrAksiContainingIgnoreCaseOrKeteranganContainingIgnoreCaseOrIpAddressContainingIgnoreCase(
            String username,
            String role,
            String modul,
            String aksi,
            String keterangan,
            String ipAddress,
            Pageable pageable
    );

    long countByAksiAndWaktuBetween(String aksi, LocalDateTime awal, LocalDateTime akhir);

    long countByModulAndAksiAndWaktuBetween(String modul, String aksi, LocalDateTime awal, LocalDateTime akhir);
}