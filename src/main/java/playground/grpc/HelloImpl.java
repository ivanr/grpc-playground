package playground.grpc;

import io.grpc.stub.StreamObserver;

public class HelloImpl extends HelloGrpc.HelloImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        HelloResponse response = HelloResponse.newBuilder()
                .setMessage("Hello.")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
