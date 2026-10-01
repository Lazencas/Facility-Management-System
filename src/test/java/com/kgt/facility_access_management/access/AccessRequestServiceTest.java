package com.kgt.facility_access_management.access;

import com.kgt.facility_access_management.access.domain.AccessRequest;
import com.kgt.facility_access_management.access.domain.AccessRequestStatus;
import com.kgt.facility_access_management.access.dto.AccessRequestCreateForm;
import com.kgt.facility_access_management.access.mapper.AccessRequestMapper;
import com.kgt.facility_access_management.access.service.AccessRequestService;
import com.kgt.facility_access_management.facility.domain.Facility;
import com.kgt.facility_access_management.facility.service.FacilityService;
import com.kgt.facility_access_management.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccessRequestServiceTest {

    @Mock
    private AccessRequestMapper accessRequestMapper;

    @Mock
    private FacilityService facilityService;

    @InjectMocks
    private AccessRequestService accessRequestService;

    @Test
    void 요청생성() {
        //given
        Long userId = 1L;
        Long facilityId = 1L;

        Facility facility = mock(Facility.class);

        when(facilityService.findById(facilityId))
                .thenReturn(Optional.of(facility));

        when(facility.isActive())
                .thenReturn(true);

        AccessRequestCreateForm form = new AccessRequestCreateForm();
        form.setFacilityId(facilityId);
        form.setRequestReason("시설 점검");
        form.setAccessStartAt(
                LocalDateTime.of(2026, 10, 1, 9, 0)
        );
        form.setAccessEndAt(
                LocalDateTime.of(2026, 10, 1, 18, 0)
        );

        //when
        AccessRequest request = accessRequestService.createRequest(userId, form);

        //then
        //서비스가 매퍼에 어떤 AccessRequest를 저장하려 했는지 확인
        ArgumentCaptor<AccessRequest> captor = ArgumentCaptor.forClass(AccessRequest.class);

        verify(accessRequestMapper).save(captor.capture());

        AccessRequest savedRequest = captor.getValue();

        assertThat(savedRequest.getUserId())
                .isEqualTo(userId);

        assertThat(savedRequest.getFacilityId())
                .isEqualTo(facilityId);

        assertThat(savedRequest.getStatus())
                .isEqualTo(AccessRequestStatus.PENDING);

        assertThat(savedRequest.getRequestReason())
                .isEqualTo("시설 점검");

    }

    @Test
    void 비활성시설저장테스트(){
        // given
        Facility facility = mock(Facility.class);

        when(facilityService.findById(1L))
                .thenReturn(Optional.of(facility));

        when(facility.isActive())
                .thenReturn(false);

        AccessRequestCreateForm form = new AccessRequestCreateForm();
        form.setFacilityId(1L);

        // when & then
        assertThatThrownBy(() ->
                accessRequestService.createRequest(1L, form)
        ).isInstanceOf(BusinessException.class).hasMessage("비활성화된 시설입니다.");

        //비활성 시설이 DB까지 안가게 차단 되는지
        verify(accessRequestMapper, never())
                .save(any());
    }

    @Test
    void 잘못된시간테스트() {
        // given
        Long facilityId = 1L;

        Facility facility = mock(Facility.class);

        when(facilityService.findById(facilityId))
                .thenReturn(Optional.of(facility));

        when(facility.isActive())
                .thenReturn(true);

        AccessRequestCreateForm form = new AccessRequestCreateForm();

        form.setFacilityId(facilityId);

        form.setAccessStartAt(
                LocalDateTime.of(2026, 10, 1, 18, 0)
        );

        form.setAccessEndAt(
                LocalDateTime.of(2026, 10, 1, 9, 0)
        );

        // when & then
        assertThatThrownBy(() ->
                accessRequestService.createRequest(1L, form)
        ).isInstanceOf(BusinessException.class).hasMessage("접근 시작 시간은 종료시간보다 빨라야 합니다.");

        verify(accessRequestMapper, never())
                .save(any());
    }

    @Test
    void 존재하지않는시설테스트() {

        // given
        AccessRequestCreateForm form = new AccessRequestCreateForm();
        form.setFacilityId(999L);

        when(facilityService.findById(999L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                accessRequestService.createRequest(1L, form)
        ).isInstanceOf(BusinessException.class).hasMessage("시설을 찾을 수 없습니다.");

        verify(accessRequestMapper, never())
                .save(any());
    }



}
