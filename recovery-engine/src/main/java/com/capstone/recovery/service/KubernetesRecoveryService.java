package com.capstone.recovery.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KubernetesRecoveryService {

    private final CoreV1Api coreV1Api;
    private final AppsV1Api appsV1Api;

    public KubernetesRecoveryService(ApiClient apiClient) {

        this.coreV1Api = new CoreV1Api(apiClient);
        this.appsV1Api = new AppsV1Api(apiClient);
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

    /**
     * Returns the deployment associated with the service.
     */
    public V1Deployment getServiceDeployment(
            String serviceName)
            throws ApiException {

        return appsV1Api
                .readNamespacedDeployment(
                        serviceName,
                        "default"
                )
                .execute();
    }

    /**
     * Returns the desired replica count.
     */
    public int getDesiredReplicas(
            String serviceName)
            throws ApiException {

        V1Deployment deployment =
                getServiceDeployment(serviceName);

        if (deployment.getSpec() == null
                || deployment.getSpec().getReplicas() == null) {

            return 0;
        }

        return deployment.getSpec().getReplicas();
    }

    /**
     * Returns the currently available replica count.
     */
    public int getAvailableReplicas(
            String serviceName)
            throws ApiException {

        V1Deployment deployment =
                getServiceDeployment(serviceName);

        if (deployment.getStatus() == null
                || deployment.getStatus().getAvailableReplicas() == null) {

            return 0;
        }

        return deployment
                .getStatus()
                .getAvailableReplicas();
    }

    /**
     * Returns the currently ready replica count.
     */
    public int getReadyReplicas(
            String serviceName)
            throws ApiException {

        V1Deployment deployment =
                getServiceDeployment(serviceName);

        if (deployment.getStatus() == null
                || deployment.getStatus().getReadyReplicas() == null) {

            return 0;
        }

        return deployment
                .getStatus()
                .getReadyReplicas();
    }

    /**
     * Checks whether the service deployment has
     * all desired replicas available.
     */
    public boolean isDeploymentHealthy(
            String serviceName)
            throws ApiException {

        int desired =
                getDesiredReplicas(serviceName);

        int available =
                getAvailableReplicas(serviceName);

        int ready =
                getReadyReplicas(serviceName);

        return desired > 0
                && available >= desired
                && ready >= desired;
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