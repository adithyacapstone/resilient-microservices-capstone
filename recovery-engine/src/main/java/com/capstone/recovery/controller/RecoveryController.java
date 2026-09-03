package com.capstone.recovery.controller;

import com.capstone.recovery.model.PodStatusResponse;
import com.capstone.recovery.model.RecoveryDecision;
import com.capstone.recovery.service.KubernetesRecoveryService;
import com.capstone.recovery.service.MultiMetricRecoveryService;
import com.capstone.recovery.service.PrometheusMetricsService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recovery")
public class RecoveryController {

    private final KubernetesRecoveryService kubernetesRecoveryService;
    private final PrometheusMetricsService prometheusMetricsService;
    private final MultiMetricRecoveryService multiMetricRecoveryService;

    public RecoveryController(
            KubernetesRecoveryService kubernetesRecoveryService,
            PrometheusMetricsService prometheusMetricsService,
            MultiMetricRecoveryService multiMetricRecoveryService) {

        this.kubernetesRecoveryService = kubernetesRecoveryService;
        this.prometheusMetricsService = prometheusMetricsService;
        this.multiMetricRecoveryService = multiMetricRecoveryService;
    }

    @GetMapping("/pods")
    public List<PodStatusResponse> getPods() throws ApiException {

        return kubernetesRecoveryService
                .getDefaultNamespacePods()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/inventory")
    public PodStatusResponse getInventoryStatus()
            throws ApiException {

        return kubernetesRecoveryService
                .getDefaultNamespacePods()
                .stream()
                .filter(pod -> {

                    if (pod.getMetadata() == null
                            || pod.getMetadata().getLabels() == null) {
                        return false;
                    }

                    return "inventory-service".equals(
                            pod.getMetadata()
                                    .getLabels()
                                    .get("app")
                    );
                })
                .findFirst()
                .map(this::toResponse)
                .orElseGet(() ->
                        new PodStatusResponse(
                                "inventory-service",
                                "NOT_FOUND",
                                "NOT_FOUND",
                                false
                        )
                );
    }

    @GetMapping("/metrics/{service}")
    public Map<String, Object> getMetrics(
            @PathVariable String service) {

        Map<String, Object> metrics =
                new LinkedHashMap<>();

        metrics.put("service", service);

        metrics.put(
                "latencySeconds",
                prometheusMetricsService
                        .getAverageRequestLatency(service)
        );

        metrics.put(
                "cpuPercent",
                prometheusMetricsService
                        .getCpuUsage(service)
        );

        metrics.put(
                "heapMemoryBytes",
                prometheusMetricsService
                        .getHeapMemoryUsage(service)
        );

        metrics.put(
                "errorRate",
                prometheusMetricsService
                        .getErrorRate(service)
        );

        return metrics;
    }

    @GetMapping("/decision/{service}")
    public RecoveryDecision getRecoveryDecision(
            @PathVariable String service)
            throws ApiException {

        return multiMetricRecoveryService.evaluate(
                service
        );
    }

    private PodStatusResponse toResponse(V1Pod pod) {

        String podName =
                pod.getMetadata().getName();

        String status =
                pod.getStatus() != null
                        ? pod.getStatus().getPhase()
                        : "UNKNOWN";

        String service =
                pod.getMetadata().getLabels() != null
                        ? pod.getMetadata()
                                .getLabels()
                                .getOrDefault(
                                        "app",
                                        "unknown"
                                )
                        : "unknown";

        boolean healthy =
                "Running".equalsIgnoreCase(status);

        return new PodStatusResponse(
                service,
                podName,
                status,
                healthy
        );
    }
}