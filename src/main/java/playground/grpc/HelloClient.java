package playground.grpc;

import com.google.protobuf.Any;
import com.google.rpc.BadRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

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

    public static void main(String[] args) throws Exception {
        HelloClient client = new HelloClient("localhost", HelloServer.SERVER_PORT);

        // Example of a blocking invocation and a successful response.

        try {
            HelloResponse response = client.blockingStub.sayHello(
                    HelloRequest.newBuilder()
                            .setName("Ivan")
                            .build());
            System.out.println("Request #1: Success: " + response.getMessage());
        } catch (StatusRuntimeException e) {
            System.out.println("Request #2: Error: " + e.getMessage());
        }

        // In this example, we don't give our name and the service responds with an error.

        try {
            HelloResponse response = client.blockingStub.sayHello(
                    HelloRequest.newBuilder()
                            // .setName("Ivan")
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
                    BadRequest br = any.unpack(BadRequest.class);
                    for (BadRequest.FieldViolation fv : br.getFieldViolationsList()) {
                        System.out.println("Violation: " + fv.getField() + ": " + fv.getDescription());
                    }
                }
            }
        }
    }
}
