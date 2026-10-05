USE facility_access_management;


-- =========================================================
-- Portfolio Core Test Data
-- 1. 상태전이 / 동시 승인
-- 2. Session / 권한
-- 3. MyBatis 동적검색
-- =========================================================


-- =========================================================
-- 0. 기존 포트폴리오 테스트 데이터 정리
-- =========================================================

DELETE FROM access_logs
WHERE access_request_id IN (
    SELECT id
    FROM access_requests
    WHERE request_reason LIKE 'PF_TEST_%'
);

DELETE FROM access_requests
WHERE request_reason LIKE 'PF_TEST_%';

DELETE FROM facilities
WHERE name IN (
               'PF_STATE_FACILITY',
               'PF_SEARCH_FACILITY_A',
               'PF_SEARCH_FACILITY_B'
    );

DELETE FROM users
WHERE login_id IN (
                   'pf_user_a',
                   'pf_user_b',
                   'pf_admin_a',
                   'pf_admin_b',
                   'pf_inactive'
    );


-- =========================================================
-- 1. 사용자
-- =========================================================

INSERT INTO users (
    login_id,
    password_hash,
    name,
    role,
    active
)
VALUES
    ('pf_user_a',   '1234', '포트폴리오 사용자 A', 'USER',  TRUE),
    ('pf_user_b',   '1234', '포트폴리오 사용자 B', 'USER',  TRUE),

    -- 동시 승인 테스트를 위해 관리자 2명
    ('pf_admin_a',  '1234', '포트폴리오 관리자 A', 'ADMIN', TRUE),
    ('pf_admin_b',  '1234', '포트폴리오 관리자 B', 'ADMIN', TRUE),

    -- 로그인 예외 테스트용
    ('pf_inactive', '1234', '비활성 사용자',       'USER',  FALSE);


SET @user_a = (
    SELECT id FROM users WHERE login_id = 'pf_user_a'
);

SET @user_b = (
    SELECT id FROM users WHERE login_id = 'pf_user_b'
);

SET @admin_a = (
    SELECT id FROM users WHERE login_id = 'pf_admin_a'
);

SET @admin_b = (
    SELECT id FROM users WHERE login_id = 'pf_admin_b'
);


-- =========================================================
-- 2. 시설
-- =========================================================

INSERT INTO facilities (
    name,
    location,
    description,
    active
)
VALUES
    (
        'PF_STATE_FACILITY',
        'PORTFOLIO-STATE',
        '상태전이 테스트용 시설',
        TRUE
    ),
    (
        'PF_SEARCH_FACILITY_A',
        'PORTFOLIO-SEARCH-A',
        '동적검색 테스트 시설 A',
        TRUE
    ),
    (
        'PF_SEARCH_FACILITY_B',
        'PORTFOLIO-SEARCH-B',
        '동적검색 테스트 시설 B',
        TRUE
    );


SET @state_facility = (
    SELECT id
    FROM facilities
    WHERE name = 'PF_STATE_FACILITY'
);

SET @search_facility_a = (
    SELECT id
    FROM facilities
    WHERE name = 'PF_SEARCH_FACILITY_A'
);

SET @search_facility_b = (
    SELECT id
    FROM facilities
    WHERE name = 'PF_SEARCH_FACILITY_B'
);


-- =========================================================
-- 3. 문제해결 ①
-- 상태전이 / 동시 승인 테스트
-- =========================================================


-- [TEST 1]
-- 두 관리자가 동시에 승인할 대상
INSERT INTO access_requests (
    user_id,
    facility_id,
    request_reason,
    access_start_at,
    access_end_at,
    status,
    created_at
)
VALUES (
           @user_a,
           @state_facility,
           'PF_TEST_STATE_CONCURRENT_APPROVE',
           '2026-10-10 09:00:00',
           '2026-10-10 18:00:00',
           'PENDING',
           '2026-10-05 09:00:00'
       );


-- [TEST 2]
-- 이미 승인된 요청 재승인 방지
INSERT INTO access_requests (
    user_id,
    facility_id,
    request_reason,
    access_start_at,
    access_end_at,
    status,
    reviewed_by,
    reviewed_at,
    created_at
)
VALUES (
           @user_a,
           @state_facility,
           'PF_TEST_STATE_ALREADY_APPROVED',
           '2026-10-11 09:00:00',
           '2026-10-11 18:00:00',
           'APPROVED',
           @admin_a,
           '2026-10-05 10:00:00',
           '2026-10-05 09:00:00'
       );


-- [TEST 3]
-- 반려 대상
INSERT INTO access_requests (
    user_id,
    facility_id,
    request_reason,
    access_start_at,
    access_end_at,
    status,
    created_at
)
VALUES (
           @user_a,
           @state_facility,
           'PF_TEST_STATE_REJECT',
           '2026-10-12 09:00:00',
           '2026-10-12 18:00:00',
           'PENDING',
           '2026-10-05 09:00:00'
       );


-- [TEST 4]
-- 사용자 본인 취소 대상
INSERT INTO access_requests (
    user_id,
    facility_id,
    request_reason,
    access_start_at,
    access_end_at,
    status,
    created_at
)
VALUES (
           @user_a,
           @state_facility,
           'PF_TEST_STATE_CANCEL',
           '2026-10-13 09:00:00',
           '2026-10-13 18:00:00',
           'PENDING',
           '2026-10-05 09:00:00'
       );


-- [TEST 5]
-- 관리자 본인 신청 승인 방지
INSERT INTO access_requests (
    user_id,
    facility_id,
    request_reason,
    access_start_at,
    access_end_at,
    status,
    created_at
)
VALUES (
           @admin_a,
           @state_facility,
           'PF_TEST_STATE_SELF_APPROVAL',
           '2026-10-14 09:00:00',
           '2026-10-14 18:00:00',
           'PENDING',
           '2026-10-05 09:00:00'
       );


-- =========================================================
-- 4. 문제해결 ③
-- MyBatis 동적검색 테스트 데이터
--
-- USER / 시설 / 상태 / 기간 조합이 명확히 갈리도록 생성
-- =========================================================


-- USER A + 시설 A + PENDING + 9월
INSERT INTO access_requests (
    user_id,
    facility_id,
    request_reason,
    access_start_at,
    access_end_at,
    status,
    created_at
)
VALUES (
           @user_a,
           @search_facility_a,
           'PF_TEST_SEARCH_01',
           '2026-09-05 09:00:00',
           '2026-09-05 18:00:00',
           'PENDING',
           '2026-09-01 10:00:00'
       );


-- USER A + 시설 A + APPROVED + 9월
INSERT INTO access_requests (
    user_id,
    facility_id,
    request_reason,
    access_start_at,
    access_end_at,
    status,
    reviewed_by,
    reviewed_at,
    created_at
)
VALUES (
           @user_a,
           @search_facility_a,
           'PF_TEST_SEARCH_02',
           '2026-09-10 09:00:00',
           '2026-09-10 18:00:00',
           'APPROVED',
           @admin_a,
           '2026-09-03 12:00:00',
           '2026-09-02 10:00:00'
       );


-- USER A + 시설 B + REJECTED + 9월
INSERT INTO access_requests (
    user_id,
    facility_id,
    request_reason,
    access_start_at,
    access_end_at,
    status,
    reviewed_by,
    reviewed_at,
    reject_reason,
    created_at
)
VALUES (
           @user_a,
           @search_facility_b,
           'PF_TEST_SEARCH_03',
           '2026-09-20 09:00:00',
           '2026-09-20 18:00:00',
           'REJECTED',
           @admin_a,
           '2026-09-04 12:00:00',
           '테스트 반려',
           '2026-09-03 10:00:00'
       );


-- USER B + 시설 A + CANCELLED + 10월
INSERT INTO access_requests (
    user_id,
    facility_id,
    request_reason,
    access_start_at,
    access_end_at,
    status,
    created_at
)
VALUES (
           @user_b,
           @search_facility_a,
           'PF_TEST_SEARCH_04',
           '2026-10-05 09:00:00',
           '2026-10-05 18:00:00',
           'CANCELLED',
           '2026-10-01 10:00:00'
       );


-- USER B + 시설 B + PENDING + 10월
INSERT INTO access_requests (
    user_id,
    facility_id,
    request_reason,
    access_start_at,
    access_end_at,
    status,
    created_at
)
VALUES (
           @user_b,
           @search_facility_b,
           'PF_TEST_SEARCH_05',
           '2026-10-10 09:00:00',
           '2026-10-10 18:00:00',
           'PENDING',
           '2026-10-02 10:00:00'
       );


-- USER B + 시설 B + APPROVED + 10월
INSERT INTO access_requests (
    user_id,
    facility_id,
    request_reason,
    access_start_at,
    access_end_at,
    status,
    reviewed_by,
    reviewed_at,
    created_at
)
VALUES (
           @user_b,
           @search_facility_b,
           'PF_TEST_SEARCH_06',
           '2026-10-20 09:00:00',
           '2026-10-20 18:00:00',
           'APPROVED',
           @admin_b,
           '2026-10-04 12:00:00',
           '2026-10-03 10:00:00'
       );


-- =========================================================
-- 5. 생성 결과 확인
-- =========================================================

SELECT
    id,
    login_id,
    name,
    role,
    active
FROM users
WHERE login_id LIKE 'pf_%'
ORDER BY id;


SELECT
    id,
    name,
    location
FROM facilities
WHERE name LIKE 'PF_%'
ORDER BY id;


SELECT
    ar.id,
    u.login_id,
    f.name AS facility_name,
    ar.request_reason,
    ar.status,
    ar.access_start_at,
    ar.access_end_at,
    ar.reviewed_by
FROM access_requests ar
         JOIN users u
              ON ar.user_id = u.id
         JOIN facilities f
              ON ar.facility_id = f.id
WHERE ar.request_reason LIKE 'PF_TEST_%'
ORDER BY ar.id;