package playground.grpc;

import lombok.Data;
import org.kohsuke.args4j.Option;

@Data
public class HelloServerOptions {

    public static final int DEFAULT_SERVER_PORT = 50051;

    @Option(name = "--port")
    int port = DEFAULT_SERVER_PORT;

    @Option(name = "--tls-certs")
    String tlsCerts;

    @Option(name = "--tls-key")
    String tlsKey;
}
