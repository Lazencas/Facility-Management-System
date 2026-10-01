package com.kgt.facility_access_management.access.domain;

import java.time.LocalDateTime;

public class AccessLog {

    private Long id;

    private Long userId;
    private Long facilityId;
    private Long accessRequestId;

    private AccessResult result;
    private AccessDecisionReason decisionReason;

    private LocalDateTime accessedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public Long getAccessRequestId() {
        return accessRequestId;
    }

    public void setAccessRequestId(Long accessRequestId) {
        this.accessRequestId = accessRequestId;
    }

    public AccessResult getResult() {
        return result;
    }

    public void setResult(AccessResult result) {
        this.result = result;
    }

    public AccessDecisionReason getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(
            AccessDecisionReason decisionReason
    ) {
        this.decisionReason = decisionReason;
    }

    public LocalDateTime getAccessedAt() {
        return accessedAt;
    }

    public void setAccessedAt(LocalDateTime accessedAt) {
        this.accessedAt = accessedAt;
    }
}