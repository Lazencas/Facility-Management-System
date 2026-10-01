package com.kgt.facility_access_management.access.service;

import com.kgt.facility_access_management.access.domain.AccessRequest;
import com.kgt.facility_access_management.access.domain.AccessResult;
import com.kgt.facility_access_management.access.mapper.AccessRequestMapper;
import com.kgt.facility_access_management.facility.domain.Facility;
import com.kgt.facility_access_management.facility.service.FacilityService;
import com.kgt.facility_access_management.user.domain.User;
import com.kgt.facility_access_management.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AccessService {

    private static final Logger log =
            LoggerFactory.getLogger(AccessService.class);

    private final UserMapper userMapper;
    private final FacilityService facilityService;
    private final AccessRequestMapper accessRequestMapper;

    public AccessService(
            UserMapper userMapper,
            FacilityService facilityService,
            AccessRequestMapper accessRequestMapper
    ) {
        this.userMapper = userMapper;
        this.facilityService = facilityService;
        this.accessRequestMapper = accessRequestMapper;
    }

    public AccessResult validateAccess(Long userId, Long facilityId) {

        /*
        사용자가 접근하기 위해서는

        1. 해당 사용자가 활성 상태
        2. 요청하는 시설이 활성 상태
        3. 현재 시점에 유효한 APPROVED 접근 요청을 가지고 있음

        모든 조건을 만족했을 때만
        AccessResult.ALLOWED 반환
         */

        log.debug("시설 접근 판정 시작 - userId={}, facilityId={}",userId,facilityId);

        // 사용자 존재 및 활성 여부
        User user = userMapper.findById(userId);

        if (user == null) {
            log.debug("시설 접근 거부 - 사용자를 찾을 수 없음, userId={}",userId);
            return AccessResult.DENIED;
        }

        if (!user.isActive()) {
            log.debug("시설 접근 거부 - 비활성 사용자, userId={}",userId);
            return AccessResult.DENIED;
        }

        // 시설 존재 및 활성 여부
        Facility facility = facilityService.findById(facilityId)
                .orElse(null);

        if (facility == null) {
            log.debug("시설 접근 거부 - 시설을 찾을 수 없음, facilityId={}",facilityId);
            return AccessResult.DENIED;
        }

        if (!facility.isActive()) {
            log.debug("시설 접근 거부 - 비활성 시설, facilityId={}",facilityId);
            return AccessResult.DENIED;
        }

        LocalDateTime now = LocalDateTime.now();

        // 현재 시점에 유효한 승인 신청 조회
        AccessRequest approvedRequest =
                accessRequestMapper.findValidApprovedRequest(
                        userId,
                        facilityId,
                        now
                );

        if (approvedRequest == null) {
            log.debug("시설 접근 거부 - 유효한 승인 신청 없음, userId={}, facilityId={}",userId,facilityId);
            return AccessResult.DENIED;
        }

        log.debug("시설 접근 허용 - userId={}, facilityId={}, accessRequestId={}",userId,facilityId,approvedRequest.getId());
        return AccessResult.ALLOWED;
    }
}