package com.example.bankapp.repository;

import com.example.bankapp.model.Kredit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KreditRepository extends JpaRepository<Kredit, Long> {

    List<Kredit> findByNasabahNomorCif(String nomorCif);

    boolean existsByNasabahNomorCif(String nomorCif);

    boolean existsByNasabahNomorCifAndStatusAktifTrue(String nomorCif);

    long countByNasabahNomorCif(String nomorCif);

    long countByTanggalDiajukanBetween(LocalDateTime awal, LocalDateTime akhir);

    @Query("""
            SELECT k FROM Kredit k
            JOIN k.nasabah n
            WHERE k.statusAktif = true OR k.statusAktif IS NULL
            ORDER BY k.kreditId DESC
            """)
    List<Kredit> findDataAktifOrderByKreditIdDesc();

    @Query("""
            SELECT k FROM Kredit k
            JOIN k.nasabah n
            WHERE (:statusAktif = true AND (k.statusAktif = true OR k.statusAktif IS NULL))
               OR (:statusAktif = false AND k.statusAktif = false)
            """)
    Page<Kredit> findByStatusSoft(
            @Param("statusAktif") Boolean statusAktif,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(k) FROM Kredit k
            WHERE (:statusAktif = true AND (k.statusAktif = true OR k.statusAktif IS NULL))
               OR (:statusAktif = false AND k.statusAktif = false)
            """)
    long countByStatusSoft(@Param("statusAktif") Boolean statusAktif);

    @Query("""
            SELECT k FROM Kredit k
            JOIN k.nasabah n
            WHERE (
                    (:statusAktif = true AND (k.statusAktif = true OR k.statusAktif IS NULL))
                 OR (:statusAktif = false AND k.statusAktif = false)
            )
              AND (
                    LOWER(n.nomorCif) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(n.namaNasabah) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(n.nomorKtp) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(n.nomorRekening) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(k.statusPengajuan) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(k.nomorKontrak) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR CAST(k.jumlahPinjaman AS string) LIKE CONCAT('%', :keyword, '%')
                 OR CAST(k.tenor AS string) LIKE CONCAT('%', :keyword, '%')
                 OR CAST(k.bungaTahunan AS string) LIKE CONCAT('%', :keyword, '%')
                 OR CAST(k.cicilanPerBulan AS string) LIKE CONCAT('%', :keyword, '%')
              )
            """)
    Page<Kredit> searchKreditByStatus(
            @Param("keyword") String keyword,
            @Param("statusAktif") Boolean statusAktif,
            Pageable pageable
    );

    @Transactional
    void deleteByNasabahNomorCif(String nomorCif);

    @Query("""
            SELECT COALESCE(SUM(k.jumlahPinjaman), 0)
            FROM Kredit k
            WHERE k.statusAktif = true OR k.statusAktif IS NULL
            """)
    Long totalPinjaman();

    long countByStatusPengajuan(String statusPengajuan);

long countByStatusPengajuanAndTanggalDiajukanBetween(
        String statusPengajuan,
        LocalDateTime awal,
        LocalDateTime akhir
);

@Query("""
        SELECT COALESCE(SUM(k.jumlahPinjaman), 0)
        FROM Kredit k
        WHERE k.statusPengajuan IN ('DICAIRKAN', 'BERJALAN')
        """)
Long totalOutstandingKredit();

@Query("""
        SELECT COALESCE(SUM(k.jumlahPinjaman), 0)
        FROM Kredit k
        WHERE k.statusPengajuan = 'DICAIRKAN'
        """)
Long totalKreditDicairkan();

@Query("""
        SELECT COALESCE(SUM(k.jumlahPinjaman), 0)
        FROM Kredit k
        WHERE k.statusPengajuan = 'LUNAS'
        """)
Long totalKreditLunas();
}