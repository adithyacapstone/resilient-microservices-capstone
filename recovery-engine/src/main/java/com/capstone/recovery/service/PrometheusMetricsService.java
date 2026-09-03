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

    public double getAverageRequestLatency(
            String serviceName) {

        String query =
                "sum(rate(http_server_requests_seconds_sum{"
                        + "job=\"" + serviceName + "\""
                        + "}[5m]))"
                        + "/"
                        + "sum(rate(http_server_requests_seconds_count{"
                        + "job=\"" + serviceName + "\""
                        + "}[5m]))";

        return getQueryValue(query);
    }

    public double getCpuUsage(
            String serviceName) {

        String query =
                "sum(rate(container_cpu_usage_seconds_total{"
                        + "namespace=\"default\","
                        + "container=\"" + serviceName + "\""
                        + "}[5m])) * 100";

        return getQueryValue(query);
    }

    public double getHeapMemoryUsage(
            String serviceName) {

        String query =
                "sum(jvm_memory_used_bytes{"
                        + "job=\"" + serviceName + "\","
                        + "area=\"heap\""
                        + "})";

        return getQueryValue(query);
    }

    public double getHeapMemoryUsagePercent(
            String serviceName) {

        String query =
                "100 * sum(jvm_memory_used_bytes{"
                        + "job=\"" + serviceName + "\","
                        + "area=\"heap\""
                        + "})"
                        + "/"
                        + "sum(jvm_memory_max_bytes{"
                        + "job=\"" + serviceName + "\","
                        + "area=\"heap\""
                        + "})";

        return getQueryValue(query);
    }

    public double getErrorRate(
            String serviceName) {

        String query =
                "sum(rate(http_server_requests_seconds_count{"
                        + "job=\"" + serviceName + "\","
                        + "status!=\"200\""
                        + "}[5m]))";

        double value =
                getQueryValue(query);

        /*
         * An empty result means there are currently
         * no non-200 HTTP request series.
         *
         * Treat that as zero observed errors.
         */
        return Double.isNaN(value)
                ? 0.0
                : value;
    }
}