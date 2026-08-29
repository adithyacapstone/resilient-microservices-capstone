package com.capstone.recovery.model;

public record RecoveryDecision(
        String service,
        boolean recoveryRequired,
        String reason
) {
}