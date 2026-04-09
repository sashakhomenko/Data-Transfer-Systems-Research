package pl.edu.vistula.firstrestapispring.pulsar;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.pulsar.client.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.edu.vistula.firstrestapispring.product.domain.Product;
import pl.edu.vistula.firstrestapispring.product.repository.ProductRepository;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ProductPulsarService {

    @Autowired
    private PulsarClient pulsarClient;

    @Autowired
    private ProductRepository productRepository;

    @Value("${pulsar.topic.request}")
    private String requestTopic;

    @Value("${pulsar.topic.reply}")
    private String replyTopic;

    private Consumer<byte[]> consumer;
    private Producer<byte[]> replyProducer;

    private ExecutorService listenerThread;

    @PostConstruct
    public void start() throws PulsarClientException {

        // Create the producer that will send replies back to the benchmark
        replyProducer = pulsarClient.newProducer()
                .topic(replyTopic)
                .producerName("product-reply-producer")
                .create();

        // Create the consumer that listens for incoming product requests
        consumer = pulsarClient.newConsumer()
                .topic(requestTopic)

                .subscriptionName("product-service-subscription")

                .subscriptionType(SubscriptionType.Exclusive)
                .subscribe();

        // Start listening in a background thread so the app doesn't freeze
        listenerThread = Executors.newSingleThreadExecutor();
        listenerThread.submit(this::listenForRequests);

        System.out.println("[Pulsar] ProductPulsarService started, listening on: " + requestTopic);
    }

    // a loop on the background thread,
    // processing one message at a time
    private void listenForRequests() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // receive() blocks until a message arrives
                Message<byte[]> message = consumer.receive();

                // The benchmark sends the product ID as a plain string like "1"
                String body = new String(message.getData(), StandardCharsets.UTF_8);
                long productId = Long.parseLong(body.trim());

                // Fetch from the same H2 database that REST API uses
                Optional<Product> productOpt = productRepository.findById(productId);

                // Build a simple text response: "id:name" format
                String responseBody;
                if (productOpt.isPresent()) {
                    Product p = productOpt.get();
                    responseBody = p.getId() + ":" + p.getName();
                } else {
                    responseBody = "NOT_FOUND";
                }

                // Send the reply back on the reply topic
                replyProducer.newMessage()
                        .value(responseBody.getBytes(StandardCharsets.UTF_8))
                        .property("correlationId", message.getProperty("correlationId"))
                        .send();

                consumer.acknowledge(message);

            } catch (Exception e) {
                System.err.println("[Pulsar] Error processing message: " + e.getMessage());
            }
        }
    }

    // @PreDestroy runs automatically when Spring shuts down the app
    @PreDestroy
    public void stop() {
        try {
            if (listenerThread != null) listenerThread.shutdownNow();
            if (consumer != null) consumer.close();
            if (replyProducer != null) replyProducer.close();
            System.out.println("[Pulsar] ProductPulsarService stopped.");
        } catch (PulsarClientException e) {
            System.err.println("[Pulsar] Error during shutdown: " + e.getMessage());
        }
    }
}