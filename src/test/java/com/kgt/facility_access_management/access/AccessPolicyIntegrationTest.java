package com.kgt.facility_access_management.access;

import com.kgt.facility_access_management.access.domain.*;
import com.kgt.facility_access_management.access.mapper.AccessRequestMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.*;

import static com.kgt.facility_access_management.access.domain.AccessRequestStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Executes the production mapper XML; H2 is not a substitute for MySQL lock verification. */
@SpringBootTest
@Transactional
class AccessPolicyIntegrationTest {
    @Autowired AccessRequestMapper mapper;
    @Autowired SqlSessionFactory sessions;
    @Autowired JdbcTemplate jdbc;

    private final LocalDateTime now = LocalDateTime.of(2030, 1, 1, 12, 0);

    private AccessRequest request(LocalDateTime start, LocalDateTime end) {
        AccessRequest request = new AccessRequest();
        request.setUserId(1L);
        request.setFacilityId(1L);
        request.setStatus(PENDING);
        request.setRequestReason("Policy test");
        request.setAccessStartAt(start);
        request.setAccessEndAt(end);
        mapper.save(request);
        return request;
    }

    @Test
    void currentApprovalIsNotHiddenByFutureApproval() {
        AccessRequest current = request(now.minusHours(1), now.plusHours(1));
        AccessRequest future = request(now.plusDays(1), now.plusDays(2));
        mapper.approve(current.getId(), 2L, PENDING, APPROVED);
        mapper.approve(future.getId(), 2L, PENDING, APPROVED);

        assertThat(mapper.findApprovedRequest(1L, 1L, now).getId()).isEqualTo(current.getId());
    }

    @Test
    void approvalLookupIncludesBothTimeBoundariesAndKeepsExpiredEvidence() {
        AccessRequest request = request(now, now.plusHours(1));
        mapper.approve(request.getId(), 2L, PENDING, APPROVED);
        assertThat(mapper.findApprovedRequest(1L, 1L, now).getId()).isEqualTo(request.getId());
        assertThat(mapper.findApprovedRequest(1L, 1L, now.plusHours(1)).getId()).isEqualTo(request.getId());
        assertThat(mapper.findApprovedRequest(1L, 1L, now.plusDays(1)).getId()).isEqualTo(request.getId());
        assertThat(mapper.findApprovedRequest(3L, 1L, now)).isNull();
        assertThat(mapper.findApprovedRequest(1L, 2L, now)).isNull();
    }

    @Test
    void completedDecisionCannotBeOverwrittenAndReviewerIsPreserved() {
        AccessRequest request = request(now.minusHours(1), now.plusHours(1));
        assertThat(mapper.approve(request.getId(), 2L, PENDING, APPROVED)).isEqualTo(1);
        assertThat(mapper.reject(request.getId(), 5L, "Late rejection", PENDING, REJECTED)).isZero();
        assertThat(mapper.updateStatus(request.getId(), 1L, PENDING, CANCELLED)).isZero();
        AccessRequest stored = mapper.findById(request.getId());
        assertThat(stored.getStatus()).isEqualTo(APPROVED);
        assertThat(stored.getReviewedBy()).isEqualTo(2L);
        assertThat(stored.getReviewedAt()).isNotNull();
        assertThat(stored.getRejectReason()).isNull();
    }

    @Test
    void sqlAlsoRejectsSelfReviewAndCancellationByAnotherUser() {
        AccessRequest request = request(now.minusHours(1), now.plusHours(1));
        assertThat(mapper.approve(request.getId(), 1L, PENDING, APPROVED)).isZero();
        assertThat(mapper.reject(request.getId(), 1L, "Self review", PENDING, REJECTED)).isZero();
        assertThat(mapper.updateStatus(request.getId(), 3L, PENDING, CANCELLED)).isZero();
        assertThat(mapper.findById(request.getId()).getStatus()).isEqualTo(PENDING);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void twoReviewersReadingPendingProduceExactlyOneWinner() throws Exception {
        AccessRequest request = request(now.minusHours(1), now.plusHours(1));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readPending = new CountDownLatch(2);
        try {
            Future<Integer> approval = executor.submit(() -> compete(request.getId(), true, readPending));
            Future<Integer> rejection = executor.submit(() -> compete(request.getId(), false, readPending));
            int approved = approval.get(10, TimeUnit.SECONDS);
            int rejected = rejection.get(10, TimeUnit.SECONDS);
            assertThat(approved + rejected).isEqualTo(1);
            AccessRequest stored = mapper.findById(request.getId());
            assertThat(stored.getStatus()).isEqualTo(approved == 1 ? APPROVED : REJECTED);
            assertThat(stored.getReviewedBy()).isEqualTo(approved == 1 ? 2L : 5L);
            assertThat(stored.getReviewedAt()).isNotNull();
            assertThat(stored.getRejectReason()).isEqualTo(approved == 1 ? null : "Conflict test");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            jdbc.update("DELETE FROM access_requests WHERE id = ?", request.getId());
        }
    }

    private int compete(Long id, boolean approve, CountDownLatch ready) throws Exception {
        try (var session = sessions.openSession(false)) {
            AccessRequestMapper isolated = session.getMapper(AccessRequestMapper.class);
            assertThat(isolated.findById(id).getStatus()).isEqualTo(PENDING);
            ready.countDown();
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Both reviewers must read PENDING before writing");
            }
            int rows = approve
                    ? isolated.approve(id, 2L, PENDING, APPROVED)
                    : isolated.reject(id, 5L, "Conflict test", PENDING, REJECTED);
            session.commit();
            return rows;
        }
    }
}
