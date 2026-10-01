package com.kgt.facility_access_management.access.mapper;

import com.kgt.facility_access_management.access.domain.AccessRequest;
import com.kgt.facility_access_management.access.domain.AccessRequestStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccessRequestMapper {

    int save(AccessRequest accessRequest);

    AccessRequest findById(Long id);

    List<AccessRequest> findByUserId(Long userId);

    int updateStatus(@Param("id") Long id, @Param("userId") Long userId,
                     @Param("currentStatus") AccessRequestStatus currentStatus,
                     @Param("newStatus") AccessRequestStatus newStatus
    );

    int approve(@Param("id") Long id, @Param("reviewerId") Long reviewerId,
                @Param("currentStatus") AccessRequestStatus currentStatus,
                @Param("newStatus") AccessRequestStatus newStatus
    );

    int reject(
            @Param("id") Long id,
            @Param("reviewerId") Long reviewerId,
            @Param("rejectReason") String rejectReason,
            @Param("currentStatus") AccessRequestStatus currentStatus,
            @Param("newStatus") AccessRequestStatus newStatus
    );

}
