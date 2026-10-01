package com.kgt.facility_access_management.access.controller;

import com.kgt.facility_access_management.access.domain.AccessRequest;
import com.kgt.facility_access_management.access.dto.AccessRequestCreateForm;
import com.kgt.facility_access_management.access.service.AccessRequestService;
import com.kgt.facility_access_management.user.domain.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{accessRequestId}/cancel")
    public ResponseEntity<String> cancel(
            @PathVariable Long accessRequestId,
            HttpSession session
    ) {
        //세션에서 로그인 사용자 꺼내기
        User loginUser = (User) session.getAttribute("LOGIN_USER");
        //처리
       accessRequestService.cancelRequest(accessRequestId, loginUser.getId());
        //응답
        return ResponseEntity.ok("취소 성공");

    }

}
