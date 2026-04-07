package pl.edu.vistula.firstrestapispring.benchmark;


public class BenchmarkResult {

    private final String systemName; // "gRPC", "REST", "RabbitMQ", etc.
    private final long totalRequests;  // the number of requests
    private final double avgLatencyMs;   // average time per request in milliseconds
    private final long minLatencyMs;     // fastest single request
    private final long maxLatencyMs;     // slowest single request
    private final long p99LatencyMs;     // 99th percentile — 99% of requests were faster than this
    private final double throughputRps;  // requests completed per second
    private final double reliabilityPct; // percentage of requests that succeeded (0-100)
    private final long totalTimeMs;      // how long the whole test took

    public BenchmarkResult(String systemName, long totalRequests, double avgLatencyMs, long minLatencyMs,
                           long maxLatencyMs, long p99LatencyMs, double throughputRps,
                           double reliabilityPct, long totalTimeMs) {
        this.systemName = systemName;
        this.totalRequests = totalRequests;
        this.avgLatencyMs = avgLatencyMs;
        this.minLatencyMs = minLatencyMs;
        this.maxLatencyMs = maxLatencyMs;
        this.p99LatencyMs = p99LatencyMs;
        this.throughputRps = throughputRps;
        this.reliabilityPct = reliabilityPct;
        this.totalTimeMs = totalTimeMs;
    }

    public String getSystemName()      { return systemName; }
    public long getTotalRequests()   { return totalRequests; }
    public double getAvgLatencyMs()    { return avgLatencyMs; }
    public long   getMinLatencyMs()    { return minLatencyMs; }
    public long   getMaxLatencyMs()    { return maxLatencyMs; }
    public long   getP99LatencyMs()    { return p99LatencyMs; }
    public double getThroughputRps()   { return throughputRps; }
    public double getReliabilityPct()  { return reliabilityPct; }
    public long   getTotalTimeMs()     { return totalTimeMs; }

    @Override
    public String toString() {
        return String.format(
                "=== %s Benchmark Results ===\n" +
                        "Number of requests : %d \n" +
                        "Total Latency : %d ms \n" +
                        "Avg Latency : %.2f ms\n" +
                        "Min Latency : %d ms\n" +
                        "Max Latency : %d ms\n" +
                        "P99 Latency : %d ms\n" +
                        "Throughput  : %.2f req/sec\n" +
                        "Reliability : %.2f%%\n" +
                        "Total Time  : %d ms",
                systemName, totalRequests, avgLatencyMs, minLatencyMs, maxLatencyMs,
                p99LatencyMs, throughputRps, reliabilityPct, totalTimeMs
        );
    }
}