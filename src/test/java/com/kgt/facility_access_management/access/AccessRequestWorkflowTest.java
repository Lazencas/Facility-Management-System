package com.kgt.facility_access_management.access;

import com.kgt.facility_access_management.access.domain.*;
import com.kgt.facility_access_management.access.mapper.AccessRequestMapper;
import com.kgt.facility_access_management.access.service.AccessRequestService;
import com.kgt.facility_access_management.common.exception.BusinessException;
import com.kgt.facility_access_management.facility.service.FacilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.kgt.facility_access_management.access.domain.AccessRequestStatus.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessRequestWorkflowTest {
    @Mock AccessRequestMapper mapper;
    @Mock FacilityService facilities;
    AccessRequestService service;

    @BeforeEach void setUp() { service = new AccessRequestService(mapper, facilities); }

    private void stored(AccessRequestStatus status) {
        AccessRequest request = new AccessRequest();
        request.setId(10L);
        request.setUserId(1L);
        request.setStatus(status);
        when(mapper.findById(10L)).thenReturn(request);
    }

    private void process(String action) {
        switch (action) {
            case "approve" -> service.approveRequest(10L, 2L);
            case "reject" -> service.rejectRequest(10L, 2L, "Policy mismatch");
            case "cancel" -> service.cancelRequest(10L, 1L);
            default -> throw new IllegalArgumentException(action);
        }
    }

    @ParameterizedTest
    @CsvSource({"approve,APPROVED", "approve,REJECTED", "approve,CANCELLED",
            "reject,APPROVED", "reject,REJECTED", "reject,CANCELLED",
            "cancel,APPROVED", "cancel,REJECTED", "cancel,CANCELLED"})
    void terminalStatesRejectEveryFurtherTransition(String action, AccessRequestStatus status) {
        stored(status);
        assertThatThrownBy(() -> process(action)).isInstanceOf(BusinessException.class)
                .hasMessage("PENDING 상태의 접근요청만 처리할 수 있습니다.");
        verify(mapper).findById(10L);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest @ValueSource(strings = {"approve", "reject", "cancel"})
    void staleReadIsNotReportedAsSuccessWhenUpdateAffectsZeroRows(String action) {
        stored(PENDING); // Mockito's default update result is zero: another request won.
        assertThatThrownBy(() -> process(action)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("상태가 변경되어");
        switch (action) {
            case "approve" -> verify(mapper).approve(10L, 2L, PENDING, APPROVED);
            case "reject" -> verify(mapper).reject(10L, 2L, "Policy mismatch", PENDING, REJECTED);
            case "cancel" -> verify(mapper).updateStatus(10L, 1L, PENDING, CANCELLED);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"approve", "reject", "cancel"})
    void pendingRequestCanReachEachPermittedTerminalState(String action) {
        stored(PENDING);
        switch (action) {
            case "approve" -> when(mapper.approve(10L, 2L, PENDING, APPROVED)).thenReturn(1);
            case "reject" -> when(mapper.reject(10L, 2L, "Policy mismatch", PENDING, REJECTED)).thenReturn(1);
            case "cancel" -> when(mapper.updateStatus(10L, 1L, PENDING, CANCELLED)).thenReturn(1);
        }
        process(action);
    }

    @Test void reviewerCannotApproveOrRejectOwnRequest() {
        stored(PENDING);
        assertThatThrownBy(() -> service.approveRequest(10L, 1L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("본인의 접근요청");
        assertThatThrownBy(() -> service.rejectRequest(10L, 1L, "Self review"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("본인의 접근요청");
        verify(mapper, times(2)).findById(10L);
        verifyNoMoreInteractions(mapper);
    }

    @Test void anotherUserCannotCancelRequest() {
        stored(PENDING);
        assertThatThrownBy(() -> service.cancelRequest(10L, 3L))
                .isInstanceOf(BusinessException.class).hasMessage("신청한 본인만 취소 가능합니다.");
        verify(mapper).findById(10L);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\t"})
    void rejectionRequiresAnExplanation(String reason) {
        stored(PENDING);
        assertThatThrownBy(() -> service.rejectRequest(10L, 2L, reason))
                .isInstanceOf(BusinessException.class).hasMessage("반려 사유를 입력해야 합니다.");
        verify(mapper).findById(10L);
        verifyNoMoreInteractions(mapper);
    }
}
