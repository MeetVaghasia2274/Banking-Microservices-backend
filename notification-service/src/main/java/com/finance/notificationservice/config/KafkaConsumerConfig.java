package com.finance.notificationservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Base64;

@Configuration
@Slf4j
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);

        // Ensure deserializers are set
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Configure SSL trust for Aiven Kafka
        String protocol = String.valueOf(props.getOrDefault("security.protocol", "PLAINTEXT"));
        if (protocol.toUpperCase().contains("SSL")) {
            // Disable hostname verification (required for Aiven)
            props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

            // Use PEM truststore from environment variable if available
            String caCert = System.getenv("KAFKA_TRUSTED_CERT");
            if (caCert == null || caCert.isBlank()) {
                log.info("KAFKA_TRUSTED_CERT not found. Attempting to fetch certificate from broker dynamically...");
                Object bootstrapServers = props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
                if (bootstrapServers == null) {
                    bootstrapServers = props.get("bootstrap.servers");
                }
                
                String serversString = String.valueOf(bootstrapServers);
                if (bootstrapServers != null && serversString.contains(":")) {
                    String[] parts = serversString.replace("[", "").replace("]", "").split(",")[0].split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    try {
                        caCert = fetchBrokerCertificate(host, port);
                        log.info("Successfully fetched certificate from broker.");
                    } catch (Exception e) {
                        log.error("Failed to fetch certificate dynamically", e);
                    }
                }
            }
            
            if (caCert != null && !caCert.isBlank()) {
                props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
                props.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, caCert);
            }
        }

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
    
    private String fetchBrokerCertificate(String host, int port) throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        SSLSocketFactory factory = sc.getSocketFactory();

        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            socket.setSoTimeout(5000);
            socket.startHandshake();
            java.security.cert.Certificate[] certs = socket.getSession().getPeerCertificates();
            StringBuilder pem = new StringBuilder();
            for (java.security.cert.Certificate c : certs) {
                pem.append("-----BEGIN CERTIFICATE-----\n");
                pem.append(Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(c.getEncoded()));
                pem.append("\n-----END CERTIFICATE-----\n");
            }
            return pem.toString();
        }
    }
}
