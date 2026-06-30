package com.example.bankapp.controller;

import com.example.bankapp.model.User;
import com.example.bankapp.repository.UserRepository;
import com.example.bankapp.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProfilController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/ganti-password")
    public String halamanGantiPassword() {
        return "ganti-password";
    }

    @PostMapping("/ganti-password")
    public String prosesGantiPassword(
            @RequestParam String passwordLama,
            @RequestParam String passwordBaru,
            @RequestParam String konfirmasiPassword,
            HttpSession session,
            HttpServletRequest request,
            Model model
    ) {
        User userLogin = (User) session.getAttribute("userLogin");

        if (userLogin == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userLogin.getId())
                .orElse(null);

        if (user == null) {
            session.invalidate();
            return "redirect:/login";
        }

        if (!BCrypt.checkpw(passwordLama, user.getPassword())) {
            model.addAttribute("error", "Password lama salah!");
            return "ganti-password";
        }

        if (passwordBaru.length() < 6) {
            model.addAttribute("error", "Password baru minimal 6 karakter!");
            return "ganti-password";
        }

        if (!passwordBaru.equals(konfirmasiPassword)) {
            model.addAttribute("error", "Password baru dan konfirmasi password tidak sama!");
            return "ganti-password";
        }

        String passwordHash = BCrypt.hashpw(passwordBaru, BCrypt.gensalt());
        user.setPassword(passwordHash);

        userRepository.save(user);

        session.setAttribute("userLogin", user);
        session.setAttribute("username", user.getUsername());

        auditLogService.catat(
                session,
                request,
                "UPDATE",
                "USER",
                "User mengganti password sendiri: " + user.getUsername()
        );

        model.addAttribute("success", "Password berhasil diganti!");
        return "ganti-password";
    }
}