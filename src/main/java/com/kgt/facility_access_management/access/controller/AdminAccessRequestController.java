package com.kgt.facility_access_management.access.controller;

import com.kgt.facility_access_management.access.service.AccessRequestService;
import com.kgt.facility_access_management.user.domain.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/access-requests")
public class AdminAccessRequestController {

    private final AccessRequestService accessRequestService;

    public AdminAccessRequestController(
            AccessRequestService accessRequestService
    ) {
        this.accessRequestService = accessRequestService;
    }

    @PostMapping("/{accessRequestId}/approve")
    public ResponseEntity<String> approve(
            @PathVariable Long accessRequestId,
            HttpSession session
    ) {
        User loginUser =
                (User) session.getAttribute("LOGIN_USER");

        accessRequestService.approveRequest(
                accessRequestId,
                loginUser.getId()
        );

        return ResponseEntity.ok("승인 성공");
    }
}