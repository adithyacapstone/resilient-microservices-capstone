package com.capstone.recovery.controller;

import com.capstone.recovery.service.KubernetesRecoveryService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recovery/action")
public class RecoveryActionController {

    private final KubernetesRecoveryService kubernetesRecoveryService;

    public RecoveryActionController(
            KubernetesRecoveryService kubernetesRecoveryService) {

        this.kubernetesRecoveryService =
                kubernetesRecoveryService;
    }

    @PostMapping("/{service}/restart")
    public ResponseEntity<String> restartService(
            @PathVariable String service)
            throws ApiException {

        String serviceName =
                service + "-service";

        V1Pod servicePod =
                kubernetesRecoveryService.findServicePod(
                        serviceName
                );

        if (servicePod == null) {

            return ResponseEntity.notFound().build();
        }

        String podName =
                servicePod.getMetadata().getName();

        kubernetesRecoveryService.deleteServicePod(
                podName
        );

        return ResponseEntity.ok(
                "Recovery action executed. Deleted "
                        + serviceName
                        + " pod: "
                        + podName
        );
    }
}