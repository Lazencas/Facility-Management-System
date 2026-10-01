package com.kgt.facility_access_management.access.controller;

import com.kgt.facility_access_management.access.domain.AccessLog;
import com.kgt.facility_access_management.access.dto.AccessLogSearchConditionDTO;
import com.kgt.facility_access_management.access.service.AccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/access-logs")
public class AccessLogController {

    private final AccessService accessService;

    public AccessLogController(AccessService accessService) {
        this.accessService = accessService;
    }

    @GetMapping
    public ResponseEntity<List<AccessLog>> search(
            @ModelAttribute AccessLogSearchConditionDTO condition
    ) {

        List<AccessLog> accessLogs =
                accessService.searchAccessLogs(condition);

        return ResponseEntity.ok(accessLogs);
    }
}