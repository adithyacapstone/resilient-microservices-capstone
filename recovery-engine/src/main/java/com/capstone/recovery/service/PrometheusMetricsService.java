package com.capstone.recovery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PrometheusMetricsService {

    @Value("${prometheus.url}")
    private String prometheusUrl;

    /**
     * Executes an instant PromQL query and returns the first numeric result.
     * Returns NaN when Prometheus has no usable result.
     */
    public double getQueryValue(String promQl) {

        try {

            String encodedQuery =
                    URLEncoder.encode(
                            promQl,
                            StandardCharsets.UTF_8
                    );

            URI uri = URI.create(
                    prometheusUrl
                            + "/api/v1/query?query="
                            + encodedQuery
            );

            Map<?, ?> response =
                    RestClient.create()
                            .get()
                            .uri(uri)
                            .retrieve()
                            .body(Map.class);

            if (response == null) {
                return Double.NaN;
            }

            Object dataObject = response.get("data");

            if (!(dataObject instanceof Map<?, ?> data)) {
                return Double.NaN;
            }

            Object resultObject = data.get("result");

            if (!(resultObject instanceof java.util.List<?> results)
                    || results.isEmpty()) {
                return Double.NaN;
            }

            Object firstResult = results.get(0);

            if (!(firstResult instanceof Map<?, ?> result)) {
                return Double.NaN;
            }

            Object valueObject = result.get("value");

            if (!(valueObject instanceof java.util.List<?> value)
                    || value.size() < 2) {
                return Double.NaN;
            }

            return Double.parseDouble(
                    String.valueOf(value.get(1))
            );

        } catch (Exception e) {

            System.out.println(
                    "[PROMETHEUS] Query failed: "
                            + promQl
                            + " | "
                            + e.getMessage()
            );

            return Double.NaN;
        }
    }

    /**
     * Average HTTP request latency in seconds over the last 5 minutes.
     */
    public double getAverageRequestLatency(
            String serviceName) {

        String query =
                "sum(rate(http_server_requests_seconds_sum{"
                        + "job=\"" + serviceName + "\""
                        + "}[5m]))"
                        + " / "
                        + "sum(rate(http_server_requests_seconds_count{"
                        + "job=\"" + serviceName + "\""
                        + "}[5m]))";

        return getQueryValue(query);
    }

    /**
     * Container CPU usage as a percentage.
     */
    public double getCpuUsage(
            String serviceName) {

        String query =
                "sum(rate(container_cpu_usage_seconds_total{"
                        + "namespace=\"default\","
                        + "container=\"" + serviceName + "\""
                        + "}[5m])) * 100";

        return getQueryValue(query);
    }

    /**
     * JVM heap memory used in bytes.
     */
  public double getHeapMemoryUsage(
        String serviceName) {

    String query =
            "sum(container_memory_working_set_bytes{"
                    + "namespace=\"default\","
                    + "container=\"" + serviceName + "\""
                    + "})";

    return getQueryValue(query);
}

    /**
     * JVM heap memory usage percentage.
     */
    public double getHeapMemoryUsagePercent(
        String serviceName) {

    String query =
            "100 * sum(container_memory_working_set_bytes{"
                    + "namespace=\"default\","
                    + "container=\"" + serviceName + "\""
                    + "}) / 1000000000";

    return getQueryValue(query);
}

    /**
     * HTTP error rate as a percentage.
     *
     * Counts both CLIENT_ERROR (4xx) and SERVER_ERROR (5xx)
     * responses, matching the Grafana error-rate definition.
     */
    public double getErrorRate(
            String serviceName) {

        String query =
                "100 * ("
                        + "sum(rate(http_server_requests_seconds_count{"
                        + "job=\"" + serviceName + "\","
                        + "outcome=~\"CLIENT_ERROR|SERVER_ERROR\""
                        + "}[1m]))"
                        + " / "
                        + "sum(rate(http_server_requests_seconds_count{"
                        + "job=\"" + serviceName + "\""
                        + "}[1m]))"
                        + ")";

        double value =
                getQueryValue(query);

        return Double.isNaN(value)
                ? 0.0
                : value;
    }
}