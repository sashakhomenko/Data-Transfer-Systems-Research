package pl.edu.vistula.firstrestapispring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.vistula.firstrestapispring.benchmark.BenchmarkResult;
import pl.edu.vistula.firstrestapispring.benchmark.GrpcBenchmark;
import pl.edu.vistula.firstrestapispring.benchmark.PulsarBenchmark;


@RestController
@RequestMapping("/benchmark")
public class BenchmarkController {

    @Autowired
    private GrpcBenchmark grpcBenchmark;

    @Autowired
    private PulsarBenchmark pulsarBenchmark;

    // Returns JSON with all benchmark metrics
    @GetMapping("/grpc") // "/benchmark/grpc"
    public BenchmarkResult runGrpc(@RequestParam(name = "requests", defaultValue = "1") int totalRequests) throws InterruptedException {
        return grpcBenchmark.run(totalRequests);
    }
    @GetMapping("/pulsar")
    public BenchmarkResult runPulsar(
            @RequestParam(name = "requests", defaultValue = "1") int totalRequests)
            throws Exception {
        return pulsarBenchmark.run(totalRequests);
    }
}
