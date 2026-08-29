package com.capstone.recovery.controller;

import com.capstone.recovery.model.PodStatusResponse;
import com.capstone.recovery.service.KubernetesRecoveryService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/recovery")
public class RecoveryController {

    private final KubernetesRecoveryService kubernetesRecoveryService;

    public RecoveryController(KubernetesRecoveryService kubernetesRecoveryService) {
        this.kubernetesRecoveryService = kubernetesRecoveryService;
    }

    @GetMapping("/pods")
    public List<PodStatusResponse> getPods() throws ApiException {
        return kubernetesRecoveryService.getDefaultNamespacePods()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/inventory")
    public PodStatusResponse getInventoryStatus() throws ApiException {

        return kubernetesRecoveryService.getDefaultNamespacePods()
                .stream()
                .filter(pod -> {
                    if (pod.getMetadata() == null ||
                        pod.getMetadata().getLabels() == null) {
                        return false;
                    }

                    return "inventory-service".equals(
                            pod.getMetadata().getLabels().get("app")
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

    private PodStatusResponse toResponse(V1Pod pod) {

        String podName = pod.getMetadata().getName();

        String status = pod.getStatus() != null
                ? pod.getStatus().getPhase()
                : "UNKNOWN";

        String service = pod.getMetadata().getLabels() != null
                ? pod.getMetadata().getLabels()
                    .getOrDefault("app", "unknown")
                : "unknown";

        boolean healthy = "Running".equalsIgnoreCase(status);

        return new PodStatusResponse(
                service,
                podName,
                status,
                healthy
        );
    }
}