package com.kgt.facility_access_management.access.dto;

import java.time.LocalDateTime;

public class AccessRequestCreateForm {
    private Long facilityId;
    private String requestReason;
    private LocalDateTime accessStartAt;
    private LocalDateTime accessEndAt;

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getRequestReason() {
        return requestReason;
    }

    public void setRequestReason(String requestReason) {
        this.requestReason = requestReason;
    }

    public LocalDateTime getAccessStartAt() {
        return accessStartAt;
    }

    public void setAccessStartAt(LocalDateTime accessStartAt) {
        this.accessStartAt = accessStartAt;
    }

    public LocalDateTime getAccessEndAt() {
        return accessEndAt;
    }

    public void setAccessEndAt(LocalDateTime accessEndAt) {
        this.accessEndAt = accessEndAt;
    }
}
