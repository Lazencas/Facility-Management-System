package com.kgt.facility_access_management.access.service;

import com.kgt.facility_access_management.access.domain.AccessDecisionReason;
import com.kgt.facility_access_management.access.domain.AccessLog;
import com.kgt.facility_access_management.access.domain.AccessRequest;
import com.kgt.facility_access_management.access.domain.AccessResult;
import com.kgt.facility_access_management.access.mapper.AccessLogMapper;
import com.kgt.facility_access_management.access.mapper.AccessRequestMapper;
import com.kgt.facility_access_management.common.exception.BusinessException;
import com.kgt.facility_access_management.facility.domain.Facility;
import com.kgt.facility_access_management.facility.service.FacilityService;
import com.kgt.facility_access_management.user.domain.User;
import com.kgt.facility_access_management.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AccessService {

    private static final Logger log =
            LoggerFactory.getLogger(AccessService.class);

    private final UserMapper userMapper;
    private final FacilityService facilityService;
    private final AccessRequestMapper accessRequestMapper;
    private final AccessLogMapper accessLogMapper;

    public AccessService(
            UserMapper userMapper,
            FacilityService facilityService,
            AccessRequestMapper accessRequestMapper,
            AccessLogMapper accessLogMapper
    ) {
        this.userMapper = userMapper;
        this.facilityService = facilityService;
        this.accessRequestMapper = accessRequestMapper;
        this.accessLogMapper = accessLogMapper;
    }

    @Transactional
    public AccessResult validateAccess(Long userId, Long facilityId) {

        /*
        사용자가 접근하기 위해서는

        1. 해당 사용자가 활성 상태
        2. 요청하는 시설이 활성 상태
        3. APPROVED 상태의 접근 요청을 가지고 있음
        4. 현재 시점이 접근 요청의 유효기간 안에 있음

        모든 조건을 만족했을 때만
        AccessResult.ALLOWED 반환

        접근 결과는 성공/실패 모두 AccessLog에 저장
         */

        log.debug(
                "시설 접근 판정 시작 - userId={}, facilityId={}",
                userId,
                facilityId
        );

        LocalDateTime now = LocalDateTime.now();

        // 사용자 존재 여부
        User user = userMapper.findById(userId);

        if (user == null) {
            log.debug(
                    "시설 접근 실패 - 사용자를 찾을 수 없음, userId={}",
                    userId
            );

            throw new BusinessException(
                    "사용자를 찾을 수 없습니다."
            );
        }

        // 시설 존재 여부
        Facility facility = facilityService.findById(facilityId)
                .orElseThrow(() ->
                        new BusinessException(
                                "시설을 찾을 수 없습니다."
                        )
                );

        /*
        여기부터는 user와 facility가 모두 실제 DB에 존재하므로
        access_logs의 FK 조건을 만족할 수 있음
         */

        // 사용자 활성 여부
        if (!user.isActive()) {

            log.debug(
                    "시설 접근 거부 - 비활성 사용자, userId={}",
                    userId
            );

            saveAccessLog(
                    userId,
                    facilityId,
                    null,
                    AccessResult.DENIED,
                    AccessDecisionReason.USER_INACTIVE,
                    now
            );

            return AccessResult.DENIED;
        }

        // 시설 활성 여부
        if (!facility.isActive()) {

            log.debug(
                    "시설 접근 거부 - 비활성 시설, facilityId={}",
                    facilityId
            );

            saveAccessLog(
                    userId,
                    facilityId,
                    null,
                    AccessResult.DENIED,
                    AccessDecisionReason.FACILITY_INACTIVE,
                    now
            );

            return AccessResult.DENIED;
        }

        // APPROVED 상태의 접근 신청 조회
        AccessRequest approvedRequest =
                accessRequestMapper.findApprovedRequest(
                        userId,
                        facilityId
                );

        // 승인 신청 자체가 없는 경우
        if (approvedRequest == null) {

            log.debug(
                    "시설 접근 거부 - 승인 신청 없음, userId={}, facilityId={}",
                    userId,
                    facilityId
            );

            saveAccessLog(
                    userId,
                    facilityId,
                    null,
                    AccessResult.DENIED,
                    AccessDecisionReason.NO_APPROVAL,
                    now
            );

            return AccessResult.DENIED;
        }

        // 승인 신청은 있지만 현재 시간이 유효기간 밖인 경우
        if (now.isBefore(approvedRequest.getAccessStartAt())
                || now.isAfter(approvedRequest.getAccessEndAt())) {

            log.debug(
                    "시설 접근 거부 - 승인 유효기간 아님, userId={}, facilityId={}, accessRequestId={}",
                    userId,
                    facilityId,
                    approvedRequest.getId()
            );

            saveAccessLog(
                    userId,
                    facilityId,
                    approvedRequest.getId(),
                    AccessResult.DENIED,
                    AccessDecisionReason.OUTSIDE_VALID_PERIOD,
                    now
            );

            return AccessResult.DENIED;
        }

        // 모든 조건 통과
        saveAccessLog(
                userId,
                facilityId,
                approvedRequest.getId(),
                AccessResult.ALLOWED,
                AccessDecisionReason.VALID_APPROVAL,
                now
        );

        log.debug(
                "시설 접근 허용 - userId={}, facilityId={}, accessRequestId={}",
                userId,
                facilityId,
                approvedRequest.getId()
        );

        return AccessResult.ALLOWED;
    }

    private void saveAccessLog(
            Long userId,
            Long facilityId,
            Long accessRequestId,
            AccessResult result,
            AccessDecisionReason decisionReason,
            LocalDateTime accessedAt
    ) {

        AccessLog accessLog = new AccessLog();

        accessLog.setUserId(userId);
        accessLog.setFacilityId(facilityId);
        accessLog.setAccessRequestId(accessRequestId);
        accessLog.setResult(result);
        accessLog.setDecisionReason(decisionReason);
        accessLog.setAccessedAt(accessedAt);

        int savedRows = accessLogMapper.save(accessLog);

        if (savedRows != 1) {
            throw new BusinessException(
                    "접근 로그 저장에 실패했습니다."
            );
        }
    }
}