package com.kgt.facility_access_management.access.service;

import com.kgt.facility_access_management.access.domain.AccessRequest;
import com.kgt.facility_access_management.access.domain.AccessRequestStatus;
import com.kgt.facility_access_management.access.dto.AccessRequestCreateForm;
import com.kgt.facility_access_management.access.mapper.AccessRequestMapper;
import com.kgt.facility_access_management.common.exception.BusinessException;
import com.kgt.facility_access_management.facility.domain.Facility;
import com.kgt.facility_access_management.facility.service.FacilityService;
import org.springframework.stereotype.Service;

@Service
public class AccessRequestService {

    private final AccessRequestMapper accessRequestMapper;
    private final FacilityService facilityService;

    public AccessRequestService(
            AccessRequestMapper accessRequestMapper,
            FacilityService facilityService
    ) {
        this.accessRequestMapper = accessRequestMapper;
        this.facilityService = facilityService;
    }


    public AccessRequest createRequest(
            Long userId,
            AccessRequestCreateForm form
    ) {

        // 시설 조회
        Facility facility = facilityService.findById(form.getFacilityId())
                .orElseThrow(() ->
                        new BusinessException("시설을 찾을 수 없습니다.")
                );

        // 활성 시설인지 확인
        if (!facility.isActive()) {
            throw new BusinessException(
                    "비활성화된 시설입니다."
            );
        }

        // 접근 시작/종료 시간 검증
        if (!form.getAccessStartAt().isBefore(form.getAccessEndAt())) {
            throw new BusinessException(
                    "접근 시작 시간은 종료시간보다 빨라야 합니다."
            );
        }

        AccessRequest accessRequest = new AccessRequest();

        // userId는 클라이언트 입력이 아닌 세션 사용자 ID 사용
        accessRequest.setUserId(userId);
        accessRequest.setFacilityId(form.getFacilityId());
        accessRequest.setRequestReason(form.getRequestReason());
        accessRequest.setAccessStartAt(form.getAccessStartAt());
        accessRequest.setAccessEndAt(form.getAccessEndAt());

        // 최초 신청 상태는 항상 PENDING
        accessRequest.setStatus(AccessRequestStatus.PENDING);

        accessRequestMapper.save(accessRequest);

        return accessRequest;
    }


    public void cancelRequest(
            Long accessRequestId,
            Long userId
    ) {

        AccessRequest accessRequest =
                accessRequestMapper.findById(accessRequestId);

        if (accessRequest == null) {
            throw new BusinessException(
                    "해당 접근요청은 존재하지 않습니다."
            );
        }

        // 본인 신청만 취소 가능
        if (!accessRequest.getUserId().equals(userId)) {
            throw new BusinessException(
                    "신청한 본인만 취소 가능합니다."
            );
        }

        // 상태 전이 검증
        validatePendingStatus(accessRequest);

        int updatedRows = accessRequestMapper.updateStatus(
                accessRequestId,
                userId,
                AccessRequestStatus.PENDING,
                AccessRequestStatus.CANCELLED
        );

        // 조회 후 UPDATE 사이에 상태가 변경된 경우
        if (updatedRows == 0) {
            throw new BusinessException(
                    "접근요청 상태가 변경되어 취소할 수 없습니다."
            );
        }
    }


    public void approveRequest(
            Long accessRequestId,
            Long reviewerId
    ) {

        AccessRequest accessRequest =
                accessRequestMapper.findById(accessRequestId);

        if (accessRequest == null) {
            throw new BusinessException(
                    "해당 접근요청은 존재하지 않습니다."
            );
        }

        // 자기 신청 승인 금지
        if (accessRequest.getUserId().equals(reviewerId)) {
            throw new BusinessException(
                    "본인의 접근요청은 직접 승인할 수 없습니다."
            );
        }

        // 상태 전이 검증
        validatePendingStatus(accessRequest);

        int updatedRows = accessRequestMapper.approve(
                accessRequestId,
                reviewerId,
                AccessRequestStatus.PENDING,
                AccessRequestStatus.APPROVED
        );

        // 조회 후 UPDATE 사이에 상태가 변경된 경우
        if (updatedRows == 0) {
            throw new BusinessException(
                    "접근요청 상태가 변경되어 승인할 수 없습니다."
            );
        }
    }


    public void rejectRequest(
            Long accessRequestId,
            Long reviewerId,
            String rejectReason
    ) {

        AccessRequest accessRequest =
                accessRequestMapper.findById(accessRequestId);

        if (accessRequest == null) {
            throw new BusinessException(
                    "해당 접근요청은 존재하지 않습니다."
            );
        }

        // 자기 신청 반려 금지
        if (accessRequest.getUserId().equals(reviewerId)) {
            throw new BusinessException(
                    "본인의 접근요청은 직접 반려할 수 없습니다."
            );
        }

        // 상태 전이 검증
        validatePendingStatus(accessRequest);

        // 반려 사유 필수
        if (rejectReason == null || rejectReason.isBlank()) {
            throw new BusinessException(
                    "반려 사유를 입력해야 합니다."
            );
        }

        int updatedRows = accessRequestMapper.reject(
                accessRequestId,
                reviewerId,
                rejectReason,
                AccessRequestStatus.PENDING,
                AccessRequestStatus.REJECTED
        );

        // 조회 후 UPDATE 사이에 상태가 변경된 경우
        if (updatedRows == 0) {
            throw new BusinessException(
                    "접근요청 상태가 변경되어 반려할 수 없습니다."
            );
        }
    }


    /**
     * 접근 신청 상태 전이 규칙
     *
     * PENDING → APPROVED
     * PENDING → REJECTED
     * PENDING → CANCELLED
     *
     * APPROVED / REJECTED / CANCELLED는 최종 상태이므로
     * 더 이상 상태 변경을 허용하지 않는다.
     */
    private void validatePendingStatus(
            AccessRequest accessRequest
    ) {

        if (accessRequest.getStatus() != AccessRequestStatus.PENDING) {
            throw new BusinessException(
                    "PENDING 상태의 접근요청만 처리할 수 있습니다."
            );
        }
    }
}