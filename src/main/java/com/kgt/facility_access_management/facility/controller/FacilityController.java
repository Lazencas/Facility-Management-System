package com.kgt.facility_access_management.facility.controller;

import com.kgt.facility_access_management.facility.domain.Facility;
import com.kgt.facility_access_management.facility.dto.FacilityCreateRequest;
import com.kgt.facility_access_management.facility.service.FacilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping("/facilities")
    public ResponseEntity<List<Facility>> findAll() {
        return ResponseEntity.ok(facilityService.findAll());
    }

    @GetMapping("/facilities/{facilityId}")
    public ResponseEntity<?> findById(@PathVariable Long facilityId) {

        Optional<Facility> facility = facilityService.findById(facilityId);

        if (facility.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(facility.get());
    }

    @PostMapping("/admin/facilities")
    public ResponseEntity<Facility> save(
            @RequestBody FacilityCreateRequest request) {

        Facility facility = new Facility();
        facility.setName(request.getName());
        facility.setLocation(request.getLocation());
        facility.setDescription(request.getDescription());

        Facility savedFacility = facilityService.save(facility);

        return ResponseEntity.ok(savedFacility);
    }

    @PostMapping("/admin/facilities/{facilityId}/deactivate")
    public ResponseEntity<String> deactivate(
            @PathVariable Long facilityId) {

        boolean result = facilityService.deactivate(facilityId);

        if (!result) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok("시설 비활성화 성공");
    }





}
