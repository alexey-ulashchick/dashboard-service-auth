package com.ulashchick.dashboard.auth;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.slf4j.Logger;

@Singleton
public class AuthInterceptor implements ServerInterceptor {

  public static final Context.Key<UUID> UUID_KEY = Context.key("uuid");
  public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key
      .of("Authorization", ASCII_STRING_MARSHALLER);

  public static final String TOKEN_TYPE = "Bearer ";

  @Inject
  private Logger logger;

  @Inject
  private JwtService jwtService;

  private List<String> servicesToExclude;

  public void setServicesToExclude(List<String> servicesToExclude) {
    this.servicesToExclude = servicesToExclude;
  }

  @Override
  public <R, T> Listener<R> interceptCall(ServerCall<R, T> call,
      Metadata headers,
      ServerCallHandler<R, T> next) {

    final MethodDescriptor<R, T> methodDescriptor = call.getMethodDescriptor();
    final String fullJavaMethodName = String
        .format("%s.%s", methodDescriptor.getServiceName(), methodDescriptor.getBareMethodName())
        .toLowerCase();

    final boolean allowRequest = servicesToExclude.stream().noneMatch(fullJavaMethodName::contains);

    if (allowRequest) {
      return Contexts.interceptCall(Context.current(), call, headers, next);
    }

    try {
      final String authorizationHeader = AuthInterceptor.getAuthorizationHeader(headers);
      final String rawToken = AuthInterceptor.validatePrefixAndReturnRawToken(authorizationHeader);
      final UUID uuid = jwtService.validateAndGetUUID(rawToken);
      final Context context = Context.current().withValue(UUID_KEY, uuid);

      return Contexts.interceptCall(context, call, headers, next);
    } catch (Exception e) {
      logger.error("Cannot authenticate user", e);
      call.close(Status.UNAUTHENTICATED, headers);
      return new Listener<R>() {
      };
    }
  }

  private static String getAuthorizationHeader(@Nonnull Metadata headers) {
    return Optional
        .ofNullable(headers.get(AUTHORIZATION_METADATA_KEY))
        .orElseThrow(() -> new JWTVerificationException("Authorization token is missing"));
  }

  private static String validatePrefixAndReturnRawToken(@Nonnull String headerValue) {
    return Optional
        .of(headerValue)
        .filter(token -> token.startsWith(TOKEN_TYPE))
        .map(token -> token.substring(TOKEN_TYPE.length()).trim())
        .orElseThrow(() -> new JWTVerificationException("Unknown authorization type"));
  }

}