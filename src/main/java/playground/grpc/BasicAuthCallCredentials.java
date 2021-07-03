package playground.grpc;

import io.grpc.CallCredentials;
import io.grpc.Metadata;

import java.util.concurrent.Executor;

public class BasicAuthCallCredentials extends CallCredentials {

    private final String username;

    private final String password;

    public BasicAuthCallCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor executor, MetadataApplier applier) {
        // Add the token to the call via the metadata.

        Metadata headers = new Metadata();
        Metadata.Key<String> authKey = Metadata.Key.of(BasicAuth.AUTH_HEADER_NAME, Metadata.ASCII_STRING_MARSHALLER);
        headers.put(authKey, BasicAuth.encodeAuthorizationHeader(username, password));
        applier.apply(headers);

        /*

        // This method shouldn't block. If the credentials are not readily available then the
        // work should be delegated to the supplied executor, as in the example below.

        executor.execute(() -> {
            try {
                // TODO Apply the headers here.
            } catch (Throwable e) {
                applier.fail(Status.UNAUTHENTICATED.withCause(e));
            }
        });

        */
    }

    @Override
    public void thisUsesUnstableApi() {
        // gRPC authors added this required method to highlight the fact that
        // the CallCredentials class is experimental. And so we have to implement it,
        // even though it won't ever be called.
    }
}
