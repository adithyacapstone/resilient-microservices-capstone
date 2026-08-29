package com.capstone.recovery.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KubernetesRecoveryService {

    private final CoreV1Api coreV1Api;

    public KubernetesRecoveryService(ApiClient apiClient) {
        this.coreV1Api = new CoreV1Api(apiClient);
    }

    public List<V1Pod> getDefaultNamespacePods()
            throws ApiException {

        V1PodList podList =
                coreV1Api.listNamespacedPod("default").execute();

        return podList.getItems();
    }

    public V1Pod findServicePod(String serviceName)
            throws ApiException {

        return getDefaultNamespacePods()
                .stream()
                .filter(pod ->
                        pod.getMetadata() != null
                                && pod.getMetadata().getLabels() != null
                                && serviceName.equals(
                                        pod.getMetadata()
                                                .getLabels()
                                                .get("app")
                                )
                )
                .findFirst()
                .orElse(null);
    }

    /*
     * Check whether Kubernetes considers the pod Ready.
     *
     * A pod being Running does not necessarily mean
     * that the application is ready to receive traffic.
     */
    public boolean isPodReady(V1Pod pod) {

        if (pod == null
                || pod.getStatus() == null
                || pod.getStatus().getConditions() == null) {

            return false;
        }

        return pod.getStatus()
                .getConditions()
                .stream()
                .anyMatch(condition ->
                        "Ready".equals(condition.getType())
                                && "True".equalsIgnoreCase(
                                        condition.getStatus()
                                )
                );
    }

    public void deleteServicePod(String podName)
            throws ApiException {

        coreV1Api.deleteNamespacedPod(
                podName,
                "default"
        ).execute();
    }
}