package com.kgt.facility_access_management.user.mapper;

import com.kgt.facility_access_management.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findByLoginId(@Param("loginId") String loginId);
    User findById(Long id);
}
