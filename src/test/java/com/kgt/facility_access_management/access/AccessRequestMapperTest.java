package com.kgt.facility_access_management.access;

import com.kgt.facility_access_management.access.domain.AccessRequest;
import com.kgt.facility_access_management.access.domain.AccessRequestStatus;
import com.kgt.facility_access_management.access.mapper.AccessRequestMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class AccessRequestMapperTest {

    @Autowired
    AccessRequestMapper mapper;
    @Autowired
    private AccessRequestMapper accessRequestMapper;

    @Test
    void 접근요청저장및아이디를통한조회(){
        //given
        AccessRequest accessRequest = new AccessRequest();
        accessRequest.setUserId(1L);
        accessRequest.setFacilityId(1L);
        accessRequest.setStatus(AccessRequestStatus.PENDING);

        accessRequest.setRequestReason("감사");
        accessRequest.setAccessStartAt(LocalDateTime.of(2026,10,1,9,0));
        accessRequest.setAccessEndAt(LocalDateTime.of(2026,10,1,18,0));

        //when
        int result = accessRequestMapper.save(accessRequest);

        AccessRequest findRequest = accessRequestMapper.findById(accessRequest.getId());

        //then
        assertThat(result).isEqualTo(1);
        assertThat(accessRequest.getId()).isNotNull();

        assertThat(findRequest.getUserId())
                .isEqualTo(accessRequest.getUserId());

        assertThat(findRequest.getFacilityId())
                .isEqualTo(accessRequest.getFacilityId());

        assertThat(findRequest.getStatus())
                .isEqualTo(AccessRequestStatus.PENDING);

        assertThat(findRequest.getRequestReason())
                .isEqualTo("감사");
    }


    @Test
    void 유저아이디로접근요청찾기(){
        //given
        AccessRequest accessRequest = new AccessRequest();
        accessRequest.setUserId(1L);
        accessRequest.setFacilityId(1L);
        accessRequest.setStatus(AccessRequestStatus.PENDING);
        accessRequest.setRequestReason("사용자별 조회 테스트");
        accessRequest.setAccessStartAt(
                LocalDateTime.of(2026, 10, 2, 9, 0)
        );
        accessRequest.setAccessEndAt(
                LocalDateTime.of(2026, 10, 2, 18, 0)
        );

        accessRequestMapper.save(accessRequest);

        //when
        List<AccessRequest> requests =
                accessRequestMapper.findByUserId(1L);

        //then
        assertThat(requests).isNotEmpty();

        assertThat(requests)
                .extracting(AccessRequest::getId)
                .contains(accessRequest.getId());
    }



}
