package playground.grpc;

import io.grpc.Context;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicAuth {

    public static final String AUTH_HEADER_NAME = "Authorization";

    public static final String AUTH_TYPE_PREFIX = "Basic ";

    public static final Context.Key<String> AUTH_USERNAME_CTX_KEY = Context.key("AUTH_USERNAME");

    public static String encodeAuthorizationHeader(String username, String password) {
        StringBuilder sb = new StringBuilder();
        sb.append(AUTH_TYPE_PREFIX);
        sb.append(Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8)
        ));
        return sb.toString();
    }
}
