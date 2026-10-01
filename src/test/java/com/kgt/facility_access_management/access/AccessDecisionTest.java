package com.kgt.facility_access_management.access;

import com.kgt.facility_access_management.access.domain.*;
import com.kgt.facility_access_management.access.mapper.*;
import com.kgt.facility_access_management.access.service.AccessService;
import com.kgt.facility_access_management.common.exception.BusinessException;
import com.kgt.facility_access_management.facility.domain.Facility;
import com.kgt.facility_access_management.facility.service.FacilityService;
import com.kgt.facility_access_management.user.domain.User;
import com.kgt.facility_access_management.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.*;
import java.util.Optional;

import static com.kgt.facility_access_management.access.domain.AccessDecisionReason.*;
import static com.kgt.facility_access_management.access.domain.AccessResult.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessDecisionTest {
    @Mock UserMapper users;
    @Mock FacilityService facilities;
    @Mock AccessRequestMapper requests;
    @Mock AccessLogMapper logs;
    private final Clock clock = Clock.fixed(Instant.parse("2030-01-01T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final LocalDateTime now = LocalDateTime.now(clock);
    private AccessService service;

    @BeforeEach void setUp() { service = new AccessService(users, facilities, requests, logs, clock); }

    private void entities(boolean userActive, boolean facilityActive) {
        User user = new User(); user.setId(1L); user.setActive(userActive);
        Facility facility = new Facility(); facility.setId(2L); facility.setActive(facilityActive);
        when(users.findById(1L)).thenReturn(user);
        when(facilities.findById(2L)).thenReturn(Optional.of(facility));
    }

    private void approval(long startOffset, long endOffset) {
        AccessRequest request = new AccessRequest();
        request.setId(10L);
        request.setAccessStartAt(now.plusSeconds(startOffset));
        request.setAccessEndAt(now.plusSeconds(endOffset));
        when(requests.findApprovedRequest(1L, 2L, now)).thenReturn(request);
    }

    private void assertLog(AccessResult result, AccessDecisionReason reason, Long requestId) {
        var captor = ArgumentCaptor.forClass(AccessLog.class);
        verify(logs).save(captor.capture()); // exactly one audit record per decision
        AccessLog record = captor.getValue();
        assertThat(record.getUserId()).isEqualTo(1L);
        assertThat(record.getFacilityId()).isEqualTo(2L);
        assertThat(record.getAccessRequestId()).isEqualTo(requestId);
        assertThat(record.getResult()).isEqualTo(result);
        assertThat(record.getDecisionReason()).isEqualTo(reason);
        assertThat(record.getAccessedAt()).isEqualTo(now);
    }

    @ParameterizedTest
    @CsvSource({"false,true,USER_INACTIVE", "true,false,FACILITY_INACTIVE", "false,false,USER_INACTIVE"})
    void inactiveEntitiesAreDeniedBeforeApprovalLookup(boolean userActive, boolean facilityActive,
                                                      AccessDecisionReason reason) {
        entities(userActive, facilityActive);
        when(logs.save(any())).thenReturn(1);
        assertThat(service.validateAccess(1L, 2L)).isEqualTo(DENIED);
        assertLog(DENIED, reason, null);
        verifyNoInteractions(requests);
    }

    @Test void missingApprovalIsDeniedWithItsOwnReason() {
        entities(true, true);
        when(logs.save(any())).thenReturn(1);
        assertThat(service.validateAccess(1L, 2L)).isEqualTo(DENIED);
        assertLog(DENIED, NO_APPROVAL, null);
    }

    @ParameterizedTest
    @CsvSource({"-3600,3600,ALLOWED,VALID_APPROVAL", "0,3600,ALLOWED,VALID_APPROVAL",
            "-3600,0,ALLOWED,VALID_APPROVAL", "1,3600,DENIED,OUTSIDE_VALID_PERIOD",
            "-3600,-1,DENIED,OUTSIDE_VALID_PERIOD"})
    void exactTimeBoundariesDecideAccessAndUseTheSameAuditTimestamp(long start, long end,
                    AccessResult result, AccessDecisionReason reason) {
        entities(true, true); approval(start, end);
        when(logs.save(any())).thenReturn(1);
        assertThat(service.validateAccess(1L, 2L)).isEqualTo(result);
        assertLog(result, reason, 10L);
    }

    @Test void unknownUserIsAnInputErrorWithoutAnInvalidForeignKeyLog() {
        assertThatThrownBy(() -> service.validateAccess(1L, 2L))
                .isInstanceOf(BusinessException.class).hasMessage("사용자를 찾을 수 없습니다.");
        verifyNoInteractions(facilities, requests, logs);
    }

    @Test void unknownFacilityIsAnInputErrorWithoutAnInvalidForeignKeyLog() {
        User user = new User(); user.setActive(true);
        when(users.findById(1L)).thenReturn(user);
        when(facilities.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.validateAccess(1L, 2L))
                .isInstanceOf(BusinessException.class).hasMessage("시설을 찾을 수 없습니다.");
        verifyNoInteractions(requests, logs);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void noDecisionIsReturnedWhenAuditInsertReportsFailure(boolean approved) {
        entities(true, true);
        if (approved) approval(-3600, 3600);
        assertThatThrownBy(() -> service.validateAccess(1L, 2L))
                .isInstanceOf(BusinessException.class).hasMessage("접근 로그 저장에 실패했습니다.");
        verify(logs).save(any());
    }

    @Test void databaseFailureIsPropagatedInsteadOfReturningAllowed() {
        entities(true, true); approval(-3600, 3600);
        when(logs.save(any())).thenThrow(new DataAccessResourceFailureException("Test DB failure"));
        assertThatThrownBy(() -> service.validateAccess(1L, 2L))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }
}
