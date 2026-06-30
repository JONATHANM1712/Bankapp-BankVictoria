package com.example.bankapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime waktu;

    private String username;
    private String role;
    private String modul;
    private String aksi;

    @Column(columnDefinition = "TEXT")
    private String keterangan;

    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    private String dataId;
    private String statusSebelum;
    private String statusSesudah;

    public AuditLog() {
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getWaktu() {
        return waktu;
    }

    public void setWaktu(LocalDateTime waktu) {
        this.waktu = waktu;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getModul() {
        return modul;
    }

    public void setModul(String modul) {
        this.modul = modul;
    }

    public String getAksi() {
        return aksi;
    }

    public void setAksi(String aksi) {
        this.aksi = aksi;
    }

    public String getKeterangan() {
        return keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getStatusSebelum() {
        return statusSebelum;
    }

    public void setStatusSebelum(String statusSebelum) {
        this.statusSebelum = statusSebelum;
    }

    public String getStatusSesudah() {
        return statusSesudah;
    }

    public void setStatusSesudah(String statusSesudah) {
        this.statusSesudah = statusSesudah;
    }
}