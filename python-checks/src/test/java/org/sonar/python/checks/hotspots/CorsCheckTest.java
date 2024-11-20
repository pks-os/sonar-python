/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.python.checks.hotspots;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.python.checks.utils.PythonCheckVerifier;

class CorsCheckTest {

  @Test
  void test() {
    PythonCheckVerifier.verify(Collections.singletonList("src/test/resources/checks/hotspots/cors/cors.py"), new CorsCheck());
  }

  @Test
  void test_django_settings() {
    PythonCheckVerifier.verify(Collections.singletonList("src/test/resources/checks/hotspots/cors/settings.py"), new CorsCheck());
  }

}
