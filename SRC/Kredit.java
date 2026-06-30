package com.example.bankapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kredit")
public class Kredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kredit_id")
    private Long kreditId;

    @Column(name = "nomor_kontrak", length = 30, unique = true)
    private String nomorKontrak;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nomor_cif", referencedColumnName = "nomor_cif")
    private Nasabah nasabah;

    @Column(name = "jumlah_pinjaman", nullable = false)
    private Long jumlahPinjaman;

    @Column(name = "tenor", nullable = false)
    private Integer tenor;

    @Column(name = "bunga_tahunan", nullable = false)
    private Double bungaTahunan;

    @Column(name = "cicilan_per_bulan", nullable = false)
    private Long cicilanPerBulan;

    @Column(name = "status_aktif")
    private Boolean statusAktif = true;

    @Column(name = "status_pengajuan", length = 30)
    private String statusPengajuan = "MENUNGGU";

    @Column(name = "tanggal_diajukan")
    private LocalDateTime tanggalDiajukan;

    @Column(name = "tanggal_disetujui")
    private LocalDateTime tanggalDisetujui;

    @Column(name = "tanggal_ditolak")
    private LocalDateTime tanggalDitolak;

    @Column(name = "tanggal_dicairkan")
    private LocalDateTime tanggalDicairkan;

    @Column(name = "tanggal_berjalan")
    private LocalDateTime tanggalBerjalan;

    @Column(name = "tanggal_lunas")
    private LocalDateTime tanggalLunas;

    @Column(name = "diproses_oleh", length = 100)
    private String diprosesOleh;

    @Transient
    private String namaPemohonInput;

    public Long getKreditId() {
        return kreditId;
    }

    public void setKreditId(Long kreditId) {
        this.kreditId = kreditId;
    }

    public String getNomorKontrak() {
        return nomorKontrak;
    }

    public void setNomorKontrak(String nomorKontrak) {
        this.nomorKontrak = nomorKontrak;
    }

    public Nasabah getNasabah() {
        return nasabah;
    }

    public void setNasabah(Nasabah nasabah) {
        this.nasabah = nasabah;
    }

    public String getNamaPemohon() {
        return nasabah != null ? nasabah.getNamaNasabah() : namaPemohonInput;
    }

    public void setNamaPemohon(String namaPemohon) {
        this.namaPemohonInput = namaPemohon == null ? null : namaPemohon.trim().toUpperCase();
    }

    public Long getJumlahPinjaman() {
        return jumlahPinjaman;
    }

    public void setJumlahPinjaman(Long jumlahPinjaman) {
        this.jumlahPinjaman = jumlahPinjaman;
    }

    public Integer getTenor() {
        return tenor;
    }

    public void setTenor(Integer tenor) {
        this.tenor = tenor;
    }

    public Double getBungaTahunan() {
        return bungaTahunan;
    }

    public void setBungaTahunan(Double bungaTahunan) {
        this.bungaTahunan = bungaTahunan;
    }

    public Long getCicilanPerBulan() {
        return cicilanPerBulan;
    }

    public void setCicilanPerBulan(Long cicilanPerBulan) {
        this.cicilanPerBulan = cicilanPerBulan;
    }

    public Boolean getStatusAktif() {
        return statusAktif;
    }

    public void setStatusAktif(Boolean statusAktif) {
        this.statusAktif = statusAktif == null ? true : statusAktif;
    }

    public boolean isStatusAktif() {
        return Boolean.TRUE.equals(statusAktif);
    }

    public String getStatusPengajuan() {
        return statusPengajuan == null || statusPengajuan.trim().isEmpty()
                ? "MENUNGGU"
                : statusPengajuan;
    }

    public void setStatusPengajuan(String statusPengajuan) {
        this.statusPengajuan = statusPengajuan == null || statusPengajuan.trim().isEmpty()
                ? "MENUNGGU"
                : statusPengajuan.trim().toUpperCase();
    }

    public LocalDateTime getTanggalDiajukan() {
        return tanggalDiajukan;
    }

    public void setTanggalDiajukan(LocalDateTime tanggalDiajukan) {
        this.tanggalDiajukan = tanggalDiajukan;
    }

    public LocalDateTime getTanggalDisetujui() {
        return tanggalDisetujui;
    }

    public void setTanggalDisetujui(LocalDateTime tanggalDisetujui) {
        this.tanggalDisetujui = tanggalDisetujui;
    }

    public LocalDateTime getTanggalDitolak() {
        return tanggalDitolak;
    }

    public void setTanggalDitolak(LocalDateTime tanggalDitolak) {
        this.tanggalDitolak = tanggalDitolak;
    }

    public LocalDateTime getTanggalDicairkan() {
        return tanggalDicairkan;
    }

    public void setTanggalDicairkan(LocalDateTime tanggalDicairkan) {
        this.tanggalDicairkan = tanggalDicairkan;
    }

    public LocalDateTime getTanggalBerjalan() {
        return tanggalBerjalan;
    }

    public void setTanggalBerjalan(LocalDateTime tanggalBerjalan) {
        this.tanggalBerjalan = tanggalBerjalan;
    }

    public LocalDateTime getTanggalLunas() {
        return tanggalLunas;
    }

    public void setTanggalLunas(LocalDateTime tanggalLunas) {
        this.tanggalLunas = tanggalLunas;
    }

    public String getDiprosesOleh() {
        return diprosesOleh;
    }

    public void setDiprosesOleh(String diprosesOleh) {
        this.diprosesOleh = diprosesOleh;
    }
}