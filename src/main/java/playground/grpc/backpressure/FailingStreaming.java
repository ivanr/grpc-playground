package playground.grpc.backpressure;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.SneakyThrows;
import playground.grpc.StreamingRequest;
import playground.grpc.StreamingResponse;
import playground.grpc.StreamingServiceGrpc;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FailingStreaming {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        startServer();

        EXECUTOR.execute(FailingStreaming::startClient);

        blockUntilShutdown();
        EXECUTOR.shutdown();
    }

    @SneakyThrows
    private static void startClient() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:50000").usePlaintext().build();
        StreamingServiceGrpc.StreamingServiceBlockingStub blockingStub = StreamingServiceGrpc.newBlockingStub(channel);
        try {
            Iterator<StreamingResponse> responseIterator = blockingStub.streamingMethod(StreamingRequest.getDefaultInstance());
            while (responseIterator.hasNext()) {
                System.out.println(responseIterator.next().getRandomId());
                Thread.sleep(1000);
            }
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static void startServer() throws IOException {
        server = ServerBuilder.forPort(50000).addService(new NoBackpressureStreamingServiceImpl()).build().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                FailingStreaming.stopServer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
    }

    private static void stopServer() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private static void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
