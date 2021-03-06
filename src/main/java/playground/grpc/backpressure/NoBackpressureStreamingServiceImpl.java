package playground.grpc.backpressure;

import io.grpc.stub.StreamObserver;
import playground.grpc.StreamingRequest;
import playground.grpc.StreamingResponse;
import playground.grpc.StreamingServiceGrpc;

import java.util.Random;

/**
 * Implementation of server streaming with infinite production of random responses. Eventually this will
 * produce {@link OutOfMemoryError} when client can't consumer stream fast enough. Server will buffer unsent
 * responses until memory is full. Backpressure is not implemented because server doesn't check if stream is
 * ready to accept new messages.
 */
public class NoBackpressureStreamingServiceImpl extends StreamingServiceGrpc.StreamingServiceImplBase {

    private static final int TOTAL_MESSAGES = 1_000_000_000;

    private final Random random = new Random();

    @Override
    public void streamingMethod(StreamingRequest request, StreamObserver<StreamingResponse> responseObserver) {
        try {
            for (int i = 0; i < TOTAL_MESSAGES; i++) {
                responseObserver.onNext(StreamingResponse.newBuilder().setRandomId(random.nextInt()).build());
            }
            responseObserver.onCompleted();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }
}
