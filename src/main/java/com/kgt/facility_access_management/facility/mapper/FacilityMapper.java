package com.kgt.facility_access_management.facility.mapper;

import com.kgt.facility_access_management.facility.domain.Facility;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FacilityMapper {
    //시설 전체 조회
    List<Facility> findAll();

    //시설 상세 조회
    Facility findById(Long id);

    //관리자 시설 등록
    void save(Facility facility);

    //물리 삭제 대신 active = false
    int deactivate(Long id);

}
