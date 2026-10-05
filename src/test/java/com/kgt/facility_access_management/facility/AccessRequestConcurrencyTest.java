package com.kgt.facility_access_management.facility;

import com.kgt.facility_access_management.access.domain.AccessRequestStatus;
import com.kgt.facility_access_management.access.mapper.AccessRequestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/facility_access_management",
        "spring.datasource.username=root",
        "spring.datasource.password=test1234",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
})
class AccessRequestConcurrencyTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    AccessRequestMapper accessRequestMapper;

    private Long requestId;
    private Long adminAId;
    private Long adminBId;

    private static final int THREAD_COUNT = 5;


    @BeforeEach
    void setUp() {

        // portfolio_test_data.sql에서 생성한 데이터 조회
        requestId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM access_requests
                WHERE request_reason = 'PF_TEST_STATE_CONCURRENT_APPROVE'
                """,
                Long.class
        );

        adminAId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM users
                WHERE login_id = 'pf_admin_a'
                """,
                Long.class
        );

        adminBId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM users
                WHERE login_id = 'pf_admin_b'
                """,
                Long.class
        );


        // 매 테스트 시작 전에 동일한 초기상태로 복구
        jdbcTemplate.update(
                """
                UPDATE access_requests
                SET status = 'PENDING',
                    reviewed_by = NULL,
                    reviewed_at = NULL,
                    reject_reason = NULL
                WHERE id = ?
                """,
                requestId
        );


        String beforeStatus = getStatus();

        assertEquals("PENDING", beforeStatus);
    }


    // =========================================================
    // TEST 1
    // DB 조건부 UPDATE 자체의 동시성 검증
    // =========================================================

    @Test
    void 조건부_UPDATE_SQL은_5개_동시_승인중_하나만_성공한다() throws Exception {

        List<Integer> results = executeConcurrentApprove(
                reviewerId -> jdbcTemplate.update(
                        """
                        UPDATE access_requests
                        SET status = 'APPROVED',
                            reviewed_by = ?,
                            reviewed_at = NOW()
                        WHERE id = ?
                          AND status = 'PENDING'
                          AND user_id != ?
                        """,
                        reviewerId,
                        requestId,
                        reviewerId
                )
        );


        printResult(
                "Conditional UPDATE SQL Test",
                results
        );


        verifyResult(results);
    }


    // =========================================================
    // TEST 2
    // 실제 MyBatis Mapper 승인 SQL 동시성 검증
    // =========================================================

    @Test
    void 실제_Mapper에서도_5개_동시_승인중_하나만_성공한다() throws Exception {

        List<Integer> results = executeConcurrentApprove(
                reviewerId -> accessRequestMapper.approve(
                        requestId,
                        reviewerId,
                        AccessRequestStatus.PENDING,
                        AccessRequestStatus.APPROVED
                )
        );


        printResult(
                "MyBatis Mapper Concurrent Test",
                results
        );


        verifyResult(results);
    }


    // =========================================================
    // 5개의 승인 요청을 최대한 같은 시점에 실행
    // =========================================================

    private List<Integer> executeConcurrentApprove(
            Function<Long, Integer> approveAction
    ) throws Exception {

        ExecutorService executorService =
                Executors.newFixedThreadPool(THREAD_COUNT);

        CountDownLatch readyLatch =
                new CountDownLatch(THREAD_COUNT);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        List<Future<Integer>> futures =
                new ArrayList<>();


        try {

            for (int i = 0; i < THREAD_COUNT; i++) {

                // 관리자 A / B를 번갈아 사용
                Long reviewerId =
                        (i % 2 == 0)
                                ? adminAId
                                : adminBId;


                Future<Integer> future =
                        executorService.submit(() -> {

                            // 스레드 준비 완료
                            readyLatch.countDown();

                            // 모든 스레드가 준비될 때까지 대기
                            startLatch.await();

                            // 승인 실행
                            return approveAction.apply(
                                    reviewerId
                            );
                        });


                futures.add(future);
            }


            // 5개의 스레드가 모두 준비됐는지 확인
            assertTrue(
                    readyLatch.await(
                            3,
                            TimeUnit.SECONDS
                    )
            );


            // 5개의 요청을 동시에 출발
            startLatch.countDown();


            List<Integer> results =
                    new ArrayList<>();


            for (Future<Integer> future : futures) {

                results.add(
                        future.get(
                                5,
                                TimeUnit.SECONDS
                        )
                );
            }


            return results;

        } finally {

            executorService.shutdownNow();
        }
    }


    // =========================================================
    // 공통 검증
    // =========================================================

    private void verifyResult(
            List<Integer> results
    ) {

        int totalUpdatedRows =
                results.stream()
                        .mapToInt(Integer::intValue)
                        .sum();


        long successCount =
                results.stream()
                        .filter(result -> result == 1)
                        .count();


        // 5개 요청의 UPDATE 결과 합은 정확히 1
        assertEquals(
                1,
                totalUpdatedRows
        );


        // 실제 성공 요청 수도 정확히 1
        assertEquals(
                1,
                successCount
        );


        // 최종 상태는 APPROVED
        assertEquals(
                "APPROVED",
                getStatus()
        );


        Long reviewedBy =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reviewed_by
                        FROM access_requests
                        WHERE id = ?
                        """,
                        Long.class,
                        requestId
                );


        // 실제 승인자는 ADMIN A 또는 ADMIN B
        assertTrue(
                reviewedBy.equals(adminAId)
                        || reviewedBy.equals(adminBId)
        );
    }


    // =========================================================
    // 현재 요청 상태 조회
    // =========================================================

    private String getStatus() {

        return jdbcTemplate.queryForObject(
                """
                SELECT status
                FROM access_requests
                WHERE id = ?
                """,
                String.class,
                requestId
        );
    }


    // =========================================================
    // 포트폴리오 캡처용 결과 출력
    // =========================================================

    private void printResult(
            String testName,
            List<Integer> results
    ) {

        System.out.println();
        System.out.println(
                "============================================="
        );

        System.out.println(testName);

        System.out.println(
                "Request ID : " + requestId
        );

        System.out.println(
                "Before Status : PENDING"
        );


        for (int i = 0; i < results.size(); i++) {

            System.out.println(
                    "Request "
                            + (i + 1)
                            + " updated rows : "
                            + results.get(i)
            );
        }


        int totalUpdatedRows =
                results.stream()
                        .mapToInt(Integer::intValue)
                        .sum();


        System.out.println(
                "Total updated rows : "
                        + totalUpdatedRows
        );


        System.out.println(
                "Final Status : "
                        + getStatus()
        );


        Long reviewedBy =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reviewed_by
                        FROM access_requests
                        WHERE id = ?
                        """,
                        Long.class,
                        requestId
                );


        System.out.println(
                "Reviewed By : "
                        + reviewedBy
        );


        System.out.println(
                "============================================="
        );
    }
}