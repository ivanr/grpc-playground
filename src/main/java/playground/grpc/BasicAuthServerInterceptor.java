package playground.grpc;

import io.grpc.*;

import java.util.Base64;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class BasicAuthServerInterceptor implements ServerInterceptor {

    private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {
    };

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String header = headers.get(Metadata.Key.of(BasicAuth.AUTH_HEADER_NAME, ASCII_STRING_MARSHALLER));

        if (header == null) {
            call.close(Status.UNAUTHENTICATED
                            .withDescription("Missing " + BasicAuth.AUTH_HEADER_NAME + " metadata header"),
                    headers);
            return NOOP_LISTENER;
        }

        String username = authenticateUser(header);
        if (username == null) {
            call.close(Status.UNAUTHENTICATED
                            .withDescription("Invalid authentication token"),
                    headers);
            return NOOP_LISTENER;
        }

        Context ctx = Context.current().withValue(BasicAuth.AUTH_USERNAME_CTX_KEY, username);

        return Contexts.interceptCall(ctx, call, headers, next);
    }

    private String authenticateUser(String header) {
        if (header == null) {
            return null;
        }

        if (!header.startsWith(BasicAuth.AUTH_TYPE_PREFIX)) {
            return null;
        }

        try {
            String encoded = header.substring(BasicAuth.AUTH_TYPE_PREFIX.length()).trim();
            String decoded = new String(Base64.getDecoder().decode(encoded));

            String[] tokens = decoded.split(":");
            if (tokens.length != 2) {
                return null;
            }

            String username = tokens[0];
            String password = tokens[1];

            if (!HelloServerOptions.DEFAULT_USERNAME.equals(username)) {
                return null;
            }

            if (!HelloServerOptions.DEFAULT_PASSWORD.equals(password)) {
                return null;
            }

            return username;
        } catch (Exception e) {
            // TODO Log
            return null;
        }
    }
}
