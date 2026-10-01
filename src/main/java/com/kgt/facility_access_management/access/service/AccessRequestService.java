package com.kgt.facility_access_management.access.service;

import com.kgt.facility_access_management.access.domain.AccessRequest;
import com.kgt.facility_access_management.access.domain.AccessRequestStatus;
import com.kgt.facility_access_management.access.dto.AccessRequestCreateForm;
import com.kgt.facility_access_management.access.mapper.AccessRequestMapper;
import com.kgt.facility_access_management.facility.domain.Facility;
import com.kgt.facility_access_management.facility.mapper.FacilityMapper;
import com.kgt.facility_access_management.facility.service.FacilityService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccessRequestService {
    private final AccessRequestMapper accessRequestMapper;
    private final FacilityService facilityService;

    public AccessRequestService(AccessRequestMapper accessRequestMapper, FacilityService facilityService) {
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

}
