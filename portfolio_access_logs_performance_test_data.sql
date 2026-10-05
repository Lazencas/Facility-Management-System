USE facility_access_management;


-- =========================================================
-- Portfolio Performance Test Data
-- access_logs 100,000건 생성
--
-- 목적:
-- 관리자 시설별 + 기간별 접근 로그 조회 성능 테스트
-- =========================================================


-- =========================================================
-- 0. 이전 성능 테스트 로그 삭제
-- 반복 실행 가능하도록 초기화
-- =========================================================

DELETE FROM access_logs
WHERE reason LIKE 'PF_PERF_LOG_%';


-- =========================================================
-- 1. 성능 테스트에 사용할 접근 요청 확인
--
-- 기존 Portfolio Core Test Data의
-- PF_TEST_SEARCH_01 ~ PF_TEST_SEARCH_06 사용
-- =========================================================

SELECT
    id,
    user_id,
    facility_id,
    request_reason
FROM access_requests
WHERE request_reason LIKE 'PF_TEST_SEARCH_%'
ORDER BY id;


-- =========================================================
-- 2. access_logs 100,000건 생성
--
-- - PF_TEST_SEARCH_01 ~ 06에 고르게 분산
-- - GRANTED 약 80%
-- - DENIED 약 20%
-- - created_at은 2026년 전체에 분산
-- =========================================================

INSERT INTO access_logs (
    user_id,
    facility_id,
    request_id,
    result,
    reason,
    created_at
)

WITH

-- 0 ~ 9
digits AS (
    SELECT 0 AS n
    UNION ALL SELECT 1
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
    UNION ALL SELECT 6
    UNION ALL SELECT 7
    UNION ALL SELECT 8
    UNION ALL SELECT 9
),

-- 0 ~ 99,999
numbers AS (
    SELECT
        d1.n
            + d2.n * 10
            + d3.n * 100
            + d4.n * 1000
            + d5.n * 10000 AS n
    FROM digits d1
             CROSS JOIN digits d2
             CROSS JOIN digits d3
             CROSS JOIN digits d4
             CROSS JOIN digits d5
),

-- 기존 검색 테스트 접근요청 6개에 순번 부여
requests AS (
    SELECT
        id,
        user_id,
        facility_id,
        ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM access_requests
    WHERE request_reason LIKE 'PF_TEST_SEARCH_%'
),

-- 접근요청 개수
request_count AS (
    SELECT COUNT(*) AS cnt
    FROM requests
)

SELECT
    r.user_id,
    r.facility_id,
    r.id,

    -- 약 80% GRANTED / 20% DENIED
    CASE
        WHEN MOD(n.n, 10) < 8
            THEN 'GRANTED'
        ELSE 'DENIED'
        END,

    -- 성능 테스트 데이터 식별용
    CONCAT(
            'PF_PERF_LOG_',
            LPAD(n.n, 6, '0')
    ),

    -- 2026년 전체에 created_at 분산
    TIMESTAMP('2026-01-01 00:00:00')
    + INTERVAL MOD(n.n * 317, 31536000) SECOND

FROM numbers n

    CROSS JOIN request_count rc

    JOIN requests r
ON r.rn = MOD(n.n, rc.cnt) + 1

WHERE rc.cnt > 0;


-- =========================================================
-- 3. 생성 결과 확인
-- =========================================================


-- 정확히 100,000건인지 확인
SELECT
    COUNT(*) AS total_logs
FROM access_logs
WHERE reason LIKE 'PF_PERF_LOG_%';


-- 시설별 데이터 분포 확인
SELECT
    f.id AS facility_id,
    f.name,
    COUNT(*) AS log_count
FROM access_logs al

         JOIN facilities f
              ON al.facility_id = f.id

WHERE al.reason LIKE 'PF_PERF_LOG_%'

GROUP BY
    f.id,
    f.name

ORDER BY log_count DESC;


-- GRANTED / DENIED 분포
SELECT
    result,
    COUNT(*) AS log_count
FROM access_logs

WHERE reason LIKE 'PF_PERF_LOG_%'

GROUP BY result;


-- 날짜 범위 확인
SELECT
    MIN(created_at) AS min_created_at,
    MAX(created_at) AS max_created_at
FROM access_logs
WHERE reason LIKE 'PF_PERF_LOG_%';