/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2025 SonarSource SA
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
package org.sonar.plugins.python.api.caching;

import java.io.InputStream;
import javax.annotation.CheckForNull;
import org.sonar.api.Beta;

@Beta
public interface PythonReadCache {
  /**
   * Returns an input stream for the data cached with the provided {@code key}. It is the responsibility of the caller to close the stream.
   */
  InputStream read(String key);

  /**
   * @return the array of bytes stored for the given key, if any. {@code null} otherwise.
   */
  @CheckForNull
  byte[] readBytes(String key);

  /**
   * Checks whether the cache contains the provided {@code key}.
   */
  boolean contains(String key);
}
