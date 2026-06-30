package com.example.bankapp.controller;

import com.example.bankapp.model.BackupLog;
import com.example.bankapp.repository.BackupLogRepository;
import com.example.bankapp.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class BackupController {

    private static final String BACKUP_FOLDER = "backup-database";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BackupLogRepository backupLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/backup-database")
    public ResponseEntity<InputStreamResource> backupDatabase(
            HttpSession session,
            HttpServletRequest request
    ) throws IOException {

        Files.createDirectories(Paths.get(BACKUP_FOLDER));

        String waktu = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String namaFile = "backup-bankapp-" + waktu + ".sql";
        Path pathFile = Paths.get(BACKUP_FOLDER, namaFile);

        StringBuilder sql = new StringBuilder();

        sql.append("-- BACKUP DATABASE BANKAPP\n");
        sql.append("-- Dibuat pada: ").append(LocalDateTime.now()).append("\n");
        sql.append("-- Restore dilakukan manual melalui pgAdmin atau psql.\n\n");

        backupTable(sql, "nasabah");
        backupTable(sql, "kredit");
        backupTable(sql, "users");
        backupTable(sql, "audit_log");
        backupTable(sql, "backup_log");

        Files.writeString(pathFile, sql.toString(), StandardCharsets.UTF_8);

        simpanLog(session, namaFile, "BERHASIL", "Backup database berhasil dibuat.");

        auditLogService.catat(
                session,
                request,
                "BACKUP_DATABASE",
                "DATABASE",
                "Backup database dibuat: " + namaFile
        );

        InputStreamResource resource = new InputStreamResource(new FileInputStream(pathFile.toFile()));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + namaFile)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(pathFile.toFile().length())
                .body(resource);
    }

    private void backupTable(StringBuilder sql, String tableName) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + tableName);

            sql.append("-- =============================\n");
            sql.append("-- TABLE: ").append(tableName).append("\n");
            sql.append("-- =============================\n\n");

            if (rows.isEmpty()) {
                sql.append("-- Tidak ada data pada tabel ").append(tableName).append("\n\n");
                return;
            }

            for (Map<String, Object> row : rows) {
                List<String> columns = new ArrayList<>(row.keySet());

                sql.append("INSERT INTO ").append(tableName).append(" (");

                for (int i = 0; i < columns.size(); i++) {
                    sql.append(columns.get(i));
                    if (i < columns.size() - 1) {
                        sql.append(", ");
                    }
                }

                sql.append(") VALUES (");

                for (int i = 0; i < columns.size(); i++) {
                    sql.append(formatSqlValue(row.get(columns.get(i))));
                    if (i < columns.size() - 1) {
                        sql.append(", ");
                    }
                }

                sql.append(");\n");
            }

            sql.append("\n");

        } catch (Exception e) {
            sql.append("-- Gagal backup tabel ")
                    .append(tableName)
                    .append(": ")
                    .append(e.getMessage())
                    .append("\n\n");
        }
    }

    private String formatSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }

        if (value instanceof Number) {
            return value.toString();
        }

        if (value instanceof Boolean) {
            return ((Boolean) value) ? "true" : "false";
        }

        if (value instanceof Timestamp) {
            return "'" + value.toString().replace("'", "''") + "'";
        }

        String text = value.toString().replace("'", "''");
        return "'" + text + "'";
    }

    private void simpanLog(
            HttpSession session,
            String namaFile,
            String status,
            String keterangan
    ) {
        Object username = session.getAttribute("username");

        BackupLog log = new BackupLog();
        log.setWaktu(LocalDateTime.now());
        log.setUsername(username == null ? "UNKNOWN" : username.toString());
        log.setNamaFile(namaFile);
        log.setStatus(status);
        log.setKeterangan(keterangan);

        backupLogRepository.save(log);
    }
}