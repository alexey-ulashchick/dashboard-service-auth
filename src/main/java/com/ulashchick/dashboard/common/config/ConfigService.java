package com.ulashchick.dashboard.common.config;

import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ulashchick.dashboard.common.config.pojo.ApplicationConfig;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;

@Singleton
public class ConfigService {

  @Inject
  private EnvironmentService environmentService;

  @Inject
  private Logger logger;

  private static final String RESOURCES_PATH = "src/main/resources";
  private static final String LOG_4J_PROP = "log4j.properties";
  private static final String APP_CONFIG = "app-config.yaml";
  private static final String CQL_INIT_DIR = "cql-init";

  private ApplicationConfig applicationConfig;

  public String getLog4jPropertyFilePath() {
    final String env = environmentService.getCurrentEnvironmentAsString();
    return String.format("%s/%s/%s", RESOURCES_PATH, env, LOG_4J_PROP);
  }

  public ApplicationConfig getApplicationConfig() throws IOException {
    if (applicationConfig != null) {
      return applicationConfig;
    }

    final String env = environmentService.getCurrentEnvironmentAsString();
    final String appConfigPath = String.format("%s/%s/%s", RESOURCES_PATH, env, APP_CONFIG);
    final File file = new File(appConfigPath);
    final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    applicationConfig = objectMapper.readValue(file, ApplicationConfig.class);

    return applicationConfig;
  }

  public List<InetSocketAddress> getCassandraEndpoints() throws IOException {
    return getApplicationConfig()
        .getCassandraConfig()
        .stream()
        .map(casConfig -> new InetSocketAddress(casConfig.getHost(), casConfig.getPort()))
        .collect(Collectors.toList());
  }

  @Nonnull
  public List<SimpleStatement> getCassandraInitStatements() {
    final String env = environmentService.getCurrentEnvironmentAsString();
    final String cqlInitPath = String.format("%s/%s", env, CQL_INIT_DIR);

    URI uriFromPath;

    try {
      @Nullable URL resource = getClass().getClassLoader().getResource(cqlInitPath);
      uriFromPath = Objects.requireNonNull(resource).toURI();
    } catch (URISyntaxException | NullPointerException e) {
      logger.error("Fail to load Cassandra init statements. Cannot convert path {} to URI.",
          cqlInitPath, e);
      return ImmutableList.of();
    }

    try (Stream<Path> paths = Files.walk(Paths.get(uriFromPath))) {
      return paths
          .filter(Files::isRegularFile)
          .sorted()
          .map(this::readFileAsString)
          .filter(Objects::nonNull)
          .filter(str -> !str.trim().isEmpty())
          .map(SimpleStatement::newInstance)
          .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error("Cannot read files files in {}", uriFromPath, e);
      return ImmutableList.of();
    }
  }

  @Nullable
  private String readFileAsString(Path path) {
    try (final Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
      return stream.collect(Collectors.joining("\n"));
    } catch (IOException e) {
      logger.error("Cannot read file: {}", path);
      return null;
    }
  }

  public String getGoogleClientId() {
    return environmentService.readEnvVariable("GOOGLE_CLIENT_ID");
  }

  public String getJwtSecret() {
    return environmentService.readEnvVariable("JWT_SECRET");
  }
}
