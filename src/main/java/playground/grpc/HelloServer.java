package playground.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

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

        server = ServerBuilder.forPort(opts.getPort())
                .addService(new HelloServerImpl())
                .build()
                .start();

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
