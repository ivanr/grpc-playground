package playground.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.BadRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
                    } catch (InvalidProtocolBufferException ipbe) {
                        // TODO Shouldn't happen but log if it does.
                    }
                }
            }
        }
    }

    private void successfulAsyncRequest() throws Exception {
        final CountDownLatch finishLatch = new CountDownLatch(1);

        HelloRequest request = HelloRequest.newBuilder()
                .setName("Ivan")
                .build();

        asyncStub.sayHello(request, new StreamObserver<HelloResponse>() {

            @Override
            public void onNext(HelloResponse response) {
                System.out.println("Request #3: Success: " + response.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Request #3: Error: " + t.getMessage());
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Request #3: Completed.");
                finishLatch.countDown();
            }
        });

        finishLatch.await(1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) throws Exception {
        HelloClient client = new HelloClient("localhost", HelloServer.SERVER_PORT);

        client.successfulBlockingRequest();

        client.failedBlockingRequest();

        client.successfulAsyncRequest();
    }
}
