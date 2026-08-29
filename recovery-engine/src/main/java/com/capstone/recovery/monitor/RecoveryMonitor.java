package com.capstone.recovery.monitor;

import com.capstone.recovery.service.KubernetesRecoveryService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RecoveryMonitor {

    private final KubernetesRecoveryService kubernetesRecoveryService;

    /*
     * Services monitored by the Recovery Engine.
     *
     * Key   = logical service name
     * Value = Kubernetes app label
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
     *
     * This allows the Recovery Engine to detect
     * container restart activity.
     */
    private final Map<String, Integer> previousRestartCounts =
            new LinkedHashMap<>();

    /*
     * Prevent repeated recovery actions while recovery
     * is already in progress for a service.
     */
    private final Map<String, Boolean> recoveryInProgress =
            new LinkedHashMap<>();

    public RecoveryMonitor(
            KubernetesRecoveryService kubernetesRecoveryService) {

        this.kubernetesRecoveryService =
                kubernetesRecoveryService;

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
             * Service pod not found.
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

            /*
             * NEW OPERATIONAL SIGNAL:
             *
             * Check Kubernetes Ready condition.
             */
            boolean podReady =
                    kubernetesRecoveryService.isPodReady(
                            servicePod
                    );

            /*
             * NEW OPERATIONAL SIGNAL:
             *
             * Read the container restart count.
             */
            int restartCount =
                    getRestartCount(servicePod);

            String previousPodName =
                    previousPodNames.get(serviceName);

            Integer previousRestartCount =
                    previousRestartCounts.get(serviceName);

            /*
             * First observation.
             */
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
             * MULTI-SIGNAL HEALTH DECISION
             *
             * Healthy requires:
             *
             * 1. Pod phase = Running
             * 2. Kubernetes Ready condition = True
             */
            if ("Running".equalsIgnoreCase(status)
                    && podReady) {

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

            } else {

                /*
                 * Pod is Running but not Ready,
                 * OR pod phase itself is unhealthy.
                 */
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

    /*
     * Get the total restart count across all containers
     * in the monitored pod.
     */
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