package playground.grpc;

import lombok.Data;
import org.kohsuke.args4j.Option;

@Data
public class HelloServerOptions {

    public static final int DEFAULT_SERVER_PORT = 50051;

    public static final String DEFAULT_USERNAME = "admin";

    public static final String DEFAULT_PASSWORD = "123456";

    @Option(name = "--port")
    int port = DEFAULT_SERVER_PORT;

    @Option(name = "--ca-cert")
    String caCert;

    @Option(name = "--tls-certs")
    String tlsCerts;

    @Option(name = "--tls-key")
    String tlsKey;
}
