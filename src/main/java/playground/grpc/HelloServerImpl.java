package playground.grpc;

import com.google.protobuf.Any;
import com.google.rpc.BadRequest;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;

public class HelloServerImpl extends HelloGrpc.HelloImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        System.out.println("SERVER TRACE_ID: " + Span.current().getSpanContext().getTraceId());
        System.out.println("BAGGAGE_TEST: " + Baggage.current().getEntryValue("BAGGAGE_TEST"));

        // We require that the client tells us their name. If they
        // don't, we refuse to talk to them and use this opportunity
        // to demonstrate error handling in a gRPC service.

        // The name is defined as a `string`, which is a primitive value
        // and therefore can't be null. The best we can do is check for blank.

        if (request.getName().isBlank()) {

            // There are two options for handling errors. The lower-level
            // gRPC approach is to return an io.grpc.Status instance
            // using the correct code (int) and description (String).

            // This code obviously won't run; it's only here to
            // demonstrate the less-flexible approach to error handling.
            if (false) {
                responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                        .withDescription("Can't talk to you without knowing your name")
                        .asRuntimeException());
            }

            // When gRPC is used with protocol buffers, there is an extension
            // that supports a richer error model. The codes are the same, but
            // with this approach supports attaching one or more additional objects.
            // https://cloud.google.com/apis/design/errors#error_model
            //
            // The code values in io.grpc.Status and com.google.rpc.Code are the same.
            //
            // There are some predefined detailed objects:
            // https://github.com/googleapis/googleapis/blob/master/google/rpc/error_details.proto

            responseObserver.onError(StatusProto.toStatusRuntimeException(Status.newBuilder()
                    .setCode(Code.INVALID_ARGUMENT.getNumber())
                    .setMessage("Invalid parameters provided")
                    .addDetails(Any.pack(BadRequest.newBuilder()
                            .addFieldViolations(
                                    BadRequest.FieldViolation.newBuilder()
                                            .setField("name")
                                            .setDescription("Field is required")
                                            .build())
                            .build()))
                    .build()));

            return;
        }

        // It seems that we have a name. Say hello back.

        // We can determine if a deadline is set on the request. We don't really
        // need to, as the gRPC library will automatically cancel if the allocated
        // time expires. Similarly, the library will propagate the deadline if
        // we make further gRPC calls from here.

        Deadline deadline = Context.current().getDeadline();
        if (deadline != null) {
            System.err.println("Request deadline: " + deadline);
        }

        HelloResponse response = HelloResponse.newBuilder()
                .setGreeting("Hello " + request.getName() + ".")
                .build();

        // In the following block we simulate slow operation and exceed
        // the deadline set by the client. The idea is to show how the
        // server can detect cancellation.

        if (request.getName().equals("Slow")) {

            // There are several ways to handle cancellation. For example, you could
            // call ServerCallStreamObserver#isCancelled or register a callback with
            // ServerCallStreamObserver#setOnCancelHandler (as below).

            ServerCallStreamObserver serverCallStreamObserver = (ServerCallStreamObserver) responseObserver;
            serverCallStreamObserver.setOnCancelHandler(new Runnable() {
                @Override
                public void run() {
                    // We don't do anything here in this example, but
                    // in real life we could take an action that cancels
                    // some work in progress.
                    System.err.println("Request cancelled");
                }
            });

            // Simulate long operation in a loop.

            for (; ; ) {
                try {
                    Thread.currentThread().sleep(10);
                } catch (InterruptedException e) {
                    responseObserver.onError(io.grpc.Status.CANCELLED
                            .withDescription("Cancelled (interrupted)")
                            .asRuntimeException());
                    return;
                }

                // Depending on how the work is structured, it may also
                // make sense to check the thread's interrupted flag.

                if (Thread.currentThread().isInterrupted()) {
                    responseObserver.onError(io.grpc.Status.CANCELLED
                            .withDescription("Cancelled (interrupted)")
                            .asRuntimeException());
                    return;
                }

                // The other way to check for cancellation is to use Context#isCancelled.

                if (Context.current().isCancelled()) {
                    responseObserver.onError(io.grpc.Status.CANCELLED
                            .withDescription("Cancelled (client)")
                            .asRuntimeException());
                    return;
                }
            }
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
