package pl.edu.vistula.firstrestapispring.pulsar;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// @Configuration tells Spring: "this class creates shared objects (beans)
@Configuration
public class PulsarConfig {

    // @Value reads a property from application.properties
    // The string inside is the property key, with a default after the colon
    @Value("${pulsar.service-url:pulsar://localhost:6650}")
    private String serviceUrl;

    // @Bean means: "create this object once and make it available
    // for @Autowired injection anywhere in the app"
    @Bean
    public PulsarClient pulsarClient() throws PulsarClientException {
        return PulsarClient.builder()
                .serviceUrl(serviceUrl)
                // How long to wait when trying to connect before giving up
                .connectionTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                // How long to wait for a response from the broker
                .operationTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }
}