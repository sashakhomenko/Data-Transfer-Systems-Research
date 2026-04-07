package pl.edu.vistula.firstrestapispring.benchmark;

import org.apache.pulsar.client.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Component
public class PulsarBenchmark {

    @Autowired
    private PulsarClient pulsarClient;

    @Value("${pulsar.topic.request}")
    private String requestTopic;

    @Value("${pulsar.topic.reply}")
    private String replyTopic;

    private static final long TEST_PRODUCT_ID = 1L;

    public BenchmarkResult run(int totalRequests) throws PulsarClientException, InterruptedException {

        // Producer sends requests to the service
        Producer<byte[]> producer = pulsarClient.newProducer()
                .topic(requestTopic)
                .producerName("benchmark-producer")
                .create();

        // Consumer listens for replies coming back from ProductPulsarService
        // Used subscription name so this benchmark consumer
        // doesn't interfere with the service consumer on the request topic
        Consumer<byte[]> replyConsumer = pulsarClient.newConsumer()
                .topic(replyTopic)
                .subscriptionName("benchmark-reply-subscription")
                .subscriptionType(SubscriptionType.Exclusive)
                // Only read messages that arrive AFTER we subscribe
                // (ignore old leftover messages from previous runs)
                .subscriptionInitialPosition(SubscriptionInitialPosition.Latest)
                .subscribe();

        List<Long> latencies = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

//        // Warm-up: 50 requests not counted in results
//        System.out.println("[Pulsar] Warming up...");
//        for (int i = 0; i < 50; i++) {
//            String warmupId = "warmup-" + i;
//            producer.newMessage()
//                    .value(String.valueOf(TEST_PRODUCT_ID).getBytes(StandardCharsets.UTF_8))
//                    .property("correlationId", warmupId)
//                    .send();
//            // Drain the reply — we don't care about timing here
//            replyConsumer.receive(3, TimeUnit.SECONDS);
//        }

        System.out.println("[Pulsar] Starting benchmark: " + totalRequests + " requests...");
        long totalStart = System.nanoTime();

        for (int i = 0; i < totalRequests; i++) {
            // Each request gets a unique ID so we can match it to its reply
            String correlationId = "req-" + i;
            long callStart = System.nanoTime();

            try {
                // Send the request — message body is the product ID as bytes
                producer.newMessage()
                        .value(String.valueOf(TEST_PRODUCT_ID).getBytes(StandardCharsets.UTF_8))
                        // correlationId travels with the message as a metadata property
                        .property("correlationId", correlationId)
                        .send();

                // Wait up to 5 seconds for a reply
                // receive(timeout) returns null if nothing arrives in time
                Message<byte[]> reply = replyConsumer.receive(5, TimeUnit.SECONDS);

                if (reply != null) {
                    // Acknowledge so Pulsar removes the reply from the topic
                    replyConsumer.acknowledge(reply);
                    successCount++;
                } else {
                    // Timeout — no reply arrived in 5 seconds
                    failureCount++;
                    System.err.println("[Pulsar] Timeout waiting for reply to: " + correlationId);
                }

            } catch (Exception e) {
                failureCount++;
                System.err.println("[Pulsar] Request failed: " + e.getMessage());
            }

            long callEnd = System.nanoTime();
            latencies.add((callEnd - callStart) / 1_000_000);
        }

        long totalEnd = System.nanoTime();
        long totalTimeMs = (totalEnd - totalStart) / 1_000_000;

        // Always close producers and consumers when done
        producer.close();
        replyConsumer.close();

        System.out.println("[Pulsar] Done. Success: " + successCount +
                ", Failures: " + failureCount);

        return calculateStats(latencies, totalTimeMs, successCount, failureCount, totalRequests);
    }

    private BenchmarkResult calculateStats(List<Long> latencies, long totalTimeMs,
                                           int successCount, int failureCount,
                                           int totalRequests) {

        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long min = Collections.min(latencies);
        long max = Collections.max(latencies);

        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        long p99 = sorted.get((int) Math.ceil(sorted.size() * 0.99) - 1);

        long totalLatency = latencies.stream().mapToLong(Long::longValue).sum();
        double throughput = (double) totalRequests / (totalTimeMs / 1000.0);
        double reliability = (double) successCount / totalRequests * 100.0;

        return new BenchmarkResult(
                "Pulsar", totalRequests, avg, min, max, p99, throughput, reliability, totalTimeMs
        );
    }
}