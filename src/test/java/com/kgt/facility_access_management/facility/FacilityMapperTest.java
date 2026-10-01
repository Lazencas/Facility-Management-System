package com.kgt.facility_access_management.facility;

import com.kgt.facility_access_management.facility.domain.Facility;
import com.kgt.facility_access_management.facility.mapper.FacilityMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FacilityMapperTest {

    @Autowired
    FacilityMapper facilityMapper;

    @Test
    void 시설입력하기() {
        String name = "westgate";

        Facility facility = new Facility();
        facility.setName(name);
        facility.setLocation("W");

        facilityMapper.save(facility);

        Facility savedFacility = facilityMapper.findById(facility.getId());

        assertThat(savedFacility).isNotNull();
        assertThat(savedFacility.getName()).isEqualTo(name);
        assertThat(savedFacility.getLocation()).isEqualTo("W");
    }

    @Test
    void 모든시설찾기() {
        // src/test/resources/data.sql의 독립된 테스트 데이터
        List<Facility> facilities = facilityMapper.findAll();
        assertThat(facilities).hasSize(3);
    }

    @Test
    void 시설한개찾기() {
        Long id = 1L;
        Facility facility = facilityMapper.findById(id);

        assertThat(facility.getId()).isEqualTo(id);

    }

    @Test
    void 시설삭제처리하기() {
        Long id = 1L;
        facilityMapper.deactivate(id);
        Facility facility = facilityMapper.findById(id);

        assertThat(facility.isActive()).isFalse();
    }


}
