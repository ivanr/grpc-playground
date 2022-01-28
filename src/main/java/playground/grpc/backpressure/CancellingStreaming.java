package playground.grpc.backpressure;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import lombok.SneakyThrows;
import playground.grpc.StreamingRequest;
import playground.grpc.StreamingResponse;
import playground.grpc.StreamingServiceGrpc;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CancellingStreaming {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        startServer();

        EXECUTOR.execute(CancellingStreaming::startClient);

        blockUntilShutdown();
        EXECUTOR.shutdown();
    }

    @SneakyThrows
    private static void startClient() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:50000").usePlaintext().build();
        StreamingServiceGrpc.StreamingServiceStub stub = StreamingServiceGrpc.newStub(channel);

        ClientResponseObserver<StreamingRequest, StreamingResponse> clientResponseObserver =
                new ClientResponseObserver<>() {

                    private static final int TOTAL_RECEIVE = 20;

                    private int received;
                    private ClientCallStreamObserver<StreamingRequest> requestStream;

                    @Override
                    public void beforeStart(ClientCallStreamObserver<StreamingRequest> requestStream) {
                        this.requestStream = requestStream;
                    }

                    @SneakyThrows
                    @Override
                    public void onNext(StreamingResponse response) {
                        System.out.println(response.getRandomId());
                        received++;
                        if (received < TOTAL_RECEIVE) {
                            Thread.sleep(500);
                        } else {
                            requestStream.cancel("Stop sending messages", null);
                            Thread.sleep(5000);
                            stopServer();
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                    }

                    @SneakyThrows
                    @Override
                    public void onCompleted() {
                        stopServer();
                    }
                };

        stub.streamingMethod(StreamingRequest.getDefaultInstance(), clientResponseObserver);
    }

    private static void startServer() throws IOException {
        server = ServerBuilder.forPort(50000).addService(new BackpressureStreamingServiceImpl()).build().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                CancellingStreaming.stopServer();
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
