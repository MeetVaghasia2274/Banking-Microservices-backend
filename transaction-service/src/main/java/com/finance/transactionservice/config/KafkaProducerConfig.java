package com.finance.transactionservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Base64;

@Configuration
@Slf4j
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties(null);

        // Ensure serializers are set
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Configure SSL trust for Aiven Kafka
        String protocol = String.valueOf(props.getOrDefault("security.protocol", "PLAINTEXT"));
        if (protocol.toUpperCase().contains("SSL")) {
            // Disable hostname verification (required for Aiven)
            props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

            // Use PEM truststore from environment variable if available
            String caCert = System.getenv("KAFKA_TRUSTED_CERT");
            if (caCert == null || caCert.isBlank()) {
                log.info("KAFKA_TRUSTED_CERT not found. Attempting to fetch certificate from broker dynamically...");
                String bootstrapServers = String.valueOf(props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
                if (bootstrapServers != null && bootstrapServers.contains(":")) {
                    String[] parts = bootstrapServers.split(",")[0].split(":");
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

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
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
