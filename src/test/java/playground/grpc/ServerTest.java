package playground.grpc;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

// Adapted from
// https://github.com/grpc/grpc-java/blob/master/examples/src/test/java/io/grpc/examples/helloworld/HelloWorldClientTest.java

public class ServerTest {

    // https://grpc.io/blog/graceful-cleanup-junit-tests/
    // https://grpc.github.io/grpc-java/javadoc/index.html?io/grpc/testing/package-summary.html

    @Rule
    public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private ManagedChannel channel;

    private HelloGrpc.HelloBlockingStub blockingStub;

    @Before
    public void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(
                        new HelloServerImpl())
                .build().start());

        channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build());

        blockingStub = HelloGrpc.newBlockingStub(channel);
    }

    @Test
    public void test() {
        HelloResponse response = blockingStub.sayHello(
                HelloRequest.newBuilder()
                        .setName("Ivan")
                        .build());
        Assert.assertEquals("Hello Ivan.", response.getGreeting());
    }
}
