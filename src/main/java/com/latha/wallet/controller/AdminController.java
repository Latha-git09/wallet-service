package com.latha.wallet.controller;

import com.latha.wallet.entity.AccountStatus;
import com.latha.wallet.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Only users with role ADMIN can reach /api/admin/** (see SecurityConfig)
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PatchMapping("/users/{id}/freeze")
    public Map<String, String> freeze(@PathVariable Long id) {
        adminService.setStatus(id, AccountStatus.FROZEN);
        return Map.of("message", "User " + id + " frozen");
    }

    @PatchMapping("/users/{id}/unfreeze")
    public Map<String, String> unfreeze(@PathVariable Long id) {
        adminService.setStatus(id, AccountStatus.ACTIVE);
        return Map.of("message", "User " + id + " unfrozen");
    }
}
