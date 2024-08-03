package playground.grpc;

import io.grpc.Grpc;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.TlsServerCredentials;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HelloServer {
    
    private Server server;

    private void start(String[] args) throws IOException {
        HelloServerOptions opts = new HelloServerOptions();
        CmdLineParser parser = new CmdLineParser(opts);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("HelloServer [options...] arguments...");
            parser.printUsage(System.err);
            System.err.println();
        }

        System.out.println("Starting server...");

        TlsServerCredentials.Builder tlsBuilder = TlsServerCredentials.newBuilder();

        tlsBuilder.clientAuth(TlsServerCredentials.ClientAuth.REQUIRE);

        if (opts.getCaCert() != null) {
            tlsBuilder.trustManager(new File(opts.getCaCert()));
        } else {
            tlsBuilder.trustManager(this.getClass().getResourceAsStream("ca.pem"));
        }

        if ((opts.getTlsCerts() != null) && (opts.getTlsKey() != null)) {
            System.out.println("Using TLS with the supplied certificate");
            tlsBuilder.keyManager(
                    new File(opts.getTlsCerts()),
                    new File(opts.getTlsKey()));
        } else {
            System.out.println("Using TLS with a self-signed certificate");
            tlsBuilder.keyManager(
                    this.getClass().getResourceAsStream("localhost.pem"),
                    this.getClass().getResourceAsStream("localhost.key.pem"));
        }

        server = Grpc.newServerBuilderForPort(opts.getPort(), tlsBuilder.build())
                // Add the service with basic authentication via interceptor.
                .addService(ServerInterceptors.intercept(
                        new HelloServerImpl(),
                        new BasicAuthServerInterceptor()))
                // TODO The default executor doesn't provide optimal performance,
                //      which is why it's generally recommended to configure your own.
                // .executor()
                .build().start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    HelloServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final HelloServer server = new HelloServer();
        server.start(args);
        server.blockUntilShutdown();
    }
}
