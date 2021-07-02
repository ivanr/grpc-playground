package playground.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.BadRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;

public class HelloClient {

    private final ManagedChannel channel;

    private final HelloGrpc.HelloBlockingStub blockingStub;

    private final HelloGrpc.HelloStub asyncStub;

    public HelloClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
    }

    public HelloClient(ManagedChannelBuilder<?> channelBuilder) {
        this.channel = channelBuilder.build();
        this.blockingStub = HelloGrpc.newBlockingStub(channel);
        this.asyncStub = HelloGrpc.newStub(channel);
    }

    // Example of a blocking invocation and a successful response.
    private void successfulBlockingRequest() {
        try {
            HelloResponse response = blockingStub.sayHello(
                    HelloRequest.newBuilder()
                            .setName("Ivan")
                            .build());
            System.out.println("Request #1: Success: " + response.getMessage());
        } catch (StatusRuntimeException e) {
            System.out.println("Request #1: Error: " + e.getMessage());
        }
    }

    // In this example, we don't give our name and the service responds with an error.
    private void failedBlockingRequest() {
        try {
            HelloResponse response = blockingStub.sayHello(
                    HelloRequest.newBuilder()
                            // Intentionally not providing a name to force an error.
                            .build());
            System.out.println("Request #2: Success: " + response.getMessage());
        } catch (StatusRuntimeException e) {
            System.out.println("Request #2: Error: " + e.getMessage());

            // This rich error response can contain one ore more additional
            // objects. In this case we're expecting only BadRequest instances,
            // so let's show if there are any.

            // Here we could probably check the code first, then decide
            // what types of error objects we should look for.

            // This method can return null if there's no com.google.rpc.Status in the
            // response, but you will know if one exists on a per-service basis.

            com.google.rpc.Status status = io.grpc.protobuf.StatusProto.fromThrowable(e);
            for (Any any : status.getDetailsList()) {
                if (any.is(BadRequest.class)) {
                    try {
                        BadRequest br = any.unpack(BadRequest.class);
                        for (BadRequest.FieldViolation fv : br.getFieldViolationsList()) {
                            System.out.println("Violation: " + fv.getField() + ": " + fv.getDescription());
                        }
                    } catch (InvalidProtocolBufferException ex) {
                        // TODO Shouldn't happen but log if it does.
                    }
                }
            }
        }
    }

    private void successfulAsyncRequest() throws Exception {
        HelloRequest request = HelloRequest.newBuilder()
                //.setName("Ivan")
                .build();

        // Asynchronous gRPC requested implemented as a CompletableFuture. It
        // doesn't quite make sense because this function returns only a
        // single response, but the same approach could be used with more
        // complex streaming functions.

        CompletableFuture<HelloResponse> future = new CompletableFuture<>();

        asyncStub.sayHello(request, new StreamObserver<HelloResponse>() {

            private HelloResponse response;

            @Override
            public void onNext(HelloResponse response) {
                this.response = response;
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                future.complete(response);
            }
        });

        try {
            HelloResponse response = future.get();
            System.out.println("Request #3: Success: " + response.getMessage());
        } catch (Exception e) {
            System.out.println("Request #3: Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        HelloClient client = new HelloClient("localhost", HelloServer.SERVER_PORT);

        client.successfulBlockingRequest();

        client.failedBlockingRequest();

        client.successfulAsyncRequest();
    }
}
