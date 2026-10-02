package com.kgt.facility_access_management.facility.service;

import com.kgt.facility_access_management.facility.domain.Facility;
import com.kgt.facility_access_management.facility.mapper.FacilityMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FacilityService {

    private final FacilityMapper facilityMapper;

    public FacilityService(FacilityMapper facilityMapper) {
        this.facilityMapper = facilityMapper;
    }

    public List<Facility> findAll() {
        return facilityMapper.findAll();
    }

    public Optional<Facility> findById(Long id) {
        return Optional.ofNullable(facilityMapper.findById(id));
    }

    public Facility save(Facility facility) {
        facilityMapper.save(facility);
        return facility;
    }

    public boolean deactivate(Long id) {
        int updatedRows = facilityMapper.deactivate(id);
        return updatedRows == 1;
    }

    public Optional<Facility> update(Long id, Facility facility) {

        facility.setId(id);

        int result = facilityMapper.update(facility);

        if (result == 0) {
            return Optional.empty();
        }

        return Optional.ofNullable(facilityMapper.findById(id));
    }
}