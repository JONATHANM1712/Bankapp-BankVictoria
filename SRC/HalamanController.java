package com.example.bankapp.controller;

import com.example.bankapp.repository.AuditLogRepository;
import com.example.bankapp.repository.KreditRepository;
import com.example.bankapp.repository.NasabahRepository;
import com.example.bankapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.example.bankapp.repository.BackupLogRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class HalamanController {

    @Autowired
    private NasabahRepository nasabahRepository;

    @Autowired
    private KreditRepository kreditRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
private BackupLogRepository backupLogRepository;

    @GetMapping("/")
    public String dashboard(Model model) {
        isiStatistik(model);

        LocalDateTime awalBulan = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime akhirBulan = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay();

        model.addAttribute("totalOutstandingKredit", kreditRepository.totalOutstandingKredit());
        model.addAttribute("totalKreditDicairkan", kreditRepository.totalKreditDicairkan());
        model.addAttribute("totalKreditLunas", kreditRepository.totalKreditLunas());

        model.addAttribute(
                "pengajuanBulanIni",
                kreditRepository.countByStatusPengajuanAndTanggalDiajukanBetween(
                        "MENUNGGU",
                        awalBulan,
                        akhirBulan
                )
        );

        return "index";
    }

    @GetMapping("/laporan")
    public String laporan(Model model) {
        isiStatistik(model);
        return "laporan";
    }

    @GetMapping("/tentang")
    public String tentang() {
        return "tentang";
    }

    @GetMapping("/database")
    public String database(Model model) {
        long totalNasabah = nasabahRepository.count();
        long totalKredit = kreditRepository.count();
        long totalUser = userRepository.count();
        long totalAuditLog = auditLogRepository.count();
        long totalRecord = totalNasabah + totalKredit + totalUser + totalAuditLog;

        model.addAttribute("totalNasabah", totalNasabah);
        model.addAttribute("totalKredit", totalKredit);
        model.addAttribute("totalUser", totalUser);
        model.addAttribute("totalAuditLog", totalAuditLog);
        model.addAttribute("totalRecord", totalRecord);

        model.addAttribute("totalSaldo", nasabahRepository.totalSaldo());
        model.addAttribute("totalPinjaman", kreditRepository.totalPinjaman());

        model.addAttribute("statusDatabase", "TERHUBUNG");
        model.addAttribute("namaDatabase", getStringSafe("SELECT current_database()"));
        model.addAttribute("schemaDatabase", getStringSafe("SELECT current_schema()"));
        model.addAttribute("waktuServer", getStringSafe("SELECT TO_CHAR(NOW(), 'DD Mon YYYY HH24:MI:SS')"));
        model.addAttribute("versiPostgreSQL", getStringSafe("SELECT version()"));
        model.addAttribute("ukuranDatabase", getStringSafe("SELECT pg_size_pretty(pg_database_size(current_database()))"));

        model.addAttribute("daftarBackup", backupLogRepository.findTop50ByOrderByWaktuDesc());

        Integer jumlahTabel = getIntegerSafe("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                """);

        model.addAttribute("jumlahTabel", jumlahTabel);
        model.addAttribute("sizeNasabah", getTableSize("nasabah"));
        model.addAttribute("sizeKredit", getTableSize("kredit"));
        model.addAttribute("sizeUsers", getTableSize("users"));
        model.addAttribute("sizeAuditLog", getTableSize("audit_log"));

        model.addAttribute("statusRelasi", "Nasabah dan Kredit sudah terhubung melalui Nomor CIF");
        model.addAttribute("hostDatabase", getStringSafe("SELECT COALESCE(inet_server_addr()::text, 'localhost')"));
        model.addAttribute("portDatabase", getStringSafe("SELECT current_setting('port')"));

        model.addAttribute("ownerDatabase", getStringSafe("""
                SELECT pg_get_userbyid(datdba)
                FROM pg_database
                WHERE datname = current_database()
                """));

        model.addAttribute("encodingDatabase", getStringSafe("SELECT current_setting('server_encoding')"));
        model.addAttribute("timezoneDatabase", getStringSafe("SELECT current_setting('TimeZone')"));

        model.addAttribute("auditHariIni", getLongSafe("""
                SELECT COUNT(*)
                FROM audit_log
                WHERE waktu::date = CURRENT_DATE
                """));

        model.addAttribute("loginHariIni", getLongSafe("""
                SELECT COUNT(*)
                FROM audit_log
                WHERE aksi = 'LOGIN'
                  AND waktu::date = CURRENT_DATE
                """));

        model.addAttribute("importHariIni", getLongSafe("""
                SELECT COUNT(*)
                FROM audit_log
                WHERE aksi = 'IMPORT'
                  AND waktu::date = CURRENT_DATE
                """));

        model.addAttribute("aktivitasTerakhir", getStringSafe("""
                SELECT COALESCE(
                    (
                        SELECT TO_CHAR(waktu, 'DD Mon YYYY HH24:MI')
                               || ' - '
                               || aksi
                               || ' '
                               || modul
                               || ' oleh '
                               || username
                        FROM audit_log
                        ORDER BY waktu DESC
                        LIMIT 1
                    ),
                    'Belum ada aktivitas'
                )
                """));

        return "database";
    }

    @GetMapping("/laporan/nasabah")
    public String laporanNasabah(Model model) {
        model.addAttribute("nasabahList", nasabahRepository.findAll());
        return "laporan-nasabah";
    }

    @GetMapping("/laporan/kredit")
    public String laporanKredit(Model model) {
        model.addAttribute("kreditList", kreditRepository.findAll());
        return "laporan-kredit";
    }

    private void isiStatistik(Model model) {
        LocalDateTime awalHari = LocalDate.now().atStartOfDay();
        LocalDateTime akhirHari = LocalDate.now().plusDays(1).atStartOfDay();

        model.addAttribute("totalNasabah", nasabahRepository.count());
        model.addAttribute("totalKredit", kreditRepository.count());
        model.addAttribute("totalUser", userRepository.count());
        model.addAttribute("totalSaldo", nasabahRepository.totalSaldo());
        model.addAttribute("totalPinjaman", kreditRepository.totalPinjaman());

        model.addAttribute("menunggu", kreditRepository.countByStatusPengajuan("MENUNGGU"));
        model.addAttribute("disetujui", kreditRepository.countByStatusPengajuan("DISETUJUI"));
        model.addAttribute("ditolak", kreditRepository.countByStatusPengajuan("DITOLAK"));
        model.addAttribute("dicairkan", kreditRepository.countByStatusPengajuan("DICAIRKAN"));
        model.addAttribute("berjalan", kreditRepository.countByStatusPengajuan("BERJALAN"));
        model.addAttribute("lunas", kreditRepository.countByStatusPengajuan("LUNAS"));

        model.addAttribute("loginHariIni", auditLogRepository.countByAksiAndWaktuBetween("LOGIN", awalHari, akhirHari));
        model.addAttribute("inputNasabahHariIni", auditLogRepository.countByModulAndAksiAndWaktuBetween("NASABAH", "CREATE", awalHari, akhirHari));
        model.addAttribute("pengajuanKreditHariIni", auditLogRepository.countByModulAndAksiAndWaktuBetween("KREDIT", "PENGAJUAN_KREDIT", awalHari, akhirHari));
        model.addAttribute("approvalKreditHariIni", auditLogRepository.countByModulAndAksiAndWaktuBetween("KREDIT", "APPROVAL_KREDIT", awalHari, akhirHari));
        model.addAttribute("importExcelHariIni", auditLogRepository.countByAksiAndWaktuBetween("IMPORT", awalHari, akhirHari));
    }

    private String getStringSafe(String sql) {
        try {
            return jdbcTemplate.queryForObject(sql, String.class);
        } catch (Exception e) {
            return "-";
        }
    }

    private Integer getIntegerSafe(String sql) {
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getTableSize(String tableName) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT pg_size_pretty(pg_total_relation_size(to_regclass(?)))",
                    String.class,
                    "public." + tableName
            );
        } catch (Exception e) {
            return "-";
        }
    }

    private Long getLongSafe(String sql) {
        try {
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception e) {
            return 0L;
        }
    }
}