package com.kgt.facility_access_management.access;

import com.kgt.facility_access_management.access.domain.*;
import com.kgt.facility_access_management.access.dto.*;
import com.kgt.facility_access_management.access.mapper.*;
import com.kgt.facility_access_management.access.service.AccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.kgt.facility_access_management.access.domain.AccessDecisionReason.*;
import static com.kgt.facility_access_management.access.domain.AccessResult.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AccessSearchIntegrationTest {
    @Autowired AccessLogMapper logs;
    @Autowired AccessRequestMapper requests;
    @Autowired AccessService service;
    @Autowired JdbcTemplate jdbc;
    private final LocalDateTime at = LocalDateTime.of(2030, 1, 1, 12, 0);

    private AccessLog record(long user, long facility, AccessResult result, LocalDateTime time) {
        AccessLog log = new AccessLog();
        log.setUserId(user); log.setFacilityId(facility); log.setResult(result);
        log.setDecisionReason(result == ALLOWED ? VALID_APPROVAL : NO_APPROVAL);
        log.setAccessedAt(time); logs.save(log); return log;
    }

    @Test void auditSearchCombinesNamesResultAndInclusivePeriod() {
        AccessLog target = record(1L, 1L, DENIED, at);
        record(1L, 1L, ALLOWED, at);
        record(3L, 1L, DENIED, at);
        record(1L, 2L, DENIED, at);
        record(1L, 1L, DENIED, at.minusSeconds(1));
        record(1L, 1L, DENIED, at.plusSeconds(1));
        AccessLogSearchConditionDTO condition = new AccessLogSearchConditionDTO();
        condition.setUserName("One"); condition.setFacilityName("Server");
        condition.setResult(DENIED); condition.setFrom(at); condition.setTo(at);
        assertThat(logs.search(condition)).extracting(AccessLog::getId).containsExactly(target.getId());
        condition.setUserName("' OR 1=1 --");
        assertThat(logs.search(condition)).isEmpty();
    }

    @Test void optionalEmptyFiltersKeepNewestFirstOrdering() {
        AccessLog older = record(1L, 1L, DENIED, at);
        AccessLog newer = record(1L, 1L, DENIED, at.plusSeconds(1));
        AccessLogSearchConditionDTO condition = new AccessLogSearchConditionDTO();
        condition.setUserName(""); condition.setFacilityName("");
        assertThat(logs.search(condition)).extracting(AccessLog::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test void requestSearchUsesCreationTimeAndCombinesAllConditions() {
        AccessRequest target = request(1L, 1L, AccessRequestStatus.PENDING, at);
        request(3L, 1L, AccessRequestStatus.PENDING, at);
        request(1L, 2L, AccessRequestStatus.PENDING, at);
        request(1L, 1L, AccessRequestStatus.CANCELLED, at);
        request(1L, 1L, AccessRequestStatus.PENDING, at.minusSeconds(1));
        AccessRequestSearchConditionDTO condition = new AccessRequestSearchConditionDTO();
        condition.setUserId(1L); condition.setFacilityId(1L);
        condition.setStatus(AccessRequestStatus.PENDING); condition.setFrom(at); condition.setTo(at);
        assertThat(requests.search(condition)).extracting(AccessRequest::getId).containsExactly(target.getId());
    }

    private AccessRequest request(long user, long facility, AccessRequestStatus status, LocalDateTime created) {
        AccessRequest request = new AccessRequest();
        request.setUserId(user); request.setFacilityId(facility); request.setStatus(status);
        request.setAccessStartAt(at.plusDays(1)); request.setAccessEndAt(at.plusDays(2));
        requests.save(request);
        jdbc.update("UPDATE access_requests SET created_at = ? WHERE id = ?", created, request.getId());
        return request;
    }

    @Test void deniedDecisionIsPersistedAsBusinessHistoryThroughRealServiceAndMapper() {
        assertThat(service.validateAccess(1L, 3L)).isEqualTo(DENIED);
        AccessLogSearchConditionDTO condition = new AccessLogSearchConditionDTO();
        condition.setFacilityName("Closed Lab");
        assertThat(logs.search(condition)).singleElement().satisfies(log -> {
            assertThat(log.getDecisionReason()).isEqualTo(FACILITY_INACTIVE);
            assertThat(log.getAccessRequestId()).isNull();
            assertThat(log.getUserId()).isEqualTo(1L);
        });
    }
}
