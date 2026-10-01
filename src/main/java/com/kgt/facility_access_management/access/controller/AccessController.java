package com.kgt.facility_access_management.access.controller;

import com.kgt.facility_access_management.access.domain.AccessResult;
import com.kgt.facility_access_management.access.service.AccessService;
import com.kgt.facility_access_management.user.domain.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/facilities")
public class AccessController {

    private final AccessService accessService;

    public AccessController(AccessService accessService) {
        this.accessService = accessService;
    }

    @PostMapping("/{facilityId}/access")
    public ResponseEntity<AccessResult> access(
            @PathVariable Long facilityId,
            HttpSession session
    ) {

        User loginUser = (User) session.getAttribute("LOGIN_USER");

        AccessResult result =
                accessService.validateAccess(
                        loginUser.getId(),
                        facilityId
                );

        return ResponseEntity.ok(result);
    }
}