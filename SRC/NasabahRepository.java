package com.example.bankapp.repository;

import com.example.bankapp.model.Nasabah;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface NasabahRepository extends JpaRepository<Nasabah, String> {

    boolean existsByNomorKtp(String nomorKtp);
    boolean existsByNomorRekening(String nomorRekening);

    Nasabah findByNomorKtp(String nomorKtp);
    Nasabah findByNomorRekening(String nomorRekening);

    @Query("""
            SELECT n FROM Nasabah n
            WHERE n.statusAktif = true OR n.statusAktif IS NULL
            ORDER BY n.namaNasabah ASC
            """)
    List<Nasabah> findDataAktifOrderByNama();

    @Query("""
            SELECT n FROM Nasabah n
            WHERE (:statusAktif = true AND (n.statusAktif = true OR n.statusAktif IS NULL))
               OR (:statusAktif = false AND n.statusAktif = false)
            """)
    Page<Nasabah> findByStatusSoft(
            @Param("statusAktif") Boolean statusAktif,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(n) FROM Nasabah n
            WHERE (:statusAktif = true AND (n.statusAktif = true OR n.statusAktif IS NULL))
               OR (:statusAktif = false AND n.statusAktif = false)
            """)
    long countByStatusSoft(@Param("statusAktif") Boolean statusAktif);

    @Query("""
            SELECT COALESCE(SUM(n.saldo), 0)
            FROM Nasabah n
            WHERE n.statusAktif = true OR n.statusAktif IS NULL
            """)
    BigDecimal totalSaldo();

    @Query("""
            SELECT n FROM Nasabah n
            WHERE (
                    (:statusAktif = true AND (n.statusAktif = true OR n.statusAktif IS NULL))
                 OR (:statusAktif = false AND n.statusAktif = false)
            )
              AND (
                    LOWER(n.nomorCif) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(n.namaNasabah) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(n.nomorKtp) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(n.handphone) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(n.nomorRekening) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(n.alamat) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<Nasabah> searchNasabahByStatus(
            @Param("keyword") String keyword,
            @Param("statusAktif") Boolean statusAktif,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT nomor_cif
                    FROM nasabah
                    WHERE nomor_cif LIKE 'CIF%'
                    ORDER BY nomor_cif DESC
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    String cariCifTerakhir();
}