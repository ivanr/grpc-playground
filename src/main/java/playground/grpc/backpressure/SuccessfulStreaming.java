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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SuccessfulStreaming {

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
    private Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        SuccessfulStreaming streamingServer = new SuccessfulStreaming();
        streamingServer.startServer();

        EXECUTOR.execute(SuccessfulStreaming::startClient);

        streamingServer.blockUntilShutdown();
    }

    @SneakyThrows
    private static void startClient() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:50000").usePlaintext().build();
        StreamingServiceGrpc.StreamingServiceStub stub = StreamingServiceGrpc.newStub(channel);

        ClientResponseObserver<StreamingRequest, StreamingResponse> clientResponseObserver =
                new ClientResponseObserver<>() {
                    @Override
                    public void beforeStart(ClientCallStreamObserver<StreamingRequest> requestStream) {
                    }

                    @SneakyThrows
                    @Override
                    public void onNext(StreamingResponse response) {
                        System.out.println(response.getRandomId());
                        Thread.sleep(500);
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                    }
                };

        stub.streamingMethod(StreamingRequest.getDefaultInstance(), clientResponseObserver);
    }

    private void startServer() throws IOException {
        server = ServerBuilder.forPort(50000).addService(new BackpressureStreamingServiceImpl()).build().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                SuccessfulStreaming.this.stopServer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
    }

    private void stopServer() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
