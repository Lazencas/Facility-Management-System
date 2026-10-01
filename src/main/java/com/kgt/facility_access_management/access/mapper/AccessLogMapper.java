package com.kgt.facility_access_management.access.mapper;

import com.kgt.facility_access_management.access.domain.AccessLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccessLogMapper {
    int save(AccessLog accessLog);
}
