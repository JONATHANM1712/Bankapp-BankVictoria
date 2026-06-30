package com.example.bankapp.controller;

import com.example.bankapp.model.Kredit;
import com.example.bankapp.model.Nasabah;
import com.example.bankapp.repository.KreditRepository;
import com.example.bankapp.repository.NasabahRepository;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.List;

@Controller
public class ExportController {

    @Autowired
    private NasabahRepository nasabahRepository;

    @Autowired
    private KreditRepository kreditRepository;

    @GetMapping("/export/nasabah/excel")
    public void exportNasabahExcel(HttpServletResponse response) throws IOException {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=laporan-nasabah.xlsx");

        List<Nasabah> dataNasabah = nasabahRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data Nasabah");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("No");
        header.createCell(1).setCellValue("Nomor CIF");
        header.createCell(2).setCellValue("Nama Nasabah");
        header.createCell(3).setCellValue("Nomor KTP");
        header.createCell(4).setCellValue("Alamat");
        header.createCell(5).setCellValue("Handphone");
        header.createCell(6).setCellValue("Nomor Rekening");
        header.createCell(7).setCellValue("Tanggal Buka Rekening");
        header.createCell(8).setCellValue("Saldo");

        int rowNum = 1;
        int no = 1;

        for (Nasabah n : dataNasabah) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(no++);
            row.createCell(1).setCellValue(text(n.getNomorCif()));
            row.createCell(2).setCellValue(text(n.getNamaNasabah()));
            row.createCell(3).setCellValue(text(n.getNomorKtp()));
            row.createCell(4).setCellValue(text(n.getAlamat()));
            row.createCell(5).setCellValue(text(n.getHandphone()));
            row.createCell(6).setCellValue(text(n.getNomorRekening()));
            row.createCell(7).setCellValue(text(n.getTanggalBukaRekening()));
            row.createCell(8).setCellValue(text(n.getSaldo()));
        }

        for (int i = 0; i <= 8; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @GetMapping("/export/kredit/excel")
    public void exportKreditExcel(HttpServletResponse response) throws IOException {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=laporan-kredit.xlsx");

        List<Kredit> dataKredit = kreditRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data Kredit");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("No");
        header.createCell(1).setCellValue("Nomor CIF");
        header.createCell(2).setCellValue("Nama Nasabah");
        header.createCell(3).setCellValue("Nomor Rekening");
        header.createCell(4).setCellValue("Jumlah Pinjaman");
        header.createCell(5).setCellValue("Tenor");
        header.createCell(6).setCellValue("Bunga Tahunan");
        header.createCell(7).setCellValue("Cicilan per Bulan");

        int rowNum = 1;
        int no = 1;

        for (Kredit k : dataKredit) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(no++);
            row.createCell(1).setCellValue(k.getNasabah() == null ? "-" : text(k.getNasabah().getNomorCif()));
            row.createCell(2).setCellValue(k.getNasabah() == null ? "-" : text(k.getNasabah().getNamaNasabah()));
            row.createCell(3).setCellValue(k.getNasabah() == null ? "-" : text(k.getNasabah().getNomorRekening()));
            row.createCell(4).setCellValue(k.getJumlahPinjaman() == null ? 0 : k.getJumlahPinjaman());
            row.createCell(5).setCellValue(k.getTenor() == null ? 0 : k.getTenor());
            row.createCell(6).setCellValue(k.getBungaTahunan() == null ? 0.0 : k.getBungaTahunan());
            row.createCell(7).setCellValue(k.getCicilanPerBulan() == null ? 0 : k.getCicilanPerBulan());
        }

        for (int i = 0; i <= 7; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @GetMapping("/export/nasabah/pdf")
    public void exportNasabahPdf(HttpServletResponse response) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=laporan-nasabah.pdf");

        List<Nasabah> dataNasabah = nasabahRepository.findAll();

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        Paragraph title = new Paragraph(
                "LAPORAN DATA NASABAH",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)
        );
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);

        table.addCell("No");
        table.addCell("CIF");
        table.addCell("Nama");
        table.addCell("KTP");
        table.addCell("Alamat");
        table.addCell("HP");
        table.addCell("Rekening");
        table.addCell("Saldo");

        int no = 1;

        for (Nasabah n : dataNasabah) {
            table.addCell(String.valueOf(no++));
            table.addCell(text(n.getNomorCif()));
            table.addCell(text(n.getNamaNasabah()));
            table.addCell(text(n.getNomorKtp()));
            table.addCell(text(n.getAlamat()));
            table.addCell(text(n.getHandphone()));
            table.addCell(text(n.getNomorRekening()));
            table.addCell(text(n.getSaldo()));
        }

        document.add(table);
        document.close();
    }

    @GetMapping("/export/kredit/pdf")
    public void exportKreditPdf(HttpServletResponse response) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=laporan-kredit.pdf");

        List<Kredit> dataKredit = kreditRepository.findAll();

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        Paragraph title = new Paragraph(
                "LAPORAN DATA KREDIT",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)
        );
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);

        table.addCell("No");
        table.addCell("CIF");
        table.addCell("Nama");
        table.addCell("Rekening");
        table.addCell("Pinjaman");
        table.addCell("Tenor");
        table.addCell("Bunga");
        table.addCell("Cicilan");

        int no = 1;

        for (Kredit k : dataKredit) {
            table.addCell(String.valueOf(no++));
            table.addCell(k.getNasabah() == null ? "-" : text(k.getNasabah().getNomorCif()));
            table.addCell(k.getNasabah() == null ? "-" : text(k.getNasabah().getNamaNasabah()));
            table.addCell(k.getNasabah() == null ? "-" : text(k.getNasabah().getNomorRekening()));
            table.addCell(text(k.getJumlahPinjaman()));
            table.addCell(text(k.getTenor()) + " bulan");
            table.addCell(text(k.getBungaTahunan()) + "%");
            table.addCell(text(k.getCicilanPerBulan()));
        }

        document.add(table);
        document.close();
    }

    private String text(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
