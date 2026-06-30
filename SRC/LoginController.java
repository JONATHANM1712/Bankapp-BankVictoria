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
import java.time.LocalDateTime;
import java.util.Random;

@Controller
public class LoginController {

    private static final int MAKSIMAL_LOGIN_GAGAL = 3;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String timeout,
            HttpSession session,
            Model model
    ) {
        if ("true".equals(timeout)) {
            model.addAttribute("error", "Session Anda telah berakhir. Silakan login kembali.");
        }

        tampilkanCaptcha(session, model);
        return "login";
    }

    @PostMapping("/login")
    public String prosesLogin(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String captcha,
            HttpSession session,
            HttpServletRequest request,
            Model model
    ) {
        String captchaJawaban = (String) session.getAttribute("captchaJawaban");

        if (captchaJawaban == null || !captchaJawaban.equals(captcha.trim())) {
            model.addAttribute("error", "Captcha salah.");
            model.addAttribute("usernameValue", username);
            buatCaptchaBaru(session, model);
            return "login";
        }

        User user = userRepository.findByUsername(username);

        if (user == null) {
            user = userRepository.findByEmail(username);
        }

        if (user == null) {
            model.addAttribute("error", "Username / Email atau Password salah!");
            model.addAttribute("usernameValue", username);
            buatCaptchaBaru(session, model);
            return "login";
        }

        if (user.isAccountLocked()) {
            auditLogService.catat(
                    session,
                    request,
                    "LOGIN_DITOLAK",
                    "USER",
                    "Akun terkunci mencoba login: " + user.getUsername()
            );

            model.addAttribute("error", "Akun Anda terkunci. Hubungi Admin untuk membuka akun.");
            model.addAttribute("usernameValue", username);
            buatCaptchaBaru(session, model);
            return "login";
        }

        if (BCrypt.checkpw(password, user.getPassword())) {
            user.setLoginAttempt(0);
            user.setAccountLocked(false);
            user.setLockedAt(null);
            userRepository.save(user);

            session.removeAttribute("captchaSoal");
            session.removeAttribute("captchaJawaban");

            session.setAttribute("userLogin", user);
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());

            auditLogService.catat(
                    session,
                    request,
                    "LOGIN",
                    "USER",
                    user.getUsername() + " berhasil login"
            );

            return "redirect:/";
        }

        int jumlahGagal = user.getLoginAttempt() + 1;
        user.setLoginAttempt(jumlahGagal);

        auditLogService.catat(
                session,
                request,
                "LOGIN_GAGAL",
                "USER",
                "Login gagal untuk akun: " + user.getUsername() + " percobaan ke-" + jumlahGagal
        );

        if (jumlahGagal >= MAKSIMAL_LOGIN_GAGAL) {
            user.setAccountLocked(true);
            user.setLockedAt(LocalDateTime.now());
            userRepository.save(user);

            auditLogService.catat(
                    session,
                    request,
                    "AKUN_TERKUNCI",
                    "USER",
                    "Akun " + user.getUsername() + " terkunci karena 3 kali salah password"
            );

            model.addAttribute("error", "Password salah 3 kali. Akun dikunci dan harus dibuka oleh Admin.");
        } else {
            userRepository.save(user);

            model.addAttribute(
                    "error",
                    "Password salah! Percobaan " + jumlahGagal + " dari " + MAKSIMAL_LOGIN_GAGAL + "."
            );
        }

        model.addAttribute("usernameValue", username);
        buatCaptchaBaru(session, model);
        return "login";
    }

    @GetMapping("/registrasi")
    public String registrasiPage() {
        return "registrasi";
    }

    @PostMapping("/registrasi")
    public String prosesRegistrasi(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model
    ) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Password dan konfirmasi password tidak sama!");
            return "registrasi";
        }

        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error", "Username sudah digunakan!");
            return "registrasi";
        }

        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "Email sudah digunakan!");
            return "registrasi";
        }

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordHash);
        user.setRole("USER");
        user.setLoginAttempt(0);
        user.setAccountLocked(false);
        user.setLockedAt(null);

        userRepository.save(user);

        model.addAttribute("success", "Registrasi berhasil. Silakan login.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletRequest request) {
        auditLogService.catat(
                session,
                request,
                "LOGOUT",
                "USER",
                session.getAttribute("username") + " logout dari sistem"
        );

        session.invalidate();
        return "redirect:/login";
    }

    private void tampilkanCaptcha(HttpSession session, Model model) {
        if (session.getAttribute("captchaSoal") == null) {
            buatCaptchaBaru(session, model);
        } else {
            model.addAttribute("captchaSoal", session.getAttribute("captchaSoal"));
        }
    }

    private void buatCaptchaBaru(HttpSession session, Model model) {
        Random random = new Random();

        int angka1 = random.nextInt(9) + 1;
        int angka2 = random.nextInt(9) + 1;

        String soal = angka1 + " + " + angka2 + " = ?";
        String jawaban = String.valueOf(angka1 + angka2);

        session.setAttribute("captchaSoal", soal);
        session.setAttribute("captchaJawaban", jawaban);

        model.addAttribute("captchaSoal", soal);
    }
}