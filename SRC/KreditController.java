package com.example.bankapp.controller;

import com.example.bankapp.model.Kredit;
import com.example.bankapp.model.Nasabah;
import com.example.bankapp.repository.KreditRepository;
import com.example.bankapp.repository.NasabahRepository;
import com.example.bankapp.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class KreditController {

    @Autowired
    private KreditRepository kreditRepository;

    @Autowired
    private NasabahRepository nasabahRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/simulasi")
    public String simulasiKredit(Model model) {
        model.addAttribute("kredit", new Kredit());
        model.addAttribute("daftarNasabah", nasabahRepository.findDataAktifOrderByNama());
        model.addAttribute("modeEdit", false);
        return "simulasi";
    }

    @PostMapping("/simpan-kredit")
    public String simpanKredit(
            @ModelAttribute Kredit kredit,
            @RequestParam String nomorCif,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            HttpServletRequest request
    ) {
        try {
            boolean dataBaru = kredit.getKreditId() == null;

            Nasabah nasabah = nasabahRepository.findById(nomorCif).orElse(null);

            if (nasabah == null) {
                redirectAttributes.addFlashAttribute("error", "Nasabah tidak ditemukan!");
                return "redirect:/simulasi";
            }

            if (kredit.getJumlahPinjaman() == null || kredit.getJumlahPinjaman() <= 0) {
                redirectAttributes.addFlashAttribute("error", "Jumlah pinjaman harus lebih dari 0!");
                return "redirect:/simulasi";
            }

            if (kredit.getTenor() == null || kredit.getTenor() <= 0) {
                redirectAttributes.addFlashAttribute("error", "Tenor harus lebih dari 0 bulan!");
                return "redirect:/simulasi";
            }

            if (kredit.getBungaTahunan() == null || kredit.getBungaTahunan() < 0) {
                redirectAttributes.addFlashAttribute("error", "Bunga tahunan tidak boleh minus!");
                return "redirect:/simulasi";
            }

            if (!dataBaru) {
                Kredit lama = kreditRepository.findById(kredit.getKreditId()).orElse(null);

                if (lama == null) {
                    redirectAttributes.addFlashAttribute("error", "Data kredit tidak ditemukan!");
                    return "redirect:/daftar-kredit";
                }

                kredit.setNomorKontrak(lama.getNomorKontrak());
                kredit.setStatusAktif(lama.getStatusAktif());
                kredit.setStatusPengajuan(lama.getStatusPengajuan());
                kredit.setTanggalDiajukan(lama.getTanggalDiajukan());
                kredit.setTanggalDisetujui(lama.getTanggalDisetujui());
                kredit.setTanggalDitolak(lama.getTanggalDitolak());
                kredit.setTanggalDicairkan(lama.getTanggalDicairkan());
                kredit.setTanggalBerjalan(lama.getTanggalBerjalan());
                kredit.setTanggalLunas(lama.getTanggalLunas());
                kredit.setDiprosesOleh(lama.getDiprosesOleh());
            } else {
                kredit.setNomorKontrak(buatNomorKontrak());
                kredit.setStatusAktif(true);
                kredit.setStatusPengajuan("MENUNGGU");
                kredit.setTanggalDiajukan(LocalDateTime.now());
                kredit.setDiprosesOleh(getUsername(session));
            }

            kredit.setNasabah(nasabah);

            double pokokBulanan = kredit.getJumlahPinjaman() / (double) kredit.getTenor();
            double bungaBulanan = kredit.getJumlahPinjaman() * (kredit.getBungaTahunan() / 100.0) / 12.0;

            kredit.setCicilanPerBulan(Math.round(pokokBulanan + bungaBulanan));
            kreditRepository.save(kredit);

            auditLogService.catat(
                    session,
                    request,
                    dataBaru ? "PENGAJUAN_KREDIT" : "UPDATE",
                    "KREDIT",
                    (dataBaru ? "Pengajuan kredit baru " : "Update kredit ") + kredit.getNomorKontrak()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    dataBaru
                            ? "Data kredit berhasil disimpan dan menunggu persetujuan!"
                            : "Data kredit berhasil diperbarui!"
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal menyimpan data kredit! " + e.getMessage());
        }

        return "redirect:/daftar-kredit";
    }

    @GetMapping("/daftar-kredit")
    public String daftarKredit(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "aktif") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "kreditId") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Model model
    ) {
        boolean statusAktif = !status.equalsIgnoreCase("terhapus");

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Kredit> halamanKredit = (keyword == null || keyword.trim().isEmpty())
                ? kreditRepository.findByStatusSoft(statusAktif, pageable)
                : kreditRepository.searchKreditByStatus(keyword.trim(), statusAktif, pageable);

        int startItem = halamanKredit.getTotalElements() == 0
                ? 0
                : halamanKredit.getNumber() * halamanKredit.getSize() + 1;

        int endItem = Math.min(
                startItem + halamanKredit.getNumberOfElements() - 1,
                (int) halamanKredit.getTotalElements()
        );

        model.addAttribute("daftarKredit", halamanKredit.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        model.addAttribute("currentPage", halamanKredit.getNumber());
        model.addAttribute("totalPages", halamanKredit.getTotalPages());
        model.addAttribute("startItem", startItem);
        model.addAttribute("endItem", endItem);
        model.addAttribute("totalItems", halamanKredit.getTotalElements());

        model.addAttribute("menunggu", kreditRepository.countByStatusPengajuan("MENUNGGU"));
        model.addAttribute("disetujui", kreditRepository.countByStatusPengajuan("DISETUJUI"));
        model.addAttribute("ditolak", kreditRepository.countByStatusPengajuan("DITOLAK"));
        model.addAttribute("dicairkan", kreditRepository.countByStatusPengajuan("DICAIRKAN"));
        model.addAttribute("berjalan", kreditRepository.countByStatusPengajuan("BERJALAN"));
        model.addAttribute("lunas", kreditRepository.countByStatusPengajuan("LUNAS"));

        return "daftar-kredit";
    }

    @GetMapping("/edit-kredit/{id}")
    public String editKredit(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Kredit kredit = kreditRepository.findById(id).orElse(null);

        if (kredit == null) {
            redirectAttributes.addFlashAttribute("error", "Data kredit tidak ditemukan!");
            return "redirect:/daftar-kredit";
        }

        model.addAttribute("kredit", kredit);
        model.addAttribute("daftarNasabah", nasabahRepository.findDataAktifOrderByNama());
        model.addAttribute("modeEdit", true);

        return "simulasi";
    }

    @GetMapping("/setujui-kredit/{id}")
    public String setujuiKredit(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            HttpServletRequest request
    ) {
        return ubahStatusPengajuan(
                id,
                "MENUNGGU",
                "DISETUJUI",
                "Kredit berhasil disetujui!",
                "APPROVAL_KREDIT",
                redirectAttributes,
                session,
                request
        );
    }

    @GetMapping("/tolak-kredit/{id}")
    public String tolakKredit(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            HttpServletRequest request
    ) {
        return ubahStatusPengajuan(
                id,
                "MENUNGGU",
                "DITOLAK",
                "Kredit berhasil ditolak!",
                "PENOLAKAN_KREDIT",
                redirectAttributes,
                session,
                request
        );
    }

    @GetMapping("/cairkan-kredit/{id}")
    public String cairkanKredit(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            HttpServletRequest request
    ) {
        return ubahStatusPengajuan(
                id,
                "DISETUJUI",
                "DICAIRKAN",
                "Kredit berhasil dicairkan!",
                "PENCAIRAN_KREDIT",
                redirectAttributes,
                session,
                request
        );
    }

    @GetMapping("/berjalan-kredit/{id}")
    public String berjalanKredit(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            HttpServletRequest request
    ) {
        return ubahStatusPengajuan(
                id,
                "DICAIRKAN",
                "BERJALAN",
                "Kredit berhasil ditandai berjalan!",
                "KREDIT_BERJALAN",
                redirectAttributes,
                session,
                request
        );
    }

    @GetMapping("/lunaskan-kredit/{id}")
    public String lunaskanKredit(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            HttpServletRequest request
    ) {
        return ubahStatusPengajuan(
                id,
                "BERJALAN",
                "LUNAS",
                "Kredit berhasil ditandai lunas!",
                "PELUNASAN_KREDIT",
                redirectAttributes,
                session,
                request
        );
    }

    private String ubahStatusPengajuan(
            Long id,
            String statusWajib,
            String statusBaru,
            String pesan,
            String aksiAudit,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            HttpServletRequest request
    ) {
        Kredit kredit = kreditRepository.findById(id).orElse(null);

        if (kredit == null) {
            redirectAttributes.addFlashAttribute("error", "Data kredit tidak ditemukan!");
            return "redirect:/daftar-kredit";
        }

        if (Boolean.FALSE.equals(kredit.getStatusAktif())) {
            redirectAttributes.addFlashAttribute("error", "Data kredit berada di Recycle Bin dan tidak bisa diproses!");
            return "redirect:/daftar-kredit";
        }

        if (!kredit.getStatusPengajuan().equals(statusWajib)) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Status tidak valid. Kredit dengan status " + kredit.getStatusPengajuan()
                            + " tidak bisa langsung menjadi " + statusBaru + "."
            );

            return "redirect:/daftar-kredit";
        }

        kredit.setStatusPengajuan(statusBaru);
        kredit.setDiprosesOleh(getUsername(session));

        LocalDateTime sekarang = LocalDateTime.now();

        if (statusBaru.equals("DISETUJUI")) {
            kredit.setTanggalDisetujui(sekarang);
        }

        if (statusBaru.equals("DITOLAK")) {
            kredit.setTanggalDitolak(sekarang);
        }

        if (statusBaru.equals("DICAIRKAN")) {
            kredit.setTanggalDicairkan(sekarang);
        }

        if (statusBaru.equals("BERJALAN")) {
            kredit.setTanggalBerjalan(sekarang);
        }

        if (statusBaru.equals("LUNAS")) {
            kredit.setTanggalLunas(sekarang);
        }

        kreditRepository.save(kredit);

        auditLogService.catat(
                session,
                request,
                aksiAudit,
                "KREDIT",
                aksiAudit + " nomor kontrak " + kredit.getNomorKontrak()
        );

        redirectAttributes.addFlashAttribute("success", pesan);
        return "redirect:/daftar-kredit";
    }

    @GetMapping("/cetak-persetujuan-kredit/{id}")
    public String cetakPersetujuan(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Kredit kredit = kreditRepository.findById(id).orElse(null);

        if (kredit == null) {
            redirectAttributes.addFlashAttribute("error", "Data kredit tidak ditemukan!");
            return "redirect:/daftar-kredit";
        }

        model.addAttribute("kredit", kredit);
        return "cetak-persetujuan-kredit";
    }

    @GetMapping("/hapus-kredit/{id}")
    public String hapusKredit(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        Kredit kredit = kreditRepository.findById(id).orElse(null);

        if (kredit != null) {
            kredit.setStatusAktif(false);
            kreditRepository.save(kredit);
            redirectAttributes.addFlashAttribute("success", "Data kredit dipindahkan ke Recycle Bin!");
        }

        return "redirect:/daftar-kredit";
    }

    @GetMapping("/restore-kredit/{id}")
    public String restoreKredit(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        Kredit kredit = kreditRepository.findById(id).orElse(null);

        if (kredit != null) {
            kredit.setStatusAktif(true);
            kreditRepository.save(kredit);
            redirectAttributes.addFlashAttribute("success", "Data kredit berhasil dipulihkan!");
        }

        return "redirect:/daftar-kredit?status=terhapus";
    }

    @PostMapping("/import-kredit")
    public String importKredit(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            HttpServletRequest request
    ) {
        int berhasil = 0;
        int gagal = 0;

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                try {
                    Row row = sheet.getRow(i);

                    if (row == null) {
                        gagal++;
                        continue;
                    }

                    String nomorCif = formatter.formatCellValue(row.getCell(0)).trim().toUpperCase();

                    Nasabah nasabah = nasabahRepository.findById(nomorCif).orElse(null);

                    if (nasabah == null) {
                        gagal++;
                        continue;
                    }

                    Long jumlahPinjaman = Long.parseLong(
                            formatter.formatCellValue(row.getCell(1))
                                    .replace(".", "")
                                    .replace(",", "")
                                    .trim()
                    );

                    Integer tenor = Integer.parseInt(
                            formatter.formatCellValue(row.getCell(2)).trim()
                    );

                    Double bungaTahunan = Double.parseDouble(
                            formatter.formatCellValue(row.getCell(3))
                                    .replace(",", ".")
                                    .trim()
                    );

                    if (jumlahPinjaman <= 0 || tenor <= 0 || bungaTahunan < 0) {
                        gagal++;
                        continue;
                    }

                    Kredit kredit = new Kredit();

                    kredit.setNasabah(nasabah);
                    kredit.setJumlahPinjaman(jumlahPinjaman);
                    kredit.setTenor(tenor);
                    kredit.setBungaTahunan(bungaTahunan);

                    kredit.setCicilanPerBulan(
                            Math.round(
                                    jumlahPinjaman / (double) tenor
                                            + jumlahPinjaman * (bungaTahunan / 100.0) / 12.0
                            )
                    );

                    kredit.setStatusAktif(true);
                    kredit.setStatusPengajuan("MENUNGGU");
                    kredit.setTanggalDiajukan(LocalDateTime.now());
                    kredit.setDiprosesOleh(getUsername(session));
                    kredit.setNomorKontrak(buatNomorKontrak());

                    kreditRepository.save(kredit);
                    berhasil++;
                } catch (Exception barisError) {
                    gagal++;
                }
            }

            workbook.close();

            auditLogService.catat(
                    session,
                    request,
                    "IMPORT",
                    "KREDIT",
                    "Import kredit berhasil " + berhasil + " data, gagal " + gagal + " data"
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Import selesai. Berhasil: " + berhasil + " data, gagal: " + gagal + " data."
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Import gagal! Periksa format Excel kredit.");
        }

        return "redirect:/daftar-kredit";
    }

    private String buatNomorKontrak() {
        String tanggal = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        long urutan = kreditRepository.countByTanggalDiajukanBetween(
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay()
        ) + 1;

        return "KRD-" + tanggal + "-" + String.format("%04d", urutan);
    }

    private String getUsername(HttpSession session) {
        Object username = session.getAttribute("username");
        return username == null ? "UNKNOWN" : username.toString();
    }
}