package com.ulashchick.dashboard.auth;

import com.google.inject.Injector;
import com.ulashchick.dashboard.auth.annotations.GrpcService;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.reflections.Reflections;

public class ApplicationServerBuilder {

  private static final Logger LOGGER = Logger.getLogger(ApplicationServerBuilder.class.getName());
  private int port;
  private List<BindableService> services;

  private ApplicationServerBuilder() {
  }

  public static ApplicationServerBuilder newServer() {
    return new ApplicationServerBuilder();
  }

  public ApplicationServerBuilder forPort(int port) {
    this.port = port;

    return this;
  }

  public ApplicationServerBuilder bindAnnotatedServices(@Nonnull Injector injector) {
    final Reflections reflections = new Reflections(this.getClass().getPackage().getName());

    services = reflections
        .getTypesAnnotatedWith(GrpcService.class)
        .stream()
        .map(this::toBindableServiceOrNull)
        .filter(Objects::nonNull)
        .map(injector::getInstance)
        .collect(Collectors.toList());

    return this;
  }

  public Server build() {
    final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);

    services
        .stream()
        .map(Object::getClass)
        .map(Class::getName)
        .map(name -> "Binding GrpcService: " + name)
        .forEach(LOGGER::info);

    services.forEach(serverBuilder::addService);

    return serverBuilder.build();
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private Class<BindableService> toBindableServiceOrNull(Class<?> klass) {
    return BindableService.class.isAssignableFrom(klass)
        ? (Class<BindableService>) klass
        : null;
  }

}
