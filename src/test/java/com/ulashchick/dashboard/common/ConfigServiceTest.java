package com.ulashchick.dashboard.common;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ulashchick.dashboard.common.config.ConfigService;
import com.ulashchick.dashboard.common.config.EnvironmentService;
import com.ulashchick.dashboard.common.config.pojo.ApplicationConfig;
import com.ulashchick.dashboard.common.config.pojo.CassandraConfig;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;


@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

  @Mock
  EnvironmentService environmentService;

  @Mock
  Logger logger;

  @InjectMocks
  private ConfigService configService;

  @Test
  void testGetLog4jPropertyFilePath() {
    when(environmentService.getCurrentEnvironmentAsString()).thenReturn("TEST");
    final String log4jPropertyFilePath = configService.getLog4jPropertyFilePath();
    assertThat(log4jPropertyFilePath).startsWith("src/main/resources/TEST");
  }

  @Test
  void testGetGoogleClientId() {
    when(environmentService.readEnvVariable(anyString())).thenReturn("123");
    final String googleClientId = configService.getGoogleClientId();
    assertThat(googleClientId).isEqualTo("123");
  }

  @Test
  void testGetJwtSecret() {
    when(environmentService.readEnvVariable(anyString())).thenReturn("123");
    final String jwtSecret = configService.getJwtSecret();
    assertThat(jwtSecret).isEqualTo("123");
  }

  @Test
  void testGetApplicationConfigSuccess() throws IOException {
    when(environmentService.getCurrentEnvironmentAsString()).thenReturn("test");
    ApplicationConfig applicationConfig = configService.getApplicationConfig();

    assertThat(applicationConfig).isNotNull();
    assertThat(applicationConfig.getGrpcServerConfig().getPort()).isEqualTo(3333);

    final List<CassandraConfig> casConfig = applicationConfig.getCassandraConfig();

    assertThat(casConfig).hasSize(2);

    assertThat(casConfig.get(0).getHost()).isEqualTo("1.1.1.1");
    assertThat(casConfig.get(0).getPort()).isEqualTo(9042);

    assertThat(casConfig.get(1).getHost()).isEqualTo("2.2.2.2");
    assertThat(casConfig.get(1).getPort()).isEqualTo(9043);
  }


  @Test
  void testGetApplicationConfigThrows() {
    when(environmentService.getCurrentEnvironmentAsString()).thenReturn("");

    final Exception fileNotFoundException = assertThrows(
        FileNotFoundException.class,
        () -> configService.getApplicationConfig());

    assertThat(fileNotFoundException).isInstanceOf(FileNotFoundException.class);
    assertThat(fileNotFoundException).hasMessageThat().contains("No such file or directory");
  }
}
