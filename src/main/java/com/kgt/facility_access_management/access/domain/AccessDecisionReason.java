package com.kgt.facility_access_management.access.domain;

public enum AccessDecisionReason {

    // 사용자 계정이 비활성 상태
    USER_INACTIVE,

    // 시설이 비활성 상태
    FACILITY_INACTIVE,

    // 해당 사용자와 시설에 대해 승인된 접근 신청이 없음
    NO_APPROVAL,

    // 승인된 접근 신청은 존재하지만 현재 시간이 접근 가능 기간이 아님
    OUTSIDE_VALID_PERIOD,

    // 사용자&시설 활성 + 승인 존재 + 유효기간 충족
    VALID_APPROVAL
}