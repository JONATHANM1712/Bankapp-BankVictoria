package com.example.bankapp.repository;

import com.example.bankapp.model.BackupLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupLogRepository extends JpaRepository<BackupLog, Long> {

    List<BackupLog> findTop50ByOrderByWaktuDesc();
}