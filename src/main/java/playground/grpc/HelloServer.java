package playground.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HelloServer {

    public static final int SERVER_PORT = 50051;

    private Server server;

    private void start() throws IOException {

        server = ServerBuilder.forPort(SERVER_PORT)
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
        System.out.println("Starting server...");
        server.start();
        server.blockUntilShutdown();
    }
}
