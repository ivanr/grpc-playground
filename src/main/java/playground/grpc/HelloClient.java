package playground.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.BadRequest;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.TlsChannelCredentials;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class HelloClient {

    private final ManagedChannel channel;

    private final HelloGrpc.HelloBlockingStub blockingStub;

    private final HelloGrpc.HelloStub asyncStub;

    public HelloClient(String host, int port) throws Exception {
        // Configure TLS using mutual authentication. To authenticate
        // the server we install a custom trust manager that uses a
        // private CA. To authenticate to the server, we use a client certificate.

        TlsChannelCredentials.Builder tlsBuilder = TlsChannelCredentials.newBuilder();
        tlsBuilder.keyManager(
                HelloClient.class.getResourceAsStream("client.pem"),
                HelloClient.class.getResourceAsStream("client.key.pem"));
        tlsBuilder.trustManager(HelloClient.class.getResourceAsStream("ca.pem"));

        this.channel = Grpc.newChannelBuilderForAddress(host, port, tlsBuilder.build())
                // The server certificate is valid for 'localhost' and so that's fine
                // but in a development environment it something might be useful to
                // relax server hostname validation as in the example below.
                //.overrideAuthority("example.com")
                .build();

        BasicAuthCallCredentials callCredentials = new BasicAuthCallCredentials(
                HelloServerOptions.DEFAULT_USERNAME,
                HelloServerOptions.DEFAULT_PASSWORD);

        // Create stubs with authentication and deadlines. Client should always
        // specify deadlines, otherwise something may go wrong and the calls
        // will take forever.

        GrpcTelemetry grpcTelemetry = GrpcTelemetry.create(GlobalOpenTelemetry.get());

        this.blockingStub = HelloGrpc.newBlockingStub(channel)
                .withCallCredentials(callCredentials)
                .withDeadlineAfter(1000, TimeUnit.MILLISECONDS)
                .withInterceptors(grpcTelemetry.newClientInterceptor());

        this.asyncStub = HelloGrpc.newStub(channel)
                .withCallCredentials(callCredentials)
                .withDeadlineAfter(1000, TimeUnit.MILLISECONDS);
    }

    public HelloClient(ManagedChannel channel) throws Exception {
        this.channel = channel;
        this.blockingStub = HelloGrpc.newBlockingStub(channel)
                .withDeadlineAfter(1000, TimeUnit.MILLISECONDS);
        this.asyncStub = HelloGrpc.newStub(channel)
                .withDeadlineAfter(1000, TimeUnit.MILLISECONDS);
    }

    // Example of a blocking invocation and a successful response.
    private void successfulBlockingRequest() {
        try {
            HelloResponse response = blockingStub.sayHello(
                    HelloRequest.newBuilder()
                            .setName("Ivan")
                            .build());
            System.out.println("Request #1: Success: " + response.getGreeting());
        } catch (StatusRuntimeException e) {
            System.out.println("Request #1: Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    // In this example, we don't give our name and the service responds with an error.
    private void failedBlockingRequest() {
        try {
            HelloResponse response = blockingStub.sayHello(
                    HelloRequest.newBuilder()
                            // Intentionally not providing a name to force an error.
                            .build());
            System.out.println("Request #2: Success: " + response.getGreeting());
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

    private void successfulAsyncRequest() {
        HelloRequest request = HelloRequest.newBuilder()
                .setName("Ivan")
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
            System.out.println("Request #3: Success: " + response.getGreeting());
        } catch (Exception e) {
            System.out.println("Request #3: Error: " + e.getMessage());
        }
    }

    // This call fails because of a short deadline.
    private void failedDeadlineExceeded() {

        // In this example, we set a deadline, which then automatically
        // cancels the request once exceeded.
        //
        // There are other ways for the client to cancel:
        //   1. Blocking -- wrap call in a Context, call cancel on the context (possibly on a separate thread)
        //   2. Async -- call RequestObserver#onError(Status.CANCELLED.asRuntimeException())
        //   3. Future -- cancel the future

        try {
            HelloResponse response = blockingStub
                    .withDeadlineAfter(1_000, TimeUnit.MILLISECONDS)
                    .sayHello(
                            HelloRequest.newBuilder()
                                    .setName("Slow")
                                    .build());
            System.out.println("Request #4: Success: " + response.getGreeting());
        } catch (StatusRuntimeException e) {
            System.out.println("Request #4: Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    public static void main(String[] args) throws Exception {
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                /* Should you want to export the collected spans, uncomment this bit.
                .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().build())
                        .setMaxExportBatchSize(1)
                        .setMaxQueueSize(1)
                        .build())*/
                .build();

        OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(TextMapPropagator.composite(
                        W3CBaggagePropagator.getInstance(),
                        W3CTraceContextPropagator.getInstance()
                )))
                .buildAndRegisterGlobal();

        HelloClient client = new HelloClient("localhost", HelloServerOptions.DEFAULT_SERVER_PORT);

        Span span = GlobalOpenTelemetry.getTracer("my tracer")
                .spanBuilder("my span")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            Baggage.current().toBuilder().put("BAGGAGE_TEST", "AAA").build().makeCurrent();
            System.out.println("CLIENT TRACE_ID: " + Span.current().getSpanContext().getTraceId());
            client.successfulBlockingRequest();
        } finally {
            span.end();
        }

        client.failedBlockingRequest();

        client.successfulAsyncRequest();

        client.failedDeadlineExceeded();

        // TODO Example with a future stub.
    }
}
