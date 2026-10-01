package com.kgt.facility_access_management.access.mapper;

import com.kgt.facility_access_management.access.domain.AccessLog;
import com.kgt.facility_access_management.access.dto.AccessLogSearchConditionDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AccessLogMapper {
    int save(AccessLog accessLog);

    List<AccessLog> search(AccessLogSearchConditionDTO condition);
}
