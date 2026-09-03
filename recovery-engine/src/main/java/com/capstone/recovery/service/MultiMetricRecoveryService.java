package com.capstone.recovery.service;

import com.capstone.recovery.model.RecoveryDecision;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MultiMetricRecoveryService {

    private final KubernetesRecoveryService kubernetesRecoveryService;
    private final PrometheusMetricsService prometheusMetricsService;

    @Value("${recovery.threshold.latency-seconds:1.0}")
    private double latencyThresholdSeconds;

    @Value("${recovery.threshold.cpu-percent:80.0}")
    private double cpuThresholdPercent;

    @Value("${recovery.threshold.memory-percent:50.0}")
    private double memoryThresholdPercent;

    @Value("${recovery.threshold.error-rate:10.0}")
    private double errorRateThreshold;

    @Value("${recovery.minimum-unhealthy-signals:1}")
    private int minimumUnhealthySignals;

    public MultiMetricRecoveryService(
            KubernetesRecoveryService kubernetesRecoveryService,
            PrometheusMetricsService prometheusMetricsService) {

        this.kubernetesRecoveryService =
                kubernetesRecoveryService;

        this.prometheusMetricsService =
                prometheusMetricsService;
    }

    public RecoveryDecision evaluate(String serviceName)
            throws ApiException {

        V1Pod pod =
                kubernetesRecoveryService.findServicePod(
                        serviceName
                );

        V1Deployment deployment =
                kubernetesRecoveryService.getServiceDeployment(
                        serviceName
                );

        if (pod == null || deployment == null) {

            return new RecoveryDecision(
                    serviceName,
                    true,
                    "CRITICAL",
                    "Pod or deployment not found",
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    false,
                    0,
                    0,
                    0,
                    0,
                    false
            );
        }

        boolean podReady =
                kubernetesRecoveryService.isPodReady(pod);

        int restartCount =
                getRestartCount(pod);

        int desiredReplicas =
                deployment.getSpec() != null
                        && deployment.getSpec().getReplicas() != null
                        ? deployment.getSpec().getReplicas()
                        : 0;

        int availableReplicas =
                deployment.getStatus() != null
                        && deployment.getStatus().getAvailableReplicas() != null
                        ? deployment.getStatus().getAvailableReplicas()
                        : 0;

        int readyReplicas =
                deployment.getStatus() != null
                        && deployment.getStatus().getReadyReplicas() != null
                        ? deployment.getStatus().getReadyReplicas()
                        : 0;

        boolean deploymentHealthy =
                kubernetesRecoveryService.isDeploymentHealthy(
                        serviceName
                );

        double latency =
                prometheusMetricsService
                        .getAverageRequestLatency(serviceName);

        double cpu =
                prometheusMetricsService
                        .getCpuUsage(serviceName);

        double containerMemory =
                prometheusMetricsService
                        .getHeapMemoryUsage(serviceName);

        double containerMemoryPercent =
                prometheusMetricsService
                        .getHeapMemoryUsagePercent(serviceName);

        double errorRate =
                prometheusMetricsService
                        .getErrorRate(serviceName);

        /*
         * Kubernetes readiness is an immediate operational failure.
         */
        if (!podReady) {

            return new RecoveryDecision(
                    serviceName,
                    true,
                    "CRITICAL",
                    "Pod not Ready",
                    latency,
                    cpu,
                    containerMemory,
                    containerMemoryPercent,
                    errorRate,
                    false,
                    restartCount,
                    desiredReplicas,
                    availableReplicas,
                    readyReplicas,
                    deploymentHealthy
            );
        }

        /*
         * Deployment state is also an important operational signal.
         */
        if (!deploymentHealthy) {

            return new RecoveryDecision(
                    serviceName,
                    true,
                    "CRITICAL",
                    "Deployment replica state unhealthy",
                    latency,
                    cpu,
                    containerMemory,
                    containerMemoryPercent,
                    errorRate,
                    podReady,
                    restartCount,
                    desiredReplicas,
                    availableReplicas,
                    readyReplicas,
                    false
            );
        }

        /*
         * Missing Prometheus data must not be interpreted
         * as healthy or unhealthy.
         */
        boolean metricsAvailable =
                !Double.isNaN(latency)
                        && !Double.isNaN(cpu)
                        && !Double.isNaN(containerMemoryPercent)
                        && !Double.isNaN(errorRate);

        if (!metricsAvailable) {

            return new RecoveryDecision(
                    serviceName,
                    false,
                    "WARNING",
                    "Prometheus metrics unavailable",
                    latency,
                    cpu,
                    containerMemory,
                    containerMemoryPercent,
                    errorRate,
                    podReady,
                    restartCount,
                    desiredReplicas,
                    availableReplicas,
                    readyReplicas,
                    deploymentHealthy
            );
        }

        int unhealthySignals = 0;

        StringBuilder reason =
                new StringBuilder();

        /*
         * Latency signal.
         */
        if (latency > latencyThresholdSeconds) {

            unhealthySignals++;

            reason.append(
                    "High latency ("
                            + latency
                            + "s); "
            );
        }

        /*
         * CPU signal.
         */
        if (cpu > cpuThresholdPercent) {

            unhealthySignals++;

            reason.append(
                    "High CPU ("
                            + cpu
                            + "%); "
            );
        }

        /*
         * Container memory signal.
         */
        if (containerMemoryPercent > memoryThresholdPercent) {

            unhealthySignals++;

            reason.append(
                    "High container memory ("
                            + containerMemoryPercent
                            + "%); "
            );
        }

        /*
         * HTTP error-rate signal.
         */
        if (errorRate > errorRateThreshold) {

            unhealthySignals++;

            reason.append(
                    "High error rate ("
                            + errorRate
                            + "%); "
            );
        }

        /*
         * Severity classification.
         */
        String severity;

        if (unhealthySignals == 0) {

            severity = "HEALTHY";

        } else if (unhealthySignals == 1) {

            severity = "WARNING";

        } else if (unhealthySignals == 2) {

            severity = "DEGRADED";

        } else {

            severity = "CRITICAL";
        }

        boolean recoveryRequired =
                unhealthySignals >= minimumUnhealthySignals;

        if (!recoveryRequired) {

            reason.append(
                    "Metrics within healthy range"
            );
        }

        return new RecoveryDecision(
                serviceName,
                recoveryRequired,
                severity,
                reason.toString(),
                latency,
                cpu,
                containerMemory,
                containerMemoryPercent,
                errorRate,
                podReady,
                restartCount,
                desiredReplicas,
                availableReplicas,
                readyReplicas,
                deploymentHealthy
        );
    }

    private int getRestartCount(V1Pod pod) {

        if (pod == null
                || pod.getStatus() == null
                || pod.getStatus().getContainerStatuses() == null) {

            return 0;
        }

        return pod.getStatus()
                .getContainerStatuses()
                .stream()
                .mapToInt(status ->
                        status.getRestartCount() != null
                                ? status.getRestartCount()
                                : 0
                )
                .sum();
    }
}