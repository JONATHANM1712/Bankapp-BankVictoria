package com.example.bankapp.service;

import com.example.bankapp.model.AuditLog;
import com.example.bankapp.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void catat(
            HttpSession session,
            HttpServletRequest request,
            String aksi,
            String modul,
            String keterangan
    ) {
        catatLengkap(session, request, aksi, modul, keterangan, null, null, null);
    }

    public void catatLengkap(
            HttpSession session,
            HttpServletRequest request,
            String aksi,
            String modul,
            String keterangan,
            String dataId,
            String statusSebelum,
            String statusSesudah
    ) {
        AuditLog log = new AuditLog();

        Object username = session.getAttribute("username");
        Object role = session.getAttribute("role");

        log.setWaktu(LocalDateTime.now());
        log.setUsername(username == null ? "UNKNOWN" : username.toString());
        log.setRole(role == null ? "UNKNOWN" : role.toString());
        log.setAksi(aksi);
        log.setModul(modul);
        log.setKeterangan(keterangan);
        log.setDataId(dataId);
        log.setStatusSebelum(statusSebelum);
        log.setStatusSesudah(statusSesudah);

        if (request != null) {
            log.setIpAddress(ambilIpAddress(request));
            log.setUserAgent(request.getHeader("User-Agent"));
        }

        auditLogRepository.save(log);
    }

    private String ambilIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0];
        }

        return request.getRemoteAddr();
    }
}