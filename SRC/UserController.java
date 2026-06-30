package com.example.bankapp.controller;

import com.example.bankapp.model.User;
import com.example.bankapp.repository.UserRepository;
import com.example.bankapp.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/users")
    public String daftarUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            Model model
    ) {
        if (page < 0) {
            page = 0;
        }

        if (size != 10 && size != 25 && size != 50 && size != 100) {
            size = 10;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> halamanUser;

        if (keyword == null || keyword.trim().isBlank()) {
            halamanUser = userRepository.findAll(pageable);
        } else {
            String key = keyword.trim();

            halamanUser = userRepository
                    .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrRoleContainingIgnoreCase(
                            key,
                            key,
                            key,
                            pageable
                    );
        }

        long totalItems = halamanUser.getTotalElements();
        long startItem = totalItems == 0 ? 0 : (long) halamanUser.getNumber() * halamanUser.getSize() + 1;
        long endItem = Math.min((long) (halamanUser.getNumber() + 1) * halamanUser.getSize(), totalItems);

        model.addAttribute("halamanUser", halamanUser);
        model.addAttribute("daftarUser", halamanUser.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", halamanUser.getNumber());
        model.addAttribute("totalPages", halamanUser.getTotalPages());
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("startItem", startItem);
        model.addAttribute("endItem", endItem);
        model.addAttribute("size", halamanUser.getSize());

        return "daftar-user";
    }

    @GetMapping("/users/edit/{id}")
    public String editUser(@PathVariable Integer id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan: " + id));

        model.addAttribute("user", user);
        return "edit-user";
    }

    @PostMapping("/users/update")
    public String updateUser(
            @RequestParam Integer id,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String role,
            @RequestParam(required = false) String oldPassword,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            HttpServletRequest request
    ) {
        User userLama = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan: " + id));

        User userUsername = userRepository.findByUsername(username);
        if (userUsername != null && !userUsername.getId().equals(id)) {
            model.addAttribute("user", userLama);
            model.addAttribute("error", "Username sudah digunakan user lain!");
            return "edit-user";
        }

        User userEmail = userRepository.findByEmail(email);
        if (userEmail != null && !userEmail.getId().equals(id)) {
            model.addAttribute("user", userLama);
            model.addAttribute("error", "Email sudah digunakan user lain!");
            return "edit-user";
        }

        userLama.setUsername(username);
        userLama.setEmail(email);
        userLama.setRole(role);

        if (password != null && !password.trim().isEmpty()) {
            if (oldPassword == null || oldPassword.trim().isEmpty()) {
                model.addAttribute("user", userLama);
                model.addAttribute("error", "Password lama wajib diisi untuk mengganti password!");
                return "edit-user";
            }

            if (!BCrypt.checkpw(oldPassword, userLama.getPassword())) {
                model.addAttribute("user", userLama);
                model.addAttribute("error", "Password lama salah!");
                return "edit-user";
            }

            if (confirmPassword == null || !password.equals(confirmPassword)) {
                model.addAttribute("user", userLama);
                model.addAttribute("error", "Password baru dan konfirmasi password tidak sama!");
                return "edit-user";
            }

            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
            userLama.setPassword(passwordHash);

            auditLogService.catat(
                    session,
                    request,
                    "GANTI_PASSWORD",
                    "USER",
                    "Password akun " + userLama.getUsername() + " diganti"
            );
        }

        userRepository.save(userLama);

        redirectAttributes.addFlashAttribute("success", "Data user berhasil diperbarui!");
        return "redirect:/users";
    }

    @GetMapping("/users/unlock/{id}")
    public String unlockUser(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            HttpServletRequest request
    ) {
        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan!");
            return "redirect:/users";
        }

        user.setLoginAttempt(0);
        user.setAccountLocked(false);
        user.setLockedAt(null);
        userRepository.save(user);

        auditLogService.catat(
                session,
                request,
                "UNLOCK_USER",
                "USER",
                "Admin membuka akun terkunci: " + user.getUsername()
        );

        redirectAttributes.addFlashAttribute("success", "Akun " + user.getUsername() + " berhasil dibuka kembali!");
        return "redirect:/users";
    }

    @GetMapping("/users/delete/{id}")
    public String hapusUser(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes
    ) {
        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "User berhasil dihapus!");
        return "redirect:/users";
    }

    @GetMapping("/users/tambah")
    public String tambahUserPage() {
        return "tambah-user";
    }

    @PostMapping("/users/tambah")
    public String simpanUserAdmin(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String role,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Password dan konfirmasi password tidak sama!");
            return "tambah-user";
        }

        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error", "Username sudah digunakan!");
            return "tambah-user";
        }

        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "Email sudah digunakan!");
            return "tambah-user";
        }

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordHash);
        user.setRole(role);
        user.setLoginAttempt(0);
        user.setAccountLocked(false);
        user.setLockedAt(null);

        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "User baru berhasil ditambahkan!");
        return "redirect:/users";
    }
}