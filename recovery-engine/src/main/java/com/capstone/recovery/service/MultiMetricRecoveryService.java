package com.capstone.recovery.service;

import com.capstone.recovery.model.RecoveryDecision;
import io.kubernetes.client.openapi.ApiException;
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

    @Value("${recovery.threshold.memory-percent:85.0}")
    private double memoryThresholdPercent;

    @Value("${recovery.threshold.error-rate:0.10}")
    private double errorRateThreshold;

    @Value("${recovery.minimum-unhealthy-signals:2}")
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

        if (pod == null) {

            return new RecoveryDecision(
                    serviceName,
                    true,
                    "Pod not found",
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    false,
                    0
            );
        }

        boolean podReady =
                kubernetesRecoveryService.isPodReady(pod);

        int restartCount =
                getRestartCount(pod);

        double latency =
                prometheusMetricsService
                        .getAverageRequestLatency(serviceName);

        double cpu =
                prometheusMetricsService
                        .getCpuUsage(serviceName);

        double heapMemory =
                prometheusMetricsService
                        .getHeapMemoryUsage(serviceName);

        double heapMemoryPercent =
                prometheusMetricsService
                        .getHeapMemoryUsagePercent(serviceName);

        double errorRate =
                prometheusMetricsService
                        .getErrorRate(serviceName);

        /*
         * Direct Kubernetes operational signal.
         */
        if (!podReady) {

            return new RecoveryDecision(
                    serviceName,
                    true,
                    "Pod not Ready",
                    latency,
                    cpu,
                    heapMemory,
                    heapMemoryPercent,
                    errorRate,
                    false,
                    restartCount
            );
        }

        /*
         * All Prometheus metrics required for metric-driven
         * recovery must be available.
         *
         * Missing data must never be interpreted as healthy
         * or unhealthy.
         */
        boolean metricsAvailable =
                !Double.isNaN(latency)
                        && !Double.isNaN(cpu)
                        && !Double.isNaN(heapMemoryPercent)
                        && !Double.isNaN(errorRate);

        if (!metricsAvailable) {

            return new RecoveryDecision(
                    serviceName,
                    false,
                    "Prometheus metrics unavailable",
                    latency,
                    cpu,
                    heapMemory,
                    heapMemoryPercent,
                    errorRate,
                    true,
                    restartCount
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
         * Heap-memory signal.
         */
        if (heapMemoryPercent
                > memoryThresholdPercent) {

            unhealthySignals++;

            reason.append(
                    "High heap memory ("
                            + heapMemoryPercent
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
                            + "); "
            );
        }

        /*
         * Metric-driven recovery requires multiple
         * independent abnormal signals.
         */
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
                reason.toString(),
                latency,
                cpu,
                heapMemory,
                heapMemoryPercent,
                errorRate,
                true,
                restartCount
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