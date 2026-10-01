package com.kgt.facility_access_management.access.mapper;

import com.kgt.facility_access_management.access.domain.AccessRequest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AccessRequestMapper {

    int save(AccessRequest accessRequest);

    AccessRequest findById(Long id);

    List<AccessRequest> findByUserId(Long userId);

}
