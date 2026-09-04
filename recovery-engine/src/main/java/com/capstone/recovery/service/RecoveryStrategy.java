package com.capstone.recovery.service;

import com.capstone.recovery.model.RecoveryDecision;
import org.springframework.stereotype.Service;

@Service
public class RecoveryStrategy {

    public String selectStrategy(RecoveryDecision decision) {

        if (decision == null) {
            return "NO_ACTION";
        }

        if (!decision.recoveryRequired()) {
            return "NO_ACTION";
        }

        if (!decision.podReady()) {
            return "POD_RESTART";
        }

        if (!decision.deploymentHealthy()) {
            return "DEPLOYMENT_RECOVERY";
        }

        return switch (decision.severity()) {
            case "CRITICAL" -> "POD_RESTART";
            case "DEGRADED" -> "POD_RESTART";
            case "WARNING" -> "POD_RESTART";
            default -> "NO_ACTION";
        };
    }
}