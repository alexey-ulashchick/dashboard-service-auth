package com.ulashchick.dashboard.common;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DependencyManagerTest {

  @Test
  void testInitialization() {
    final Exception notInitializedException = assertThrows(
        IllegalStateException.class,
        DependencyManager::getInjector
    );

    assertThat(notInitializedException)
        .hasMessageThat()
        .isEqualTo("Injector has not been created yet.");

    DependencyManager.init();

    final Exception illegalStateException = assertThrows(
        IllegalStateException.class,
        DependencyManager::init
    );

    assertThat(illegalStateException)
        .hasMessageThat()
        .isEqualTo("Injector already has been created.");
  }

}
