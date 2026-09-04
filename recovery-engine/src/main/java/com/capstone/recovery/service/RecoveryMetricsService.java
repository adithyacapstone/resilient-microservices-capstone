package com.capstone.recovery.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecoveryMetricsService {

    private static final String[] SERVICES = {
            "product-service",
            "inventory-service",
            "order-service"
    };

    private final Map<String, Double> recoveryRequired = new ConcurrentHashMap<>();
    private final Map<String, Double> severity = new ConcurrentHashMap<>();
    private final Map<String, Double> unhealthyEvaluations = new ConcurrentHashMap<>();
    private final Map<String, Double> deploymentHealthy = new ConcurrentHashMap<>();
    private final Map<String, Double> desiredReplicas = new ConcurrentHashMap<>();
    private final Map<String, Double> availableReplicas = new ConcurrentHashMap<>();
    private final Map<String, Double> readyReplicas = new ConcurrentHashMap<>();

    private final Map<String, String> selectedStrategy =
            new ConcurrentHashMap<>();

    private final Counter recoveryAttempts;
    private final Counter successfulRecoveries;

    public RecoveryMetricsService(MeterRegistry meterRegistry) {

        recoveryAttempts = Counter.builder(
                        "recovery_engine_recovery_attempts_total")
                .description("Total number of recovery attempts")
                .register(meterRegistry);

        successfulRecoveries = Counter.builder(
                        "recovery_engine_successful_recoveries_total")
                .description("Total number of successful recoveries")
                .register(meterRegistry);

        for (String service : SERVICES) {

            recoveryRequired.put(service, 0.0);
            severity.put(service, 0.0);
            unhealthyEvaluations.put(service, 0.0);
            deploymentHealthy.put(service, 1.0);
            desiredReplicas.put(service, 0.0);
            availableReplicas.put(service, 0.0);
            readyReplicas.put(service, 0.0);
            selectedStrategy.put(service, "NO_ACTION");

            registerServiceGauge(
                    meterRegistry,
                    "recovery_engine_recovery_required",
                    "Recovery required status",
                    service,
                    recoveryRequired
            );

            registerServiceGauge(
                    meterRegistry,
                    "recovery_engine_severity",
                    "Recovery severity level: HEALTHY=0, WARNING=1, DEGRADED=2, CRITICAL=3",
                    service,
                    severity
            );

            registerServiceGauge(
                    meterRegistry,
                    "recovery_engine_unhealthy_evaluations",
                    "Consecutive unhealthy evaluation count",
                    service,
                    unhealthyEvaluations
            );

            registerServiceGauge(
                    meterRegistry,
                    "recovery_engine_deployment_healthy",
                    "Deployment health status",
                    service,
                    deploymentHealthy
            );

            registerServiceGauge(
                    meterRegistry,
                    "recovery_engine_desired_replicas",
                    "Desired replica count",
                    service,
                    desiredReplicas
            );

            registerServiceGauge(
                    meterRegistry,
                    "recovery_engine_available_replicas",
                    "Available replica count",
                    service,
                    availableReplicas
            );

            registerServiceGauge(
                    meterRegistry,
                    "recovery_engine_ready_replicas",
                    "Ready replica count",
                    service,
                    readyReplicas
            );

            Gauge.builder(
                            "recovery_engine_strategy",
                            selectedStrategy,
                            map -> strategyValue(
                                    map.getOrDefault(service, "NO_ACTION")
                            )
                    )
                    .description("Selected recovery strategy")
                    .tag("service", service)
                    .register(meterRegistry);
        }
    }

    private void registerServiceGauge(
            MeterRegistry meterRegistry,
            String metricName,
            String description,
            String service,
            Map<String, Double> values
    ) {
        Gauge.builder(
                        metricName,
                        values,
                        map -> map.getOrDefault(service, 0.0)
                )
                .description(description)
                .tag("service", service)
                .register(meterRegistry);
    }

    private double severityValue(String severityName) {

        if (severityName == null) {
            return 0.0;
        }

        return switch (severityName) {
            case "WARNING" -> 1.0;
            case "DEGRADED" -> 2.0;
            case "CRITICAL" -> 3.0;
            default -> 0.0;
        };
    }

    private double strategyValue(String strategy) {

        if (strategy == null) {
            return 0.0;
        }

        return switch (strategy) {
            case "POD_RESTART" -> 1.0;
            case "DEPLOYMENT_RECOVERY" -> 2.0;
            default -> 0.0;
        };
    }

    public void updateRecoveryState(
            String service,
            boolean recoveryRequiredValue,
            String severityValueName,
            int unhealthyEvaluationCount,
            boolean deploymentHealthyValue,
            int desiredReplicaCount,
            int availableReplicaCount,
            int readyReplicaCount
    ) {
        recoveryRequired.put(
                service,
                recoveryRequiredValue ? 1.0 : 0.0
        );

        severity.put(
                service,
                severityValue(severityValueName)
        );

        unhealthyEvaluations.put(
                service,
                (double) unhealthyEvaluationCount
        );

        deploymentHealthy.put(
                service,
                deploymentHealthyValue ? 1.0 : 0.0
        );

        desiredReplicas.put(
                service,
                (double) desiredReplicaCount
        );

        availableReplicas.put(
                service,
                (double) availableReplicaCount
        );

        readyReplicas.put(
                service,
                (double) readyReplicaCount
        );
    }

    public void setSelectedStrategy(
            String service,
            String strategy
    ) {
        selectedStrategy.put(
                service,
                strategy == null ? "NO_ACTION" : strategy
        );
    }

    public void recordRecoveryAttempt() {
        recoveryAttempts.increment();
    }

    public void recordSuccessfulRecovery() {
        successfulRecoveries.increment();
    }

    public String getSelectedStrategy(String service) {
        return selectedStrategy.getOrDefault(
                service,
                "NO_ACTION"
        );
    }
}