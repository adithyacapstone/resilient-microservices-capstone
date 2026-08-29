package com.capstone.recovery.model;

public record PodStatusResponse(
        String service,
        String pod,
        String status,
        boolean healthy
) {
}