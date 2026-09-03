package com.capstone.recovery.monitor;

import com.capstone.recovery.model.RecoveryDecision;
import com.capstone.recovery.service.KubernetesRecoveryService;
import com.capstone.recovery.service.MultiMetricRecoveryService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RecoveryMonitor {

    private final KubernetesRecoveryService kubernetesRecoveryService;
    private final MultiMetricRecoveryService multiMetricRecoveryService;

    /*
     * Services monitored by the Recovery Engine.
     */
    private final Map<String, String> monitoredServices =
            new LinkedHashMap<>();

    /*
     * Previous known pod for each service.
     */
    private final Map<String, String> previousPodNames =
            new LinkedHashMap<>();

    /*
     * Previous restart count for each service.
     */
    private final Map<String, Integer> previousRestartCounts =
            new LinkedHashMap<>();

    /*
     * Prevent repeated recovery actions while recovery
     * is already in progress for a service.
     */
    private final Map<String, Boolean> recoveryInProgress =
            new LinkedHashMap<>();

    /*
     * Consecutive unhealthy metric evaluations.
     */
    private final Map<String, Integer> consecutiveUnhealthyEvaluations =
            new LinkedHashMap<>();

    /*
     * Number of consecutive unhealthy evaluations required
     * before metric-driven recovery is triggered.
     */
    @Value("${recovery.consecutive-unhealthy-evaluations:3}")
    private int requiredConsecutiveUnhealthyEvaluations;

    public RecoveryMonitor(
            KubernetesRecoveryService kubernetesRecoveryService,
            MultiMetricRecoveryService multiMetricRecoveryService) {

        this.kubernetesRecoveryService =
                kubernetesRecoveryService;

        this.multiMetricRecoveryService =
                multiMetricRecoveryService;

        monitoredServices.put(
                "product-service",
                "product-service"
        );

        monitoredServices.put(
                "inventory-service",
                "inventory-service"
        );

        monitoredServices.put(
                "order-service",
                "order-service"
        );

        for (String serviceName : monitoredServices.keySet()) {

            recoveryInProgress.put(
                    serviceName,
                    false
            );

            consecutiveUnhealthyEvaluations.put(
                    serviceName,
                    0
            );
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void monitorServices() {

        for (String serviceName : monitoredServices.keySet()) {

            monitorService(serviceName);
        }
    }

    private void monitorService(String serviceName) {

        try {

            V1Pod servicePod =
                    kubernetesRecoveryService.findServicePod(
                            monitoredServices.get(serviceName)
                    );

            /*
             * Pod not found.
             */
            if (servicePod == null) {

                System.out.println(
                        "[RECOVERY ENGINE] "
                                + serviceName
                                + " pod NOT FOUND"
                );

                if (!recoveryInProgress.get(serviceName)) {

                    recoveryInProgress.put(
                            serviceName,
                            true
                    );

                    System.out.println(
                            "[RECOVERY ENGINE] RECOVERY REQUIRED: "
                                    + serviceName
                    );

                    System.out.println(
                            "[RECOVERY ENGINE] Starting automatic recovery: "
                                    + serviceName
                    );

                    performRecovery(serviceName);
                }

                return;
            }

            String currentPodName =
                    servicePod.getMetadata().getName();

            String status =
                    servicePod.getStatus() != null
                            ? servicePod.getStatus().getPhase()
                            : "UNKNOWN";

            boolean podReady =
                    kubernetesRecoveryService.isPodReady(
                            servicePod
                    );

            int restartCount =
                    getRestartCount(servicePod);

            /*
             * First observation.
             */
            String previousPodName =
                    previousPodNames.get(serviceName);

            Integer previousRestartCount =
                    previousRestartCounts.get(serviceName);

            if (previousPodName == null) {

                previousPodNames.put(
                        serviceName,
                        currentPodName
                );

                previousRestartCounts.put(
                        serviceName,
                        restartCount
                );

                System.out.println(
                        "[RECOVERY ENGINE] Initial "
                                + serviceName
                                + " pod detected: "
                                + currentPodName
                );

                System.out.println(
                        "[RECOVERY ENGINE] "
                                + serviceName
                                + " readiness: "
                                + podReady
                                + ", restarts: "
                                + restartCount
                );

                return;
            }

            /*
             * Detect pod replacement.
             */
            if (!previousPodName.equals(currentPodName)) {

                System.out.println(
                        "[RECOVERY ENGINE] "
                                + serviceName
                                + " pod replacement detected"
                );

                System.out.println(
                        "[RECOVERY ENGINE] Previous pod: "
                                + previousPodName
                );

                System.out.println(
                        "[RECOVERY ENGINE] New pod: "
                                + currentPodName
                );

                previousPodNames.put(
                        serviceName,
                        currentPodName
                );

                previousRestartCounts.put(
                        serviceName,
                        restartCount
                );

                consecutiveUnhealthyEvaluations.put(
                        serviceName,
                        0
                );

                if ("Running".equalsIgnoreCase(status)
                        && podReady) {

                    System.out.println(
                            "[RECOVERY ENGINE] Recovery confirmed: "
                                    + serviceName
                                    + " -> "
                                    + currentPodName
                                    + " (Ready)"
                    );

                    recoveryInProgress.put(
                            serviceName,
                            false
                    );
                }

                return;
            }

            /*
             * Detect increased container restart count.
             */
            if (previousRestartCount != null
                    && restartCount > previousRestartCount) {

                System.out.println(
                        "[RECOVERY ENGINE] "
                                + serviceName
                                + " restart count increased: "
                                + previousRestartCount
                                + " -> "
                                + restartCount
                );
            }

            previousRestartCounts.put(
                    serviceName,
                    restartCount
            );

            /*
             * Direct Kubernetes safety recovery.
             *
             * A pod that is not Running or not Ready is
             * immediately considered operationally unhealthy.
             */
            if (!"Running".equalsIgnoreCase(status)
                    || !podReady) {

                consecutiveUnhealthyEvaluations.put(
                        serviceName,
                        0
                );

                System.out.println(
                        "[RECOVERY ENGINE] "
                                + serviceName
                                + " operational health problem: "
                                + currentPodName
                                + " | Phase="
                                + status
                                + " | Ready="
                                + podReady
                                + " | Restarts="
                                + restartCount
                );

                if (!recoveryInProgress.get(serviceName)) {

                    recoveryInProgress.put(
                            serviceName,
                            true
                    );

                    System.out.println(
                            "[RECOVERY ENGINE] RECOVERY REQUIRED: "
                                    + serviceName
                    );

                    System.out.println(
                            "[RECOVERY ENGINE] Starting automatic recovery: "
                                    + serviceName
                    );

                    performRecovery(serviceName);
                }

                return;
            }

            /*
             * Multi-metric decision.
             */
            RecoveryDecision decision =
                    multiMetricRecoveryService.evaluate(
                            serviceName
                    );

            System.out.println(
                    "[RECOVERY ENGINE] Multi-metric decision: "
                            + serviceName
                            + " | RecoveryRequired="
                            + decision.recoveryRequired()
                            + " | Reason="
                            + decision.reason()
                            + " | Latency="
                            + decision.latencySeconds()
                            + "s"
                            + " | CPU="
                            + decision.cpuPercent()
                            + "%"
                            + " | Memory="
                            + decision.heapMemoryPercent()
                            + "%"
                            + " | ErrorRate="
                            + decision.errorRate()
                            + " | Ready="
                            + decision.podReady()
            );

            /*
             * Persistent metric anomaly detection.
             */
            if (decision.recoveryRequired()) {

                int currentCount =
                        consecutiveUnhealthyEvaluations.get(serviceName) + 1;

                consecutiveUnhealthyEvaluations.put(
                        serviceName,
                        currentCount
                );

                System.out.println(
                        "[RECOVERY ENGINE] "
                                + serviceName
                                + " unhealthy evaluation "
                                + currentCount
                                + "/"
                                + requiredConsecutiveUnhealthyEvaluations
                );

                if (currentCount >= requiredConsecutiveUnhealthyEvaluations) {

                    if (!recoveryInProgress.get(serviceName)) {

                        recoveryInProgress.put(
                                serviceName,
                                true
                        );

                        System.out.println(
                                "[RECOVERY ENGINE] PERSISTENT ANOMALY CONFIRMED: "
                                        + serviceName
                        );

                        System.out.println(
                                "[RECOVERY ENGINE] MULTI-METRIC RECOVERY REQUIRED: "
                                        + serviceName
                        );

                        System.out.println(
                                "[RECOVERY ENGINE] Reason: "
                                        + decision.reason()
                        );

                        System.out.println(
                                "[RECOVERY ENGINE] Starting automatic recovery: "
                                        + serviceName
                        );

                        performRecovery(serviceName);
                    }

                    return;
                }

                return;
            }

            /*
             * Healthy metric evaluation resets the persistence counter.
             */
            if (consecutiveUnhealthyEvaluations.get(serviceName) > 0) {

                System.out.println(
                        "[RECOVERY ENGINE] "
                                + serviceName
                                + " anomaly cleared before persistence threshold"
                );
            }

            consecutiveUnhealthyEvaluations.put(
                    serviceName,
                    0
            );

            /*
             * Healthy state.
             */
            System.out.println(
                    "[RECOVERY ENGINE] "
                            + serviceName
                            + " healthy: "
                            + currentPodName
                            + " | Ready=true"
                            + " | Restarts="
                            + restartCount
            );

            if (recoveryInProgress.get(serviceName)) {

                System.out.println(
                        "[RECOVERY ENGINE] Recovery confirmed: "
                                + serviceName
                                + " -> "
                                + currentPodName
                );

                recoveryInProgress.put(
                        serviceName,
                        false
                );
            }

        } catch (ApiException e) {

            System.out.println(
                    "[RECOVERY ENGINE] Kubernetes API error for "
                            + serviceName
                            + ": "
                            + e.getMessage()
            );
        }
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

    private void performRecovery(String serviceName) {

        try {

            V1Pod servicePod =
                    kubernetesRecoveryService.findServicePod(
                            monitoredServices.get(serviceName)
                    );

            if (servicePod == null) {

                System.out.println(
                        "[RECOVERY ENGINE] No pod available to delete for "
                                + serviceName
                );

                return;
            }

            String podName =
                    servicePod.getMetadata().getName();

            System.out.println(
                    "[RECOVERY ENGINE] Deleting "
                            + serviceName
                            + " pod: "
                            + podName
            );

            kubernetesRecoveryService.deleteServicePod(
                    podName
            );

            System.out.println(
                    "[RECOVERY ENGINE] Recovery action executed successfully: "
                            + serviceName
            );

        } catch (ApiException e) {

            System.out.println(
                    "[RECOVERY ENGINE] Recovery action failed for "
                            + serviceName
                            + ": "
                            + e.getMessage()
            );

            recoveryInProgress.put(
                    serviceName,
                    false
            );
        }
    }
}