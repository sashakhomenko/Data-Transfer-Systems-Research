package pl.edu.vistula.firstrestapispring.benchmark;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Component;
import pl.edu.vistula.firstrestapispring.grpc.ProductProto;
import pl.edu.vistula.firstrestapispring.grpc.ProductServiceGrpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcBenchmark {


    // The ID of the product that would be fetched repeatedly
    private static final long TEST_PRODUCT_ID = 1L;

    public BenchmarkResult run(int totalRequests) throws InterruptedException {

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        ProductServiceGrpc.ProductServiceBlockingStub stub =
                ProductServiceGrpc.newBlockingStub(channel);

        List<Long> latencies = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        // Warm-up: sending 50 requests before measuring for JVM to optimize code
        System.out.println("[gRPC] Warming up...");
//        for (int i = 0; i < 50; i++) {
//            try {
//                stub.getProduct(
//                        ProductProto.ProductRequest.newBuilder()
//                                .setId(TEST_PRODUCT_ID)
//                                .build()
//                );
//            } catch (Exception ignored) {}
//        }

        System.out.println("[gRPC] Starting benchmark: " + totalRequests + " requests...");
        long totalStart = System.nanoTime();

        for (int i = 0; i < totalRequests; i++) {
            long callStart = System.nanoTime();  // start timer for request

            try {
                // equivalent to GET /products/1 in REST
                stub.getProduct(
                        ProductProto.ProductRequest.newBuilder()
                                .setId(TEST_PRODUCT_ID)
                                .build()
                );
                successCount++;

            } catch (Exception e) {
                failureCount++;  // failures for the reliability metric
            }

            long callEnd = System.nanoTime();
            // nanoTime() gives nanoseconds; divide by 1,000,000 to get milliseconds
            latencies.add((callEnd - callStart) / 1_000_000);
        }

        long totalEnd = System.nanoTime();
        long totalTimeMs = (totalEnd - totalStart) / 1_000_000;

        // releases network resources
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("[gRPC] Done. Success: " + successCount +
                ", Failures: " + failureCount);

        return calculateStats(latencies, totalTimeMs, successCount, failureCount, totalRequests);
    }

    private BenchmarkResult calculateStats(List<Long> latencies, long totalTimeMs,
                                           int successCount, int failureCount, int totalRequests) {

        // Average latency: sum of all times divided by count
        double avg = latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        long min = Collections.min(latencies);
        long max = Collections.max(latencies);

        // P99 (99th percentile): sort all latencies, take the value at position 99% (variable ingores extreme outliers)
        // worst perfomance
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        long p99 = sorted.get((int) Math.ceil(sorted.size() * 0.99) - 1);

        // Throughput: total requests divided by total seconds elapsed
        double throughput = (double) totalRequests / (totalTimeMs / 1000.0);

        // Reliability: what % of requests succeeded
        double reliability = (double) successCount / totalRequests * 100.0;

        return new BenchmarkResult(
                "gRPC", totalRequests, totalTimeMs, avg, min, max, p99, throughput, reliability, totalTimeMs
        );
    }
}