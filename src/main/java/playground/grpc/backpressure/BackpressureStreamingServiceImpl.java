package playground.grpc.backpressure;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import playground.grpc.StreamingRequest;
import playground.grpc.StreamingResponse;
import playground.grpc.StreamingServiceGrpc;

/**
 * Implementation of server streaming with implemented backpressure. Server checks if stream is ready to accept
 * new message before sending them.
 */
public class BackpressureStreamingServiceImpl extends StreamingServiceGrpc.StreamingServiceImplBase {

    private static final int TOTAL_MESSAGES = 1_000_000;

    @Override
    public void streamingMethod(StreamingRequest request, StreamObserver<StreamingResponse> responseObserver) {
        // Convert response observer to ServerCallStreamObserver, so we can access additional API
        // for checking stream status
        ServerCallStreamObserver<StreamingResponse> serverCallStreamObserver =
                (ServerCallStreamObserver<StreamingResponse>) responseObserver;

        // This handler will be called each time stream becomes ready and can accept new messages.
        // Handler is executed in the same thread as service method, so we must return from it before
        // this handler is actually executed
        serverCallStreamObserver.setOnReadyHandler(new Runnable() {

            private int counter;

            @Override
            public void run() {
                // isReady() method doesn't return false immediately after client stops requesting more messages.
                // There is still some amount of buffering but will not produce OutOfMemoryError.
                while (serverCallStreamObserver.isReady() && !serverCallStreamObserver.isCancelled()
                        && counter < TOTAL_MESSAGES) {
                    responseObserver.onNext(StreamingResponse.newBuilder().setRandomId(counter++).build());
                }
                if (counter >= TOTAL_MESSAGES && !serverCallStreamObserver.isCancelled()) {
                    responseObserver.onCompleted();
                }
            }
        });

        serverCallStreamObserver.setOnCancelHandler(() -> System.out.println("Stream canceled"));
    }
}
