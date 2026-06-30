package com.example.bankapp.controller;

import com.example.bankapp.model.AuditLog;
import com.example.bankapp.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuditLogController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping("/audit-log")
    public String auditLog(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        if (page < 0) page = 0;
        if (size != 10 && size != 25 && size != 50 && size != 100) size = 10;

        Pageable pageable = PageRequest.of(page, size, Sort.by("waktu").descending());

        Page<AuditLog> halamanAudit;

        if (keyword == null || keyword.trim().isEmpty()) {
            halamanAudit = auditLogRepository.findAll(pageable);
        } else {
            String key = keyword.trim();

            halamanAudit =
                    auditLogRepository.findByUsernameContainingIgnoreCaseOrRoleContainingIgnoreCaseOrModulContainingIgnoreCaseOrAksiContainingIgnoreCaseOrKeteranganContainingIgnoreCaseOrIpAddressContainingIgnoreCase(
                            key, key, key, key, key, key, pageable
                    );
        }

        long totalItems = halamanAudit.getTotalElements();
        long startItem = totalItems == 0 ? 0 : halamanAudit.getNumber() * halamanAudit.getSize() + 1;
        long endItem = Math.min((halamanAudit.getNumber() + 1L) * halamanAudit.getSize(), totalItems);

        model.addAttribute("halamanAudit", halamanAudit);
        model.addAttribute("daftarAudit", halamanAudit.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", halamanAudit.getNumber());
        model.addAttribute("totalPages", halamanAudit.getTotalPages());
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("startItem", startItem);
        model.addAttribute("endItem", endItem);
        model.addAttribute("size", halamanAudit.getSize());

        return "audit-log";
    }
}