package com.example.bankapp.controller;

import com.example.bankapp.model.Kredit;
import com.example.bankapp.model.Nasabah;
import com.example.bankapp.repository.KreditRepository;
import com.example.bankapp.repository.NasabahRepository;

import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class NasabahController {

    @Autowired
    private NasabahRepository nasabahRepository;

    @Autowired
    private KreditRepository kreditRepository;

@GetMapping("/input")
public String inputNasabah(Model model) {
    model.addAttribute("nasabah", new Nasabah());
    model.addAttribute("modeEdit", false);

    return "input";
}

    @PostMapping("/simpan")
    public String simpanNasabah(
            @ModelAttribute Nasabah nasabah,
            RedirectAttributes redirectAttributes
    ) {
        try {

            nasabah.setNomorCif(nasabah.getNomorCif().toUpperCase());
            nasabah.setStatusAktif(true);

            if (nasabahRepository.existsById(nasabah.getNomorCif())) {
                redirectAttributes.addFlashAttribute("error", "Nomor CIF sudah terdaftar!");
                return "redirect:/input";
            }

            if (nasabahRepository.existsByNomorKtp(nasabah.getNomorKtp())) {
                redirectAttributes.addFlashAttribute("error", "Nomor KTP sudah terdaftar!");
                return "redirect:/input";
            }

            if (nasabahRepository.existsByNomorRekening(nasabah.getNomorRekening())) {
                redirectAttributes.addFlashAttribute("error", "Nomor rekening sudah terdaftar!");
                return "redirect:/input";
            }

            if (nasabah.getSaldo() == null || nasabah.getSaldo().compareTo(BigDecimal.ZERO) < 0) {
                redirectAttributes.addFlashAttribute("error", "Saldo tidak boleh kosong atau negatif!");
                return "redirect:/input";
            }

            nasabahRepository.save(nasabah);
            redirectAttributes.addFlashAttribute("success", "Data nasabah berhasil disimpan!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal menyimpan data nasabah!");
        }

        return "redirect:/daftar";
    }

    @GetMapping("/daftar")
    public String daftarNasabah(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "aktif") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "namaNasabah") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            Model model
    ) {
        boolean statusAktif = !status.equalsIgnoreCase("terhapus");

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Nasabah> halamanNasabah;

        if (keyword == null || keyword.trim().isEmpty()) {
            halamanNasabah = nasabahRepository.findByStatusSoft(statusAktif, pageable);
        } else {
            halamanNasabah = nasabahRepository.searchNasabahByStatus(
                    keyword.trim(),
                    statusAktif,
                    pageable
            );
        }

        int startItem = halamanNasabah.getNumber() * halamanNasabah.getSize() + 1;
        int endItem = Math.min(
                startItem + halamanNasabah.getNumberOfElements() - 1,
                (int) halamanNasabah.getTotalElements()
        );

        if (halamanNasabah.getTotalElements() == 0) {
            startItem = 0;
            endItem = 0;
        }

        model.addAttribute("daftarNasabah", halamanNasabah.getContent());

        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);

        model.addAttribute("currentPage", halamanNasabah.getNumber());
        model.addAttribute("totalPages", halamanNasabah.getTotalPages());
        model.addAttribute("startItem", startItem);
        model.addAttribute("endItem", endItem);
        model.addAttribute("totalItems", halamanNasabah.getTotalElements());

        model.addAttribute("breadcrumb", "Daftar Nasabah");

        return "daftar";
    }

    @GetMapping("/edit/{nomorCif}")
    public String editNasabah(
            @PathVariable String nomorCif,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Nasabah nasabah = nasabahRepository.findById(nomorCif).orElse(null);

        if (nasabah == null) {
            redirectAttributes.addFlashAttribute("error", "Data nasabah tidak ditemukan!");
            return "redirect:/daftar";
        }

        model.addAttribute("nasabah", nasabah);
        model.addAttribute("modeEdit", true);
        model.addAttribute("breadcrumb", "Edit Nasabah");

        return "input";
    }

    @PostMapping("/update/{oldCif}")
    public String updateNasabah(
            @PathVariable String oldCif,
            @ModelAttribute Nasabah nasabah,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Nasabah dataLama = nasabahRepository.findById(oldCif).orElse(null);

            if (dataLama == null) {
                redirectAttributes.addFlashAttribute("error", "Data nasabah tidak ditemukan!");
                return "redirect:/daftar";
            }

            if (!nasabah.getNomorKtp().equals(dataLama.getNomorKtp())
                    && nasabahRepository.existsByNomorKtp(nasabah.getNomorKtp())) {
                redirectAttributes.addFlashAttribute("error", "Nomor KTP sudah digunakan nasabah lain!");
                return "redirect:/edit/" + oldCif;
            }

            if (!nasabah.getNomorRekening().equals(dataLama.getNomorRekening())
                    && nasabahRepository.existsByNomorRekening(nasabah.getNomorRekening())) {
                redirectAttributes.addFlashAttribute("error", "Nomor rekening sudah digunakan nasabah lain!");
                return "redirect:/edit/" + oldCif;
            }

            nasabah.setNomorCif(oldCif);
            nasabah.setStatusAktif(dataLama.getStatusAktif());

            if (nasabah.getSaldo() == null || nasabah.getSaldo().compareTo(BigDecimal.ZERO) < 0) {
                redirectAttributes.addFlashAttribute("error", "Saldo tidak boleh kosong atau negatif!");
                return "redirect:/edit/" + oldCif;
            }

            nasabahRepository.save(nasabah);
            redirectAttributes.addFlashAttribute("success", "Data nasabah berhasil diupdate!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal update data nasabah!");
        }

        return "redirect:/daftar";
    }

    @GetMapping("/hapus/{nomorCif}")
    public String hapusNasabah(
            @PathVariable String nomorCif,
            RedirectAttributes redirectAttributes
    ) {
        Nasabah nasabah = nasabahRepository.findById(nomorCif).orElse(null);

        if (nasabah != null) {
            nasabah.setStatusAktif(false);
            nasabahRepository.save(nasabah);
            redirectAttributes.addFlashAttribute("success", "Data nasabah dipindahkan ke Recycle Bin!");
        }

        return "redirect:/daftar";
    }

    @GetMapping("/restore/{nomorCif}")
    public String restoreNasabah(
            @PathVariable String nomorCif,
            RedirectAttributes redirectAttributes
    ) {
        Nasabah nasabah = nasabahRepository.findById(nomorCif).orElse(null);

        if (nasabah != null) {
            nasabah.setStatusAktif(true);
            nasabahRepository.save(nasabah);
            redirectAttributes.addFlashAttribute("success", "Data nasabah berhasil dipulihkan!");
        }

        return "redirect:/daftar?status=terhapus";
    }

    @GetMapping("/cetak-nasabah/{nomorCif}")
    public String cetakNasabah(
            @PathVariable String nomorCif,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Nasabah nasabah = nasabahRepository.findById(nomorCif).orElse(null);

        if (nasabah == null) {
            redirectAttributes.addFlashAttribute("error", "Data nasabah tidak ditemukan!");
            return "redirect:/daftar";
        }

        List<Kredit> daftarKredit = kreditRepository.findByNasabahNomorCif(nomorCif)
                .stream()
                .filter(kredit -> kredit.getStatusAktif() == null || kredit.getStatusAktif())
                .toList();

        Long totalPinjaman = daftarKredit.stream()
                .mapToLong(kredit -> kredit.getJumlahPinjaman() == null ? 0L : kredit.getJumlahPinjaman())
                .sum();

        model.addAttribute("nasabah", nasabah);
        model.addAttribute("daftarKredit", daftarKredit);
        model.addAttribute("totalPinjaman", totalPinjaman);

        return "cetak-nasabah";
    }

   @PostMapping("/import-nasabah")
public String importNasabah(
        @RequestParam("file") MultipartFile file,
        RedirectAttributes redirectAttributes
) {
    try (InputStream inputStream = file.getInputStream()) {

        Workbook workbook = WorkbookFactory.create(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        DataFormatter formatter = new DataFormatter();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            String nomorCif = formatter.formatCellValue(row.getCell(0))
                    .trim()
                    .toUpperCase();

            if (nomorCif.isEmpty()) {
                continue;
            }

            if (nasabahRepository.existsById(nomorCif)) {
                continue;
            }

            Nasabah nasabah = new Nasabah();

            nasabah.setNomorCif(nomorCif);
            nasabah.setNamaNasabah(formatter.formatCellValue(row.getCell(1)));
            nasabah.setNomorKtp(formatter.formatCellValue(row.getCell(2)));
            nasabah.setAlamat(formatter.formatCellValue(row.getCell(3)));
            nasabah.setHandphone(formatter.formatCellValue(row.getCell(4)));
            nasabah.setNomorRekening(formatter.formatCellValue(row.getCell(5)));

            Cell tanggalCell = row.getCell(6);

            if (tanggalCell != null && DateUtil.isCellDateFormatted(tanggalCell)) {
                nasabah.setTanggalBukaRekening(
                        tanggalCell.getLocalDateTimeCellValue().toLocalDate()
                );
            } else {
                String tanggalText = formatter.formatCellValue(tanggalCell);
                nasabah.setTanggalBukaRekening(LocalDate.parse(tanggalText));
            }

            String saldoText = formatter.formatCellValue(row.getCell(7))
                    .replace(".", "")
                    .replace(",", "")
                    .trim();

            nasabah.setSaldo(new BigDecimal(saldoText));
            nasabah.setStatusAktif(true);

            nasabahRepository.save(nasabah);
        }

        workbook.close();
        redirectAttributes.addFlashAttribute("success", "Import data nasabah berhasil!");

    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Import gagal! Periksa format Excel.");
    }

    return "redirect:/daftar";
}

}