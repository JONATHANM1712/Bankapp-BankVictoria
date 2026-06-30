package com.example.bankapp.model;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "nasabah")
public class Nasabah {

    @Id
    @Column(name = "nomor_cif", length = 9, nullable = false, unique = true)
    private String nomorCif;

    @Column(name = "nama_nasabah", nullable = false)
    private String namaNasabah;

    @Column(name = "nomor_ktp", length = 16, nullable = false, unique = true)
    private String nomorKtp;

    @Column(name = "alamat", nullable = false)
    private String alamat;

    @Column(name = "handphone", nullable = false)
    private String handphone;

    @Column(name = "nomor_rekening", length = 10, nullable = false, unique = true)
    private String nomorRekening;

    @Column(name = "tanggal_buka_rekening", nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate tanggalBukaRekening;

    @Column(name = "saldo", nullable = false)
    private BigDecimal saldo;

    @Column(name = "status_aktif")
    private Boolean statusAktif = true;

    @OneToMany(mappedBy = "nasabah", fetch = FetchType.LAZY)
    private List<Kredit> daftarKredit = new ArrayList<>();

    public String getNomorCif() { return nomorCif; }
    public void setNomorCif(String nomorCif) { this.nomorCif = nomorCif == null ? null : nomorCif.trim().toUpperCase(); }

    public String getNamaNasabah() { return namaNasabah; }
    public void setNamaNasabah(String namaNasabah) { this.namaNasabah = namaNasabah == null ? null : namaNasabah.trim().toUpperCase(); }

    public String getNomorKtp() { return nomorKtp; }
    public void setNomorKtp(String nomorKtp) { this.nomorKtp = nomorKtp == null ? null : nomorKtp.trim(); }

    public String getAlamat() { return alamat; }
    public void setAlamat(String alamat) { this.alamat = alamat == null ? null : alamat.trim(); }

    public String getHandphone() { return handphone; }
    public void setHandphone(String handphone) { this.handphone = handphone == null ? null : handphone.trim(); }

    public String getNomorRekening() { return nomorRekening; }
    public void setNomorRekening(String nomorRekening) { this.nomorRekening = nomorRekening == null ? null : nomorRekening.trim(); }

    public LocalDate getTanggalBukaRekening() { return tanggalBukaRekening; }
    public void setTanggalBukaRekening(LocalDate tanggalBukaRekening) { this.tanggalBukaRekening = tanggalBukaRekening; }

    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }

    public Boolean getStatusAktif() { return statusAktif; }
    public void setStatusAktif(Boolean statusAktif) { this.statusAktif = statusAktif == null ? true : statusAktif; }

    public boolean isStatusAktif() { return Boolean.TRUE.equals(statusAktif); }

    public List<Kredit> getDaftarKredit() { return daftarKredit; }
    public void setDaftarKredit(List<Kredit> daftarKredit) { this.daftarKredit = daftarKredit; }
}