package com.kgt.facility_access_management.access.service;

import com.kgt.facility_access_management.access.domain.AccessRequest;
import com.kgt.facility_access_management.access.domain.AccessRequestStatus;
import com.kgt.facility_access_management.access.dto.AccessRequestCreateForm;
import com.kgt.facility_access_management.access.mapper.AccessRequestMapper;
import com.kgt.facility_access_management.auth.service.AuthService;
import com.kgt.facility_access_management.facility.domain.Facility;
import com.kgt.facility_access_management.facility.mapper.FacilityMapper;
import com.kgt.facility_access_management.facility.service.FacilityService;
import com.kgt.facility_access_management.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccessRequestService {
    private final AccessRequestMapper accessRequestMapper;
    private final FacilityService facilityService;

    public AccessRequestService(AccessRequestMapper accessRequestMapper, FacilityService facilityService, AuthService authService, UserMapper userMapper) {
        this.accessRequestMapper = accessRequestMapper;
        this.facilityService = facilityService;
    }

    public AccessRequest createRequest(Long userId, AccessRequestCreateForm form) {
        //시설을 조회
        Facility facility = facilityService.findById(form.getFacilityId())
                .orElseThrow(() -> new IllegalArgumentException("시설을 찾을 수 없습니다."));

        //활성시설인지 체크
        if (!facility.isActive()) {
            throw new IllegalArgumentException("비활성화된 시설입니다.");
        }

        //접근시작~종료시간 시간검증
        if (!form.getAccessStartAt().isBefore(form.getAccessEndAt())) {
            throw new IllegalArgumentException("접근 시작 시간은 종료시간보다 빨라야 합니다.");
        }

        //접근요청 객체 생성
        AccessRequest accessRequest = new AccessRequest();

        //userId는 실제로 신청시 입력을 받지 않고, 서버에서 인증한 값으로 덮어쓴다.
        //세션에서 꺼낸 ID를 사용, 다른사람 ID로 신청못하게 함
        accessRequest.setUserId(userId);
        accessRequest.setFacilityId(form.getFacilityId());
        accessRequest.setRequestReason(form.getRequestReason());
        accessRequest.setAccessStartAt(form.getAccessStartAt());
        accessRequest.setAccessEndAt(form.getAccessEndAt());
        //서버에서 무조건 PENDING값으로 설정
        accessRequest.setStatus(AccessRequestStatus.PENDING);

        //이 조건들을 만족했을때만 저장
        accessRequestMapper.save(accessRequest);

        return accessRequest;
    }

    public void cancelRequest(Long accessRequestId, Long userId) {

        // 접근요청을 조회
        AccessRequest accessRequest = accessRequestMapper.findById(accessRequestId);
        if (accessRequest == null) {
            throw new IllegalArgumentException("해당 접근요청은 존재 하지 않습니다.");
        }

        // 본인만이 변경할 수 있게, 신청 유저아이디와 해당 유저아이디 비교
        if (!accessRequest.getUserId().equals(userId)) {
            throw new IllegalArgumentException("신청한 본인만 취소 가능합니다.");
        }

        // PENDING상태만 CANCELLED로 변경 할 수있으니 상태체크
        if (accessRequest.getStatus() != AccessRequestStatus.PENDING) {
            throw new IllegalArgumentException("PENDING상태의 신청만 취소할 수 있습니다.");
        }

        // 이 조건들을 통과하면 업데이트문 실행
        int updatedRows = accessRequestMapper.updateStatus(accessRequestId, userId, AccessRequestStatus.PENDING, AccessRequestStatus.CANCELLED);

        // 서비스에서 상태확인하고나서, UPDATE하기전에 다른요청이 먼저 상태를 변경 할 수도있다.
        // 그 사이에 변경점이 없을때만 통과임
        if (updatedRows == 0) {
            throw new IllegalStateException("접근요청 상태가 변경되어 취소할 수 없습니다.");
        }



    }

}
