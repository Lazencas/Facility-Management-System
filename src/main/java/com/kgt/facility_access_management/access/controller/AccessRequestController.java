package com.kgt.facility_access_management.access.controller;

import com.kgt.facility_access_management.access.domain.AccessRequest;
import com.kgt.facility_access_management.access.dto.AccessRequestCreateForm;
import com.kgt.facility_access_management.access.service.AccessRequestService;
import com.kgt.facility_access_management.user.domain.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/access-requests")
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    public AccessRequestController(AccessRequestService accessRequestService) {
        this.accessRequestService = accessRequestService;
    }

    @PostMapping
    public ResponseEntity<AccessRequest> save(
            @RequestBody AccessRequestCreateForm form,
            HttpSession session) {

        User loginUser =
                (User) session.getAttribute("LOGIN_USER");

        AccessRequest accessRequest =
                accessRequestService.createRequest(
                        loginUser.getId(),
                        form
                );

        return ResponseEntity.ok(accessRequest);
    }

}
