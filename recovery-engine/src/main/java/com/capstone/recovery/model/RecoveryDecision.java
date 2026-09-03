package com.capstone.recovery.model;

public record RecoveryDecision(
        String service,
        boolean recoveryRequired,
        String severity,
        String reason,
        double latencySeconds,
        double cpuPercent,
        double heapMemoryBytes,
        double heapMemoryPercent,
        double errorRate,
        boolean podReady,
        int restartCount,
        int desiredReplicas,
        int availableReplicas,
        int readyReplicas,
        boolean deploymentHealthy
) {
}